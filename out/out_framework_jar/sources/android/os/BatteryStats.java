package android.os;

import android.app.ActivityManager;
import android.app.backup.FullBackup;
import android.app.blob.XmlTags;
import android.app.job.JobParameters;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.hardware.gnss.GnssSignalType;
import android.hardware.gnss.visibility_control.V1_0.IGnssVisibilityControlCallback;
import android.hardware.tv.tuner.FrontendInnerFec;
import android.inputmethodservice.navigationbar.NavigationBarInflaterView;
import android.location.LocationManager;
import android.media.AudioSystem;
import android.media.audio.common.AudioDeviceDescription;
import android.os.BatteryStats;
import android.os.BatteryUsageStatsQuery;
import android.provider.DeviceConfig;
import android.security.Credentials;
import android.telephony.CellSignalStrength;
import android.telephony.TelephonyManager;
import android.telephony.ims.RcsContactPresenceTuple;
import android.text.format.DateFormat;
import android.util.ArrayMap;
import android.util.LongSparseArray;
import android.util.MutableBoolean;
import android.util.Pair;
import android.util.Printer;
import android.util.SparseArray;
import android.util.SparseDoubleArray;
import android.util.SparseIntArray;
import android.util.TimeUtils;
import android.util.proto.ProtoOutputStream;
import android.view.SurfaceControl;
import com.android.internal.R;
import com.android.internal.content.NativeLibraryHelper;
import com.android.internal.os.BatteryUsageStatsProvider;
import com.android.internal.telephony.DctConstants;
import com.google.android.collect.Lists;
import java.io.FileDescriptor;
import java.io.PrintWriter;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Formatter;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
/* loaded from: classes2.dex */
public abstract class BatteryStats implements Parcelable {
    private static final String AGGREGATED_WAKELOCK_DATA = "awl";
    public static final int AGGREGATED_WAKE_TYPE_PARTIAL = 20;
    private static final String APK_DATA = "apk";
    private static final String AUDIO_DATA = "aud";
    public static final int AUDIO_TURNED_ON = 15;
    private static final String BATTERY_DATA = "bt";
    private static final String BATTERY_DISCHARGE_DATA = "dc";
    private static final String BATTERY_LEVEL_DATA = "lv";
    private static final int BATTERY_STATS_CHECKIN_VERSION = 9;
    private static final String BLUETOOTH_CONTROLLER_DATA = "ble";
    private static final String BLUETOOTH_MISC_DATA = "blem";
    public static final int BLUETOOTH_SCAN_ON = 19;
    public static final int BLUETOOTH_UNOPTIMIZED_SCAN_ON = 21;
    private static final long BYTES_PER_GB = 1073741824;
    private static final long BYTES_PER_KB = 1024;
    private static final long BYTES_PER_MB = 1048576;
    private static final String CAMERA_DATA = "cam";
    public static final int CAMERA_TURNED_ON = 17;
    private static final String CELLULAR_CONTROLLER_NAME = "Cellular";
    private static final String CHARGE_STEP_DATA = "csd";
    private static final String CHARGE_TIME_REMAIN_DATA = "ctr";
    private static final String[] CHECKIN_POWER_COMPONENT_LABELS;
    static final int CHECKIN_VERSION = 35;
    private static final String CPU_DATA = "cpu";
    private static final String CPU_TIMES_AT_FREQ_DATA = "ctf";
    private static final String DATA_CONNECTION_COUNT_DATA = "dcc";
    public static final int DATA_CONNECTION_EMERGENCY_SERVICE;
    static final String[] DATA_CONNECTION_NAMES;
    public static final int DATA_CONNECTION_OTHER;
    public static final int DATA_CONNECTION_OUT_OF_SERVICE = 0;
    private static final String DATA_CONNECTION_TIME_DATA = "dct";
    public static final int DEVICE_IDLE_MODE_DEEP = 2;
    public static final int DEVICE_IDLE_MODE_LIGHT = 1;
    public static final int DEVICE_IDLE_MODE_OFF = 0;
    private static final String DISCHARGE_STEP_DATA = "dsd";
    private static final String DISCHARGE_TIME_REMAIN_DATA = "dtr";
    public static final int DUMP_CHARGED_ONLY = 2;
    public static final int DUMP_DAILY_ONLY = 4;
    public static final int DUMP_DEVICE_WIFI_ONLY = 64;
    public static final int DUMP_HISTORY_ONLY = 8;
    public static final int DUMP_INCLUDE_HISTORY = 16;
    public static final int DUMP_VERBOSE = 32;
    public static final long DURATION_UNAVAILABLE = -1;
    private static final String FLASHLIGHT_DATA = "fla";
    public static final int FLASHLIGHT_TURNED_ON = 16;
    public static final int FOREGROUND_ACTIVITY = 10;
    private static final String FOREGROUND_ACTIVITY_DATA = "fg";
    public static final int FOREGROUND_SERVICE = 22;
    private static final String FOREGROUND_SERVICE_DATA = "fgs";
    public static final int FULL_WIFI_LOCK = 5;
    private static final String GLOBAL_BLUETOOTH_CONTROLLER_DATA = "gble";
    private static final String GLOBAL_CPU_FREQ_DATA = "gcf";
    private static final String GLOBAL_MODEM_CONTROLLER_DATA = "gmcd";
    private static final String GLOBAL_NETWORK_DATA = "gn";
    private static final String GLOBAL_WIFI_CONTROLLER_DATA = "gwfcd";
    private static final String GLOBAL_WIFI_DATA = "gwfl";
    private static final String HISTORY_DATA = "h";
    public static final String[] HISTORY_EVENT_CHECKIN_NAMES;
    public static final IntToString[] HISTORY_EVENT_INT_FORMATTERS;
    public static final String[] HISTORY_EVENT_NAMES;
    public static final BitDescription[] HISTORY_STATE2_DESCRIPTIONS;
    public static final BitDescription[] HISTORY_STATE_DESCRIPTIONS;
    private static final String HISTORY_STRING_POOL = "hsp";
    public static final int JOB = 14;
    private static final String JOBS_DEFERRED_DATA = "jbd";
    private static final String JOB_COMPLETION_DATA = "jbc";
    private static final String JOB_DATA = "jb";
    private static final String KERNEL_WAKELOCK_DATA = "kwl";
    private static final boolean LOCAL_LOGV = false;
    public static final int MAX_TRACKED_SCREEN_STATE = 4;
    public static final double MILLISECONDS_IN_HOUR = 3600000.0d;
    private static final String MISC_DATA = "m";
    private static final String MODEM_CONTROLLER_DATA = "mcd";
    public static final int NETWORK_BT_RX_DATA = 4;
    public static final int NETWORK_BT_TX_DATA = 5;
    private static final String NETWORK_DATA = "nt";
    public static final int NETWORK_MOBILE_BG_RX_DATA = 6;
    public static final int NETWORK_MOBILE_BG_TX_DATA = 7;
    public static final int NETWORK_MOBILE_RX_DATA = 0;
    public static final int NETWORK_MOBILE_TX_DATA = 1;
    public static final int NETWORK_WIFI_BG_RX_DATA = 8;
    public static final int NETWORK_WIFI_BG_TX_DATA = 9;
    public static final int NETWORK_WIFI_RX_DATA = 2;
    public static final int NETWORK_WIFI_TX_DATA = 3;
    public static final int NUM_DATA_CONNECTION_TYPES;
    public static final int NUM_NETWORK_ACTIVITY_TYPES = 10;
    public static final int NUM_SCREEN_BRIGHTNESS_BINS = 5;
    public static final int NUM_WIFI_SIGNAL_STRENGTH_BINS = 5;
    public static final long POWER_DATA_UNAVAILABLE = -1;
    private static final String POWER_USE_ITEM_DATA = "pwi";
    private static final String POWER_USE_SUMMARY_DATA = "pws";
    private static final String PROCESS_DATA = "pr";
    public static final int PROCESS_STATE = 12;
    public static final int RADIO_ACCESS_TECHNOLOGY_COUNT = 3;
    public static final int RADIO_ACCESS_TECHNOLOGY_LTE = 1;
    public static final String[] RADIO_ACCESS_TECHNOLOGY_NAMES;
    public static final int RADIO_ACCESS_TECHNOLOGY_NR = 2;
    public static final int RADIO_ACCESS_TECHNOLOGY_OTHER = 0;
    private static final String RESOURCE_POWER_MANAGER_DATA = "rpm";
    public static final String RESULT_RECEIVER_CONTROLLER_KEY = "controller_activity";
    public static final int SCREEN_BRIGHTNESS_BRIGHT = 4;
    public static final int SCREEN_BRIGHTNESS_DARK = 0;
    private static final String SCREEN_BRIGHTNESS_DATA = "br";
    public static final int SCREEN_BRIGHTNESS_DIM = 1;
    public static final int SCREEN_BRIGHTNESS_LIGHT = 3;
    public static final int SCREEN_BRIGHTNESS_MEDIUM = 2;
    static final String[] SCREEN_BRIGHTNESS_NAMES;
    static final String[] SCREEN_BRIGHTNESS_SHORT_NAMES;
    protected static final boolean SCREEN_OFF_RPM_STATS_ENABLED = false;
    public static final int SENSOR = 3;
    private static final String SENSOR_DATA = "sr";
    public static final String SERVICE_NAME = "batterystats";
    private static final String SIGNAL_SCANNING_TIME_DATA = "sst";
    private static final String SIGNAL_STRENGTH_COUNT_DATA = "sgc";
    private static final String SIGNAL_STRENGTH_TIME_DATA = "sgt";
    private static final String STATE_TIME_DATA = "st";
    @Deprecated
    public static final int STATS_CURRENT = 1;
    public static final int STATS_SINCE_CHARGED = 0;
    @Deprecated
    public static final int STATS_SINCE_UNPLUGGED = 2;
    public static final long STEP_LEVEL_INITIAL_MODE_MASK = 71776119061217280L;
    public static final int STEP_LEVEL_INITIAL_MODE_SHIFT = 48;
    public static final long STEP_LEVEL_LEVEL_MASK = 280375465082880L;
    public static final int STEP_LEVEL_LEVEL_SHIFT = 40;
    public static final int[] STEP_LEVEL_MODES_OF_INTEREST;
    public static final int STEP_LEVEL_MODE_DEVICE_IDLE = 8;
    public static final String[] STEP_LEVEL_MODE_LABELS;
    public static final int STEP_LEVEL_MODE_POWER_SAVE = 4;
    public static final int STEP_LEVEL_MODE_SCREEN_STATE = 3;
    public static final int[] STEP_LEVEL_MODE_VALUES;
    public static final long STEP_LEVEL_MODIFIED_MODE_MASK = -72057594037927936L;
    public static final int STEP_LEVEL_MODIFIED_MODE_SHIFT = 56;
    public static final long STEP_LEVEL_TIME_MASK = 1099511627775L;
    public static final int SYNC = 13;
    private static final String SYNC_DATA = "sy";
    private static final String TAG = "BatteryStats";
    private static final String UID_DATA = "uid";
    public static final String UID_TIMES_TYPE_ALL = "A";
    private static final String USER_ACTIVITY_DATA = "ua";
    private static final String VERSION_DATA = "vers";
    private static final String VIBRATOR_DATA = "vib";
    public static final int VIBRATOR_ON = 9;
    private static final String VIDEO_DATA = "vid";
    public static final int VIDEO_TURNED_ON = 8;
    private static final String WAKELOCK_DATA = "wl";
    private static final String WAKEUP_ALARM_DATA = "wua";
    private static final String WAKEUP_REASON_DATA = "wr";
    public static final int WAKE_TYPE_DRAW = 18;
    public static final int WAKE_TYPE_FULL = 1;
    public static final int WAKE_TYPE_PARTIAL = 0;
    public static final int WAKE_TYPE_WINDOW = 2;
    public static final int WIFI_AGGREGATE_MULTICAST_ENABLED = 23;
    public static final int WIFI_BATCHED_SCAN = 11;
    private static final String WIFI_CONTROLLER_DATA = "wfcd";
    private static final String WIFI_CONTROLLER_NAME = "WiFi";
    private static final String WIFI_DATA = "wfl";
    private static final String WIFI_MULTICAST_DATA = "wmc";
    public static final int WIFI_MULTICAST_ENABLED = 7;
    private static final String WIFI_MULTICAST_TOTAL_DATA = "wmct";
    public static final int WIFI_RUNNING = 4;
    public static final int WIFI_SCAN = 6;
    private static final String WIFI_SIGNAL_STRENGTH_COUNT_DATA = "wsgc";
    private static final String WIFI_SIGNAL_STRENGTH_TIME_DATA = "wsgt";
    private static final String WIFI_STATE_COUNT_DATA = "wsc";
    static final String[] WIFI_STATE_NAMES;
    private static final String WIFI_STATE_TIME_DATA = "wst";
    private static final String WIFI_SUPPL_STATE_COUNT_DATA = "wssc";
    static final String[] WIFI_SUPPL_STATE_NAMES;
    static final String[] WIFI_SUPPL_STATE_SHORT_NAMES;
    private static final String WIFI_SUPPL_STATE_TIME_DATA = "wsst";
    private static final IntToString sIntToString;
    private static final IntToString sUidToString;
    private final StringBuilder mFormatBuilder;
    private final Formatter mFormatter;
    private static final String[] STAT_NAMES = {XmlTags.TAG_LEASEE, "c", XmlTags.ATTR_UID};
    public static final long[] JOB_FRESHNESS_BUCKETS = {3600000, 7200000, 14400000, 28800000, Long.MAX_VALUE};

    /* loaded from: classes2.dex */
    public static abstract class ControllerActivityCounter {
        public abstract LongCounter getIdleTimeCounter();

        public abstract LongCounter getMonitoredRailChargeConsumedMaMs();

        public abstract LongCounter getPowerCounter();

        public abstract LongCounter getRxTimeCounter();

        public abstract LongCounter getScanTimeCounter();

        public abstract LongCounter getSleepTimeCounter();

        public abstract LongCounter[] getTxTimeCounters();
    }

    /* loaded from: classes2.dex */
    public static abstract class Counter {
        public abstract int getCountLocked(int i);

        public abstract void logState(Printer printer, String str);
    }

    /* loaded from: classes2.dex */
    public static final class DailyItem {
        public LevelStepTracker mChargeSteps;
        public LevelStepTracker mDischargeSteps;
        public long mEndTime;
        public ArrayList<PackageChange> mPackageChanges;
        public long mStartTime;
    }

    @FunctionalInterface
    /* loaded from: classes2.dex */
    public interface IntToString {
        String applyAsString(int i);
    }

    /* loaded from: classes2.dex */
    public static abstract class LongCounter {
        public abstract long getCountForProcessState(int i);

        public abstract long getCountLocked(int i);

        public abstract void logState(Printer printer, String str);
    }

    /* loaded from: classes2.dex */
    public static abstract class LongCounterArray {
        public abstract long[] getCountsLocked(int i);

        public abstract void logState(Printer printer, String str);
    }

    /* loaded from: classes2.dex */
    public static final class PackageChange {
        public String mPackageName;
        public boolean mUpdate;
        public long mVersionCode;
    }

    @Retention(RetentionPolicy.SOURCE)
    /* loaded from: classes2.dex */
    public @interface RadioAccessTechnology {
    }

    @Retention(RetentionPolicy.SOURCE)
    /* loaded from: classes2.dex */
    public @interface StatName {
    }

    public abstract void commitCurrentHistoryBatchLocked();

    public abstract long computeBatteryRealtime(long j, int i);

    public abstract long computeBatteryScreenOffRealtime(long j, int i);

    public abstract long computeBatteryScreenOffUptime(long j, int i);

    public abstract long computeBatteryTimeRemaining(long j);

    public abstract long computeBatteryUptime(long j, int i);

    public abstract long computeChargeTimeRemaining(long j);

    public abstract long computeRealtime(long j, int i);

    public abstract long computeUptime(long j, int i);

    public abstract void finishIteratingHistoryLocked();

    public abstract long getActiveRadioDurationMs(int i, int i2, int i3, long j);

    public abstract long getActiveRxRadioDurationMs(int i, int i2, long j);

    public abstract long getActiveTxRadioDurationMs(int i, int i2, int i3, long j);

    public abstract long getBatteryRealtime(long j);

    public abstract long getBatteryUptime(long j);

    public abstract BluetoothBatteryStats getBluetoothBatteryStats();

    public abstract ControllerActivityCounter getBluetoothControllerActivity();

    public abstract long getBluetoothMeasuredBatteryConsumptionUC();

    public abstract long getBluetoothScanTime(long j, int i);

    public abstract long getCameraOnTime(long j, int i);

    public abstract LevelStepTracker getChargeLevelStepTracker();

    public abstract int getCpuFreqCount();

    public abstract long[] getCpuFreqs();

    public abstract long getCpuMeasuredBatteryConsumptionUC();

    public abstract long getCurrentDailyStartTime();

    public abstract long[] getCustomConsumerMeasuredBatteryConsumptionUC();

    public abstract String[] getCustomEnergyConsumerNames();

    public abstract LevelStepTracker getDailyChargeLevelStepTracker();

    public abstract LevelStepTracker getDailyDischargeLevelStepTracker();

    public abstract DailyItem getDailyItemLocked(int i);

    public abstract ArrayList<PackageChange> getDailyPackageChanges();

    public abstract int getDeviceIdleModeCount(int i, int i2);

    public abstract long getDeviceIdleModeTime(int i, long j, int i2);

    public abstract int getDeviceIdlingCount(int i, int i2);

    public abstract long getDeviceIdlingTime(int i, long j, int i2);

    public abstract int getDischargeAmount(int i);

    public abstract int getDischargeAmountScreenDoze();

    public abstract int getDischargeAmountScreenDozeSinceCharge();

    public abstract int getDischargeAmountScreenOff();

    public abstract int getDischargeAmountScreenOffSinceCharge();

    public abstract int getDischargeAmountScreenOn();

    public abstract int getDischargeAmountScreenOnSinceCharge();

    public abstract int getDischargeCurrentLevel();

    public abstract LevelStepTracker getDischargeLevelStepTracker();

    public abstract int getDischargeStartLevel();

    public abstract int getDisplayCount();

    public abstract long getDisplayScreenBrightnessTime(int i, int i2, long j);

    public abstract long getDisplayScreenDozeTime(int i, long j);

    public abstract long getDisplayScreenOnTime(int i, long j);

    public abstract String getEndPlatformVersion();

    public abstract int getEstimatedBatteryCapacity();

    public abstract long getFlashlightOnCount(int i);

    public abstract long getFlashlightOnTime(long j, int i);

    public abstract long getGlobalWifiRunningTime(long j, int i);

    public abstract long getGnssMeasuredBatteryConsumptionUC();

    public abstract long getGpsBatteryDrainMaMs();

    public abstract long getGpsSignalQualityTime(int i, long j, int i2);

    public abstract int getHighDischargeAmountSinceCharge();

    public abstract long getHistoryBaseTime();

    public abstract int getHistoryStringPoolBytes();

    public abstract int getHistoryStringPoolSize();

    public abstract String getHistoryTagPoolString(int i);

    public abstract int getHistoryTagPoolUid(int i);

    public abstract int getHistoryTotalSize();

    public abstract int getHistoryUsedSize();

    public abstract long getInteractiveTime(long j, int i);

    public abstract boolean getIsOnBattery();

    public abstract LongSparseArray<? extends Timer> getKernelMemoryStats();

    public abstract Map<String, ? extends Timer> getKernelWakelockStats();

    public abstract int getLearnedBatteryCapacity();

    public abstract long getLongestDeviceIdleModeTime(int i);

    public abstract int getLowDischargeAmountSinceCharge();

    public abstract int getMaxLearnedBatteryCapacity();

    public abstract int getMinLearnedBatteryCapacity();

    public abstract long getMobileRadioActiveAdjustedTime(int i);

    public abstract int getMobileRadioActiveCount(int i);

    public abstract long getMobileRadioActiveTime(long j, int i);

    public abstract int getMobileRadioActiveUnknownCount(int i);

    public abstract long getMobileRadioActiveUnknownTime(int i);

    public abstract long getMobileRadioMeasuredBatteryConsumptionUC();

    public abstract ControllerActivityCounter getModemControllerActivity();

    public abstract long getNetworkActivityBytes(int i, int i2);

    public abstract long getNetworkActivityPackets(int i, int i2);

    public abstract boolean getNextHistoryLocked(HistoryItem historyItem);

    public abstract long getNextMaxDailyDeadline();

    public abstract long getNextMinDailyDeadline();

    public abstract int getNumConnectivityChange(int i);

    public abstract int getParcelVersion();

    public abstract int getPhoneDataConnectionCount(int i, int i2);

    public abstract long getPhoneDataConnectionTime(int i, long j, int i2);

    public abstract Timer getPhoneDataConnectionTimer(int i);

    public abstract int getPhoneOnCount(int i);

    public abstract long getPhoneOnTime(long j, int i);

    public abstract long getPhoneSignalScanningTime(long j, int i);

    public abstract Timer getPhoneSignalScanningTimer();

    public abstract int getPhoneSignalStrengthCount(int i, int i2);

    public abstract long getPhoneSignalStrengthTime(int i, long j, int i2);

    protected abstract Timer getPhoneSignalStrengthTimer(int i);

    public abstract int getPowerSaveModeEnabledCount(int i);

    public abstract long getPowerSaveModeEnabledTime(long j, int i);

    public abstract Map<String, ? extends Timer> getRpmStats();

    public abstract long getScreenBrightnessTime(int i, long j, int i2);

    public abstract Timer getScreenBrightnessTimer(int i);

    public abstract int getScreenDozeCount(int i);

    public abstract long getScreenDozeMeasuredBatteryConsumptionUC();

    public abstract long getScreenDozeTime(long j, int i);

    public abstract Map<String, ? extends Timer> getScreenOffRpmStats();

    public abstract int getScreenOnCount(int i);

    public abstract long getScreenOnMeasuredBatteryConsumptionUC();

    public abstract long getScreenOnTime(long j, int i);

    public abstract long getStartClockTime();

    public abstract int getStartCount();

    public abstract String getStartPlatformVersion();

    public abstract long getStatsStartRealtime();

    public abstract long[] getSystemServiceTimeAtCpuSpeeds();

    public abstract long getUahDischarge(int i);

    public abstract long getUahDischargeDeepDoze(int i);

    public abstract long getUahDischargeLightDoze(int i);

    public abstract long getUahDischargeScreenDoze(int i);

    public abstract long getUahDischargeScreenOff(int i);

    public abstract SparseArray<? extends Uid> getUidStats();

    public abstract WakeLockStats getWakeLockStats();

    public abstract Map<String, ? extends Timer> getWakeupReasonStats();

    public abstract long getWifiActiveTime(long j, int i);

    public abstract ControllerActivityCounter getWifiControllerActivity();

    public abstract long getWifiMeasuredBatteryConsumptionUC();

    public abstract int getWifiMulticastWakelockCount(int i);

    public abstract long getWifiMulticastWakelockTime(long j, int i);

    public abstract long getWifiOnTime(long j, int i);

    public abstract int getWifiSignalStrengthCount(int i, int i2);

    public abstract long getWifiSignalStrengthTime(int i, long j, int i2);

    public abstract Timer getWifiSignalStrengthTimer(int i);

    public abstract int getWifiStateCount(int i, int i2);

    public abstract long getWifiStateTime(int i, long j, int i2);

    public abstract Timer getWifiStateTimer(int i);

    public abstract int getWifiSupplStateCount(int i, int i2);

    public abstract long getWifiSupplStateTime(int i, long j, int i2);

    public abstract Timer getWifiSupplStateTimer(int i);

    public abstract boolean hasBluetoothActivityReporting();

    public abstract boolean hasModemActivityReporting();

    public abstract boolean hasWifiActivityReporting();

    public abstract boolean isProcessStateDataAvailable();

    public abstract boolean startIteratingHistoryLocked();

    public abstract void writeToParcelWithoutUids(Parcel parcel, int i);

    public BatteryStats() {
        StringBuilder sb = new StringBuilder(32);
        this.mFormatBuilder = sb;
        this.mFormatter = new Formatter(sb);
    }

    static {
        String[] strArr = {"dark", "dim", "medium", "light", "bright"};
        SCREEN_BRIGHTNESS_NAMES = strArr;
        String[] strArr2 = {AudioSystem.LEGACY_REMOTE_SUBMIX_ADDRESS, "1", "2", "3", "4"};
        SCREEN_BRIGHTNESS_SHORT_NAMES = strArr2;
        int length = TelephonyManager.getAllNetworkTypes().length + 1;
        DATA_CONNECTION_EMERGENCY_SERVICE = length;
        int i = length + 1;
        DATA_CONNECTION_OTHER = i;
        String[] strArr3 = {"oos", "gprs", "edge", "umts", "cdma", "evdo_0", "evdo_A", "1xrtt", "hsdpa", "hsupa", "hspa", "iden", "evdo_b", "lte", "ehrpd", "hspap", "gsm", "td_scdma", "iwlan", "lte_ca", "nr", "emngcy", "other"};
        DATA_CONNECTION_NAMES = strArr3;
        NUM_DATA_CONNECTION_TYPES = i + 1;
        RADIO_ACCESS_TECHNOLOGY_NAMES = new String[]{"Other", DctConstants.RAT_NAME_LTE, "NR"};
        String[] strArr4 = {"invalid", "disconn", "disabled", "inactive", "scanning", "authenticating", "associating", "associated", "4-way-handshake", "group-handshake", "completed", "dormant", "uninit"};
        WIFI_SUPPL_STATE_NAMES = strArr4;
        String[] strArr5 = {"inv", "dsc", "dis", "inact", "scan", Context.AUTH_SERVICE, "ascing", "asced", "4-way", "group", "compl", "dorm", "uninit"};
        WIFI_SUPPL_STATE_SHORT_NAMES = strArr5;
        HISTORY_STATE_DESCRIPTIONS = new BitDescription[]{new BitDescription(Integer.MIN_VALUE, "running", "r"), new BitDescription(1073741824, "wake_lock", "w"), new BitDescription(8388608, Context.SENSOR_SERVICE, XmlTags.TAG_SESSION), new BitDescription(536870912, LocationManager.GPS_PROVIDER, "g"), new BitDescription(268435456, "wifi_full_lock", "Wl"), new BitDescription(134217728, "wifi_scan", "Ws"), new BitDescription(65536, "wifi_multicast", "Wm"), new BitDescription(67108864, "wifi_radio", "Wr"), new BitDescription(33554432, "mobile_radio", "Pr"), new BitDescription(2097152, "phone_scanning", "Psc"), new BitDescription(4194304, "audio", FullBackup.APK_TREE_TOKEN), new BitDescription(1048576, "screen", GnssSignalType.CODE_TYPE_S), new BitDescription(524288, BatteryManager.EXTRA_PLUGGED, "BP"), new BitDescription(262144, "screen_doze", "Sd"), new BitDescription(HistoryItem.STATE_DATA_CONNECTION_MASK, 9, "data_conn", "Pcn", strArr3, strArr3), new BitDescription(448, 6, "phone_state", "Pst", new String[]{"in", "out", "emergency", "off"}, new String[]{"in", "out", "em", "off"}), new BitDescription(56, 3, "phone_signal_strength", "Pss", new String[]{"none", "poor", "moderate", "good", "great"}, new String[]{AudioSystem.LEGACY_REMOTE_SUBMIX_ADDRESS, "1", "2", "3", "4"}), new BitDescription(7, 0, "brightness", "Sb", strArr, strArr2)};
        HISTORY_STATE2_DESCRIPTIONS = new BitDescription[]{new BitDescription(Integer.MIN_VALUE, "power_save", "ps"), new BitDescription(1073741824, "video", "v"), new BitDescription(536870912, "wifi_running", "Ww"), new BitDescription(268435456, "wifi", GnssSignalType.CODE_TYPE_W), new BitDescription(134217728, "flashlight", "fl"), new BitDescription(100663296, 25, DeviceConfig.NAMESPACE_DEVICE_IDLE, "di", new String[]{"off", "light", RcsContactPresenceTuple.ServiceCapabilities.DUPLEX_MODE_FULL, "???"}, new String[]{"off", "light", RcsContactPresenceTuple.ServiceCapabilities.DUPLEX_MODE_FULL, "???"}), new BitDescription(16777216, "charging", "ch"), new BitDescription(262144, "usb_data", "Ud"), new BitDescription(8388608, "phone_in_call", "Pcl"), new BitDescription(4194304, "bluetooth", XmlTags.TAG_BLOB), new BitDescription(112, 4, "wifi_signal_strength", "Wss", new String[]{AudioSystem.LEGACY_REMOTE_SUBMIX_ADDRESS, "1", "2", "3", "4"}, new String[]{AudioSystem.LEGACY_REMOTE_SUBMIX_ADDRESS, "1", "2", "3", "4"}), new BitDescription(15, 0, "wifi_suppl", "Wsp", strArr4, strArr5), new BitDescription(2097152, Context.CAMERA_SERVICE, Credentials.CERTIFICATE_USAGE_CA), new BitDescription(1048576, "ble_scan", "bles"), new BitDescription(524288, "cellular_high_tx_power", "Chtp"), new BitDescription(128, 7, "gps_signal_quality", "Gss", new String[]{"poor", "good"}, new String[]{"poor", "good"})};
        HISTORY_EVENT_NAMES = new String[]{"null", "proc", FOREGROUND_ACTIVITY_DATA, "top", "sync", "wake_lock_in", "job", "user", "userfg", "conn", "active", "pkginst", "pkgunin", "alarm", Context.STATS_MANAGER, "pkginactive", "pkgactive", "tmpwhitelist", "screenwake", "wakeupap", "longwake", "est_capacity"};
        HISTORY_EVENT_CHECKIN_NAMES = new String[]{"Enl", "Epr", "Efg", "Etp", "Esy", "Ewl", "Ejb", "Eur", "Euf", "Ecn", "Eac", "Epi", "Epu", "Eal", "Est", "Eai", "Eaa", "Etw", "Esw", "Ewa", "Elw", "Eec"};
        IntToString intToString = new IntToString() { // from class: android.os.BatteryStats$$ExternalSyntheticLambda0
            @Override // android.os.BatteryStats.IntToString
            public final String applyAsString(int i2) {
                return UserHandle.formatUid(i2);
            }
        };
        sUidToString = intToString;
        IntToString intToString2 = new IntToString() { // from class: android.os.BatteryStats$$ExternalSyntheticLambda1
            @Override // android.os.BatteryStats.IntToString
            public final String applyAsString(int i2) {
                return Integer.toString(i2);
            }
        };
        sIntToString = intToString2;
        HISTORY_EVENT_INT_FORMATTERS = new IntToString[]{intToString, intToString, intToString, intToString, intToString, intToString, intToString, intToString, intToString, intToString, intToString, intToString2, intToString, intToString, intToString, intToString, intToString, intToString, intToString, intToString, intToString, intToString2};
        WIFI_STATE_NAMES = new String[]{"off", "scanning", "no_net", "disconn", "sta", "p2p", "sta_p2p", "soft_ap"};
        STEP_LEVEL_MODES_OF_INTEREST = new int[]{7, 15, 11, 7, 7, 7, 7, 7, 15, 11};
        STEP_LEVEL_MODE_VALUES = new int[]{0, 4, 8, 1, 5, 2, 6, 3, 7, 11};
        STEP_LEVEL_MODE_LABELS = new String[]{"screen off", "screen off power save", "screen off device idle", "screen on", "screen on power save", "screen doze", "screen doze power save", "screen doze-suspend", "screen doze-suspend power save", "screen doze-suspend device idle"};
        String[] strArr6 = new String[18];
        CHECKIN_POWER_COMPONENT_LABELS = strArr6;
        strArr6[0] = "scrn";
        strArr6[1] = CPU_DATA;
        strArr6[2] = "blue";
        strArr6[3] = Context.CAMERA_SERVICE;
        strArr6[4] = "audio";
        strArr6[5] = "video";
        strArr6[6] = "flashlight";
        strArr6[8] = "cell";
        strArr6[9] = "sensors";
        strArr6[10] = "gnss";
        strArr6[11] = "wifi";
        strArr6[13] = "memory";
        strArr6[14] = "phone";
        strArr6[15] = "ambi";
        strArr6[16] = "idle";
    }

    /* loaded from: classes2.dex */
    public static abstract class Timer {
        public abstract int getCountLocked(int i);

        public abstract long getTimeSinceMarkLocked(long j);

        public abstract long getTotalTimeLocked(long j, int i);

        public abstract void logState(Printer printer, String str);

        public long getMaxDurationMsLocked(long elapsedRealtimeMs) {
            return -1L;
        }

        public long getCurrentDurationMsLocked(long elapsedRealtimeMs) {
            return -1L;
        }

        public long getTotalDurationMsLocked(long elapsedRealtimeMs) {
            return -1L;
        }

        public Timer getSubTimer() {
            return null;
        }

        public boolean isRunningLocked() {
            return false;
        }
    }

    public static int mapToInternalProcessState(int procState) {
        if (procState == 20) {
            return 7;
        }
        if (procState == 2) {
            return 0;
        }
        if (ActivityManager.isForegroundService(procState)) {
            return 1;
        }
        if (procState <= 6) {
            return 2;
        }
        if (procState <= 11) {
            return 3;
        }
        if (procState <= 12) {
            return 4;
        }
        if (procState > 13) {
            return 6;
        }
        return 5;
    }

    public static int mapUidProcessStateToBatteryConsumerProcessState(int processState) {
        switch (processState) {
            case 0:
            case 2:
                return 1;
            case 1:
                return 3;
            case 3:
            case 4:
                return 2;
            case 5:
            default:
                return 0;
            case 6:
                return 4;
        }
    }

    /* loaded from: classes2.dex */
    public static abstract class Uid {
        public static final int NUM_PROCESS_STATE = 7;
        public static final int NUM_USER_ACTIVITY_TYPES;
        public static final int NUM_WIFI_BATCHED_SCAN_BINS = 5;
        public static final int PROCESS_STATE_BACKGROUND = 3;
        public static final int PROCESS_STATE_CACHED = 6;
        public static final int PROCESS_STATE_FOREGROUND = 2;
        public static final int PROCESS_STATE_FOREGROUND_SERVICE = 1;
        public static final int PROCESS_STATE_HEAVY_WEIGHT = 5;
        public static final int PROCESS_STATE_NONEXISTENT = 7;
        public static final int PROCESS_STATE_TOP = 0;
        public static final int PROCESS_STATE_TOP_SLEEPING = 4;
        static final String[] USER_ACTIVITY_TYPES;
        static final String[] PROCESS_STATE_NAMES = {"Top", "Fg Service", "Foreground", "Background", "Top Sleeping", "Heavy Weight", "Cached"};
        public static final String[] UID_PROCESS_TYPES = {"T", "FS", "F", GnssSignalType.CODE_TYPE_B, "TS", "HW", GnssSignalType.CODE_TYPE_C};

        /* loaded from: classes2.dex */
        public static abstract class Pkg {

            /* loaded from: classes2.dex */
            public static abstract class Serv {
                public abstract int getLaunches(int i);

                public abstract long getStartTime(long j, int i);

                public abstract int getStarts(int i);
            }

            public abstract ArrayMap<String, ? extends Serv> getServiceStats();

            public abstract ArrayMap<String, ? extends Counter> getWakeupAlarmStats();
        }

        /* loaded from: classes2.dex */
        public static abstract class Proc {

            /* loaded from: classes2.dex */
            public static class ExcessivePower {
                public static final int TYPE_CPU = 2;
                public static final int TYPE_WAKE = 1;
                public long overTime;
                public int type;
                public long usedTime;
            }

            public abstract int countExcessivePowers();

            public abstract ExcessivePower getExcessivePower(int i);

            public abstract long getForegroundTime(int i);

            public abstract int getNumAnrs(int i);

            public abstract int getNumCrashes(int i);

            public abstract int getStarts(int i);

            public abstract long getSystemTime(int i);

            public abstract long getUserTime(int i);

            public abstract boolean isActive();
        }

        /* loaded from: classes2.dex */
        public static abstract class Sensor {
            public static final int GPS = -10000;

            public abstract int getHandle();

            public abstract Timer getSensorBackgroundTime();

            public abstract Timer getSensorTime();
        }

        /* loaded from: classes2.dex */
        public static abstract class Wakelock {
            public abstract Timer getWakeTime(int i);
        }

        public abstract Timer getAggregatedPartialWakelockTimer();

        public abstract Timer getAudioTurnedOnTimer();

        public abstract ControllerActivityCounter getBluetoothControllerActivity();

        public abstract long getBluetoothMeasuredBatteryConsumptionUC();

        public abstract long getBluetoothMeasuredBatteryConsumptionUC(int i);

        public abstract Timer getBluetoothScanBackgroundTimer();

        public abstract Counter getBluetoothScanResultBgCounter();

        public abstract Counter getBluetoothScanResultCounter();

        public abstract Timer getBluetoothScanTimer();

        public abstract Timer getBluetoothUnoptimizedScanBackgroundTimer();

        public abstract Timer getBluetoothUnoptimizedScanTimer();

        public abstract Timer getCameraTurnedOnTimer();

        public abstract long getCpuActiveTime();

        public abstract long getCpuActiveTime(int i);

        public abstract long[] getCpuClusterTimes();

        public abstract boolean getCpuFreqTimes(long[] jArr, int i);

        public abstract long[] getCpuFreqTimes(int i);

        public abstract long getCpuMeasuredBatteryConsumptionUC();

        public abstract long getCpuMeasuredBatteryConsumptionUC(int i);

        public abstract long[] getCustomConsumerMeasuredBatteryConsumptionUC();

        public abstract void getDeferredJobsCheckinLineLocked(StringBuilder sb, int i);

        public abstract void getDeferredJobsLineLocked(StringBuilder sb, int i);

        public abstract Timer getFlashlightTurnedOnTimer();

        public abstract Timer getForegroundActivityTimer();

        public abstract Timer getForegroundServiceTimer();

        public abstract long getFullWifiLockTime(long j, int i);

        public abstract long getGnssMeasuredBatteryConsumptionUC();

        public abstract ArrayMap<String, SparseIntArray> getJobCompletionStats();

        public abstract ArrayMap<String, ? extends Timer> getJobStats();

        public abstract int getMobileRadioActiveCount(int i);

        public abstract long getMobileRadioActiveTime(int i);

        public abstract long getMobileRadioActiveTimeInProcessState(int i);

        public abstract long getMobileRadioApWakeupCount(int i);

        public abstract long getMobileRadioMeasuredBatteryConsumptionUC();

        public abstract long getMobileRadioMeasuredBatteryConsumptionUC(int i);

        public abstract ControllerActivityCounter getModemControllerActivity();

        public abstract Timer getMulticastWakelockStats();

        public abstract long getNetworkActivityBytes(int i, int i2);

        public abstract long getNetworkActivityPackets(int i, int i2);

        public abstract ArrayMap<String, ? extends Pkg> getPackageStats();

        public abstract SparseArray<? extends Pid> getPidStats();

        public abstract long getProcessStateTime(int i, long j, int i2);

        public abstract Timer getProcessStateTimer(int i);

        public abstract ArrayMap<String, ? extends Proc> getProcessStats();

        public abstract double getProportionalSystemServiceUsage();

        public abstract boolean getScreenOffCpuFreqTimes(long[] jArr, int i);

        public abstract long[] getScreenOffCpuFreqTimes(int i);

        public abstract long getScreenOnMeasuredBatteryConsumptionUC();

        public abstract SparseArray<? extends Sensor> getSensorStats();

        public abstract ArrayMap<String, ? extends Timer> getSyncStats();

        public abstract long getSystemCpuTimeUs(int i);

        public abstract long getTimeAtCpuSpeed(int i, int i2, int i3);

        public abstract int getUid();

        public abstract int getUserActivityCount(int i, int i2);

        public abstract long getUserCpuTimeUs(int i);

        public abstract Timer getVibratorOnTimer();

        public abstract Timer getVideoTurnedOnTimer();

        public abstract ArrayMap<String, ? extends Wakelock> getWakelockStats();

        public abstract int getWifiBatchedScanCount(int i, int i2);

        public abstract long getWifiBatchedScanTime(int i, long j, int i2);

        public abstract ControllerActivityCounter getWifiControllerActivity();

        public abstract long getWifiMeasuredBatteryConsumptionUC();

        public abstract long getWifiMeasuredBatteryConsumptionUC(int i);

        public abstract long getWifiMulticastTime(long j, int i);

        public abstract long getWifiRadioApWakeupCount(int i);

        public abstract long getWifiRunningTime(long j, int i);

        public abstract long getWifiScanActualTime(long j);

        public abstract int getWifiScanBackgroundCount(int i);

        public abstract long getWifiScanBackgroundTime(long j);

        public abstract Timer getWifiScanBackgroundTimer();

        public abstract int getWifiScanCount(int i);

        public abstract long getWifiScanTime(long j, int i);

        public abstract Timer getWifiScanTimer();

        public abstract boolean hasNetworkActivity();

        public abstract boolean hasUserActivity();

        public abstract void noteActivityPausedLocked(long j);

        public abstract void noteActivityResumedLocked(long j);

        public abstract void noteFullWifiLockAcquiredLocked(long j);

        public abstract void noteFullWifiLockReleasedLocked(long j);

        public abstract void noteUserActivityLocked(int i);

        public abstract void noteWifiBatchedScanStartedLocked(int i, long j);

        public abstract void noteWifiBatchedScanStoppedLocked(long j);

        public abstract void noteWifiMulticastDisabledLocked(long j);

        public abstract void noteWifiMulticastEnabledLocked(long j);

        public abstract void noteWifiRunningLocked(long j);

        public abstract void noteWifiScanStartedLocked(long j);

        public abstract void noteWifiScanStoppedLocked(long j);

        public abstract void noteWifiStoppedLocked(long j);

        static {
            String[] strArr = {"other", "button", "touch", Context.ACCESSIBILITY_SERVICE, Context.ATTENTION_SERVICE};
            USER_ACTIVITY_TYPES = strArr;
            NUM_USER_ACTIVITY_TYPES = strArr.length;
        }

        /* loaded from: classes2.dex */
        public class Pid {
            public int mWakeNesting;
            public long mWakeStartMs;
            public long mWakeSumMs;

            public Pid() {
            }
        }
    }

    /* loaded from: classes2.dex */
    public static final class LevelStepTracker {
        public long mLastStepTime = -1;
        public int mNumStepDurations;
        public final long[] mStepDurations;

        public LevelStepTracker(int maxLevelSteps) {
            this.mStepDurations = new long[maxLevelSteps];
        }

        public LevelStepTracker(int numSteps, long[] steps) {
            this.mNumStepDurations = numSteps;
            long[] jArr = new long[numSteps];
            this.mStepDurations = jArr;
            System.arraycopy(steps, 0, jArr, 0, numSteps);
        }

        public long getDurationAt(int index) {
            return this.mStepDurations[index] & BatteryStats.STEP_LEVEL_TIME_MASK;
        }

        public int getLevelAt(int index) {
            return (int) ((this.mStepDurations[index] & BatteryStats.STEP_LEVEL_LEVEL_MASK) >> 40);
        }

        public int getInitModeAt(int index) {
            return (int) ((this.mStepDurations[index] & BatteryStats.STEP_LEVEL_INITIAL_MODE_MASK) >> 48);
        }

        public int getModModeAt(int index) {
            return (int) ((this.mStepDurations[index] & BatteryStats.STEP_LEVEL_MODIFIED_MODE_MASK) >> 56);
        }

        private void appendHex(long val, int topOffset, StringBuilder out) {
            boolean hasData = false;
            while (topOffset >= 0) {
                int digit = (int) ((val >> topOffset) & 15);
                topOffset -= 4;
                if (hasData || digit != 0) {
                    hasData = true;
                    if (digit >= 0 && digit <= 9) {
                        out.append((char) (digit + 48));
                    } else {
                        out.append((char) ((digit + 97) - 10));
                    }
                }
            }
        }

        public void encodeEntryAt(int index, StringBuilder out) {
            long item = this.mStepDurations[index];
            long duration = BatteryStats.STEP_LEVEL_TIME_MASK & item;
            int level = (int) ((BatteryStats.STEP_LEVEL_LEVEL_MASK & item) >> 40);
            int initMode = (int) ((BatteryStats.STEP_LEVEL_INITIAL_MODE_MASK & item) >> 48);
            int modMode = (int) ((BatteryStats.STEP_LEVEL_MODIFIED_MODE_MASK & item) >> 56);
            switch ((initMode & 3) + 1) {
                case 1:
                    out.append('f');
                    break;
                case 2:
                    out.append('o');
                    break;
                case 3:
                    out.append(DateFormat.DATE);
                    break;
                case 4:
                    out.append(DateFormat.TIME_ZONE);
                    break;
            }
            if ((initMode & 4) != 0) {
                out.append('p');
            }
            if ((initMode & 8) != 0) {
                out.append('i');
            }
            switch ((modMode & 3) + 1) {
                case 1:
                    out.append('F');
                    break;
                case 2:
                    out.append('O');
                    break;
                case 3:
                    out.append('D');
                    break;
                case 4:
                    out.append('Z');
                    break;
            }
            if ((modMode & 4) != 0) {
                out.append('P');
            }
            if ((modMode & 8) != 0) {
                out.append('I');
            }
            out.append('-');
            appendHex(level, 4, out);
            out.append('-');
            appendHex(duration, 36, out);
        }

        public void decodeEntryAt(int index, String value) {
            char c;
            char c2;
            char c3;
            char c4;
            char c5;
            char c6;
            int N = value.length();
            int i = 0;
            long out = 0;
            while (true) {
                c = '-';
                if (i < N && (c6 = value.charAt(i)) != '-') {
                    i++;
                    switch (c6) {
                        case 'D':
                            out |= 144115188075855872L;
                            break;
                        case 'F':
                            out |= 0;
                            break;
                        case 'I':
                            out |= 576460752303423488L;
                            break;
                        case 'O':
                            out |= 72057594037927936L;
                            break;
                        case 'P':
                            out |= 288230376151711744L;
                            break;
                        case 'Z':
                            out |= 216172782113783808L;
                            break;
                        case 'd':
                            out |= FrontendInnerFec.FEC_128_180;
                            break;
                        case 'f':
                            out |= 0;
                            break;
                        case 'i':
                            out |= FrontendInnerFec.FEC_135_180;
                            break;
                        case 'o':
                            out |= FrontendInnerFec.FEC_104_180;
                            break;
                        case 'p':
                            out |= FrontendInnerFec.FEC_132_180;
                            break;
                        case 'z':
                            out |= 844424930131968L;
                            break;
                    }
                }
            }
            int i2 = i + 1;
            long level = 0;
            while (true) {
                c2 = '9';
                c3 = 4;
                if (i2 < N && (c5 = value.charAt(i2)) != '-') {
                    i2++;
                    level <<= 4;
                    if (c5 >= '0' && c5 <= '9') {
                        level += c5 - '0';
                    } else if (c5 >= 'a' && c5 <= 'f') {
                        level += (c5 - 'a') + 10;
                    } else if (c5 >= 'A' && c5 <= 'F') {
                        level += (c5 - 'A') + 10;
                    }
                }
            }
            int i3 = i2 + 1;
            long out2 = out | ((level << 40) & BatteryStats.STEP_LEVEL_LEVEL_MASK);
            long duration = 0;
            while (i3 < N) {
                char c7 = value.charAt(i3);
                if (c7 != c) {
                    i3++;
                    duration <<= c3;
                    if (c7 >= '0' && c7 <= c2) {
                        duration += c7 - '0';
                        c = '-';
                        c2 = '9';
                        c3 = 4;
                    } else if (c7 >= 'a' && c7 <= 'f') {
                        duration += (c7 - 'a') + 10;
                        c = '-';
                        c2 = '9';
                        c3 = 4;
                    } else {
                        if (c7 >= 'A') {
                            c4 = 'F';
                            if (c7 <= 'F') {
                                duration += (c7 - 'A') + 10;
                                c = '-';
                                c2 = '9';
                                c3 = 4;
                            }
                        } else {
                            c4 = 'F';
                        }
                        c = '-';
                        c2 = '9';
                        c3 = 4;
                    }
                } else {
                    this.mStepDurations[index] = (BatteryStats.STEP_LEVEL_TIME_MASK & duration) | out2;
                }
            }
            this.mStepDurations[index] = (BatteryStats.STEP_LEVEL_TIME_MASK & duration) | out2;
        }

        public void init() {
            this.mLastStepTime = -1L;
            this.mNumStepDurations = 0;
        }

        public void clearTime() {
            this.mLastStepTime = -1L;
        }

        public long computeTimePerLevel() {
            long[] steps = this.mStepDurations;
            int numSteps = this.mNumStepDurations;
            if (numSteps <= 0) {
                return -1L;
            }
            long total = 0;
            for (int i = 0; i < numSteps; i++) {
                total += steps[i] & BatteryStats.STEP_LEVEL_TIME_MASK;
            }
            return total / numSteps;
        }

        public long computeTimeEstimate(long modesOfInterest, long modeValues, int[] outNumOfInterest) {
            long[] steps = this.mStepDurations;
            int count = this.mNumStepDurations;
            if (count <= 0) {
                return -1L;
            }
            long total = 0;
            int numOfInterest = 0;
            for (int i = 0; i < count; i++) {
                long initMode = (steps[i] & BatteryStats.STEP_LEVEL_INITIAL_MODE_MASK) >> 48;
                long modMode = (steps[i] & BatteryStats.STEP_LEVEL_MODIFIED_MODE_MASK) >> 56;
                if ((modMode & modesOfInterest) == 0 && (initMode & modesOfInterest) == modeValues) {
                    numOfInterest++;
                    total += steps[i] & BatteryStats.STEP_LEVEL_TIME_MASK;
                }
            }
            if (numOfInterest <= 0) {
                return -1L;
            }
            if (outNumOfInterest != null) {
                outNumOfInterest[0] = numOfInterest;
            }
            return (total / numOfInterest) * 100;
        }

        public void addLevelSteps(int numStepLevels, long modeBits, long elapsedRealtime) {
            int stepCount = this.mNumStepDurations;
            long lastStepTime = this.mLastStepTime;
            if (lastStepTime >= 0 && numStepLevels > 0) {
                long[] steps = this.mStepDurations;
                long duration = elapsedRealtime - lastStepTime;
                for (int i = 0; i < numStepLevels; i++) {
                    System.arraycopy(steps, 0, steps, 1, steps.length - 1);
                    long thisDuration = duration / (numStepLevels - i);
                    duration -= thisDuration;
                    if (thisDuration > BatteryStats.STEP_LEVEL_TIME_MASK) {
                        thisDuration = BatteryStats.STEP_LEVEL_TIME_MASK;
                    }
                    steps[0] = thisDuration | modeBits;
                }
                stepCount += numStepLevels;
                if (stepCount > steps.length) {
                    stepCount = steps.length;
                }
            }
            this.mNumStepDurations = stepCount;
            this.mLastStepTime = elapsedRealtime;
        }

        public void readFromParcel(Parcel in) {
            int N = in.readInt();
            if (N > this.mStepDurations.length) {
                throw new ParcelFormatException("more step durations than available: " + N);
            }
            this.mNumStepDurations = N;
            for (int i = 0; i < N; i++) {
                this.mStepDurations[i] = in.readLong();
            }
        }

        public void writeToParcel(Parcel out) {
            int N = this.mNumStepDurations;
            out.writeInt(N);
            for (int i = 0; i < N; i++) {
                out.writeLong(this.mStepDurations[i]);
            }
        }
    }

    /* loaded from: classes2.dex */
    public static final class HistoryTag {
        public int poolIdx;
        public String string;
        public int uid;

        public void setTo(HistoryTag o) {
            this.string = o.string;
            this.uid = o.uid;
            this.poolIdx = o.poolIdx;
        }

        public void setTo(String _string, int _uid) {
            this.string = _string;
            this.uid = _uid;
            this.poolIdx = -1;
        }

        public void writeToParcel(Parcel dest, int flags) {
            dest.writeString(this.string);
            dest.writeInt(this.uid);
        }

        public void readFromParcel(Parcel src) {
            this.string = src.readString();
            this.uid = src.readInt();
            this.poolIdx = -1;
        }

        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }
            HistoryTag that = (HistoryTag) o;
            if (this.uid == that.uid && this.string.equals(that.string)) {
                return true;
            }
            return false;
        }

        public int hashCode() {
            int result = this.string.hashCode();
            return (result * 31) + this.uid;
        }
    }

    /* loaded from: classes2.dex */
    public static final class HistoryStepDetails {
        public int appCpuSTime1;
        public int appCpuSTime2;
        public int appCpuSTime3;
        public int appCpuUTime1;
        public int appCpuUTime2;
        public int appCpuUTime3;
        public int appCpuUid1;
        public int appCpuUid2;
        public int appCpuUid3;
        public int statIOWaitTime;
        public int statIdlTime;
        public int statIrqTime;
        public int statSoftIrqTime;
        public String statSubsystemPowerState;
        public int statSystemTime;
        public int statUserTime;
        public int systemTime;
        public int userTime;

        public HistoryStepDetails() {
            clear();
        }

        public void clear() {
            this.systemTime = 0;
            this.userTime = 0;
            this.appCpuUid3 = -1;
            this.appCpuUid2 = -1;
            this.appCpuUid1 = -1;
            this.appCpuSTime3 = 0;
            this.appCpuUTime3 = 0;
            this.appCpuSTime2 = 0;
            this.appCpuUTime2 = 0;
            this.appCpuSTime1 = 0;
            this.appCpuUTime1 = 0;
        }

        public void writeToParcel(Parcel out) {
            out.writeInt(this.userTime);
            out.writeInt(this.systemTime);
            out.writeInt(this.appCpuUid1);
            out.writeInt(this.appCpuUTime1);
            out.writeInt(this.appCpuSTime1);
            out.writeInt(this.appCpuUid2);
            out.writeInt(this.appCpuUTime2);
            out.writeInt(this.appCpuSTime2);
            out.writeInt(this.appCpuUid3);
            out.writeInt(this.appCpuUTime3);
            out.writeInt(this.appCpuSTime3);
            out.writeInt(this.statUserTime);
            out.writeInt(this.statSystemTime);
            out.writeInt(this.statIOWaitTime);
            out.writeInt(this.statIrqTime);
            out.writeInt(this.statSoftIrqTime);
            out.writeInt(this.statIdlTime);
            out.writeString(this.statSubsystemPowerState);
        }

        public void readFromParcel(Parcel in) {
            this.userTime = in.readInt();
            this.systemTime = in.readInt();
            this.appCpuUid1 = in.readInt();
            this.appCpuUTime1 = in.readInt();
            this.appCpuSTime1 = in.readInt();
            this.appCpuUid2 = in.readInt();
            this.appCpuUTime2 = in.readInt();
            this.appCpuSTime2 = in.readInt();
            this.appCpuUid3 = in.readInt();
            this.appCpuUTime3 = in.readInt();
            this.appCpuSTime3 = in.readInt();
            this.statUserTime = in.readInt();
            this.statSystemTime = in.readInt();
            this.statIOWaitTime = in.readInt();
            this.statIrqTime = in.readInt();
            this.statSoftIrqTime = in.readInt();
            this.statIdlTime = in.readInt();
            this.statSubsystemPowerState = in.readString();
        }
    }

    /* loaded from: classes2.dex */
    public static final class HistoryItem {
        public static final byte CMD_CURRENT_TIME = 5;
        public static final byte CMD_NULL = -1;
        public static final byte CMD_OVERFLOW = 6;
        public static final byte CMD_RESET = 7;
        public static final byte CMD_SHUTDOWN = 8;
        public static final byte CMD_START = 4;
        public static final byte CMD_UPDATE = 0;
        public static final int EVENT_ACTIVE = 10;
        public static final int EVENT_ALARM = 13;
        public static final int EVENT_ALARM_FINISH = 16397;
        public static final int EVENT_ALARM_START = 32781;
        public static final int EVENT_COLLECT_EXTERNAL_STATS = 14;
        public static final int EVENT_CONNECTIVITY_CHANGED = 9;
        public static final int EVENT_COUNT = 22;
        public static final int EVENT_FLAG_FINISH = 16384;
        public static final int EVENT_FLAG_START = 32768;
        public static final int EVENT_FOREGROUND = 2;
        public static final int EVENT_FOREGROUND_FINISH = 16386;
        public static final int EVENT_FOREGROUND_START = 32770;
        public static final int EVENT_JOB = 6;
        public static final int EVENT_JOB_FINISH = 16390;
        public static final int EVENT_JOB_START = 32774;
        public static final int EVENT_LONG_WAKE_LOCK = 20;
        public static final int EVENT_LONG_WAKE_LOCK_FINISH = 16404;
        public static final int EVENT_LONG_WAKE_LOCK_START = 32788;
        public static final int EVENT_NONE = 0;
        public static final int EVENT_PACKAGE_ACTIVE = 16;
        public static final int EVENT_PACKAGE_INACTIVE = 15;
        public static final int EVENT_PACKAGE_INSTALLED = 11;
        public static final int EVENT_PACKAGE_UNINSTALLED = 12;
        public static final int EVENT_PROC = 1;
        public static final int EVENT_PROC_FINISH = 16385;
        public static final int EVENT_PROC_START = 32769;
        public static final int EVENT_SCREEN_WAKE_UP = 18;
        public static final int EVENT_SYNC = 4;
        public static final int EVENT_SYNC_FINISH = 16388;
        public static final int EVENT_SYNC_START = 32772;
        public static final int EVENT_TEMP_WHITELIST = 17;
        public static final int EVENT_TEMP_WHITELIST_FINISH = 16401;
        public static final int EVENT_TEMP_WHITELIST_START = 32785;
        public static final int EVENT_TOP = 3;
        public static final int EVENT_TOP_FINISH = 16387;
        public static final int EVENT_TOP_START = 32771;
        public static final int EVENT_TYPE_MASK = -49153;
        public static final int EVENT_USER_FOREGROUND = 8;
        public static final int EVENT_USER_FOREGROUND_FINISH = 16392;
        public static final int EVENT_USER_FOREGROUND_START = 32776;
        public static final int EVENT_USER_RUNNING = 7;
        public static final int EVENT_USER_RUNNING_FINISH = 16391;
        public static final int EVENT_USER_RUNNING_START = 32775;
        public static final int EVENT_WAKEUP_AP = 19;
        public static final int EVENT_WAKE_LOCK = 5;
        public static final int EVENT_WAKE_LOCK_FINISH = 16389;
        public static final int EVENT_WAKE_LOCK_START = 32773;
        public static final int MOST_INTERESTING_STATES = 1835008;
        public static final int MOST_INTERESTING_STATES2 = -1749024768;
        public static final int SETTLE_TO_ZERO_STATES = -1900544;
        public static final int SETTLE_TO_ZERO_STATES2 = 1748959232;
        public static final int STATE2_BLUETOOTH_ON_FLAG = 4194304;
        public static final int STATE2_BLUETOOTH_SCAN_FLAG = 1048576;
        public static final int STATE2_CAMERA_FLAG = 2097152;
        public static final int STATE2_CELLULAR_HIGH_TX_POWER_FLAG = 524288;
        public static final int STATE2_CHARGING_FLAG = 16777216;
        public static final int STATE2_DEVICE_IDLE_MASK = 100663296;
        public static final int STATE2_DEVICE_IDLE_SHIFT = 25;
        public static final int STATE2_FLASHLIGHT_FLAG = 134217728;
        public static final int STATE2_GPS_SIGNAL_QUALITY_MASK = 128;
        public static final int STATE2_GPS_SIGNAL_QUALITY_SHIFT = 7;
        public static final int STATE2_PHONE_IN_CALL_FLAG = 8388608;
        public static final int STATE2_POWER_SAVE_FLAG = Integer.MIN_VALUE;
        public static final int STATE2_USB_DATA_LINK_FLAG = 262144;
        public static final int STATE2_VIDEO_ON_FLAG = 1073741824;
        public static final int STATE2_WIFI_ON_FLAG = 268435456;
        public static final int STATE2_WIFI_RUNNING_FLAG = 536870912;
        public static final int STATE2_WIFI_SIGNAL_STRENGTH_MASK = 112;
        public static final int STATE2_WIFI_SIGNAL_STRENGTH_SHIFT = 4;
        public static final int STATE2_WIFI_SUPPL_STATE_MASK = 15;
        public static final int STATE2_WIFI_SUPPL_STATE_SHIFT = 0;
        public static final int STATE_AUDIO_ON_FLAG = 4194304;
        public static final int STATE_BATTERY_PLUGGED_FLAG = 524288;
        public static final int STATE_BRIGHTNESS_MASK = 7;
        public static final int STATE_BRIGHTNESS_SHIFT = 0;
        public static final int STATE_CPU_RUNNING_FLAG = Integer.MIN_VALUE;
        public static final int STATE_DATA_CONNECTION_MASK = 15872;
        public static final int STATE_DATA_CONNECTION_SHIFT = 9;
        public static final int STATE_GPS_ON_FLAG = 536870912;
        public static final int STATE_MOBILE_RADIO_ACTIVE_FLAG = 33554432;
        public static final int STATE_PHONE_SCANNING_FLAG = 2097152;
        public static final int STATE_PHONE_SIGNAL_STRENGTH_MASK = 56;
        public static final int STATE_PHONE_SIGNAL_STRENGTH_SHIFT = 3;
        public static final int STATE_PHONE_STATE_MASK = 448;
        public static final int STATE_PHONE_STATE_SHIFT = 6;
        private static final int STATE_RESERVED_0 = 16777216;
        public static final int STATE_SCREEN_DOZE_FLAG = 262144;
        public static final int STATE_SCREEN_ON_FLAG = 1048576;
        public static final int STATE_SENSOR_ON_FLAG = 8388608;
        public static final int STATE_WAKE_LOCK_FLAG = 1073741824;
        public static final int STATE_WIFI_FULL_LOCK_FLAG = 268435456;
        public static final int STATE_WIFI_MULTICAST_ON_FLAG = 65536;
        public static final int STATE_WIFI_RADIO_ACTIVE_FLAG = 67108864;
        public static final int STATE_WIFI_SCAN_FLAG = 134217728;
        public int batteryChargeUah;
        public byte batteryHealth;
        public byte batteryLevel;
        public byte batteryPlugType;
        public byte batteryStatus;
        public short batteryTemperature;
        public char batteryVoltage;
        public long currentTime;
        public int eventCode;
        public HistoryTag eventTag;
        public double modemRailChargeMah;
        public HistoryItem next;
        public int numReadInts;
        public int states;
        public int states2;
        public HistoryStepDetails stepDetails;
        public boolean tagsFirstOccurrence;
        public long time;
        public HistoryTag wakeReasonTag;
        public HistoryTag wakelockTag;
        public double wifiRailChargeMah;
        public byte cmd = -1;
        public final HistoryTag localWakelockTag = new HistoryTag();
        public final HistoryTag localWakeReasonTag = new HistoryTag();
        public final HistoryTag localEventTag = new HistoryTag();

        public boolean isDeltaData() {
            return this.cmd == 0;
        }

        public HistoryItem() {
        }

        public HistoryItem(Parcel src) {
            readFromParcel(src);
        }

        public void writeToParcel(Parcel dest, int flags) {
            dest.writeLong(this.time);
            int bat = (this.cmd & 255) | ((this.batteryLevel << 8) & 65280) | ((this.batteryStatus << 16) & SurfaceControl.FX_SURFACE_MASK) | ((this.batteryHealth << IGnssVisibilityControlCallback.NfwRequestor.AUTOMOBILE_CLIENT) & 15728640) | ((this.batteryPlugType << 24) & 251658240) | (this.wakelockTag != null ? 268435456 : 0) | (this.wakeReasonTag != null ? 536870912 : 0) | (this.eventCode != 0 ? 1073741824 : 0);
            dest.writeInt(bat);
            int bat2 = (this.batteryTemperature & 65535) | ((this.batteryVoltage << 16) & (-65536));
            dest.writeInt(bat2);
            dest.writeInt(this.batteryChargeUah);
            dest.writeDouble(this.modemRailChargeMah);
            dest.writeDouble(this.wifiRailChargeMah);
            dest.writeInt(this.states);
            dest.writeInt(this.states2);
            HistoryTag historyTag = this.wakelockTag;
            if (historyTag != null) {
                historyTag.writeToParcel(dest, flags);
            }
            HistoryTag historyTag2 = this.wakeReasonTag;
            if (historyTag2 != null) {
                historyTag2.writeToParcel(dest, flags);
            }
            int i = this.eventCode;
            if (i != 0) {
                dest.writeInt(i);
                this.eventTag.writeToParcel(dest, flags);
            }
            byte b = this.cmd;
            if (b == 5 || b == 7) {
                dest.writeLong(this.currentTime);
            }
        }

        public void readFromParcel(Parcel src) {
            int start = src.dataPosition();
            this.time = src.readLong();
            int bat = src.readInt();
            this.cmd = (byte) (bat & 255);
            this.batteryLevel = (byte) ((bat >> 8) & 255);
            this.batteryStatus = (byte) ((bat >> 16) & 15);
            this.batteryHealth = (byte) ((bat >> 20) & 15);
            this.batteryPlugType = (byte) ((bat >> 24) & 15);
            int bat2 = src.readInt();
            this.batteryTemperature = (short) (bat2 & 65535);
            this.batteryVoltage = (char) (65535 & (bat2 >> 16));
            this.batteryChargeUah = src.readInt();
            this.modemRailChargeMah = src.readDouble();
            this.wifiRailChargeMah = src.readDouble();
            this.states = src.readInt();
            this.states2 = src.readInt();
            if ((268435456 & bat) != 0) {
                HistoryTag historyTag = this.localWakelockTag;
                this.wakelockTag = historyTag;
                historyTag.readFromParcel(src);
            } else {
                this.wakelockTag = null;
            }
            if ((536870912 & bat) != 0) {
                HistoryTag historyTag2 = this.localWakeReasonTag;
                this.wakeReasonTag = historyTag2;
                historyTag2.readFromParcel(src);
            } else {
                this.wakeReasonTag = null;
            }
            if ((1073741824 & bat) != 0) {
                this.eventCode = src.readInt();
                HistoryTag historyTag3 = this.localEventTag;
                this.eventTag = historyTag3;
                historyTag3.readFromParcel(src);
            } else {
                this.eventCode = 0;
                this.eventTag = null;
            }
            byte b = this.cmd;
            if (b == 5 || b == 7) {
                this.currentTime = src.readLong();
            } else {
                this.currentTime = 0L;
            }
            this.numReadInts += (src.dataPosition() - start) / 4;
        }

        public void clear() {
            this.time = 0L;
            this.cmd = (byte) -1;
            this.batteryLevel = (byte) 0;
            this.batteryStatus = (byte) 0;
            this.batteryHealth = (byte) 0;
            this.batteryPlugType = (byte) 0;
            this.batteryTemperature = (short) 0;
            this.batteryVoltage = (char) 0;
            this.batteryChargeUah = 0;
            this.modemRailChargeMah = 0.0d;
            this.wifiRailChargeMah = 0.0d;
            this.states = 0;
            this.states2 = 0;
            this.wakelockTag = null;
            this.wakeReasonTag = null;
            this.eventCode = 0;
            this.eventTag = null;
            this.tagsFirstOccurrence = false;
        }

        public void setTo(HistoryItem o) {
            this.time = o.time;
            this.cmd = o.cmd;
            setToCommon(o);
        }

        public void setTo(long time, byte cmd, HistoryItem o) {
            this.time = time;
            this.cmd = cmd;
            setToCommon(o);
        }

        private void setToCommon(HistoryItem o) {
            this.batteryLevel = o.batteryLevel;
            this.batteryStatus = o.batteryStatus;
            this.batteryHealth = o.batteryHealth;
            this.batteryPlugType = o.batteryPlugType;
            this.batteryTemperature = o.batteryTemperature;
            this.batteryVoltage = o.batteryVoltage;
            this.batteryChargeUah = o.batteryChargeUah;
            this.modemRailChargeMah = o.modemRailChargeMah;
            this.wifiRailChargeMah = o.wifiRailChargeMah;
            this.states = o.states;
            this.states2 = o.states2;
            if (o.wakelockTag != null) {
                HistoryTag historyTag = this.localWakelockTag;
                this.wakelockTag = historyTag;
                historyTag.setTo(o.wakelockTag);
            } else {
                this.wakelockTag = null;
            }
            if (o.wakeReasonTag != null) {
                HistoryTag historyTag2 = this.localWakeReasonTag;
                this.wakeReasonTag = historyTag2;
                historyTag2.setTo(o.wakeReasonTag);
            } else {
                this.wakeReasonTag = null;
            }
            this.eventCode = o.eventCode;
            if (o.eventTag != null) {
                HistoryTag historyTag3 = this.localEventTag;
                this.eventTag = historyTag3;
                historyTag3.setTo(o.eventTag);
            } else {
                this.eventTag = null;
            }
            this.tagsFirstOccurrence = o.tagsFirstOccurrence;
            this.currentTime = o.currentTime;
        }

        public boolean sameNonEvent(HistoryItem o) {
            return this.batteryLevel == o.batteryLevel && this.batteryStatus == o.batteryStatus && this.batteryHealth == o.batteryHealth && this.batteryPlugType == o.batteryPlugType && this.batteryTemperature == o.batteryTemperature && this.batteryVoltage == o.batteryVoltage && this.batteryChargeUah == o.batteryChargeUah && this.modemRailChargeMah == o.modemRailChargeMah && this.wifiRailChargeMah == o.wifiRailChargeMah && this.states == o.states && this.states2 == o.states2 && this.currentTime == o.currentTime;
        }

        public boolean same(HistoryItem o) {
            if (sameNonEvent(o) && this.eventCode == o.eventCode) {
                HistoryTag historyTag = this.wakelockTag;
                HistoryTag historyTag2 = o.wakelockTag;
                if (historyTag == historyTag2 || !(historyTag == null || historyTag2 == null || !historyTag.equals(historyTag2))) {
                    HistoryTag historyTag3 = this.wakeReasonTag;
                    HistoryTag historyTag4 = o.wakeReasonTag;
                    if (historyTag3 == historyTag4 || !(historyTag3 == null || historyTag4 == null || !historyTag3.equals(historyTag4))) {
                        HistoryTag historyTag5 = this.eventTag;
                        HistoryTag historyTag6 = o.eventTag;
                        if (historyTag5 != historyTag6) {
                            return (historyTag5 == null || historyTag6 == null || !historyTag5.equals(historyTag6)) ? false : true;
                        }
                        return true;
                    }
                    return false;
                }
                return false;
            }
            return false;
        }
    }

    /* loaded from: classes2.dex */
    public static final class HistoryEventTracker {
        private final HashMap<String, SparseIntArray>[] mActiveEvents = new HashMap[22];

        public boolean updateState(int code, String name, int uid, int poolIdx) {
            SparseIntArray uids;
            int idx;
            if ((32768 & code) != 0) {
                int idx2 = code & HistoryItem.EVENT_TYPE_MASK;
                HashMap<String, SparseIntArray> active = this.mActiveEvents[idx2];
                if (active == null) {
                    active = new HashMap<>();
                    this.mActiveEvents[idx2] = active;
                }
                SparseIntArray uids2 = active.get(name);
                if (uids2 == null) {
                    uids2 = new SparseIntArray();
                    active.put(name, uids2);
                }
                if (uids2.indexOfKey(uid) >= 0) {
                    return false;
                }
                uids2.put(uid, poolIdx);
                return true;
            } else if ((code & 16384) != 0) {
                HashMap<String, SparseIntArray> active2 = this.mActiveEvents[code & HistoryItem.EVENT_TYPE_MASK];
                if (active2 == null || (uids = active2.get(name)) == null || (idx = uids.indexOfKey(uid)) < 0) {
                    return false;
                }
                uids.removeAt(idx);
                if (uids.size() <= 0) {
                    active2.remove(name);
                    return true;
                }
                return true;
            } else {
                return true;
            }
        }

        public void removeEvents(int code) {
            int idx = (-49153) & code;
            this.mActiveEvents[idx] = null;
        }

        public HashMap<String, SparseIntArray> getStateForEvent(int code) {
            return this.mActiveEvents[code];
        }
    }

    /* loaded from: classes2.dex */
    public static final class BitDescription {
        public final int mask;
        public final String name;
        public final int shift;
        public final String shortName;
        public final String[] shortValues;
        public final String[] values;

        public BitDescription(int mask, String name, String shortName) {
            this.mask = mask;
            this.shift = -1;
            this.name = name;
            this.shortName = shortName;
            this.values = null;
            this.shortValues = null;
        }

        public BitDescription(int mask, int shift, String name, String shortName, String[] values, String[] shortValues) {
            this.mask = mask;
            this.shift = shift;
            this.name = name;
            this.shortName = shortName;
            this.values = values;
            this.shortValues = shortValues;
        }
    }

    private static final void formatTimeRaw(StringBuilder out, long seconds) {
        long days = seconds / 86400;
        if (days != 0) {
            out.append(days);
            out.append("d ");
        }
        long used = days * 60 * 60 * 24;
        long hours = (seconds - used) / 3600;
        if (hours != 0 || used != 0) {
            out.append(hours);
            out.append("h ");
        }
        long used2 = used + (hours * 60 * 60);
        long mins = (seconds - used2) / 60;
        if (mins != 0 || used2 != 0) {
            out.append(mins);
            out.append("m ");
        }
        long used3 = used2 + (60 * mins);
        if (seconds != 0 || used3 != 0) {
            out.append(seconds - used3);
            out.append("s ");
        }
    }

    public static final void formatTimeMs(StringBuilder sb, long time) {
        long sec = time / 1000;
        formatTimeRaw(sb, sec);
        sb.append(time - (1000 * sec));
        sb.append("ms ");
    }

    public static final void formatTimeMsNoSpace(StringBuilder sb, long time) {
        long sec = time / 1000;
        formatTimeRaw(sb, sec);
        sb.append(time - (1000 * sec));
        sb.append("ms");
    }

    public final String formatRatioLocked(long num, long den) {
        if (den == 0) {
            return "--%";
        }
        float perc = (((float) num) / ((float) den)) * 100.0f;
        this.mFormatBuilder.setLength(0);
        this.mFormatter.format("%.1f%%", Float.valueOf(perc));
        return this.mFormatBuilder.toString();
    }

    final String formatBytesLocked(long bytes) {
        this.mFormatBuilder.setLength(0);
        if (bytes < 1024) {
            return bytes + GnssSignalType.CODE_TYPE_B;
        }
        if (bytes < 1048576) {
            this.mFormatter.format("%.2fKB", Double.valueOf(bytes / 1024.0d));
            return this.mFormatBuilder.toString();
        } else if (bytes < 1073741824) {
            this.mFormatter.format("%.2fMB", Double.valueOf(bytes / 1048576.0d));
            return this.mFormatBuilder.toString();
        } else {
            this.mFormatter.format("%.2fGB", Double.valueOf(bytes / 1.073741824E9d));
            return this.mFormatBuilder.toString();
        }
    }

    public static String formatCharge(double power) {
        return formatValue(power);
    }

    private static String formatValue(double value) {
        String format;
        if (value == 0.0d) {
            return AudioSystem.LEGACY_REMOTE_SUBMIX_ADDRESS;
        }
        if (value < 1.0E-5d) {
            format = "%.8f";
        } else if (value < 1.0E-4d) {
            format = "%.7f";
        } else if (value < 0.001d) {
            format = "%.6f";
        } else if (value < 0.01d) {
            format = "%.5f";
        } else if (value < 0.1d) {
            format = "%.4f";
        } else if (value < 1.0d) {
            format = "%.3f";
        } else if (value < 10.0d) {
            format = "%.2f";
        } else if (value < 100.0d) {
            format = "%.1f";
        } else {
            format = "%.0f";
        }
        return String.format(Locale.ENGLISH, format, Double.valueOf(value));
    }

    private static long roundUsToMs(long timeUs) {
        return (500 + timeUs) / 1000;
    }

    private static long computeWakeLock(Timer timer, long elapsedRealtimeUs, int which) {
        if (timer != null) {
            long totalTimeMicros = timer.getTotalTimeLocked(elapsedRealtimeUs, which);
            long totalTimeMillis = (500 + totalTimeMicros) / 1000;
            return totalTimeMillis;
        }
        return 0L;
    }

    private static final String printWakeLock(StringBuilder sb, Timer timer, long elapsedRealtimeUs, String name, int which, String linePrefix) {
        if (timer != null) {
            long totalTimeMillis = computeWakeLock(timer, elapsedRealtimeUs, which);
            int count = timer.getCountLocked(which);
            if (totalTimeMillis != 0) {
                sb.append(linePrefix);
                formatTimeMs(sb, totalTimeMillis);
                if (name != null) {
                    sb.append(name);
                    sb.append(' ');
                }
                sb.append('(');
                sb.append(count);
                sb.append(" times)");
                long maxDurationMs = timer.getMaxDurationMsLocked(elapsedRealtimeUs / 1000);
                if (maxDurationMs >= 0) {
                    sb.append(" max=");
                    sb.append(maxDurationMs);
                }
                long totalDurMs = timer.getTotalDurationMsLocked(elapsedRealtimeUs / 1000);
                if (totalDurMs > totalTimeMillis) {
                    sb.append(" actual=");
                    sb.append(totalDurMs);
                }
                if (timer.isRunningLocked()) {
                    long currentMs = timer.getCurrentDurationMsLocked(elapsedRealtimeUs / 1000);
                    if (currentMs >= 0) {
                        sb.append(" (running for ");
                        sb.append(currentMs);
                        sb.append("ms)");
                        return ", ";
                    }
                    sb.append(" (running)");
                    return ", ";
                }
                return ", ";
            }
        }
        return linePrefix;
    }

    private static final boolean printTimer(PrintWriter pw, StringBuilder sb, Timer timer, long rawRealtimeUs, int which, String prefix, String type) {
        if (timer != null) {
            long totalTimeMs = (timer.getTotalTimeLocked(rawRealtimeUs, which) + 500) / 1000;
            int count = timer.getCountLocked(which);
            if (totalTimeMs != 0) {
                sb.setLength(0);
                sb.append(prefix);
                sb.append("    ");
                sb.append(type);
                sb.append(": ");
                formatTimeMs(sb, totalTimeMs);
                sb.append("realtime (");
                sb.append(count);
                sb.append(" times)");
                long maxDurationMs = timer.getMaxDurationMsLocked(rawRealtimeUs / 1000);
                if (maxDurationMs >= 0) {
                    sb.append(" max=");
                    sb.append(maxDurationMs);
                }
                if (timer.isRunningLocked()) {
                    long currentMs = timer.getCurrentDurationMsLocked(rawRealtimeUs / 1000);
                    if (currentMs >= 0) {
                        sb.append(" (running for ");
                        sb.append(currentMs);
                        sb.append("ms)");
                    } else {
                        sb.append(" (running)");
                    }
                }
                pw.println(sb.toString());
                return true;
            }
        }
        return false;
    }

    private static final String printWakeLockCheckin(StringBuilder sb, Timer timer, long elapsedRealtimeUs, String name, int which, String linePrefix) {
        long totalTimeMicros = 0;
        int count = 0;
        long max = 0;
        long current = 0;
        long totalDuration = 0;
        if (timer != null) {
            long totalTimeMicros2 = timer.getTotalTimeLocked(elapsedRealtimeUs, which);
            count = timer.getCountLocked(which);
            current = timer.getCurrentDurationMsLocked(elapsedRealtimeUs / 1000);
            max = timer.getMaxDurationMsLocked(elapsedRealtimeUs / 1000);
            totalDuration = timer.getTotalDurationMsLocked(elapsedRealtimeUs / 1000);
            totalTimeMicros = totalTimeMicros2;
        }
        sb.append(linePrefix);
        sb.append((totalTimeMicros + 500) / 1000);
        sb.append(',');
        sb.append(name != null ? name + "," : "");
        sb.append(count);
        sb.append(',');
        sb.append(current);
        sb.append(',');
        sb.append(max);
        if (name != null) {
            sb.append(',');
            sb.append(totalDuration);
        }
        return ",";
    }

    private static final void dumpLineHeader(PrintWriter pw, int uid, String category, String type) {
        pw.print(9);
        pw.print(',');
        pw.print(uid);
        pw.print(',');
        pw.print(category);
        pw.print(',');
        pw.print(type);
    }

    private static final void dumpLine(PrintWriter pw, int uid, String category, String type, Object... args) {
        dumpLineHeader(pw, uid, category, type);
        for (Object arg : args) {
            pw.print(',');
            pw.print(arg);
        }
        pw.println();
    }

    private static final void dumpTimer(PrintWriter pw, int uid, String category, String type, Timer timer, long rawRealtime, int which) {
        if (timer != null) {
            long totalTime = roundUsToMs(timer.getTotalTimeLocked(rawRealtime, which));
            int count = timer.getCountLocked(which);
            if (totalTime != 0 || count != 0) {
                dumpLine(pw, uid, category, type, Long.valueOf(totalTime), Integer.valueOf(count));
            }
        }
    }

    private static void dumpTimer(ProtoOutputStream proto, long fieldId, Timer timer, long rawRealtimeUs, int which) {
        if (timer == null) {
            return;
        }
        long timeMs = roundUsToMs(timer.getTotalTimeLocked(rawRealtimeUs, which));
        int count = timer.getCountLocked(which);
        long maxDurationMs = timer.getMaxDurationMsLocked(rawRealtimeUs / 1000);
        long curDurationMs = timer.getCurrentDurationMsLocked(rawRealtimeUs / 1000);
        long totalDurationMs = timer.getTotalDurationMsLocked(rawRealtimeUs / 1000);
        if (timeMs != 0 || count != 0 || maxDurationMs != -1 || curDurationMs != -1 || totalDurationMs != -1) {
            long token = proto.start(fieldId);
            proto.write(1112396529665L, timeMs);
            proto.write(1112396529666L, count);
            if (maxDurationMs != -1) {
                proto.write(1112396529667L, maxDurationMs);
            }
            if (curDurationMs != -1) {
                proto.write(1112396529668L, curDurationMs);
            }
            if (totalDurationMs != -1) {
                proto.write(1112396529669L, totalDurationMs);
            }
            proto.end(token);
        }
    }

    private static boolean controllerActivityHasData(ControllerActivityCounter counter, int which) {
        LongCounter[] txTimeCounters;
        if (counter == null) {
            return false;
        }
        if (counter.getIdleTimeCounter().getCountLocked(which) == 0 && counter.getRxTimeCounter().getCountLocked(which) == 0 && counter.getPowerCounter().getCountLocked(which) == 0 && counter.getMonitoredRailChargeConsumedMaMs().getCountLocked(which) == 0) {
            for (LongCounter c : counter.getTxTimeCounters()) {
                if (c.getCountLocked(which) != 0) {
                    return true;
                }
            }
            return false;
        }
        return true;
    }

    private static final void dumpControllerActivityLine(PrintWriter pw, int uid, String category, String type, ControllerActivityCounter counter, int which) {
        LongCounter[] txTimeCounters;
        if (!controllerActivityHasData(counter, which)) {
            return;
        }
        dumpLineHeader(pw, uid, category, type);
        pw.print(",");
        pw.print(counter.getIdleTimeCounter().getCountLocked(which));
        pw.print(",");
        pw.print(counter.getRxTimeCounter().getCountLocked(which));
        pw.print(",");
        pw.print(counter.getPowerCounter().getCountLocked(which) / 3600000.0d);
        pw.print(",");
        pw.print(counter.getMonitoredRailChargeConsumedMaMs().getCountLocked(which) / 3600000.0d);
        for (LongCounter c : counter.getTxTimeCounters()) {
            pw.print(",");
            pw.print(c.getCountLocked(which));
        }
        pw.println();
    }

    private static void dumpControllerActivityProto(ProtoOutputStream proto, long fieldId, ControllerActivityCounter counter, int which) {
        if (!controllerActivityHasData(counter, which)) {
            return;
        }
        long cToken = proto.start(fieldId);
        proto.write(1112396529665L, counter.getIdleTimeCounter().getCountLocked(which));
        proto.write(1112396529666L, counter.getRxTimeCounter().getCountLocked(which));
        proto.write(1112396529667L, counter.getPowerCounter().getCountLocked(which) / 3600000.0d);
        proto.write(1103806595077L, counter.getMonitoredRailChargeConsumedMaMs().getCountLocked(which) / 3600000.0d);
        LongCounter[] txCounters = counter.getTxTimeCounters();
        for (int i = 0; i < txCounters.length; i++) {
            LongCounter c = txCounters[i];
            long tToken = proto.start(2246267895812L);
            proto.write(1120986464257L, i);
            proto.write(1112396529666L, c.getCountLocked(which));
            proto.end(tToken);
        }
        proto.end(cToken);
    }

    private final void printControllerActivityIfInteresting(PrintWriter pw, StringBuilder sb, String prefix, String controllerName, ControllerActivityCounter counter, int which) {
        if (controllerActivityHasData(counter, which)) {
            printControllerActivity(pw, sb, prefix, controllerName, counter, which);
        }
    }

    private final void printControllerActivity(PrintWriter pw, StringBuilder sb, String prefix, String controllerName, ControllerActivityCounter counter, int which) {
        long rxTimeMs;
        String str;
        int i;
        Object obj;
        String[] powerLevel;
        String[] powerLevel2;
        long powerDrainMaMs;
        long idleTimeMs = counter.getIdleTimeCounter().getCountLocked(which);
        long rxTimeMs2 = counter.getRxTimeCounter().getCountLocked(which);
        long powerDrainMaMs2 = counter.getPowerCounter().getCountLocked(which);
        long monitoredRailChargeConsumedMaMs = counter.getMonitoredRailChargeConsumedMaMs().getCountLocked(which);
        long totalControllerActivityTimeMs = computeBatteryRealtime(SystemClock.elapsedRealtime() * 1000, which) / 1000;
        long totalTxTimeMs = 0;
        LongCounter[] txTimeCounters = counter.getTxTimeCounters();
        int length = txTimeCounters.length;
        int i2 = 0;
        while (i2 < length) {
            int i3 = length;
            LongCounter txState = txTimeCounters[i2];
            totalTxTimeMs += txState.getCountLocked(which);
            i2++;
            length = i3;
        }
        if (!controllerName.equals(WIFI_CONTROLLER_NAME)) {
            rxTimeMs = rxTimeMs2;
            str = " Sleep time:  ";
        } else {
            long scanTimeMs = counter.getScanTimeCounter().getCountLocked(which);
            sb.setLength(0);
            sb.append(prefix);
            sb.append("     ");
            sb.append(controllerName);
            sb.append(" Scan time:  ");
            formatTimeMs(sb, scanTimeMs);
            sb.append(NavigationBarInflaterView.KEY_CODE_START);
            sb.append(formatRatioLocked(scanTimeMs, totalControllerActivityTimeMs));
            sb.append(NavigationBarInflaterView.KEY_CODE_END);
            pw.println(sb.toString());
            long scanTimeMs2 = totalControllerActivityTimeMs - ((idleTimeMs + rxTimeMs2) + totalTxTimeMs);
            sb.setLength(0);
            sb.append(prefix);
            sb.append("     ");
            sb.append(controllerName);
            str = " Sleep time:  ";
            sb.append(str);
            formatTimeMs(sb, scanTimeMs2);
            sb.append(NavigationBarInflaterView.KEY_CODE_START);
            rxTimeMs = rxTimeMs2;
            sb.append(formatRatioLocked(scanTimeMs2, totalControllerActivityTimeMs));
            sb.append(NavigationBarInflaterView.KEY_CODE_END);
            pw.println(sb.toString());
        }
        if (!controllerName.equals(CELLULAR_CONTROLLER_NAME)) {
            i = which;
            obj = CELLULAR_CONTROLLER_NAME;
        } else {
            i = which;
            long sleepTimeMs = counter.getSleepTimeCounter().getCountLocked(i);
            obj = CELLULAR_CONTROLLER_NAME;
            sb.setLength(0);
            sb.append(prefix);
            sb.append("     ");
            sb.append(controllerName);
            sb.append(str);
            formatTimeMs(sb, sleepTimeMs);
            sb.append(NavigationBarInflaterView.KEY_CODE_START);
            sb.append(formatRatioLocked(sleepTimeMs, totalControllerActivityTimeMs));
            sb.append(NavigationBarInflaterView.KEY_CODE_END);
            pw.println(sb.toString());
        }
        sb.setLength(0);
        sb.append(prefix);
        sb.append("     ");
        sb.append(controllerName);
        sb.append(" Idle time:   ");
        formatTimeMs(sb, idleTimeMs);
        sb.append(NavigationBarInflaterView.KEY_CODE_START);
        sb.append(formatRatioLocked(idleTimeMs, totalControllerActivityTimeMs));
        sb.append(NavigationBarInflaterView.KEY_CODE_END);
        pw.println(sb.toString());
        sb.setLength(0);
        sb.append(prefix);
        sb.append("     ");
        sb.append(controllerName);
        sb.append(" Rx time:     ");
        long rxTimeMs3 = rxTimeMs;
        formatTimeMs(sb, rxTimeMs3);
        sb.append(NavigationBarInflaterView.KEY_CODE_START);
        sb.append(formatRatioLocked(rxTimeMs3, totalControllerActivityTimeMs));
        sb.append(NavigationBarInflaterView.KEY_CODE_END);
        pw.println(sb.toString());
        sb.setLength(0);
        sb.append(prefix);
        sb.append("     ");
        sb.append(controllerName);
        sb.append(" Tx time:     ");
        char c = 65535;
        switch (controllerName.hashCode()) {
            case -851952246:
                if (controllerName.equals(obj)) {
                    c = 0;
                    break;
                }
                break;
        }
        switch (c) {
            case 0:
                powerLevel = new String[]{"   less than 0dBm: ", "   0dBm to 8dBm: ", "   8dBm to 15dBm: ", "   15dBm to 20dBm: ", "   above 20dBm: "};
                break;
            default:
                powerLevel = new String[]{"[0]", "[1]", "[2]", "[3]", "[4]"};
                break;
        }
        int numTxLvls = Math.min(counter.getTxTimeCounters().length, powerLevel.length);
        if (numTxLvls > 1) {
            pw.println(sb.toString());
            for (int lvl = 0; lvl < numTxLvls; lvl++) {
                long txLvlTimeMs = counter.getTxTimeCounters()[lvl].getCountLocked(i);
                sb.setLength(0);
                sb.append(prefix);
                sb.append("    ");
                sb.append(powerLevel[lvl]);
                sb.append(" ");
                formatTimeMs(sb, txLvlTimeMs);
                sb.append(NavigationBarInflaterView.KEY_CODE_START);
                sb.append(formatRatioLocked(txLvlTimeMs, totalControllerActivityTimeMs));
                sb.append(NavigationBarInflaterView.KEY_CODE_END);
                pw.println(sb.toString());
            }
        } else {
            long txLvlTimeMs2 = counter.getTxTimeCounters()[0].getCountLocked(i);
            formatTimeMs(sb, txLvlTimeMs2);
            sb.append(NavigationBarInflaterView.KEY_CODE_START);
            sb.append(formatRatioLocked(txLvlTimeMs2, totalControllerActivityTimeMs));
            sb.append(NavigationBarInflaterView.KEY_CODE_END);
            pw.println(sb.toString());
        }
        if (powerDrainMaMs2 <= 0) {
            powerLevel2 = powerLevel;
            powerDrainMaMs = powerDrainMaMs2;
        } else {
            sb.setLength(0);
            sb.append(prefix);
            sb.append("     ");
            sb.append(controllerName);
            powerLevel2 = powerLevel;
            powerDrainMaMs = powerDrainMaMs2;
            sb.append(" Battery drain: ").append(formatCharge(powerDrainMaMs / 3600000.0d));
            sb.append("mAh");
            pw.println(sb.toString());
        }
        if (monitoredRailChargeConsumedMaMs > 0) {
            sb.setLength(0);
            sb.append(prefix);
            sb.append("     ");
            sb.append(controllerName);
            sb.append(" Monitored rail energy drain: ").append(new DecimalFormat("#.##").format(monitoredRailChargeConsumedMaMs / 3600000.0d));
            sb.append(" mAh");
            pw.println(sb.toString());
        }
    }

    private void printCellularPerRatBreakdown(PrintWriter pw, StringBuilder sb, String prefix, long rawRealtimeMs) {
        String[] nrFrequencyRangeDescription;
        String signalStrengthHeader;
        long j = rawRealtimeMs;
        String allFrequenciesHeader = "    All frequencies:\n";
        String[] nrFrequencyRangeDescription2 = {"    Unknown frequency:\n", "    Low frequency (less than 1GHz):\n", "    Middle frequency (1GHz to 3GHz):\n", "    High frequency (3GHz to 6GHz):\n", "    Mmwave frequency (greater than 6GHz):\n"};
        String signalStrengthHeader2 = "      Signal Strength Time:\n";
        String txHeader = "      Tx Time:\n";
        String rxHeader = "      Rx Time: ";
        String[] signalStrengthDescription = {"        unknown:  ", "        poor:     ", "        moderate: ", "        good:     ", "        great:    "};
        int rat = 0;
        long totalActiveTimesMs = getMobileRadioActiveTime(j * 1000, 0) / 1000;
        sb.setLength(0);
        sb.append(prefix);
        sb.append("Active Cellular Radio Access Technology Breakdown:");
        pw.println(sb);
        boolean hasData = false;
        int numSignalStrength = CellSignalStrength.getNumSignalStrengthLevels();
        int rat2 = 2;
        while (rat2 >= 0) {
            sb.setLength(rat);
            sb.append(prefix);
            sb.append("  ");
            sb.append(RADIO_ACCESS_TECHNOLOGY_NAMES[rat2]);
            sb.append(":\n");
            sb.append(prefix);
            int numFreqLvl = rat2 == 2 ? nrFrequencyRangeDescription2.length : 1;
            int freqLvl = numFreqLvl - 1;
            boolean hasData2 = hasData;
            while (freqLvl >= 0) {
                int freqDescriptionStart = sb.length();
                boolean hasFreqData = false;
                String allFrequenciesHeader2 = allFrequenciesHeader;
                if (rat2 == 2) {
                    sb.append(nrFrequencyRangeDescription2[freqLvl]);
                } else {
                    sb.append("    All frequencies:\n");
                }
                sb.append(prefix);
                sb.append("      Signal Strength Time:\n");
                int strength = 0;
                while (true) {
                    nrFrequencyRangeDescription = nrFrequencyRangeDescription2;
                    signalStrengthHeader = signalStrengthHeader2;
                    if (strength >= numSignalStrength) {
                        break;
                    }
                    String txHeader2 = txHeader;
                    int freqDescriptionStart2 = freqDescriptionStart;
                    int rat3 = rat2;
                    String rxHeader2 = rxHeader;
                    long totalActiveTimesMs2 = totalActiveTimesMs;
                    int freqLvl2 = freqLvl;
                    int numSignalStrength2 = numSignalStrength;
                    long timeMs = getActiveRadioDurationMs(rat2, freqLvl, strength, rawRealtimeMs);
                    if (timeMs > 0) {
                        sb.append(prefix);
                        sb.append(signalStrengthDescription[strength]);
                        formatTimeMs(sb, timeMs);
                        sb.append(NavigationBarInflaterView.KEY_CODE_START);
                        sb.append(formatRatioLocked(timeMs, totalActiveTimesMs2));
                        sb.append(")\n");
                        hasFreqData = true;
                    }
                    strength++;
                    numSignalStrength = numSignalStrength2;
                    freqLvl = freqLvl2;
                    totalActiveTimesMs = totalActiveTimesMs2;
                    nrFrequencyRangeDescription2 = nrFrequencyRangeDescription;
                    signalStrengthHeader2 = signalStrengthHeader;
                    txHeader = txHeader2;
                    rat2 = rat3;
                    rxHeader = rxHeader2;
                    freqDescriptionStart = freqDescriptionStart2;
                }
                int freqDescriptionStart3 = freqDescriptionStart;
                int rat4 = rat2;
                int freqLvl3 = freqLvl;
                int numSignalStrength3 = numSignalStrength;
                String txHeader3 = txHeader;
                String rxHeader3 = rxHeader;
                long totalActiveTimesMs3 = totalActiveTimesMs;
                sb.append(prefix);
                sb.append("      Tx Time:\n");
                for (int strength2 = 0; strength2 < numSignalStrength3; strength2++) {
                    long timeMs2 = getActiveTxRadioDurationMs(rat4, freqLvl3, strength2, rawRealtimeMs);
                    if (timeMs2 > 0) {
                        sb.append(prefix);
                        sb.append(signalStrengthDescription[strength2]);
                        formatTimeMs(sb, timeMs2);
                        sb.append(NavigationBarInflaterView.KEY_CODE_START);
                        sb.append(formatRatioLocked(timeMs2, totalActiveTimesMs3));
                        sb.append(")\n");
                        hasFreqData = true;
                    }
                }
                sb.append(prefix);
                sb.append("      Rx Time: ");
                long rxTimeMs = getActiveRxRadioDurationMs(rat4, freqLvl3, rawRealtimeMs);
                formatTimeMs(sb, rxTimeMs);
                sb.append(NavigationBarInflaterView.KEY_CODE_START);
                sb.append(formatRatioLocked(rxTimeMs, totalActiveTimesMs3));
                sb.append(")\n");
                if (hasFreqData) {
                    hasData2 = true;
                    pw.print(sb);
                    sb.setLength(0);
                    sb.append(prefix);
                } else {
                    sb.setLength(freqDescriptionStart3);
                }
                int freqDescriptionStart4 = freqLvl3 - 1;
                j = rawRealtimeMs;
                numSignalStrength = numSignalStrength3;
                rat2 = rat4;
                totalActiveTimesMs = totalActiveTimesMs3;
                allFrequenciesHeader = allFrequenciesHeader2;
                nrFrequencyRangeDescription2 = nrFrequencyRangeDescription;
                signalStrengthHeader2 = signalStrengthHeader;
                txHeader = txHeader3;
                rxHeader = rxHeader3;
                freqLvl = freqDescriptionStart4;
            }
            int freqLvl4 = rat2;
            int i = freqLvl4 - 1;
            j = j;
            rat = 0;
            rat2 = i;
            numSignalStrength = numSignalStrength;
            totalActiveTimesMs = totalActiveTimesMs;
            hasData = hasData2;
            txHeader = txHeader;
            rxHeader = rxHeader;
        }
        int numSignalStrength4 = rat;
        if (!hasData) {
            sb.setLength(numSignalStrength4);
            sb.append(prefix);
            sb.append("  (no activity)");
            pw.println(sb);
        }
    }

    public final void dumpCheckinLocked(Context context, PrintWriter pw, int which, int reqUid) {
        dumpCheckinLocked(context, pw, which, reqUid, checkWifiOnly(context));
    }

    /*  JADX ERROR: IndexOutOfBoundsException in pass: SSATransform
        java.lang.IndexOutOfBoundsException: bitIndex < 0: -80
        	at java.base/java.util.BitSet.get(BitSet.java:624)
        	at jadx.core.dex.visitors.ssa.LiveVarAnalysis.fillBasicBlockInfo(LiveVarAnalysis.java:65)
        	at jadx.core.dex.visitors.ssa.LiveVarAnalysis.runAnalysis(LiveVarAnalysis.java:36)
        	at jadx.core.dex.visitors.ssa.SSATransform.process(SSATransform.java:55)
        	at jadx.core.dex.visitors.ssa.SSATransform.visit(SSATransform.java:41)
        */
    public final void dumpCheckinLocked(android.content.Context r211, java.io.PrintWriter r212, int r213, int r214, boolean r215) {
        /*
            r210 = this;
            r0 = r210
            r9 = r212
            r10 = r213
            r11 = r214
            r12 = 1
            r13 = 0
            java.lang.Integer r14 = java.lang.Integer.valueOf(r13)
            if (r10 == 0) goto L37
            java.lang.String[] r1 = android.os.BatteryStats.STAT_NAMES
            r1 = r1[r10]
            java.lang.Object[] r2 = new java.lang.Object[r12]
            java.lang.StringBuilder r3 = new java.lang.StringBuilder
            r3.<init>()
            java.lang.String r4 = "ERROR: BatteryStats.dumpCheckin called for which type "
            java.lang.StringBuilder r3 = r3.append(r4)
            java.lang.StringBuilder r3 = r3.append(r10)
            java.lang.String r4 = " but only STATS_SINCE_CHARGED is supported."
            java.lang.StringBuilder r3 = r3.append(r4)
            java.lang.String r3 = r3.toString()
            r2[r13] = r3
            java.lang.String r3 = "err"
            dumpLine(r9, r13, r1, r3, r2)
            return
        L37:
            long r1 = android.os.SystemClock.uptimeMillis()
            r15 = 1000(0x3e8, double:4.94E-321)
            long r7 = r1 * r15
            long r5 = android.os.SystemClock.elapsedRealtime()
            long r3 = r5 * r15
            long r1 = r0.getBatteryUptime(r7)
            long r17 = r0.computeBatteryUptime(r7, r10)
            long r19 = r0.computeBatteryRealtime(r3, r10)
            long r21 = r0.computeBatteryScreenOffUptime(r7, r10)
            long r23 = r0.computeBatteryScreenOffRealtime(r3, r10)
            long r25 = r0.computeRealtime(r3, r10)
            long r27 = r0.computeUptime(r7, r10)
            long r29 = r0.getScreenOnTime(r3, r10)
            long r31 = r0.getScreenDozeTime(r3, r10)
            long r33 = r0.getInteractiveTime(r3, r10)
            long r35 = r0.getPowerSaveModeEnabledTime(r3, r10)
            long r37 = r0.getDeviceIdleModeTime(r12, r3, r10)
            r39 = r1
            r2 = 2
            long r41 = r0.getDeviceIdleModeTime(r2, r3, r10)
            long r43 = r0.getDeviceIdlingTime(r12, r3, r10)
            long r45 = r0.getDeviceIdlingTime(r2, r3, r10)
            int r47 = r0.getNumConnectivityChange(r10)
            long r48 = r0.getPhoneOnTime(r3, r10)
            long r50 = r0.getUahDischarge(r10)
            long r52 = r0.getUahDischargeScreenOff(r10)
            long r54 = r0.getUahDischargeScreenDoze(r10)
            long r56 = r0.getUahDischargeLightDoze(r10)
            long r58 = r0.getUahDischargeDeepDoze(r10)
            java.lang.StringBuilder r1 = new java.lang.StringBuilder
            r2 = 128(0x80, float:1.794E-43)
            r1.<init>(r2)
            r2 = r1
            android.util.SparseArray r1 = r210.getUidStats()
            r61 = r2
            int r2 = r1.size()
            java.lang.String[] r62 = android.os.BatteryStats.STAT_NAMES
            r12 = r62[r10]
            r15 = 12
            java.lang.Object[] r13 = new java.lang.Object[r15]
            if (r10 != 0) goto Lc5
            int r62 = r210.getStartCount()
            java.lang.Integer r62 = java.lang.Integer.valueOf(r62)
            goto Lc7
        Lc5:
            java.lang.String r62 = "N/A"
        Lc7:
            r16 = 0
            r13[r16] = r62
            r64 = 1000(0x3e8, double:4.94E-321)
            long r66 = r19 / r64
            java.lang.Long r62 = java.lang.Long.valueOf(r66)
            r63 = 1
            r13[r63] = r62
            long r66 = r17 / r64
            java.lang.Long r62 = java.lang.Long.valueOf(r66)
            r60 = 2
            r13[r60] = r62
            long r66 = r25 / r64
            java.lang.Long r62 = java.lang.Long.valueOf(r66)
            r15 = 3
            r13[r15] = r62
            long r67 = r27 / r64
            java.lang.Long r62 = java.lang.Long.valueOf(r67)
            r15 = 4
            r13[r15] = r62
            long r68 = r210.getStartClockTime()
            java.lang.Long r62 = java.lang.Long.valueOf(r68)
            r15 = 5
            r13[r15] = r62
            long r69 = r23 / r64
            java.lang.Long r62 = java.lang.Long.valueOf(r69)
            r15 = 6
            r13[r15] = r62
            long r70 = r21 / r64
            java.lang.Long r62 = java.lang.Long.valueOf(r70)
            r15 = 7
            r13[r15] = r62
            int r62 = r210.getEstimatedBatteryCapacity()
            java.lang.Integer r62 = java.lang.Integer.valueOf(r62)
            r15 = 8
            r13[r15] = r62
            int r62 = r210.getMinLearnedBatteryCapacity()
            java.lang.Integer r62 = java.lang.Integer.valueOf(r62)
            r15 = 9
            r13[r15] = r62
            int r62 = r210.getMaxLearnedBatteryCapacity()
            java.lang.Integer r62 = java.lang.Integer.valueOf(r62)
            r15 = 10
            r13[r15] = r62
            r64 = 1000(0x3e8, double:4.94E-321)
            long r74 = r31 / r64
            java.lang.Long r62 = java.lang.Long.valueOf(r74)
            r15 = 11
            r13[r15] = r62
            java.lang.String r15 = "bt"
            r75 = r5
            r5 = 0
            dumpLine(r9, r5, r12, r15, r13)
            r5 = 0
            r77 = 0
            r13 = 0
            r79 = r77
            r77 = r5
        L151:
            if (r13 >= r2) goto L1a0
            java.lang.Object r5 = r1.valueAt(r13)
            android.os.BatteryStats$Uid r5 = (android.os.BatteryStats.Uid) r5
            android.util.ArrayMap r6 = r5.getWakelockStats()
            int r15 = r6.size()
            r81 = r1
            r1 = 1
            int r15 = r15 - r1
        L166:
            if (r15 < 0) goto L197
            java.lang.Object r63 = r6.valueAt(r15)
            r82 = r2
            r2 = r63
            android.os.BatteryStats$Uid$Wakelock r2 = (android.os.BatteryStats.Uid.Wakelock) r2
            r83 = r5
            android.os.BatteryStats$Timer r5 = r2.getWakeTime(r1)
            if (r5 == 0) goto L180
            long r84 = r5.getTotalTimeLocked(r3, r10)
            long r77 = r77 + r84
        L180:
            r84 = r5
            r1 = 0
            android.os.BatteryStats$Timer r5 = r2.getWakeTime(r1)
            if (r5 == 0) goto L18f
            long r85 = r5.getTotalTimeLocked(r3, r10)
            long r79 = r79 + r85
        L18f:
            int r15 = r15 + (-1)
            r2 = r82
            r5 = r83
            r1 = 1
            goto L166
        L197:
            r82 = r2
            r83 = r5
            int r13 = r13 + 1
            r1 = r81
            goto L151
        L1a0:
            r81 = r1
            r82 = r2
            r1 = 0
            long r83 = r0.getNetworkActivityBytes(r1, r10)
            r2 = 1
            long r85 = r0.getNetworkActivityBytes(r2, r10)
            r5 = 2
            long r87 = r0.getNetworkActivityBytes(r5, r10)
            r6 = 3
            long r89 = r0.getNetworkActivityBytes(r6, r10)
            long r91 = r0.getNetworkActivityPackets(r1, r10)
            long r93 = r0.getNetworkActivityPackets(r2, r10)
            long r95 = r0.getNetworkActivityPackets(r5, r10)
            long r97 = r0.getNetworkActivityPackets(r6, r10)
            r1 = 4
            long r99 = r0.getNetworkActivityBytes(r1, r10)
            r1 = 5
            long r101 = r0.getNetworkActivityBytes(r1, r10)
            r1 = 10
            java.lang.Object[] r2 = new java.lang.Object[r1]
            java.lang.Long r1 = java.lang.Long.valueOf(r83)
            r5 = 0
            r2[r5] = r1
            java.lang.Long r1 = java.lang.Long.valueOf(r85)
            r5 = 1
            r2[r5] = r1
            java.lang.Long r1 = java.lang.Long.valueOf(r87)
            r5 = 2
            r2[r5] = r1
            java.lang.Long r1 = java.lang.Long.valueOf(r89)
            r6 = 3
            r2[r6] = r1
            java.lang.Long r1 = java.lang.Long.valueOf(r91)
            r6 = 4
            r2[r6] = r1
            java.lang.Long r1 = java.lang.Long.valueOf(r93)
            r6 = 5
            r2[r6] = r1
            java.lang.Long r1 = java.lang.Long.valueOf(r95)
            r6 = 6
            r2[r6] = r1
            java.lang.Long r1 = java.lang.Long.valueOf(r97)
            r6 = 7
            r2[r6] = r1
            java.lang.Long r1 = java.lang.Long.valueOf(r99)
            r6 = 8
            r2[r6] = r1
            java.lang.Long r1 = java.lang.Long.valueOf(r101)
            r6 = 9
            r2[r6] = r1
            java.lang.String r1 = "gn"
            r6 = 0
            dumpLine(r9, r6, r12, r1, r2)
            r2 = 0
            android.os.BatteryStats$ControllerActivityCounter r6 = r210.getModemControllerActivity()
            java.lang.String r13 = "gmcd"
            r103 = r39
            r15 = r81
            r1 = r212
            r39 = r7
            r8 = r82
            r7 = r5
            r5 = r61
            r7 = r3
            r3 = r12
            r4 = r13
            r13 = r5
            r105 = r75
            r5 = r6
            r6 = r213
            dumpControllerActivityLine(r1, r2, r3, r4, r5, r6)
            long r75 = r0.getWifiOnTime(r7, r10)
            long r107 = r0.getGlobalWifiRunningTime(r7, r10)
            r1 = 5
            java.lang.Object[] r2 = new java.lang.Object[r1]
            r3 = 1000(0x3e8, double:4.94E-321)
            long r5 = r75 / r3
            java.lang.Long r1 = java.lang.Long.valueOf(r5)
            r5 = 0
            r2[r5] = r1
            long r109 = r107 / r3
            java.lang.Long r1 = java.lang.Long.valueOf(r109)
            r3 = 1
            r2[r3] = r1
            r1 = 2
            r2[r1] = r14
            r1 = 3
            r2[r1] = r14
            r1 = 4
            r2[r1] = r14
            java.lang.String r1 = "gwfl"
            dumpLine(r9, r5, r12, r1, r2)
            r2 = 0
            android.os.BatteryStats$ControllerActivityCounter r5 = r210.getWifiControllerActivity()
            java.lang.String r4 = "gwfcd"
            r1 = r212
            r3 = r12
            r6 = r213
            dumpControllerActivityLine(r1, r2, r3, r4, r5, r6)
            android.os.BatteryStats$ControllerActivityCounter r5 = r210.getBluetoothControllerActivity()
            java.lang.String r4 = "gble"
            dumpControllerActivityLine(r1, r2, r3, r4, r5, r6)
            r1 = 21
            java.lang.Object[] r1 = new java.lang.Object[r1]
            r2 = 1000(0x3e8, double:4.94E-321)
            long r4 = r29 / r2
            java.lang.Long r4 = java.lang.Long.valueOf(r4)
            r5 = 0
            r1[r5] = r4
            long r4 = r48 / r2
            java.lang.Long r4 = java.lang.Long.valueOf(r4)
            r5 = 1
            r1[r5] = r4
            long r4 = r77 / r2
            java.lang.Long r4 = java.lang.Long.valueOf(r4)
            r5 = 2
            r1[r5] = r4
            long r4 = r79 / r2
            java.lang.Long r4 = java.lang.Long.valueOf(r4)
            r5 = 3
            r1[r5] = r4
            long r4 = r0.getMobileRadioActiveTime(r7, r10)
            long r4 = r4 / r2
            java.lang.Long r4 = java.lang.Long.valueOf(r4)
            r5 = 4
            r1[r5] = r4
            long r4 = r0.getMobileRadioActiveAdjustedTime(r10)
            long r4 = r4 / r2
            java.lang.Long r4 = java.lang.Long.valueOf(r4)
            r5 = 5
            r1[r5] = r4
            long r4 = r33 / r2
            java.lang.Long r4 = java.lang.Long.valueOf(r4)
            r5 = 6
            r1[r5] = r4
            long r4 = r35 / r2
            java.lang.Long r4 = java.lang.Long.valueOf(r4)
            r5 = 7
            r1[r5] = r4
            java.lang.Integer r4 = java.lang.Integer.valueOf(r47)
            r5 = 8
            r1[r5] = r4
            long r4 = r41 / r2
            java.lang.Long r4 = java.lang.Long.valueOf(r4)
            r5 = 9
            r1[r5] = r4
            r4 = 2
            int r5 = r0.getDeviceIdleModeCount(r4, r10)
            java.lang.Integer r5 = java.lang.Integer.valueOf(r5)
            r6 = 10
            r1[r6] = r5
            long r5 = r45 / r2
            java.lang.Long r2 = java.lang.Long.valueOf(r5)
            r3 = 11
            r1[r3] = r2
            int r2 = r0.getDeviceIdlingCount(r4, r10)
            java.lang.Integer r2 = java.lang.Integer.valueOf(r2)
            r3 = 12
            r1[r3] = r2
            int r2 = r0.getMobileRadioActiveCount(r10)
            java.lang.Integer r2 = java.lang.Integer.valueOf(r2)
            r6 = 13
            r1[r6] = r2
            r2 = 14
            long r3 = r0.getMobileRadioActiveUnknownTime(r10)
            r64 = 1000(0x3e8, double:4.94E-321)
            long r3 = r3 / r64
            java.lang.Long r3 = java.lang.Long.valueOf(r3)
            r1[r2] = r3
            r2 = 15
            long r3 = r37 / r64
            java.lang.Long r3 = java.lang.Long.valueOf(r3)
            r1[r2] = r3
            r2 = 16
            r3 = 1
            int r4 = r0.getDeviceIdleModeCount(r3, r10)
            java.lang.Integer r4 = java.lang.Integer.valueOf(r4)
            r1[r2] = r4
            r2 = 17
            long r4 = r43 / r64
            java.lang.Long r4 = java.lang.Long.valueOf(r4)
            r1[r2] = r4
            int r2 = r0.getDeviceIdlingCount(r3, r10)
            java.lang.Integer r2 = java.lang.Integer.valueOf(r2)
            r5 = 18
            r1[r5] = r2
            r2 = 19
            long r109 = r0.getLongestDeviceIdleModeTime(r3)
            java.lang.Long r3 = java.lang.Long.valueOf(r109)
            r1[r2] = r3
            r2 = 20
            r3 = 2
            long r109 = r0.getLongestDeviceIdleModeTime(r3)
            java.lang.Long r3 = java.lang.Long.valueOf(r109)
            r1[r2] = r3
            java.lang.String r2 = "m"
            r3 = 0
            dumpLine(r9, r3, r12, r2, r1)
            r1 = 5
            java.lang.Object[] r2 = new java.lang.Object[r1]
            r3 = 0
        L37f:
            if (r3 >= r1) goto L393
            long r109 = r0.getScreenBrightnessTime(r3, r7, r10)
            r64 = 1000(0x3e8, double:4.94E-321)
            long r109 = r109 / r64
            java.lang.Long r1 = java.lang.Long.valueOf(r109)
            r2[r3] = r1
            int r3 = r3 + 1
            r1 = 5
            goto L37f
        L393:
            java.lang.String r1 = "br"
            r3 = 0
            dumpLine(r9, r3, r12, r1, r2)
            int r1 = android.telephony.CellSignalStrength.getNumSignalStrengthLevels()
            java.lang.Object[] r1 = new java.lang.Object[r1]
            r2 = 0
        L3a0:
            int r3 = android.telephony.CellSignalStrength.getNumSignalStrengthLevels()
            if (r2 >= r3) goto L3b7
            long r3 = r0.getPhoneSignalStrengthTime(r2, r7, r10)
            r64 = 1000(0x3e8, double:4.94E-321)
            long r3 = r3 / r64
            java.lang.Long r3 = java.lang.Long.valueOf(r3)
            r1[r2] = r3
            int r2 = r2 + 1
            goto L3a0
        L3b7:
            java.lang.String r2 = "sgt"
            r3 = 0
            dumpLine(r9, r3, r12, r2, r1)
            r2 = 1
            java.lang.Object[] r4 = new java.lang.Object[r2]
            long r109 = r0.getPhoneSignalScanningTime(r7, r10)
            r64 = 1000(0x3e8, double:4.94E-321)
            long r109 = r109 / r64
            java.lang.Long r2 = java.lang.Long.valueOf(r109)
            r4[r3] = r2
            java.lang.String r2 = "sst"
            dumpLine(r9, r3, r12, r2, r4)
            r2 = 0
        L3d6:
            int r3 = android.telephony.CellSignalStrength.getNumSignalStrengthLevels()
            if (r2 >= r3) goto L3e9
            int r3 = r0.getPhoneSignalStrengthCount(r2, r10)
            java.lang.Integer r3 = java.lang.Integer.valueOf(r3)
            r1[r2] = r3
            int r2 = r2 + 1
            goto L3d6
        L3e9:
            java.lang.String r2 = "sgc"
            r3 = 0
            dumpLine(r9, r3, r12, r2, r1)
            int r2 = android.os.BatteryStats.NUM_DATA_CONNECTION_TYPES
            java.lang.Object[] r1 = new java.lang.Object[r2]
            r2 = 0
        L3f5:
            int r3 = android.os.BatteryStats.NUM_DATA_CONNECTION_TYPES
            if (r2 >= r3) goto L40a
            long r3 = r0.getPhoneDataConnectionTime(r2, r7, r10)
            r64 = 1000(0x3e8, double:4.94E-321)
            long r3 = r3 / r64
            java.lang.Long r3 = java.lang.Long.valueOf(r3)
            r1[r2] = r3
            int r2 = r2 + 1
            goto L3f5
        L40a:
            java.lang.String r2 = "dct"
            r3 = 0
            dumpLine(r9, r3, r12, r2, r1)
            r2 = 0
        L411:
            int r3 = android.os.BatteryStats.NUM_DATA_CONNECTION_TYPES
            if (r2 >= r3) goto L422
            int r3 = r0.getPhoneDataConnectionCount(r2, r10)
            java.lang.Integer r3 = java.lang.Integer.valueOf(r3)
            r1[r2] = r3
            int r2 = r2 + 1
            goto L411
        L422:
            java.lang.String r2 = "dcc"
            r3 = 0
            dumpLine(r9, r3, r12, r2, r1)
            r2 = 8
            java.lang.Object[] r1 = new java.lang.Object[r2]
            r3 = 0
        L42d:
            if (r3 >= r2) goto L442
            long r109 = r0.getWifiStateTime(r3, r7, r10)
            r64 = 1000(0x3e8, double:4.94E-321)
            long r109 = r109 / r64
            java.lang.Long r2 = java.lang.Long.valueOf(r109)
            r1[r3] = r2
            int r3 = r3 + 1
            r2 = 8
            goto L42d
        L442:
            java.lang.String r2 = "wst"
            r3 = 0
            dumpLine(r9, r3, r12, r2, r1)
            r2 = 0
        L44a:
            r3 = 8
            if (r2 >= r3) goto L45b
            int r3 = r0.getWifiStateCount(r2, r10)
            java.lang.Integer r3 = java.lang.Integer.valueOf(r3)
            r1[r2] = r3
            int r2 = r2 + 1
            goto L44a
        L45b:
            java.lang.String r2 = "wsc"
            r3 = 0
            dumpLine(r9, r3, r12, r2, r1)
            java.lang.Object[] r1 = new java.lang.Object[r6]
            r2 = 0
        L465:
            if (r2 >= r6) goto L478
            long r3 = r0.getWifiSupplStateTime(r2, r7, r10)
            r64 = 1000(0x3e8, double:4.94E-321)
            long r3 = r3 / r64
            java.lang.Long r3 = java.lang.Long.valueOf(r3)
            r1[r2] = r3
            int r2 = r2 + 1
            goto L465
        L478:
            java.lang.String r2 = "wsst"
            r3 = 0
            dumpLine(r9, r3, r12, r2, r1)
            r2 = 0
        L480:
            if (r2 >= r6) goto L48f
            int r3 = r0.getWifiSupplStateCount(r2, r10)
            java.lang.Integer r3 = java.lang.Integer.valueOf(r3)
            r1[r2] = r3
            int r2 = r2 + 1
            goto L480
        L48f:
            java.lang.String r2 = "wssc"
            r3 = 0
            dumpLine(r9, r3, r12, r2, r1)
            r2 = 5
            java.lang.Object[] r3 = new java.lang.Object[r2]
            r1 = 0
        L49a:
            if (r1 >= r2) goto L4ae
            long r109 = r0.getWifiSignalStrengthTime(r1, r7, r10)
            r64 = 1000(0x3e8, double:4.94E-321)
            long r109 = r109 / r64
            java.lang.Long r2 = java.lang.Long.valueOf(r109)
            r3[r1] = r2
            int r1 = r1 + 1
            r2 = 5
            goto L49a
        L4ae:
            java.lang.String r1 = "wsgt"
            r2 = 0
            dumpLine(r9, r2, r12, r1, r3)
            r1 = 0
        L4b6:
            r2 = 5
            if (r1 >= r2) goto L4c6
            int r2 = r0.getWifiSignalStrengthCount(r1, r10)
            java.lang.Integer r2 = java.lang.Integer.valueOf(r2)
            r3[r1] = r2
            int r1 = r1 + 1
            goto L4b6
        L4c6:
            java.lang.String r1 = "wsgc"
            r2 = 0
            dumpLine(r9, r2, r12, r1, r3)
            long r109 = r0.getWifiMulticastWakelockTime(r7, r10)
            int r61 = r0.getWifiMulticastWakelockCount(r10)
            r1 = 2
            java.lang.Object[] r4 = new java.lang.Object[r1]
            r64 = 1000(0x3e8, double:4.94E-321)
            long r111 = r109 / r64
            java.lang.Long r1 = java.lang.Long.valueOf(r111)
            r4[r2] = r1
            java.lang.Integer r1 = java.lang.Integer.valueOf(r61)
            r16 = 1
            r4[r16] = r1
            java.lang.String r1 = "wmct"
            dumpLine(r9, r2, r12, r1, r4)
            r1 = 10
            java.lang.Object[] r4 = new java.lang.Object[r1]
            int r1 = r210.getLowDischargeAmountSinceCharge()
            java.lang.Integer r1 = java.lang.Integer.valueOf(r1)
            r4[r2] = r1
            int r1 = r210.getHighDischargeAmountSinceCharge()
            java.lang.Integer r1 = java.lang.Integer.valueOf(r1)
            r2 = 1
            r4[r2] = r1
            int r1 = r210.getDischargeAmountScreenOnSinceCharge()
            java.lang.Integer r1 = java.lang.Integer.valueOf(r1)
            r60 = 2
            r4[r60] = r1
            int r1 = r210.getDischargeAmountScreenOffSinceCharge()
            java.lang.Integer r1 = java.lang.Integer.valueOf(r1)
            r2 = 3
            r4[r2] = r1
            r1 = 1000(0x3e8, double:4.94E-321)
            long r64 = r50 / r1
            java.lang.Long r64 = java.lang.Long.valueOf(r64)
            r65 = 4
            r4[r65] = r64
            long r64 = r52 / r1
            java.lang.Long r64 = java.lang.Long.valueOf(r64)
            r65 = 5
            r4[r65] = r64
            int r64 = r210.getDischargeAmountScreenDozeSinceCharge()
            java.lang.Integer r64 = java.lang.Integer.valueOf(r64)
            r65 = 6
            r4[r65] = r64
            long r64 = r54 / r1
            java.lang.Long r64 = java.lang.Long.valueOf(r64)
            r65 = 7
            r4[r65] = r64
            long r64 = r56 / r1
            java.lang.Long r64 = java.lang.Long.valueOf(r64)
            r65 = 8
            r4[r65] = r64
            long r111 = r58 / r1
            java.lang.Long r1 = java.lang.Long.valueOf(r111)
            r2 = 9
            r4[r2] = r1
            java.lang.String r1 = "dc"
            r2 = 0
            dumpLine(r9, r2, r12, r1, r4)
            r111 = 500(0x1f4, double:2.47E-321)
            java.lang.String r4 = "\""
            if (r11 >= 0) goto L68c
            java.util.Map r81 = r210.getKernelWakelockStats()
            int r1 = r81.size()
            if (r1 <= 0) goto L5f6
            java.util.Set r1 = r81.entrySet()
            java.util.Iterator r113 = r1.iterator()
        L57f:
            boolean r1 = r113.hasNext()
            if (r1 == 0) goto L5ec
            java.lang.Object r1 = r113.next()
            r114 = r1
            java.util.Map$Entry r114 = (java.util.Map.Entry) r114
            r1 = 0
            r13.setLength(r1)
            java.lang.Object r1 = r114.getValue()
            r2 = r1
            android.os.BatteryStats$Timer r2 = (android.os.BatteryStats.Timer) r2
            r115 = 0
            java.lang.String r116 = ""
            r1 = r13
            r117 = r3
            r118 = r14
            r14 = r4
            r3 = r7
            r119 = r7
            r8 = r5
            r5 = r115
            r7 = r6
            r6 = r213
            r8 = r60
            r121 = r119
            r7 = r116
            printWakeLockCheckin(r1, r2, r3, r5, r6, r7)
            java.lang.Object[] r1 = new java.lang.Object[r8]
            java.lang.StringBuilder r2 = new java.lang.StringBuilder
            r2.<init>()
            java.lang.StringBuilder r2 = r2.append(r14)
            java.lang.Object r3 = r114.getKey()
            java.lang.String r3 = (java.lang.String) r3
            java.lang.StringBuilder r2 = r2.append(r3)
            java.lang.StringBuilder r2 = r2.append(r14)
            java.lang.String r2 = r2.toString()
            r3 = 0
            r1[r3] = r2
            java.lang.String r2 = r13.toString()
            r4 = 1
            r1[r4] = r2
            java.lang.String r2 = "kwl"
            dumpLine(r9, r3, r12, r2, r1)
            r4 = r14
            r3 = r117
            r14 = r118
            r7 = r121
            r5 = 18
            r6 = 13
            goto L57f
        L5ec:
            r117 = r3
            r121 = r7
            r118 = r14
            r8 = r60
            r14 = r4
            goto L5ff
        L5f6:
            r117 = r3
            r121 = r7
            r118 = r14
            r8 = r60
            r14 = r4
        L5ff:
            java.util.Map r1 = r210.getWakeupReasonStats()
            int r2 = r1.size()
            if (r2 <= 0) goto L687
            java.util.Set r2 = r1.entrySet()
            java.util.Iterator r2 = r2.iterator()
        L611:
            boolean r3 = r2.hasNext()
            if (r3 == 0) goto L682
            java.lang.Object r3 = r2.next()
            java.util.Map$Entry r3 = (java.util.Map.Entry) r3
            java.lang.Object r4 = r3.getValue()
            android.os.BatteryStats$Timer r4 = (android.os.BatteryStats.Timer) r4
            r6 = r121
            long r4 = r4.getTotalTimeLocked(r6, r10)
            java.lang.Object r113 = r3.getValue()
            r8 = r113
            android.os.BatteryStats$Timer r8 = (android.os.BatteryStats.Timer) r8
            int r8 = r8.getCountLocked(r10)
            r113 = r1
            r115 = r2
            r1 = 3
            java.lang.Object[] r2 = new java.lang.Object[r1]
            java.lang.StringBuilder r1 = new java.lang.StringBuilder
            r1.<init>()
            java.lang.StringBuilder r1 = r1.append(r14)
            java.lang.Object r116 = r3.getKey()
            r119 = r3
            r3 = r116
            java.lang.String r3 = (java.lang.String) r3
            java.lang.StringBuilder r1 = r1.append(r3)
            java.lang.StringBuilder r1 = r1.append(r14)
            java.lang.String r1 = r1.toString()
            r3 = 0
            r2[r3] = r1
            long r120 = r4 + r111
            r64 = 1000(0x3e8, double:4.94E-321)
            long r120 = r120 / r64
            java.lang.Long r1 = java.lang.Long.valueOf(r120)
            r16 = 1
            r2[r16] = r1
            java.lang.Integer r1 = java.lang.Integer.valueOf(r8)
            r16 = 2
            r2[r16] = r1
            java.lang.String r1 = "wr"
            dumpLine(r9, r3, r12, r1, r2)
            r121 = r6
            r1 = r113
            r2 = r115
            r8 = 2
            goto L611
        L682:
            r113 = r1
            r6 = r121
            goto L692
        L687:
            r113 = r1
            r6 = r121
            goto L692
        L68c:
            r117 = r3
            r6 = r7
            r118 = r14
            r14 = r4
        L692:
            java.util.Map r81 = r210.getRpmStats()
            java.util.Map r8 = r210.getScreenOffRpmStats()
            int r1 = r81.size()
            r115 = 0
            if (r1 <= 0) goto L734
            java.util.Set r1 = r81.entrySet()
            java.util.Iterator r1 = r1.iterator()
        L6aa:
            boolean r2 = r1.hasNext()
            if (r2 == 0) goto L734
            java.lang.Object r2 = r1.next()
            java.util.Map$Entry r2 = (java.util.Map.Entry) r2
            r3 = 0
            r13.setLength(r3)
            java.lang.Object r3 = r2.getValue()
            android.os.BatteryStats$Timer r3 = (android.os.BatteryStats.Timer) r3
            long r4 = r3.getTotalTimeLocked(r6, r10)
            long r4 = r4 + r111
            r64 = 1000(0x3e8, double:4.94E-321)
            long r4 = r4 / r64
            int r113 = r3.getCountLocked(r10)
            r119 = r1
            java.lang.Object r1 = r2.getKey()
            java.lang.Object r1 = r8.get(r1)
            android.os.BatteryStats$Timer r1 = (android.os.BatteryStats.Timer) r1
            if (r1 == 0) goto L6e7
            long r120 = r1.getTotalTimeLocked(r6, r10)
            long r120 = r120 + r111
            r64 = 1000(0x3e8, double:4.94E-321)
            long r120 = r120 / r64
            goto L6e9
        L6e7:
            r120 = r115
        L6e9:
            if (r1 == 0) goto L6f0
            int r122 = r1.getCountLocked(r10)
            goto L6f2
        L6f0:
            r122 = 0
        L6f2:
            r124 = r1
            r125 = r3
            r1 = 3
            java.lang.Object[] r3 = new java.lang.Object[r1]
            java.lang.StringBuilder r1 = new java.lang.StringBuilder
            r1.<init>()
            java.lang.StringBuilder r1 = r1.append(r14)
            java.lang.Object r126 = r2.getKey()
            r127 = r2
            r2 = r126
            java.lang.String r2 = (java.lang.String) r2
            java.lang.StringBuilder r1 = r1.append(r2)
            java.lang.StringBuilder r1 = r1.append(r14)
            java.lang.String r1 = r1.toString()
            r2 = 0
            r3[r2] = r1
            java.lang.Long r1 = java.lang.Long.valueOf(r4)
            r16 = 1
            r3[r16] = r1
            java.lang.Integer r1 = java.lang.Integer.valueOf(r113)
            r16 = 2
            r3[r16] = r1
            java.lang.String r1 = "rpm"
            dumpLine(r9, r2, r12, r1, r3)
            r1 = r119
            goto L6aa
        L734:
            android.os.BatteryUsageStats r5 = r210.getBatteryUsageStats(r211)
            r1 = 4
            java.lang.Object[] r2 = new java.lang.Object[r1]
            double r3 = r5.getBatteryCapacity()
            java.lang.String r1 = formatCharge(r3)
            r3 = 0
            r2[r3] = r1
            double r3 = r5.getConsumedPower()
            java.lang.String r1 = formatCharge(r3)
            r3 = 1
            r2[r3] = r1
            android.util.Range r1 = r5.getDischargedPowerRange()
            java.lang.Comparable r1 = r1.getLower()
            java.lang.Double r1 = (java.lang.Double) r1
            double r3 = r1.doubleValue()
            java.lang.String r1 = formatCharge(r3)
            r3 = 2
            r2[r3] = r1
            android.util.Range r1 = r5.getDischargedPowerRange()
            java.lang.Comparable r1 = r1.getUpper()
            java.lang.Double r1 = (java.lang.Double) r1
            double r3 = r1.doubleValue()
            java.lang.String r1 = formatCharge(r3)
            r3 = 3
            r2[r3] = r1
            java.lang.String r1 = "pws"
            r3 = 0
            dumpLine(r9, r3, r12, r1, r2)
            android.os.BatteryConsumer r4 = r5.getAggregateBatteryConsumer(r3)
            r1 = 0
        L786:
            r2 = 18
            if (r1 >= r2) goto L7c5
            java.lang.String[] r2 = android.os.BatteryStats.CHECKIN_POWER_COMPONENT_LABELS
            r2 = r2[r1]
            if (r2 != 0) goto L792
            java.lang.String r2 = "???"
        L792:
            r121 = r6
            r3 = 5
            java.lang.Object[] r6 = new java.lang.Object[r3]
            r3 = 0
            r6[r3] = r2
            double r119 = r4.getConsumedPower(r1)
            java.lang.String r3 = formatCharge(r119)
            r7 = 1
            r6[r7] = r3
            boolean r3 = r0.shouldHidePowerComponent(r1)
            java.lang.Integer r3 = java.lang.Integer.valueOf(r3)
            r7 = 2
            r6[r7] = r3
            java.lang.String r3 = "0"
            r7 = 3
            r6[r7] = r3
            java.lang.String r3 = "0"
            r7 = 4
            r6[r7] = r3
            java.lang.String r3 = "pwi"
            r7 = 0
            dumpLine(r9, r7, r12, r3, r6)
            int r1 = r1 + 1
            r6 = r121
            goto L786
        L7c5:
            r121 = r6
            android.os.BatteryStats$ProportionalAttributionCalculator r1 = new android.os.BatteryStats$ProportionalAttributionCalculator
            r7 = r211
            r1.<init>(r7, r5)
            r6 = r1
            java.util.List r3 = r5.getUidBatteryConsumers()
            r1 = 0
        L7d4:
            int r2 = r3.size()
            if (r1 >= r2) goto L831
            java.lang.Object r2 = r3.get(r1)
            android.os.UidBatteryConsumer r2 = (android.os.UidBatteryConsumer) r2
            int r0 = r2.getUid()
            r113 = r3
            r119 = r4
            r3 = 5
            java.lang.Object[] r4 = new java.lang.Object[r3]
            java.lang.String r3 = "uid"
            r16 = 0
            r4[r16] = r3
            double r124 = r2.getConsumedPower()
            java.lang.String r3 = formatCharge(r124)
            r63 = 1
            r4[r63] = r3
            boolean r3 = r6.isSystemBatteryConsumer(r2)
            java.lang.Integer r3 = java.lang.Integer.valueOf(r3)
            r114 = 2
            r4[r114] = r3
            r3 = 0
            double r124 = r2.getConsumedPower(r3)
            java.lang.String r3 = formatCharge(r124)
            r67 = 3
            r4[r67] = r3
            double r124 = r6.getProportionalPowerMah(r2)
            java.lang.String r3 = formatCharge(r124)
            r68 = 4
            r4[r68] = r3
            java.lang.String r3 = "pwi"
            dumpLine(r9, r0, r12, r3, r4)
            int r1 = r1 + 1
            r0 = r210
            r3 = r113
            r4 = r119
            goto L7d4
        L831:
            r113 = r3
            r119 = r4
            long[] r0 = r210.getCpuFreqs()
            r4 = 44
            if (r0 == 0) goto L861
            r1 = 0
            r13.setLength(r1)
            r1 = 0
        L842:
            int r2 = r0.length
            if (r1 >= r2) goto L852
            if (r1 == 0) goto L84a
            r13.append(r4)
        L84a:
            r2 = r0[r1]
            r13.append(r2)
            int r1 = r1 + 1
            goto L842
        L852:
            r1 = 1
            java.lang.Object[] r2 = new java.lang.Object[r1]
            java.lang.String r1 = r13.toString()
            r3 = 0
            r2[r3] = r1
            java.lang.String r1 = "gcf"
            dumpLine(r9, r3, r12, r1, r2)
        L861:
            r1 = 0
            r3 = r1
        L863:
            r2 = r82
            if (r3 >= r2) goto L13bc
            int r1 = r15.keyAt(r3)
            if (r11 < 0) goto L8a4
            if (r1 == r11) goto L8a4
            r60 = r2
            r169 = r3
            r170 = r5
            r114 = r8
            r206 = r13
            r202 = r14
            r167 = r15
            r14 = r103
            r196 = r105
            r82 = r119
            r200 = r121
            r16 = 0
            r63 = 1
            r64 = 1000(0x3e8, double:4.94E-321)
            r66 = 12
            r67 = 3
            r68 = 4
            r69 = 5
            r72 = 8
            r73 = 9
            r183 = 18
            r188 = 13
            r189 = 10
            r204 = 2
            r8 = r0
            r121 = r6
            goto L139b
        L8a4:
            java.lang.Object r82 = r15.valueAt(r3)
            r11 = r82
            android.os.BatteryStats$Uid r11 = (android.os.BatteryStats.Uid) r11
            r4 = 0
            long r124 = r11.getNetworkActivityBytes(r4, r10)
            r4 = 1
            long r126 = r11.getNetworkActivityBytes(r4, r10)
            r4 = 2
            long r128 = r11.getNetworkActivityBytes(r4, r10)
            r4 = 3
            long r130 = r11.getNetworkActivityBytes(r4, r10)
            r4 = 0
            long r132 = r11.getNetworkActivityPackets(r4, r10)
            r4 = 1
            long r134 = r11.getNetworkActivityPackets(r4, r10)
            long r136 = r11.getMobileRadioActiveTime(r10)
            int r120 = r11.getMobileRadioActiveCount(r10)
            long r138 = r11.getMobileRadioApWakeupCount(r10)
            r4 = 2
            long r140 = r11.getNetworkActivityPackets(r4, r10)
            r4 = 3
            long r142 = r11.getNetworkActivityPackets(r4, r10)
            long r144 = r11.getWifiRadioApWakeupCount(r10)
            r4 = 4
            long r146 = r11.getNetworkActivityBytes(r4, r10)
            r4 = 5
            long r148 = r11.getNetworkActivityBytes(r4, r10)
            r4 = 6
            long r150 = r11.getNetworkActivityBytes(r4, r10)
            r4 = 7
            long r152 = r11.getNetworkActivityBytes(r4, r10)
            r4 = 8
            long r154 = r11.getNetworkActivityBytes(r4, r10)
            r4 = 9
            long r156 = r11.getNetworkActivityBytes(r4, r10)
            r4 = 6
            long r158 = r11.getNetworkActivityPackets(r4, r10)
            r4 = 7
            long r160 = r11.getNetworkActivityPackets(r4, r10)
            r4 = 8
            long r162 = r11.getNetworkActivityPackets(r4, r10)
            r4 = 9
            long r164 = r11.getNetworkActivityPackets(r4, r10)
            int r4 = (r124 > r115 ? 1 : (r124 == r115 ? 0 : -1))
            if (r4 > 0) goto L979
            int r4 = (r126 > r115 ? 1 : (r126 == r115 ? 0 : -1))
            if (r4 > 0) goto L979
            int r4 = (r128 > r115 ? 1 : (r128 == r115 ? 0 : -1))
            if (r4 > 0) goto L979
            int r4 = (r130 > r115 ? 1 : (r130 == r115 ? 0 : -1))
            if (r4 > 0) goto L979
            int r4 = (r132 > r115 ? 1 : (r132 == r115 ? 0 : -1))
            if (r4 > 0) goto L979
            int r4 = (r134 > r115 ? 1 : (r134 == r115 ? 0 : -1))
            if (r4 > 0) goto L979
            int r4 = (r140 > r115 ? 1 : (r140 == r115 ? 0 : -1))
            if (r4 > 0) goto L979
            int r4 = (r142 > r115 ? 1 : (r142 == r115 ? 0 : -1))
            if (r4 > 0) goto L979
            int r4 = (r136 > r115 ? 1 : (r136 == r115 ? 0 : -1))
            if (r4 > 0) goto L979
            if (r120 > 0) goto L979
            int r4 = (r146 > r115 ? 1 : (r146 == r115 ? 0 : -1))
            if (r4 > 0) goto L979
            int r4 = (r148 > r115 ? 1 : (r148 == r115 ? 0 : -1))
            if (r4 > 0) goto L979
            int r4 = (r138 > r115 ? 1 : (r138 == r115 ? 0 : -1))
            if (r4 > 0) goto L979
            int r4 = (r144 > r115 ? 1 : (r144 == r115 ? 0 : -1))
            if (r4 > 0) goto L979
            int r4 = (r150 > r115 ? 1 : (r150 == r115 ? 0 : -1))
            if (r4 > 0) goto L979
            int r4 = (r152 > r115 ? 1 : (r152 == r115 ? 0 : -1))
            if (r4 > 0) goto L979
            int r4 = (r154 > r115 ? 1 : (r154 == r115 ? 0 : -1))
            if (r4 > 0) goto L979
            int r4 = (r156 > r115 ? 1 : (r156 == r115 ? 0 : -1))
            if (r4 > 0) goto L979
            int r4 = (r158 > r115 ? 1 : (r158 == r115 ? 0 : -1))
            if (r4 > 0) goto L979
            int r4 = (r160 > r115 ? 1 : (r160 == r115 ? 0 : -1))
            if (r4 > 0) goto L979
            int r4 = (r162 > r115 ? 1 : (r162 == r115 ? 0 : -1))
            if (r4 > 0) goto L979
            int r4 = (r164 > r115 ? 1 : (r164 == r115 ? 0 : -1))
            if (r4 <= 0) goto L971
            goto L979
        L971:
            r167 = r15
            r60 = 18
            r66 = 12
            goto La34
        L979:
            r4 = 22
            java.lang.Object[] r4 = new java.lang.Object[r4]
            java.lang.Long r166 = java.lang.Long.valueOf(r124)
            r16 = 0
            r4[r16] = r166
            java.lang.Long r166 = java.lang.Long.valueOf(r126)
            r63 = 1
            r4[r63] = r166
            java.lang.Long r166 = java.lang.Long.valueOf(r128)
            r114 = 2
            r4[r114] = r166
            java.lang.Long r166 = java.lang.Long.valueOf(r130)
            r67 = 3
            r4[r67] = r166
            java.lang.Long r166 = java.lang.Long.valueOf(r132)
            r68 = 4
            r4[r68] = r166
            java.lang.Long r166 = java.lang.Long.valueOf(r134)
            r69 = 5
            r4[r69] = r166
            java.lang.Long r166 = java.lang.Long.valueOf(r140)
            r70 = 6
            r4[r70] = r166
            java.lang.Long r166 = java.lang.Long.valueOf(r142)
            r71 = 7
            r4[r71] = r166
            java.lang.Long r166 = java.lang.Long.valueOf(r136)
            r72 = 8
            r4[r72] = r166
            java.lang.Integer r166 = java.lang.Integer.valueOf(r120)
            r73 = 9
            r4[r73] = r166
            java.lang.Long r166 = java.lang.Long.valueOf(r146)
            r74 = 10
            r4[r74] = r166
            java.lang.Long r166 = java.lang.Long.valueOf(r148)
            r62 = 11
            r4[r62] = r166
            java.lang.Long r166 = java.lang.Long.valueOf(r138)
            r66 = 12
            r4[r66] = r166
            java.lang.Long r166 = java.lang.Long.valueOf(r144)
            r167 = r15
            r15 = 13
            r4[r15] = r166
            r123 = 14
            java.lang.Long r166 = java.lang.Long.valueOf(r150)
            r4[r123] = r166
            r123 = 15
            java.lang.Long r166 = java.lang.Long.valueOf(r152)
            r4[r123] = r166
            r123 = 16
            java.lang.Long r166 = java.lang.Long.valueOf(r154)
            r4[r123] = r166
            r123 = 17
            java.lang.Long r166 = java.lang.Long.valueOf(r156)
            r4[r123] = r166
            java.lang.Long r123 = java.lang.Long.valueOf(r158)
            r60 = 18
            r4[r60] = r123
            r123 = 19
            java.lang.Long r166 = java.lang.Long.valueOf(r160)
            r4[r123] = r166
            r123 = 20
            java.lang.Long r166 = java.lang.Long.valueOf(r162)
            r4[r123] = r166
            r123 = 21
            java.lang.Long r166 = java.lang.Long.valueOf(r164)
            r4[r123] = r166
            java.lang.String r15 = "nt"
            dumpLine(r9, r1, r12, r15, r4)
        La34:
            android.os.BatteryStats$ControllerActivityCounter r15 = r11.getModemControllerActivity()
            java.lang.String r4 = "mcd"
            r166 = r1
            r1 = r212
            r168 = r2
            r2 = r166
            r169 = r3
            r3 = r12
            r82 = r119
            r119 = r0
            r0 = 44
            r170 = r5
            r5 = r15
            r171 = r14
            r14 = r121
            r121 = r6
            r6 = r213
            dumpControllerActivityLine(r1, r2, r3, r4, r5, r6)
            long r172 = r11.getFullWifiLockTime(r14, r10)
            long r174 = r11.getWifiScanTime(r14, r10)
            int r122 = r11.getWifiScanCount(r10)
            int r176 = r11.getWifiScanBackgroundCount(r10)
            long r1 = r11.getWifiScanActualTime(r14)
            long r1 = r1 + r111
            r3 = 1000(0x3e8, double:4.94E-321)
            long r177 = r1 / r3
            long r1 = r11.getWifiScanBackgroundTime(r14)
            long r1 = r1 + r111
            long r179 = r1 / r3
            long r181 = r11.getWifiRunningTime(r14, r10)
            int r1 = (r172 > r115 ? 1 : (r172 == r115 ? 0 : -1))
            if (r1 != 0) goto La9c
            int r1 = (r174 > r115 ? 1 : (r174 == r115 ? 0 : -1))
            if (r1 != 0) goto La9c
            if (r122 != 0) goto La9c
            if (r-80 != 0) goto La9c
            int r1 = (r177 > r115 ? 1 : (r177 == r115 ? 0 : -1))
            if (r1 != 0) goto La9c
            int r1 = (r179 > r115 ? 1 : (r179 == r115 ? 0 : -1))
            if (r1 != 0) goto La9c
            int r1 = (r181 > r115 ? 1 : (r181 == r115 ? 0 : -1))
            if (r1 == 0) goto La99
            goto La9c
        La99:
            r6 = r166
            goto Lae4
        La9c:
            r1 = 10
            java.lang.Object[] r2 = new java.lang.Object[r1]
            java.lang.Long r1 = java.lang.Long.valueOf(r172)
            r3 = 0
            r2[r3] = r1
            java.lang.Long r1 = java.lang.Long.valueOf(r174)
            r3 = 1
            r2[r3] = r1
            java.lang.Long r1 = java.lang.Long.valueOf(r181)
            r3 = 2
            r2[r3] = r1
            java.lang.Integer r1 = java.lang.Integer.valueOf(r122)
            r3 = 3
            r2[r3] = r1
            r1 = 4
            r2[r1] = r118
            r1 = 5
            r2[r1] = r118
            r1 = 6
            r2[r1] = r118
            java.lang.Integer r1 = java.lang.Integer.valueOf(r176)
            r3 = 7
            r2[r3] = r1
            java.lang.Long r1 = java.lang.Long.valueOf(r177)
            r3 = 8
            r2[r3] = r1
            java.lang.Long r1 = java.lang.Long.valueOf(r179)
            r3 = 9
            r2[r3] = r1
            java.lang.String r1 = "wfl"
            r6 = r166
            dumpLine(r9, r6, r12, r1, r2)
        Lae4:
            android.os.BatteryStats$ControllerActivityCounter r5 = r11.getWifiControllerActivity()
            java.lang.String r4 = "wfcd"
            r1 = r212
            r2 = r6
            r3 = r12
            r0 = r6
            r6 = r213
            dumpControllerActivityLine(r1, r2, r3, r4, r5, r6)
            android.os.BatteryStats$Timer r6 = r11.getBluetoothScanTimer()
            if (r6 == 0) goto Lbf4
            long r1 = r6.getTotalTimeLocked(r14, r10)
            long r1 = r1 + r111
            r3 = 1000(0x3e8, double:4.94E-321)
            long r1 = r1 / r3
            int r3 = (r1 > r115 ? 1 : (r1 == r115 ? 0 : -1))
            if (r3 == 0) goto Lbe7
            int r3 = r6.getCountLocked(r10)
            android.os.BatteryStats$Timer r4 = r11.getBluetoothScanBackgroundTimer()
            if (r4 == 0) goto Lb18
            int r5 = r4.getCountLocked(r10)
            goto Lb19
        Lb18:
            r5 = 0
        Lb19:
            r183 = r8
            r7 = r105
            long r105 = r6.getTotalDurationMsLocked(r7)
            if (r4 == 0) goto Lb28
            long r184 = r4.getTotalDurationMsLocked(r7)
            goto Lb2a
        Lb28:
            r184 = r115
        Lb2a:
            android.os.BatteryStats$Counter r186 = r11.getBluetoothScanResultCounter()
            if (r-70 == 0) goto Lb3b
            r186 = r4
            android.os.BatteryStats$Counter r4 = r11.getBluetoothScanResultCounter()
            int r4 = r4.getCountLocked(r10)
            goto Lb3e
        Lb3b:
            r186 = r4
            r4 = 0
        Lb3e:
            android.os.BatteryStats$Counter r187 = r11.getBluetoothScanResultBgCounter()
            if (r-69 == 0) goto Lb4f
            r187 = r6
            android.os.BatteryStats$Counter r6 = r11.getBluetoothScanResultBgCounter()
            int r6 = r6.getCountLocked(r10)
            goto Lb52
        Lb4f:
            r187 = r6
            r6 = 0
        Lb52:
            r188 = r14
            android.os.BatteryStats$Timer r14 = r11.getBluetoothUnoptimizedScanTimer()
            if (r14 == 0) goto Lb5f
            long r190 = r14.getTotalDurationMsLocked(r7)
            goto Lb61
        Lb5f:
            r190 = r115
        Lb61:
            if (r14 == 0) goto Lb68
            long r192 = r14.getMaxDurationMsLocked(r7)
            goto Lb6a
        Lb68:
            r192 = r115
        Lb6a:
            android.os.BatteryStats$Timer r15 = r11.getBluetoothUnoptimizedScanBackgroundTimer()
            if (r15 == 0) goto Lb76
            long r194 = r15.getTotalDurationMsLocked(r7)
            goto Lb78
        Lb76:
            r194 = r115
        Lb78:
            if (r15 == 0) goto Lb7f
            long r196 = r15.getMaxDurationMsLocked(r7)
            goto Lb81
        Lb7f:
            r196 = r115
        Lb81:
            r198 = r14
            r62 = r15
            r14 = 11
            java.lang.Object[] r15 = new java.lang.Object[r14]
            java.lang.Long r199 = java.lang.Long.valueOf(r1)
            r16 = 0
            r15[r16] = r199
            java.lang.Integer r199 = java.lang.Integer.valueOf(r3)
            r63 = 1
            r15[r63] = r199
            java.lang.Integer r199 = java.lang.Integer.valueOf(r5)
            r114 = 2
            r15[r114] = r199
            java.lang.Long r199 = java.lang.Long.valueOf(r105)
            r67 = 3
            r15[r67] = r199
            java.lang.Long r199 = java.lang.Long.valueOf(r184)
            r68 = 4
            r15[r68] = r199
            java.lang.Integer r199 = java.lang.Integer.valueOf(r4)
            r69 = 5
            r15[r69] = r199
            java.lang.Integer r199 = java.lang.Integer.valueOf(r6)
            r70 = 6
            r15[r70] = r199
            java.lang.Long r199 = java.lang.Long.valueOf(r190)
            r71 = 7
            r15[r71] = r199
            java.lang.Long r199 = java.lang.Long.valueOf(r194)
            r72 = 8
            r15[r72] = r199
            java.lang.Long r199 = java.lang.Long.valueOf(r192)
            r73 = 9
            r15[r73] = r199
            java.lang.Long r199 = java.lang.Long.valueOf(r196)
            r74 = 10
            r15[r74] = r199
            java.lang.String r14 = "blem"
            dumpLine(r9, r0, r12, r14, r15)
            goto Lc00
        Lbe7:
            r187 = r6
            r183 = r8
            r188 = r14
            r7 = r105
            r72 = 8
            r73 = 9
            goto Lc00
        Lbf4:
            r187 = r6
            r183 = r8
            r188 = r14
            r7 = r105
            r72 = 8
            r73 = 9
        Lc00:
            android.os.BatteryStats$ControllerActivityCounter r5 = r11.getBluetoothControllerActivity()
            java.lang.String r4 = "ble"
            r1 = r212
            r2 = r0
            r3 = r12
            r14 = r187
            r6 = r213
            dumpControllerActivityLine(r1, r2, r3, r4, r5, r6)
            boolean r1 = r11.hasUserActivity()
            if (r1 == 0) goto Lc3c
            int r1 = android.os.BatteryStats.Uid.NUM_USER_ACTIVITY_TYPES
            java.lang.Object[] r1 = new java.lang.Object[r1]
            r2 = 0
            r3 = 0
        Lc1e:
            int r4 = android.os.BatteryStats.Uid.NUM_USER_ACTIVITY_TYPES
            if (r3 >= r4) goto Lc32
            int r4 = r11.getUserActivityCount(r3, r10)
            java.lang.Integer r5 = java.lang.Integer.valueOf(r4)
            r1[r3] = r5
            if (r4 == 0) goto Lc2f
            r2 = 1
        Lc2f:
            int r3 = r3 + 1
            goto Lc1e
        Lc32:
            if (r2 == 0) goto Lc3a
            java.lang.String r3 = "ua"
            dumpLine(r9, r0, r12, r3, r1)
        Lc3a:
            r117 = r1
        Lc3c:
            android.os.BatteryStats$Timer r1 = r11.getAggregatedPartialWakelockTimer()
            if (r1 == 0) goto Lc72
            android.os.BatteryStats$Timer r1 = r11.getAggregatedPartialWakelockTimer()
            long r2 = r1.getTotalDurationMsLocked(r7)
            android.os.BatteryStats$Timer r4 = r1.getSubTimer()
            if (r4 == 0) goto Lc55
            long r5 = r4.getTotalDurationMsLocked(r7)
            goto Lc57
        Lc55:
            r5 = r115
        Lc57:
            r62 = r1
            r15 = 2
            java.lang.Object[] r1 = new java.lang.Object[r15]
            java.lang.Long r15 = java.lang.Long.valueOf(r2)
            r16 = 0
            r1[r16] = r15
            java.lang.Long r15 = java.lang.Long.valueOf(r5)
            r63 = 1
            r1[r63] = r15
            java.lang.String r15 = "awl"
            dumpLine(r9, r0, r12, r15, r1)
            goto Lc74
        Lc72:
            r63 = 1
        Lc74:
            android.util.ArrayMap r15 = r11.getWakelockStats()
            int r1 = r15.size()
            int r1 = r1 + (-1)
            r6 = r1
        Lc7f:
            r5 = 95
            if (r6 < 0) goto Ld36
            java.lang.Object r1 = r15.valueAt(r6)
            r3 = r1
            android.os.BatteryStats$Uid$Wakelock r3 = (android.os.BatteryStats.Uid.Wakelock) r3
            java.lang.String r62 = ""
            r1 = 0
            r13.setLength(r1)
            r1 = 1
            android.os.BatteryStats$Timer r2 = r3.getWakeTime(r1)
            java.lang.String r105 = "f"
            r1 = r13
            r184 = r7
            r8 = r3
            r3 = r188
            r7 = r5
            r5 = r105
            r187 = r14
            r14 = r6
            r6 = r213
            r10 = r7
            r200 = r184
            r7 = r62
            java.lang.String r62 = printWakeLockCheckin(r1, r2, r3, r5, r6, r7)
            r1 = 0
            android.os.BatteryStats$Timer r105 = r8.getWakeTime(r1)
            java.lang.String r5 = "p"
            r1 = r13
            r2 = r105
            r7 = r62
            java.lang.String r62 = printWakeLockCheckin(r1, r2, r3, r5, r6, r7)
            if (r105 == 0) goto Lcc5
            android.os.BatteryStats$Timer r1 = r105.getSubTimer()
            goto Lcc6
        Lcc5:
            r1 = 0
        Lcc6:
            r2 = r1
            java.lang.String r5 = "bp"
            r1 = r13
            r3 = r188
            r6 = r213
            r7 = r62
            java.lang.String r62 = printWakeLockCheckin(r1, r2, r3, r5, r6, r7)
            r1 = 2
            android.os.BatteryStats$Timer r2 = r8.getWakeTime(r1)
            java.lang.String r5 = "w"
            r1 = r13
            r7 = r62
            java.lang.String r1 = printWakeLockCheckin(r1, r2, r3, r5, r6, r7)
            int r2 = r13.length()
            if (r2 <= 0) goto Ld27
            java.lang.Object r2 = r15.keyAt(r14)
            java.lang.String r2 = (java.lang.String) r2
            r3 = 44
            int r4 = r2.indexOf(r3)
            if (r4 < 0) goto Lcfb
            java.lang.String r2 = r2.replace(r3, r10)
        Lcfb:
            r6 = 10
            int r3 = r2.indexOf(r6)
            if (r3 < 0) goto Ld07
            java.lang.String r2 = r2.replace(r6, r10)
        Ld07:
            r7 = 13
            int r3 = r2.indexOf(r7)
            if (r3 < 0) goto Ld13
            java.lang.String r2 = r2.replace(r7, r10)
        Ld13:
            r3 = 2
            java.lang.Object[] r4 = new java.lang.Object[r3]
            r3 = 0
            r4[r3] = r2
            java.lang.String r3 = r13.toString()
            r5 = 1
            r4[r5] = r3
            java.lang.String r3 = "wl"
            dumpLine(r9, r0, r12, r3, r4)
            goto Ld2b
        Ld27:
            r6 = 10
            r7 = 13
        Ld2b:
            int r1 = r14 + (-1)
            r10 = r213
            r6 = r1
            r14 = r187
            r7 = r200
            goto Lc7f
        Ld36:
            r10 = r5
            r200 = r7
            r187 = r14
            r7 = 13
            r14 = r6
            r6 = 10
            android.os.BatteryStats$Timer r14 = r11.getMulticastWakelockStats()
            if (r14 == 0) goto Ld76
        Ld47:
            r8 = r10
            r4 = r188
            r10 = r213
            long r1 = r14.getTotalTimeLocked(r4, r10)
            r64 = 1000(0x3e8, double:4.94E-321)
            long r1 = r1 / r64
            int r3 = r14.getCountLocked(r10)
            int r62 = (r1 > r115 ? 1 : (r1 == r115 ? 0 : -1))
            if (r62 <= 0) goto Ld7b
            r6 = 2
            java.lang.Object[] r7 = new java.lang.Object[r6]
            java.lang.Long r6 = java.lang.Long.valueOf(r1)
            r16 = 0
            r7[r16] = r6
            java.lang.Integer r6 = java.lang.Integer.valueOf(r3)
            r62 = 1
            r7[r62] = r6
            java.lang.String r6 = "wmc"
            dumpLine(r9, r0, r12, r6, r7)
            goto Ld7b
        Ld76:
            r8 = r10
            r4 = r188
            r10 = r213
        Ld7b:
            android.util.ArrayMap r6 = r11.getSyncStats()
            int r1 = r6.size()
            r2 = 1
            int r1 = r1 - r2
        Ld85:
            if (r1 < 0) goto Le27
            java.lang.Object r3 = r6.valueAt(r1)
            android.os.BatteryStats$Timer r3 = (android.os.BatteryStats.Timer) r3
            long r105 = r3.getTotalTimeLocked(r4, r10)
            long r105 = r105 + r111
            r64 = 1000(0x3e8, double:4.94E-321)
            long r105 = r105 / r64
            int r7 = r3.getCountLocked(r10)
            android.os.BatteryStats$Timer r2 = r3.getSubTimer()
            if (r2 == 0) goto Ldac
            r185 = r14
            r184 = r15
            r14 = r200
            long r188 = r2.getTotalDurationMsLocked(r14)
            goto Ldb4
        Ldac:
            r185 = r14
            r184 = r15
            r14 = r200
            r188 = -1
        Ldb4:
            if (r2 == 0) goto Ldbb
            int r62 = r2.getCountLocked(r10)
            goto Ldbd
        Ldbb:
            r62 = -1
        Ldbd:
            int r186 = (r105 > r115 ? 1 : (r105 == r115 ? 0 : -1))
            if (r-70 == 0) goto Le0f
            r190 = r2
            r8 = 5
            java.lang.Object[] r2 = new java.lang.Object[r8]
            java.lang.StringBuilder r8 = new java.lang.StringBuilder
            r8.<init>()
            r191 = r3
            r3 = r171
            java.lang.StringBuilder r8 = r8.append(r3)
            java.lang.Object r171 = r6.keyAt(r1)
            r192 = r6
            r6 = r171
            java.lang.String r6 = (java.lang.String) r6
            java.lang.StringBuilder r6 = r8.append(r6)
            java.lang.StringBuilder r6 = r6.append(r3)
            java.lang.String r6 = r6.toString()
            r8 = 0
            r2[r8] = r6
            java.lang.Long r6 = java.lang.Long.valueOf(r105)
            r8 = 1
            r2[r8] = r6
            java.lang.Integer r6 = java.lang.Integer.valueOf(r7)
            r8 = 2
            r2[r8] = r6
            java.lang.Long r6 = java.lang.Long.valueOf(r188)
            r8 = 3
            r2[r8] = r6
            java.lang.Integer r6 = java.lang.Integer.valueOf(r62)
            r8 = 4
            r2[r8] = r6
            java.lang.String r6 = "sy"
            dumpLine(r9, r0, r12, r6, r2)
            goto Le17
        Le0f:
            r190 = r2
            r191 = r3
            r192 = r6
            r3 = r171
        Le17:
            int r1 = r1 + (-1)
            r171 = r3
            r200 = r14
            r15 = r184
            r14 = r185
            r6 = r192
            r8 = 95
            goto Ld85
        Le27:
            r192 = r6
            r185 = r14
            r184 = r15
            r3 = r171
            r14 = r200
            android.util.ArrayMap r8 = r11.getJobStats()
            int r1 = r8.size()
            r2 = 1
            int r1 = r1 - r2
        Le3b:
            if (r1 < 0) goto Lec8
            java.lang.Object r2 = r8.valueAt(r1)
            android.os.BatteryStats$Timer r2 = (android.os.BatteryStats.Timer) r2
            long r6 = r2.getTotalTimeLocked(r4, r10)
            long r6 = r6 + r111
            r64 = 1000(0x3e8, double:4.94E-321)
            long r6 = r6 / r64
            int r105 = r2.getCountLocked(r10)
            r188 = r4
            android.os.BatteryStats$Timer r4 = r2.getSubTimer()
            if (r4 == 0) goto Le5e
            long r190 = r4.getTotalDurationMsLocked(r14)
            goto Le60
        Le5e:
            r190 = -1
        Le60:
            if (r4 == 0) goto Le67
            int r5 = r4.getCountLocked(r10)
            goto Le68
        Le67:
            r5 = -1
        Le68:
            int r106 = (r6 > r115 ? 1 : (r6 == r115 ? 0 : -1))
            if (r106 == 0) goto Leb9
            r106 = r2
            r171 = r4
            r2 = 5
            java.lang.Object[] r4 = new java.lang.Object[r2]
            java.lang.StringBuilder r2 = new java.lang.StringBuilder
            r2.<init>()
            java.lang.StringBuilder r2 = r2.append(r3)
            java.lang.Object r193 = r8.keyAt(r1)
            r194 = r8
            r8 = r193
            java.lang.String r8 = (java.lang.String) r8
            java.lang.StringBuilder r2 = r2.append(r8)
            java.lang.StringBuilder r2 = r2.append(r3)
            java.lang.String r2 = r2.toString()
            r8 = 0
            r4[r8] = r2
            java.lang.Long r2 = java.lang.Long.valueOf(r6)
            r8 = 1
            r4[r8] = r2
            java.lang.Integer r2 = java.lang.Integer.valueOf(r105)
            r8 = 2
            r4[r8] = r2
            java.lang.Long r2 = java.lang.Long.valueOf(r190)
            r67 = 3
            r4[r67] = r2
            java.lang.Integer r2 = java.lang.Integer.valueOf(r5)
            r68 = 4
            r4[r68] = r2
            java.lang.String r2 = "jb"
            dumpLine(r9, r0, r12, r2, r4)
            goto Lec0
        Leb9:
            r106 = r2
            r171 = r4
            r194 = r8
            r8 = 2
        Lec0:
            int r1 = r1 + (-1)
            r4 = r188
            r8 = r194
            goto Le3b
        Lec8:
            r188 = r4
            r194 = r8
            r8 = 2
            int[] r6 = android.app.job.JobParameters.getJobStopReasonCodes()
            int r1 = r6.length
            r2 = 1
            int r1 = r1 + r2
            java.lang.Object[] r7 = new java.lang.Object[r1]
            android.util.ArrayMap r5 = r11.getJobCompletionStats()
            int r1 = r5.size()
            int r1 = r1 - r2
        Ledf:
            if (r1 < 0) goto Lf3e
            java.lang.Object r2 = r5.valueAt(r1)
            android.util.SparseIntArray r2 = (android.util.SparseIntArray) r2
            if (r2 == 0) goto Lf32
            java.lang.StringBuilder r4 = new java.lang.StringBuilder
            r4.<init>()
            java.lang.StringBuilder r4 = r4.append(r3)
            java.lang.Object r62 = r5.keyAt(r1)
            r8 = r62
            java.lang.String r8 = (java.lang.String) r8
            java.lang.StringBuilder r4 = r4.append(r8)
            java.lang.StringBuilder r4 = r4.append(r3)
            java.lang.String r4 = r4.toString()
            r8 = 0
            r7[r8] = r4
            r4 = 0
        Lf0a:
            int r8 = r6.length
            if (r4 >= r8) goto Lf28
            int r8 = r4 + 1
            r171 = r3
            r3 = r6[r4]
            r62 = r5
            r5 = 0
            int r3 = r2.get(r3, r5)
            java.lang.Integer r3 = java.lang.Integer.valueOf(r3)
            r7[r8] = r3
            int r4 = r4 + 1
            r5 = r62
            r3 = r171
            r8 = 0
            goto Lf0a
        Lf28:
            r171 = r3
            r62 = r5
            java.lang.String r3 = "jbc"
            dumpLine(r9, r0, r12, r3, r7)
            goto Lf36
        Lf32:
            r171 = r3
            r62 = r5
        Lf36:
            int r1 = r1 + (-1)
            r5 = r62
            r3 = r171
            r8 = 2
            goto Ledf
        Lf3e:
            r171 = r3
            r62 = r5
            r11.getDeferredJobsCheckinLineLocked(r13, r10)
            int r1 = r13.length()
            if (r1 <= 0) goto Lf5a
            r1 = 1
            java.lang.Object[] r2 = new java.lang.Object[r1]
            java.lang.String r1 = r13.toString()
            r3 = 0
            r2[r3] = r1
            java.lang.String r1 = "jbd"
            dumpLine(r9, r0, r12, r1, r2)
        Lf5a:
            android.os.BatteryStats$Timer r5 = r11.getFlashlightTurnedOnTimer()
            java.lang.String r4 = "fla"
            r1 = r212
            r2 = r0
            r8 = r171
            r3 = r12
            r105 = r188
            r123 = r6
            r171 = r7
            r74 = r192
            r188 = 13
            r189 = 10
            r6 = r105
            r202 = r8
            r186 = r13
            r114 = r183
            r13 = 2
            r183 = r60
            r60 = r168
            r168 = r194
            r8 = r213
            dumpTimer(r1, r2, r3, r4, r5, r6, r8)
            android.os.BatteryStats$Timer r5 = r11.getCameraTurnedOnTimer()
            java.lang.String r4 = "cam"
            dumpTimer(r1, r2, r3, r4, r5, r6, r8)
            android.os.BatteryStats$Timer r5 = r11.getVideoTurnedOnTimer()
            java.lang.String r4 = "vid"
            dumpTimer(r1, r2, r3, r4, r5, r6, r8)
            android.os.BatteryStats$Timer r5 = r11.getAudioTurnedOnTimer()
            java.lang.String r4 = "aud"
            dumpTimer(r1, r2, r3, r4, r5, r6, r8)
            android.util.SparseArray r8 = r11.getSensorStats()
            int r6 = r8.size()
            r1 = 0
        Lfab:
            if (r1 >= r6) goto L1041
            java.lang.Object r2 = r8.valueAt(r1)
            android.os.BatteryStats$Uid$Sensor r2 = (android.os.BatteryStats.Uid.Sensor) r2
            int r3 = r8.keyAt(r1)
            android.os.BatteryStats$Timer r4 = r2.getSensorTime()
            if (r4 == 0) goto L1034
            r200 = r14
            r13 = r105
            long r105 = r4.getTotalTimeLocked(r13, r10)
            long r105 = r105 + r111
            r64 = 1000(0x3e8, double:4.94E-321)
            long r105 = r105 / r64
            int r5 = (r105 > r115 ? 1 : (r105 == r115 ? 0 : -1))
            if (r5 == 0) goto L102d
            int r5 = r4.getCountLocked(r10)
            android.os.BatteryStats$Timer r7 = r2.getSensorBackgroundTime()
            if (r7 == 0) goto Lfde
            int r190 = r7.getCountLocked(r10)
            goto Lfe0
        Lfde:
            r190 = 0
        Lfe0:
            r191 = r13
            r13 = r200
            long r193 = r4.getTotalDurationMsLocked(r13)
            if (r7 == 0) goto Lfef
            long r195 = r7.getTotalDurationMsLocked(r13)
            goto Lff1
        Lfef:
            r195 = r115
        Lff1:
            r198 = r2
            r15 = 6
            java.lang.Object[] r2 = new java.lang.Object[r15]
            java.lang.Integer r15 = java.lang.Integer.valueOf(r3)
            r16 = 0
            r2[r16] = r15
            java.lang.Long r15 = java.lang.Long.valueOf(r105)
            r63 = 1
            r2[r63] = r15
            java.lang.Integer r15 = java.lang.Integer.valueOf(r5)
            r197 = 2
            r2[r197] = r15
            java.lang.Integer r197 = java.lang.Integer.valueOf(r190)
            r67 = 3
            r2[r67] = r197
            java.lang.Long r197 = java.lang.Long.valueOf(r193)
            r68 = 4
            r2[r68] = r197
            java.lang.Long r197 = java.lang.Long.valueOf(r195)
            r69 = 5
            r2[r69] = r197
            java.lang.String r15 = "sr"
            dumpLine(r9, r0, r12, r15, r2)
            goto L1039
        L102d:
            r198 = r2
            r191 = r13
            r13 = r200
            goto L1039
        L1034:
            r198 = r2
            r13 = r14
            r191 = r105
        L1039:
            int r1 = r1 + 1
            r14 = r13
            r105 = r191
            r13 = 2
            goto Lfab
        L1041:
            r13 = r14
            r191 = r105
            android.os.BatteryStats$Timer r5 = r11.getVibratorOnTimer()
            java.lang.String r4 = "vib"
            r1 = r212
            r2 = r0
            r3 = r12
            r105 = r6
            r6 = r191
            r106 = r8
            r8 = r213
            dumpTimer(r1, r2, r3, r4, r5, r6, r8)
            android.os.BatteryStats$Timer r5 = r11.getForegroundActivityTimer()
            java.lang.String r4 = "fg"
            dumpTimer(r1, r2, r3, r4, r5, r6, r8)
            android.os.BatteryStats$Timer r5 = r11.getForegroundServiceTimer()
            java.lang.String r4 = "fgs"
            dumpTimer(r1, r2, r3, r4, r5, r6, r8)
            r1 = 7
            java.lang.Object[] r2 = new java.lang.Object[r1]
            r3 = 0
            r5 = 0
        L1072:
            if (r5 >= r1) goto L108e
            r6 = r191
            long r190 = r11.getProcessStateTime(r5, r6, r10)
            long r3 = r3 + r190
            long r192 = r190 + r111
            r64 = 1000(0x3e8, double:4.94E-321)
            long r192 = r192 / r64
            java.lang.Long r1 = java.lang.Long.valueOf(r192)
            r2[r5] = r1
            int r5 = r5 + 1
            r191 = r6
            r1 = 7
            goto L1072
        L108e:
            r6 = r191
            int r1 = (r3 > r115 ? 1 : (r3 == r115 ? 0 : -1))
            if (r1 <= 0) goto L109a
            java.lang.String r1 = "st"
            dumpLine(r9, r0, r12, r1, r2)
        L109a:
            long r190 = r11.getUserCpuTimeUs(r10)
            long r192 = r11.getSystemCpuTimeUs(r10)
            int r1 = (r190 > r115 ? 1 : (r190 == r115 ? 0 : -1))
            if (r1 > 0) goto L10aa
            int r1 = (r192 > r115 ? 1 : (r192 == r115 ? 0 : -1))
            if (r1 <= 0) goto L10c9
        L10aa:
            r1 = 3
            java.lang.Object[] r5 = new java.lang.Object[r1]
            r64 = 1000(0x3e8, double:4.94E-321)
            long r194 = r190 / r64
            java.lang.Long r1 = java.lang.Long.valueOf(r194)
            r8 = 0
            r5[r8] = r1
            long r194 = r192 / r64
            java.lang.Long r1 = java.lang.Long.valueOf(r194)
            r8 = 1
            r5[r8] = r1
            r1 = 2
            r5[r1] = r118
            java.lang.String r1 = "cpu"
            dumpLine(r9, r0, r12, r1, r5)
        L10c9:
            if (r119 == 0) goto L11d4
            long[] r1 = r11.getCpuFreqTimes(r10)
            if (r1 == 0) goto L114d
            int r5 = r1.length
            r8 = r119
            int r15 = r8.length
            if (r5 != r15) goto L1144
            r5 = r186
            r15 = 0
            r5.setLength(r15)
            r15 = 0
        L10de:
            r119 = r2
            int r2 = r1.length
            if (r15 >= r2) goto L10f8
            if (r15 == 0) goto L10ea
            r2 = 44
            r5.append(r2)
        L10ea:
            r194 = r3
            r2 = r1[r15]
            r5.append(r2)
            int r15 = r15 + 1
            r2 = r119
            r3 = r194
            goto L10de
        L10f8:
            r194 = r3
            long[] r2 = r11.getScreenOffCpuFreqTimes(r10)
            if (r2 == 0) goto L1119
            r3 = 0
        L1101:
            int r4 = r2.length
            if (r3 >= r4) goto L1116
            r4 = 44
            java.lang.StringBuilder r15 = r5.append(r4)
            r200 = r6
            r6 = r2[r3]
            r15.append(r6)
            int r3 = r3 + 1
            r6 = r200
            goto L1101
        L1116:
            r200 = r6
            goto L1127
        L1119:
            r200 = r6
            r3 = 0
        L111c:
            int r4 = r1.length
            if (r3 >= r4) goto L1127
            java.lang.String r4 = ",0"
            r5.append(r4)
            int r3 = r3 + 1
            goto L111c
        L1127:
            r3 = 3
            java.lang.Object[] r4 = new java.lang.Object[r3]
            java.lang.String r3 = "A"
            r6 = 0
            r4[r6] = r3
            int r3 = r1.length
            java.lang.Integer r3 = java.lang.Integer.valueOf(r3)
            r6 = 1
            r4[r6] = r3
            java.lang.String r3 = r5.toString()
            r6 = 2
            r4[r6] = r3
            java.lang.String r3 = "ctf"
            dumpLine(r9, r0, r12, r3, r4)
            goto L1157
        L1144:
            r119 = r2
            r194 = r3
            r200 = r6
            r5 = r186
            goto L1157
        L114d:
            r194 = r3
            r200 = r6
            r8 = r119
            r5 = r186
            r119 = r2
        L1157:
            int r2 = r210.getCpuFreqCount()
            long[] r2 = new long[r2]
            r3 = 0
        L115e:
            r4 = 7
            if (r3 >= r4) goto L11d1
            boolean r4 = r11.getCpuFreqTimes(r2, r3)
            if (r4 == 0) goto L11ca
            r4 = 0
            r5.setLength(r4)
            r4 = 0
        L116c:
            int r6 = r2.length
            if (r4 >= r6) goto L117e
            if (r4 == 0) goto L1176
            r6 = 44
            r5.append(r6)
        L1176:
            r6 = r2[r4]
            r5.append(r6)
            int r4 = r4 + 1
            goto L116c
        L117e:
            boolean r4 = r11.getScreenOffCpuFreqTimes(r2, r3)
            if (r4 == 0) goto L119d
            r4 = 0
        L1185:
            int r6 = r2.length
            if (r4 >= r6) goto L119a
            r6 = 44
            java.lang.StringBuilder r7 = r5.append(r6)
            r196 = r13
            r13 = r2[r4]
            r7.append(r13)
            int r4 = r4 + 1
            r13 = r196
            goto L1185
        L119a:
            r196 = r13
            goto L11ab
        L119d:
            r196 = r13
            r4 = 0
        L11a0:
            int r6 = r2.length
            if (r4 >= r6) goto L11ab
            java.lang.String r6 = ",0"
            r5.append(r6)
            int r4 = r4 + 1
            goto L11a0
        L11ab:
            r4 = 3
            java.lang.Object[] r6 = new java.lang.Object[r4]
            java.lang.String[] r4 = android.os.BatteryStats.Uid.UID_PROCESS_TYPES
            r4 = r4[r3]
            r7 = 0
            r6[r7] = r4
            int r4 = r2.length
            java.lang.Integer r4 = java.lang.Integer.valueOf(r4)
            r7 = 1
            r6[r7] = r4
            java.lang.String r4 = r5.toString()
            r7 = 2
            r6[r7] = r4
            java.lang.String r4 = "ctf"
            dumpLine(r9, r0, r12, r4, r6)
            goto L11cc
        L11ca:
            r196 = r13
        L11cc:
            int r3 = r3 + 1
            r13 = r196
            goto L115e
        L11d1:
            r196 = r13
            goto L11e0
        L11d4:
            r194 = r3
            r200 = r6
            r196 = r13
            r8 = r119
            r5 = r186
            r119 = r2
        L11e0:
            android.util.ArrayMap r1 = r11.getProcessStats()
            int r2 = r1.size()
            r3 = 1
            int r2 = r2 - r3
        L11eb:
            if (r2 < 0) goto L128d
            java.lang.Object r3 = r1.valueAt(r2)
            android.os.BatteryStats$Uid$Proc r3 = (android.os.BatteryStats.Uid.Proc) r3
            long r6 = r3.getUserTime(r10)
            long r13 = r3.getSystemTime(r10)
            long r203 = r3.getForegroundTime(r10)
            int r4 = r3.getStarts(r10)
            int r186 = r3.getNumCrashes(r10)
            int r198 = r3.getNumAnrs(r10)
            int r205 = (r6 > r115 ? 1 : (r6 == r115 ? 0 : -1))
            if (r-51 != 0) goto L1225
            int r205 = (r13 > r115 ? 1 : (r13 == r115 ? 0 : -1))
            if (r-51 != 0) goto L1225
            int r205 = (r203 > r115 ? 1 : (r203 == r115 ? 0 : -1))
            if (r-51 != 0) goto L1225
            if (r4 != 0) goto L1225
            if (r-58 != 0) goto L1225
            if (r-70 == 0) goto L121e
            goto L1225
        L121e:
            r207 = r1
            r206 = r5
            r5 = r202
            goto L1283
        L1225:
            r71 = r3
            r15 = 7
            java.lang.Object[] r3 = new java.lang.Object[r15]
            java.lang.StringBuilder r15 = new java.lang.StringBuilder
            r15.<init>()
            r206 = r5
            r5 = r202
            java.lang.StringBuilder r15 = r15.append(r5)
            java.lang.Object r202 = r1.keyAt(r2)
            r207 = r1
            r1 = r202
            java.lang.String r1 = (java.lang.String) r1
            java.lang.StringBuilder r1 = r15.append(r1)
            java.lang.StringBuilder r1 = r1.append(r5)
            java.lang.String r1 = r1.toString()
            r15 = 0
            r3[r15] = r1
            java.lang.Long r1 = java.lang.Long.valueOf(r6)
            r15 = 1
            r3[r15] = r1
            java.lang.Long r1 = java.lang.Long.valueOf(r13)
            r15 = 2
            r3[r15] = r1
            java.lang.Long r15 = java.lang.Long.valueOf(r203)
            r67 = 3
            r3[r67] = r15
            java.lang.Integer r15 = java.lang.Integer.valueOf(r4)
            r68 = 4
            r3[r68] = r15
            java.lang.Integer r15 = java.lang.Integer.valueOf(r198)
            r69 = 5
            r3[r69] = r15
            java.lang.Integer r15 = java.lang.Integer.valueOf(r186)
            r70 = 6
            r3[r70] = r15
            java.lang.String r15 = "pr"
            dumpLine(r9, r0, r12, r15, r3)
        L1283:
            int r2 = r2 + (-1)
            r202 = r5
            r5 = r206
            r1 = r207
            goto L11eb
        L128d:
            r207 = r1
            r206 = r5
            r5 = r202
            android.util.ArrayMap r2 = r11.getPackageStats()
            int r3 = r2.size()
            r4 = 1
            int r3 = r3 - r4
        L129e:
            if (r3 < 0) goto L1389
            java.lang.Object r4 = r2.valueAt(r3)
            android.os.BatteryStats$Uid$Pkg r4 = (android.os.BatteryStats.Uid.Pkg) r4
            r6 = 0
            android.util.ArrayMap r7 = r4.getWakeupAlarmStats()
            int r13 = r7.size()
            r14 = 1
            int r13 = r13 - r14
        L12b1:
            if (r13 < 0) goto L12e8
            java.lang.Object r14 = r7.valueAt(r13)
            android.os.BatteryStats$Counter r14 = (android.os.BatteryStats.Counter) r14
            int r14 = r14.getCountLocked(r10)
            int r6 = r6 + r14
            java.lang.Object r15 = r7.keyAt(r13)
            java.lang.String r15 = (java.lang.String) r15
            r202 = r5
            r1 = 95
            r5 = 44
            java.lang.String r15 = r15.replace(r5, r1)
            r1 = 2
            java.lang.Object[] r5 = new java.lang.Object[r1]
            r16 = 0
            r5[r16] = r15
            java.lang.Integer r71 = java.lang.Integer.valueOf(r14)
            r63 = 1
            r5[r63] = r71
            java.lang.String r1 = "wua"
            dumpLine(r9, r0, r12, r1, r5)
            int r13 = r13 + (-1)
            r5 = r202
            goto L12b1
        L12e8:
            r202 = r5
            android.util.ArrayMap r1 = r4.getServiceStats()
            int r5 = r1.size()
            r13 = 1
            int r5 = r5 - r13
        L12f4:
            if (r5 < 0) goto L136f
            java.lang.Object r13 = r1.valueAt(r5)
            android.os.BatteryStats$Uid$Pkg$Serv r13 = (android.os.BatteryStats.Uid.Pkg.Serv) r13
            r14 = r103
            long r103 = r13.getStartTime(r14, r10)
            int r71 = r13.getStarts(r10)
            int r186 = r13.getLaunches(r10)
            int r198 = (r103 > r115 ? 1 : (r103 == r115 ? 0 : -1))
            if (r-58 != 0) goto L1326
            if (r71 != 0) goto L1326
            if (r-70 == 0) goto L1313
            goto L1326
        L1313:
            r198 = r4
            r70 = r7
            r16 = 0
            r63 = 1
            r64 = 1000(0x3e8, double:4.94E-321)
            r67 = 3
            r68 = 4
            r69 = 5
            r204 = 2
            goto L1366
        L1326:
            r198 = r4
            r70 = r7
            r4 = 6
            java.lang.Object[] r7 = new java.lang.Object[r4]
            java.lang.Integer r203 = java.lang.Integer.valueOf(r6)
            r16 = 0
            r7[r16] = r203
            java.lang.Object r203 = r2.keyAt(r3)
            r63 = 1
            r7[r63] = r203
            java.lang.Object r203 = r1.keyAt(r5)
            r204 = 2
            r7[r204] = r203
            r64 = 1000(0x3e8, double:4.94E-321)
            long r208 = r103 / r64
            java.lang.Long r203 = java.lang.Long.valueOf(r208)
            r67 = 3
            r7[r67] = r203
            java.lang.Integer r203 = java.lang.Integer.valueOf(r71)
            r68 = 4
            r7[r68] = r203
            java.lang.Integer r203 = java.lang.Integer.valueOf(r186)
            r69 = 5
            r7[r69] = r203
            java.lang.String r4 = "apk"
            dumpLine(r9, r0, r12, r4, r7)
        L1366:
            int r5 = r5 + (-1)
            r103 = r14
            r7 = r70
            r4 = r198
            goto L12f4
        L136f:
            r198 = r4
            r70 = r7
            r14 = r103
            r16 = 0
            r63 = 1
            r64 = 1000(0x3e8, double:4.94E-321)
            r67 = 3
            r68 = 4
            r69 = 5
            r204 = 2
            int r3 = r3 + (-1)
            r5 = r202
            goto L129e
        L1389:
            r202 = r5
            r14 = r103
            r16 = 0
            r63 = 1
            r64 = 1000(0x3e8, double:4.94E-321)
            r67 = 3
            r68 = 4
            r69 = 5
            r204 = 2
        L139b:
            int r3 = r169 + 1
            r7 = r211
            r11 = r214
            r0 = r8
            r103 = r14
            r119 = r82
            r8 = r114
            r6 = r121
            r15 = r167
            r5 = r170
            r105 = r196
            r121 = r200
            r14 = r202
            r13 = r206
            r4 = 44
            r82 = r60
            goto L863
        L13bc:
            return
        */
        throw new UnsupportedOperationException("Method not decompiled: android.os.BatteryStats.dumpCheckinLocked(android.content.Context, java.io.PrintWriter, int, int, boolean):void");
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* loaded from: classes2.dex */
    public static final class TimerEntry {
        final int mId;
        final String mName;
        final long mTime;
        final Timer mTimer;

        TimerEntry(String name, int id, Timer timer, long time) {
            this.mName = name;
            this.mId = id;
            this.mTimer = timer;
            this.mTime = time;
        }
    }

    private void printmAh(PrintWriter printer, double power) {
        printer.print(formatCharge(power));
    }

    private void printmAh(StringBuilder sb, double power) {
        sb.append(formatCharge(power));
    }

    public final void dumpLocked(Context context, PrintWriter pw, String prefix, int which, int reqUid) {
        dumpLocked(context, pw, prefix, which, reqUid, checkWifiOnly(context));
    }

    /*  JADX ERROR: IndexOutOfBoundsException in pass: SSATransform
        java.lang.IndexOutOfBoundsException: bitIndex < 0: -17
        	at java.base/java.util.BitSet.get(BitSet.java:624)
        	at jadx.core.dex.visitors.ssa.LiveVarAnalysis.fillBasicBlockInfo(LiveVarAnalysis.java:65)
        	at jadx.core.dex.visitors.ssa.LiveVarAnalysis.runAnalysis(LiveVarAnalysis.java:36)
        	at jadx.core.dex.visitors.ssa.SSATransform.process(SSATransform.java:55)
        	at jadx.core.dex.visitors.ssa.SSATransform.visit(SSATransform.java:41)
        */
    public final void dumpLocked(android.content.Context r235, java.io.PrintWriter r236, java.lang.String r237, int r238, int r239, boolean r240) {
        /*
            r234 = this;
            r7 = r234
            r15 = r236
            r14 = r237
            r13 = r238
            r11 = r239
            if (r13 == 0) goto L29
            java.lang.StringBuilder r0 = new java.lang.StringBuilder
            r0.<init>()
            java.lang.String r1 = "ERROR: BatteryStats.dump called for which type "
            java.lang.StringBuilder r0 = r0.append(r1)
            java.lang.StringBuilder r0 = r0.append(r13)
            java.lang.String r1 = " but only STATS_SINCE_CHARGED is supported"
            java.lang.StringBuilder r0 = r0.append(r1)
            java.lang.String r0 = r0.toString()
            r15.println(r0)
            return
        L29:
            long r0 = android.os.SystemClock.uptimeMillis()
            r16 = 1000(0x3e8, double:4.94E-321)
            long r9 = r0 * r16
            long r0 = android.os.SystemClock.elapsedRealtime()
            long r5 = r0 * r16
            r18 = 500(0x1f4, double:2.47E-321)
            long r0 = r5 + r18
            long r3 = r0 / r16
            long r1 = r7.getBatteryUptime(r9)
            r20 = r3
            long r3 = r7.computeBatteryUptime(r9, r13)
            r22 = r1
            long r1 = r7.computeBatteryRealtime(r5, r13)
            long r11 = r7.computeRealtime(r5, r13)
            long r24 = r7.computeUptime(r9, r13)
            r26 = r3
            long r3 = r7.computeBatteryScreenOffUptime(r9, r13)
            r28 = r9
            long r9 = r7.computeBatteryScreenOffRealtime(r5, r13)
            long r30 = r7.computeBatteryTimeRemaining(r5)
            long r32 = r7.computeChargeTimeRemaining(r5)
            r34 = r3
            long r3 = r7.getScreenDozeTime(r5, r13)
            java.lang.StringBuilder r0 = new java.lang.StringBuilder
            r8 = 128(0x80, float:1.794E-43)
            r0.<init>(r8)
            r8 = r0
            android.util.SparseArray r0 = r234.getUidStats()
            r36 = r5
            int r5 = r0.size()
            int r6 = r234.getEstimatedBatteryCapacity()
            r38 = r0
            java.lang.String r0 = " mAh"
            r39 = r5
            r5 = 0
            if (r6 <= 0) goto Lae
            r8.setLength(r5)
            r8.append(r14)
            java.lang.String r5 = "  Estimated battery capacity: "
            r8.append(r5)
            r41 = r3
            double r3 = (double) r6
            java.lang.String r3 = formatCharge(r3)
            r8.append(r3)
            r8.append(r0)
            java.lang.String r3 = r8.toString()
            r15.println(r3)
            goto Lb0
        Lae:
            r41 = r3
        Lb0:
            int r5 = r234.getLearnedBatteryCapacity()
            if (r5 <= 0) goto Ld6
            r3 = 0
            r8.setLength(r3)
            r8.append(r14)
            java.lang.String r3 = "  Last learned battery capacity: "
            r8.append(r3)
            int r3 = r5 / 1000
            double r3 = (double) r3
            java.lang.String r3 = formatCharge(r3)
            r8.append(r3)
            r8.append(r0)
            java.lang.String r3 = r8.toString()
            r15.println(r3)
        Ld6:
            int r4 = r234.getMinLearnedBatteryCapacity()
            if (r4 <= 0) goto Lff
            r3 = 0
            r8.setLength(r3)
            r8.append(r14)
            java.lang.String r3 = "  Min learned battery capacity: "
            r8.append(r3)
            int r3 = r4 / 1000
            r43 = r4
            double r3 = (double) r3
            java.lang.String r3 = formatCharge(r3)
            r8.append(r3)
            r8.append(r0)
            java.lang.String r3 = r8.toString()
            r15.println(r3)
            goto L101
        Lff:
            r43 = r4
        L101:
            int r4 = r234.getMaxLearnedBatteryCapacity()
            if (r4 <= 0) goto L12a
            r3 = 0
            r8.setLength(r3)
            r8.append(r14)
            java.lang.String r3 = "  Max learned battery capacity: "
            r8.append(r3)
            int r3 = r4 / 1000
            r44 = r4
            double r3 = (double) r3
            java.lang.String r3 = formatCharge(r3)
            r8.append(r3)
            r8.append(r0)
            java.lang.String r3 = r8.toString()
            r15.println(r3)
            goto L12c
        L12a:
            r44 = r4
        L12c:
            r3 = 0
            r8.setLength(r3)
            r8.append(r14)
            java.lang.String r3 = "  Time on battery: "
            r8.append(r3)
            long r3 = r1 / r16
            formatTimeMs(r8, r3)
            java.lang.String r4 = "("
            r8.append(r4)
            java.lang.String r3 = r7.formatRatioLocked(r1, r11)
            r8.append(r3)
            java.lang.String r3 = ") realtime, "
            r8.append(r3)
            r46 = r5
            r45 = r6
            long r5 = r26 / r16
            formatTimeMs(r8, r5)
            r8.append(r4)
            r5 = r26
            java.lang.String r3 = r7.formatRatioLocked(r5, r1)
            r8.append(r3)
            java.lang.String r3 = ") uptime"
            r8.append(r3)
            java.lang.String r3 = r8.toString()
            r15.println(r3)
            r3 = 0
            r8.setLength(r3)
            r8.append(r14)
            java.lang.String r3 = "  Time on battery screen off: "
            r8.append(r3)
            long r5 = r9 / r16
            formatTimeMs(r8, r5)
            r8.append(r4)
            java.lang.String r3 = r7.formatRatioLocked(r9, r1)
            r8.append(r3)
            java.lang.String r3 = ") realtime, "
            r8.append(r3)
            long r5 = r34 / r16
            formatTimeMs(r8, r5)
            r8.append(r4)
            r5 = r34
            java.lang.String r3 = r7.formatRatioLocked(r5, r1)
            r8.append(r3)
            java.lang.String r3 = ") uptime"
            r8.append(r3)
            java.lang.String r3 = r8.toString()
            r15.println(r3)
            r3 = 0
            r8.setLength(r3)
            r8.append(r14)
            java.lang.String r3 = "  Time on battery screen doze: "
            r8.append(r3)
            long r5 = r41 / r16
            formatTimeMs(r8, r5)
            r8.append(r4)
            r5 = r41
            java.lang.String r3 = r7.formatRatioLocked(r5, r1)
            r8.append(r3)
            java.lang.String r3 = ")"
            r8.append(r3)
            java.lang.String r5 = r8.toString()
            r15.println(r5)
            r5 = 0
            r8.setLength(r5)
            r8.append(r14)
            java.lang.String r5 = "  Total run time: "
            r8.append(r5)
            long r5 = r11 / r16
            formatTimeMs(r8, r5)
            java.lang.String r5 = "realtime, "
            r8.append(r5)
            long r5 = r24 / r16
            formatTimeMs(r8, r5)
            java.lang.String r5 = "uptime"
            r8.append(r5)
            java.lang.String r5 = r8.toString()
            r15.println(r5)
            r5 = 0
            int r47 = (r30 > r5 ? 1 : (r30 == r5 ? 0 : -1))
            if (r47 < 0) goto L21e
            r5 = 0
            r8.setLength(r5)
            r8.append(r14)
            java.lang.String r5 = "  Battery time remaining: "
            r8.append(r5)
            long r5 = r30 / r16
            formatTimeMs(r8, r5)
            java.lang.String r5 = r8.toString()
            r15.println(r5)
        L21e:
            r5 = 0
            int r49 = (r32 > r5 ? 1 : (r32 == r5 ? 0 : -1))
            if (r49 < 0) goto L23c
            r5 = 0
            r8.setLength(r5)
            r8.append(r14)
            java.lang.String r5 = "  Charge time remaining: "
            r8.append(r5)
            long r5 = r32 / r16
            formatTimeMs(r8, r5)
            java.lang.String r5 = r8.toString()
            r15.println(r5)
        L23c:
            long r5 = r7.getUahDischarge(r13)
            r47 = 0
            int r49 = (r5 > r47 ? 1 : (r5 == r47 ? 0 : -1))
            r50 = 4652007308841189376(0x408f400000000000, double:1000.0)
            if (r49 < 0) goto L26e
            r52 = r9
            r9 = 0
            r8.setLength(r9)
            r8.append(r14)
            java.lang.String r9 = "  Discharge: "
            r8.append(r9)
            double r9 = (double) r5
            double r9 = r9 / r50
            java.lang.String r9 = formatCharge(r9)
            r8.append(r9)
            r8.append(r0)
            java.lang.String r9 = r8.toString()
            r15.println(r9)
            goto L270
        L26e:
            r52 = r9
        L270:
            long r9 = r7.getUahDischargeScreenOff(r13)
            r47 = 0
            int r49 = (r9 > r47 ? 1 : (r9 == r47 ? 0 : -1))
            if (r49 < 0) goto L29d
            r54 = r11
            r11 = 0
            r8.setLength(r11)
            r8.append(r14)
            java.lang.String r11 = "  Screen off discharge: "
            r8.append(r11)
            double r11 = (double) r9
            double r11 = r11 / r50
            java.lang.String r11 = formatCharge(r11)
            r8.append(r11)
            r8.append(r0)
            java.lang.String r11 = r8.toString()
            r15.println(r11)
            goto L29f
        L29d:
            r54 = r11
        L29f:
            long r11 = r7.getUahDischargeScreenDoze(r13)
            r47 = 0
            int r49 = (r11 > r47 ? 1 : (r11 == r47 ? 0 : -1))
            if (r49 < 0) goto L2ce
            r49 = r3
            r3 = 0
            r8.setLength(r3)
            r8.append(r14)
            java.lang.String r3 = "  Screen doze discharge: "
            r8.append(r3)
            r56 = r1
            double r1 = (double) r11
            double r1 = r1 / r50
            java.lang.String r1 = formatCharge(r1)
            r8.append(r1)
            r8.append(r0)
            java.lang.String r1 = r8.toString()
            r15.println(r1)
            goto L2d2
        L2ce:
            r56 = r1
            r49 = r3
        L2d2:
            long r2 = r5 - r9
            r47 = 0
            int r1 = (r2 > r47 ? 1 : (r2 == r47 ? 0 : -1))
            if (r1 < 0) goto L2fd
            r1 = 0
            r8.setLength(r1)
            r8.append(r14)
            java.lang.String r1 = "  Screen on discharge: "
            r8.append(r1)
            r58 = r5
            double r5 = (double) r2
            double r5 = r5 / r50
            java.lang.String r1 = formatCharge(r5)
            r8.append(r1)
            r8.append(r0)
            java.lang.String r1 = r8.toString()
            r15.println(r1)
            goto L2ff
        L2fd:
            r58 = r5
        L2ff:
            long r5 = r7.getUahDischargeLightDoze(r13)
            r47 = 0
            int r1 = (r5 > r47 ? 1 : (r5 == r47 ? 0 : -1))
            if (r1 < 0) goto L32c
            r1 = 0
            r8.setLength(r1)
            r8.append(r14)
            java.lang.String r1 = "  Device light doze discharge: "
            r8.append(r1)
            r60 = r2
            double r1 = (double) r5
            double r1 = r1 / r50
            java.lang.String r1 = formatCharge(r1)
            r8.append(r1)
            r8.append(r0)
            java.lang.String r1 = r8.toString()
            r15.println(r1)
            goto L32e
        L32c:
            r60 = r2
        L32e:
            long r2 = r7.getUahDischargeDeepDoze(r13)
            r47 = 0
            int r1 = (r2 > r47 ? 1 : (r2 == r47 ? 0 : -1))
            if (r1 < 0) goto L35b
            r1 = 0
            r8.setLength(r1)
            r8.append(r14)
            java.lang.String r1 = "  Device deep doze discharge: "
            r8.append(r1)
            r62 = r5
            double r5 = (double) r2
            double r5 = r5 / r50
            java.lang.String r1 = formatCharge(r5)
            r8.append(r1)
            r8.append(r0)
            java.lang.String r0 = r8.toString()
            r15.println(r0)
            goto L35d
        L35b:
            r62 = r5
        L35d:
            java.lang.String r0 = "  Start clock time: "
            r15.print(r0)
            long r0 = r234.getStartClockTime()
            java.lang.String r5 = "yyyy-MM-dd-HH-mm-ss"
            java.lang.CharSequence r0 = android.text.format.DateFormat.format(r5, r0)
            java.lang.String r0 = r0.toString()
            r15.println(r0)
            r5 = r36
            long r0 = r7.getScreenOnTime(r5, r13)
            r36 = r11
            long r11 = r7.getInteractiveTime(r5, r13)
            r50 = r9
            long r9 = r7.getPowerSaveModeEnabledTime(r5, r13)
            r64 = r2
            r3 = 1
            r66 = r9
            long r9 = r7.getDeviceIdleModeTime(r3, r5, r13)
            r2 = 2
            r68 = r9
            long r9 = r7.getDeviceIdleModeTime(r2, r5, r13)
            r70 = r9
            long r9 = r7.getDeviceIdlingTime(r3, r5, r13)
            r72 = r4
            long r3 = r7.getDeviceIdlingTime(r2, r5, r13)
            r75 = r3
            long r2 = r7.getPhoneOnTime(r5, r13)
            long r77 = r7.getGlobalWifiRunningTime(r5, r13)
            long r79 = r7.getWifiOnTime(r5, r13)
            r4 = 0
            r8.setLength(r4)
            r8.append(r14)
            java.lang.String r4 = "  Screen on: "
            r8.append(r4)
            r81 = r2
            long r2 = r0 / r16
            formatTimeMs(r8, r2)
            r4 = r72
            r8.append(r4)
            r2 = r56
            r56 = r9
            java.lang.String r9 = r7.formatRatioLocked(r0, r2)
            r8.append(r9)
            java.lang.String r10 = ") "
            r8.append(r10)
            int r9 = r7.getScreenOnCount(r13)
            r8.append(r9)
            java.lang.String r9 = "x, Interactive: "
            r8.append(r9)
            r72 = r10
            long r9 = r11 / r16
            formatTimeMs(r8, r9)
            r8.append(r4)
            java.lang.String r9 = r7.formatRatioLocked(r11, r2)
            r8.append(r9)
            r9 = r49
            r8.append(r9)
            java.lang.String r10 = r8.toString()
            r15.println(r10)
            r10 = 0
            r8.setLength(r10)
            r8.append(r14)
            java.lang.String r10 = "  Screen brightnesses:"
            r8.append(r10)
            r10 = 0
            r49 = 0
            r83 = r11
            r11 = r49
        L415:
            r12 = 5
            r85 = r2
            java.lang.String r2 = " "
            if (r11 >= r12) goto L45c
            r49 = r9
            r3 = r10
            long r9 = r7.getScreenBrightnessTime(r11, r5, r13)
            r47 = 0
            int r12 = (r9 > r47 ? 1 : (r9 == r47 ? 0 : -1))
            if (r12 != 0) goto L42d
            r10 = r3
            r2 = r49
            goto L456
        L42d:
            java.lang.String r12 = "\n    "
            r8.append(r12)
            r8.append(r14)
            r3 = 1
            java.lang.String[] r12 = android.os.BatteryStats.SCREEN_BRIGHTNESS_NAMES
            r12 = r12[r11]
            r8.append(r12)
            r8.append(r2)
            r12 = r3
            long r2 = r9 / r16
            formatTimeMs(r8, r2)
            r8.append(r4)
            java.lang.String r2 = r7.formatRatioLocked(r9, r0)
            r8.append(r2)
            r2 = r49
            r8.append(r2)
            r10 = r12
        L456:
            int r11 = r11 + 1
            r9 = r2
            r2 = r85
            goto L415
        L45c:
            r3 = r10
            java.lang.String r10 = " (no activity)"
            if (r3 != 0) goto L464
            r8.append(r10)
        L464:
            java.lang.String r11 = r8.toString()
            r15.println(r11)
            r47 = 0
            int r11 = (r66 > r47 ? 1 : (r66 == r47 ? 0 : -1))
            if (r11 == 0) goto L49f
            r11 = 0
            r8.setLength(r11)
            r8.append(r14)
            java.lang.String r11 = "  Power save mode enabled: "
            r8.append(r11)
            long r12 = r66 / r16
            formatTimeMs(r8, r12)
            r8.append(r4)
            r12 = r85
            r228 = r0
            r0 = r66
            r66 = r228
            java.lang.String r11 = r7.formatRatioLocked(r0, r12)
            r8.append(r11)
            r8.append(r9)
            java.lang.String r11 = r8.toString()
            r15.println(r11)
            goto L4a7
        L49f:
            r12 = r85
            r228 = r0
            r0 = r66
            r66 = r228
        L4a7:
            r47 = 0
            int r11 = (r56 > r47 ? 1 : (r56 == r47 ? 0 : -1))
            r85 = r3
            java.lang.String r3 = "x"
            if (r11 == 0) goto L4ed
            r11 = 0
            r8.setLength(r11)
            r8.append(r14)
            java.lang.String r11 = "  Device light idling: "
            r8.append(r11)
            r86 = r0
            long r0 = r56 / r16
            formatTimeMs(r8, r0)
            r8.append(r4)
            r0 = r56
            java.lang.String r11 = r7.formatRatioLocked(r0, r12)
            r8.append(r11)
            r11 = r72
            r8.append(r11)
            r72 = r10
            r0 = 1
            r1 = r238
            int r10 = r7.getDeviceIdlingCount(r0, r1)
            r8.append(r10)
            r8.append(r3)
            java.lang.String r0 = r8.toString()
            r15.println(r0)
            goto L4f5
        L4ed:
            r86 = r0
            r11 = r72
            r1 = r238
            r72 = r10
        L4f5:
            r47 = 0
            int r0 = (r68 > r47 ? 1 : (r68 == r47 ? 0 : -1))
            if (r0 == 0) goto L53c
            r0 = 0
            r8.setLength(r0)
            r8.append(r14)
            java.lang.String r0 = "  Idle mode light time: "
            r8.append(r0)
            r88 = r9
            long r9 = r68 / r16
            formatTimeMs(r8, r9)
            r8.append(r4)
            r9 = r68
            java.lang.String r0 = r7.formatRatioLocked(r9, r12)
            r8.append(r0)
            r8.append(r11)
            r0 = 1
            int r9 = r7.getDeviceIdleModeCount(r0, r1)
            r8.append(r9)
            r8.append(r3)
            java.lang.String r9 = " -- longest "
            r8.append(r9)
            long r9 = r7.getLongestDeviceIdleModeTime(r0)
            formatTimeMs(r8, r9)
            java.lang.String r0 = r8.toString()
            r15.println(r0)
            goto L53e
        L53c:
            r88 = r9
        L53e:
            r9 = 0
            int r0 = (r75 > r9 ? 1 : (r75 == r9 ? 0 : -1))
            if (r0 == 0) goto L576
            r0 = 0
            r8.setLength(r0)
            r8.append(r14)
            java.lang.String r0 = "  Device full idling: "
            r8.append(r0)
            long r9 = r75 / r16
            formatTimeMs(r8, r9)
            r8.append(r4)
            r9 = r75
            java.lang.String r0 = r7.formatRatioLocked(r9, r12)
            r8.append(r0)
            r8.append(r11)
            r0 = 2
            int r9 = r7.getDeviceIdlingCount(r0, r1)
            r8.append(r9)
            r8.append(r3)
            java.lang.String r0 = r8.toString()
            r15.println(r0)
        L576:
            r9 = 0
            int r0 = (r70 > r9 ? 1 : (r70 == r9 ? 0 : -1))
            if (r0 == 0) goto L5ba
            r0 = 0
            r8.setLength(r0)
            r8.append(r14)
            java.lang.String r0 = "  Idle mode full time: "
            r8.append(r0)
            long r9 = r70 / r16
            formatTimeMs(r8, r9)
            r8.append(r4)
            r9 = r70
            java.lang.String r0 = r7.formatRatioLocked(r9, r12)
            r8.append(r0)
            r8.append(r11)
            r0 = 2
            int r9 = r7.getDeviceIdleModeCount(r0, r1)
            r8.append(r9)
            r8.append(r3)
            java.lang.String r9 = " -- longest "
            r8.append(r9)
            long r9 = r7.getLongestDeviceIdleModeTime(r0)
            formatTimeMs(r8, r9)
            java.lang.String r0 = r8.toString()
            r15.println(r0)
        L5ba:
            r9 = 0
            int r0 = (r81 > r9 ? 1 : (r81 == r9 ? 0 : -1))
            if (r0 == 0) goto L5eb
            r0 = 0
            r8.setLength(r0)
            r8.append(r14)
            java.lang.String r0 = "  Active phone call: "
            r8.append(r0)
            long r9 = r81 / r16
            formatTimeMs(r8, r9)
            r8.append(r4)
            r9 = r81
            java.lang.String r0 = r7.formatRatioLocked(r9, r12)
            r8.append(r0)
            r8.append(r11)
            int r0 = r7.getPhoneOnCount(r1)
            r8.append(r0)
            r8.append(r3)
            goto L5ed
        L5eb:
            r9 = r81
        L5ed:
            int r0 = r7.getNumConnectivityChange(r1)
            if (r0 == 0) goto L601
            r236.print(r237)
            r81 = r3
            java.lang.String r3 = "  Connectivity changes: "
            r15.print(r3)
            r15.println(r0)
            goto L603
        L601:
            r81 = r3
        L603:
            r89 = 0
            r91 = 0
            java.util.ArrayList r3 = new java.util.ArrayList
            r3.<init>()
            r82 = 0
            r93 = r0
            r0 = r82
        L612:
            r94 = r9
            r9 = r39
            if (r0 >= r9) goto L695
            r10 = r38
            java.lang.Object r38 = r10.valueAt(r0)
            android.os.BatteryStats$Uid r38 = (android.os.BatteryStats.Uid) r38
            r39 = r9
            android.util.ArrayMap r9 = r38.getWakelockStats()
            int r82 = r9.size()
            r96 = r10
            r10 = 1
            int r82 = r82 + (-1)
            r10 = r82
        L632:
            if (r10 < 0) goto L687
            java.lang.Object r82 = r9.valueAt(r10)
            r97 = r11
            r11 = r82
            android.os.BatteryStats$Uid$Wakelock r11 = (android.os.BatteryStats.Uid.Wakelock) r11
            r82 = r2
            r98 = r12
            r2 = 1
            android.os.BatteryStats$Timer r12 = r11.getWakeTime(r2)
            if (r12 == 0) goto L64f
            long r100 = r12.getTotalTimeLocked(r5, r1)
            long r89 = r89 + r100
        L64f:
            r2 = 0
            android.os.BatteryStats$Timer r13 = r11.getWakeTime(r2)
            if (r13 == 0) goto L67e
            long r106 = r13.getTotalTimeLocked(r5, r1)
            r47 = 0
            int r2 = (r106 > r47 ? 1 : (r106 == r47 ? 0 : -1))
            if (r2 <= 0) goto L67e
            if (r-17 >= 0) goto L67c
            android.os.BatteryStats$TimerEntry r2 = new android.os.BatteryStats$TimerEntry
            java.lang.Object r100 = r9.keyAt(r10)
            r101 = r100
            java.lang.String r101 = (java.lang.String) r101
            int r102 = r38.getUid()
            r100 = r2
            r103 = r13
            r104 = r106
            r100.<init>(r101, r102, r103, r104)
            r3.add(r2)
        L67c:
            long r91 = r91 + r106
        L67e:
            int r10 = r10 + (-1)
            r2 = r82
            r11 = r97
            r12 = r98
            goto L632
        L687:
            r82 = r2
            r97 = r11
            r98 = r12
            int r0 = r0 + 1
            r9 = r94
            r38 = r96
            goto L612
        L695:
            r82 = r2
            r39 = r9
            r97 = r11
            r98 = r12
            r96 = r38
            r0 = 0
            long r12 = r7.getNetworkActivityBytes(r0, r1)
            r2 = 1
            long r9 = r7.getNetworkActivityBytes(r2, r1)
            r38 = r3
            r11 = 2
            long r2 = r7.getNetworkActivityBytes(r11, r1)
            r11 = 3
            r100 = r9
            long r9 = r7.getNetworkActivityBytes(r11, r1)
            r102 = r9
            long r9 = r7.getNetworkActivityPackets(r0, r1)
            r104 = r9
            r0 = 1
            long r9 = r7.getNetworkActivityPackets(r0, r1)
            r106 = r9
            r0 = 2
            long r9 = r7.getNetworkActivityPackets(r0, r1)
            r108 = r9
            long r9 = r7.getNetworkActivityPackets(r11, r1)
            r11 = 4
            r110 = r9
            long r9 = r7.getNetworkActivityBytes(r11, r1)
            r112 = r9
            r11 = 5
            long r9 = r7.getNetworkActivityBytes(r11, r1)
            r49 = r0
            r0 = r97
            r47 = 0
            int r74 = (r89 > r47 ? 1 : (r89 == r47 ? 0 : -1))
            if (r74 == 0) goto L706
            r11 = 0
            r8.setLength(r11)
            r8.append(r14)
            java.lang.String r11 = "  Total full wakelock time: "
            r8.append(r11)
            long r114 = r89 + r18
            r116 = r2
            long r2 = r114 / r16
            formatTimeMsNoSpace(r8, r2)
            java.lang.String r2 = r8.toString()
            r15.println(r2)
            goto L708
        L706:
            r116 = r2
        L708:
            r2 = 0
            int r11 = (r91 > r2 ? 1 : (r91 == r2 ? 0 : -1))
            if (r11 == 0) goto L728
            r2 = 0
            r8.setLength(r2)
            r8.append(r14)
            java.lang.String r2 = "  Total partial wakelock time: "
            r8.append(r2)
            long r2 = r91 + r18
            long r2 = r2 / r16
            formatTimeMsNoSpace(r8, r2)
            java.lang.String r2 = r8.toString()
            r15.println(r2)
        L728:
            long r114 = r7.getWifiMulticastWakelockTime(r5, r1)
            int r3 = r7.getWifiMulticastWakelockCount(r1)
            r47 = 0
            int r2 = (r114 > r47 ? 1 : (r114 == r47 ? 0 : -1))
            if (r2 == 0) goto L76a
            r2 = 0
            r8.setLength(r2)
            r8.append(r14)
            java.lang.String r2 = "  Total WiFi Multicast wakelock Count: "
            r8.append(r2)
            r8.append(r3)
            java.lang.String r2 = r8.toString()
            r15.println(r2)
            r2 = 0
            r8.setLength(r2)
            r8.append(r14)
            java.lang.String r2 = "  Total WiFi Multicast wakelock time: "
            r8.append(r2)
            long r118 = r114 + r18
            r97 = r3
            long r2 = r118 / r16
            formatTimeMsNoSpace(r8, r2)
            java.lang.String r2 = r8.toString()
            r15.println(r2)
            goto L76c
        L76a:
            r97 = r3
        L76c:
            int r3 = r234.getDisplayCount()
            r2 = 1
            if (r3 <= r2) goto L8d7
            java.lang.String r11 = ""
            r15.println(r11)
            r236.print(r237)
            r11 = 0
            r8.setLength(r11)
            r8.append(r14)
            java.lang.String r11 = "  MULTI-DISPLAY POWER SUMMARY START"
            r8.append(r11)
            java.lang.String r11 = r8.toString()
            r15.println(r11)
            r11 = 0
        L78f:
            if (r11 >= r3) goto L8ae
            r2 = 0
            r8.setLength(r2)
            r8.append(r14)
            java.lang.String r2 = "  Display "
            r8.append(r2)
            r8.append(r11)
            java.lang.String r2 = " Statistics:"
            r8.append(r2)
            java.lang.String r2 = r8.toString()
            r15.println(r2)
            r118 = r3
            long r2 = r7.getDisplayScreenOnTime(r11, r5)
            r119 = r11
            r11 = 0
            r8.setLength(r11)
            r8.append(r14)
            java.lang.String r11 = "    Screen on: "
            r8.append(r11)
            r120 = r9
            long r9 = r2 / r16
            formatTimeMs(r8, r9)
            r8.append(r4)
            r9 = r98
            java.lang.String r11 = r7.formatRatioLocked(r2, r9)
            r8.append(r11)
            r8.append(r0)
            java.lang.String r11 = r8.toString()
            r15.println(r11)
            r11 = 0
            r8.setLength(r11)
            java.lang.String r11 = "    Screen brightness levels:"
            r8.append(r11)
            r11 = 0
            r85 = 0
            r228 = r85
            r85 = r11
            r11 = r228
        L7ef:
            r98 = r12
            r12 = 5
            if (r11 >= r12) goto L848
            r13 = r11
            r12 = r119
            r74 = r0
            long r0 = r7.getDisplayScreenBrightnessTime(r12, r13, r5)
            r47 = 0
            int r119 = (r0 > r47 ? 1 : (r0 == r47 ? 0 : -1))
            if (r119 != 0) goto L80a
            r122 = r9
            r11 = r82
            r9 = r88
            goto L836
        L80a:
            r85 = 1
            java.lang.String r11 = "\n      "
            r8.append(r11)
            r8.append(r14)
            java.lang.String[] r11 = android.os.BatteryStats.SCREEN_BRIGHTNESS_NAMES
            r11 = r11[r13]
            r8.append(r11)
            r11 = r82
            r8.append(r11)
            r122 = r9
            long r9 = r0 / r16
            formatTimeMs(r8, r9)
            r8.append(r4)
            java.lang.String r9 = r7.formatRatioLocked(r0, r2)
            r8.append(r9)
            r9 = r88
            r8.append(r9)
        L836:
            int r0 = r13 + 1
            r1 = r238
            r88 = r9
            r82 = r11
            r119 = r12
            r12 = r98
            r9 = r122
            r11 = r0
            r0 = r74
            goto L7ef
        L848:
            r74 = r0
            r122 = r9
            r13 = r11
            r11 = r82
            r9 = r88
            r12 = r119
            r47 = 0
            if (r85 != 0) goto L85d
            r10 = r72
            r8.append(r10)
            goto L85f
        L85d:
            r10 = r72
        L85f:
            java.lang.String r0 = r8.toString()
            r15.println(r0)
            long r0 = r7.getDisplayScreenDozeTime(r12, r5)
            r13 = 0
            r8.setLength(r13)
            r8.append(r14)
            java.lang.String r13 = "    Screen Doze: "
            r8.append(r13)
            r124 = r2
            long r2 = r0 / r16
            formatTimeMs(r8, r2)
            r8.append(r4)
            r2 = r122
            java.lang.String r13 = r7.formatRatioLocked(r0, r2)
            r8.append(r13)
            r13 = r74
            r8.append(r13)
            r122 = r0
            java.lang.String r0 = r8.toString()
            r15.println(r0)
            int r0 = r12 + 1
            r1 = r238
            r88 = r9
            r72 = r10
            r82 = r11
            r9 = r120
            r11 = r0
            r0 = r13
            r12 = r98
            r98 = r2
            r3 = r118
            r2 = 1
            goto L78f
        L8ae:
            r118 = r3
            r120 = r9
            r10 = r72
            r9 = r88
            r2 = r98
            r47 = 0
            r98 = r12
            r13 = r0
            r12 = r11
            r11 = r82
            r236.print(r237)
            r0 = 0
            r8.setLength(r0)
            r8.append(r14)
            java.lang.String r0 = "  MULTI-DISPLAY POWER SUMMARY END"
            r8.append(r0)
            java.lang.String r0 = r8.toString()
            r15.println(r0)
            goto L8e8
        L8d7:
            r118 = r3
            r120 = r9
            r10 = r72
            r11 = r82
            r9 = r88
            r2 = r98
            r47 = 0
            r98 = r12
            r13 = r0
        L8e8:
            java.lang.String r0 = ""
            r15.println(r0)
            r236.print(r237)
            r0 = 0
            r8.setLength(r0)
            r8.append(r14)
            java.lang.String r0 = "  CONNECTIVITY POWER SUMMARY START"
            r8.append(r0)
            java.lang.String r0 = r8.toString()
            r15.println(r0)
            r236.print(r237)
            r0 = 0
            r8.setLength(r0)
            r8.append(r14)
            java.lang.String r0 = "  Logging duration for connectivity statistics: "
            r8.append(r0)
            long r0 = r2 / r16
            formatTimeMs(r8, r0)
            java.lang.String r0 = r8.toString()
            r15.println(r0)
            r0 = 0
            r8.setLength(r0)
            r8.append(r14)
            java.lang.String r0 = "  Cellular Statistics:"
            r8.append(r0)
            java.lang.String r0 = r8.toString()
            r15.println(r0)
            r236.print(r237)
            r12 = 0
            r8.setLength(r12)
            r8.append(r14)
            java.lang.String r0 = "     Cellular kernel active time: "
            r8.append(r0)
            r1 = r238
            r82 = r11
            long r11 = r7.getMobileRadioActiveTime(r5, r1)
            long r0 = r11 / r16
            formatTimeMs(r8, r0)
            r8.append(r4)
            java.lang.String r0 = r7.formatRatioLocked(r11, r2)
            r8.append(r0)
            r8.append(r9)
            java.lang.String r0 = r8.toString()
            r15.println(r0)
            android.os.BatteryStats$ControllerActivityCounter r72 = r234.getModemControllerActivity()
            java.lang.String r74 = "Cellular"
            r1 = r13
            r88 = r93
            r13 = r96
            r228 = r66
            r66 = r86
            r86 = r56
            r56 = r228
            r0 = r234
            r126 = r22
            r22 = r11
            r11 = r2
            r3 = r238
            r2 = r1
            r1 = r236
            r49 = r9
            r13 = r82
            r93 = r94
            r128 = r116
            r82 = 1
            r9 = r2
            r2 = r8
            r95 = r10
            r131 = r49
            r130 = r81
            r49 = r97
            r81 = r118
            r10 = r3
            r3 = r237
            r97 = r9
            r9 = r4
            r4 = r74
            r122 = r11
            r133 = r39
            r39 = r46
            r11 = r5
            r6 = 0
            r5 = r72
            r72 = r9
            r40 = r45
            r9 = r6
            r6 = r238
            r0.printControllerActivity(r1, r2, r3, r4, r5, r6)
            java.lang.StringBuilder r0 = new java.lang.StringBuilder
            r0.<init>()
            java.lang.StringBuilder r0 = r0.append(r14)
            java.lang.String r1 = "     "
            java.lang.StringBuilder r0 = r0.append(r1)
            java.lang.String r3 = r0.toString()
            r0 = r234
            r1 = r236
            r4 = r20
            r0.printCellularPerRatBreakdown(r1, r2, r3, r4)
            java.lang.String r0 = "     Cellular data received: "
            r15.print(r0)
            r5 = r98
            java.lang.String r0 = r7.formatBytesLocked(r5)
            r15.println(r0)
            java.lang.String r0 = "     Cellular data sent: "
            r15.print(r0)
            r3 = r100
            java.lang.String r0 = r7.formatBytesLocked(r3)
            r15.println(r0)
            java.lang.String r0 = "     Cellular packets received: "
            r15.print(r0)
            r1 = r104
            r15.println(r1)
            java.lang.String r0 = "     Cellular packets sent: "
            r15.print(r0)
            r9 = r106
            r15.println(r9)
            r0 = 0
            r8.setLength(r0)
            r8.append(r14)
            java.lang.String r0 = "     Cellular Radio Access Technology:"
            r8.append(r0)
            r0 = 0
            r46 = 0
            r1 = r46
        La10:
            int r2 = android.os.BatteryStats.NUM_DATA_CONNECTION_TYPES
            if (r1 >= r2) goto La6f
            r100 = r3
            r98 = r9
            r10 = r238
            long r2 = r7.getPhoneDataConnectionTime(r1, r11, r10)
            r46 = 0
            int r4 = (r2 > r46 ? 1 : (r2 == r46 ? 0 : -1))
            if (r4 != 0) goto La2d
            r106 = r5
            r9 = r72
            r4 = r97
            r5 = r122
            goto La60
        La2d:
            java.lang.String r4 = "\n       "
            r8.append(r4)
            r8.append(r14)
            r0 = 1
            java.lang.String[] r4 = android.os.BatteryStats.DATA_CONNECTION_NAMES
            int r9 = r4.length
            if (r1 >= r9) goto La3e
            r4 = r4[r1]
            goto La40
        La3e:
            java.lang.String r4 = "ERROR"
        La40:
            r8.append(r4)
            r8.append(r13)
            r106 = r5
            long r4 = r2 / r16
            formatTimeMs(r8, r4)
            r9 = r72
            r8.append(r9)
            r5 = r122
            java.lang.String r4 = r7.formatRatioLocked(r2, r5)
            r8.append(r4)
            r4 = r97
            r8.append(r4)
        La60:
            int r1 = r1 + 1
            r97 = r4
            r122 = r5
            r72 = r9
            r9 = r98
            r3 = r100
            r5 = r106
            goto La10
        La6f:
            r100 = r3
            r106 = r5
            r98 = r9
            r9 = r72
            r4 = r97
            r5 = r122
            r10 = r238
            if (r0 != 0) goto La85
            r3 = r95
            r8.append(r3)
            goto La87
        La85:
            r3 = r95
        La87:
            java.lang.String r1 = r8.toString()
            r15.println(r1)
            r1 = 0
            r8.setLength(r1)
            r8.append(r14)
            java.lang.String r1 = "     Cellular Rx signal strength (RSRP):"
            r8.append(r1)
            java.lang.String r1 = "very poor (less than -128dBm): "
            java.lang.String r2 = "poor (-128dBm to -118dBm): "
            r46 = r0
            java.lang.String r0 = "moderate (-118dBm to -108dBm): "
            java.lang.String r15 = "good (-108dBm to -98dBm): "
            r72 = r3
            java.lang.String r3 = "great (greater than -98dBm): "
            java.lang.String[] r0 = new java.lang.String[]{r1, r2, r0, r15, r3}
            r15 = r0
            r0 = 0
            int r1 = android.telephony.CellSignalStrength.getNumSignalStrengthLevels()
            int r2 = r15.length
            int r3 = java.lang.Math.min(r1, r2)
            r1 = 0
            r46 = r0
        Labb:
            if (r1 >= r3) goto Lafc
            r74 = r3
            long r2 = r7.getPhoneSignalStrengthTime(r1, r11, r10)
            r47 = 0
            int r0 = (r2 > r47 ? 1 : (r2 == r47 ? 0 : -1))
            if (r0 != 0) goto Lacc
            r116 = r11
            goto Laf3
        Lacc:
            java.lang.String r0 = "\n       "
            r8.append(r0)
            r8.append(r14)
            r0 = 1
            r46 = r0
            r0 = r15[r1]
            r8.append(r0)
            r8.append(r13)
            r116 = r11
            long r10 = r2 / r16
            formatTimeMs(r8, r10)
            r8.append(r9)
            java.lang.String r0 = r7.formatRatioLocked(r2, r5)
            r8.append(r0)
            r8.append(r4)
        Laf3:
            int r1 = r1 + 1
            r10 = r238
            r3 = r74
            r11 = r116
            goto Labb
        Lafc:
            r74 = r3
            r116 = r11
            if (r46 != 0) goto Lb08
            r3 = r72
            r8.append(r3)
            goto Lb0a
        Lb08:
            r3 = r72
        Lb0a:
            java.lang.String r0 = r8.toString()
            r11 = r236
            r11.println(r0)
            r236.print(r237)
            r0 = 0
            r8.setLength(r0)
            r8.append(r14)
            java.lang.String r0 = "  Wifi Statistics:"
            r8.append(r0)
            java.lang.String r0 = r8.toString()
            r11.println(r0)
            r236.print(r237)
            r0 = 0
            r8.setLength(r0)
            r8.append(r14)
            java.lang.String r0 = "     Wifi kernel active time: "
            r8.append(r0)
            r10 = r238
            r82 = r13
            r1 = r116
            long r12 = r7.getWifiActiveTime(r1, r10)
            long r0 = r12 / r16
            formatTimeMs(r8, r0)
            r8.append(r9)
            java.lang.String r0 = r7.formatRatioLocked(r12, r5)
            r8.append(r0)
            r2 = r131
            r8.append(r2)
            java.lang.String r0 = r8.toString()
            r11.println(r0)
            android.os.BatteryStats$ControllerActivityCounter r72 = r234.getWifiControllerActivity()
            java.lang.String r85 = "WiFi"
            r0 = r234
            r122 = r12
            r12 = r116
            r1 = r236
            r95 = r15
            r15 = r2
            r2 = r8
            r131 = r15
            r15 = r3
            r3 = r237
            r97 = r15
            r15 = r4
            r4 = r85
            r134 = r5
            r5 = r72
            r6 = r238
            r0.printControllerActivity(r1, r2, r3, r4, r5, r6)
            java.lang.String r0 = "     Wifi data received: "
            r11.print(r0)
            r5 = r128
            java.lang.String r0 = r7.formatBytesLocked(r5)
            r11.println(r0)
            java.lang.String r0 = "     Wifi data sent: "
            r11.print(r0)
            r3 = r102
            java.lang.String r0 = r7.formatBytesLocked(r3)
            r11.println(r0)
            java.lang.String r0 = "     Wifi packets received: "
            r11.print(r0)
            r1 = r108
            r11.println(r1)
            java.lang.String r0 = "     Wifi packets sent: "
            r11.print(r0)
            r5 = r110
            r11.println(r5)
            r0 = 0
            r8.setLength(r0)
            r8.append(r14)
            java.lang.String r0 = "     Wifi states:"
            r8.append(r0)
            r0 = 0
            r46 = 0
            r1 = r46
        Lbc5:
            r2 = 8
            if (r1 >= r2) goto Lc13
            r102 = r3
            long r2 = r7.getWifiStateTime(r1, r12, r10)
            r46 = 0
            int r4 = (r2 > r46 ? 1 : (r2 == r46 ? 0 : -1))
            if (r4 != 0) goto Lbdc
            r110 = r5
            r4 = r82
            r5 = r134
            goto Lc08
        Lbdc:
            java.lang.String r4 = "\n       "
            r8.append(r4)
            r0 = 1
            java.lang.String[] r4 = android.os.BatteryStats.WIFI_STATE_NAMES
            r4 = r4[r1]
            r8.append(r4)
            r4 = r82
            r8.append(r4)
            r110 = r5
            long r5 = r2 / r16
            formatTimeMs(r8, r5)
            r8.append(r9)
            r46 = r0
            r5 = r134
            java.lang.String r0 = r7.formatRatioLocked(r2, r5)
            r8.append(r0)
            r8.append(r15)
            r0 = r46
        Lc08:
            int r1 = r1 + 1
            r82 = r4
            r134 = r5
            r3 = r102
            r5 = r110
            goto Lbc5
        Lc13:
            r102 = r3
            r110 = r5
            r4 = r82
            r5 = r134
            if (r0 != 0) goto Lc23
            r1 = r97
            r8.append(r1)
            goto Lc25
        Lc23:
            r1 = r97
        Lc25:
            java.lang.String r2 = r8.toString()
            r11.println(r2)
            r2 = 0
            r8.setLength(r2)
            r8.append(r14)
            java.lang.String r2 = "     Wifi supplicant states:"
            r8.append(r2)
            r0 = 0
            r2 = 0
        Lc3a:
            r3 = 13
            if (r2 >= r3) goto Lc7e
            r3 = r0
            r72 = r1
            long r0 = r7.getWifiSupplStateTime(r2, r12, r10)
            r46 = 0
            int r82 = (r0 > r46 ? 1 : (r0 == r46 ? 0 : -1))
            if (r82 != 0) goto Lc4f
            r0 = r3
            r82 = r4
            goto Lc75
        Lc4f:
            java.lang.String r10 = "\n       "
            r8.append(r10)
            r3 = 1
            java.lang.String[] r10 = android.os.BatteryStats.WIFI_SUPPL_STATE_NAMES
            r10 = r10[r2]
            r8.append(r10)
            r8.append(r4)
            r10 = r3
            r82 = r4
            long r3 = r0 / r16
            formatTimeMs(r8, r3)
            r8.append(r9)
            java.lang.String r3 = r7.formatRatioLocked(r0, r5)
            r8.append(r3)
            r8.append(r15)
            r0 = r10
        Lc75:
            int r2 = r2 + 1
            r10 = r238
            r1 = r72
            r4 = r82
            goto Lc3a
        Lc7e:
            r3 = r0
            r72 = r1
            r82 = r4
            if (r3 != 0) goto Lc8b
            r0 = r72
            r8.append(r0)
            goto Lc8d
        Lc8b:
            r0 = r72
        Lc8d:
            java.lang.String r1 = r8.toString()
            r11.println(r1)
            r1 = 0
            r8.setLength(r1)
            r8.append(r14)
            java.lang.String r1 = "     Wifi Rx signal strength (RSSI):"
            r8.append(r1)
            java.lang.String r1 = "very poor (less than -88.75dBm): "
            java.lang.String r2 = "poor (-88.75 to -77.5dBm): "
            java.lang.String r4 = "moderate (-77.5dBm to -66.25dBm): "
            java.lang.String r10 = "good (-66.25dBm to -55dBm): "
            r46 = r3
            java.lang.String r3 = "great (greater than -55dBm): "
            java.lang.String[] r1 = new java.lang.String[]{r1, r2, r4, r10, r3}
            r10 = r1
            r1 = 0
            int r2 = r10.length
            r4 = 5
            int r3 = java.lang.Math.min(r4, r2)
            r2 = 0
            r46 = r1
        Lcbc:
            if (r2 >= r3) goto Ld09
            r1 = r238
            r134 = r5
            long r4 = r7.getWifiSignalStrengthTime(r2, r12, r1)
            r47 = 0
            int r6 = (r4 > r47 ? 1 : (r4 == r47 ? 0 : -1))
            if (r6 != 0) goto Lcd3
            r72 = r3
            r116 = r12
            r12 = r134
            goto Ld00
        Lcd3:
            java.lang.String r6 = "\n    "
            r8.append(r6)
            r8.append(r14)
            r6 = 1
            r72 = r3
            java.lang.String r3 = "     "
            r8.append(r3)
            r3 = r10[r2]
            r8.append(r3)
            r116 = r12
            long r12 = r4 / r16
            formatTimeMs(r8, r12)
            r8.append(r9)
            r12 = r134
            java.lang.String r3 = r7.formatRatioLocked(r4, r12)
            r8.append(r3)
            r8.append(r15)
            r46 = r6
        Ld00:
            int r2 = r2 + 1
            r5 = r12
            r3 = r72
            r12 = r116
            r4 = 5
            goto Lcbc
        Ld09:
            r1 = r238
            r72 = r3
            r116 = r12
            r12 = r5
            if (r46 != 0) goto Ld15
            r8.append(r0)
        Ld15:
            java.lang.String r0 = r8.toString()
            r11.println(r0)
            r236.print(r237)
            r0 = 0
            r8.setLength(r0)
            r8.append(r14)
            java.lang.String r2 = "  GPS Statistics:"
            r8.append(r2)
            java.lang.String r2 = r8.toString()
            r11.println(r2)
            r8.setLength(r0)
            r8.append(r14)
            java.lang.String r0 = "     GPS signal quality (Top 4 Average CN0):"
            r8.append(r0)
            java.lang.String r0 = "poor (less than 20 dBHz): "
            java.lang.String r2 = "good (greater than 20 dBHz): "
            java.lang.String[] r0 = new java.lang.String[]{r0, r2}
            r6 = r0
            int r0 = r6.length
            r5 = 2
            int r4 = java.lang.Math.min(r5, r0)
            r0 = 0
        Ld4d:
            if (r0 >= r4) goto Ld88
            r73 = r6
            r2 = r116
            long r5 = r7.getGpsSignalQualityTime(r0, r2, r1)
            r97 = r4
            java.lang.String r4 = "\n    "
            r8.append(r4)
            r8.append(r14)
            java.lang.String r4 = "  "
            r8.append(r4)
            r4 = r73[r0]
            r8.append(r4)
            long r1 = r5 / r16
            formatTimeMs(r8, r1)
            r8.append(r9)
            java.lang.String r1 = r7.formatRatioLocked(r5, r12)
            r8.append(r1)
            r8.append(r15)
            int r0 = r0 + 1
            r1 = r238
            r6 = r73
            r4 = r97
            r5 = 2
            goto Ld4d
        Ld88:
            r97 = r4
            r73 = r6
            java.lang.String r0 = r8.toString()
            r11.println(r0)
            long r5 = r234.getGpsBatteryDrainMaMs()
            r0 = 0
            int r2 = (r5 > r0 ? 1 : (r5 == r0 ? 0 : -1))
            if (r2 <= 0) goto Ldcd
            r236.print(r237)
            r0 = 0
            r8.setLength(r0)
            r8.append(r14)
            java.lang.String r0 = "     GPS Battery Drain: "
            r8.append(r0)
            java.text.DecimalFormat r0 = new java.text.DecimalFormat
            java.lang.String r1 = "#.##"
            r0.<init>(r1)
            double r1 = (double) r5
            r3 = 4704985352480227328(0x414b774000000000, double:3600000.0)
            double r1 = r1 / r3
            java.lang.String r0 = r0.format(r1)
            r8.append(r0)
            java.lang.String r0 = "mAh"
            r8.append(r0)
            java.lang.String r0 = r8.toString()
            r11.println(r0)
        Ldcd:
            r236.print(r237)
            r0 = 0
            r8.setLength(r0)
            r8.append(r14)
            java.lang.String r0 = "  CONNECTIVITY POWER SUMMARY END"
            r8.append(r0)
            java.lang.String r0 = r8.toString()
            r11.println(r0)
            java.lang.String r0 = ""
            r11.println(r0)
            r236.print(r237)
            java.lang.String r0 = "  Bluetooth total received: "
            r11.print(r0)
            r3 = r112
            java.lang.String r0 = r7.formatBytesLocked(r3)
            r11.print(r0)
            java.lang.String r0 = ", sent: "
            r11.print(r0)
            r1 = r120
            java.lang.String r0 = r7.formatBytesLocked(r1)
            r11.println(r0)
            r0 = r238
            r2 = r116
            long r116 = r7.getBluetoothScanTime(r2, r0)
            r134 = r12
            long r12 = r116 / r16
            r1 = 0
            r8.setLength(r1)
            r8.append(r14)
            java.lang.String r1 = "  Bluetooth scan time: "
            r8.append(r1)
            formatTimeMs(r8, r12)
            java.lang.String r1 = r8.toString()
            r11.println(r1)
            android.os.BatteryStats$ControllerActivityCounter r116 = r234.getBluetoothControllerActivity()
            java.lang.String r4 = "Bluetooth"
            r0 = r234
            r117 = r10
            r10 = r238
            r1 = r236
            r124 = r12
            r12 = r2
            r2 = r8
            r3 = r237
            r136 = r82
            r82 = r97
            r97 = 5
            r118 = r12
            r12 = r134
            r85 = 2
            r134 = r5
            r228 = r110
            r110 = r128
            r128 = r228
            r5 = r116
            r6 = r238
            r0.printControllerActivity(r1, r2, r3, r4, r5, r6)
            r236.println()
            r236.print(r237)
            java.lang.String r0 = "  Device battery use since last full charge"
            r11.println(r0)
            r236.print(r237)
            java.lang.String r0 = "    Amount discharged (lower bound): "
            r11.print(r0)
            int r0 = r234.getLowDischargeAmountSinceCharge()
            r11.println(r0)
            r236.print(r237)
            java.lang.String r0 = "    Amount discharged (upper bound): "
            r11.print(r0)
            int r0 = r234.getHighDischargeAmountSinceCharge()
            r11.println(r0)
            r236.print(r237)
            java.lang.String r0 = "    Amount discharged while screen on: "
            r11.print(r0)
            int r0 = r234.getDischargeAmountScreenOnSinceCharge()
            r11.println(r0)
            r236.print(r237)
            java.lang.String r0 = "    Amount discharged while screen off: "
            r11.print(r0)
            int r0 = r234.getDischargeAmountScreenOffSinceCharge()
            r11.println(r0)
            r236.print(r237)
            java.lang.String r0 = "    Amount discharged while screen doze: "
            r11.print(r0)
            int r0 = r234.getDischargeAmountScreenDozeSinceCharge()
            r11.println(r0)
            r236.println()
            com.android.internal.os.BatteryUsageStatsProvider r0 = new com.android.internal.os.BatteryUsageStatsProvider
            r6 = r235
            r0.<init>(r6, r7)
            r5 = r0
            android.os.BatteryUsageStatsQuery$Builder r0 = new android.os.BatteryUsageStatsQuery$Builder
            r0.<init>()
            r1 = 0
            android.os.BatteryUsageStatsQuery$Builder r0 = r0.setMaxStatsAgeMs(r1)
            android.os.BatteryUsageStatsQuery$Builder r0 = r0.includePowerModels()
            android.os.BatteryUsageStatsQuery$Builder r0 = r0.includeProcessStateData()
            android.os.BatteryUsageStatsQuery$Builder r0 = r0.includeVirtualUids()
            android.os.BatteryUsageStatsQuery r0 = r0.build()
            android.os.BatteryUsageStats r4 = r5.getBatteryUsageStats(r0)
            r4.dump(r11, r14)
            java.util.List r0 = r4.getUidBatteryConsumers()
            java.util.List r2 = r7.getUidMobileRadioStats(r0)
            int r0 = r2.size()
            java.lang.String r3 = " ("
            java.lang.String r1 = ": "
            if (r0 <= 0) goto Lf92
            r236.print(r237)
            java.lang.String r0 = "  Per-app mobile ms per packet:"
            r11.println(r0)
            r137 = 0
            r0 = 0
            r116 = r5
            r228 = r137
            r137 = r4
            r4 = r228
        Lf01:
            int r6 = r2.size()
            if (r0 >= r6) goto Lf63
            java.lang.Object r6 = r2.get(r0)
            android.os.BatteryStats$UidMobileRadioStats r6 = (android.os.BatteryStats.UidMobileRadioStats) r6
            r138 = r2
            r2 = 0
            r8.setLength(r2)
            r8.append(r14)
            java.lang.String r2 = "    Uid "
            r8.append(r2)
            int r2 = r6.uid
            android.os.UserHandle.formatUid(r8, r2)
            r8.append(r1)
            r139 = r1
            double r1 = r6.millisecondsPerPacket
            java.lang.String r1 = formatValue(r1)
            r8.append(r1)
            r8.append(r3)
            long r1 = r6.rxPackets
            r140 = r12
            long r12 = r6.txPackets
            long r1 = r1 + r12
            r8.append(r1)
            java.lang.String r1 = " packets over "
            r8.append(r1)
            long r1 = r6.radioActiveMs
            formatTimeMsNoSpace(r8, r1)
            r8.append(r15)
            int r1 = r6.radioActiveCount
            r8.append(r1)
            r13 = r130
            r8.append(r13)
            r11.println(r8)
            long r1 = r6.radioActiveMs
            long r4 = r4 + r1
            int r0 = r0 + 1
            r6 = r235
            r2 = r138
            r1 = r139
            r12 = r140
            goto Lf01
        Lf63:
            r139 = r1
            r138 = r2
            r140 = r12
            r13 = r130
            r0 = 0
            r8.setLength(r0)
            r8.append(r14)
            java.lang.String r0 = "    TOTAL TIME: "
            r8.append(r0)
            formatTimeMs(r8, r4)
            r8.append(r9)
            r1 = r140
            java.lang.String r0 = r7.formatRatioLocked(r4, r1)
            r8.append(r0)
            r12 = r131
            r8.append(r12)
            r11.println(r8)
            r236.println()
            goto Lf9f
        Lf92:
            r139 = r1
            r138 = r2
            r137 = r4
            r116 = r5
            r1 = r12
            r13 = r130
            r12 = r131
        Lf9f:
            android.os.BatteryStats$1 r0 = new android.os.BatteryStats$1
            r0.<init>()
            r6 = r0
            java.lang.String r5 = " realtime"
            if (r-17 >= 0) goto L11fd
        Lfaa:
            java.util.Map r130 = r234.getKernelWakelockStats()
            int r0 = r130.size()
            if (r0 <= 0) goto L10b6
            java.util.ArrayList r0 = new java.util.ArrayList
            r0.<init>()
            r4 = r0
            java.util.Set r0 = r130.entrySet()
            java.util.Iterator r0 = r0.iterator()
        Lfc2:
            boolean r131 = r0.hasNext()
            if (r-125 == 0) goto L1010
            java.lang.Object r131 = r0.next()
            java.util.Map$Entry r131 = (java.util.Map.Entry) r131
            java.lang.Object r140 = r131.getValue()
            r147 = r0
            r0 = r140
            android.os.BatteryStats$Timer r0 = (android.os.BatteryStats.Timer) r0
            r148 = r12
            r140 = r13
            r12 = r118
            long r118 = computeWakeLock(r0, r12, r10)
            r47 = 0
            int r141 = (r118 > r47 ? 1 : (r118 == r47 ? 0 : -1))
            if (r-115 <= 0) goto L1003
            r149 = r1
            android.os.BatteryStats$TimerEntry r1 = new android.os.BatteryStats$TimerEntry
            java.lang.Object r2 = r131.getKey()
            r142 = r2
            java.lang.String r142 = (java.lang.String) r142
            r143 = 0
            r141 = r1
            r144 = r0
            r145 = r118
            r141.<init>(r142, r143, r144, r145)
            r4.add(r1)
            goto L1005
        L1003:
            r149 = r1
        L1005:
            r118 = r12
            r13 = r140
            r0 = r147
            r12 = r148
            r1 = r149
            goto Lfc2
        L1010:
            r149 = r1
            r148 = r12
            r140 = r13
            r12 = r118
            r47 = 0
            int r0 = r4.size()
            if (r0 <= 0) goto L10aa
            java.util.Collections.sort(r4, r6)
            r236.print(r237)
            java.lang.String r0 = "  All kernel wake locks:"
            r11.println(r0)
            r0 = 0
            r2 = r0
        L102d:
            int r0 = r4.size()
            if (r2 >= r0) goto L1099
            java.lang.Object r0 = r4.get(r2)
            r1 = r0
            android.os.BatteryStats$TimerEntry r1 = (android.os.BatteryStats.TimerEntry) r1
            java.lang.String r118 = ": "
            r0 = 0
            r8.setLength(r0)
            r8.append(r14)
            java.lang.String r0 = "  Kernel Wake lock "
            r8.append(r0)
            java.lang.String r0 = r1.mName
            r8.append(r0)
            android.os.BatteryStats$Timer r0 = r1.mTimer
            r119 = 0
            r131 = r0
            r0 = r8
            r151 = r139
            r141 = r149
            r139 = r1
            r1 = r131
            r152 = r3
            r131 = r138
            r138 = r2
            r2 = r12
            r143 = r4
            r4 = r119
            r153 = r5
            r5 = r238
            r7 = r6
            r6 = r118
            java.lang.String r0 = printWakeLock(r0, r1, r2, r4, r5, r6)
            r6 = r151
            boolean r1 = r0.equals(r6)
            if (r1 != 0) goto L1087
            r5 = r153
            r8.append(r5)
            java.lang.String r1 = r8.toString()
            r11.println(r1)
            goto L1089
        L1087:
            r5 = r153
        L1089:
            int r2 = r138 + 1
            r139 = r6
            r6 = r7
            r138 = r131
            r149 = r141
            r4 = r143
            r3 = r152
            r7 = r234
            goto L102d
        L1099:
            r152 = r3
            r143 = r4
            r7 = r6
            r131 = r138
            r6 = r139
            r141 = r149
            r138 = r2
            r236.println()
            goto L10c7
        L10aa:
            r152 = r3
            r143 = r4
            r7 = r6
            r131 = r138
            r6 = r139
            r141 = r149
            goto L10c7
        L10b6:
            r141 = r1
            r152 = r3
            r7 = r6
            r148 = r12
            r140 = r13
            r12 = r118
            r131 = r138
            r6 = r139
            r47 = 0
        L10c7:
            int r0 = r38.size()
            if (r0 <= 0) goto L1142
            r4 = r38
            java.util.Collections.sort(r4, r7)
            r236.print(r237)
            java.lang.String r0 = "  All partial wake locks:"
            r11.println(r0)
            r0 = 0
            r2 = r0
        L10dc:
            int r0 = r4.size()
            if (r2 >= r0) goto L1134
            java.lang.Object r0 = r4.get(r2)
            r3 = r0
            android.os.BatteryStats$TimerEntry r3 = (android.os.BatteryStats.TimerEntry) r3
            r0 = 0
            r8.setLength(r0)
            java.lang.String r0 = "  Wake lock "
            r8.append(r0)
            int r0 = r3.mId
            android.os.UserHandle.formatUid(r8, r0)
            r1 = r136
            r8.append(r1)
            java.lang.String r0 = r3.mName
            r8.append(r0)
            android.os.BatteryStats$Timer r0 = r3.mTimer
            r38 = 0
            java.lang.String r118 = ": "
            r119 = r0
            r0 = r8
            r1 = r119
            r119 = r2
            r138 = r3
            r2 = r12
            r139 = r4
            r4 = r38
            r154 = r5
            r5 = r238
            r155 = r6
            r6 = r118
            printWakeLock(r0, r1, r2, r4, r5, r6)
            r6 = r154
            r8.append(r6)
            java.lang.String r0 = r8.toString()
            r11.println(r0)
            int r2 = r119 + 1
            r5 = r6
            r4 = r139
            r6 = r155
            goto L10dc
        L1134:
            r119 = r2
            r139 = r4
            r155 = r6
            r6 = r5
            r139.clear()
            r236.println()
            goto L1147
        L1142:
            r155 = r6
            r139 = r38
            r6 = r5
        L1147:
            java.util.Map r38 = r234.getWakeupReasonStats()
            int r0 = r38.size()
            if (r0 <= 0) goto L11f9
            r236.print(r237)
            java.lang.String r0 = "  All wakeup reasons:"
            r11.println(r0)
            java.util.ArrayList r0 = new java.util.ArrayList
            r0.<init>()
            r5 = r0
            java.util.Set r0 = r38.entrySet()
            java.util.Iterator r0 = r0.iterator()
        L1167:
            boolean r1 = r0.hasNext()
            if (r1 == 0) goto L119d
            java.lang.Object r1 = r0.next()
            java.util.Map$Entry r1 = (java.util.Map.Entry) r1
            java.lang.Object r2 = r1.getValue()
            android.os.BatteryStats$Timer r2 = (android.os.BatteryStats.Timer) r2
            android.os.BatteryStats$TimerEntry r3 = new android.os.BatteryStats$TimerEntry
            java.lang.Object r4 = r1.getKey()
            r157 = r4
            java.lang.String r157 = (java.lang.String) r157
            r158 = 0
            int r4 = r2.getCountLocked(r10)
            r118 = r0
            r119 = r1
            long r0 = (long) r4
            r156 = r3
            r159 = r2
            r160 = r0
            r156.<init>(r157, r158, r159, r160)
            r5.add(r3)
            r0 = r118
            goto L1167
        L119d:
            java.util.Collections.sort(r5, r7)
            r0 = 0
            r4 = r0
        L11a2:
            int r0 = r5.size()
            if (r4 >= r0) goto L11ee
            java.lang.Object r0 = r5.get(r4)
            r2 = r0
            android.os.BatteryStats$TimerEntry r2 = (android.os.BatteryStats.TimerEntry) r2
            java.lang.String r118 = ": "
            r0 = 0
            r8.setLength(r0)
            r8.append(r14)
            java.lang.String r0 = "  Wakeup reason "
            r8.append(r0)
            java.lang.String r0 = r2.mName
            r8.append(r0)
            android.os.BatteryStats$Timer r1 = r2.mTimer
            r119 = 0
            java.lang.String r138 = ": "
            r0 = r8
            r143 = r2
            r2 = r12
            r144 = r4
            r4 = r119
            r119 = r5
            r5 = r238
            r145 = r7
            r7 = r6
            r6 = r138
            printWakeLock(r0, r1, r2, r4, r5, r6)
            r8.append(r7)
            java.lang.String r0 = r8.toString()
            r11.println(r0)
            int r4 = r144 + 1
            r6 = r7
            r5 = r119
            r7 = r145
            goto L11a2
        L11ee:
            r144 = r4
            r119 = r5
            r145 = r7
            r7 = r6
            r236.println()
            goto L1212
        L11f9:
            r145 = r7
            r7 = r6
            goto L1212
        L11fd:
            r141 = r1
            r152 = r3
            r7 = r5
            r145 = r6
            r148 = r12
            r140 = r13
            r12 = r118
            r131 = r138
            r155 = r139
            r47 = 0
            r139 = r38
        L1212:
            android.util.LongSparseArray r6 = r234.getKernelMemoryStats()
            int r0 = r6.size()
            if (r0 <= 0) goto L1259
            java.lang.String r0 = "  Memory Stats"
            r11.println(r0)
            r0 = 0
        L1222:
            int r1 = r6.size()
            if (r0 >= r1) goto L1254
            r1 = 0
            r8.setLength(r1)
            java.lang.String r2 = "  Bandwidth "
            r8.append(r2)
            long r2 = r6.keyAt(r0)
            r8.append(r2)
            java.lang.String r2 = " Time "
            r8.append(r2)
            java.lang.Object r2 = r6.valueAt(r0)
            android.os.BatteryStats$Timer r2 = (android.os.BatteryStats.Timer) r2
            long r2 = r2.getTotalTimeLocked(r12, r10)
            r8.append(r2)
            java.lang.String r2 = r8.toString()
            r11.println(r2)
            int r0 = r0 + 1
            goto L1222
        L1254:
            r1 = 0
            r236.println()
            goto L125a
        L1259:
            r1 = 0
        L125a:
            java.util.Map r38 = r234.getRpmStats()
            int r0 = r38.size()
            if (r0 <= 0) goto L138b
            r236.print(r237)
            java.lang.String r0 = "  Resource Power Manager Stats"
            r11.println(r0)
            int r0 = r38.size()
            if (r0 <= 0) goto L134e
            java.util.Set r0 = r38.entrySet()
            java.util.Iterator r0 = r0.iterator()
        L127a:
            boolean r2 = r0.hasNext()
            if (r2 == 0) goto L1314
            java.lang.Object r2 = r0.next()
            java.util.Map$Entry r2 = (java.util.Map.Entry) r2
            java.lang.Object r3 = r2.getKey()
            java.lang.String r3 = (java.lang.String) r3
            java.lang.Object r4 = r2.getValue()
            android.os.BatteryStats$Timer r4 = (android.os.BatteryStats.Timer) r4
            r5 = r8
            r8 = r236
            r162 = r9
            r85 = r86
            r118 = r112
            r112 = r128
            r228 = r102
            r102 = r104
            r104 = r98
            r98 = r100
            r100 = r228
            r230 = r50
            r50 = r52
            r52 = r230
            r9 = r5
            r87 = r117
            r10 = r4
            r165 = r22
            r22 = r36
            r163 = r141
            r97 = r148
            r36 = r12
            r13 = r239
            r228 = r122
            r122 = r124
            r124 = r47
            r47 = r83
            r83 = r106
            r106 = r228
            r11 = r36
            r168 = r96
            r170 = r136
            r169 = r140
            r13 = r238
            r14 = r237
            r172 = r15
            r171 = r97
            r15 = r3
            printTimer(r8, r9, r10, r11, r13, r14, r15)
            r11 = r236
            r10 = r238
            r8 = r5
            r12 = r36
            r112 = r118
            r9 = r162
            r148 = r171
            r15 = r172
            r97 = 5
            r36 = r22
            r86 = r85
            r22 = r165
            r85 = 2
            r228 = r47
            r47 = r124
            r124 = r122
            r122 = r106
            r106 = r83
            r83 = r228
            r230 = r98
            r98 = r104
            r104 = r102
            r102 = r100
            r100 = r230
            r232 = r50
            r50 = r52
            r52 = r232
            goto L127a
        L1314:
            r5 = r8
            r162 = r9
            r172 = r15
            r165 = r22
            r22 = r36
            r85 = r86
            r168 = r96
            r118 = r112
            r87 = r117
            r112 = r128
            r170 = r136
            r169 = r140
            r163 = r141
            r171 = r148
            r36 = r12
            r228 = r122
            r122 = r124
            r124 = r47
            r47 = r83
            r83 = r106
            r106 = r228
            r230 = r102
            r102 = r104
            r104 = r98
            r98 = r100
            r100 = r230
            r232 = r50
            r50 = r52
            r52 = r232
            goto L1387
        L134e:
            r5 = r8
            r162 = r9
            r172 = r15
            r165 = r22
            r22 = r36
            r85 = r86
            r168 = r96
            r118 = r112
            r87 = r117
            r112 = r128
            r170 = r136
            r169 = r140
            r163 = r141
            r171 = r148
            r36 = r12
            r228 = r122
            r122 = r124
            r124 = r47
            r47 = r83
            r83 = r106
            r106 = r228
            r230 = r102
            r102 = r104
            r104 = r98
            r98 = r100
            r100 = r230
            r232 = r50
            r50 = r52
            r52 = r232
        L1387:
            r236.println()
            goto L13c4
        L138b:
            r5 = r8
            r162 = r9
            r172 = r15
            r165 = r22
            r22 = r36
            r85 = r86
            r168 = r96
            r118 = r112
            r87 = r117
            r112 = r128
            r170 = r136
            r169 = r140
            r163 = r141
            r171 = r148
            r36 = r12
            r228 = r122
            r122 = r124
            r124 = r47
            r47 = r83
            r83 = r106
            r106 = r228
            r230 = r102
            r102 = r104
            r104 = r98
            r98 = r100
            r100 = r230
            r232 = r50
            r50 = r52
            r52 = r232
        L13c4:
            long[] r15 = r234.getCpuFreqs()
            if (r15 == 0) goto L13f1
            r5.setLength(r1)
            java.lang.String r0 = "  CPU freqs:"
            r5.append(r0)
            r0 = 0
        L13d3:
            int r2 = r15.length
            if (r0 >= r2) goto L13e4
            r2 = 32
            java.lang.StringBuilder r2 = r5.append(r2)
            r3 = r15[r0]
            r2.append(r3)
            int r0 = r0 + 1
            goto L13d3
        L13e4:
            java.lang.String r0 = r5.toString()
            r14 = r236
            r14.println(r0)
            r236.println()
            goto L13f3
        L13f1:
            r14 = r236
        L13f3:
            r0 = 0
            r13 = r0
        L13f5:
            r11 = r133
            if (r13 >= r11) goto L25c4
            r12 = r168
            int r10 = r12.keyAt(r13)
            r9 = r239
            if (r9 < 0) goto L143b
            if (r10 == r9) goto L143b
            r0 = 1000(0x3e8, float:1.401E-42)
            if (r10 == r0) goto L143b
            r3 = r237
            r45 = r6
            r150 = r11
            r147 = r12
            r117 = r15
            r210 = r20
            r215 = r36
            r128 = r145
            r219 = r152
            r9 = r155
            r160 = r165
            r152 = r169
            r203 = r170
            r170 = r171
            r153 = r172
            r133 = 5
            r169 = 1
            r12 = r238
            r6 = r5
            r21 = r7
            r20 = r13
            r5 = r14
            r14 = r126
            r166 = r162
            r162 = r163
            goto L2599
        L143b:
            java.lang.Object r0 = r12.valueAt(r13)
            r8 = r0
            android.os.BatteryStats$Uid r8 = (android.os.BatteryStats.Uid) r8
            r236.print(r237)
            java.lang.String r0 = "  "
            r14.print(r0)
            android.os.UserHandle.formatUid(r14, r10)
            java.lang.String r0 = ":"
            r14.println(r0)
            r96 = 0
            r4 = r238
            long r2 = r8.getNetworkActivityBytes(r1, r4)
            r128 = r2
            r0 = 1
            long r1 = r8.getNetworkActivityBytes(r0, r4)
            r140 = r1
            r3 = 2
            long r0 = r8.getNetworkActivityBytes(r3, r4)
            r2 = 3
            long r2 = r8.getNetworkActivityBytes(r2, r4)
            r142 = r0
            r0 = 4
            long r0 = r8.getNetworkActivityBytes(r0, r4)
            r133 = r11
            r168 = r12
            r117 = r15
            r15 = 5
            long r11 = r8.getNetworkActivityBytes(r15, r4)
            r130 = r10
            r15 = 0
            long r9 = r8.getNetworkActivityPackets(r15, r4)
            r146 = r11
            r15 = 1
            long r11 = r8.getNetworkActivityPackets(r15, r4)
            r45 = r6
            r153 = r7
            r15 = 2
            long r6 = r8.getNetworkActivityPackets(r15, r4)
            r15 = 3
            r148 = r6
            long r6 = r8.getNetworkActivityPackets(r15, r4)
            r150 = r6
            long r6 = r8.getMobileRadioActiveTime(r4)
            int r15 = r8.getMobileRadioActiveCount(r4)
            r156 = r2
            r2 = r36
            r36 = r0
            long r0 = r8.getFullWifiLockTime(r2, r4)
            r158 = r0
            long r0 = r8.getWifiScanTime(r2, r4)
            r138 = r13
            int r13 = r8.getWifiScanCount(r4)
            r144 = r13
            int r13 = r8.getWifiScanBackgroundCount(r4)
            r160 = r0
            long r0 = r8.getWifiScanActualTime(r2)
            r173 = r0
            long r0 = r8.getWifiScanBackgroundTime(r2)
            r175 = r0
            long r0 = r8.getWifiRunningTime(r2, r4)
            r177 = r2
            long r2 = r8.getMobileRadioApWakeupCount(r4)
            r179 = r0
            long r0 = r8.getWifiRadioApWakeupCount(r4)
            int r154 = (r128 > r124 ? 1 : (r128 == r124 ? 0 : -1))
            if (r-102 > 0) goto L14ff
            int r154 = (r140 > r124 ? 1 : (r140 == r124 ? 0 : -1))
            if (r-102 > 0) goto L14ff
            int r154 = (r9 > r124 ? 1 : (r9 == r124 ? 0 : -1))
            if (r-102 > 0) goto L14ff
            int r154 = (r11 > r124 ? 1 : (r11 == r124 ? 0 : -1))
            if (r-102 <= 0) goto L14f2
            goto L14ff
        L14f2:
            r181 = r0
            r183 = r2
            r185 = r128
            r2 = r140
            r128 = r145
            r1 = r234
            goto L153d
        L14ff:
            r236.print(r237)
            r181 = r0
            java.lang.String r0 = "    Mobile network: "
            r14.print(r0)
            r1 = r234
            r183 = r2
            r2 = r128
            r128 = r145
            java.lang.String r0 = r1.formatBytesLocked(r2)
            r14.print(r0)
            java.lang.String r0 = " received, "
            r14.print(r0)
            r185 = r2
            r2 = r140
            java.lang.String r0 = r1.formatBytesLocked(r2)
            r14.print(r0)
            java.lang.String r0 = " sent (packets "
            r14.print(r0)
            r14.print(r9)
            java.lang.String r0 = " received, "
            r14.print(r0)
            r14.print(r11)
            java.lang.String r0 = " sent)"
            r14.println(r0)
        L153d:
            int r0 = (r6 > r124 ? 1 : (r6 == r124 ? 0 : -1))
            if (r0 > 0) goto L154f
            if (r15 <= 0) goto L1544
            goto L154f
        L1544:
            r0 = r237
            r140 = r2
            r187 = r6
            r129 = r15
            r15 = r169
            goto L15b3
        L154f:
            r0 = 0
            r5.setLength(r0)
            r0 = r237
            r5.append(r0)
            r140 = r2
            java.lang.String r2 = "    Mobile radio active: "
            r5.append(r2)
            long r2 = r6 / r16
            formatTimeMs(r5, r2)
            r3 = r162
            r5.append(r3)
            r3 = r165
            java.lang.String r2 = r1.formatRatioLocked(r6, r3)
            r5.append(r2)
            r2 = r172
            r5.append(r2)
            r5.append(r15)
            r129 = r15
            r15 = r169
            r5.append(r15)
            long r165 = r9 + r11
            int r145 = (r165 > r124 ? 1 : (r165 == r124 ? 0 : -1))
            if (r-111 != 0) goto L158e
            r165 = 1
            r172 = r2
            r1 = r165
            goto L1592
        L158e:
            r172 = r2
            r1 = r165
        L1592:
            r165 = r3
            java.lang.String r3 = " @ "
            r5.append(r3)
            long r3 = r6 / r16
            double r3 = (double) r3
            r187 = r6
            double r6 = (double) r1
            double r3 = r3 / r6
            java.lang.String r3 = formatCharge(r3)
            r5.append(r3)
            java.lang.String r3 = " mspp"
            r5.append(r3)
            java.lang.String r3 = r5.toString()
            r14.println(r3)
        L15b3:
            int r1 = (r183 > r124 ? 1 : (r183 == r124 ? 0 : -1))
            if (r1 <= 0) goto L15d0
            r1 = 0
            r5.setLength(r1)
            r5.append(r0)
            java.lang.String r2 = "    Mobile radio AP wakeups: "
            r5.append(r2)
            r2 = r183
            r5.append(r2)
            java.lang.String r4 = r5.toString()
            r14.println(r4)
            goto L15d3
        L15d0:
            r2 = r183
            r1 = 0
        L15d3:
            java.lang.StringBuilder r4 = new java.lang.StringBuilder
            r4.<init>()
            java.lang.StringBuilder r4 = r4.append(r0)
            java.lang.String r6 = "  "
            java.lang.StringBuilder r4 = r4.append(r6)
            java.lang.String r4 = r4.toString()
            android.os.BatteryStats$ControllerActivityCounter r6 = r8.getModemControllerActivity()
            java.lang.String r7 = "Cellular"
            r183 = r11
            r189 = r36
            r11 = r158
            r191 = r160
            r193 = r173
            r195 = r175
            r197 = r179
            r199 = r181
            r36 = r9
            r9 = r142
            r0 = r234
            r97 = r8
            r169 = r15
            r8 = r234
            r15 = r1
            r1 = r236
            r11 = r156
            r202 = r177
            r142 = r185
            r156 = r2
            r3 = r172
            r2 = r5
            r204 = r3
            r205 = r162
            r160 = r165
            r3 = r4
            r4 = r7
            r7 = r5
            r5 = r6
            r132 = r7
            r136 = r13
            r7 = r148
            r13 = r150
            r148 = r187
            r6 = r238
            r0.printControllerActivityIfInteresting(r1, r2, r3, r4, r5, r6)
            int r0 = (r9 > r124 ? 1 : (r9 == r124 ? 0 : -1))
            if (r0 > 0) goto L1647
            int r0 = (r11 > r124 ? 1 : (r11 == r124 ? 0 : -1))
            if (r0 > 0) goto L1647
            int r0 = (r7 > r124 ? 1 : (r7 == r124 ? 0 : -1))
            if (r0 > 0) goto L1647
            int r0 = (r13 > r124 ? 1 : (r13 == r124 ? 0 : -1))
            if (r0 <= 0) goto L1640
            goto L1647
        L1640:
            r3 = r7
            r5 = r13
            r7 = r234
            r14 = r236
            goto L167d
        L1647:
            r236.print(r237)
            java.lang.String r0 = "    Wi-Fi network: "
            r5 = r13
            r14 = r236
            r14.print(r0)
            r3 = r7
            r7 = r234
            java.lang.String r0 = r7.formatBytesLocked(r9)
            r14.print(r0)
            java.lang.String r0 = " received, "
            r14.print(r0)
            java.lang.String r0 = r7.formatBytesLocked(r11)
            r14.print(r0)
            java.lang.String r0 = " sent (packets "
            r14.print(r0)
            r14.print(r3)
            java.lang.String r0 = " received, "
            r14.print(r0)
            r14.print(r5)
            java.lang.String r0 = " sent)"
            r14.println(r0)
        L167d:
            int r0 = (r158 > r124 ? 1 : (r158 == r124 ? 0 : -1))
            if (r0 != 0) goto L16de
            r1 = r191
            int r0 = (r1 > r124 ? 1 : (r1 == r124 ? 0 : -1))
            if (r0 != 0) goto L16d1
            if (r-112 != 0) goto L16d1
            if (r-120 != 0) goto L16d1
            r150 = r11
            r11 = r193
            int r0 = (r11 > r124 ? 1 : (r11 == r124 ? 0 : -1))
            if (r0 != 0) goto L16c8
            r165 = r9
            r9 = r195
            int r0 = (r9 > r124 ? 1 : (r9 == r124 ? 0 : -1))
            if (r0 != 0) goto L16c3
            r172 = r5
            r5 = r197
            int r0 = (r5 > r124 ? 1 : (r5 == r124 ? 0 : -1))
            if (r0 == 0) goto L16a4
            goto L16ec
        L16a4:
            r8 = r237
            r191 = r1
            r197 = r5
            r193 = r11
            r13 = r132
            r5 = r136
            r6 = r144
            r144 = r158
            r162 = r163
            r1 = r202
            r158 = r3
            r11 = r9
            r4 = r169
            r9 = r204
            r10 = r205
            goto L17c6
        L16c3:
            r172 = r5
            r5 = r197
            goto L16ec
        L16c8:
            r172 = r5
            r165 = r9
            r9 = r195
            r5 = r197
            goto L16ec
        L16d1:
            r172 = r5
            r165 = r9
            r150 = r11
            r11 = r193
            r9 = r195
            r5 = r197
            goto L16ec
        L16de:
            r172 = r5
            r165 = r9
            r150 = r11
            r1 = r191
            r11 = r193
            r9 = r195
            r5 = r197
        L16ec:
            r13 = r132
            r13.setLength(r15)
            r8 = r237
            r228 = r3
            r3 = r158
            r158 = r228
            r13.append(r8)
            java.lang.String r0 = "    Wifi Running: "
            r13.append(r0)
            r195 = r9
            long r9 = r5 / r16
            formatTimeMs(r13, r9)
            r10 = r205
            r13.append(r10)
            r14 = r163
            java.lang.String r0 = r7.formatRatioLocked(r5, r14)
            r13.append(r0)
            java.lang.String r0 = ")\n"
            r13.append(r0)
            r13.append(r8)
            java.lang.String r0 = "    Full Wifi Lock: "
            r13.append(r0)
            r197 = r5
            long r5 = r3 / r16
            formatTimeMs(r13, r5)
            r13.append(r10)
            java.lang.String r0 = r7.formatRatioLocked(r3, r14)
            r13.append(r0)
            java.lang.String r0 = ")\n"
            r13.append(r0)
            r13.append(r8)
            java.lang.String r0 = "    Wifi Scan (blamed): "
            r13.append(r0)
            long r5 = r1 / r16
            formatTimeMs(r13, r5)
            r13.append(r10)
            java.lang.String r0 = r7.formatRatioLocked(r1, r14)
            r13.append(r0)
            r9 = r204
            r13.append(r9)
            r6 = r144
            r13.append(r6)
            java.lang.String r0 = "x\n"
            r13.append(r0)
            r13.append(r8)
            java.lang.String r0 = "    Wifi Scan (actual): "
            r13.append(r0)
            r191 = r1
            long r0 = r11 / r16
            formatTimeMs(r13, r0)
            r13.append(r10)
            r144 = r3
            r1 = r202
            r0 = 0
            long r3 = r7.computeBatteryRealtime(r1, r0)
            java.lang.String r0 = r7.formatRatioLocked(r11, r3)
            r13.append(r0)
            r13.append(r9)
            r13.append(r6)
            java.lang.String r0 = "x\n"
            r13.append(r0)
            r13.append(r8)
            java.lang.String r0 = "    Background Wifi Scan: "
            r13.append(r0)
            long r3 = r195 / r16
            formatTimeMs(r13, r3)
            r13.append(r10)
            r0 = 0
            long r3 = r7.computeBatteryRealtime(r1, r0)
            r193 = r11
            r11 = r195
            java.lang.String r0 = r7.formatRatioLocked(r11, r3)
            r13.append(r0)
            r13.append(r9)
            r5 = r136
            r13.append(r5)
            r4 = r169
            r13.append(r4)
            java.lang.String r0 = r13.toString()
            r162 = r14
            r14 = r236
            r14.println(r0)
        L17c6:
            r195 = r11
            r11 = r199
            int r0 = (r11 > r124 ? 1 : (r11 == r124 ? 0 : -1))
            if (r0 <= 0) goto L17e4
            r0 = 0
            r13.setLength(r0)
            r13.append(r8)
            java.lang.String r0 = "    WiFi AP wakeups: "
            r13.append(r0)
            r13.append(r11)
            java.lang.String r0 = r13.toString()
            r14.println(r0)
        L17e4:
            java.lang.StringBuilder r0 = new java.lang.StringBuilder
            r0.<init>()
            java.lang.StringBuilder r0 = r0.append(r8)
            java.lang.String r3 = "  "
            java.lang.StringBuilder r0 = r0.append(r3)
            java.lang.String r3 = r0.toString()
            android.os.BatteryStats$ControllerActivityCounter r15 = r97.getWifiControllerActivity()
            java.lang.String r136 = "WiFi"
            r0 = r234
            r199 = r11
            r174 = r191
            r11 = r1
            r1 = r236
            r2 = r13
            r228 = r144
            r144 = r158
            r158 = r228
            r154 = r4
            r4 = r136
            r136 = r5
            r176 = r197
            r5 = r15
            r15 = r6
            r6 = r238
            r0.printControllerActivityIfInteresting(r1, r2, r3, r4, r5, r6)
            r5 = r189
            int r0 = (r5 > r124 ? 1 : (r5 == r124 ? 0 : -1))
            if (r0 > 0) goto L182a
            int r0 = (r146 > r124 ? 1 : (r146 == r124 ? 0 : -1))
            if (r0 <= 0) goto L1827
            goto L182a
        L1827:
            r2 = r146
            goto L184c
        L182a:
            r236.print(r237)
            java.lang.String r0 = "    Bluetooth network: "
            r14.print(r0)
            java.lang.String r0 = r7.formatBytesLocked(r5)
            r14.print(r0)
            java.lang.String r0 = " received, "
            r14.print(r0)
            r2 = r146
            java.lang.String r0 = r7.formatBytesLocked(r2)
            r14.print(r0)
            java.lang.String r0 = " sent"
            r14.println(r0)
        L184c:
            android.os.BatteryStats$Timer r4 = r97.getBluetoothScanTimer()
            java.lang.String r1 = "\n"
            java.lang.String r0 = " times)"
            if (r4 == 0) goto L1a3b
            r7 = r238
            long r146 = r4.getTotalTimeLocked(r11, r7)
            long r146 = r146 + r18
            r178 = r2
            long r2 = r146 / r16
            int r146 = (r2 > r124 ? 1 : (r2 == r124 ? 0 : -1))
            if (r-110 == 0) goto L1a25
            r189 = r5
            int r5 = r4.getCountLocked(r7)
            android.os.BatteryStats$Timer r6 = r97.getBluetoothScanBackgroundTimer()
            if (r6 == 0) goto L1877
            int r146 = r6.getCountLocked(r7)
            goto L1879
        L1877:
            r146 = 0
        L1879:
            r147 = r146
            r204 = r9
            r205 = r10
            r202 = r11
            r9 = r20
            long r11 = r4.getTotalDurationMsLocked(r9)
            if (r6 == 0) goto L188e
            long r20 = r6.getTotalDurationMsLocked(r9)
            goto L1890
        L188e:
            r20 = r124
        L1890:
            r180 = r20
            android.os.BatteryStats$Counter r20 = r97.getBluetoothScanResultCounter()
            if (r20 == 0) goto L18a3
            r20 = r15
            android.os.BatteryStats$Counter r15 = r97.getBluetoothScanResultCounter()
            int r15 = r15.getCountLocked(r7)
            goto L18a6
        L18a3:
            r20 = r15
            r15 = 0
        L18a6:
            android.os.BatteryStats$Counter r21 = r97.getBluetoothScanResultBgCounter()
            if (r21 == 0) goto L18b5
            android.os.BatteryStats$Counter r14 = r97.getBluetoothScanResultBgCounter()
            int r14 = r14.getCountLocked(r7)
            goto L18b6
        L18b5:
            r14 = 0
        L18b6:
            android.os.BatteryStats$Timer r7 = r97.getBluetoothUnoptimizedScanTimer()
            if (r7 == 0) goto L18c1
            long r185 = r7.getTotalDurationMsLocked(r9)
            goto L18c3
        L18c1:
            r185 = r124
        L18c3:
            r187 = r185
            if (r7 == 0) goto L18cc
            long r185 = r7.getMaxDurationMsLocked(r9)
            goto L18ce
        L18cc:
            r185 = r124
        L18ce:
            r191 = r185
            r21 = r7
            android.os.BatteryStats$Timer r7 = r97.getBluetoothUnoptimizedScanBackgroundTimer()
            if (r7 == 0) goto L18de
            long r185 = r7.getTotalDurationMsLocked(r9)
            goto L18e0
        L18de:
            r185 = r124
        L18e0:
            r197 = r185
            if (r7 == 0) goto L18e9
            long r185 = r7.getMaxDurationMsLocked(r9)
            goto L18eb
        L18e9:
            r185 = r124
        L18eb:
            r206 = r185
            r185 = r9
            r9 = 0
            r13.setLength(r9)
            int r9 = (r11 > r2 ? 1 : (r11 == r2 ? 0 : -1))
            if (r9 == 0) goto L191c
            r13.append(r8)
            java.lang.String r9 = "    Bluetooth Scan (total blamed realtime): "
            r13.append(r9)
            formatTimeMs(r13, r2)
            r10 = r152
            r13.append(r10)
            r13.append(r5)
            r13.append(r0)
            boolean r9 = r4.isRunningLocked()
            if (r9 == 0) goto L1918
            java.lang.String r9 = " (currently running)"
            r13.append(r9)
        L1918:
            r13.append(r1)
            goto L191e
        L191c:
            r10 = r152
        L191e:
            r13.append(r8)
            java.lang.String r9 = "    Bluetooth Scan (total actual realtime): "
            r13.append(r9)
            formatTimeMs(r13, r11)
            r13.append(r10)
            r13.append(r5)
            r13.append(r0)
            boolean r9 = r4.isRunningLocked()
            if (r9 == 0) goto L193d
            java.lang.String r9 = " (currently running)"
            r13.append(r9)
        L193d:
            r13.append(r1)
            r208 = r2
            r2 = r180
            int r9 = (r2 > r124 ? 1 : (r2 == r124 ? 0 : -1))
            if (r9 > 0) goto L1950
            r9 = r147
            if (r9 <= 0) goto L194d
            goto L1952
        L194d:
            r146 = r4
            goto L1978
        L1950:
            r9 = r147
        L1952:
            r13.append(r8)
            r146 = r4
            java.lang.String r4 = "    Bluetooth Scan (background realtime): "
            r13.append(r4)
            formatTimeMs(r13, r2)
            r13.append(r10)
            r13.append(r9)
            r13.append(r0)
            if (r6 == 0) goto L1975
            boolean r4 = r6.isRunningLocked()
            if (r4 == 0) goto L1975
            java.lang.String r4 = " (currently running in background)"
            r13.append(r4)
        L1975:
            r13.append(r1)
        L1978:
            r13.append(r8)
            java.lang.String r4 = "    Bluetooth Scan Results: "
            r13.append(r4)
            r13.append(r15)
            r13.append(r10)
            r13.append(r14)
            java.lang.String r4 = " in background)"
            r13.append(r4)
            r180 = r2
            r2 = r187
            int r4 = (r2 > r124 ? 1 : (r2 == r124 ? 0 : -1))
            if (r4 > 0) goto L19a7
            r147 = r5
            r4 = r197
            int r152 = (r4 > r124 ? 1 : (r4 == r124 ? 0 : -1))
            if (r-104 <= 0) goto L199f
            goto L19ab
        L199f:
            r152 = r0
            r187 = r2
            r2 = r206
            goto L1a17
        L19a7:
            r147 = r5
            r4 = r197
        L19ab:
            r13.append(r1)
            r13.append(r8)
            r152 = r0
            java.lang.String r0 = "    Unoptimized Bluetooth Scan (realtime): "
            r13.append(r0)
            formatTimeMs(r13, r2)
            java.lang.String r0 = " (max "
            r13.append(r0)
            r187 = r2
            r2 = r191
            formatTimeMs(r13, r2)
            r0 = r171
            r13.append(r0)
            if (r21 == 0) goto L19df
            boolean r164 = r21.isRunningLocked()
            if (r-92 == 0) goto L19dc
            r191 = r2
            java.lang.String r2 = " (currently running unoptimized)"
            r13.append(r2)
            goto L19e1
        L19dc:
            r191 = r2
            goto L19e1
        L19df:
            r191 = r2
        L19e1:
            if (r7 == 0) goto L1a13
            int r2 = (r4 > r124 ? 1 : (r4 == r124 ? 0 : -1))
            if (r2 <= 0) goto L1a13
            r13.append(r1)
            r13.append(r8)
            java.lang.String r2 = "    Unoptimized Bluetooth Scan (background realtime): "
            r13.append(r2)
            formatTimeMs(r13, r4)
            java.lang.String r2 = " (max "
            r13.append(r2)
            r2 = r206
            formatTimeMs(r13, r2)
            r13.append(r0)
            boolean r164 = r7.isRunningLocked()
            if (r-92 == 0) goto L1a10
            r171 = r0
            java.lang.String r0 = " (currently running unoptimized in background)"
            r13.append(r0)
            goto L1a17
        L1a10:
            r171 = r0
            goto L1a17
        L1a13:
            r171 = r0
            r2 = r206
        L1a17:
            java.lang.String r0 = r13.toString()
            r164 = r7
            r7 = r236
            r7.println(r0)
            r96 = 1
            goto L1a50
        L1a25:
            r208 = r2
            r146 = r4
            r189 = r5
            r204 = r9
            r205 = r10
            r202 = r11
            r7 = r14
            r185 = r20
            r10 = r152
            r152 = r0
            r20 = r15
            goto L1a50
        L1a3b:
            r178 = r2
            r146 = r4
            r189 = r5
            r204 = r9
            r205 = r10
            r202 = r11
            r7 = r14
            r185 = r20
            r10 = r152
            r152 = r0
            r20 = r15
        L1a50:
            boolean r0 = r97.hasUserActivity()
            java.lang.String r15 = ", "
            if (r0 == 0) goto L1aa1
            r0 = 0
            r2 = 0
        L1a5a:
            int r3 = android.os.BatteryStats.Uid.NUM_USER_ACTIVITY_TYPES
            if (r2 >= r3) goto L1a91
            r14 = r238
            r9 = r97
            int r3 = r9.getUserActivityCount(r2, r14)
            if (r3 == 0) goto L1a88
            if (r0 != 0) goto L1a75
            r4 = 0
            r13.setLength(r4)
            java.lang.String r4 = "    User activity: "
            r13.append(r4)
            r0 = 1
            goto L1a78
        L1a75:
            r13.append(r15)
        L1a78:
            r13.append(r3)
            r11 = r170
            r13.append(r11)
            java.lang.String[] r4 = android.os.BatteryStats.Uid.USER_ACTIVITY_TYPES
            r4 = r4[r2]
            r13.append(r4)
            goto L1a8a
        L1a88:
            r11 = r170
        L1a8a:
            int r2 = r2 + 1
            r97 = r9
            r170 = r11
            goto L1a5a
        L1a91:
            r14 = r238
            r9 = r97
            r11 = r170
            if (r0 == 0) goto L1aa7
            java.lang.String r2 = r13.toString()
            r7.println(r2)
            goto L1aa7
        L1aa1:
            r14 = r238
            r9 = r97
            r11 = r170
        L1aa7:
            android.util.ArrayMap r12 = r9.getWakelockStats()
            r2 = 0
            r4 = 0
            r169 = 0
            r180 = 0
            r0 = 0
            int r6 = r12.size()
            r21 = r10
            r10 = 1
            int r6 = r6 - r10
            r210 = r169
            r212 = r180
            r228 = r6
            r6 = r0
            r0 = r228
            r229 = r2
            r2 = r4
            r4 = r229
        L1acb:
            if (r0 < 0) goto L1bbc
            java.lang.Object r97 = r12.valueAt(r0)
            r10 = r97
            android.os.BatteryStats$Uid$Wakelock r10 = (android.os.BatteryStats.Uid.Wakelock) r10
            java.lang.String r97 = ": "
            r147 = r1
            r1 = 0
            r13.setLength(r1)
            r13.append(r8)
            java.lang.String r1 = "    Wake lock "
            r13.append(r1)
            java.lang.Object r1 = r12.keyAt(r0)
            java.lang.String r1 = (java.lang.String) r1
            r13.append(r1)
            r1 = 1
            android.os.BatteryStats$Timer r164 = r10.getWakeTime(r1)
            java.lang.String r169 = "full"
            r1 = r152
            r170 = r171
            r152 = r0
            r0 = r13
            r214 = r147
            r147 = r12
            r12 = r1
            r1 = r164
            r164 = r11
            r171 = r12
            r11 = r2
            r2 = r202
            r180 = r9
            r8 = r4
            r4 = r169
            r181 = r189
            r5 = r238
            r169 = r15
            r15 = r6
            r6 = r97
            java.lang.String r97 = printWakeLock(r0, r1, r2, r4, r5, r6)
            r0 = 0
            android.os.BatteryStats$Timer r187 = r10.getWakeTime(r0)
            java.lang.String r4 = "partial"
            r0 = r13
            r1 = r187
            r6 = r97
            java.lang.String r97 = printWakeLock(r0, r1, r2, r4, r5, r6)
            if (r-69 == 0) goto L1b33
            android.os.BatteryStats$Timer r0 = r187.getSubTimer()
            goto L1b34
        L1b33:
            r0 = 0
        L1b34:
            r1 = r0
            java.lang.String r4 = "background partial"
            r0 = r13
            r2 = r202
            r5 = r238
            r6 = r97
            java.lang.String r97 = printWakeLock(r0, r1, r2, r4, r5, r6)
            r6 = 2
            android.os.BatteryStats$Timer r1 = r10.getWakeTime(r6)
            java.lang.String r4 = "window"
            r6 = r97
            java.lang.String r97 = printWakeLock(r0, r1, r2, r4, r5, r6)
            r0 = 18
            android.os.BatteryStats$Timer r1 = r10.getWakeTime(r0)
            java.lang.String r4 = "draw"
            r0 = r13
            r6 = r97
            java.lang.String r0 = printWakeLock(r0, r1, r2, r4, r5, r6)
            r1 = r153
            r13.append(r1)
            java.lang.String r2 = r13.toString()
            r7.println(r2)
            r96 = 1
            int r6 = r15 + 1
            r2 = 1
            android.os.BatteryStats$Timer r3 = r10.getWakeTime(r2)
            r4 = r202
            long r2 = computeWakeLock(r3, r4, r14)
            long r2 = r2 + r8
            r8 = 0
            android.os.BatteryStats$Timer r9 = r10.getWakeTime(r8)
            long r8 = computeWakeLock(r9, r4, r14)
            long r8 = r8 + r11
            r11 = 2
            android.os.BatteryStats$Timer r12 = r10.getWakeTime(r11)
            long r188 = computeWakeLock(r12, r4, r14)
            r190 = r2
            r2 = r210
            long r210 = r2 + r188
            r2 = 18
            android.os.BatteryStats$Timer r2 = r10.getWakeTime(r2)
            long r2 = computeWakeLock(r2, r4, r14)
            r4 = r212
            long r212 = r4 + r2
            int r0 = r152 + (-1)
            r2 = r8
            r12 = r147
            r11 = r164
            r15 = r169
            r152 = r171
            r9 = r180
            r4 = r190
            r1 = r214
            r10 = 1
            r8 = r237
            r171 = r170
            r189 = r181
            goto L1acb
        L1bbc:
            r214 = r1
            r180 = r9
            r164 = r11
            r147 = r12
            r169 = r15
            r1 = r153
            r170 = r171
            r181 = r189
            r11 = r2
            r8 = r4
            r15 = r6
            r171 = r152
            r2 = r210
            r4 = r212
            r152 = r0
            r0 = 2
            r6 = 1
            if (r15 <= r6) goto L1cd7
            r152 = 0
            r187 = 0
            android.os.BatteryStats$Timer r6 = r180.getAggregatedPartialWakelockTimer()
            if (r6 == 0) goto L1c09
            android.os.BatteryStats$Timer r6 = r180.getAggregatedPartialWakelockTimer()
            r97 = r1
            r0 = r185
            long r152 = r6.getTotalDurationMsLocked(r0)
            android.os.BatteryStats$Timer r10 = r6.getSubTimer()
            if (r10 == 0) goto L1bfd
            long r185 = r10.getTotalDurationMsLocked(r0)
            goto L1bff
        L1bfd:
            r185 = r124
        L1bff:
            r187 = r185
            r185 = r0
            r6 = r15
            r0 = r152
            r14 = r187
            goto L1c12
        L1c09:
            r97 = r1
            r0 = r185
            r6 = r15
            r0 = r152
            r14 = r187
        L1c12:
            int r10 = (r0 > r124 ? 1 : (r0 == r124 ? 0 : -1))
            if (r10 != 0) goto L1c33
            int r10 = (r14 > r124 ? 1 : (r14 == r124 ? 0 : -1))
            if (r10 != 0) goto L1c33
            int r10 = (r8 > r124 ? 1 : (r8 == r124 ? 0 : -1))
            if (r10 != 0) goto L1c33
            int r10 = (r11 > r124 ? 1 : (r11 == r124 ? 0 : -1))
            if (r10 != 0) goto L1c33
            int r10 = (r2 > r124 ? 1 : (r2 == r124 ? 0 : -1))
            if (r10 == 0) goto L1c27
            goto L1c33
        L1c27:
            r152 = r6
            r187 = r8
            r10 = r97
            r6 = r169
            r8 = r237
            goto L1ce0
        L1c33:
            r10 = 0
            r13.setLength(r10)
            r9 = r8
            r8 = r237
            r13.append(r8)
            r152 = r6
            java.lang.String r6 = "    TOTAL wake: "
            r13.append(r6)
            r6 = 0
            int r153 = (r9 > r124 ? 1 : (r9 == r124 ? 0 : -1))
            if (r-103 == 0) goto L1c56
            r6 = 1
            formatTimeMs(r13, r9)
            r153 = r6
            java.lang.String r6 = "full"
            r13.append(r6)
            r6 = r153
        L1c56:
            int r153 = (r11 > r124 ? 1 : (r11 == r124 ? 0 : -1))
            if (r-103 == 0) goto L1c75
            if (r6 == 0) goto L1c64
            r153 = r6
            r6 = r169
            r13.append(r6)
            goto L1c68
        L1c64:
            r153 = r6
            r6 = r169
        L1c68:
            r153 = 1
            formatTimeMs(r13, r11)
            r187 = r9
            java.lang.String r9 = "blamed partial"
            r13.append(r9)
            goto L1c7b
        L1c75:
            r153 = r6
            r187 = r9
            r6 = r169
        L1c7b:
            int r9 = (r0 > r124 ? 1 : (r0 == r124 ? 0 : -1))
            if (r9 == 0) goto L1c8e
            if (r-103 == 0) goto L1c84
            r13.append(r6)
        L1c84:
            r153 = 1
            formatTimeMs(r13, r0)
            java.lang.String r9 = "actual partial"
            r13.append(r9)
        L1c8e:
            int r9 = (r14 > r124 ? 1 : (r14 == r124 ? 0 : -1))
            if (r9 == 0) goto L1ca1
            if (r-103 == 0) goto L1c97
            r13.append(r6)
        L1c97:
            r153 = 1
            formatTimeMs(r13, r14)
            java.lang.String r9 = "actual background partial"
            r13.append(r9)
        L1ca1:
            int r9 = (r2 > r124 ? 1 : (r2 == r124 ? 0 : -1))
            if (r9 == 0) goto L1cb5
            if (r-103 == 0) goto L1caa
            r13.append(r6)
        L1caa:
            r153 = 1
            formatTimeMs(r13, r2)
            java.lang.String r9 = "window"
            r13.append(r9)
        L1cb5:
            int r9 = (r4 > r124 ? 1 : (r4 == r124 ? 0 : -1))
            if (r9 == 0) goto L1cca
            if (r-103 == 0) goto L1cc0
            java.lang.String r9 = ","
            r13.append(r9)
        L1cc0:
            r153 = 1
            formatTimeMs(r13, r4)
            java.lang.String r9 = "draw"
            r13.append(r9)
        L1cca:
            r10 = r97
            r13.append(r10)
            java.lang.String r9 = r13.toString()
            r7.println(r9)
            goto L1ce0
        L1cd7:
            r10 = r1
            r187 = r8
            r152 = r15
            r6 = r169
            r8 = r237
        L1ce0:
            android.os.BatteryStats$Timer r0 = r180.getMulticastWakelockStats()
            if (r0 == 0) goto L1d27
            r1 = r238
            r14 = r202
            long r189 = r0.getTotalTimeLocked(r14, r1)
            int r9 = r0.getCountLocked(r1)
            int r97 = (r189 > r124 ? 1 : (r189 == r124 ? 0 : -1))
            if (r97 <= 0) goto L1d22
            r97 = r0
            r0 = 0
            r13.setLength(r0)
            r13.append(r8)
            java.lang.String r0 = "    WiFi Multicast Wakelock"
            r13.append(r0)
            java.lang.String r0 = " count = "
            r13.append(r0)
            r13.append(r9)
            java.lang.String r0 = " time = "
            r13.append(r0)
            long r191 = r189 + r18
            r197 = r2
            long r2 = r191 / r16
            formatTimeMsNoSpace(r13, r2)
            java.lang.String r0 = r13.toString()
            r7.println(r0)
            goto L1d2f
        L1d22:
            r97 = r0
            r197 = r2
            goto L1d2f
        L1d27:
            r1 = r238
            r97 = r0
            r197 = r2
            r14 = r202
        L1d2f:
            android.util.ArrayMap r0 = r180.getSyncStats()
            int r2 = r0.size()
            r3 = 1
            int r2 = r2 - r3
        L1d39:
            if (r2 < 0) goto L1df0
            java.lang.Object r9 = r0.valueAt(r2)
            android.os.BatteryStats$Timer r9 = (android.os.BatteryStats.Timer) r9
            long r189 = r9.getTotalTimeLocked(r14, r1)
            long r189 = r189 + r18
            r191 = r4
            long r3 = r189 / r16
            int r5 = r9.getCountLocked(r1)
            r169 = r10
            android.os.BatteryStats$Timer r10 = r9.getSubTimer()
            if (r10 == 0) goto L1d60
            r189 = r11
            r11 = r185
            long r185 = r10.getTotalDurationMsLocked(r11)
            goto L1d66
        L1d60:
            r189 = r11
            r11 = r185
            r185 = -1
        L1d66:
            r202 = r185
            if (r10 == 0) goto L1d6f
            int r153 = r10.getCountLocked(r1)
            goto L1d71
        L1d6f:
            r153 = -1
        L1d71:
            r185 = r153
            r186 = r9
            r9 = 0
            r13.setLength(r9)
            r13.append(r8)
            java.lang.String r9 = "    Sync "
            r13.append(r9)
            java.lang.Object r9 = r0.keyAt(r2)
            java.lang.String r9 = (java.lang.String) r9
            r13.append(r9)
            r9 = r155
            r13.append(r9)
            int r153 = (r3 > r124 ? 1 : (r3 == r124 ? 0 : -1))
            if (r-103 == 0) goto L1dc8
            formatTimeMs(r13, r3)
            r155 = r0
            java.lang.String r0 = "realtime ("
            r13.append(r0)
            r13.append(r5)
            r0 = r171
            r13.append(r0)
            r206 = r3
            r3 = r202
            int r153 = (r3 > r124 ? 1 : (r3 == r124 ? 0 : -1))
            if (r-103 <= 0) goto L1dc3
            r13.append(r6)
            formatTimeMs(r13, r3)
            r202 = r3
            java.lang.String r3 = "background ("
            r13.append(r3)
            r3 = r185
            r13.append(r3)
            r13.append(r0)
            goto L1dd5
        L1dc3:
            r202 = r3
            r3 = r185
            goto L1dd5
        L1dc8:
            r155 = r0
            r206 = r3
            r0 = r171
            r3 = r185
            java.lang.String r4 = "(not used)"
            r13.append(r4)
        L1dd5:
            java.lang.String r4 = r13.toString()
            r7.println(r4)
            r96 = 1
            int r2 = r2 + (-1)
            r171 = r0
            r185 = r11
            r0 = r155
            r10 = r169
            r11 = r189
            r4 = r191
            r155 = r9
            goto L1d39
        L1df0:
            r191 = r4
            r169 = r10
            r189 = r11
            r9 = r155
            r11 = r185
            r155 = r0
            r0 = r171
            android.util.ArrayMap r2 = r180.getJobStats()
            int r3 = r2.size()
            r4 = 1
            int r3 = r3 - r4
        L1e08:
            if (r3 < 0) goto L1ead
            java.lang.Object r4 = r2.valueAt(r3)
            android.os.BatteryStats$Timer r4 = (android.os.BatteryStats.Timer) r4
            long r185 = r4.getTotalTimeLocked(r14, r1)
            long r185 = r185 + r18
            r202 = r14
            long r14 = r185 / r16
            int r5 = r4.getCountLocked(r1)
            android.os.BatteryStats$Timer r10 = r4.getSubTimer()
            if (r10 == 0) goto L1e29
            long r185 = r10.getTotalDurationMsLocked(r11)
            goto L1e2b
        L1e29:
            r185 = -1
        L1e2b:
            r206 = r185
            if (r10 == 0) goto L1e34
            int r171 = r10.getCountLocked(r1)
            goto L1e36
        L1e34:
            r171 = -1
        L1e36:
            r185 = r171
            r171 = r4
            r4 = 0
            r13.setLength(r4)
            r13.append(r8)
            java.lang.String r4 = "    Job "
            r13.append(r4)
            java.lang.Object r4 = r2.keyAt(r3)
            java.lang.String r4 = (java.lang.String) r4
            r13.append(r4)
            r13.append(r9)
            int r4 = (r14 > r124 ? 1 : (r14 == r124 ? 0 : -1))
            if (r4 == 0) goto L1e8b
            formatTimeMs(r13, r14)
            java.lang.String r4 = "realtime ("
            r13.append(r4)
            r13.append(r5)
            r13.append(r0)
            r186 = r5
            r4 = r206
            int r206 = (r4 > r124 ? 1 : (r4 == r124 ? 0 : -1))
            if (r-50 <= 0) goto L1e84
            r13.append(r6)
            formatTimeMs(r13, r4)
            r206 = r2
            java.lang.String r2 = "background ("
            r13.append(r2)
            r2 = r185
            r13.append(r2)
            r13.append(r0)
            r185 = r0
            goto L1e9a
        L1e84:
            r206 = r2
            r2 = r185
            r185 = r0
            goto L1e9a
        L1e8b:
            r186 = r5
            r4 = r206
            r206 = r2
            r2 = r185
            r185 = r0
            java.lang.String r0 = "(not used)"
            r13.append(r0)
        L1e9a:
            java.lang.String r0 = r13.toString()
            r7.println(r0)
            r96 = 1
            int r3 = r3 + (-1)
            r0 = r185
            r14 = r202
            r2 = r206
            goto L1e08
        L1ead:
            r185 = r0
            r206 = r2
            r202 = r14
            android.util.ArrayMap r0 = r180.getJobCompletionStats()
            int r2 = r0.size()
            r3 = 1
            int r2 = r2 - r3
        L1ebd:
            if (r2 < 0) goto L1f1c
            java.lang.Object r4 = r0.valueAt(r2)
            android.util.SparseIntArray r4 = (android.util.SparseIntArray) r4
            if (r4 == 0) goto L1f11
            r236.print(r237)
            java.lang.String r5 = "    Job Completions "
            r7.print(r5)
            java.lang.Object r5 = r0.keyAt(r2)
            java.lang.String r5 = (java.lang.String) r5
            r7.print(r5)
            java.lang.String r5 = ":"
            r7.print(r5)
            r5 = 0
        L1ede:
            int r10 = r4.size()
            if (r5 >= r10) goto L1f09
            r14 = r164
            r7.print(r14)
            int r10 = r4.keyAt(r5)
            java.lang.String r10 = android.app.job.JobParameters.getInternalReasonCodeDescription(r10)
            r7.print(r10)
            r10 = r205
            r7.print(r10)
            int r15 = r4.valueAt(r5)
            r7.print(r15)
            java.lang.String r15 = "x)"
            r7.print(r15)
            int r5 = r5 + 1
            goto L1ede
        L1f09:
            r14 = r164
            r10 = r205
            r236.println()
            goto L1f15
        L1f11:
            r14 = r164
            r10 = r205
        L1f15:
            int r2 = r2 + (-1)
            r205 = r10
            r164 = r14
            goto L1ebd
        L1f1c:
            r14 = r164
            r10 = r205
            r2 = r180
            r2.getDeferredJobsLineLocked(r13, r1)
            int r4 = r13.length()
            if (r4 <= 0) goto L1f37
            java.lang.String r4 = "    Jobs deferred on launch "
            r7.print(r4)
            java.lang.String r4 = r13.toString()
            r7.println(r4)
        L1f37:
            android.os.BatteryStats$Timer r4 = r2.getFlashlightTurnedOnTimer()
            java.lang.String r15 = "Flashlight"
            r8 = r236
            r5 = r9
            r153 = r204
            r228 = r187
            r186 = r195
            r195 = r228
            r230 = r36
            r36 = r165
            r164 = r230
            r9 = r13
            r166 = r10
            r228 = r130
            r130 = r0
            r0 = r21
            r21 = r169
            r169 = r3
            r3 = r228
            r10 = r4
            r4 = r237
            r1 = r14
            r14 = r185
            r204 = r189
            r188 = r183
            r183 = r178
            r178 = r150
            r150 = r133
            r151 = r147
            r147 = r168
            r133 = r6
            r6 = r11
            r11 = r202
            r168 = r13
            r228 = r136
            r136 = r20
            r20 = r138
            r138 = r228
            r13 = r238
            r180 = r0
            r185 = r1
            r171 = r3
            r1 = r14
            r3 = r236
            r0 = r238
            r14 = r237
            r132 = r152
            r152 = r154
            r3 = 0
            r154 = r133
            r133 = 5
            boolean r8 = printTimer(r8, r9, r10, r11, r13, r14, r15)
            r96 = r96 | r8
            android.os.BatteryStats$Timer r10 = r2.getCameraTurnedOnTimer()
            java.lang.String r15 = "Camera"
            r8 = r236
            r9 = r168
            boolean r8 = printTimer(r8, r9, r10, r11, r13, r14, r15)
            r96 = r96 | r8
            android.os.BatteryStats$Timer r10 = r2.getVideoTurnedOnTimer()
            java.lang.String r15 = "Video"
            r8 = r236
            boolean r8 = printTimer(r8, r9, r10, r11, r13, r14, r15)
            r96 = r96 | r8
            android.os.BatteryStats$Timer r10 = r2.getAudioTurnedOnTimer()
            java.lang.String r15 = "Audio"
            r8 = r236
            boolean r8 = printTimer(r8, r9, r10, r11, r13, r14, r15)
            r8 = r96 | r8
            android.util.SparseArray r15 = r2.getSensorStats()
            int r14 = r15.size()
            r9 = 0
            r96 = r8
        L1fd5:
            if (r9 >= r14) goto L20c3
            java.lang.Object r8 = r15.valueAt(r9)
            android.os.BatteryStats$Uid$Sensor r8 = (android.os.BatteryStats.Uid.Sensor) r8
            int r10 = r15.keyAt(r9)
            r13 = r168
            r13.setLength(r3)
            r13.append(r4)
            java.lang.String r11 = "    Sensor "
            r13.append(r11)
            int r11 = r8.getHandle()
            r12 = -10000(0xffffffffffffd8f0, float:NaN)
            if (r11 != r12) goto L1ffc
            java.lang.String r12 = "GPS"
            r13.append(r12)
            goto L1fff
        L1ffc:
            r13.append(r11)
        L1fff:
            r13.append(r5)
            android.os.BatteryStats$Timer r12 = r8.getSensorTime()
            if (r12 == 0) goto L2094
            r3 = r202
            long r202 = r12.getTotalTimeLocked(r3, r0)
            long r202 = r202 + r18
            r168 = r10
            r190 = r11
            long r10 = r202 / r16
            r201 = r14
            int r14 = r12.getCountLocked(r0)
            r202 = r15
            android.os.BatteryStats$Timer r15 = r8.getSensorBackgroundTime()
            if (r15 == 0) goto L2029
            int r203 = r15.getCountLocked(r0)
            goto L202b
        L2029:
            r203 = 0
        L202b:
            r207 = r203
            r208 = r3
            long r3 = r12.getTotalDurationMsLocked(r6)
            if (r15 == 0) goto L203a
            long r210 = r15.getTotalDurationMsLocked(r6)
            goto L203c
        L203a:
            r210 = r124
        L203c:
            r212 = r210
            int r203 = (r10 > r124 ? 1 : (r10 == r124 ? 0 : -1))
            if (r-53 == 0) goto L2086
            int r203 = (r3 > r10 ? 1 : (r3 == r10 ? 0 : -1))
            if (r-53 == 0) goto L2051
            formatTimeMs(r13, r10)
            r210 = r6
            java.lang.String r6 = "blamed realtime, "
            r13.append(r6)
            goto L2053
        L2051:
            r210 = r6
        L2053:
            formatTimeMs(r13, r3)
            java.lang.String r6 = "realtime ("
            r13.append(r6)
            r13.append(r14)
            r13.append(r1)
            r6 = r212
            int r203 = (r6 > r124 ? 1 : (r6 == r124 ? 0 : -1))
            if (r-53 != 0) goto L206e
            r212 = r3
            r3 = r207
            if (r3 <= 0) goto L2093
            goto L2072
        L206e:
            r212 = r3
            r3 = r207
        L2072:
            r4 = r154
            r13.append(r4)
            formatTimeMs(r13, r6)
            java.lang.String r4 = "background ("
            r13.append(r4)
            r13.append(r3)
            r13.append(r1)
            goto L2093
        L2086:
            r210 = r6
            r6 = r212
            r212 = r3
            r3 = r207
            java.lang.String r4 = "(not used)"
            r13.append(r4)
        L2093:
            goto L20a5
        L2094:
            r210 = r6
            r168 = r10
            r190 = r11
            r201 = r14
            r208 = r202
            r202 = r15
            java.lang.String r3 = "(not used)"
            r13.append(r3)
        L20a5:
            java.lang.String r3 = r13.toString()
            r4 = r236
            r6 = r154
            r4.println(r3)
            r96 = 1
            int r9 = r9 + 1
            r4 = r237
            r168 = r13
            r14 = r201
            r15 = r202
            r202 = r208
            r6 = r210
            r3 = 0
            goto L1fd5
        L20c3:
            r4 = r236
            r210 = r6
            r201 = r14
            r6 = r154
            r13 = r168
            r208 = r202
            r202 = r15
            android.os.BatteryStats$Timer r10 = r2.getVibratorOnTimer()
            java.lang.String r15 = "Vibrator"
            r8 = r236
            r9 = r13
            r11 = r208
            r1 = r13
            r13 = r238
            r3 = r201
            r14 = r237
            r7 = r202
            boolean r8 = printTimer(r8, r9, r10, r11, r13, r14, r15)
            r96 = r96 | r8
            android.os.BatteryStats$Timer r10 = r2.getForegroundActivityTimer()
            java.lang.String r15 = "Foreground activities"
            r8 = r236
            r9 = r1
            boolean r8 = printTimer(r8, r9, r10, r11, r13, r14, r15)
            r96 = r96 | r8
            android.os.BatteryStats$Timer r10 = r2.getForegroundServiceTimer()
            java.lang.String r15 = "Foreground services"
            r8 = r236
            boolean r8 = printTimer(r8, r9, r10, r11, r13, r14, r15)
            r8 = r96 | r8
            r9 = 0
            r11 = 0
        L210b:
            r12 = 7
            if (r11 >= r12) goto L215b
            r12 = r208
            long r14 = r2.getProcessStateTime(r11, r12, r0)
            int r96 = (r14 > r124 ? 1 : (r14 == r124 ? 0 : -1))
            if (r96 <= 0) goto L214a
            long r9 = r9 + r14
            r201 = r3
            r3 = 0
            r1.setLength(r3)
            r3 = r237
            r1.append(r3)
            r202 = r7
            java.lang.String r7 = "    "
            r1.append(r7)
            java.lang.String[] r7 = android.os.BatteryStats.Uid.PROCESS_STATE_NAMES
            r7 = r7[r11]
            r1.append(r7)
            java.lang.String r7 = " for: "
            r1.append(r7)
            long r207 = r14 + r18
            r96 = r8
            long r7 = r207 / r16
            formatTimeMs(r1, r7)
            java.lang.String r7 = r1.toString()
            r4.println(r7)
            r7 = 1
            r8 = r7
            goto L2152
        L214a:
            r201 = r3
            r202 = r7
            r96 = r8
            r3 = r237
        L2152:
            int r11 = r11 + 1
            r208 = r12
            r3 = r201
            r7 = r202
            goto L210b
        L215b:
            r201 = r3
            r202 = r7
            r96 = r8
            r12 = r208
            r3 = r237
            int r7 = (r9 > r124 ? 1 : (r9 == r124 ? 0 : -1))
            if (r7 <= 0) goto L2183
            r7 = 0
            r1.setLength(r7)
            r1.append(r3)
            java.lang.String r7 = "    Total running: "
            r1.append(r7)
            long r7 = r9 + r18
            long r7 = r7 / r16
            formatTimeMs(r1, r7)
            java.lang.String r7 = r1.toString()
            r4.println(r7)
        L2183:
            long r7 = r2.getUserCpuTimeUs(r0)
            long r14 = r2.getSystemCpuTimeUs(r0)
            int r11 = (r7 > r124 ? 1 : (r7 == r124 ? 0 : -1))
            if (r11 > 0) goto L2197
            int r11 = (r14 > r124 ? 1 : (r14 == r124 ? 0 : -1))
            if (r11 <= 0) goto L2194
            goto L2197
        L2194:
            r207 = r9
            goto L21bb
        L2197:
            r11 = 0
            r1.setLength(r11)
            r1.append(r3)
            java.lang.String r11 = "    Total cpu time: u="
            r1.append(r11)
            r207 = r9
            long r9 = r7 / r16
            formatTimeMs(r1, r9)
            java.lang.String r9 = "s="
            r1.append(r9)
            long r9 = r14 / r16
            formatTimeMs(r1, r9)
            java.lang.String r9 = r1.toString()
            r4.println(r9)
        L21bb:
            long[] r9 = r2.getCpuFreqTimes(r0)
            if (r9 == 0) goto L21ea
            r10 = 0
            r1.setLength(r10)
            java.lang.String r10 = "    Total cpu time per freq:"
            r1.append(r10)
            r10 = 0
        L21cb:
            int r11 = r9.length
            if (r10 >= r11) goto L21e0
            r11 = 32
            java.lang.StringBuilder r11 = r1.append(r11)
            r212 = r7
            r7 = r9[r10]
            r11.append(r7)
            int r10 = r10 + 1
            r7 = r212
            goto L21cb
        L21e0:
            r212 = r7
            java.lang.String r7 = r1.toString()
            r4.println(r7)
            goto L21ec
        L21ea:
            r212 = r7
        L21ec:
            long[] r7 = r2.getScreenOffCpuFreqTimes(r0)
            if (r7 == 0) goto L221b
            r8 = 0
            r1.setLength(r8)
            java.lang.String r8 = "    Total screen-off cpu time per freq:"
            r1.append(r8)
            r8 = 0
        L21fc:
            int r10 = r7.length
            if (r8 >= r10) goto L2211
            r10 = 32
            java.lang.StringBuilder r10 = r1.append(r10)
            r215 = r12
            r11 = r7[r8]
            r10.append(r11)
            int r8 = r8 + 1
            r12 = r215
            goto L21fc
        L2211:
            r215 = r12
            java.lang.String r8 = r1.toString()
            r4.println(r8)
            goto L221d
        L221b:
            r215 = r12
        L221d:
            int r8 = r234.getCpuFreqCount()
            long[] r8 = new long[r8]
            r10 = 0
        L2224:
            r11 = 7
            if (r10 >= r11) goto L22a6
            boolean r11 = r2.getCpuFreqTimes(r8, r10)
            if (r11 == 0) goto L2266
            r11 = 0
            r1.setLength(r11)
            java.lang.String r11 = "    Cpu times per freq at state "
            java.lang.StringBuilder r11 = r1.append(r11)
            java.lang.String[] r12 = android.os.BatteryStats.Uid.PROCESS_STATE_NAMES
            r12 = r12[r10]
            java.lang.StringBuilder r11 = r11.append(r12)
            r12 = 58
            r11.append(r12)
            r11 = 0
        L2245:
            int r12 = r8.length
            if (r11 >= r12) goto L225a
            r12 = r185
            java.lang.StringBuilder r13 = r1.append(r12)
            r217 = r14
            r14 = r8[r11]
            r13.append(r14)
            int r11 = r11 + 1
            r14 = r217
            goto L2245
        L225a:
            r217 = r14
            r12 = r185
            java.lang.String r11 = r1.toString()
            r4.println(r11)
            goto L226a
        L2266:
            r217 = r14
            r12 = r185
        L226a:
            boolean r11 = r2.getScreenOffCpuFreqTimes(r8, r10)
            if (r11 == 0) goto L229e
            r11 = 0
            r1.setLength(r11)
            java.lang.String r11 = "   Screen-off cpu times per freq at state "
            java.lang.StringBuilder r11 = r1.append(r11)
            java.lang.String[] r13 = android.os.BatteryStats.Uid.PROCESS_STATE_NAMES
            r13 = r13[r10]
            java.lang.StringBuilder r11 = r11.append(r13)
            r13 = 58
            r11.append(r13)
            r11 = 0
        L2288:
            int r13 = r8.length
            if (r11 >= r13) goto L2297
            java.lang.StringBuilder r13 = r1.append(r12)
            r14 = r8[r11]
            r13.append(r14)
            int r11 = r11 + 1
            goto L2288
        L2297:
            java.lang.String r11 = r1.toString()
            r4.println(r11)
        L229e:
            int r10 = r10 + 1
            r185 = r12
            r14 = r217
            goto L2224
        L22a6:
            r217 = r14
            r12 = r185
            android.util.ArrayMap r10 = r2.getProcessStats()
            int r11 = r10.size()
            int r11 = r11 + (-1)
        L22b5:
            if (r11 < 0) goto L244a
            java.lang.Object r13 = r10.valueAt(r11)
            android.os.BatteryStats$Uid$Proc r13 = (android.os.BatteryStats.Uid.Proc) r13
            long r14 = r13.getUserTime(r0)
            r154 = r7
            r168 = r8
            long r7 = r13.getSystemTime(r0)
            r185 = r5
            long r4 = r13.getForegroundTime(r0)
            r190 = r9
            int r9 = r13.getStarts(r0)
            r203 = r12
            int r12 = r13.getNumCrashes(r0)
            r209 = r2
            int r2 = r13.getNumAnrs(r0)
            if (r0 != 0) goto L22e8
            int r219 = r13.countExcessivePowers()
            goto L22ea
        L22e8:
            r219 = 0
        L22ea:
            r220 = r219
            int r219 = (r14 > r124 ? 1 : (r14 == r124 ? 0 : -1))
            if (r-37 != 0) goto L230f
            int r219 = (r7 > r124 ? 1 : (r7 == r124 ? 0 : -1))
            if (r-37 != 0) goto L230f
            int r219 = (r4 > r124 ? 1 : (r4 == r124 ? 0 : -1))
            if (r-37 != 0) goto L230f
            if (r9 != 0) goto L230f
            r0 = r220
            if (r0 != 0) goto L2311
            if (r12 != 0) goto L2311
            if (r2 == 0) goto L2303
            goto L2311
        L2303:
            r5 = r236
            r223 = r6
            r6 = r180
            r13 = r214
            r180 = r1
            goto L242f
        L230f:
            r0 = r220
        L2311:
            r219 = r13
            r13 = 0
            r1.setLength(r13)
            r1.append(r3)
            java.lang.String r13 = "    Proc "
            r1.append(r13)
            java.lang.Object r13 = r10.keyAt(r11)
            java.lang.String r13 = (java.lang.String) r13
            r1.append(r13)
            java.lang.String r13 = ":\n"
            r1.append(r13)
            r1.append(r3)
            java.lang.String r13 = "      CPU: "
            r1.append(r13)
            formatTimeMs(r1, r14)
            java.lang.String r13 = "usr + "
            r1.append(r13)
            formatTimeMs(r1, r7)
            java.lang.String r13 = "krn ; "
            r1.append(r13)
            formatTimeMs(r1, r4)
            java.lang.String r13 = "fg"
            r1.append(r13)
            if (r9 != 0) goto L235a
            if (r12 != 0) goto L235a
            if (r2 == 0) goto L2355
            goto L235a
        L2355:
            r220 = r4
            r13 = r214
            goto L2394
        L235a:
            r13 = r214
            r1.append(r13)
            r1.append(r3)
            r220 = r4
            java.lang.String r4 = "      "
            r1.append(r4)
            r4 = 0
            if (r9 == 0) goto L2375
            r4 = 1
            r1.append(r9)
            java.lang.String r5 = " starts"
            r1.append(r5)
        L2375:
            if (r12 == 0) goto L2385
            if (r4 == 0) goto L237c
            r1.append(r6)
        L237c:
            r4 = 1
            r1.append(r12)
            java.lang.String r5 = " crashes"
            r1.append(r5)
        L2385:
            if (r2 == 0) goto L2394
            if (r4 == 0) goto L238c
            r1.append(r6)
        L238c:
            r1.append(r2)
            java.lang.String r5 = " anrs"
            r1.append(r5)
        L2394:
            java.lang.String r4 = r1.toString()
            r5 = r236
            r5.println(r4)
            r4 = 0
        L239e:
            if (r4 >= r0) goto L241e
            r214 = r0
            r0 = r219
            r219 = r2
            android.os.BatteryStats$Uid$Proc$ExcessivePower r2 = r0.getExcessivePower(r4)
            if (r2 == 0) goto L2403
            r236.print(r237)
            r222 = r0
            java.lang.String r0 = "      * Killed for "
            r5.print(r0)
            int r0 = r2.type
            r223 = r6
            r6 = 2
            if (r0 != r6) goto L23c3
            java.lang.String r0 = "cpu"
            r5.print(r0)
            goto L23c9
        L23c3:
            java.lang.String r0 = "unknown"
            r5.print(r0)
        L23c9:
            java.lang.String r0 = " use: "
            r5.print(r0)
            r224 = r7
            long r6 = r2.usedTime
            android.util.TimeUtils.formatDuration(r6, r5)
            java.lang.String r6 = " over "
            r5.print(r6)
            long r6 = r2.overTime
            android.util.TimeUtils.formatDuration(r6, r5)
            long r6 = r2.overTime
            int r6 = (r6 > r124 ? 1 : (r6 == r124 ? 0 : -1))
            if (r6 == 0) goto L23fe
            r6 = r180
            r5.print(r6)
            long r7 = r2.usedTime
            r226 = 100
            long r7 = r7 * r226
            r180 = r1
            long r0 = r2.overTime
            long r7 = r7 / r0
            r5.print(r7)
            java.lang.String r0 = "%)"
            r5.println(r0)
            goto L240d
        L23fe:
            r6 = r180
            r180 = r1
            goto L240d
        L2403:
            r222 = r0
            r223 = r6
            r224 = r7
            r6 = r180
            r180 = r1
        L240d:
            int r4 = r4 + 1
            r1 = r180
            r0 = r214
            r2 = r219
            r219 = r222
            r7 = r224
            r180 = r6
            r6 = r223
            goto L239e
        L241e:
            r214 = r0
            r223 = r6
            r224 = r7
            r6 = r180
            r222 = r219
            r180 = r1
            r219 = r2
            r0 = 1
            r96 = r0
        L242f:
            int r11 = r11 + (-1)
            r0 = r238
            r4 = r5
            r214 = r13
            r7 = r154
            r8 = r168
            r1 = r180
            r5 = r185
            r9 = r190
            r12 = r203
            r2 = r209
            r180 = r6
            r6 = r223
            goto L22b5
        L244a:
            r209 = r2
            r185 = r5
            r154 = r7
            r168 = r8
            r190 = r9
            r203 = r12
            r6 = r180
            r180 = r1
            r5 = r4
            android.util.ArrayMap r0 = r209.getPackageStats()
            int r1 = r0.size()
            int r1 = r1 + (-1)
        L2466:
            if (r1 < 0) goto L2583
            r236.print(r237)
            java.lang.String r2 = "    Apk "
            r5.print(r2)
            java.lang.Object r2 = r0.keyAt(r1)
            java.lang.String r2 = (java.lang.String) r2
            r5.print(r2)
            java.lang.String r2 = ":"
            r5.println(r2)
            r2 = 0
            java.lang.Object r4 = r0.valueAt(r1)
            android.os.BatteryStats$Uid$Pkg r4 = (android.os.BatteryStats.Uid.Pkg) r4
            android.util.ArrayMap r7 = r4.getWakeupAlarmStats()
            int r8 = r7.size()
            int r8 = r8 + (-1)
        L248f:
            if (r8 < 0) goto L24bf
            r236.print(r237)
            java.lang.String r9 = "      Wakeup alarm "
            r5.print(r9)
            java.lang.Object r9 = r7.keyAt(r8)
            java.lang.String r9 = (java.lang.String) r9
            r5.print(r9)
            r9 = r185
            r5.print(r9)
            java.lang.Object r11 = r7.valueAt(r8)
            android.os.BatteryStats$Counter r11 = (android.os.BatteryStats.Counter) r11
            r12 = r238
            int r11 = r11.getCountLocked(r12)
            r5.print(r11)
            java.lang.String r11 = " times"
            r5.println(r11)
            r2 = 1
            int r8 = r8 + (-1)
            goto L248f
        L24bf:
            r12 = r238
            r9 = r185
            android.util.ArrayMap r8 = r4.getServiceStats()
            int r11 = r8.size()
            int r11 = r11 + (-1)
        L24cd:
            if (r11 < 0) goto L255b
            java.lang.Object r13 = r8.valueAt(r11)
            android.os.BatteryStats$Uid$Pkg$Serv r13 = (android.os.BatteryStats.Uid.Pkg.Serv) r13
            r14 = r126
            long r126 = r13.getStartTime(r14, r12)
            r185 = r0
            int r0 = r13.getStarts(r12)
            r214 = r4
            int r4 = r13.getLaunches(r12)
            int r219 = (r126 > r124 ? 1 : (r126 == r124 ? 0 : -1))
            if (r-37 != 0) goto L24f9
            if (r0 != 0) goto L24f9
            if (r4 == 0) goto L24f0
            goto L24f9
        L24f0:
            r219 = r6
            r167 = r7
            r220 = r8
            r6 = r180
            goto L2549
        L24f9:
            r219 = r6
            r167 = r7
            r6 = r180
            r7 = 0
            r6.setLength(r7)
            r6.append(r3)
            java.lang.String r7 = "      Service "
            r6.append(r7)
            java.lang.Object r7 = r8.keyAt(r11)
            java.lang.String r7 = (java.lang.String) r7
            r6.append(r7)
            java.lang.String r7 = ":\n"
            r6.append(r7)
            r6.append(r3)
            java.lang.String r7 = "        Created for: "
            r6.append(r7)
            r220 = r8
            long r7 = r126 / r16
            formatTimeMs(r6, r7)
            java.lang.String r7 = "uptime\n"
            r6.append(r7)
            r6.append(r3)
            java.lang.String r7 = "        Starts: "
            r6.append(r7)
            r6.append(r0)
            java.lang.String r7 = ", launches: "
            r6.append(r7)
            r6.append(r4)
            java.lang.String r7 = r6.toString()
            r5.println(r7)
            r2 = 1
        L2549:
            int r11 = r11 + (-1)
            r180 = r6
            r126 = r14
            r7 = r167
            r0 = r185
            r4 = r214
            r6 = r219
            r8 = r220
            goto L24cd
        L255b:
            r185 = r0
            r214 = r4
            r219 = r6
            r167 = r7
            r220 = r8
            r14 = r126
            r6 = r180
            if (r2 != 0) goto L2573
            r236.print(r237)
            java.lang.String r0 = "      (nothing executed)"
            r5.println(r0)
        L2573:
            r96 = 1
            int r1 = r1 + (-1)
            r180 = r6
            r126 = r14
            r0 = r185
            r6 = r219
            r185 = r9
            goto L2466
        L2583:
            r12 = r238
            r219 = r6
            r14 = r126
            r6 = r180
            r9 = r185
            r185 = r0
            if (r96 != 0) goto L2599
            r236.print(r237)
            java.lang.String r0 = "    (nothing executed)"
            r5.println(r0)
        L2599:
            int r13 = r20 + 1
            r155 = r9
            r126 = r14
            r7 = r21
            r15 = r117
            r145 = r128
            r168 = r147
            r133 = r150
            r169 = r152
            r172 = r153
            r163 = r162
            r162 = r166
            r171 = r170
            r170 = r203
            r20 = r210
            r36 = r215
            r152 = r219
            r1 = 0
            r14 = r5
            r5 = r6
            r6 = r45
            r165 = r160
            goto L13f5
        L25c4:
            return
        */
        throw new UnsupportedOperationException("Method not decompiled: android.os.BatteryStats.dumpLocked(android.content.Context, java.io.PrintWriter, java.lang.String, int, int, boolean):void");
    }

    static void printBitDescriptions(StringBuilder sb, int oldval, int newval, HistoryTag wakelockTag, BitDescription[] descriptions, boolean longNames) {
        int diff = oldval ^ newval;
        if (diff == 0) {
            return;
        }
        boolean didWake = false;
        for (BitDescription bd : descriptions) {
            if ((bd.mask & diff) != 0) {
                sb.append(longNames ? " " : ",");
                if (bd.shift < 0) {
                    sb.append((bd.mask & newval) != 0 ? "+" : NativeLibraryHelper.CLEAR_ABI_OVERRIDE);
                    sb.append(longNames ? bd.name : bd.shortName);
                    if (bd.mask == 1073741824 && wakelockTag != null) {
                        didWake = true;
                        sb.append("=");
                        if (longNames) {
                            UserHandle.formatUid(sb, wakelockTag.uid);
                            sb.append(":\"");
                            sb.append(wakelockTag.string);
                            sb.append("\"");
                        } else {
                            sb.append(wakelockTag.poolIdx);
                        }
                    }
                } else {
                    sb.append(longNames ? bd.name : bd.shortName);
                    sb.append("=");
                    int val = (bd.mask & newval) >> bd.shift;
                    if (bd.values != null && val >= 0 && val < bd.values.length) {
                        sb.append(longNames ? bd.values[val] : bd.shortValues[val]);
                    } else {
                        sb.append(val);
                    }
                }
            }
        }
        if (!didWake && wakelockTag != null) {
            sb.append(longNames ? " wake_lock=" : ",w=");
            if (longNames) {
                UserHandle.formatUid(sb, wakelockTag.uid);
                sb.append(":\"");
                sb.append(wakelockTag.string);
                sb.append("\"");
                return;
            }
            sb.append(wakelockTag.poolIdx);
        }
    }

    public void prepareForDumpLocked() {
    }

    /* loaded from: classes2.dex */
    public static class HistoryPrinter {
        int oldState = 0;
        int oldState2 = 0;
        int oldLevel = -1;
        int oldStatus = -1;
        int oldHealth = -1;
        int oldPlug = -1;
        int oldTemp = -1;
        int oldVolt = -1;
        int oldChargeMAh = -1;
        double oldModemRailChargeMah = -1.0d;
        double oldWifiRailChargeMah = -1.0d;
        long lastTime = -1;

        void reset() {
            this.oldState2 = 0;
            this.oldState = 0;
            this.oldLevel = -1;
            this.oldStatus = -1;
            this.oldHealth = -1;
            this.oldPlug = -1;
            this.oldTemp = -1;
            this.oldVolt = -1;
            this.oldChargeMAh = -1;
            this.oldModemRailChargeMah = -1.0d;
            this.oldWifiRailChargeMah = -1.0d;
        }

        public void printNextItem(PrintWriter pw, HistoryItem rec, long baseTime, boolean checkin, boolean verbose) {
            pw.print(printNextItem(rec, baseTime, checkin, verbose));
        }

        public void printNextItem(ProtoOutputStream proto, HistoryItem rec, long baseTime, boolean verbose) {
            String[] split;
            String item = printNextItem(rec, baseTime, true, verbose);
            for (String line : item.split("\n")) {
                proto.write(2237677961222L, line);
            }
        }

        private String printNextItem(HistoryItem rec, long baseTime, boolean checkin, boolean verbose) {
            String str;
            StringBuilder item = new StringBuilder();
            if (!checkin) {
                item.append("  ");
                TimeUtils.formatDuration(rec.time - baseTime, item, 19);
                item.append(" (");
                item.append(rec.numReadInts);
                item.append(") ");
            } else {
                item.append(9);
                item.append(',');
                item.append(BatteryStats.HISTORY_DATA);
                item.append(',');
                if (this.lastTime >= 0) {
                    item.append(rec.time - this.lastTime);
                } else {
                    item.append(rec.time - baseTime);
                }
                this.lastTime = rec.time;
            }
            if (rec.cmd == 4) {
                if (checkin) {
                    item.append(":");
                }
                item.append("START\n");
                reset();
            } else {
                if (rec.cmd == 5) {
                    str = ":";
                } else if (rec.cmd == 7) {
                    str = ":";
                } else if (rec.cmd == 8) {
                    if (checkin) {
                        item.append(":");
                    }
                    item.append("SHUTDOWN\n");
                } else if (rec.cmd == 6) {
                    if (checkin) {
                        item.append(":");
                    }
                    item.append("*OVERFLOW*\n");
                } else {
                    if (!checkin) {
                        if (rec.batteryLevel < 10) {
                            item.append("00");
                        } else if (rec.batteryLevel < 100) {
                            item.append(AudioSystem.LEGACY_REMOTE_SUBMIX_ADDRESS);
                        }
                        item.append(rec.batteryLevel);
                        if (verbose) {
                            item.append(" ");
                            if (rec.states >= 0) {
                                if (rec.states < 16) {
                                    item.append("0000000");
                                } else if (rec.states < 256) {
                                    item.append("000000");
                                } else if (rec.states < 4096) {
                                    item.append("00000");
                                } else if (rec.states < 65536) {
                                    item.append("0000");
                                } else if (rec.states < 1048576) {
                                    item.append("000");
                                } else if (rec.states < 16777216) {
                                    item.append("00");
                                } else if (rec.states < 268435456) {
                                    item.append(AudioSystem.LEGACY_REMOTE_SUBMIX_ADDRESS);
                                }
                            }
                            item.append(Integer.toHexString(rec.states));
                        }
                    } else if (this.oldLevel != rec.batteryLevel) {
                        this.oldLevel = rec.batteryLevel;
                        item.append(",Bl=");
                        item.append(rec.batteryLevel);
                    }
                    int i = this.oldStatus;
                    byte b = rec.batteryStatus;
                    String str2 = FullBackup.FILES_TREE_TOKEN;
                    String str3 = XmlTags.ATTR_DESCRIPTION;
                    if (i != b) {
                        this.oldStatus = rec.batteryStatus;
                        item.append(checkin ? ",Bs=" : " status=");
                        int i2 = this.oldStatus;
                        switch (i2) {
                            case 1:
                                item.append(checkin ? "?" : "unknown");
                                break;
                            case 2:
                                item.append(checkin ? "c" : "charging");
                                break;
                            case 3:
                                item.append(checkin ? XmlTags.ATTR_DESCRIPTION : "discharging");
                                break;
                            case 4:
                                item.append(checkin ? "n" : "not-charging");
                                break;
                            case 5:
                                item.append(checkin ? FullBackup.FILES_TREE_TOKEN : RcsContactPresenceTuple.ServiceCapabilities.DUPLEX_MODE_FULL);
                                break;
                            default:
                                item.append(i2);
                                break;
                        }
                    }
                    if (this.oldHealth != rec.batteryHealth) {
                        this.oldHealth = rec.batteryHealth;
                        item.append(checkin ? ",Bh=" : " health=");
                        int i3 = this.oldHealth;
                        switch (i3) {
                            case 1:
                                item.append(checkin ? "?" : "unknown");
                                break;
                            case 2:
                                item.append(checkin ? "g" : "good");
                                break;
                            case 3:
                                item.append(checkin ? BatteryStats.HISTORY_DATA : "overheat");
                                break;
                            case 4:
                                if (!checkin) {
                                    str3 = "dead";
                                }
                                item.append(str3);
                                break;
                            case 5:
                                item.append(checkin ? "v" : "over-voltage");
                                break;
                            case 6:
                                if (!checkin) {
                                    str2 = "failure";
                                }
                                item.append(str2);
                                break;
                            case 7:
                                item.append(checkin ? "c" : "cold");
                                break;
                            default:
                                item.append(i3);
                                break;
                        }
                    }
                    if (this.oldPlug != rec.batteryPlugType) {
                        this.oldPlug = rec.batteryPlugType;
                        item.append(checkin ? ",Bp=" : " plug=");
                        int i4 = this.oldPlug;
                        switch (i4) {
                            case 0:
                                item.append(checkin ? "n" : "none");
                                break;
                            case 1:
                                item.append(checkin ? FullBackup.APK_TREE_TOKEN : "ac");
                                break;
                            case 2:
                                item.append(checkin ? XmlTags.ATTR_UID : "usb");
                                break;
                            case 3:
                            default:
                                item.append(i4);
                                break;
                            case 4:
                                item.append(checkin ? "w" : AudioDeviceDescription.CONNECTION_WIRELESS);
                                break;
                        }
                    }
                    if (this.oldTemp != rec.batteryTemperature) {
                        this.oldTemp = rec.batteryTemperature;
                        item.append(checkin ? ",Bt=" : " temp=");
                        item.append(this.oldTemp);
                    }
                    if (this.oldVolt != rec.batteryVoltage) {
                        this.oldVolt = rec.batteryVoltage;
                        item.append(checkin ? ",Bv=" : " volt=");
                        item.append(this.oldVolt);
                    }
                    int chargeMAh = rec.batteryChargeUah / 1000;
                    if (this.oldChargeMAh != chargeMAh) {
                        this.oldChargeMAh = chargeMAh;
                        item.append(checkin ? ",Bcc=" : " charge=");
                        item.append(this.oldChargeMAh);
                    }
                    if (this.oldModemRailChargeMah != rec.modemRailChargeMah) {
                        this.oldModemRailChargeMah = rec.modemRailChargeMah;
                        item.append(checkin ? ",Mrc=" : " modemRailChargemAh=");
                        item.append(new DecimalFormat("#.##").format(this.oldModemRailChargeMah));
                    }
                    if (this.oldWifiRailChargeMah != rec.wifiRailChargeMah) {
                        this.oldWifiRailChargeMah = rec.wifiRailChargeMah;
                        item.append(checkin ? ",Wrc=" : " wifiRailChargemAh=");
                        item.append(new DecimalFormat("#.##").format(this.oldWifiRailChargeMah));
                    }
                    BatteryStats.printBitDescriptions(item, this.oldState, rec.states, rec.wakelockTag, BatteryStats.HISTORY_STATE_DESCRIPTIONS, !checkin);
                    BatteryStats.printBitDescriptions(item, this.oldState2, rec.states2, null, BatteryStats.HISTORY_STATE2_DESCRIPTIONS, !checkin);
                    if (rec.wakeReasonTag != null) {
                        if (checkin) {
                            item.append(",wr=");
                            item.append(rec.wakeReasonTag.poolIdx);
                        } else {
                            item.append(" wake_reason=");
                            item.append(rec.wakeReasonTag.uid);
                            item.append(":\"");
                            item.append(rec.wakeReasonTag.string);
                            item.append("\"");
                        }
                    }
                    if (rec.eventCode != 0) {
                        item.append(checkin ? "," : " ");
                        if ((rec.eventCode & 32768) != 0) {
                            item.append("+");
                        } else if ((rec.eventCode & 16384) != 0) {
                            item.append(NativeLibraryHelper.CLEAR_ABI_OVERRIDE);
                        }
                        String[] eventNames = checkin ? BatteryStats.HISTORY_EVENT_CHECKIN_NAMES : BatteryStats.HISTORY_EVENT_NAMES;
                        int idx = rec.eventCode & HistoryItem.EVENT_TYPE_MASK;
                        if (idx >= 0 && idx < eventNames.length) {
                            item.append(eventNames[idx]);
                        } else {
                            item.append(checkin ? "Ev" : "event");
                            item.append(idx);
                        }
                        item.append("=");
                        if (checkin) {
                            item.append(rec.eventTag.poolIdx);
                        } else {
                            item.append(BatteryStats.HISTORY_EVENT_INT_FORMATTERS[idx].applyAsString(rec.eventTag.uid));
                            item.append(":\"");
                            item.append(rec.eventTag.string);
                            item.append("\"");
                        }
                    }
                    item.append("\n");
                    if (rec.stepDetails != null) {
                        if (!checkin) {
                            item.append("                 Details: cpu=");
                            item.append(rec.stepDetails.userTime);
                            item.append("u+");
                            item.append(rec.stepDetails.systemTime);
                            item.append(XmlTags.TAG_SESSION);
                            if (rec.stepDetails.appCpuUid1 >= 0) {
                                item.append(" (");
                                printStepCpuUidDetails(item, rec.stepDetails.appCpuUid1, rec.stepDetails.appCpuUTime1, rec.stepDetails.appCpuSTime1);
                                if (rec.stepDetails.appCpuUid2 >= 0) {
                                    item.append(", ");
                                    printStepCpuUidDetails(item, rec.stepDetails.appCpuUid2, rec.stepDetails.appCpuUTime2, rec.stepDetails.appCpuSTime2);
                                }
                                if (rec.stepDetails.appCpuUid3 >= 0) {
                                    item.append(", ");
                                    printStepCpuUidDetails(item, rec.stepDetails.appCpuUid3, rec.stepDetails.appCpuUTime3, rec.stepDetails.appCpuSTime3);
                                }
                                item.append(')');
                            }
                            item.append("\n");
                            item.append("                          /proc/stat=");
                            item.append(rec.stepDetails.statUserTime);
                            item.append(" usr, ");
                            item.append(rec.stepDetails.statSystemTime);
                            item.append(" sys, ");
                            item.append(rec.stepDetails.statIOWaitTime);
                            item.append(" io, ");
                            item.append(rec.stepDetails.statIrqTime);
                            item.append(" irq, ");
                            item.append(rec.stepDetails.statSoftIrqTime);
                            item.append(" sirq, ");
                            item.append(rec.stepDetails.statIdlTime);
                            item.append(" idle");
                            int totalRun = rec.stepDetails.statUserTime + rec.stepDetails.statSystemTime + rec.stepDetails.statIOWaitTime + rec.stepDetails.statIrqTime + rec.stepDetails.statSoftIrqTime;
                            int total = rec.stepDetails.statIdlTime + totalRun;
                            if (total > 0) {
                                item.append(" (");
                                float perc = (totalRun / total) * 100.0f;
                                item.append(String.format("%.1f%%", Float.valueOf(perc)));
                                item.append(" of ");
                                StringBuilder sb = new StringBuilder(64);
                                BatteryStats.formatTimeMsNoSpace(sb, total * 10);
                                item.append((CharSequence) sb);
                                item.append(NavigationBarInflaterView.KEY_CODE_END);
                            }
                            item.append(", SubsystemPowerState ");
                            item.append(rec.stepDetails.statSubsystemPowerState);
                            item.append("\n");
                        } else {
                            item.append(9);
                            item.append(',');
                            item.append(BatteryStats.HISTORY_DATA);
                            item.append(",0,Dcpu=");
                            item.append(rec.stepDetails.userTime);
                            item.append(":");
                            item.append(rec.stepDetails.systemTime);
                            if (rec.stepDetails.appCpuUid1 >= 0) {
                                printStepCpuUidCheckinDetails(item, rec.stepDetails.appCpuUid1, rec.stepDetails.appCpuUTime1, rec.stepDetails.appCpuSTime1);
                                if (rec.stepDetails.appCpuUid2 >= 0) {
                                    printStepCpuUidCheckinDetails(item, rec.stepDetails.appCpuUid2, rec.stepDetails.appCpuUTime2, rec.stepDetails.appCpuSTime2);
                                }
                                if (rec.stepDetails.appCpuUid3 >= 0) {
                                    printStepCpuUidCheckinDetails(item, rec.stepDetails.appCpuUid3, rec.stepDetails.appCpuUTime3, rec.stepDetails.appCpuSTime3);
                                }
                            }
                            item.append("\n");
                            item.append(9);
                            item.append(',');
                            item.append(BatteryStats.HISTORY_DATA);
                            item.append(",0,Dpst=");
                            item.append(rec.stepDetails.statUserTime);
                            item.append(',');
                            item.append(rec.stepDetails.statSystemTime);
                            item.append(',');
                            item.append(rec.stepDetails.statIOWaitTime);
                            item.append(',');
                            item.append(rec.stepDetails.statIrqTime);
                            item.append(',');
                            item.append(rec.stepDetails.statSoftIrqTime);
                            item.append(',');
                            item.append(rec.stepDetails.statIdlTime);
                            item.append(',');
                            if (rec.stepDetails.statSubsystemPowerState != null) {
                                item.append(rec.stepDetails.statSubsystemPowerState);
                            }
                            item.append("\n");
                        }
                    }
                    this.oldState = rec.states;
                    this.oldState2 = rec.states2;
                    if ((rec.states2 & 524288) != 0) {
                        rec.states2 &= -524289;
                    }
                }
                if (checkin) {
                    item.append(str);
                }
                if (rec.cmd == 7) {
                    item.append("RESET:");
                    reset();
                }
                item.append("TIME:");
                if (checkin) {
                    item.append(rec.currentTime);
                    item.append("\n");
                } else {
                    item.append(" ");
                    item.append(DateFormat.format("yyyy-MM-dd-HH-mm-ss", rec.currentTime).toString());
                    item.append("\n");
                }
            }
            return item.toString();
        }

        private void printStepCpuUidDetails(StringBuilder sb, int uid, int utime, int stime) {
            UserHandle.formatUid(sb, uid);
            sb.append("=");
            sb.append(utime);
            sb.append("u+");
            sb.append(stime);
            sb.append(XmlTags.TAG_SESSION);
        }

        private void printStepCpuUidCheckinDetails(StringBuilder sb, int uid, int utime, int stime) {
            sb.append('/');
            sb.append(uid);
            sb.append(":");
            sb.append(utime);
            sb.append(":");
            sb.append(stime);
        }
    }

    private void printSizeValue(PrintWriter pw, long size) {
        float result = (float) size;
        String suffix = "";
        if (result >= 10240.0f) {
            suffix = "KB";
            result /= 1024.0f;
        }
        if (result >= 10240.0f) {
            suffix = "MB";
            result /= 1024.0f;
        }
        if (result >= 10240.0f) {
            suffix = "GB";
            result /= 1024.0f;
        }
        if (result >= 10240.0f) {
            suffix = "TB";
            result /= 1024.0f;
        }
        if (result >= 10240.0f) {
            suffix = "PB";
            result /= 1024.0f;
        }
        pw.print((int) result);
        pw.print(suffix);
    }

    private static boolean dumpTimeEstimate(PrintWriter pw, String label1, String label2, String label3, long estimatedTime) {
        if (estimatedTime < 0) {
            return false;
        }
        pw.print(label1);
        pw.print(label2);
        pw.print(label3);
        StringBuilder sb = new StringBuilder(64);
        formatTimeMs(sb, estimatedTime);
        pw.print(sb);
        pw.println();
        return true;
    }

    private static boolean dumpDurationSteps(PrintWriter pw, String prefix, String header, LevelStepTracker steps, boolean checkin) {
        int count;
        int count2;
        String str = header;
        LevelStepTracker levelStepTracker = steps;
        char c = 0;
        if (levelStepTracker != null && (count = levelStepTracker.mNumStepDurations) > 0) {
            if (!checkin) {
                pw.println(str);
            }
            String[] lineArgs = new String[5];
            int i = 0;
            while (i < count) {
                long duration = levelStepTracker.getDurationAt(i);
                int level = levelStepTracker.getLevelAt(i);
                long initMode = levelStepTracker.getInitModeAt(i);
                long modMode = levelStepTracker.getModModeAt(i);
                if (checkin) {
                    lineArgs[c] = Long.toString(duration);
                    lineArgs[1] = Integer.toString(level);
                    if ((modMode & 3) == 0) {
                        count2 = count;
                        switch (((int) (initMode & 3)) + 1) {
                            case 1:
                                lineArgs[2] = "s-";
                                break;
                            case 2:
                                lineArgs[2] = "s+";
                                break;
                            case 3:
                                lineArgs[2] = "sd";
                                break;
                            case 4:
                                lineArgs[2] = "sds";
                                break;
                            default:
                                lineArgs[2] = "?";
                                break;
                        }
                    } else {
                        count2 = count;
                        lineArgs[2] = "";
                    }
                    if ((modMode & 4) == 0) {
                        lineArgs[3] = (initMode & 4) != 0 ? "p+" : "p-";
                    } else {
                        lineArgs[3] = "";
                    }
                    if ((modMode & 8) == 0) {
                        lineArgs[4] = (8 & initMode) != 0 ? "i+" : "i-";
                    } else {
                        lineArgs[4] = "";
                    }
                    dumpLine(pw, 0, "i", str, lineArgs);
                } else {
                    count2 = count;
                    pw.print(prefix);
                    pw.print("#");
                    pw.print(i);
                    pw.print(": ");
                    TimeUtils.formatDuration(duration, pw);
                    pw.print(" to ");
                    pw.print(level);
                    boolean haveModes = false;
                    if ((modMode & 3) == 0) {
                        pw.print(" (");
                        switch (((int) (initMode & 3)) + 1) {
                            case 1:
                                pw.print("screen-off");
                                break;
                            case 2:
                                pw.print("screen-on");
                                break;
                            case 3:
                                pw.print("screen-doze");
                                break;
                            case 4:
                                pw.print("screen-doze-suspend");
                                break;
                            default:
                                pw.print("screen-?");
                                break;
                        }
                        haveModes = true;
                    }
                    if ((modMode & 4) == 0) {
                        pw.print(haveModes ? ", " : " (");
                        pw.print((initMode & 4) != 0 ? "power-save-on" : "power-save-off");
                        haveModes = true;
                    }
                    if ((modMode & 8) == 0) {
                        pw.print(haveModes ? ", " : " (");
                        pw.print((initMode & 8) != 0 ? "device-idle-on" : "device-idle-off");
                        haveModes = true;
                    }
                    if (haveModes) {
                        pw.print(NavigationBarInflaterView.KEY_CODE_END);
                    }
                    pw.println();
                }
                i++;
                str = header;
                levelStepTracker = steps;
                count = count2;
                c = 0;
            }
            return true;
        }
        return false;
    }

    private static void dumpDurationSteps(ProtoOutputStream proto, long fieldId, LevelStepTracker steps) {
        if (steps == null) {
            return;
        }
        int count = steps.mNumStepDurations;
        for (int i = 0; i < count; i++) {
            long token = proto.start(fieldId);
            proto.write(1112396529665L, steps.getDurationAt(i));
            proto.write(1120986464258L, steps.getLevelAt(i));
            long initMode = steps.getInitModeAt(i);
            long modMode = steps.getModModeAt(i);
            int ds = 0;
            if ((modMode & 3) == 0) {
                switch (((int) (3 & initMode)) + 1) {
                    case 1:
                        ds = 2;
                        break;
                    case 2:
                        ds = 1;
                        break;
                    case 3:
                        ds = 3;
                        break;
                    case 4:
                        ds = 4;
                        break;
                    default:
                        ds = 5;
                        break;
                }
            }
            proto.write(1159641169923L, ds);
            int psm = 0;
            if ((modMode & 4) == 0) {
                psm = (4 & initMode) == 0 ? 2 : 1;
            }
            proto.write(1159641169924L, psm);
            int im = 0;
            if ((modMode & 8) == 0) {
                im = (8 & initMode) == 0 ? 3 : 2;
            }
            proto.write(1159641169925L, im);
            proto.end(token);
        }
    }

    private void dumpHistoryLocked(PrintWriter pw, int flags, long histStart, boolean checkin) {
        long baseTime;
        boolean printed;
        boolean z;
        HistoryPrinter hprinter = new HistoryPrinter();
        HistoryItem rec = new HistoryItem();
        long lastTime = -1;
        long baseTime2 = -1;
        boolean printed2 = false;
        HistoryEventTracker tracker = null;
        while (getNextHistoryLocked(rec)) {
            long lastTime2 = rec.time;
            if (baseTime2 >= 0) {
                baseTime = baseTime2;
            } else {
                baseTime = lastTime2;
            }
            if (rec.time < histStart) {
                lastTime = lastTime2;
                baseTime2 = baseTime;
            } else {
                if (histStart >= 0 && !printed2) {
                    if (rec.cmd == 5 || rec.cmd == 7 || rec.cmd == 4 || rec.cmd == 8) {
                        printed = true;
                        z = false;
                        hprinter.printNextItem(pw, rec, baseTime, checkin, (flags & 32) != 0);
                        rec.cmd = (byte) 0;
                    } else if (rec.currentTime == 0) {
                        printed = printed2;
                        z = false;
                    } else {
                        printed = true;
                        byte cmd = rec.cmd;
                        rec.cmd = (byte) 5;
                        hprinter.printNextItem(pw, rec, baseTime, checkin, (flags & 32) != 0);
                        rec.cmd = cmd;
                        z = false;
                    }
                    if (tracker != null) {
                        if (rec.cmd != 0) {
                            hprinter.printNextItem(pw, rec, baseTime, checkin, (flags & 32) != 0 ? true : z ? 1 : 0);
                            rec.cmd = z ? (byte) 1 : (byte) 0;
                        }
                        int oldEventCode = rec.eventCode;
                        HistoryTag oldEventTag = rec.eventTag;
                        rec.eventTag = new HistoryTag();
                        int i = 0;
                        while (i < 22) {
                            HashMap<String, SparseIntArray> active = tracker.getStateForEvent(i);
                            if (active != null) {
                                for (Map.Entry<String, SparseIntArray> ent : active.entrySet()) {
                                    SparseIntArray uids = ent.getValue();
                                    int j = 0;
                                    while (j < uids.size()) {
                                        rec.eventCode = i;
                                        rec.eventTag.string = ent.getKey();
                                        rec.eventTag.uid = uids.keyAt(j);
                                        rec.eventTag.poolIdx = uids.valueAt(j);
                                        hprinter.printNextItem(pw, rec, baseTime, checkin, (flags & 32) != 0 ? true : z);
                                        rec.wakeReasonTag = null;
                                        rec.wakelockTag = null;
                                        j++;
                                        oldEventTag = oldEventTag;
                                        uids = uids;
                                        i = i;
                                        z = false;
                                    }
                                    z = false;
                                }
                            }
                            i++;
                            oldEventTag = oldEventTag;
                            z = false;
                        }
                        rec.eventCode = oldEventCode;
                        rec.eventTag = oldEventTag;
                        tracker = null;
                    }
                } else {
                    printed = printed2;
                }
                hprinter.printNextItem(pw, rec, baseTime, checkin, (flags & 32) != 0);
                printed2 = printed;
                lastTime = lastTime2;
                baseTime2 = baseTime;
            }
        }
        if (histStart >= 0) {
            commitCurrentHistoryBatchLocked();
            pw.print(checkin ? "NEXT: " : "  NEXT: ");
            pw.println(1 + lastTime);
        }
    }

    private void dumpDailyLevelStepSummary(PrintWriter pw, String prefix, String label, LevelStepTracker steps, StringBuilder tmpSb, int[] tmpOutInt) {
        int[] iArr;
        if (steps == null) {
            return;
        }
        long timeRemaining = steps.computeTimeEstimate(0L, 0L, tmpOutInt);
        if (timeRemaining >= 0) {
            pw.print(prefix);
            pw.print(label);
            pw.print(" total time: ");
            tmpSb.setLength(0);
            formatTimeMs(tmpSb, timeRemaining);
            pw.print(tmpSb);
            pw.print(" (from ");
            pw.print(tmpOutInt[0]);
            pw.println(" steps)");
        }
        int i = 0;
        while (true) {
            if (i < STEP_LEVEL_MODES_OF_INTEREST.length) {
                int i2 = i;
                long estimatedTime = steps.computeTimeEstimate(iArr[i], STEP_LEVEL_MODE_VALUES[i], tmpOutInt);
                if (estimatedTime > 0) {
                    pw.print(prefix);
                    pw.print(label);
                    pw.print(" ");
                    pw.print(STEP_LEVEL_MODE_LABELS[i2]);
                    pw.print(" time: ");
                    tmpSb.setLength(0);
                    formatTimeMs(tmpSb, estimatedTime);
                    pw.print(tmpSb);
                    pw.print(" (from ");
                    pw.print(tmpOutInt[0]);
                    pw.println(" steps)");
                }
                i = i2 + 1;
            } else {
                return;
            }
        }
    }

    private void dumpDailyPackageChanges(PrintWriter pw, String prefix, ArrayList<PackageChange> changes) {
        if (changes == null) {
            return;
        }
        pw.print(prefix);
        pw.println("Package changes:");
        for (int i = 0; i < changes.size(); i++) {
            PackageChange pc = changes.get(i);
            if (pc.mUpdate) {
                pw.print(prefix);
                pw.print("  Update ");
                pw.print(pc.mPackageName);
                pw.print(" vers=");
                pw.println(pc.mVersionCode);
            } else {
                pw.print(prefix);
                pw.print("  Uninstall ");
                pw.println(pc.mPackageName);
            }
        }
    }

    public void dumpLocked(Context context, PrintWriter pw, int flags, int reqUid, long histStart) {
        boolean z;
        int[] iArr;
        boolean z2;
        String str;
        ArrayList<PackageChange> pkgc;
        LevelStepTracker csteps;
        LevelStepTracker dsteps;
        int[] outInt;
        boolean z3;
        CharSequence charSequence;
        boolean z4;
        CharSequence charSequence2;
        DailyItem dit;
        LevelStepTracker dsteps2;
        boolean z5;
        String str2;
        prepareForDumpLocked();
        boolean filtering = (flags & 14) != 0;
        if ((flags & 8) != 0 || !filtering) {
            long historyTotalSize = getHistoryTotalSize();
            long historyUsedSize = getHistoryUsedSize();
            if (startIteratingHistoryLocked()) {
                try {
                    pw.print("Battery History (");
                    pw.print((100 * historyUsedSize) / historyTotalSize);
                    pw.print("% used, ");
                    printSizeValue(pw, historyUsedSize);
                    pw.print(" used of ");
                    printSizeValue(pw, historyTotalSize);
                    pw.print(", ");
                    pw.print(getHistoryStringPoolSize());
                    pw.print(" strings using ");
                    printSizeValue(pw, getHistoryStringPoolBytes());
                    pw.println("):");
                    dumpHistoryLocked(pw, flags, histStart, false);
                    pw.println();
                } finally {
                    finishIteratingHistoryLocked();
                }
            }
        }
        if (filtering && (flags & 6) == 0) {
            return;
        }
        if (!filtering) {
            SparseArray<? extends Uid> uidStats = getUidStats();
            int NU = uidStats.size();
            boolean didPid = false;
            long nowRealtime = SystemClock.elapsedRealtime();
            for (int i = 0; i < NU; i++) {
                Uid uid = uidStats.valueAt(i);
                SparseArray<? extends Uid.Pid> pids = uid.getPidStats();
                if (pids != null) {
                    for (int j = 0; j < pids.size(); j++) {
                        Uid.Pid pid = pids.valueAt(j);
                        if (!didPid) {
                            pw.println("Per-PID Stats:");
                            didPid = true;
                        }
                        long time = pid.mWakeSumMs + (pid.mWakeNesting > 0 ? nowRealtime - pid.mWakeStartMs : 0L);
                        pw.print("  PID ");
                        pw.print(pids.keyAt(j));
                        pw.print(" wake time: ");
                        TimeUtils.formatDuration(time, pw);
                        pw.println("");
                    }
                }
            }
            if (didPid) {
                pw.println();
            }
        }
        if (filtering && (flags & 2) == 0) {
            z = false;
        } else {
            if (dumpDurationSteps(pw, "  ", "Discharge step durations:", getDischargeLevelStepTracker(), false)) {
                long timeRemaining = computeBatteryTimeRemaining(SystemClock.elapsedRealtime() * 1000);
                if (timeRemaining >= 0) {
                    pw.print("  Estimated discharge time remaining: ");
                    TimeUtils.formatDuration(timeRemaining / 1000, pw);
                    pw.println();
                }
                LevelStepTracker steps = getDischargeLevelStepTracker();
                int i2 = 0;
                while (true) {
                    if (i2 >= STEP_LEVEL_MODES_OF_INTEREST.length) {
                        break;
                    }
                    dumpTimeEstimate(pw, "  Estimated ", STEP_LEVEL_MODE_LABELS[i2], " time: ", steps.computeTimeEstimate(iArr[i2], STEP_LEVEL_MODE_VALUES[i2], null));
                    i2++;
                }
                pw.println();
            }
            z = false;
            if (dumpDurationSteps(pw, "  ", "Charge step durations:", getChargeLevelStepTracker(), false)) {
                long timeRemaining2 = computeChargeTimeRemaining(SystemClock.elapsedRealtime() * 1000);
                if (timeRemaining2 >= 0) {
                    pw.print("  Estimated charge time remaining: ");
                    TimeUtils.formatDuration(timeRemaining2 / 1000, pw);
                    pw.println();
                }
                pw.println();
            }
        }
        if (filtering && (flags & 4) == 0) {
            z4 = z;
            z2 = true;
        } else {
            pw.println("Daily stats:");
            pw.print("  Current start time: ");
            pw.println(DateFormat.format("yyyy-MM-dd-HH-mm-ss", getCurrentDailyStartTime()).toString());
            pw.print("  Next min deadline: ");
            pw.println(DateFormat.format("yyyy-MM-dd-HH-mm-ss", getNextMinDailyDeadline()).toString());
            pw.print("  Next max deadline: ");
            pw.println(DateFormat.format("yyyy-MM-dd-HH-mm-ss", getNextMaxDailyDeadline()).toString());
            StringBuilder sb = new StringBuilder(64);
            int[] outInt2 = new int[1];
            LevelStepTracker dsteps3 = getDailyDischargeLevelStepTracker();
            LevelStepTracker csteps2 = getDailyChargeLevelStepTracker();
            ArrayList<PackageChange> pkgc2 = getDailyPackageChanges();
            if (dsteps3.mNumStepDurations <= 0 && csteps2.mNumStepDurations <= 0 && pkgc2 == null) {
                z2 = true;
                str = "    ";
                dsteps = dsteps3;
                outInt = outInt2;
                z3 = z;
                charSequence = "yyyy-MM-dd-HH-mm-ss";
            } else {
                if ((flags & 4) != 0) {
                    z2 = true;
                    str = "    ";
                    pkgc = pkgc2;
                    csteps = csteps2;
                    dsteps = dsteps3;
                    outInt = outInt2;
                    z3 = z;
                    charSequence = "yyyy-MM-dd-HH-mm-ss";
                } else if (!filtering) {
                    z2 = true;
                    str = "    ";
                    pkgc = pkgc2;
                    csteps = csteps2;
                    dsteps = dsteps3;
                    outInt = outInt2;
                    z3 = z;
                    charSequence = "yyyy-MM-dd-HH-mm-ss";
                } else {
                    pw.println("  Current daily steps:");
                    str = "    ";
                    dumpDailyLevelStepSummary(pw, "    ", "Discharge", dsteps3, sb, outInt2);
                    dsteps = dsteps3;
                    outInt = outInt2;
                    z3 = z;
                    charSequence = "yyyy-MM-dd-HH-mm-ss";
                    z2 = true;
                    dumpDailyLevelStepSummary(pw, "    ", "Charge", csteps2, sb, outInt);
                }
                if (dumpDurationSteps(pw, str, "  Current daily discharge step durations:", dsteps, z3)) {
                    dumpDailyLevelStepSummary(pw, "      ", "Discharge", dsteps, sb, outInt);
                }
                if (dumpDurationSteps(pw, str, "  Current daily charge step durations:", csteps, z3)) {
                    dumpDailyLevelStepSummary(pw, "      ", "Charge", csteps, sb, outInt);
                }
                dumpDailyPackageChanges(pw, str, pkgc);
            }
            int curIndex = 0;
            while (true) {
                DailyItem dit2 = getDailyItemLocked(curIndex);
                if (dit2 == null) {
                    break;
                }
                int curIndex2 = curIndex + 1;
                int curIndex3 = flags & 4;
                if (curIndex3 != 0) {
                    pw.println();
                }
                pw.print("  Daily from ");
                CharSequence charSequence3 = charSequence;
                pw.print(DateFormat.format(charSequence3, dit2.mStartTime).toString());
                pw.print(" to ");
                pw.print(DateFormat.format(charSequence3, dit2.mEndTime).toString());
                pw.println(":");
                if ((flags & 4) != 0) {
                    charSequence2 = charSequence3;
                    dit = dit2;
                } else if (filtering) {
                    charSequence2 = charSequence3;
                    int[] iArr2 = outInt;
                    dumpDailyLevelStepSummary(pw, "    ", "Discharge", dit2.mDischargeSteps, sb, iArr2);
                    dumpDailyLevelStepSummary(pw, "    ", "Charge", dit2.mChargeSteps, sb, iArr2);
                    dsteps2 = dsteps;
                    z5 = false;
                    z3 = z5;
                    curIndex = curIndex2;
                    charSequence = charSequence2;
                    dsteps = dsteps2;
                } else {
                    charSequence2 = charSequence3;
                    dit = dit2;
                }
                if (!dumpDurationSteps(pw, "      ", "    Discharge step durations:", dit.mDischargeSteps, false)) {
                    dsteps2 = dsteps;
                    str2 = "      ";
                } else {
                    dsteps2 = dsteps;
                    str2 = "      ";
                    dumpDailyLevelStepSummary(pw, "        ", "Discharge", dit.mDischargeSteps, sb, outInt);
                }
                if (!dumpDurationSteps(pw, str2, "    Charge step durations:", dit.mChargeSteps, false)) {
                    z5 = false;
                } else {
                    z5 = false;
                    dumpDailyLevelStepSummary(pw, "        ", "Charge", dit.mChargeSteps, sb, outInt);
                }
                dumpDailyPackageChanges(pw, str, dit.mPackageChanges);
                z3 = z5;
                curIndex = curIndex2;
                charSequence = charSequence2;
                dsteps = dsteps2;
            }
            z4 = z3;
            pw.println();
        }
        if (!filtering || (flags & 2) != 0) {
            pw.println("Statistics since last charge:");
            pw.println("  System starts: " + getStartCount() + ", currently on battery: " + getIsOnBattery());
            dumpLocked(context, pw, "", 0, reqUid, (flags & 64) != 0 ? z2 : z4);
            pw.println();
        }
    }

    public void dumpCheckinLocked(Context context, PrintWriter pw, List<ApplicationInfo> apps, int flags, long histStart) {
        prepareForDumpLocked();
        boolean z = true;
        dumpLine(pw, 0, "i", VERSION_DATA, 35, Integer.valueOf(getParcelVersion()), getStartPlatformVersion(), getEndPlatformVersion());
        long historyBaseTime = getHistoryBaseTime() + SystemClock.elapsedRealtime();
        if ((flags & 24) != 0 && startIteratingHistoryLocked()) {
            for (int i = 0; i < getHistoryStringPoolSize(); i++) {
                try {
                    pw.print(9);
                    pw.print(',');
                    pw.print(HISTORY_STRING_POOL);
                    pw.print(',');
                    pw.print(i);
                    pw.print(",");
                    pw.print(getHistoryTagPoolUid(i));
                    pw.print(",\"");
                    String str = getHistoryTagPoolString(i);
                    if (str != null) {
                        pw.print(str.replace("\\", "\\\\").replace("\"", "\\\""));
                    }
                    pw.print("\"");
                    pw.println();
                } finally {
                    finishIteratingHistoryLocked();
                }
            }
            dumpHistoryLocked(pw, flags, histStart, true);
        }
        if ((flags & 8) != 0) {
            return;
        }
        if (apps != null) {
            SparseArray<Pair<ArrayList<String>, MutableBoolean>> uids = new SparseArray<>();
            for (int i2 = 0; i2 < apps.size(); i2++) {
                ApplicationInfo ai = apps.get(i2);
                Pair<ArrayList<String>, MutableBoolean> pkgs = uids.get(UserHandle.getAppId(ai.uid));
                if (pkgs == null) {
                    pkgs = new Pair<>(new ArrayList(), new MutableBoolean(false));
                    uids.put(UserHandle.getAppId(ai.uid), pkgs);
                }
                ((ArrayList) pkgs.first).add(ai.packageName);
            }
            SparseArray<? extends Uid> uidStats = getUidStats();
            int NU = uidStats.size();
            String[] lineArgs = new String[2];
            int i3 = 0;
            while (i3 < NU) {
                int uid = UserHandle.getAppId(uidStats.keyAt(i3));
                Pair<ArrayList<String>, MutableBoolean> pkgs2 = uids.get(uid);
                if (pkgs2 != null && !((MutableBoolean) pkgs2.second).value) {
                    ((MutableBoolean) pkgs2.second).value = z;
                    int j = 0;
                    while (j < ((ArrayList) pkgs2.first).size()) {
                        lineArgs[0] = Integer.toString(uid);
                        lineArgs[1] = (String) ((ArrayList) pkgs2.first).get(j);
                        dumpLine(pw, 0, "i", "uid", lineArgs);
                        j++;
                        uids = uids;
                    }
                }
                i3++;
                uids = uids;
                z = true;
            }
        }
        if ((flags & 4) == 0) {
            dumpDurationSteps(pw, "", DISCHARGE_STEP_DATA, getDischargeLevelStepTracker(), true);
            String[] lineArgs2 = new String[1];
            long timeRemaining = computeBatteryTimeRemaining(SystemClock.elapsedRealtime() * 1000);
            if (timeRemaining >= 0) {
                lineArgs2[0] = Long.toString(timeRemaining);
                dumpLine(pw, 0, "i", DISCHARGE_TIME_REMAIN_DATA, lineArgs2);
            }
            dumpDurationSteps(pw, "", CHARGE_STEP_DATA, getChargeLevelStepTracker(), true);
            long timeRemaining2 = computeChargeTimeRemaining(1000 * SystemClock.elapsedRealtime());
            if (timeRemaining2 >= 0) {
                lineArgs2[0] = Long.toString(timeRemaining2);
                dumpLine(pw, 0, "i", CHARGE_TIME_REMAIN_DATA, lineArgs2);
            }
            dumpCheckinLocked(context, pw, 0, -1, (flags & 64) != 0);
        }
    }

    public void dumpProtoLocked(Context context, FileDescriptor fd, List<ApplicationInfo> apps, int flags, long histStart) {
        ProtoOutputStream proto = new ProtoOutputStream(fd);
        prepareForDumpLocked();
        if ((flags & 24) != 0) {
            dumpProtoHistoryLocked(proto, flags, histStart);
            proto.flush();
            return;
        }
        long bToken = proto.start(1146756268033L);
        proto.write(1120986464257L, 35);
        proto.write(1112396529666L, getParcelVersion());
        proto.write(1138166333443L, getStartPlatformVersion());
        proto.write(1138166333444L, getEndPlatformVersion());
        if ((flags & 4) == 0) {
            BatteryUsageStats stats = getBatteryUsageStats(context);
            ProportionalAttributionCalculator proportionalAttributionCalculator = new ProportionalAttributionCalculator(context, stats);
            dumpProtoAppsLocked(proto, stats, apps, proportionalAttributionCalculator);
            dumpProtoSystemLocked(proto, stats);
        }
        proto.end(bToken);
        proto.flush();
    }

    private void dumpProtoAppsLocked(ProtoOutputStream proto, BatteryUsageStats stats, List<ApplicationInfo> apps, ProportionalAttributionCalculator proportionalAttributionCalculator) {
        long rawRealtimeUs;
        SparseArray<ArrayList<String>> aidToPackages;
        long rawRealtimeMs;
        long j;
        long[] cpuFreqs;
        long rawRealtimeMs2;
        SparseArray<? extends Uid.Sensor> sensors;
        UidBatteryConsumer consumer;
        long[] timesInFreqMs;
        long[] timesInFreqScreenOffMs;
        int iu;
        ArrayList<String> pkgs;
        long[] cpuFreqs2;
        SparseArray<UidBatteryConsumer> uidToConsumer;
        ProtoOutputStream protoOutputStream;
        long uTkn;
        int uid;
        long rawRealtimeMs3;
        long batteryUptimeUs;
        SparseArray<? extends Uid> uidStats;
        Uid u;
        ArrayMap<String, ? extends Uid.Pkg> packageStats;
        ArrayList<String> pkgs2;
        Uid u2;
        ArrayMap<String, ? extends Uid.Pkg> packageStats2;
        ArrayList<String> pkgs3;
        ProtoOutputStream protoOutputStream2 = proto;
        int which = 0;
        long rawUptimeUs = SystemClock.uptimeMillis() * 1000;
        long rawRealtimeMs4 = SystemClock.elapsedRealtime();
        long rawRealtimeUs2 = rawRealtimeMs4 * 1000;
        long batteryUptimeUs2 = getBatteryUptime(rawUptimeUs);
        SparseArray<ArrayList<String>> aidToPackages2 = new SparseArray<>();
        if (apps == null) {
            rawRealtimeUs = rawRealtimeUs2;
        } else {
            int i = 0;
            while (i < apps.size()) {
                ApplicationInfo ai = apps.get(i);
                long rawRealtimeUs3 = rawRealtimeUs2;
                int aid = UserHandle.getAppId(ai.uid);
                ArrayList<String> pkgs4 = aidToPackages2.get(aid);
                if (pkgs4 == null) {
                    pkgs4 = new ArrayList<>();
                    aidToPackages2.put(aid, pkgs4);
                }
                pkgs4.add(ai.packageName);
                i++;
                rawRealtimeUs2 = rawRealtimeUs3;
            }
            rawRealtimeUs = rawRealtimeUs2;
        }
        SparseArray<UidBatteryConsumer> uidToConsumer2 = new SparseArray<>();
        List<UidBatteryConsumer> consumers = stats.getUidBatteryConsumers();
        int i2 = consumers.size() - 1;
        while (i2 >= 0) {
            UidBatteryConsumer bs = consumers.get(i2);
            uidToConsumer2.put(bs.getUid(), bs);
            i2--;
            consumers = consumers;
        }
        List<UidBatteryConsumer> consumers2 = consumers;
        SparseArray<? extends Uid> uidStats2 = getUidStats();
        int n = uidStats2.size();
        int iu2 = 0;
        while (iu2 < n) {
            int n2 = n;
            SparseArray<UidBatteryConsumer> uidToConsumer3 = uidToConsumer2;
            long uTkn2 = protoOutputStream2.start(2246267895813L);
            Uid u3 = uidStats2.valueAt(iu2);
            int which2 = which;
            int uid2 = uidStats2.keyAt(iu2);
            long rawUptimeUs2 = rawUptimeUs;
            protoOutputStream2.write(1120986464257L, uid2);
            ArrayList<String> pkgs5 = aidToPackages2.get(UserHandle.getAppId(uid2));
            if (pkgs5 == null) {
                pkgs5 = new ArrayList<>();
            }
            ArrayMap<String, ? extends Uid.Pkg> packageStats3 = u3.getPackageStats();
            int iu3 = iu2;
            int iu4 = packageStats3.size() - 1;
            while (true) {
                aidToPackages = aidToPackages2;
                if (iu4 < 0) {
                    break;
                }
                String pkg = packageStats3.keyAt(iu4);
                ArrayMap<String, ? extends Uid.Pkg.Serv> serviceStats = packageStats3.valueAt(iu4).getServiceStats();
                if (serviceStats.size() == 0) {
                    protoOutputStream = proto;
                    batteryUptimeUs = batteryUptimeUs2;
                    uTkn = uTkn2;
                    uidStats = uidStats2;
                    u = u3;
                    uid = uid2;
                    pkgs2 = pkgs5;
                    packageStats = packageStats3;
                    rawRealtimeMs3 = rawRealtimeMs4;
                } else {
                    protoOutputStream = proto;
                    uTkn = uTkn2;
                    uid = uid2;
                    ArrayMap<String, ? extends Uid.Pkg> packageStats4 = packageStats3;
                    rawRealtimeMs3 = rawRealtimeMs4;
                    long pToken = protoOutputStream.start(2246267895810L);
                    protoOutputStream.write(1138166333441L, pkg);
                    pkgs5.remove(pkg);
                    int isvc = serviceStats.size() - 1;
                    while (isvc >= 0) {
                        Uid.Pkg.Serv ss = serviceStats.valueAt(isvc);
                        String pkg2 = pkg;
                        SparseArray<? extends Uid> uidStats3 = uidStats2;
                        long startTimeMs = roundUsToMs(ss.getStartTime(batteryUptimeUs2, 0));
                        long batteryUptimeUs3 = batteryUptimeUs2;
                        int starts = ss.getStarts(0);
                        int launches = ss.getLaunches(0);
                        if (startTimeMs == 0 && starts == 0 && launches == 0) {
                            u2 = u3;
                            packageStats2 = packageStats4;
                            pkgs3 = pkgs5;
                        } else {
                            u2 = u3;
                            long sToken = protoOutputStream.start(2246267895810L);
                            packageStats2 = packageStats4;
                            pkgs3 = pkgs5;
                            protoOutputStream.write(1138166333441L, serviceStats.keyAt(isvc));
                            protoOutputStream.write(1112396529666L, startTimeMs);
                            protoOutputStream.write(1120986464259L, starts);
                            protoOutputStream.write(1120986464260L, launches);
                            protoOutputStream.end(sToken);
                        }
                        isvc--;
                        pkgs5 = pkgs3;
                        pkg = pkg2;
                        uidStats2 = uidStats3;
                        batteryUptimeUs2 = batteryUptimeUs3;
                        u3 = u2;
                        packageStats4 = packageStats2;
                    }
                    batteryUptimeUs = batteryUptimeUs2;
                    uidStats = uidStats2;
                    u = u3;
                    packageStats = packageStats4;
                    pkgs2 = pkgs5;
                    protoOutputStream.end(pToken);
                }
                iu4--;
                pkgs5 = pkgs2;
                aidToPackages2 = aidToPackages;
                uid2 = uid;
                uidStats2 = uidStats;
                uTkn2 = uTkn;
                rawRealtimeMs4 = rawRealtimeMs3;
                batteryUptimeUs2 = batteryUptimeUs;
                u3 = u;
                packageStats3 = packageStats;
            }
            long batteryUptimeUs4 = batteryUptimeUs2;
            long uTkn3 = uTkn2;
            SparseArray<? extends Uid> uidStats4 = uidStats2;
            Uid u4 = u3;
            int uid3 = uid2;
            ArrayList<String> pkgs6 = pkgs5;
            ArrayMap<String, ? extends Uid.Pkg> packageStats5 = packageStats3;
            long rawRealtimeMs5 = rawRealtimeMs4;
            Iterator<String> it = pkgs6.iterator();
            while (it.hasNext()) {
                String p = it.next();
                long pToken2 = proto.start(2246267895810L);
                proto.write(1138166333441L, p);
                proto.end(pToken2);
            }
            if (u4.getAggregatedPartialWakelockTimer() == null) {
                rawRealtimeMs = rawRealtimeMs5;
            } else {
                Timer timer = u4.getAggregatedPartialWakelockTimer();
                rawRealtimeMs = rawRealtimeMs5;
                long totTimeMs = timer.getTotalDurationMsLocked(rawRealtimeMs);
                Timer bgTimer = timer.getSubTimer();
                long bgTimeMs = bgTimer != null ? bgTimer.getTotalDurationMsLocked(rawRealtimeMs) : 0L;
                long awToken = proto.start(1146756268056L);
                proto.write(1112396529665L, totTimeMs);
                proto.write(1112396529666L, bgTimeMs);
                proto.end(awToken);
            }
            int iu5 = iu3;
            long rawRealtimeUs4 = rawRealtimeUs;
            List<UidBatteryConsumer> consumers3 = consumers2;
            SparseArray<UidBatteryConsumer> uidToConsumer4 = uidToConsumer3;
            dumpTimer(proto, 1146756268040L, u4.getAudioTurnedOnTimer(), rawRealtimeUs4, 0);
            dumpControllerActivityProto(proto, 1146756268035L, u4.getBluetoothControllerActivity(), 0);
            Timer bleTimer = u4.getBluetoothScanTimer();
            if (bleTimer != null) {
                long bmToken = proto.start(1146756268038L);
                dumpTimer(proto, 1146756268033L, bleTimer, rawRealtimeUs4, 0);
                dumpTimer(proto, 1146756268034L, u4.getBluetoothScanBackgroundTimer(), rawRealtimeUs4, 0);
                dumpTimer(proto, 1146756268035L, u4.getBluetoothUnoptimizedScanTimer(), rawRealtimeUs4, 0);
                dumpTimer(proto, 1146756268036L, u4.getBluetoothUnoptimizedScanBackgroundTimer(), rawRealtimeUs4, 0);
                j = 1120986464261L;
                proto.write(1120986464261L, u4.getBluetoothScanResultCounter() != null ? u4.getBluetoothScanResultCounter().getCountLocked(0) : 0);
                proto.write(1120986464262L, u4.getBluetoothScanResultBgCounter() != null ? u4.getBluetoothScanResultBgCounter().getCountLocked(0) : 0);
                proto.end(bmToken);
            } else {
                j = 1120986464261L;
            }
            dumpTimer(proto, 1146756268041L, u4.getCameraTurnedOnTimer(), rawRealtimeUs4, 0);
            long cpuToken = proto.start(1146756268039L);
            proto.write(1112396529665L, roundUsToMs(u4.getUserCpuTimeUs(0)));
            proto.write(1112396529666L, roundUsToMs(u4.getSystemCpuTimeUs(0)));
            long[] cpuFreqs3 = getCpuFreqs();
            if (cpuFreqs3 == null) {
                cpuFreqs = cpuFreqs3;
                rawRealtimeMs2 = rawRealtimeMs;
            } else {
                long[] cpuFreqTimeMs = u4.getCpuFreqTimes(0);
                if (cpuFreqTimeMs == null || cpuFreqTimeMs.length != cpuFreqs3.length) {
                    cpuFreqs = cpuFreqs3;
                    rawRealtimeMs2 = rawRealtimeMs;
                } else {
                    long[] screenOffCpuFreqTimeMs = u4.getScreenOffCpuFreqTimes(0);
                    if (screenOffCpuFreqTimeMs == null) {
                        screenOffCpuFreqTimeMs = new long[cpuFreqTimeMs.length];
                    }
                    int ic = 0;
                    while (ic < cpuFreqTimeMs.length) {
                        long cToken = proto.start(2246267895811L);
                        proto.write(1120986464257L, ic + 1);
                        proto.write(1112396529666L, cpuFreqTimeMs[ic]);
                        proto.write(1112396529667L, screenOffCpuFreqTimeMs[ic]);
                        proto.end(cToken);
                        ic++;
                        cpuFreqs3 = cpuFreqs3;
                        cpuFreqTimeMs = cpuFreqTimeMs;
                        rawRealtimeMs = rawRealtimeMs;
                    }
                    cpuFreqs = cpuFreqs3;
                    rawRealtimeMs2 = rawRealtimeMs;
                }
            }
            long[] timesInFreqMs2 = new long[getCpuFreqCount()];
            long[] timesInFreqScreenOffMs2 = new long[getCpuFreqCount()];
            int procState = 0;
            while (procState < 7) {
                if (!u4.getCpuFreqTimes(timesInFreqMs2, procState)) {
                    iu = iu5;
                    pkgs = pkgs6;
                    cpuFreqs2 = cpuFreqs;
                    uidToConsumer = uidToConsumer4;
                } else {
                    if (!u4.getScreenOffCpuFreqTimes(timesInFreqScreenOffMs2, procState)) {
                        Arrays.fill(timesInFreqScreenOffMs2, 0L);
                    }
                    long procToken = proto.start(2246267895812L);
                    proto.write(1159641169921L, procState);
                    int ic2 = 0;
                    while (ic2 < timesInFreqMs2.length) {
                        int iu6 = iu5;
                        long cToken2 = proto.start(2246267895810L);
                        proto.write(1120986464257L, ic2 + 1);
                        proto.write(1112396529666L, timesInFreqMs2[ic2]);
                        proto.write(1112396529667L, timesInFreqScreenOffMs2[ic2]);
                        proto.end(cToken2);
                        ic2++;
                        pkgs6 = pkgs6;
                        iu5 = iu6;
                        uidToConsumer4 = uidToConsumer4;
                        cpuFreqs = cpuFreqs;
                    }
                    iu = iu5;
                    pkgs = pkgs6;
                    cpuFreqs2 = cpuFreqs;
                    uidToConsumer = uidToConsumer4;
                    proto.end(procToken);
                }
                procState++;
                pkgs6 = pkgs;
                iu5 = iu;
                uidToConsumer4 = uidToConsumer;
                cpuFreqs = cpuFreqs2;
            }
            int iu7 = iu5;
            SparseArray<UidBatteryConsumer> uidToConsumer5 = uidToConsumer4;
            long j2 = 2246267895810L;
            proto.end(cpuToken);
            dumpTimer(proto, 1146756268042L, u4.getFlashlightTurnedOnTimer(), rawRealtimeUs4, 0);
            dumpTimer(proto, 1146756268043L, u4.getForegroundActivityTimer(), rawRealtimeUs4, 0);
            dumpTimer(proto, 1146756268044L, u4.getForegroundServiceTimer(), rawRealtimeUs4, 0);
            ArrayMap<String, SparseIntArray> completions = u4.getJobCompletionStats();
            int ic3 = 0;
            while (ic3 < completions.size()) {
                SparseIntArray types = completions.valueAt(ic3);
                if (types != null) {
                    long jcToken = proto.start(2246267895824L);
                    proto.write(1138166333441L, completions.keyAt(ic3));
                    int[] jobStopReasonCodes = JobParameters.getJobStopReasonCodes();
                    int length = jobStopReasonCodes.length;
                    int i3 = 0;
                    while (i3 < length) {
                        int r = jobStopReasonCodes[i3];
                        long[] timesInFreqMs3 = timesInFreqMs2;
                        long rToken = proto.start(j2);
                        proto.write(1159641169921L, r);
                        proto.write(1120986464258L, types.get(r, 0));
                        proto.end(rToken);
                        i3++;
                        timesInFreqMs2 = timesInFreqMs3;
                        timesInFreqScreenOffMs2 = timesInFreqScreenOffMs2;
                        types = types;
                        jcToken = jcToken;
                        j2 = 2246267895810L;
                    }
                    timesInFreqMs = timesInFreqMs2;
                    timesInFreqScreenOffMs = timesInFreqScreenOffMs2;
                    proto.end(jcToken);
                } else {
                    timesInFreqMs = timesInFreqMs2;
                    timesInFreqScreenOffMs = timesInFreqScreenOffMs2;
                }
                ic3++;
                timesInFreqMs2 = timesInFreqMs;
                timesInFreqScreenOffMs2 = timesInFreqScreenOffMs;
                j2 = 2246267895810L;
            }
            long[] timesInFreqMs4 = timesInFreqMs2;
            long j3 = 1120986464258L;
            ArrayMap<String, ? extends Timer> jobs = u4.getJobStats();
            int ij = jobs.size() - 1;
            while (ij >= 0) {
                Timer timer2 = jobs.valueAt(ij);
                Timer bgTimer2 = timer2.getSubTimer();
                long jToken = proto.start(2246267895823L);
                proto.write(1138166333441L, jobs.keyAt(ij));
                dumpTimer(proto, 1146756268034L, timer2, rawRealtimeUs4, 0);
                dumpTimer(proto, 1146756268035L, bgTimer2, rawRealtimeUs4, 0);
                proto.end(jToken);
                ij--;
                completions = completions;
                j3 = 1120986464258L;
            }
            dumpControllerActivityProto(proto, 1146756268036L, u4.getModemControllerActivity(), 0);
            long nToken = proto.start(1146756268049L);
            proto.write(1112396529665L, u4.getNetworkActivityBytes(0, 0));
            proto.write(1112396529666L, u4.getNetworkActivityBytes(1, 0));
            proto.write(1112396529667L, u4.getNetworkActivityBytes(2, 0));
            proto.write(1112396529668L, u4.getNetworkActivityBytes(3, 0));
            proto.write(1112396529669L, u4.getNetworkActivityBytes(4, 0));
            proto.write(1112396529670L, u4.getNetworkActivityBytes(5, 0));
            proto.write(1112396529671L, u4.getNetworkActivityPackets(0, 0));
            proto.write(1112396529672L, u4.getNetworkActivityPackets(1, 0));
            proto.write(1112396529673L, u4.getNetworkActivityPackets(2, 0));
            proto.write(1112396529674L, u4.getNetworkActivityPackets(3, 0));
            proto.write(1112396529675L, roundUsToMs(u4.getMobileRadioActiveTime(0)));
            proto.write(1120986464268L, u4.getMobileRadioActiveCount(0));
            proto.write(1120986464269L, u4.getMobileRadioApWakeupCount(0));
            proto.write(1120986464270L, u4.getWifiRadioApWakeupCount(0));
            proto.write(1112396529679L, u4.getNetworkActivityBytes(6, 0));
            proto.write(1112396529680L, u4.getNetworkActivityBytes(7, 0));
            proto.write(1112396529681L, u4.getNetworkActivityBytes(8, 0));
            proto.write(1112396529682L, u4.getNetworkActivityBytes(9, 0));
            proto.write(1112396529683L, u4.getNetworkActivityPackets(6, 0));
            proto.write(1112396529684L, u4.getNetworkActivityPackets(7, 0));
            proto.write(1112396529685L, u4.getNetworkActivityPackets(8, 0));
            proto.write(1112396529686L, u4.getNetworkActivityPackets(9, 0));
            proto.end(nToken);
            int uid4 = uid3;
            SparseArray<UidBatteryConsumer> uidToConsumer6 = uidToConsumer5;
            UidBatteryConsumer consumer2 = uidToConsumer6.get(uid4);
            if (consumer2 != null) {
                long bsToken = proto.start(1146756268050L);
                proto.write(1103806595073L, consumer2.getConsumedPower());
                proto.write(1133871366146L, proportionalAttributionCalculator.isSystemBatteryConsumer(consumer2));
                proto.write(1103806595075L, consumer2.getConsumedPower(0));
                proto.write(1103806595076L, proportionalAttributionCalculator.getProportionalPowerMah(consumer2));
                proto.end(bsToken);
            }
            ArrayMap<String, ? extends Uid.Proc> processStats = u4.getProcessStats();
            int ipr = processStats.size() - 1;
            while (ipr >= 0) {
                Uid.Proc ps = processStats.valueAt(ipr);
                long prToken = proto.start(2246267895827L);
                proto.write(1138166333441L, processStats.keyAt(ipr));
                proto.write(1112396529666L, ps.getUserTime(0));
                proto.write(1112396529667L, ps.getSystemTime(0));
                proto.write(1112396529668L, ps.getForegroundTime(0));
                proto.write(1120986464261L, ps.getStarts(0));
                proto.write(1120986464262L, ps.getNumAnrs(0));
                proto.write(1120986464263L, ps.getNumCrashes(0));
                proto.end(prToken);
                ipr--;
                processStats = processStats;
                uidToConsumer6 = uidToConsumer6;
                consumer2 = consumer2;
            }
            UidBatteryConsumer consumer3 = consumer2;
            SparseArray<UidBatteryConsumer> uidToConsumer7 = uidToConsumer6;
            SparseArray<? extends Uid.Sensor> sensors2 = u4.getSensorStats();
            int ise = 0;
            while (ise < sensors2.size()) {
                Uid.Sensor se = sensors2.valueAt(ise);
                Timer timer3 = se.getSensorTime();
                if (timer3 == null) {
                    consumer = consumer3;
                } else {
                    Timer bgTimer3 = se.getSensorBackgroundTime();
                    int sensorNumber = sensors2.keyAt(ise);
                    long seToken = proto.start(2246267895829L);
                    proto.write(1120986464257L, sensorNumber);
                    consumer = consumer3;
                    dumpTimer(proto, 1146756268034L, timer3, rawRealtimeUs4, 0);
                    dumpTimer(proto, 1146756268035L, bgTimer3, rawRealtimeUs4, 0);
                    proto.end(seToken);
                }
                ise++;
                consumer3 = consumer;
            }
            int ips = 0;
            while (ips < 7) {
                long rawRealtimeUs5 = rawRealtimeUs4;
                long durMs = roundUsToMs(u4.getProcessStateTime(ips, rawRealtimeUs5, 0));
                if (durMs == 0) {
                    sensors = sensors2;
                } else {
                    long stToken = proto.start(2246267895828L);
                    proto.write(1159641169921L, ips);
                    sensors = sensors2;
                    proto.write(1112396529666L, durMs);
                    proto.end(stToken);
                }
                ips++;
                rawRealtimeUs4 = rawRealtimeUs5;
                sensors2 = sensors;
            }
            long rawRealtimeUs6 = rawRealtimeUs4;
            long j4 = 1112396529666L;
            ArrayMap<String, ? extends Timer> syncs = u4.getSyncStats();
            int isy = syncs.size() - 1;
            while (isy >= 0) {
                Timer timer4 = syncs.valueAt(isy);
                Timer bgTimer4 = timer4.getSubTimer();
                long syToken = proto.start(2246267895830L);
                proto.write(1138166333441L, syncs.keyAt(isy));
                dumpTimer(proto, 1146756268034L, timer4, rawRealtimeUs6, 0);
                dumpTimer(proto, 1146756268035L, bgTimer4, rawRealtimeUs6, 0);
                proto.end(syToken);
                isy--;
                j4 = 1112396529666L;
                syncs = syncs;
                timesInFreqMs4 = timesInFreqMs4;
                uid4 = uid4;
            }
            if (u4.hasUserActivity()) {
                for (int i4 = 0; i4 < Uid.NUM_USER_ACTIVITY_TYPES; i4++) {
                    int val = u4.getUserActivityCount(i4, 0);
                    if (val != 0) {
                        long uaToken = proto.start(2246267895831L);
                        proto.write(1159641169921L, i4);
                        proto.write(1120986464258L, val);
                        proto.end(uaToken);
                    }
                }
            }
            dumpTimer(proto, 1146756268045L, u4.getVibratorOnTimer(), rawRealtimeUs6, 0);
            dumpTimer(proto, 1146756268046L, u4.getVideoTurnedOnTimer(), rawRealtimeUs6, 0);
            ArrayMap<String, ? extends Uid.Wakelock> wakelocks = u4.getWakelockStats();
            int iw = wakelocks.size() - 1;
            while (iw >= 0) {
                Uid.Wakelock wl = wakelocks.valueAt(iw);
                long wToken = proto.start(2246267895833L);
                proto.write(1138166333441L, wakelocks.keyAt(iw));
                int iw2 = iw;
                ArrayMap<String, ? extends Uid.Wakelock> wakelocks2 = wakelocks;
                dumpTimer(proto, 1146756268034L, wl.getWakeTime(1), rawRealtimeUs6, 0);
                Timer pTimer = wl.getWakeTime(0);
                if (pTimer != null) {
                    dumpTimer(proto, 1146756268035L, pTimer, rawRealtimeUs6, 0);
                    dumpTimer(proto, 1146756268036L, pTimer.getSubTimer(), rawRealtimeUs6, 0);
                }
                dumpTimer(proto, 1146756268037L, wl.getWakeTime(2), rawRealtimeUs6, 0);
                proto.end(wToken);
                iw = iw2 - 1;
                wakelocks = wakelocks2;
            }
            dumpTimer(proto, 1146756268060L, u4.getMulticastWakelockStats(), rawRealtimeUs6, 0);
            int i5 = 1;
            int ipkg = packageStats5.size() - 1;
            while (ipkg >= 0) {
                ArrayMap<String, ? extends Uid.Pkg> packageStats6 = packageStats5;
                ArrayMap<String, ? extends Counter> alarms = packageStats6.valueAt(ipkg).getWakeupAlarmStats();
                for (int iwa = alarms.size() - i5; iwa >= 0; iwa--) {
                    long waToken = proto.start(2246267895834L);
                    proto.write(1138166333441L, alarms.keyAt(iwa));
                    proto.write(1120986464258L, alarms.valueAt(iwa).getCountLocked(0));
                    proto.end(waToken);
                }
                ipkg--;
                packageStats5 = packageStats6;
                i5 = 1;
            }
            dumpControllerActivityProto(proto, 1146756268037L, u4.getWifiControllerActivity(), 0);
            long wToken2 = proto.start(1146756268059L);
            proto.write(1112396529665L, roundUsToMs(u4.getFullWifiLockTime(rawRealtimeUs6, 0)));
            dumpTimer(proto, 1146756268035L, u4.getWifiScanTimer(), rawRealtimeUs6, 0);
            proto.write(1112396529666L, roundUsToMs(u4.getWifiRunningTime(rawRealtimeUs6, 0)));
            dumpTimer(proto, 1146756268036L, u4.getWifiScanBackgroundTimer(), rawRealtimeUs6, 0);
            proto.end(wToken2);
            proto.end(uTkn3);
            iu2 = iu7 + 1;
            protoOutputStream2 = proto;
            uidStats2 = uidStats4;
            aidToPackages2 = aidToPackages;
            n = n2;
            which = which2;
            batteryUptimeUs2 = batteryUptimeUs4;
            uidToConsumer2 = uidToConsumer7;
            consumers2 = consumers3;
            rawRealtimeUs = rawRealtimeUs6;
            rawUptimeUs = rawUptimeUs2;
            rawRealtimeMs4 = rawRealtimeMs2;
        }
    }

    /* JADX WARN: Removed duplicated region for block: B:44:0x00e8 A[Catch: all -> 0x01ec, TryCatch #0 {all -> 0x01ec, blocks: (B:6:0x003b, B:8:0x0041, B:9:0x0067, B:10:0x0078, B:12:0x0080, B:16:0x008a, B:21:0x0099, B:23:0x009e, B:25:0x00a3, B:27:0x00a8, B:30:0x00af, B:32:0x00b5, B:36:0x00c3, B:44:0x00e8, B:46:0x00ec, B:50:0x00f4, B:51:0x00fe, B:54:0x0112, B:70:0x0194, B:57:0x011f, B:58:0x0127, B:60:0x012d, B:61:0x013e, B:63:0x0144, B:67:0x0169, B:71:0x019a, B:73:0x01a5, B:77:0x01ad, B:38:0x00d1, B:42:0x00da, B:81:0x01c6), top: B:87:0x003b }] */
    /*
        Code decompiled incorrectly, please refer to instructions dump.
    */
    private void dumpProtoHistoryLocked(ProtoOutputStream proto, int flags, long histStart) {
        long baseTime;
        boolean printed;
        boolean z;
        int i;
        HistoryTag oldEventTag;
        BatteryStats batteryStats = this;
        if (!startIteratingHistoryLocked()) {
            return;
        }
        proto.write(1120986464257L, 35);
        proto.write(1112396529666L, getParcelVersion());
        proto.write(1138166333443L, getStartPlatformVersion());
        proto.write(1138166333444L, getEndPlatformVersion());
        for (int i2 = 0; i2 < getHistoryStringPoolSize(); i2++) {
            try {
                long token = proto.start(2246267895813L);
                proto.write(1120986464257L, i2);
                proto.write(1120986464258L, batteryStats.getHistoryTagPoolUid(i2));
                proto.write(1138166333443L, batteryStats.getHistoryTagPoolString(i2));
                proto.end(token);
            } finally {
                finishIteratingHistoryLocked();
            }
        }
        HistoryPrinter hprinter = new HistoryPrinter();
        HistoryItem rec = new HistoryItem();
        long lastTime = -1;
        long baseTime2 = -1;
        boolean printed2 = false;
        HistoryEventTracker tracker = null;
        while (batteryStats.getNextHistoryLocked(rec)) {
            long lastTime2 = rec.time;
            if (baseTime2 >= 0) {
                baseTime = baseTime2;
            } else {
                baseTime = lastTime2;
            }
            if (rec.time < histStart) {
                batteryStats = this;
                lastTime = lastTime2;
                baseTime2 = baseTime;
            } else {
                if (histStart >= 0 && !printed2) {
                    if (rec.cmd != 5 && rec.cmd != 7 && rec.cmd != 4 && rec.cmd != 8) {
                        if (rec.currentTime == 0) {
                            printed = printed2;
                            z = false;
                        } else {
                            printed = true;
                            byte cmd = rec.cmd;
                            rec.cmd = (byte) 5;
                            hprinter.printNextItem(proto, rec, baseTime, (flags & 32) != 0);
                            rec.cmd = cmd;
                            z = false;
                        }
                        if (tracker != null) {
                            if (rec.cmd != 0) {
                                hprinter.printNextItem(proto, rec, baseTime, (flags & 32) != 0 ? true : z ? 1 : 0);
                                rec.cmd = z ? (byte) 1 : (byte) 0;
                            }
                            int oldEventCode = rec.eventCode;
                            HistoryTag oldEventTag2 = rec.eventTag;
                            rec.eventTag = new HistoryTag();
                            int i3 = 0;
                            while (i3 < 22) {
                                HashMap<String, SparseIntArray> active = tracker.getStateForEvent(i3);
                                if (active == null) {
                                    i = i3;
                                    oldEventTag = oldEventTag2;
                                } else {
                                    for (Map.Entry<String, SparseIntArray> ent : active.entrySet()) {
                                        SparseIntArray uids = ent.getValue();
                                        int j = 0;
                                        while (j < uids.size()) {
                                            rec.eventCode = i3;
                                            rec.eventTag.string = ent.getKey();
                                            rec.eventTag.uid = uids.keyAt(j);
                                            rec.eventTag.poolIdx = uids.valueAt(j);
                                            hprinter.printNextItem(proto, rec, baseTime, (flags & 32) != 0 ? true : z);
                                            rec.wakeReasonTag = null;
                                            rec.wakelockTag = null;
                                            j++;
                                            oldEventTag2 = oldEventTag2;
                                            i3 = i3;
                                            uids = uids;
                                            z = false;
                                        }
                                        z = false;
                                    }
                                    i = i3;
                                    oldEventTag = oldEventTag2;
                                }
                                i3 = i + 1;
                                oldEventTag2 = oldEventTag;
                                z = false;
                            }
                            rec.eventCode = oldEventCode;
                            rec.eventTag = oldEventTag2;
                            tracker = null;
                        }
                    }
                    printed = true;
                    z = false;
                    hprinter.printNextItem(proto, rec, baseTime, (flags & 32) != 0);
                    rec.cmd = (byte) 0;
                    if (tracker != null) {
                    }
                } else {
                    printed = printed2;
                }
                hprinter.printNextItem(proto, rec, baseTime, (flags & 32) != 0);
                batteryStats = this;
                printed2 = printed;
                lastTime = lastTime2;
                baseTime2 = baseTime;
            }
        }
        if (histStart >= 0) {
            commitCurrentHistoryBatchLocked();
            proto.write(2237677961222L, "NEXT: " + (1 + lastTime));
        }
    }

    private void dumpProtoSystemLocked(ProtoOutputStream proto, BatteryUsageStats stats) {
        long timeRemainingUs;
        int i;
        long pdcToken;
        long sToken = proto.start(1146756268038L);
        long rawUptimeUs = SystemClock.uptimeMillis() * 1000;
        long rawRealtimeMs = SystemClock.elapsedRealtime();
        long rawRealtimeUs = rawRealtimeMs * 1000;
        long bToken = proto.start(1146756268033L);
        proto.write(1112396529665L, getStartClockTime());
        proto.write(1112396529666L, getStartCount());
        proto.write(1112396529667L, computeRealtime(rawRealtimeUs, 0) / 1000);
        proto.write(1112396529668L, computeUptime(rawUptimeUs, 0) / 1000);
        proto.write(1112396529669L, computeBatteryRealtime(rawRealtimeUs, 0) / 1000);
        proto.write(1112396529670L, computeBatteryUptime(rawUptimeUs, 0) / 1000);
        proto.write(1112396529671L, computeBatteryScreenOffRealtime(rawRealtimeUs, 0) / 1000);
        proto.write(1112396529672L, computeBatteryScreenOffUptime(rawUptimeUs, 0) / 1000);
        proto.write(1112396529673L, getScreenDozeTime(rawRealtimeUs, 0) / 1000);
        proto.write(1112396529674L, getEstimatedBatteryCapacity());
        proto.write(1112396529675L, getMinLearnedBatteryCapacity());
        proto.write(1112396529676L, getMaxLearnedBatteryCapacity());
        proto.end(bToken);
        long bdToken = proto.start(1146756268034L);
        proto.write(1120986464257L, getLowDischargeAmountSinceCharge());
        proto.write(1120986464258L, getHighDischargeAmountSinceCharge());
        proto.write(1120986464259L, getDischargeAmountScreenOnSinceCharge());
        proto.write(1120986464260L, getDischargeAmountScreenOffSinceCharge());
        proto.write(1120986464261L, getDischargeAmountScreenDozeSinceCharge());
        proto.write(1112396529670L, getUahDischarge(0) / 1000);
        proto.write(1112396529671L, getUahDischargeScreenOff(0) / 1000);
        proto.write(1112396529672L, getUahDischargeScreenDoze(0) / 1000);
        proto.write(1112396529673L, getUahDischargeLightDoze(0) / 1000);
        proto.write(1112396529674L, getUahDischargeDeepDoze(0) / 1000);
        proto.end(bdToken);
        long timeRemainingUs2 = computeChargeTimeRemaining(rawRealtimeUs);
        if (timeRemainingUs2 >= 0) {
            proto.write(1112396529667L, timeRemainingUs2 / 1000);
            timeRemainingUs = timeRemainingUs2;
        } else {
            long timeRemainingUs3 = computeBatteryTimeRemaining(rawRealtimeUs);
            if (timeRemainingUs3 >= 0) {
                proto.write(1112396529668L, timeRemainingUs3 / 1000);
            } else {
                proto.write(1112396529668L, -1);
            }
            timeRemainingUs = timeRemainingUs3;
        }
        dumpDurationSteps(proto, 2246267895813L, getChargeLevelStepTracker());
        int i2 = 0;
        while (true) {
            if (i2 >= NUM_DATA_CONNECTION_TYPES) {
                break;
            }
            boolean isNone = i2 == 0;
            int telephonyNetworkType = i2;
            int telephonyNetworkType2 = (i2 == DATA_CONNECTION_OTHER || i2 == DATA_CONNECTION_EMERGENCY_SERVICE) ? 0 : telephonyNetworkType;
            long rawRealtimeUs2 = rawRealtimeUs;
            long pdcToken2 = proto.start(2246267895816L);
            if (isNone) {
                pdcToken = pdcToken2;
                proto.write(1133871366146L, isNone);
            } else {
                pdcToken = pdcToken2;
                proto.write(1159641169921L, telephonyNetworkType2);
            }
            rawRealtimeUs = rawRealtimeUs2;
            dumpTimer(proto, 1146756268035L, getPhoneDataConnectionTimer(i2), rawRealtimeUs, 0);
            proto.end(pdcToken);
            i2++;
            timeRemainingUs = timeRemainingUs;
        }
        long rawRealtimeUs3 = rawRealtimeUs;
        dumpDurationSteps(proto, 2246267895814L, getDischargeLevelStepTracker());
        long[] cpuFreqs = getCpuFreqs();
        if (cpuFreqs != null) {
            for (long i3 : cpuFreqs) {
                proto.write(SystemProto.CPU_FREQUENCY, i3);
            }
        }
        dumpControllerActivityProto(proto, 1146756268041L, getBluetoothControllerActivity(), 0);
        dumpControllerActivityProto(proto, 1146756268042L, getModemControllerActivity(), 0);
        long gnToken = proto.start(1146756268044L);
        proto.write(1112396529665L, getNetworkActivityBytes(0, 0));
        proto.write(1112396529666L, getNetworkActivityBytes(1, 0));
        proto.write(1112396529669L, getNetworkActivityPackets(0, 0));
        proto.write(1112396529670L, getNetworkActivityPackets(1, 0));
        proto.write(1112396529667L, getNetworkActivityBytes(2, 0));
        proto.write(1112396529668L, getNetworkActivityBytes(3, 0));
        proto.write(1112396529671L, getNetworkActivityPackets(2, 0));
        proto.write(1112396529672L, getNetworkActivityPackets(3, 0));
        proto.write(1112396529673L, getNetworkActivityBytes(4, 0));
        proto.write(1112396529674L, getNetworkActivityBytes(5, 0));
        proto.end(gnToken);
        dumpControllerActivityProto(proto, 1146756268043L, getWifiControllerActivity(), 0);
        long gwToken = proto.start(1146756268045L);
        proto.write(1112396529665L, getWifiOnTime(rawRealtimeUs3, 0) / 1000);
        proto.write(1112396529666L, getGlobalWifiRunningTime(rawRealtimeUs3, 0) / 1000);
        proto.end(gwToken);
        Map<String, ? extends Timer> kernelWakelocks = getKernelWakelockStats();
        for (Map.Entry<String, ? extends Timer> ent : kernelWakelocks.entrySet()) {
            long kwToken = proto.start(2246267895822L);
            proto.write(1138166333441L, ent.getKey());
            dumpTimer(proto, 1146756268034L, ent.getValue(), rawRealtimeUs3, 0);
            proto.end(kwToken);
            kernelWakelocks = kernelWakelocks;
            gwToken = gwToken;
        }
        int i4 = 1;
        SparseArray<? extends Uid> uidStats = getUidStats();
        int iu = 0;
        long fullWakeLockTimeTotalUs = 0;
        long partialWakeLockTimeTotalUs = 0;
        while (iu < uidStats.size()) {
            Uid u = uidStats.valueAt(iu);
            ArrayMap<String, ? extends Uid.Wakelock> wakelocks = u.getWakelockStats();
            int iw = wakelocks.size() - i4;
            while (iw >= 0) {
                Uid.Wakelock wl = wakelocks.valueAt(iw);
                Timer fullWakeTimer = wl.getWakeTime(i4);
                if (fullWakeTimer == null) {
                    i = 0;
                } else {
                    i = 0;
                    fullWakeLockTimeTotalUs += fullWakeTimer.getTotalTimeLocked(rawRealtimeUs3, 0);
                }
                Uid u2 = u;
                Timer partialWakeTimer = wl.getWakeTime(i);
                if (partialWakeTimer != null) {
                    partialWakeLockTimeTotalUs += partialWakeTimer.getTotalTimeLocked(rawRealtimeUs3, i);
                }
                iw--;
                u = u2;
                i4 = 1;
            }
            iu++;
            i4 = 1;
        }
        long mToken = proto.start(1146756268047L);
        proto.write(1112396529665L, getScreenOnTime(rawRealtimeUs3, 0) / 1000);
        proto.write(1112396529666L, getPhoneOnTime(rawRealtimeUs3, 0) / 1000);
        proto.write(1112396529667L, fullWakeLockTimeTotalUs / 1000);
        proto.write(1112396529668L, partialWakeLockTimeTotalUs / 1000);
        proto.write(1112396529669L, getMobileRadioActiveTime(rawRealtimeUs3, 0) / 1000);
        proto.write(1112396529670L, getMobileRadioActiveAdjustedTime(0) / 1000);
        proto.write(1120986464263L, getMobileRadioActiveCount(0));
        proto.write(1120986464264L, getMobileRadioActiveUnknownTime(0) / 1000);
        proto.write(1112396529673L, getInteractiveTime(rawRealtimeUs3, 0) / 1000);
        proto.write(1112396529674L, getPowerSaveModeEnabledTime(rawRealtimeUs3, 0) / 1000);
        proto.write(1120986464267L, getNumConnectivityChange(0));
        proto.write(1112396529676L, getDeviceIdleModeTime(2, rawRealtimeUs3, 0) / 1000);
        proto.write(1120986464269L, getDeviceIdleModeCount(2, 0));
        proto.write(1112396529678L, getDeviceIdlingTime(2, rawRealtimeUs3, 0) / 1000);
        proto.write(1120986464271L, getDeviceIdlingCount(2, 0));
        proto.write(1112396529680L, getLongestDeviceIdleModeTime(2));
        proto.write(1112396529681L, getDeviceIdleModeTime(1, rawRealtimeUs3, 0) / 1000);
        proto.write(1120986464274L, getDeviceIdleModeCount(1, 0));
        proto.write(1112396529683L, getDeviceIdlingTime(1, rawRealtimeUs3, 0) / 1000);
        proto.write(1120986464276L, getDeviceIdlingCount(1, 0));
        proto.write(1112396529685L, getLongestDeviceIdleModeTime(1));
        proto.end(mToken);
        long multicastWakeLockTimeTotalUs = getWifiMulticastWakelockTime(rawRealtimeUs3, 0);
        int multicastWakeLockCountTotal = getWifiMulticastWakelockCount(0);
        long wmctToken = proto.start(1146756268055L);
        long mToken2 = multicastWakeLockTimeTotalUs / 1000;
        proto.write(1112396529665L, mToken2);
        proto.write(1120986464258L, multicastWakeLockCountTotal);
        proto.end(wmctToken);
        BatteryConsumer deviceConsumer = stats.getAggregateBatteryConsumer(0);
        int powerComponent = 0;
        while (powerComponent < 18) {
            int n = 0;
            switch (powerComponent) {
                case 0:
                    n = 7;
                    break;
                case 2:
                    n = 5;
                    break;
                case 3:
                    n = 11;
                    break;
                case 6:
                    n = 6;
                    break;
                case 8:
                    n = 2;
                    break;
                case 11:
                    n = 4;
                    break;
                case 13:
                    n = 12;
                    break;
                case 14:
                    n = 3;
                    break;
                case 15:
                    n = 13;
                    break;
                case 16:
                    n = 1;
                    break;
            }
            long wmctToken2 = wmctToken;
            long puiToken = proto.start(2246267895825L);
            proto.write(1159641169921L, n);
            proto.write(1120986464258L, 0);
            proto.write(1103806595075L, deviceConsumer.getConsumedPower(powerComponent));
            proto.write(1133871366148L, shouldHidePowerComponent(powerComponent));
            proto.write(1103806595077L, 0);
            proto.write(1103806595078L, 0);
            proto.end(puiToken);
            powerComponent++;
            multicastWakeLockCountTotal = multicastWakeLockCountTotal;
            wmctToken = wmctToken2;
            rawUptimeUs = rawUptimeUs;
        }
        int multicastWakeLockCountTotal2 = multicastWakeLockCountTotal;
        long pusToken = proto.start(1146756268050L);
        proto.write(1103806595073L, stats.getBatteryCapacity());
        proto.write(1103806595074L, stats.getConsumedPower());
        proto.write(1103806595075L, stats.getDischargedPowerRange().getLower().doubleValue());
        proto.write(1103806595076L, stats.getDischargedPowerRange().getUpper().doubleValue());
        proto.end(pusToken);
        Map<String, ? extends Timer> rpmStats = getRpmStats();
        Map<String, ? extends Timer> screenOffRpmStats = getScreenOffRpmStats();
        for (Map.Entry<String, ? extends Timer> ent2 : rpmStats.entrySet()) {
            long rpmToken = proto.start(2246267895827L);
            proto.write(1138166333441L, ent2.getKey());
            Map<String, ? extends Timer> screenOffRpmStats2 = screenOffRpmStats;
            dumpTimer(proto, 1146756268034L, ent2.getValue(), rawRealtimeUs3, 0);
            dumpTimer(proto, 1146756268035L, screenOffRpmStats2.get(ent2.getKey()), rawRealtimeUs3, 0);
            proto.end(rpmToken);
            uidStats = uidStats;
            screenOffRpmStats = screenOffRpmStats2;
            multicastWakeLockCountTotal2 = multicastWakeLockCountTotal2;
        }
        for (int i5 = 0; i5 < 5; i5++) {
            long sbToken = proto.start(2246267895828L);
            proto.write(1159641169921L, i5);
            dumpTimer(proto, 1146756268034L, getScreenBrightnessTimer(i5), rawRealtimeUs3, 0);
            proto.end(sbToken);
        }
        dumpTimer(proto, 1146756268053L, getPhoneSignalScanningTimer(), rawRealtimeUs3, 0);
        for (int i6 = 0; i6 < CellSignalStrength.getNumSignalStrengthLevels(); i6++) {
            long pssToken = proto.start(2246267895824L);
            proto.write(1159641169921L, i6);
            dumpTimer(proto, 1146756268034L, getPhoneSignalStrengthTimer(i6), rawRealtimeUs3, 0);
            proto.end(pssToken);
        }
        Map<String, ? extends Timer> wakeupReasons = getWakeupReasonStats();
        for (Map.Entry<String, ? extends Timer> ent3 : wakeupReasons.entrySet()) {
            long wrToken = proto.start(2246267895830L);
            proto.write(1138166333441L, ent3.getKey());
            dumpTimer(proto, 1146756268034L, ent3.getValue(), rawRealtimeUs3, 0);
            proto.end(wrToken);
        }
        for (int i7 = 0; i7 < 5; i7++) {
            long wssToken = proto.start(2246267895832L);
            proto.write(1159641169921L, i7);
            dumpTimer(proto, 1146756268034L, getWifiSignalStrengthTimer(i7), rawRealtimeUs3, 0);
            proto.end(wssToken);
        }
        for (int i8 = 0; i8 < 8; i8++) {
            long wsToken = proto.start(2246267895833L);
            proto.write(1159641169921L, i8);
            dumpTimer(proto, 1146756268034L, getWifiStateTimer(i8), rawRealtimeUs3, 0);
            proto.end(wsToken);
        }
        for (int i9 = 0; i9 < 13; i9++) {
            long wssToken2 = proto.start(2246267895834L);
            proto.write(1159641169921L, i9);
            dumpTimer(proto, 1146756268034L, getWifiSupplStateTimer(i9), rawRealtimeUs3, 0);
            proto.end(wssToken2);
        }
        proto.end(sToken);
    }

    public static boolean checkWifiOnly(Context context) {
        TelephonyManager tm = (TelephonyManager) context.getSystemService(TelephonyManager.class);
        if (tm == null) {
            return false;
        }
        return !tm.isDataCapable();
    }

    private BatteryUsageStats getBatteryUsageStats(Context context) {
        BatteryUsageStatsProvider provider = new BatteryUsageStatsProvider(context, this);
        BatteryUsageStatsQuery query = new BatteryUsageStatsQuery.Builder().setMaxStatsAgeMs(0L).build();
        return provider.getBatteryUsageStats(query);
    }

    private boolean shouldHidePowerComponent(int powerComponent) {
        return powerComponent == 16 || powerComponent == 8 || powerComponent == 0 || powerComponent == 15;
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes2.dex */
    public static class ProportionalAttributionCalculator {
        private static final double SYSTEM_BATTERY_CONSUMER = -1.0d;
        private final PackageManager mPackageManager;
        private final SparseDoubleArray mProportionalPowerMah;
        private final HashSet<String> mSystemAndServicePackages;

        ProportionalAttributionCalculator(Context context, BatteryUsageStats stats) {
            double d;
            Resources resources;
            ProportionalAttributionCalculator proportionalAttributionCalculator = this;
            proportionalAttributionCalculator.mPackageManager = context.getPackageManager();
            Resources resources2 = context.getResources();
            String[] systemPackageArray = resources2.getStringArray(R.array.config_batteryPackageTypeSystem);
            String[] servicePackageArray = resources2.getStringArray(R.array.config_batteryPackageTypeService);
            proportionalAttributionCalculator.mSystemAndServicePackages = new HashSet<>(systemPackageArray.length + servicePackageArray.length);
            for (String packageName : systemPackageArray) {
                proportionalAttributionCalculator.mSystemAndServicePackages.add(packageName);
            }
            for (String packageName2 : servicePackageArray) {
                proportionalAttributionCalculator.mSystemAndServicePackages.add(packageName2);
            }
            List<UidBatteryConsumer> uidBatteryConsumers = stats.getUidBatteryConsumers();
            proportionalAttributionCalculator.mProportionalPowerMah = new SparseDoubleArray(uidBatteryConsumers.size());
            double systemPowerMah = 0.0d;
            int i = uidBatteryConsumers.size();
            while (true) {
                i--;
                d = -1.0d;
                if (i < 0) {
                    break;
                }
                UidBatteryConsumer consumer = uidBatteryConsumers.get(i);
                int uid = consumer.getUid();
                if (proportionalAttributionCalculator.isSystemUid(uid)) {
                    proportionalAttributionCalculator.mProportionalPowerMah.put(uid, -1.0d);
                    systemPowerMah += consumer.getConsumedPower();
                }
            }
            double totalRemainingPower = stats.getConsumedPower() - systemPowerMah;
            if (Math.abs(totalRemainingPower) > 0.001d) {
                int i2 = uidBatteryConsumers.size() - 1;
                while (i2 >= 0) {
                    UidBatteryConsumer consumer2 = uidBatteryConsumers.get(i2);
                    int uid2 = consumer2.getUid();
                    if (proportionalAttributionCalculator.mProportionalPowerMah.get(uid2) == d) {
                        resources = resources2;
                    } else {
                        double power = consumer2.getConsumedPower();
                        resources = resources2;
                        proportionalAttributionCalculator.mProportionalPowerMah.put(uid2, power + ((systemPowerMah * power) / totalRemainingPower));
                    }
                    i2--;
                    proportionalAttributionCalculator = this;
                    resources2 = resources;
                    d = -1.0d;
                }
            }
        }

        boolean isSystemBatteryConsumer(UidBatteryConsumer consumer) {
            return this.mProportionalPowerMah.get(consumer.getUid()) < 0.0d;
        }

        double getProportionalPowerMah(UidBatteryConsumer consumer) {
            double powerMah = this.mProportionalPowerMah.get(consumer.getUid());
            if (powerMah >= 0.0d) {
                return powerMah;
            }
            return 0.0d;
        }

        private boolean isSystemUid(int uid) {
            if (uid >= 0 && uid < 10000) {
                return true;
            }
            String[] packages = this.mPackageManager.getPackagesForUid(uid);
            if (packages == null) {
                return false;
            }
            for (String packageName : packages) {
                if (this.mSystemAndServicePackages.contains(packageName)) {
                    return true;
                }
            }
            return false;
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes2.dex */
    public static class UidMobileRadioStats {
        public final double millisecondsPerPacket;
        public final int radioActiveCount;
        public final long radioActiveMs;
        public final long rxPackets;
        public final long txPackets;
        public final int uid;

        private UidMobileRadioStats(int uid, long rxPackets, long txPackets, long radioActiveMs, int radioActiveCount, double millisecondsPerPacket) {
            this.uid = uid;
            this.txPackets = txPackets;
            this.rxPackets = rxPackets;
            this.radioActiveMs = radioActiveMs;
            this.radioActiveCount = radioActiveCount;
            this.millisecondsPerPacket = millisecondsPerPacket;
        }
    }

    private List<UidMobileRadioStats> getUidMobileRadioStats(List<UidBatteryConsumer> uidBatteryConsumers) {
        SparseArray<? extends Uid> uidStats = getUidStats();
        List<UidMobileRadioStats> uidMobileRadioStats = Lists.newArrayList();
        for (int i = 0; i < uidBatteryConsumers.size(); i++) {
            UidBatteryConsumer consumer = uidBatteryConsumers.get(i);
            if (consumer.getConsumedPower(8) != 0.0d) {
                int uid = consumer.getUid();
                Uid u = uidStats.get(uid);
                long rxPackets = u.getNetworkActivityPackets(0, 0);
                long txPackets = u.getNetworkActivityPackets(1, 0);
                if (rxPackets != 0 || txPackets != 0) {
                    long radioActiveMs = u.getMobileRadioActiveTime(0) / 1000;
                    int radioActiveCount = u.getMobileRadioActiveCount(0);
                    double msPerPacket = radioActiveMs / (rxPackets + txPackets);
                    if (msPerPacket != 0.0d) {
                        uidMobileRadioStats.add(new UidMobileRadioStats(uid, rxPackets, txPackets, radioActiveMs, radioActiveCount, msPerPacket));
                    }
                }
            }
        }
        uidMobileRadioStats.sort(new Comparator() { // from class: android.os.BatteryStats$$ExternalSyntheticLambda2
            @Override // java.util.Comparator
            public final int compare(Object obj, Object obj2) {
                int compare;
                compare = Double.compare(((BatteryStats.UidMobileRadioStats) obj2).millisecondsPerPacket, ((BatteryStats.UidMobileRadioStats) obj).millisecondsPerPacket);
                return compare;
            }
        });
        return uidMobileRadioStats;
    }
}
