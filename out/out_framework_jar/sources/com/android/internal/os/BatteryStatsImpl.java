package com.android.internal.os;

import android.app.ActivityManager;
import android.app.blob.XmlTags;
import android.app.usage.NetworkStatsManager;
import android.bluetooth.BluetoothActivityEnergyInfo;
import android.bluetooth.UidTraffic;
import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Configuration;
import android.database.ContentObserver;
import android.hardware.usb.UsbManager;
import android.inputmethodservice.navigationbar.NavigationBarInflaterView;
import android.media.AudioSystem;
import android.media.MediaMetrics;
import android.net.NetworkStats;
import android.net.Uri;
import android.net.wifi.WifiManager;
import android.os.BatteryConsumer;
import android.os.BatteryManager;
import android.os.BatteryStats;
import android.os.Binder;
import android.os.BluetoothBatteryStats;
import android.os.Build;
import android.os.Handler;
import android.os.IBatteryPropertiesRegistrar;
import android.os.Looper;
import android.os.Message;
import android.os.Parcel;
import android.os.ParcelFormatException;
import android.os.Parcelable;
import android.os.Process;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.os.SystemClock;
import android.os.UserHandle;
import android.os.WakeLockStats;
import android.os.WorkSource;
import android.os.connectivity.CellularBatteryStats;
import android.os.connectivity.GpsBatteryStats;
import android.os.connectivity.WifiActivityEnergyInfo;
import android.os.connectivity.WifiBatteryStats;
import android.provider.Settings;
import android.telecom.Logging.Session;
import android.telecom.ParcelableCallAnalytics;
import android.telephony.CellSignalStrength;
import android.telephony.CellSignalStrengthLte;
import android.telephony.CellSignalStrengthNr;
import android.telephony.ModemActivityInfo;
import android.telephony.SignalStrength;
import android.telephony.TelephonyManager;
import android.text.TextUtils;
import android.util.ArrayMap;
import android.util.ArraySet;
import android.util.AtomicFile;
import android.util.IndentingPrintWriter;
import android.util.KeyValueListParser;
import android.util.Log;
import android.util.LongSparseArray;
import android.util.LongSparseLongArray;
import android.util.MutableInt;
import android.util.Printer;
import android.util.Slog;
import android.util.SparseArray;
import android.util.SparseDoubleArray;
import android.util.SparseIntArray;
import android.util.SparseLongArray;
import android.util.TimeUtils;
import android.util.TypedXmlPullParser;
import android.util.TypedXmlSerializer;
import android.util.Xml;
import android.view.Display;
import com.android.ims.ImsConfig;
import com.android.internal.logging.EventLogTags;
import com.android.internal.os.BatteryStatsImpl;
import com.android.internal.os.BinderCallsStats;
import com.android.internal.os.KernelCpuUidTimeReader;
import com.android.internal.os.KernelWakelockStats;
import com.android.internal.os.LongArrayMultiStateCounter;
import com.android.internal.os.RpmStats;
import com.android.internal.os.SystemServerCpuThreadReader;
import com.android.internal.power.MeasuredEnergyStats;
import com.android.internal.util.ArrayUtils;
import com.android.internal.util.FrameworkStatsLog;
import com.android.internal.util.XmlUtils;
import com.android.net.module.util.NetworkCapabilitiesUtils;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Queue;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Function;
import libcore.util.EmptyArray;
import org.xmlpull.v1.XmlPullParserException;
/* loaded from: classes4.dex */
public class BatteryStatsImpl extends BatteryStats {
    static final int BATTERY_DELTA_LEVEL_FLAG = 1;
    public static final int BATTERY_PLUGGED_NONE = 0;
    public static final Parcelable.Creator<BatteryStatsImpl> CREATOR;
    private static final boolean DEBUG = false;
    private static final boolean DEBUG_BINDER_STATS = false;
    public static final boolean DEBUG_ENERGY = false;
    private static final boolean DEBUG_ENERGY_CPU = false;
    private static final boolean DEBUG_HISTORY = false;
    private static final boolean DEBUG_MEMORY = false;
    static final long DELAY_UPDATE_WAKELOCKS = 60000;
    static final int DELTA_BATTERY_CHARGE_FLAG = 16777216;
    static final int DELTA_BATTERY_LEVEL_FLAG = 524288;
    static final int DELTA_EVENT_FLAG = 8388608;
    static final int DELTA_STATE2_FLAG = 2097152;
    static final int DELTA_STATE_FLAG = 1048576;
    static final int DELTA_STATE_MASK = -33554432;
    static final int DELTA_TIME_ABS = 524285;
    static final int DELTA_TIME_INT = 524286;
    static final int DELTA_TIME_LONG = 524287;
    static final int DELTA_TIME_MASK = 524287;
    static final int DELTA_WAKELOCK_FLAG = 4194304;
    private static final int HISTORY_TAG_INDEX_LIMIT = 32766;
    private static final int MAGIC = -1166707595;
    static final int MAX_DAILY_ITEMS = 10;
    private static final int MAX_HISTORY_TAG_STRING_LENGTH = 1024;
    static final int MAX_LEVEL_STEPS = 200;
    private static final int MAX_WAKELOCKS_PER_UID;
    private static final double MILLISECONDS_IN_HOUR = 3600000.0d;
    private static final long MILLISECONDS_IN_YEAR = 31536000000L;
    static final int MSG_REPORT_CHARGING = 3;
    static final int MSG_REPORT_CPU_UPDATE_NEEDED = 1;
    static final int MSG_REPORT_POWER_CHANGE = 2;
    static final int MSG_REPORT_RESET_STATS = 4;
    private static final int NR_FREQUENCY_COUNT = 5;
    private static final int NUM_BT_TX_LEVELS = 1;
    private static final int NUM_WIFI_TX_LEVELS = 1;
    private static final int PROC_STATE_TIME_COUNTER_STATE_COUNT = 8;
    public static final int RESET_REASON_ADB_COMMAND = 2;
    public static final int RESET_REASON_CORRUPT_FILE = 1;
    public static final int RESET_REASON_FULL_CHARGE = 3;
    public static final int RESET_REASON_MEASURED_ENERGY_BUCKETS_CHANGE = 4;
    private static final long RPM_STATS_UPDATE_FREQ_MS = 1000;
    static final int STATE_BATTERY_HEALTH_MASK = 7;
    static final int STATE_BATTERY_HEALTH_SHIFT = 26;
    static final int STATE_BATTERY_MASK = -16777216;
    static final int STATE_BATTERY_PLUG_MASK = 3;
    static final int STATE_BATTERY_PLUG_SHIFT = 24;
    static final int STATE_BATTERY_STATUS_MASK = 7;
    static final int STATE_BATTERY_STATUS_SHIFT = 29;
    private static final int[] SUPPORTED_PER_PROCESS_STATE_STANDARD_ENERGY_BUCKETS;
    private static final String TAG = "BatteryStatsImpl";
    static final int TAG_FIRST_OCCURRENCE_FLAG = 32768;
    private static final int USB_DATA_CONNECTED = 2;
    private static final int USB_DATA_DISCONNECTED = 1;
    private static final int USB_DATA_UNKNOWN = 0;
    static final int VERSION = 208;
    public static final int WAKE_LOCK_WEIGHT = 50;
    private static final BatteryStats.LongCounter ZERO_LONG_COUNTER;
    private static final BatteryStats.LongCounter[] ZERO_LONG_COUNTER_ARRAY;
    private static Object mLock = new Object();
    final BatteryStats.HistoryEventTracker mActiveEvents;
    int mActiveHistoryStates;
    int mActiveHistoryStates2;
    int mActiveRat;
    int mAudioOnNesting;
    StopwatchTimer mAudioOnTimer;
    final ArrayList<StopwatchTimer> mAudioTurnedOnTimers;
    private BatteryResetListener mBatteryResetListener;
    final BatteryStatsHistory mBatteryStatsHistory;
    private BatteryStatsHistoryIterator mBatteryStatsHistoryIterator;
    private long mBatteryTimeToFullSeconds;
    private int mBatteryVoltageMv;
    private LongSamplingCounterArray mBinderThreadCpuTimesUs;
    ControllerActivityCounterImpl mBluetoothActivity;
    BluetoothPowerCalculator mBluetoothPowerCalculator;
    int mBluetoothScanNesting;
    final ArrayList<StopwatchTimer> mBluetoothScanOnTimers;
    StopwatchTimer mBluetoothScanTimer;
    private BatteryCallback mCallback;
    int mCameraOnNesting;
    StopwatchTimer mCameraOnTimer;
    final ArrayList<StopwatchTimer> mCameraTurnedOnTimers;
    int mChangedStates;
    int mChangedStates2;
    final BatteryStats.LevelStepTracker mChargeStepTracker;
    boolean mCharging;
    public final AtomicFile mCheckinFile;
    protected Clock mClock;
    protected final Constants mConstants;
    private long[] mCpuFreqs;
    private boolean mCpuFreqsInitialized;
    CpuPowerCalculator mCpuPowerCalculator;
    private long mCpuTimeReadsTrackingStartTimeMs;
    protected KernelCpuUidTimeReader.KernelCpuUidActiveTimeReader mCpuUidActiveTimeReader;
    protected KernelCpuUidTimeReader.KernelCpuUidClusterTimeReader mCpuUidClusterTimeReader;
    protected KernelCpuUidTimeReader.KernelCpuUidFreqTimeReader mCpuUidFreqTimeReader;
    protected KernelCpuUidTimeReader.KernelCpuUidUserSysTimeReader mCpuUidUserSysTimeReader;
    final BatteryStats.HistoryStepDetails mCurHistoryStepDetails;
    long mCurStepCpuSystemTimeMs;
    long mCurStepCpuUserTimeMs;
    int mCurStepMode;
    long mCurStepStatIOWaitTimeMs;
    long mCurStepStatIdleTimeMs;
    long mCurStepStatIrqTimeMs;
    long mCurStepStatSoftIrqTimeMs;
    long mCurStepStatSystemTimeMs;
    long mCurStepStatUserTimeMs;
    int mCurrentBatteryLevel;
    final BatteryStats.LevelStepTracker mDailyChargeStepTracker;
    final BatteryStats.LevelStepTracker mDailyDischargeStepTracker;
    public final AtomicFile mDailyFile;
    final ArrayList<BatteryStats.DailyItem> mDailyItems;
    ArrayList<BatteryStats.PackageChange> mDailyPackageChanges;
    long mDailyStartTimeMs;
    private final Runnable mDeferSetCharging;
    int mDeviceIdleMode;
    StopwatchTimer mDeviceIdleModeFullTimer;
    StopwatchTimer mDeviceIdleModeLightTimer;
    boolean mDeviceIdling;
    StopwatchTimer mDeviceIdlingTimer;
    boolean mDeviceLightIdling;
    StopwatchTimer mDeviceLightIdlingTimer;
    int mDischargeAmountScreenDoze;
    int mDischargeAmountScreenDozeSinceCharge;
    int mDischargeAmountScreenOff;
    int mDischargeAmountScreenOffSinceCharge;
    int mDischargeAmountScreenOn;
    int mDischargeAmountScreenOnSinceCharge;
    private LongSamplingCounter mDischargeCounter;
    int mDischargeCurrentLevel;
    private LongSamplingCounter mDischargeDeepDozeCounter;
    private LongSamplingCounter mDischargeLightDozeCounter;
    int mDischargePlugLevel;
    private LongSamplingCounter mDischargeScreenDozeCounter;
    int mDischargeScreenDozeUnplugLevel;
    private LongSamplingCounter mDischargeScreenOffCounter;
    int mDischargeScreenOffUnplugLevel;
    int mDischargeScreenOnUnplugLevel;
    int mDischargeStartLevel;
    final BatteryStats.LevelStepTracker mDischargeStepTracker;
    int mDischargeUnplugLevel;
    private int mDisplayMismatchWtfCount;
    boolean mDistributeWakelockCpu;
    final ArrayList<StopwatchTimer> mDrawTimers;
    String mEndPlatformVersion;
    private int mEstimatedBatteryCapacityMah;
    private ExternalStatsSync mExternalSync;
    int mFlashlightOnNesting;
    StopwatchTimer mFlashlightOnTimer;
    final ArrayList<StopwatchTimer> mFlashlightTurnedOnTimers;
    final ArrayList<StopwatchTimer> mFullTimers;
    final ArrayList<StopwatchTimer> mFullWifiLockTimers;
    protected MeasuredEnergyStats mGlobalMeasuredEnergyStats;
    boolean mGlobalWifiRunning;
    StopwatchTimer mGlobalWifiRunningTimer;
    int mGpsNesting;
    int mGpsSignalQualityBin;
    final StopwatchTimer[] mGpsSignalQualityTimer;
    public Handler mHandler;
    boolean mHasBluetoothReporting;
    boolean mHasModemReporting;
    boolean mHasWifiReporting;
    protected boolean mHaveBatteryLevel;
    int mHighDischargeAmountSinceCharge;
    BatteryStats.HistoryItem mHistory;
    final BatteryStats.HistoryItem mHistoryAddTmp;
    long mHistoryBaseTimeMs;
    final Parcel mHistoryBuffer;
    int mHistoryBufferLastPos;
    BatteryStats.HistoryItem mHistoryCache;
    final BatteryStats.HistoryItem mHistoryCur;
    BatteryStats.HistoryItem mHistoryEnd;
    private BatteryStats.HistoryItem mHistoryIterator;
    BatteryStats.HistoryItem mHistoryLastEnd;
    final BatteryStats.HistoryItem mHistoryLastLastWritten;
    final BatteryStats.HistoryItem mHistoryLastWritten;
    final HashMap<BatteryStats.HistoryTag, Integer> mHistoryTagPool;
    private SparseArray<BatteryStats.HistoryTag> mHistoryTags;
    boolean mIgnoreNextExternalStats;
    int mInitStepMode;
    private String mInitialAcquireWakeName;
    private int mInitialAcquireWakeUid;
    boolean mInteractive;
    StopwatchTimer mInteractiveTimer;
    final SparseIntArray mIsolatedUidRefCounts;
    final SparseIntArray mIsolatedUids;
    protected KernelCpuSpeedReader[] mKernelCpuSpeedReaders;
    private final KernelMemoryBandwidthStats mKernelMemoryBandwidthStats;
    private final LongSparseArray<SamplingTimer> mKernelMemoryStats;
    protected KernelSingleUidTimeReader mKernelSingleUidTimeReader;
    private final KernelWakelockReader mKernelWakelockReader;
    private final HashMap<String, SamplingTimer> mKernelWakelockStats;
    private final BluetoothActivityInfoCache mLastBluetoothActivityInfo;
    int mLastChargeStepLevel;
    int mLastChargingStateLevel;
    int mLastDischargeStepLevel;
    long mLastHistoryElapsedRealtimeMs;
    BatteryStats.HistoryStepDetails mLastHistoryStepDetails;
    byte mLastHistoryStepLevel;
    long mLastIdleTimeStartMs;
    private int mLastLearnedBatteryCapacityUah;
    private ModemActivityInfo mLastModemActivityInfo;
    private NetworkStats mLastModemNetworkStats;
    protected ArrayList<StopwatchTimer> mLastPartialTimers;
    private long mLastRpmStatsUpdateTimeMs;
    long mLastStepCpuSystemTimeMs;
    long mLastStepCpuUserTimeMs;
    long mLastStepStatIOWaitTimeMs;
    long mLastStepStatIdleTimeMs;
    long mLastStepStatIrqTimeMs;
    long mLastStepStatSoftIrqTimeMs;
    long mLastStepStatSystemTimeMs;
    long mLastStepStatUserTimeMs;
    String mLastWakeupReason;
    long mLastWakeupUptimeMs;
    private NetworkStats mLastWifiNetworkStats;
    long mLastWriteTimeMs;
    long mLongestFullIdleTimeMs;
    long mLongestLightIdleTimeMs;
    int mLowDischargeAmountSinceCharge;
    int mMaxChargeStepLevel;
    private int mMaxLearnedBatteryCapacityUah;
    public final MeasuredEnergyRetriever mMeasuredEnergyRetriever;
    protected MeasuredEnergyStats.Config mMeasuredEnergyStatsConfig;
    int mMinDischargeStepLevel;
    private int mMinLearnedBatteryCapacityUah;
    LongSamplingCounter mMobileRadioActiveAdjustedTime;
    StopwatchTimer mMobileRadioActivePerAppTimer;
    long mMobileRadioActiveStartTimeMs;
    StopwatchTimer mMobileRadioActiveTimer;
    LongSamplingCounter mMobileRadioActiveUnknownCount;
    LongSamplingCounter mMobileRadioActiveUnknownTime;
    MobileRadioPowerCalculator mMobileRadioPowerCalculator;
    int mMobileRadioPowerState;
    int mModStepMode;
    ControllerActivityCounterImpl mModemActivity;
    private String[] mModemIfaces;
    private final Object mModemNetworkLock;
    final LongSamplingCounter[] mNetworkByteActivityCounters;
    final LongSamplingCounter[] mNetworkPacketActivityCounters;
    int mNextHistoryTagIdx;
    long mNextMaxDailyDeadlineMs;
    long mNextMinDailyDeadlineMs;
    boolean mNoAutoReset;
    private int mNumAllUidCpuTimeReads;
    private int mNumConnectivityChange;
    int mNumHistoryItems;
    int mNumHistoryTagChars;
    private long mNumSingleUidCpuTimeReads;
    private int mNumUidsRemoved;
    boolean mOnBattery;
    protected boolean mOnBatteryInternal;
    protected final TimeBase mOnBatteryScreenOffTimeBase;
    protected final TimeBase mOnBatteryTimeBase;
    protected ArrayList<StopwatchTimer> mPartialTimers;
    protected Queue<UidToRemove> mPendingRemovedUids;
    DisplayBatteryStats[] mPerDisplayBatteryStats;
    public boolean mPerProcStateCpuTimesAvailable;
    RadioAccessTechnologyBatteryStats[] mPerRatBatteryStats;
    int mPhoneDataConnectionType;
    final StopwatchTimer[] mPhoneDataConnectionsTimer;
    boolean mPhoneOn;
    StopwatchTimer mPhoneOnTimer;
    private int mPhoneServiceState;
    private int mPhoneServiceStateRaw;
    StopwatchTimer mPhoneSignalScanningTimer;
    int mPhoneSignalStrengthBin;
    int mPhoneSignalStrengthBinRaw;
    final StopwatchTimer[] mPhoneSignalStrengthsTimer;
    private int mPhoneSimStateRaw;
    private final PlatformIdleStateCallback mPlatformIdleStateCallback;
    protected PowerProfile mPowerProfile;
    boolean mPowerSaveModeEnabled;
    StopwatchTimer mPowerSaveModeEnabledTimer;
    boolean mPretendScreenOff;
    final BatteryStats.HistoryStepDetails mReadHistoryStepDetails;
    long mRealtimeStartUs;
    long mRealtimeUs;
    public boolean mRecordAllHistory;
    protected boolean mRecordingHistory;
    private final HashMap<String, SamplingTimer> mRpmStats;
    int mScreenBrightnessBin;
    final StopwatchTimer[] mScreenBrightnessTimer;
    StopwatchTimer mScreenDozeTimer;
    private final HashMap<String, SamplingTimer> mScreenOffRpmStats;
    StopwatchTimer mScreenOnTimer;
    protected int mScreenState;
    int mSensorNesting;
    final SparseArray<ArrayList<StopwatchTimer>> mSensorTimers;
    boolean mShuttingDown;
    long mStartClockTimeMs;
    int mStartCount;
    String mStartPlatformVersion;
    private final AtomicFile mStatsFile;
    private boolean mSystemReady;
    protected SystemServerCpuThreadReader mSystemServerCpuThreadReader;
    long mTempTotalCpuSystemTimeUs;
    long mTempTotalCpuUserTimeUs;
    private LongArrayMultiStateCounter.LongArrayContainer mTmpCpuTimeInFreq;
    final BatteryStats.HistoryStepDetails mTmpHistoryStepDetails;
    private final RailStats mTmpRailStats;
    private RpmStats mTmpRpmStats;
    private final KernelWakelockStats mTmpWakelockStats;
    long mTrackRunningHistoryElapsedRealtimeMs;
    long mTrackRunningHistoryUptimeMs;
    final SparseArray<Uid> mUidStats;
    long mUptimeStartUs;
    long mUptimeUs;
    int mUsbDataState;
    protected UserInfoProvider mUserInfoProvider;
    int mVideoOnNesting;
    StopwatchTimer mVideoOnTimer;
    final ArrayList<StopwatchTimer> mVideoTurnedOnTimers;
    long[][] mWakeLockAllocationsUs;
    boolean mWakeLockImportant;
    int mWakeLockNesting;
    private final HashMap<String, SamplingTimer> mWakeupReasonStats;
    StopwatchTimer mWifiActiveTimer;
    ControllerActivityCounterImpl mWifiActivity;
    final SparseArray<ArrayList<StopwatchTimer>> mWifiBatchedScanTimers;
    int mWifiFullLockNesting;
    private String[] mWifiIfaces;
    int mWifiMulticastNesting;
    final ArrayList<StopwatchTimer> mWifiMulticastTimers;
    StopwatchTimer mWifiMulticastWakelockTimer;
    private final Object mWifiNetworkLock;
    boolean mWifiOn;
    StopwatchTimer mWifiOnTimer;
    WifiPowerCalculator mWifiPowerCalculator;
    int mWifiRadioPowerState;
    final ArrayList<StopwatchTimer> mWifiRunningTimers;
    int mWifiScanNesting;
    final ArrayList<StopwatchTimer> mWifiScanTimers;
    int mWifiSignalStrengthBin;
    final StopwatchTimer[] mWifiSignalStrengthsTimer;
    int mWifiState;
    final StopwatchTimer[] mWifiStateTimer;
    int mWifiSupplState;
    final StopwatchTimer[] mWifiSupplStateTimer;
    final ArrayList<StopwatchTimer> mWindowTimers;
    final ReentrantLock mWriteLock;

    /* loaded from: classes4.dex */
    public interface BatteryCallback {
        void batteryNeedsCpuUpdate();

        void batteryPowerChanged(boolean z);

        void batterySendBroadcast(Intent intent);

        void batteryStatsReset();
    }

    /* loaded from: classes4.dex */
    public interface BatteryResetListener {
        void prepareForBatteryStatsReset(int i);
    }

    /* loaded from: classes4.dex */
    public interface ExternalStatsSync {
        public static final int RESET = 64;
        public static final int UPDATE_ALL = 63;
        public static final int UPDATE_BT = 8;
        public static final int UPDATE_CPU = 1;
        public static final int UPDATE_DISPLAY = 32;
        public static final int UPDATE_ON_PROC_STATE_CHANGE = 14;
        public static final int UPDATE_ON_RESET = 127;
        public static final int UPDATE_RADIO = 4;
        public static final int UPDATE_RPM = 16;
        public static final int UPDATE_WIFI = 2;

        @Retention(RetentionPolicy.SOURCE)
        /* loaded from: classes4.dex */
        public @interface ExternalUpdateFlag {
        }

        void cancelCpuSyncDueToWakelockChange();

        Future<?> scheduleCleanupDueToRemovedUser(int i);

        Future<?> scheduleCpuSyncDueToRemovedUid(int i);

        Future<?> scheduleCpuSyncDueToSettingChange();

        Future<?> scheduleCpuSyncDueToWakelockChange(long j);

        Future<?> scheduleSync(String str, int i);

        Future<?> scheduleSyncDueToBatteryLevelChange(long j);

        void scheduleSyncDueToProcessStateChange(int i, long j);

        Future<?> scheduleSyncDueToScreenStateChange(int i, boolean z, boolean z2, int i2, int[] iArr);
    }

    /* loaded from: classes4.dex */
    public interface MeasuredEnergyRetriever {
        void fillRailDataStats(RailStats railStats);
    }

    /* loaded from: classes4.dex */
    public interface PlatformIdleStateCallback {
        void fillLowPowerStats(RpmStats rpmStats);

        String getSubsystemLowPowerStats();
    }

    static {
        if (ActivityManager.isLowRamDeviceStatic()) {
            MAX_WAKELOCKS_PER_UID = 40;
        } else {
            MAX_WAKELOCKS_PER_UID = 200;
        }
        BatteryStats.LongCounter longCounter = new BatteryStats.LongCounter() { // from class: com.android.internal.os.BatteryStatsImpl.1
            @Override // android.os.BatteryStats.LongCounter
            public long getCountLocked(int which) {
                return 0L;
            }

            @Override // android.os.BatteryStats.LongCounter
            public long getCountForProcessState(int procState) {
                return 0L;
            }

            @Override // android.os.BatteryStats.LongCounter
            public void logState(Printer pw, String prefix) {
                pw.println(prefix + "mCount=0");
            }
        };
        ZERO_LONG_COUNTER = longCounter;
        ZERO_LONG_COUNTER_ARRAY = new BatteryStats.LongCounter[]{longCounter};
        SUPPORTED_PER_PROCESS_STATE_STANDARD_ENERGY_BUCKETS = new int[]{3, 7, 4, 5};
        CREATOR = new Parcelable.Creator<BatteryStatsImpl>() { // from class: com.android.internal.os.BatteryStatsImpl.7
            /* JADX DEBUG: Method merged with bridge method */
            /* JADX WARN: Can't rename method to resolve collision */
            @Override // android.os.Parcelable.Creator
            public BatteryStatsImpl createFromParcel(Parcel in) {
                return new BatteryStatsImpl(in);
            }

            /* JADX DEBUG: Method merged with bridge method */
            /* JADX WARN: Can't rename method to resolve collision */
            @Override // android.os.Parcelable.Creator
            public BatteryStatsImpl[] newArray(int size) {
                return new BatteryStatsImpl[size];
            }
        };
    }

    @Override // android.os.BatteryStats
    public LongSparseArray<SamplingTimer> getKernelMemoryStats() {
        return this.mKernelMemoryStats;
    }

    /* loaded from: classes4.dex */
    public final class UidToRemove {
        private final int mEndUid;
        private final int mStartUid;
        private final long mUidRemovalTimestamp;

        public UidToRemove(BatteryStatsImpl this$0, int uid, long timestamp) {
            this(uid, uid, timestamp);
        }

        public UidToRemove(int startUid, int endUid, long timestamp) {
            this.mStartUid = startUid;
            this.mEndUid = endUid;
            this.mUidRemovalTimestamp = timestamp;
        }

        public long getUidRemovalTimestamp() {
            return this.mUidRemovalTimestamp;
        }

        void removeLocked() {
            BatteryStatsImpl.this.removeCpuStatsForUidRangeLocked(this.mStartUid, this.mEndUid);
        }
    }

    /* loaded from: classes4.dex */
    public static abstract class UserInfoProvider {
        private int[] userIds;

        protected abstract int[] getUserIds();

        public final void refreshUserIds() {
            this.userIds = getUserIds();
        }

        public boolean exists(int userId) {
            int[] iArr = this.userIds;
            if (iArr != null) {
                return ArrayUtils.contains(iArr, userId);
            }
            return true;
        }
    }

    /* loaded from: classes4.dex */
    final class MyHandler extends Handler {
        public MyHandler(Looper looper) {
            super(looper, null, true);
        }

        @Override // android.os.Handler
        public void handleMessage(Message msg) {
            String action;
            BatteryCallback cb = BatteryStatsImpl.this.mCallback;
            switch (msg.what) {
                case 1:
                    if (cb != null) {
                        cb.batteryNeedsCpuUpdate();
                        return;
                    }
                    return;
                case 2:
                    if (cb != null) {
                        cb.batteryPowerChanged(msg.arg1 != 0);
                        return;
                    }
                    return;
                case 3:
                    if (cb != null) {
                        synchronized (BatteryStatsImpl.this) {
                            action = BatteryStatsImpl.this.mCharging ? BatteryManager.ACTION_CHARGING : BatteryManager.ACTION_DISCHARGING;
                        }
                        Intent intent = new Intent(action);
                        intent.addFlags(67108864);
                        cb.batterySendBroadcast(intent);
                        return;
                    }
                    return;
                case 4:
                    if (cb != null) {
                        cb.batteryStatsReset();
                        return;
                    }
                    return;
                default:
                    return;
            }
        }
    }

    public void postBatteryNeedsCpuUpdateMsg() {
        this.mHandler.sendEmptyMessage(1);
    }

    public void updateProcStateCpuTimesLocked(int uid, long timestampMs) {
        if (!initKernelSingleUidTimeReaderLocked()) {
            return;
        }
        Uid u = getUidStatsLocked(uid);
        this.mNumSingleUidCpuTimeReads++;
        LongArrayMultiStateCounter onBatteryCounter = u.getProcStateTimeCounter(timestampMs).getCounter();
        LongArrayMultiStateCounter onBatteryScreenOffCounter = u.getProcStateScreenOffTimeCounter(timestampMs).getCounter();
        this.mKernelSingleUidTimeReader.addDelta(uid, onBatteryCounter, timestampMs);
        this.mKernelSingleUidTimeReader.addDelta(uid, onBatteryScreenOffCounter, timestampMs);
        if (u.mChildUids != null) {
            LongArrayMultiStateCounter.LongArrayContainer deltaContainer = getCpuTimeInFreqContainer();
            int childUidCount = u.mChildUids.size();
            for (int j = childUidCount - 1; j >= 0; j--) {
                LongArrayMultiStateCounter cpuTimeInFreqCounter = u.mChildUids.valueAt(j).cpuTimeInFreqCounter;
                if (cpuTimeInFreqCounter != null) {
                    this.mKernelSingleUidTimeReader.addDelta(u.mChildUids.keyAt(j), cpuTimeInFreqCounter, timestampMs, deltaContainer);
                    onBatteryCounter.addCounts(deltaContainer);
                    onBatteryScreenOffCounter.addCounts(deltaContainer);
                }
            }
        }
    }

    public void clearPendingRemovedUidsLocked() {
        long cutOffTimeMs = this.mClock.elapsedRealtime() - this.mConstants.UID_REMOVE_DELAY_MS;
        while (!this.mPendingRemovedUids.isEmpty() && this.mPendingRemovedUids.peek().getUidRemovalTimestamp() < cutOffTimeMs) {
            this.mPendingRemovedUids.poll().removeLocked();
        }
    }

    public void updateCpuTimesForAllUids() {
        SparseArray<long[]> allUidCpuFreqTimesMs;
        LongArrayMultiStateCounter onBatteryScreenOffCounter;
        synchronized (this) {
            if (trackPerProcStateCpuTimes()) {
                if (initKernelSingleUidTimeReaderLocked()) {
                    SparseArray<long[]> allUidCpuFreqTimesMs2 = this.mCpuUidFreqTimeReader.getAllUidCpuFreqTimeMs();
                    int i = allUidCpuFreqTimesMs2.size() - 1;
                    while (i >= 0) {
                        int uid = allUidCpuFreqTimesMs2.keyAt(i);
                        int parentUid = mapUid(uid);
                        Uid u = getAvailableUidStatsLocked(parentUid);
                        if (u == null) {
                            allUidCpuFreqTimesMs = allUidCpuFreqTimesMs2;
                        } else {
                            int procState = u.mProcessState;
                            if (procState == 7) {
                                allUidCpuFreqTimesMs = allUidCpuFreqTimesMs2;
                            } else {
                                long timestampMs = this.mClock.elapsedRealtime();
                                LongArrayMultiStateCounter onBatteryCounter = u.getProcStateTimeCounter(timestampMs).getCounter();
                                LongArrayMultiStateCounter onBatteryScreenOffCounter2 = u.getProcStateScreenOffTimeCounter(timestampMs).getCounter();
                                if (uid == parentUid) {
                                    allUidCpuFreqTimesMs = allUidCpuFreqTimesMs2;
                                    onBatteryScreenOffCounter = onBatteryScreenOffCounter2;
                                } else if (Process.isSdkSandboxUid(uid)) {
                                    allUidCpuFreqTimesMs = allUidCpuFreqTimesMs2;
                                    onBatteryScreenOffCounter = onBatteryScreenOffCounter2;
                                } else {
                                    Uid.ChildUid childUid = u.getChildUid(uid);
                                    if (childUid == null) {
                                        allUidCpuFreqTimesMs = allUidCpuFreqTimesMs2;
                                    } else {
                                        LongArrayMultiStateCounter counter = childUid.cpuTimeInFreqCounter;
                                        if (counter == null) {
                                            allUidCpuFreqTimesMs = allUidCpuFreqTimesMs2;
                                        } else {
                                            LongArrayMultiStateCounter.LongArrayContainer deltaContainer = getCpuTimeInFreqContainer();
                                            allUidCpuFreqTimesMs = allUidCpuFreqTimesMs2;
                                            this.mKernelSingleUidTimeReader.addDelta(uid, counter, timestampMs, deltaContainer);
                                            onBatteryCounter.addCounts(deltaContainer);
                                            onBatteryScreenOffCounter2.addCounts(deltaContainer);
                                        }
                                    }
                                }
                                this.mKernelSingleUidTimeReader.addDelta(parentUid, onBatteryCounter, timestampMs);
                                this.mKernelSingleUidTimeReader.addDelta(parentUid, onBatteryScreenOffCounter, timestampMs);
                            }
                        }
                        i--;
                        allUidCpuFreqTimesMs2 = allUidCpuFreqTimesMs;
                    }
                }
            }
        }
    }

    public static long[] addCpuTimes(long[] timesA, long[] timesB) {
        if (timesA != null && timesB != null) {
            for (int i = timesA.length - 1; i >= 0; i--) {
                timesA[i] = timesA[i] + timesB[i];
            }
            return timesA;
        } else if (timesA == null) {
            if (timesB == null) {
                return null;
            }
            return timesB;
        } else {
            return timesA;
        }
    }

    private boolean initKernelSingleUidTimeReaderLocked() {
        boolean z = false;
        if (this.mKernelSingleUidTimeReader == null) {
            PowerProfile powerProfile = this.mPowerProfile;
            if (powerProfile == null) {
                return false;
            }
            if (this.mCpuFreqs == null) {
                this.mCpuFreqs = this.mCpuUidFreqTimeReader.readFreqs(powerProfile);
            }
            if (this.mCpuFreqs != null) {
                this.mKernelSingleUidTimeReader = new KernelSingleUidTimeReader(this.mCpuFreqs.length);
            } else {
                this.mPerProcStateCpuTimesAvailable = this.mCpuUidFreqTimeReader.allUidTimesAvailable();
                return false;
            }
        }
        if (this.mCpuUidFreqTimeReader.allUidTimesAvailable() && this.mKernelSingleUidTimeReader.singleUidCpuTimesAvailable()) {
            z = true;
        }
        this.mPerProcStateCpuTimesAvailable = z;
        return true;
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes4.dex */
    public static class DisplayBatteryStats {
        public StopwatchTimer screenDozeTimer;
        public StopwatchTimer screenOnTimer;
        public int screenState = 0;
        public int screenBrightnessBin = -1;
        public StopwatchTimer[] screenBrightnessTimers = new StopwatchTimer[5];
        public int screenStateAtLastEnergyMeasurement = 0;

        DisplayBatteryStats(Clock clock, TimeBase timeBase) {
            this.screenOnTimer = new StopwatchTimer(clock, null, -1, null, timeBase);
            this.screenDozeTimer = new StopwatchTimer(clock, null, -1, null, timeBase);
            for (int i = 0; i < 5; i++) {
                this.screenBrightnessTimers[i] = new StopwatchTimer(clock, null, (-100) - i, null, timeBase);
            }
        }

        public void reset(long elapsedRealtimeUs) {
            this.screenOnTimer.reset(false, elapsedRealtimeUs);
            this.screenDozeTimer.reset(false, elapsedRealtimeUs);
            for (int i = 0; i < 5; i++) {
                this.screenBrightnessTimers[i].reset(false, elapsedRealtimeUs);
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes4.dex */
    public static class RadioAccessTechnologyBatteryStats {
        public final StopwatchTimer[][] perStateTimers;
        private boolean mActive = false;
        private int mFrequencyRange = 0;
        private int mSignalStrength = 0;
        private LongSamplingCounter[][] mPerStateTxDurationMs = null;
        private LongSamplingCounter[] mPerFrequencyRxDurationMs = null;

        RadioAccessTechnologyBatteryStats(int freqCount, Clock clock, TimeBase timeBase) {
            this.perStateTimers = (StopwatchTimer[][]) Array.newInstance(StopwatchTimer.class, freqCount, 6);
            for (int i = 0; i < freqCount; i++) {
                for (int j = 0; j < 6; j++) {
                    this.perStateTimers[i][j] = new StopwatchTimer(clock, null, -1, null, timeBase);
                }
            }
        }

        public void noteActive(boolean active, long elapsedRealtimeMs) {
            if (this.mActive == active) {
                return;
            }
            this.mActive = active;
            if (active) {
                this.perStateTimers[this.mFrequencyRange][this.mSignalStrength].startRunningLocked(elapsedRealtimeMs);
            } else {
                this.perStateTimers[this.mFrequencyRange][this.mSignalStrength].stopRunningLocked(elapsedRealtimeMs);
            }
        }

        public void noteFrequencyRange(int frequencyRange, long elapsedRealtimeMs) {
            int i = this.mFrequencyRange;
            if (i == frequencyRange) {
                return;
            }
            if (!this.mActive) {
                this.mFrequencyRange = frequencyRange;
                return;
            }
            this.perStateTimers[i][this.mSignalStrength].stopRunningLocked(elapsedRealtimeMs);
            this.perStateTimers[frequencyRange][this.mSignalStrength].startRunningLocked(elapsedRealtimeMs);
            this.mFrequencyRange = frequencyRange;
        }

        public void noteSignalStrength(int signalStrength, long elapsedRealtimeMs) {
            int i = this.mSignalStrength;
            if (i == signalStrength) {
                return;
            }
            if (!this.mActive) {
                this.mSignalStrength = signalStrength;
                return;
            }
            this.perStateTimers[this.mFrequencyRange][i].stopRunningLocked(elapsedRealtimeMs);
            this.perStateTimers[this.mFrequencyRange][signalStrength].startRunningLocked(elapsedRealtimeMs);
            this.mSignalStrength = signalStrength;
        }

        public long getTimeSinceMark(int frequencyRange, int signalStrength, long elapsedRealtimeMs) {
            return this.perStateTimers[frequencyRange][signalStrength].getTimeSinceMarkLocked(elapsedRealtimeMs * 1000) / 1000;
        }

        public void setMark(long elapsedRealtimeMs) {
            int size = this.perStateTimers.length;
            for (int i = 0; i < size; i++) {
                for (int j = 0; j < 6; j++) {
                    this.perStateTimers[i][j].setMark(elapsedRealtimeMs);
                }
            }
        }

        public int getFrequencyRangeCount() {
            return this.perStateTimers.length;
        }

        public void incrementTxDuration(int frequencyRange, int signalStrength, long durationMs) {
            getTxDurationCounter(frequencyRange, signalStrength, true).addCountLocked(durationMs);
        }

        public void incrementRxDuration(int frequencyRange, long durationMs) {
            getRxDurationCounter(frequencyRange, true).addCountLocked(durationMs);
        }

        public void reset(long elapsedRealtimeUs) {
            int size = this.perStateTimers.length;
            for (int i = 0; i < size; i++) {
                for (int j = 0; j < 6; j++) {
                    this.perStateTimers[i][j].reset(false, elapsedRealtimeUs);
                    LongSamplingCounter[][] longSamplingCounterArr = this.mPerStateTxDurationMs;
                    if (longSamplingCounterArr != null) {
                        longSamplingCounterArr[i][j].reset(false, elapsedRealtimeUs);
                    }
                }
                LongSamplingCounter[] longSamplingCounterArr2 = this.mPerFrequencyRxDurationMs;
                if (longSamplingCounterArr2 != null) {
                    longSamplingCounterArr2[i].reset(false, elapsedRealtimeUs);
                }
            }
        }

        public void writeSummaryToParcel(Parcel out, long elapsedRealtimeUs) {
            int freqCount = this.perStateTimers.length;
            out.writeInt(freqCount);
            out.writeInt(6);
            for (int i = 0; i < freqCount; i++) {
                for (int j = 0; j < 6; j++) {
                    this.perStateTimers[i][j].writeSummaryFromParcelLocked(out, elapsedRealtimeUs);
                }
            }
            if (this.mPerStateTxDurationMs == null) {
                out.writeInt(0);
            } else {
                out.writeInt(1);
                for (int i2 = 0; i2 < freqCount; i2++) {
                    for (int j2 = 0; j2 < 6; j2++) {
                        this.mPerStateTxDurationMs[i2][j2].writeSummaryFromParcelLocked(out);
                    }
                }
            }
            if (this.mPerFrequencyRxDurationMs == null) {
                out.writeInt(0);
                return;
            }
            out.writeInt(1);
            for (int i3 = 0; i3 < freqCount; i3++) {
                this.mPerFrequencyRxDurationMs[i3].writeSummaryFromParcelLocked(out);
            }
        }

        public void readSummaryFromParcel(Parcel in) {
            int oldFreqCount = in.readInt();
            int oldSignalStrengthCount = in.readInt();
            int currFreqCount = this.perStateTimers.length;
            for (int freq = 0; freq < oldFreqCount; freq++) {
                for (int strength = 0; strength < oldSignalStrengthCount; strength++) {
                    if (freq >= currFreqCount || strength >= 6) {
                        StopwatchTimer temp = new StopwatchTimer(null, null, -1, null, new TimeBase());
                        temp.readSummaryFromParcelLocked(in);
                    } else {
                        this.perStateTimers[freq][strength].readSummaryFromParcelLocked(in);
                    }
                }
            }
            int freq2 = in.readInt();
            if (freq2 == 1) {
                for (int freq3 = 0; freq3 < oldFreqCount; freq3++) {
                    for (int strength2 = 0; strength2 < oldSignalStrengthCount; strength2++) {
                        if (freq3 >= currFreqCount || strength2 >= 6) {
                            StopwatchTimer temp2 = new StopwatchTimer(null, null, -1, null, new TimeBase());
                            temp2.readSummaryFromParcelLocked(in);
                        }
                        getTxDurationCounter(freq3, strength2, true).readSummaryFromParcelLocked(in);
                    }
                }
            }
            int freq4 = in.readInt();
            if (freq4 == 1) {
                for (int freq5 = 0; freq5 < oldFreqCount; freq5++) {
                    if (freq5 >= currFreqCount) {
                        StopwatchTimer temp3 = new StopwatchTimer(null, null, -1, null, new TimeBase());
                        temp3.readSummaryFromParcelLocked(in);
                    } else {
                        getRxDurationCounter(freq5, true).readSummaryFromParcelLocked(in);
                    }
                }
            }
        }

        /* JADX INFO: Access modifiers changed from: private */
        public LongSamplingCounter getTxDurationCounter(int frequencyRange, int signalStrength, boolean make) {
            if (this.mPerStateTxDurationMs == null) {
                if (!make) {
                    return null;
                }
                int freqCount = getFrequencyRangeCount();
                StopwatchTimer[] stopwatchTimerArr = this.perStateTimers[0];
                int signalStrengthCount = stopwatchTimerArr.length;
                TimeBase timeBase = stopwatchTimerArr[0].mTimeBase;
                this.mPerStateTxDurationMs = (LongSamplingCounter[][]) Array.newInstance(LongSamplingCounter.class, freqCount, signalStrengthCount);
                for (int freq = 0; freq < freqCount; freq++) {
                    for (int strength = 0; strength < signalStrengthCount; strength++) {
                        this.mPerStateTxDurationMs[freq][strength] = new LongSamplingCounter(timeBase);
                    }
                }
            }
            if (frequencyRange < 0 || frequencyRange >= getFrequencyRangeCount()) {
                Slog.w(BatteryStatsImpl.TAG, "Unexpected frequency range (" + frequencyRange + ") requested in getTxDurationCounter");
                return null;
            } else if (signalStrength < 0 || signalStrength >= this.perStateTimers[0].length) {
                Slog.w(BatteryStatsImpl.TAG, "Unexpected signal strength (" + signalStrength + ") requested in getTxDurationCounter");
                return null;
            } else {
                return this.mPerStateTxDurationMs[frequencyRange][signalStrength];
            }
        }

        /* JADX INFO: Access modifiers changed from: private */
        public LongSamplingCounter getRxDurationCounter(int frequencyRange, boolean make) {
            if (this.mPerFrequencyRxDurationMs == null) {
                if (!make) {
                    return null;
                }
                int freqCount = getFrequencyRangeCount();
                TimeBase timeBase = this.perStateTimers[0][0].mTimeBase;
                this.mPerFrequencyRxDurationMs = new LongSamplingCounter[freqCount];
                for (int freq = 0; freq < freqCount; freq++) {
                    this.mPerFrequencyRxDurationMs[freq] = new LongSamplingCounter(timeBase);
                }
            }
            if (frequencyRange < 0 || frequencyRange >= getFrequencyRangeCount()) {
                Slog.w(BatteryStatsImpl.TAG, "Unexpected frequency range (" + frequencyRange + ") requested in getRxDurationCounter");
                return null;
            }
            return this.mPerFrequencyRxDurationMs[frequencyRange];
        }
    }

    private RadioAccessTechnologyBatteryStats getRatBatteryStatsLocked(int rat) {
        RadioAccessTechnologyBatteryStats stats = this.mPerRatBatteryStats[rat];
        if (stats == null) {
            int freqCount = rat == 2 ? 5 : 1;
            RadioAccessTechnologyBatteryStats stats2 = new RadioAccessTechnologyBatteryStats(freqCount, this.mClock, this.mOnBatteryTimeBase);
            this.mPerRatBatteryStats[rat] = stats2;
            return stats2;
        }
        return stats;
    }

    @Override // android.os.BatteryStats
    public Map<String, ? extends Timer> getRpmStats() {
        return this.mRpmStats;
    }

    @Override // android.os.BatteryStats
    public Map<String, ? extends Timer> getScreenOffRpmStats() {
        return this.mScreenOffRpmStats;
    }

    @Override // android.os.BatteryStats
    public Map<String, ? extends Timer> getKernelWakelockStats() {
        return this.mKernelWakelockStats;
    }

    @Override // android.os.BatteryStats
    public WakeLockStats getWakeLockStats() {
        long j;
        long realtimeMs = this.mClock.elapsedRealtime();
        long j2 = 1000;
        long realtimeUs = realtimeMs * 1000;
        List<WakeLockStats.WakeLock> uidWakeLockStats = new ArrayList<>();
        int i = this.mUidStats.size() - 1;
        while (i >= 0) {
            Uid uid = this.mUidStats.valueAt(i);
            ArrayMap<String, ? extends BatteryStats.Uid.Wakelock> wakelockStats = uid.mWakelockStats.getMap();
            int j3 = wakelockStats.size() - 1;
            while (j3 >= 0) {
                String name = wakelockStats.keyAt(j3);
                Uid.Wakelock wakelock = wakelockStats.valueAt(j3);
                DualTimer timer = wakelock.mTimerPartial;
                if (timer != null) {
                    long totalTimeLockHeldMs = timer.getTotalTimeLocked(realtimeUs, 0) / j2;
                    if (totalTimeLockHeldMs != 0) {
                        int uid2 = uid.getUid();
                        int countLocked = timer.getCountLocked(0);
                        if (timer.isRunningLocked()) {
                            j = timer.getCurrentDurationMsLocked(realtimeMs);
                        } else {
                            j = 0;
                        }
                        uidWakeLockStats.add(new WakeLockStats.WakeLock(uid2, name, countLocked, totalTimeLockHeldMs, j));
                    }
                }
                j3--;
                j2 = 1000;
            }
            i--;
            j2 = 1000;
        }
        return new WakeLockStats(uidWakeLockStats);
    }

    @Override // android.os.BatteryStats
    public BluetoothBatteryStats getBluetoothBatteryStats() {
        long scanTimeMs;
        long j;
        int i;
        long rxTimeMs;
        long txTimeMs;
        long j2 = 1000;
        long elapsedRealtimeUs = this.mClock.elapsedRealtime() * 1000;
        ArrayList<BluetoothBatteryStats.UidStats> uidStats = new ArrayList<>();
        int i2 = this.mUidStats.size() - 1;
        while (i2 >= 0) {
            Uid uid = this.mUidStats.valueAt(i2);
            Timer scanTimer = uid.getBluetoothScanTimer();
            if (scanTimer != null) {
                scanTimeMs = scanTimer.getTotalTimeLocked(elapsedRealtimeUs, 0) / j2;
            } else {
                scanTimeMs = 0;
            }
            Timer unoptimizedScanTimer = uid.getBluetoothUnoptimizedScanTimer();
            if (unoptimizedScanTimer != null) {
                j = unoptimizedScanTimer.getTotalTimeLocked(elapsedRealtimeUs, 0) / j2;
            } else {
                j = 0;
            }
            long unoptimizedScanTimeMs = j;
            Counter scanResultCounter = uid.getBluetoothScanResultCounter();
            if (scanResultCounter != null) {
                i = scanResultCounter.getCountLocked(0);
            } else {
                i = 0;
            }
            int scanResultCount = i;
            BatteryStats.ControllerActivityCounter counter = uid.getBluetoothControllerActivity();
            if (counter != null) {
                rxTimeMs = counter.getRxTimeCounter().getCountLocked(0);
            } else {
                rxTimeMs = 0;
            }
            if (counter != null) {
                txTimeMs = counter.getTxTimeCounters()[0].getCountLocked(0);
            } else {
                txTimeMs = 0;
            }
            if (scanTimeMs != 0 || unoptimizedScanTimeMs != 0 || scanResultCount != 0 || rxTimeMs != 0 || txTimeMs != 0) {
                uidStats.add(new BluetoothBatteryStats.UidStats(uid.getUid(), scanTimeMs, unoptimizedScanTimeMs, scanResultCount, rxTimeMs, txTimeMs));
            }
            i2--;
            j2 = 1000;
        }
        return new BluetoothBatteryStats(uidStats);
    }

    @Override // android.os.BatteryStats
    public Map<String, ? extends Timer> getWakeupReasonStats() {
        HashMap<String, SamplingTimer> reasonstats;
        synchronized (mLock) {
            reasonstats = new HashMap<>(this.mWakeupReasonStats);
        }
        return reasonstats;
    }

    @Override // android.os.BatteryStats
    public long getUahDischarge(int which) {
        return this.mDischargeCounter.getCountLocked(which);
    }

    @Override // android.os.BatteryStats
    public long getUahDischargeScreenOff(int which) {
        return this.mDischargeScreenOffCounter.getCountLocked(which);
    }

    @Override // android.os.BatteryStats
    public long getUahDischargeScreenDoze(int which) {
        return this.mDischargeScreenDozeCounter.getCountLocked(which);
    }

    @Override // android.os.BatteryStats
    public long getUahDischargeLightDoze(int which) {
        return this.mDischargeLightDozeCounter.getCountLocked(which);
    }

    @Override // android.os.BatteryStats
    public long getUahDischargeDeepDoze(int which) {
        return this.mDischargeDeepDozeCounter.getCountLocked(which);
    }

    @Override // android.os.BatteryStats
    public int getEstimatedBatteryCapacity() {
        return this.mEstimatedBatteryCapacityMah;
    }

    @Override // android.os.BatteryStats
    public int getLearnedBatteryCapacity() {
        return this.mLastLearnedBatteryCapacityUah;
    }

    @Override // android.os.BatteryStats
    public int getMinLearnedBatteryCapacity() {
        return this.mMinLearnedBatteryCapacityUah;
    }

    @Override // android.os.BatteryStats
    public int getMaxLearnedBatteryCapacity() {
        return this.mMaxLearnedBatteryCapacityUah;
    }

    public BatteryStatsImpl() {
        this(Clock.SYSTEM_CLOCK);
    }

    public BatteryStatsImpl(Clock clock) {
        this(clock, (File) null);
    }

    public BatteryStatsImpl(Clock clock, File historyDirectory) {
        this.mKernelWakelockReader = new KernelWakelockReader();
        this.mTmpWakelockStats = new KernelWakelockStats();
        this.mSystemServerCpuThreadReader = SystemServerCpuThreadReader.create();
        this.mKernelMemoryBandwidthStats = new KernelMemoryBandwidthStats();
        this.mKernelMemoryStats = new LongSparseArray<>();
        this.mPerProcStateCpuTimesAvailable = true;
        this.mCpuTimeReadsTrackingStartTimeMs = SystemClock.uptimeMillis();
        this.mTmpRpmStats = null;
        this.mLastRpmStatsUpdateTimeMs = -1000L;
        this.mTmpRailStats = new RailStats();
        this.mPendingRemovedUids = new LinkedList();
        this.mDeferSetCharging = new Runnable() { // from class: com.android.internal.os.BatteryStatsImpl.2
            @Override // java.lang.Runnable
            public void run() {
                synchronized (BatteryStatsImpl.this) {
                    if (BatteryStatsImpl.this.mOnBattery) {
                        return;
                    }
                    boolean changed = BatteryStatsImpl.this.setChargingLocked(true);
                    if (changed) {
                        long uptimeMs = BatteryStatsImpl.this.mClock.uptimeMillis();
                        long elapsedRealtimeMs = BatteryStatsImpl.this.mClock.elapsedRealtime();
                        BatteryStatsImpl.this.addHistoryRecordLocked(elapsedRealtimeMs, uptimeMs);
                    }
                }
            }
        };
        this.mExternalSync = null;
        this.mUserInfoProvider = null;
        this.mIsolatedUids = new SparseIntArray();
        this.mIsolatedUidRefCounts = new SparseIntArray();
        this.mUidStats = new SparseArray<>();
        this.mPartialTimers = new ArrayList<>();
        this.mFullTimers = new ArrayList<>();
        this.mWindowTimers = new ArrayList<>();
        this.mDrawTimers = new ArrayList<>();
        this.mSensorTimers = new SparseArray<>();
        this.mWifiRunningTimers = new ArrayList<>();
        this.mFullWifiLockTimers = new ArrayList<>();
        this.mWifiMulticastTimers = new ArrayList<>();
        this.mWifiScanTimers = new ArrayList<>();
        this.mWifiBatchedScanTimers = new SparseArray<>();
        this.mAudioTurnedOnTimers = new ArrayList<>();
        this.mVideoTurnedOnTimers = new ArrayList<>();
        this.mFlashlightTurnedOnTimers = new ArrayList<>();
        this.mCameraTurnedOnTimers = new ArrayList<>();
        this.mBluetoothScanOnTimers = new ArrayList<>();
        this.mLastPartialTimers = new ArrayList<>();
        this.mOnBatteryTimeBase = new TimeBase(true);
        this.mOnBatteryScreenOffTimeBase = new TimeBase(true);
        this.mActiveEvents = new BatteryStats.HistoryEventTracker();
        this.mHaveBatteryLevel = false;
        this.mRecordingHistory = false;
        this.mHistoryTagPool = new HashMap<>();
        Parcel obtain = Parcel.obtain();
        this.mHistoryBuffer = obtain;
        this.mHistoryLastWritten = new BatteryStats.HistoryItem();
        this.mHistoryLastLastWritten = new BatteryStats.HistoryItem();
        this.mHistoryAddTmp = new BatteryStats.HistoryItem();
        this.mNextHistoryTagIdx = 0;
        this.mNumHistoryTagChars = 0;
        this.mHistoryBufferLastPos = -1;
        this.mActiveHistoryStates = -1;
        this.mActiveHistoryStates2 = -1;
        this.mLastHistoryElapsedRealtimeMs = 0L;
        this.mTrackRunningHistoryElapsedRealtimeMs = 0L;
        this.mTrackRunningHistoryUptimeMs = 0L;
        this.mHistoryCur = new BatteryStats.HistoryItem();
        this.mLastHistoryStepDetails = null;
        this.mLastHistoryStepLevel = (byte) 0;
        this.mCurHistoryStepDetails = new BatteryStats.HistoryStepDetails();
        this.mReadHistoryStepDetails = new BatteryStats.HistoryStepDetails();
        this.mTmpHistoryStepDetails = new BatteryStats.HistoryStepDetails();
        this.mIgnoreNextExternalStats = false;
        this.mScreenState = 0;
        this.mScreenBrightnessBin = -1;
        this.mScreenBrightnessTimer = new StopwatchTimer[5];
        this.mDisplayMismatchWtfCount = 0;
        this.mUsbDataState = 0;
        this.mGpsSignalQualityBin = -1;
        this.mGpsSignalQualityTimer = new StopwatchTimer[2];
        this.mPhoneSignalStrengthBin = -1;
        this.mPhoneSignalStrengthBinRaw = -1;
        this.mPhoneSignalStrengthsTimer = new StopwatchTimer[CellSignalStrength.getNumSignalStrengthLevels() + 1];
        this.mPhoneDataConnectionType = -1;
        this.mPhoneDataConnectionsTimer = new StopwatchTimer[NUM_DATA_CONNECTION_TYPES];
        this.mActiveRat = 0;
        this.mPerRatBatteryStats = new RadioAccessTechnologyBatteryStats[3];
        this.mNetworkByteActivityCounters = new LongSamplingCounter[10];
        this.mNetworkPacketActivityCounters = new LongSamplingCounter[10];
        this.mHasWifiReporting = false;
        this.mHasBluetoothReporting = false;
        this.mHasModemReporting = false;
        this.mWifiState = -1;
        this.mWifiStateTimer = new StopwatchTimer[8];
        this.mWifiSupplState = -1;
        this.mWifiSupplStateTimer = new StopwatchTimer[13];
        this.mWifiSignalStrengthBin = -1;
        this.mWifiSignalStrengthsTimer = new StopwatchTimer[5];
        this.mMobileRadioPowerState = 1;
        this.mWifiRadioPowerState = 1;
        this.mBluetoothPowerCalculator = null;
        this.mCpuPowerCalculator = null;
        this.mMobileRadioPowerCalculator = null;
        this.mWifiPowerCalculator = null;
        this.mCharging = true;
        this.mInitStepMode = 0;
        this.mCurStepMode = 0;
        this.mModStepMode = 0;
        this.mDischargeStepTracker = new BatteryStats.LevelStepTracker(200);
        this.mDailyDischargeStepTracker = new BatteryStats.LevelStepTracker(400);
        this.mChargeStepTracker = new BatteryStats.LevelStepTracker(200);
        this.mDailyChargeStepTracker = new BatteryStats.LevelStepTracker(400);
        this.mDailyStartTimeMs = 0L;
        this.mNextMinDailyDeadlineMs = 0L;
        this.mNextMaxDailyDeadlineMs = 0L;
        this.mDailyItems = new ArrayList<>();
        this.mLastWriteTimeMs = 0L;
        this.mPhoneServiceState = -1;
        this.mPhoneServiceStateRaw = -1;
        this.mPhoneSimStateRaw = -1;
        this.mBatteryVoltageMv = -1;
        this.mEstimatedBatteryCapacityMah = -1;
        this.mLastLearnedBatteryCapacityUah = -1;
        this.mMinLearnedBatteryCapacityUah = -1;
        this.mMaxLearnedBatteryCapacityUah = -1;
        this.mBatteryTimeToFullSeconds = -1L;
        this.mRpmStats = new HashMap<>();
        this.mScreenOffRpmStats = new HashMap<>();
        this.mKernelWakelockStats = new HashMap<>();
        this.mLastWakeupReason = null;
        this.mLastWakeupUptimeMs = 0L;
        this.mWakeupReasonStats = new HashMap<>();
        this.mChangedStates = 0;
        this.mChangedStates2 = 0;
        this.mInitialAcquireWakeUid = -1;
        this.mWifiFullLockNesting = 0;
        this.mWifiScanNesting = 0;
        this.mWifiMulticastNesting = 0;
        this.mWifiNetworkLock = new Object();
        this.mWifiIfaces = EmptyArray.STRING;
        this.mLastWifiNetworkStats = new NetworkStats(0L, -1);
        this.mModemNetworkLock = new Object();
        this.mModemIfaces = EmptyArray.STRING;
        this.mLastModemNetworkStats = new NetworkStats(0L, -1);
        this.mLastModemActivityInfo = null;
        this.mLastBluetoothActivityInfo = new BluetoothActivityInfoCache();
        this.mWriteLock = new ReentrantLock();
        init(clock);
        this.mStartClockTimeMs = clock.currentTimeMillis();
        this.mCheckinFile = null;
        this.mDailyFile = null;
        if (historyDirectory == null) {
            this.mStatsFile = null;
            this.mBatteryStatsHistory = new BatteryStatsHistory(obtain);
        } else {
            this.mStatsFile = new AtomicFile(new File(historyDirectory, "batterystats.bin"));
            this.mBatteryStatsHistory = new BatteryStatsHistory(this, historyDirectory, obtain);
        }
        this.mHandler = null;
        this.mPlatformIdleStateCallback = null;
        this.mMeasuredEnergyRetriever = null;
        this.mUserInfoProvider = null;
        this.mConstants = new Constants(null);
        clearHistoryLocked();
    }

    private void init(Clock clock) {
        this.mClock = clock;
        this.mCpuUidUserSysTimeReader = new KernelCpuUidTimeReader.KernelCpuUidUserSysTimeReader(true, clock);
        this.mCpuUidFreqTimeReader = new KernelCpuUidTimeReader.KernelCpuUidFreqTimeReader(true, clock);
        this.mCpuUidActiveTimeReader = new KernelCpuUidTimeReader.KernelCpuUidActiveTimeReader(true, clock);
        this.mCpuUidClusterTimeReader = new KernelCpuUidTimeReader.KernelCpuUidClusterTimeReader(true, clock);
    }

    /* loaded from: classes4.dex */
    public interface TimeBaseObs {
        void detach();

        void onTimeStarted(long j, long j2, long j3);

        void onTimeStopped(long j, long j2, long j3);

        boolean reset(boolean z, long j);

        default boolean reset(boolean detachIfReset) {
            return reset(detachIfReset, SystemClock.elapsedRealtime() * 1000);
        }
    }

    /* loaded from: classes4.dex */
    public static class TimeBase {
        protected final Collection<TimeBaseObs> mObservers;
        protected long mPastRealtimeUs;
        protected long mPastUptimeUs;
        protected long mRealtimeStartUs;
        protected long mRealtimeUs;
        protected boolean mRunning;
        protected long mUnpluggedRealtimeUs;
        protected long mUnpluggedUptimeUs;
        protected long mUptimeStartUs;
        protected long mUptimeUs;

        public void dump(PrintWriter pw, String prefix) {
            StringBuilder sb = new StringBuilder(128);
            pw.print(prefix);
            pw.print("mRunning=");
            pw.println(this.mRunning);
            sb.setLength(0);
            sb.append(prefix);
            sb.append("mUptime=");
            BatteryStats.formatTimeMs(sb, this.mUptimeUs / 1000);
            pw.println(sb.toString());
            sb.setLength(0);
            sb.append(prefix);
            sb.append("mRealtime=");
            BatteryStats.formatTimeMs(sb, this.mRealtimeUs / 1000);
            pw.println(sb.toString());
            sb.setLength(0);
            sb.append(prefix);
            sb.append("mPastUptime=");
            BatteryStats.formatTimeMs(sb, this.mPastUptimeUs / 1000);
            sb.append("mUptimeStart=");
            BatteryStats.formatTimeMs(sb, this.mUptimeStartUs / 1000);
            sb.append("mUnpluggedUptime=");
            BatteryStats.formatTimeMs(sb, this.mUnpluggedUptimeUs / 1000);
            pw.println(sb.toString());
            sb.setLength(0);
            sb.append(prefix);
            sb.append("mPastRealtime=");
            BatteryStats.formatTimeMs(sb, this.mPastRealtimeUs / 1000);
            sb.append("mRealtimeStart=");
            BatteryStats.formatTimeMs(sb, this.mRealtimeStartUs / 1000);
            sb.append("mUnpluggedRealtime=");
            BatteryStats.formatTimeMs(sb, this.mUnpluggedRealtimeUs / 1000);
            pw.println(sb.toString());
        }

        public TimeBase(boolean isLongList) {
            this.mObservers = isLongList ? new HashSet<>() : new ArrayList<>();
        }

        public TimeBase() {
            this(false);
        }

        public void add(TimeBaseObs observer) {
            this.mObservers.add(observer);
        }

        public void remove(TimeBaseObs observer) {
            this.mObservers.remove(observer);
        }

        public boolean hasObserver(TimeBaseObs observer) {
            return this.mObservers.contains(observer);
        }

        public void init(long uptimeUs, long elapsedRealtimeUs) {
            this.mRealtimeUs = 0L;
            this.mUptimeUs = 0L;
            this.mPastUptimeUs = 0L;
            this.mPastRealtimeUs = 0L;
            this.mUptimeStartUs = uptimeUs;
            this.mRealtimeStartUs = elapsedRealtimeUs;
            this.mUnpluggedUptimeUs = getUptime(uptimeUs);
            this.mUnpluggedRealtimeUs = getRealtime(this.mRealtimeStartUs);
        }

        public void reset(long uptimeUs, long elapsedRealtimeUs) {
            if (!this.mRunning) {
                this.mPastUptimeUs = 0L;
                this.mPastRealtimeUs = 0L;
                return;
            }
            this.mUptimeStartUs = uptimeUs;
            this.mRealtimeStartUs = elapsedRealtimeUs;
            this.mUnpluggedUptimeUs = getUptime(uptimeUs);
            this.mUnpluggedRealtimeUs = getRealtime(elapsedRealtimeUs);
        }

        public long computeUptime(long curTimeUs, int which) {
            return this.mUptimeUs + getUptime(curTimeUs);
        }

        public long computeRealtime(long curTimeUs, int which) {
            return this.mRealtimeUs + getRealtime(curTimeUs);
        }

        public long getUptime(long curTimeUs) {
            long time = this.mPastUptimeUs;
            if (this.mRunning) {
                return time + (curTimeUs - this.mUptimeStartUs);
            }
            return time;
        }

        public long getRealtime(long curTimeUs) {
            long time = this.mPastRealtimeUs;
            if (this.mRunning) {
                return time + (curTimeUs - this.mRealtimeStartUs);
            }
            return time;
        }

        public long getUptimeStart() {
            return this.mUptimeStartUs;
        }

        public long getRealtimeStart() {
            return this.mRealtimeStartUs;
        }

        public boolean isRunning() {
            return this.mRunning;
        }

        public boolean setRunning(boolean running, long uptimeUs, long elapsedRealtimeUs) {
            if (this.mRunning != running) {
                this.mRunning = running;
                if (!running) {
                    this.mPastUptimeUs += uptimeUs - this.mUptimeStartUs;
                    this.mPastRealtimeUs += elapsedRealtimeUs - this.mRealtimeStartUs;
                    long batteryUptimeUs = getUptime(uptimeUs);
                    long batteryRealtimeUs = getRealtime(elapsedRealtimeUs);
                    for (TimeBaseObs timeBaseObs : this.mObservers) {
                        timeBaseObs.onTimeStopped(elapsedRealtimeUs, batteryUptimeUs, batteryRealtimeUs);
                    }
                    return true;
                }
                this.mUptimeStartUs = uptimeUs;
                this.mRealtimeStartUs = elapsedRealtimeUs;
                long batteryUptimeUs2 = getUptime(uptimeUs);
                this.mUnpluggedUptimeUs = batteryUptimeUs2;
                long batteryRealtimeUs2 = getRealtime(elapsedRealtimeUs);
                this.mUnpluggedRealtimeUs = batteryRealtimeUs2;
                for (TimeBaseObs timeBaseObs2 : this.mObservers) {
                    timeBaseObs2.onTimeStarted(elapsedRealtimeUs, batteryUptimeUs2, batteryRealtimeUs2);
                }
                return true;
            }
            return false;
        }

        public void readSummaryFromParcel(Parcel in) {
            this.mUptimeUs = in.readLong();
            this.mRealtimeUs = in.readLong();
        }

        public void writeSummaryToParcel(Parcel out, long uptimeUs, long elapsedRealtimeUs) {
            out.writeLong(computeUptime(uptimeUs, 0));
            out.writeLong(computeRealtime(elapsedRealtimeUs, 0));
        }

        public void readFromParcel(Parcel in) {
            this.mRunning = false;
            this.mUptimeUs = in.readLong();
            this.mPastUptimeUs = in.readLong();
            this.mUptimeStartUs = in.readLong();
            this.mRealtimeUs = in.readLong();
            this.mPastRealtimeUs = in.readLong();
            this.mRealtimeStartUs = in.readLong();
            this.mUnpluggedUptimeUs = in.readLong();
            this.mUnpluggedRealtimeUs = in.readLong();
        }

        public void writeToParcel(Parcel out, long uptimeUs, long elapsedRealtimeUs) {
            long runningUptime = getUptime(uptimeUs);
            long runningRealtime = getRealtime(elapsedRealtimeUs);
            out.writeLong(this.mUptimeUs);
            out.writeLong(runningUptime);
            out.writeLong(this.mUptimeStartUs);
            out.writeLong(this.mRealtimeUs);
            out.writeLong(runningRealtime);
            out.writeLong(this.mRealtimeStartUs);
            out.writeLong(this.mUnpluggedUptimeUs);
            out.writeLong(this.mUnpluggedRealtimeUs);
        }
    }

    /* loaded from: classes4.dex */
    public static class Counter extends BatteryStats.Counter implements TimeBaseObs {
        final AtomicInteger mCount;
        final TimeBase mTimeBase;

        public Counter(TimeBase timeBase, Parcel in) {
            AtomicInteger atomicInteger = new AtomicInteger();
            this.mCount = atomicInteger;
            this.mTimeBase = timeBase;
            atomicInteger.set(in.readInt());
            timeBase.add(this);
        }

        public Counter(TimeBase timeBase) {
            this.mCount = new AtomicInteger();
            this.mTimeBase = timeBase;
            timeBase.add(this);
        }

        public void writeToParcel(Parcel out) {
            out.writeInt(this.mCount.get());
        }

        @Override // com.android.internal.os.BatteryStatsImpl.TimeBaseObs
        public void onTimeStarted(long elapsedRealtimeUs, long baseUptimeUs, long baseRealtimeUs) {
        }

        @Override // com.android.internal.os.BatteryStatsImpl.TimeBaseObs
        public void onTimeStopped(long elapsedRealtimeUs, long baseUptimeUs, long baseRealtimeUs) {
        }

        public static void writeCounterToParcel(Parcel out, Counter counter) {
            if (counter == null) {
                out.writeInt(0);
                return;
            }
            out.writeInt(1);
            counter.writeToParcel(out);
        }

        public static Counter readCounterFromParcel(TimeBase timeBase, Parcel in) {
            if (in.readInt() == 0) {
                return null;
            }
            return new Counter(timeBase, in);
        }

        @Override // android.os.BatteryStats.Counter
        public int getCountLocked(int which) {
            return this.mCount.get();
        }

        @Override // android.os.BatteryStats.Counter
        public void logState(Printer pw, String prefix) {
            pw.println(prefix + "mCount=" + this.mCount.get());
        }

        public void stepAtomic() {
            if (this.mTimeBase.isRunning()) {
                this.mCount.incrementAndGet();
            }
        }

        void addAtomic(int delta) {
            if (this.mTimeBase.isRunning()) {
                this.mCount.addAndGet(delta);
            }
        }

        @Override // com.android.internal.os.BatteryStatsImpl.TimeBaseObs
        public boolean reset(boolean detachIfReset, long elapsedRealtimeUs) {
            this.mCount.set(0);
            if (detachIfReset) {
                detach();
                return true;
            }
            return true;
        }

        @Override // com.android.internal.os.BatteryStatsImpl.TimeBaseObs
        public void detach() {
            this.mTimeBase.remove(this);
        }

        public void writeSummaryFromParcelLocked(Parcel out) {
            out.writeInt(this.mCount.get());
        }

        public void readSummaryFromParcelLocked(Parcel in) {
            this.mCount.set(in.readInt());
        }
    }

    /* loaded from: classes4.dex */
    public static class LongSamplingCounterArray extends BatteryStats.LongCounterArray implements TimeBaseObs {
        public long[] mCounts;
        final TimeBase mTimeBase;

        private LongSamplingCounterArray(TimeBase timeBase, Parcel in) {
            this.mTimeBase = timeBase;
            this.mCounts = in.createLongArray();
            timeBase.add(this);
        }

        public LongSamplingCounterArray(TimeBase timeBase) {
            this.mTimeBase = timeBase;
            timeBase.add(this);
        }

        /* JADX INFO: Access modifiers changed from: private */
        public void writeToParcel(Parcel out) {
            out.writeLongArray(this.mCounts);
        }

        @Override // com.android.internal.os.BatteryStatsImpl.TimeBaseObs
        public void onTimeStarted(long elapsedRealTimeUs, long baseUptimeUs, long baseRealtimeUs) {
        }

        @Override // com.android.internal.os.BatteryStatsImpl.TimeBaseObs
        public void onTimeStopped(long elapsedRealtimeUs, long baseUptimeUs, long baseRealtimeUs) {
        }

        @Override // android.os.BatteryStats.LongCounterArray
        public long[] getCountsLocked(int which) {
            long[] jArr = this.mCounts;
            if (jArr == null) {
                return null;
            }
            return Arrays.copyOf(jArr, jArr.length);
        }

        @Override // android.os.BatteryStats.LongCounterArray
        public void logState(Printer pw, String prefix) {
            pw.println(prefix + "mCounts=" + Arrays.toString(this.mCounts));
        }

        public void addCountLocked(long[] counts) {
            addCountLocked(counts, this.mTimeBase.isRunning());
        }

        public void addCountLocked(long[] counts, boolean isRunning) {
            if (counts != null && isRunning) {
                if (this.mCounts == null) {
                    this.mCounts = new long[counts.length];
                }
                for (int i = 0; i < counts.length; i++) {
                    long[] jArr = this.mCounts;
                    jArr[i] = jArr[i] + counts[i];
                }
            }
        }

        public int getSize() {
            long[] jArr = this.mCounts;
            if (jArr == null) {
                return 0;
            }
            return jArr.length;
        }

        @Override // com.android.internal.os.BatteryStatsImpl.TimeBaseObs
        public boolean reset(boolean detachIfReset, long elapsedRealtimeUs) {
            long[] jArr = this.mCounts;
            if (jArr != null) {
                Arrays.fill(jArr, 0L);
            }
            if (detachIfReset) {
                detach();
                return true;
            }
            return true;
        }

        @Override // com.android.internal.os.BatteryStatsImpl.TimeBaseObs
        public void detach() {
            this.mTimeBase.remove(this);
        }

        /* JADX INFO: Access modifiers changed from: private */
        public void writeSummaryToParcelLocked(Parcel out) {
            out.writeLongArray(this.mCounts);
        }

        /* JADX INFO: Access modifiers changed from: private */
        public void readSummaryFromParcelLocked(Parcel in) {
            this.mCounts = in.createLongArray();
        }

        public static void writeToParcel(Parcel out, LongSamplingCounterArray counterArray) {
            if (counterArray != null) {
                out.writeInt(1);
                counterArray.writeToParcel(out);
                return;
            }
            out.writeInt(0);
        }

        public static LongSamplingCounterArray readFromParcel(Parcel in, TimeBase timeBase) {
            if (in.readInt() != 0) {
                return new LongSamplingCounterArray(timeBase, in);
            }
            return null;
        }

        public static void writeSummaryToParcelLocked(Parcel out, LongSamplingCounterArray counterArray) {
            if (counterArray != null) {
                out.writeInt(1);
                counterArray.writeSummaryToParcelLocked(out);
                return;
            }
            out.writeInt(0);
        }

        public static LongSamplingCounterArray readSummaryFromParcelLocked(Parcel in, TimeBase timeBase) {
            if (in.readInt() != 0) {
                LongSamplingCounterArray counterArray = new LongSamplingCounterArray(timeBase);
                counterArray.readSummaryFromParcelLocked(in);
                return counterArray;
            }
            return null;
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes4.dex */
    public static class TimeMultiStateCounter extends BatteryStats.LongCounter implements TimeBaseObs {
        private final LongMultiStateCounter mCounter;
        private final TimeBase mTimeBase;

        private TimeMultiStateCounter(TimeBase timeBase, int stateCount, long timestampMs) {
            this(timeBase, new LongMultiStateCounter(stateCount), timestampMs);
        }

        private TimeMultiStateCounter(TimeBase timeBase, LongMultiStateCounter counter, long timestampMs) {
            this.mTimeBase = timeBase;
            this.mCounter = counter;
            counter.setEnabled(timeBase.isRunning(), timestampMs);
            timeBase.add(this);
        }

        /* JADX INFO: Access modifiers changed from: private */
        public static TimeMultiStateCounter readFromParcel(Parcel in, TimeBase timeBase, int stateCount, long timestampMs) {
            LongMultiStateCounter counter = LongMultiStateCounter.CREATOR.createFromParcel(in);
            if (counter.getStateCount() != stateCount) {
                return null;
            }
            return new TimeMultiStateCounter(timeBase, counter, timestampMs);
        }

        /* JADX INFO: Access modifiers changed from: private */
        public void writeToParcel(Parcel out) {
            this.mCounter.writeToParcel(out, 0);
        }

        @Override // com.android.internal.os.BatteryStatsImpl.TimeBaseObs
        public void onTimeStarted(long elapsedRealtimeUs, long baseUptimeUs, long baseRealtimeUs) {
            this.mCounter.setEnabled(true, elapsedRealtimeUs / 1000);
        }

        @Override // com.android.internal.os.BatteryStatsImpl.TimeBaseObs
        public void onTimeStopped(long elapsedRealtimeUs, long baseUptimeUs, long baseRealtimeUs) {
            this.mCounter.setEnabled(false, elapsedRealtimeUs / 1000);
        }

        public int getStateCount() {
            return this.mCounter.getStateCount();
        }

        /* JADX INFO: Access modifiers changed from: private */
        public void setState(int processState, long elapsedRealtimeMs) {
            this.mCounter.setState(processState, elapsedRealtimeMs);
        }

        /* JADX INFO: Access modifiers changed from: private */
        public long update(long value, long timestampMs) {
            return this.mCounter.updateValue(value, timestampMs);
        }

        /* JADX INFO: Access modifiers changed from: private */
        public void increment(long increment, long timestampMs) {
            this.mCounter.incrementValue(increment, timestampMs);
        }

        @Override // android.os.BatteryStats.LongCounter
        public long getCountForProcessState(int procState) {
            return this.mCounter.getCount(procState);
        }

        public long getTotalCountLocked() {
            return this.mCounter.getTotalCount();
        }

        @Override // android.os.BatteryStats.LongCounter
        public long getCountLocked(int statsType) {
            return getTotalCountLocked();
        }

        @Override // android.os.BatteryStats.LongCounter
        public void logState(Printer pw, String prefix) {
            pw.println(prefix + "mCounter=" + this.mCounter);
        }

        @Override // com.android.internal.os.BatteryStatsImpl.TimeBaseObs
        public boolean reset(boolean detachIfReset, long elapsedRealtimeUs) {
            this.mCounter.reset();
            if (detachIfReset) {
                detach();
                return true;
            }
            return true;
        }

        @Override // com.android.internal.os.BatteryStatsImpl.TimeBaseObs
        public void detach() {
            this.mTimeBase.remove(this);
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes4.dex */
    public static class TimeInFreqMultiStateCounter implements TimeBaseObs {
        private final LongArrayMultiStateCounter mCounter;
        private final TimeBase mTimeBase;

        private TimeInFreqMultiStateCounter(TimeBase timeBase, int stateCount, int cpuFreqCount, long timestampMs) {
            this(timeBase, new LongArrayMultiStateCounter(stateCount, cpuFreqCount), timestampMs);
        }

        private TimeInFreqMultiStateCounter(TimeBase timeBase, LongArrayMultiStateCounter counter, long timestampMs) {
            this.mTimeBase = timeBase;
            this.mCounter = counter;
            counter.setEnabled(timeBase.isRunning(), timestampMs);
            timeBase.add(this);
        }

        /* JADX INFO: Access modifiers changed from: private */
        public void writeToParcel(Parcel out) {
            this.mCounter.writeToParcel(out, 0);
        }

        /* JADX INFO: Access modifiers changed from: private */
        public static TimeInFreqMultiStateCounter readFromParcel(Parcel in, TimeBase timeBase, int stateCount, int cpuFreqCount, long timestampMs) {
            LongArrayMultiStateCounter counter = LongArrayMultiStateCounter.CREATOR.createFromParcel(in);
            if (counter.getStateCount() != stateCount || counter.getArrayLength() != cpuFreqCount) {
                return null;
            }
            return new TimeInFreqMultiStateCounter(timeBase, counter, timestampMs);
        }

        @Override // com.android.internal.os.BatteryStatsImpl.TimeBaseObs
        public void onTimeStarted(long elapsedRealtimeUs, long baseUptimeUs, long baseRealtimeUs) {
            this.mCounter.setEnabled(true, elapsedRealtimeUs / 1000);
        }

        @Override // com.android.internal.os.BatteryStatsImpl.TimeBaseObs
        public void onTimeStopped(long elapsedRealtimeUs, long baseUptimeUs, long baseRealtimeUs) {
            this.mCounter.setEnabled(false, elapsedRealtimeUs / 1000);
        }

        public LongArrayMultiStateCounter getCounter() {
            return this.mCounter;
        }

        public int getStateCount() {
            return this.mCounter.getStateCount();
        }

        public void setTrackingEnabled(boolean enabled, long timestampMs) {
            this.mCounter.setEnabled(enabled && this.mTimeBase.isRunning(), timestampMs);
        }

        /* JADX INFO: Access modifiers changed from: private */
        public void setState(int uidRunningState, long elapsedRealtimeMs) {
            this.mCounter.setState(uidRunningState, elapsedRealtimeMs);
        }

        public boolean getCountsLocked(long[] counts, int procState) {
            if (counts.length != this.mCounter.getArrayLength()) {
                return false;
            }
            this.mCounter.getCounts(counts, procState);
            for (int i = counts.length - 1; i >= 0; i--) {
                if (counts[i] != 0) {
                    return true;
                }
            }
            return false;
        }

        public void logState(Printer pw, String prefix) {
            pw.println(prefix + "mCounter=" + this.mCounter);
        }

        @Override // com.android.internal.os.BatteryStatsImpl.TimeBaseObs
        public boolean reset(boolean detachIfReset, long elapsedRealtimeUs) {
            this.mCounter.reset();
            if (detachIfReset) {
                detach();
                return true;
            }
            return true;
        }

        @Override // com.android.internal.os.BatteryStatsImpl.TimeBaseObs
        public void detach() {
            this.mTimeBase.remove(this);
        }
    }

    /* loaded from: classes4.dex */
    public static class LongSamplingCounter extends BatteryStats.LongCounter implements TimeBaseObs {
        private long mCount;
        final TimeBase mTimeBase;

        public LongSamplingCounter(TimeBase timeBase, Parcel in) {
            this.mTimeBase = timeBase;
            this.mCount = in.readLong();
            timeBase.add(this);
        }

        public LongSamplingCounter(TimeBase timeBase) {
            this.mTimeBase = timeBase;
            timeBase.add(this);
        }

        public void writeToParcel(Parcel out) {
            out.writeLong(this.mCount);
        }

        @Override // com.android.internal.os.BatteryStatsImpl.TimeBaseObs
        public void onTimeStarted(long elapsedRealtimeUs, long baseUptimeUs, long baseRealtimeUs) {
        }

        @Override // com.android.internal.os.BatteryStatsImpl.TimeBaseObs
        public void onTimeStopped(long elapsedRealtimeUs, long baseUptimeUs, long baseRealtimeUs) {
        }

        @Override // android.os.BatteryStats.LongCounter
        public long getCountLocked(int which) {
            return this.mCount;
        }

        @Override // android.os.BatteryStats.LongCounter
        public long getCountForProcessState(int procState) {
            if (procState == 0) {
                return getCountLocked(0);
            }
            return 0L;
        }

        @Override // android.os.BatteryStats.LongCounter
        public void logState(Printer pw, String prefix) {
            pw.println(prefix + "mCount=" + this.mCount);
        }

        public void addCountLocked(long count) {
            addCountLocked(count, this.mTimeBase.isRunning());
        }

        public void addCountLocked(long count, boolean isRunning) {
            if (isRunning) {
                this.mCount += count;
            }
        }

        @Override // com.android.internal.os.BatteryStatsImpl.TimeBaseObs
        public boolean reset(boolean detachIfReset, long elapsedRealtimeUs) {
            this.mCount = 0L;
            if (detachIfReset) {
                detach();
                return true;
            }
            return true;
        }

        @Override // com.android.internal.os.BatteryStatsImpl.TimeBaseObs
        public void detach() {
            this.mTimeBase.remove(this);
        }

        public void writeSummaryFromParcelLocked(Parcel out) {
            out.writeLong(this.mCount);
        }

        public void readSummaryFromParcelLocked(Parcel in) {
            this.mCount = in.readLong();
        }
    }

    /* loaded from: classes4.dex */
    public static abstract class Timer extends BatteryStats.Timer implements TimeBaseObs {
        protected final Clock mClock;
        protected int mCount;
        protected final TimeBase mTimeBase;
        protected long mTimeBeforeMarkUs;
        protected long mTotalTimeUs;
        protected final int mType;

        protected abstract int computeCurrentCountLocked();

        protected abstract long computeRunTimeLocked(long j, long j2);

        public Timer(Clock clock, int type, TimeBase timeBase, Parcel in) {
            this.mClock = clock;
            this.mType = type;
            this.mTimeBase = timeBase;
            this.mCount = in.readInt();
            this.mTotalTimeUs = in.readLong();
            this.mTimeBeforeMarkUs = in.readLong();
            timeBase.add(this);
        }

        public Timer(Clock clock, int type, TimeBase timeBase) {
            this.mClock = clock;
            this.mType = type;
            this.mTimeBase = timeBase;
            timeBase.add(this);
        }

        public void writeToParcel(Parcel out, long elapsedRealtimeUs) {
            out.writeInt(computeCurrentCountLocked());
            out.writeLong(computeRunTimeLocked(this.mTimeBase.getRealtime(elapsedRealtimeUs), elapsedRealtimeUs));
            out.writeLong(this.mTimeBeforeMarkUs);
        }

        @Override // com.android.internal.os.BatteryStatsImpl.TimeBaseObs
        public boolean reset(boolean detachIfReset) {
            return reset(detachIfReset, this.mClock.elapsedRealtime() * 1000);
        }

        public boolean reset(boolean detachIfReset, long elapsedRealtimeUs) {
            this.mTimeBeforeMarkUs = 0L;
            this.mTotalTimeUs = 0L;
            this.mCount = 0;
            if (detachIfReset) {
                detach();
                return true;
            }
            return true;
        }

        public void detach() {
            this.mTimeBase.remove(this);
        }

        public void onTimeStarted(long elapsedRealtimeUs, long timeBaseUptimeUs, long baseRealtimeUs) {
        }

        public void onTimeStopped(long elapsedRealtimeUs, long baseUptimeUs, long baseRealtimeUs) {
            this.mTotalTimeUs = computeRunTimeLocked(baseRealtimeUs, elapsedRealtimeUs);
            this.mCount = computeCurrentCountLocked();
        }

        public static void writeTimerToParcel(Parcel out, Timer timer, long elapsedRealtimeUs) {
            if (timer == null) {
                out.writeInt(0);
                return;
            }
            out.writeInt(1);
            timer.writeToParcel(out, elapsedRealtimeUs);
        }

        @Override // android.os.BatteryStats.Timer
        public long getTotalTimeLocked(long elapsedRealtimeUs, int which) {
            return computeRunTimeLocked(this.mTimeBase.getRealtime(elapsedRealtimeUs), elapsedRealtimeUs);
        }

        @Override // android.os.BatteryStats.Timer
        public int getCountLocked(int which) {
            return computeCurrentCountLocked();
        }

        @Override // android.os.BatteryStats.Timer
        public long getTimeSinceMarkLocked(long elapsedRealtimeUs) {
            long val = computeRunTimeLocked(this.mTimeBase.getRealtime(elapsedRealtimeUs), elapsedRealtimeUs);
            return val - this.mTimeBeforeMarkUs;
        }

        @Override // android.os.BatteryStats.Timer
        public void logState(Printer pw, String prefix) {
            pw.println(prefix + "mCount=" + this.mCount);
            pw.println(prefix + "mTotalTime=" + this.mTotalTimeUs);
        }

        public void writeSummaryFromParcelLocked(Parcel out, long elapsedRealtimeUs) {
            long runTimeUs = computeRunTimeLocked(this.mTimeBase.getRealtime(elapsedRealtimeUs), elapsedRealtimeUs);
            out.writeLong(runTimeUs);
            out.writeInt(computeCurrentCountLocked());
        }

        public void readSummaryFromParcelLocked(Parcel in) {
            this.mTotalTimeUs = in.readLong();
            this.mCount = in.readInt();
            this.mTimeBeforeMarkUs = this.mTotalTimeUs;
        }
    }

    /* loaded from: classes4.dex */
    public static class SamplingTimer extends Timer {
        int mCurrentReportedCount;
        long mCurrentReportedTotalTimeUs;
        boolean mTimeBaseRunning;
        boolean mTrackingReportedValues;
        int mUnpluggedReportedCount;
        long mUnpluggedReportedTotalTimeUs;
        int mUpdateVersion;

        public SamplingTimer(Clock clock, TimeBase timeBase, Parcel in) {
            super(clock, 0, timeBase, in);
            this.mCurrentReportedCount = in.readInt();
            this.mUnpluggedReportedCount = in.readInt();
            this.mCurrentReportedTotalTimeUs = in.readLong();
            this.mUnpluggedReportedTotalTimeUs = in.readLong();
            this.mTrackingReportedValues = in.readInt() == 1;
            this.mTimeBaseRunning = timeBase.isRunning();
        }

        public SamplingTimer(Clock clock, TimeBase timeBase) {
            super(clock, 0, timeBase);
            this.mTrackingReportedValues = false;
            this.mTimeBaseRunning = timeBase.isRunning();
        }

        public void endSample() {
            endSample(this.mClock.elapsedRealtime() * 1000);
        }

        public void endSample(long elapsedRealtimeUs) {
            this.mTotalTimeUs = computeRunTimeLocked(0L, elapsedRealtimeUs);
            this.mCount = computeCurrentCountLocked();
            this.mCurrentReportedTotalTimeUs = 0L;
            this.mUnpluggedReportedTotalTimeUs = 0L;
            this.mCurrentReportedCount = 0;
            this.mUnpluggedReportedCount = 0;
            this.mTrackingReportedValues = false;
        }

        public void setUpdateVersion(int version) {
            this.mUpdateVersion = version;
        }

        public int getUpdateVersion() {
            return this.mUpdateVersion;
        }

        public void updated(long totalTimeUs, int count) {
            update(totalTimeUs, count, this.mClock.elapsedRealtime() * 1000);
        }

        public void update(long totalTimeUs, int count, long elapsedRealtimeUs) {
            if (this.mTimeBaseRunning && !this.mTrackingReportedValues) {
                this.mUnpluggedReportedTotalTimeUs = totalTimeUs;
                this.mUnpluggedReportedCount = count;
            }
            this.mTrackingReportedValues = true;
            if (totalTimeUs < this.mCurrentReportedTotalTimeUs || count < this.mCurrentReportedCount) {
                endSample(elapsedRealtimeUs);
            }
            this.mCurrentReportedTotalTimeUs = totalTimeUs;
            this.mCurrentReportedCount = count;
        }

        public void add(long deltaTimeUs, int deltaCount) {
            add(deltaTimeUs, deltaCount, this.mClock.elapsedRealtime() * 1000);
        }

        public void add(long deltaTimeUs, int deltaCount, long elapsedRealtimeUs) {
            update(this.mCurrentReportedTotalTimeUs + deltaTimeUs, this.mCurrentReportedCount + deltaCount, elapsedRealtimeUs);
        }

        @Override // com.android.internal.os.BatteryStatsImpl.Timer, com.android.internal.os.BatteryStatsImpl.TimeBaseObs
        public void onTimeStarted(long elapsedRealtimeUs, long baseUptimeUs, long baseRealtimeUs) {
            super.onTimeStarted(elapsedRealtimeUs, baseUptimeUs, baseRealtimeUs);
            if (this.mTrackingReportedValues) {
                this.mUnpluggedReportedTotalTimeUs = this.mCurrentReportedTotalTimeUs;
                this.mUnpluggedReportedCount = this.mCurrentReportedCount;
            }
            this.mTimeBaseRunning = true;
        }

        @Override // com.android.internal.os.BatteryStatsImpl.Timer, com.android.internal.os.BatteryStatsImpl.TimeBaseObs
        public void onTimeStopped(long elapsedRealtimeUs, long baseUptimeUs, long baseRealtimeUs) {
            super.onTimeStopped(elapsedRealtimeUs, baseUptimeUs, baseRealtimeUs);
            this.mTimeBaseRunning = false;
        }

        @Override // com.android.internal.os.BatteryStatsImpl.Timer, android.os.BatteryStats.Timer
        public void logState(Printer pw, String prefix) {
            super.logState(pw, prefix);
            pw.println(prefix + "mCurrentReportedCount=" + this.mCurrentReportedCount + " mUnpluggedReportedCount=" + this.mUnpluggedReportedCount + " mCurrentReportedTotalTime=" + this.mCurrentReportedTotalTimeUs + " mUnpluggedReportedTotalTime=" + this.mUnpluggedReportedTotalTimeUs);
        }

        @Override // com.android.internal.os.BatteryStatsImpl.Timer
        protected long computeRunTimeLocked(long curBatteryRealtime, long elapsedRealtimeUs) {
            return this.mTotalTimeUs + ((this.mTimeBaseRunning && this.mTrackingReportedValues) ? this.mCurrentReportedTotalTimeUs - this.mUnpluggedReportedTotalTimeUs : 0L);
        }

        @Override // com.android.internal.os.BatteryStatsImpl.Timer
        protected int computeCurrentCountLocked() {
            return this.mCount + ((this.mTimeBaseRunning && this.mTrackingReportedValues) ? this.mCurrentReportedCount - this.mUnpluggedReportedCount : 0);
        }

        @Override // com.android.internal.os.BatteryStatsImpl.Timer
        public void writeToParcel(Parcel out, long elapsedRealtimeUs) {
            super.writeToParcel(out, elapsedRealtimeUs);
            out.writeInt(this.mCurrentReportedCount);
            out.writeInt(this.mUnpluggedReportedCount);
            out.writeLong(this.mCurrentReportedTotalTimeUs);
            out.writeLong(this.mUnpluggedReportedTotalTimeUs);
            out.writeInt(this.mTrackingReportedValues ? 1 : 0);
        }

        @Override // com.android.internal.os.BatteryStatsImpl.Timer, com.android.internal.os.BatteryStatsImpl.TimeBaseObs
        public boolean reset(boolean detachIfReset, long elapsedRealtimeUs) {
            super.reset(detachIfReset, elapsedRealtimeUs);
            this.mTrackingReportedValues = false;
            this.mUnpluggedReportedTotalTimeUs = 0L;
            this.mUnpluggedReportedCount = 0;
            return true;
        }
    }

    /* loaded from: classes4.dex */
    public static class BatchTimer extends Timer {
        boolean mInDischarge;
        long mLastAddedDurationUs;
        long mLastAddedTimeUs;
        final Uid mUid;

        BatchTimer(Clock clock, Uid uid, int type, TimeBase timeBase, Parcel in) {
            super(clock, type, timeBase, in);
            this.mUid = uid;
            this.mLastAddedTimeUs = in.readLong();
            this.mLastAddedDurationUs = in.readLong();
            this.mInDischarge = timeBase.isRunning();
        }

        BatchTimer(Clock clock, Uid uid, int type, TimeBase timeBase) {
            super(clock, type, timeBase);
            this.mUid = uid;
            this.mInDischarge = timeBase.isRunning();
        }

        @Override // com.android.internal.os.BatteryStatsImpl.Timer
        public void writeToParcel(Parcel out, long elapsedRealtimeUs) {
            super.writeToParcel(out, elapsedRealtimeUs);
            out.writeLong(this.mLastAddedTimeUs);
            out.writeLong(this.mLastAddedDurationUs);
        }

        @Override // com.android.internal.os.BatteryStatsImpl.Timer, com.android.internal.os.BatteryStatsImpl.TimeBaseObs
        public void onTimeStopped(long elapsedRealtimeUs, long baseUptimeUs, long baseRealtimeUs) {
            recomputeLastDuration(elapsedRealtimeUs, false);
            this.mInDischarge = false;
            super.onTimeStopped(elapsedRealtimeUs, baseUptimeUs, baseRealtimeUs);
        }

        @Override // com.android.internal.os.BatteryStatsImpl.Timer, com.android.internal.os.BatteryStatsImpl.TimeBaseObs
        public void onTimeStarted(long elapsedRealtimeUs, long baseUptimeUs, long baseRealtimeUs) {
            recomputeLastDuration(elapsedRealtimeUs, false);
            this.mInDischarge = true;
            if (this.mLastAddedTimeUs == elapsedRealtimeUs) {
                this.mTotalTimeUs += this.mLastAddedDurationUs;
            }
            super.onTimeStarted(elapsedRealtimeUs, baseUptimeUs, baseRealtimeUs);
        }

        @Override // com.android.internal.os.BatteryStatsImpl.Timer, android.os.BatteryStats.Timer
        public void logState(Printer pw, String prefix) {
            super.logState(pw, prefix);
            pw.println(prefix + "mLastAddedTime=" + this.mLastAddedTimeUs + " mLastAddedDuration=" + this.mLastAddedDurationUs);
        }

        private long computeOverage(long curTimeUs) {
            if (this.mLastAddedTimeUs > 0) {
                return this.mLastAddedDurationUs - curTimeUs;
            }
            return 0L;
        }

        private void recomputeLastDuration(long curTimeUs, boolean abort) {
            long overage = computeOverage(curTimeUs);
            if (overage > 0) {
                if (this.mInDischarge) {
                    this.mTotalTimeUs -= overage;
                }
                if (abort) {
                    this.mLastAddedTimeUs = 0L;
                    return;
                }
                this.mLastAddedTimeUs = curTimeUs;
                this.mLastAddedDurationUs -= overage;
            }
        }

        public void addDuration(BatteryStatsImpl stats, long durationMs) {
            addDuration(stats, durationMs, this.mClock.elapsedRealtime());
        }

        public void addDuration(BatteryStatsImpl stats, long durationMs, long elapsedRealtimeMs) {
            long nowUs = elapsedRealtimeMs * 1000;
            recomputeLastDuration(nowUs, true);
            this.mLastAddedTimeUs = nowUs;
            this.mLastAddedDurationUs = 1000 * durationMs;
            if (this.mInDischarge) {
                this.mTotalTimeUs += this.mLastAddedDurationUs;
                this.mCount++;
            }
        }

        public void abortLastDuration(BatteryStatsImpl stats) {
            abortLastDuration(stats, this.mClock.elapsedRealtime());
        }

        public void abortLastDuration(BatteryStatsImpl stats, long elapsedRealtimeMs) {
            long nowUs = 1000 * elapsedRealtimeMs;
            recomputeLastDuration(nowUs, true);
        }

        @Override // com.android.internal.os.BatteryStatsImpl.Timer
        protected int computeCurrentCountLocked() {
            return this.mCount;
        }

        @Override // com.android.internal.os.BatteryStatsImpl.Timer
        protected long computeRunTimeLocked(long curBatteryRealtimeUs, long elapsedRealtimeUs) {
            long overage = computeOverage(elapsedRealtimeUs);
            if (overage > 0) {
                this.mTotalTimeUs = overage;
                return overage;
            }
            return this.mTotalTimeUs;
        }

        @Override // com.android.internal.os.BatteryStatsImpl.Timer, com.android.internal.os.BatteryStatsImpl.TimeBaseObs
        public boolean reset(boolean detachIfReset, long elapsedRealtimeUs) {
            recomputeLastDuration(elapsedRealtimeUs, true);
            boolean stillActive = this.mLastAddedTimeUs == elapsedRealtimeUs;
            super.reset(!stillActive && detachIfReset, elapsedRealtimeUs);
            return !stillActive;
        }
    }

    /* loaded from: classes4.dex */
    public static class DurationTimer extends StopwatchTimer {
        long mCurrentDurationMs;
        long mMaxDurationMs;
        long mStartTimeMs;
        long mTotalDurationMs;

        public DurationTimer(Clock clock, Uid uid, int type, ArrayList<StopwatchTimer> timerPool, TimeBase timeBase, Parcel in) {
            super(clock, uid, type, timerPool, timeBase, in);
            this.mStartTimeMs = -1L;
            this.mMaxDurationMs = in.readLong();
            this.mTotalDurationMs = in.readLong();
            this.mCurrentDurationMs = in.readLong();
        }

        public DurationTimer(Clock clock, Uid uid, int type, ArrayList<StopwatchTimer> timerPool, TimeBase timeBase) {
            super(clock, uid, type, timerPool, timeBase);
            this.mStartTimeMs = -1L;
        }

        @Override // com.android.internal.os.BatteryStatsImpl.StopwatchTimer, com.android.internal.os.BatteryStatsImpl.Timer
        public void writeToParcel(Parcel out, long elapsedRealtimeUs) {
            super.writeToParcel(out, elapsedRealtimeUs);
            out.writeLong(getMaxDurationMsLocked(elapsedRealtimeUs / 1000));
            out.writeLong(this.mTotalDurationMs);
            out.writeLong(getCurrentDurationMsLocked(elapsedRealtimeUs / 1000));
        }

        @Override // com.android.internal.os.BatteryStatsImpl.Timer
        public void writeSummaryFromParcelLocked(Parcel out, long elapsedRealtimeUs) {
            super.writeSummaryFromParcelLocked(out, elapsedRealtimeUs);
            out.writeLong(getMaxDurationMsLocked(elapsedRealtimeUs / 1000));
            out.writeLong(getTotalDurationMsLocked(elapsedRealtimeUs / 1000));
        }

        @Override // com.android.internal.os.BatteryStatsImpl.StopwatchTimer, com.android.internal.os.BatteryStatsImpl.Timer
        public void readSummaryFromParcelLocked(Parcel in) {
            super.readSummaryFromParcelLocked(in);
            this.mMaxDurationMs = in.readLong();
            this.mTotalDurationMs = in.readLong();
            this.mStartTimeMs = -1L;
            this.mCurrentDurationMs = 0L;
        }

        @Override // com.android.internal.os.BatteryStatsImpl.Timer, com.android.internal.os.BatteryStatsImpl.TimeBaseObs
        public void onTimeStarted(long elapsedRealtimeUs, long baseUptimeUs, long baseRealtimeUs) {
            super.onTimeStarted(elapsedRealtimeUs, baseUptimeUs, baseRealtimeUs);
            if (this.mNesting > 0) {
                this.mStartTimeMs = baseRealtimeUs / 1000;
            }
        }

        @Override // com.android.internal.os.BatteryStatsImpl.StopwatchTimer, com.android.internal.os.BatteryStatsImpl.Timer, com.android.internal.os.BatteryStatsImpl.TimeBaseObs
        public void onTimeStopped(long elapsedRealtimeUs, long baseUptimeUs, long baseRealtimeUs) {
            super.onTimeStopped(elapsedRealtimeUs, baseUptimeUs, baseRealtimeUs);
            if (this.mNesting > 0) {
                this.mCurrentDurationMs += (baseRealtimeUs / 1000) - this.mStartTimeMs;
            }
            this.mStartTimeMs = -1L;
        }

        @Override // com.android.internal.os.BatteryStatsImpl.StopwatchTimer, com.android.internal.os.BatteryStatsImpl.Timer, android.os.BatteryStats.Timer
        public void logState(Printer pw, String prefix) {
            super.logState(pw, prefix);
        }

        @Override // com.android.internal.os.BatteryStatsImpl.StopwatchTimer
        public void startRunningLocked(long elapsedRealtimeMs) {
            super.startRunningLocked(elapsedRealtimeMs);
            if (this.mNesting == 1 && this.mTimeBase.isRunning()) {
                this.mStartTimeMs = this.mTimeBase.getRealtime(elapsedRealtimeMs * 1000) / 1000;
            }
        }

        @Override // com.android.internal.os.BatteryStatsImpl.StopwatchTimer
        public void stopRunningLocked(long elapsedRealtimeMs) {
            if (this.mNesting == 1) {
                long durationMs = getCurrentDurationMsLocked(elapsedRealtimeMs);
                this.mTotalDurationMs += durationMs;
                if (durationMs > this.mMaxDurationMs) {
                    this.mMaxDurationMs = durationMs;
                }
                this.mStartTimeMs = -1L;
                this.mCurrentDurationMs = 0L;
            }
            super.stopRunningLocked(elapsedRealtimeMs);
        }

        @Override // com.android.internal.os.BatteryStatsImpl.StopwatchTimer, com.android.internal.os.BatteryStatsImpl.Timer, com.android.internal.os.BatteryStatsImpl.TimeBaseObs
        public boolean reset(boolean detachIfReset, long elapsedRealtimeUs) {
            boolean result = super.reset(detachIfReset, elapsedRealtimeUs);
            this.mMaxDurationMs = 0L;
            this.mTotalDurationMs = 0L;
            this.mCurrentDurationMs = 0L;
            if (this.mNesting > 0) {
                this.mStartTimeMs = this.mTimeBase.getRealtime(elapsedRealtimeUs) / 1000;
            } else {
                this.mStartTimeMs = -1L;
            }
            return result;
        }

        @Override // android.os.BatteryStats.Timer
        public long getMaxDurationMsLocked(long elapsedRealtimeMs) {
            if (this.mNesting > 0) {
                long durationMs = getCurrentDurationMsLocked(elapsedRealtimeMs);
                if (durationMs > this.mMaxDurationMs) {
                    return durationMs;
                }
            }
            return this.mMaxDurationMs;
        }

        @Override // android.os.BatteryStats.Timer
        public long getCurrentDurationMsLocked(long elapsedRealtimeMs) {
            long durationMs = this.mCurrentDurationMs;
            if (this.mNesting > 0 && this.mTimeBase.isRunning()) {
                return durationMs + ((this.mTimeBase.getRealtime(elapsedRealtimeMs * 1000) / 1000) - this.mStartTimeMs);
            }
            return durationMs;
        }

        @Override // android.os.BatteryStats.Timer
        public long getTotalDurationMsLocked(long elapsedRealtimeMs) {
            return this.mTotalDurationMs + getCurrentDurationMsLocked(elapsedRealtimeMs);
        }
    }

    /* loaded from: classes4.dex */
    public static class StopwatchTimer extends Timer {
        long mAcquireTimeUs;
        public boolean mInList;
        int mNesting;
        long mTimeoutUs;
        final ArrayList<StopwatchTimer> mTimerPool;
        final Uid mUid;
        long mUpdateTimeUs;

        public StopwatchTimer(Clock clock, Uid uid, int type, ArrayList<StopwatchTimer> timerPool, TimeBase timeBase, Parcel in) {
            super(clock, type, timeBase, in);
            this.mAcquireTimeUs = -1L;
            this.mUid = uid;
            this.mTimerPool = timerPool;
            this.mUpdateTimeUs = in.readLong();
        }

        public StopwatchTimer(Clock clock, Uid uid, int type, ArrayList<StopwatchTimer> timerPool, TimeBase timeBase) {
            super(clock, type, timeBase);
            this.mAcquireTimeUs = -1L;
            this.mUid = uid;
            this.mTimerPool = timerPool;
        }

        public void setTimeout(long timeoutUs) {
            this.mTimeoutUs = timeoutUs;
        }

        @Override // com.android.internal.os.BatteryStatsImpl.Timer
        public void writeToParcel(Parcel out, long elapsedRealtimeUs) {
            super.writeToParcel(out, elapsedRealtimeUs);
            out.writeLong(this.mUpdateTimeUs);
        }

        @Override // com.android.internal.os.BatteryStatsImpl.Timer, com.android.internal.os.BatteryStatsImpl.TimeBaseObs
        public void onTimeStopped(long elapsedRealtimeUs, long baseUptimeUs, long baseRealtimeUs) {
            if (this.mNesting > 0) {
                super.onTimeStopped(elapsedRealtimeUs, baseUptimeUs, baseRealtimeUs);
                this.mUpdateTimeUs = baseRealtimeUs;
            }
        }

        @Override // com.android.internal.os.BatteryStatsImpl.Timer, android.os.BatteryStats.Timer
        public void logState(Printer pw, String prefix) {
            super.logState(pw, prefix);
            pw.println(prefix + "mNesting=" + this.mNesting + " mUpdateTime=" + this.mUpdateTimeUs + " mAcquireTime=" + this.mAcquireTimeUs);
        }

        public void startRunningLocked(long elapsedRealtimeMs) {
            int i = this.mNesting;
            this.mNesting = i + 1;
            if (i == 0) {
                long batteryRealtimeUs = this.mTimeBase.getRealtime(1000 * elapsedRealtimeMs);
                this.mUpdateTimeUs = batteryRealtimeUs;
                ArrayList<StopwatchTimer> arrayList = this.mTimerPool;
                if (arrayList != null) {
                    refreshTimersLocked(batteryRealtimeUs, arrayList, null);
                    this.mTimerPool.add(this);
                }
                if (this.mTimeBase.isRunning()) {
                    this.mCount++;
                    this.mAcquireTimeUs = this.mTotalTimeUs;
                    return;
                }
                this.mAcquireTimeUs = -1L;
            }
        }

        @Override // android.os.BatteryStats.Timer
        public boolean isRunningLocked() {
            return this.mNesting > 0;
        }

        public void stopRunningLocked(long elapsedRealtimeMs) {
            int i = this.mNesting;
            if (i == 0) {
                return;
            }
            int i2 = i - 1;
            this.mNesting = i2;
            if (i2 == 0) {
                long batteryRealtimeUs = this.mTimeBase.getRealtime(elapsedRealtimeMs * 1000);
                ArrayList<StopwatchTimer> arrayList = this.mTimerPool;
                if (arrayList != null) {
                    refreshTimersLocked(batteryRealtimeUs, arrayList, null);
                    this.mTimerPool.remove(this);
                } else {
                    this.mNesting = 1;
                    this.mTotalTimeUs = computeRunTimeLocked(batteryRealtimeUs, 1000 * elapsedRealtimeMs);
                    this.mNesting = 0;
                }
                if (this.mAcquireTimeUs >= 0 && this.mTotalTimeUs == this.mAcquireTimeUs) {
                    this.mCount--;
                }
            }
        }

        public void stopAllRunningLocked(long elapsedRealtimeMs) {
            if (this.mNesting > 0) {
                this.mNesting = 1;
                stopRunningLocked(elapsedRealtimeMs);
            }
        }

        private static long refreshTimersLocked(long batteryRealtimeUs, ArrayList<StopwatchTimer> pool, StopwatchTimer self) {
            long selfTimeUs = 0;
            int N = pool.size();
            for (int i = N - 1; i >= 0; i--) {
                StopwatchTimer t = pool.get(i);
                long heldTimeUs = batteryRealtimeUs - t.mUpdateTimeUs;
                if (heldTimeUs > 0) {
                    long myTimeUs = heldTimeUs / N;
                    if (t == self) {
                        selfTimeUs = myTimeUs;
                    }
                    t.mTotalTimeUs += myTimeUs;
                }
                t.mUpdateTimeUs = batteryRealtimeUs;
            }
            return selfTimeUs;
        }

        @Override // com.android.internal.os.BatteryStatsImpl.Timer
        protected long computeRunTimeLocked(long curBatteryRealtimeUs, long elapsedRealtimeUs) {
            long j = this.mTimeoutUs;
            long j2 = 0;
            if (j > 0) {
                long j3 = this.mUpdateTimeUs;
                if (curBatteryRealtimeUs > j3 + j) {
                    curBatteryRealtimeUs = j3 + j;
                }
            }
            long j4 = this.mTotalTimeUs;
            if (this.mNesting > 0) {
                long j5 = curBatteryRealtimeUs - this.mUpdateTimeUs;
                ArrayList<StopwatchTimer> arrayList = this.mTimerPool;
                j2 = j5 / (arrayList != null ? arrayList.size() : 1);
            }
            return j4 + j2;
        }

        @Override // com.android.internal.os.BatteryStatsImpl.Timer
        protected int computeCurrentCountLocked() {
            return this.mCount;
        }

        @Override // com.android.internal.os.BatteryStatsImpl.Timer, com.android.internal.os.BatteryStatsImpl.TimeBaseObs
        public boolean reset(boolean detachIfReset, long elapsedRealtimeUs) {
            boolean z = true;
            boolean canDetach = this.mNesting <= 0;
            if (!canDetach || !detachIfReset) {
                z = false;
            }
            super.reset(z, elapsedRealtimeUs);
            if (this.mNesting > 0) {
                this.mUpdateTimeUs = this.mTimeBase.getRealtime(elapsedRealtimeUs);
            }
            this.mAcquireTimeUs = -1L;
            return canDetach;
        }

        @Override // com.android.internal.os.BatteryStatsImpl.Timer, com.android.internal.os.BatteryStatsImpl.TimeBaseObs
        public void detach() {
            super.detach();
            ArrayList<StopwatchTimer> arrayList = this.mTimerPool;
            if (arrayList != null) {
                arrayList.remove(this);
            }
        }

        @Override // com.android.internal.os.BatteryStatsImpl.Timer
        public void readSummaryFromParcelLocked(Parcel in) {
            super.readSummaryFromParcelLocked(in);
            this.mNesting = 0;
        }

        public void setMark(long elapsedRealtimeMs) {
            long batteryRealtimeUs = this.mTimeBase.getRealtime(1000 * elapsedRealtimeMs);
            if (this.mNesting > 0) {
                ArrayList<StopwatchTimer> arrayList = this.mTimerPool;
                if (arrayList != null) {
                    refreshTimersLocked(batteryRealtimeUs, arrayList, this);
                } else {
                    this.mTotalTimeUs += batteryRealtimeUs - this.mUpdateTimeUs;
                    this.mUpdateTimeUs = batteryRealtimeUs;
                }
            }
            this.mTimeBeforeMarkUs = this.mTotalTimeUs;
        }
    }

    /* loaded from: classes4.dex */
    public static class DualTimer extends DurationTimer {
        private final DurationTimer mSubTimer;

        public DualTimer(Clock clock, Uid uid, int type, ArrayList<StopwatchTimer> timerPool, TimeBase timeBase, TimeBase subTimeBase, Parcel in) {
            super(clock, uid, type, timerPool, timeBase, in);
            this.mSubTimer = new DurationTimer(clock, uid, type, null, subTimeBase, in);
        }

        public DualTimer(Clock clock, Uid uid, int type, ArrayList<StopwatchTimer> timerPool, TimeBase timeBase, TimeBase subTimeBase) {
            super(clock, uid, type, timerPool, timeBase);
            this.mSubTimer = new DurationTimer(clock, uid, type, null, subTimeBase);
        }

        /* JADX DEBUG: Method merged with bridge method */
        @Override // android.os.BatteryStats.Timer
        public DurationTimer getSubTimer() {
            return this.mSubTimer;
        }

        @Override // com.android.internal.os.BatteryStatsImpl.DurationTimer, com.android.internal.os.BatteryStatsImpl.StopwatchTimer
        public void startRunningLocked(long elapsedRealtimeMs) {
            super.startRunningLocked(elapsedRealtimeMs);
            this.mSubTimer.startRunningLocked(elapsedRealtimeMs);
        }

        @Override // com.android.internal.os.BatteryStatsImpl.DurationTimer, com.android.internal.os.BatteryStatsImpl.StopwatchTimer
        public void stopRunningLocked(long elapsedRealtimeMs) {
            super.stopRunningLocked(elapsedRealtimeMs);
            this.mSubTimer.stopRunningLocked(elapsedRealtimeMs);
        }

        @Override // com.android.internal.os.BatteryStatsImpl.StopwatchTimer
        public void stopAllRunningLocked(long elapsedRealtimeMs) {
            super.stopAllRunningLocked(elapsedRealtimeMs);
            this.mSubTimer.stopAllRunningLocked(elapsedRealtimeMs);
        }

        @Override // com.android.internal.os.BatteryStatsImpl.DurationTimer, com.android.internal.os.BatteryStatsImpl.StopwatchTimer, com.android.internal.os.BatteryStatsImpl.Timer, com.android.internal.os.BatteryStatsImpl.TimeBaseObs
        public boolean reset(boolean detachIfReset, long elapsedRealtimeUs) {
            boolean active = false | (!this.mSubTimer.reset(false, elapsedRealtimeUs));
            return !(active | (!super.reset(detachIfReset, elapsedRealtimeUs)));
        }

        @Override // com.android.internal.os.BatteryStatsImpl.StopwatchTimer, com.android.internal.os.BatteryStatsImpl.Timer, com.android.internal.os.BatteryStatsImpl.TimeBaseObs
        public void detach() {
            this.mSubTimer.detach();
            super.detach();
        }

        @Override // com.android.internal.os.BatteryStatsImpl.DurationTimer, com.android.internal.os.BatteryStatsImpl.StopwatchTimer, com.android.internal.os.BatteryStatsImpl.Timer
        public void writeToParcel(Parcel out, long elapsedRealtimeUs) {
            super.writeToParcel(out, elapsedRealtimeUs);
            this.mSubTimer.writeToParcel(out, elapsedRealtimeUs);
        }

        @Override // com.android.internal.os.BatteryStatsImpl.DurationTimer, com.android.internal.os.BatteryStatsImpl.Timer
        public void writeSummaryFromParcelLocked(Parcel out, long elapsedRealtimeUs) {
            super.writeSummaryFromParcelLocked(out, elapsedRealtimeUs);
            this.mSubTimer.writeSummaryFromParcelLocked(out, elapsedRealtimeUs);
        }

        @Override // com.android.internal.os.BatteryStatsImpl.DurationTimer, com.android.internal.os.BatteryStatsImpl.StopwatchTimer, com.android.internal.os.BatteryStatsImpl.Timer
        public void readSummaryFromParcelLocked(Parcel in) {
            super.readSummaryFromParcelLocked(in);
            this.mSubTimer.readSummaryFromParcelLocked(in);
        }
    }

    /* loaded from: classes4.dex */
    public abstract class OverflowArrayMap<T> {
        private static final String OVERFLOW_NAME = "*overflow*";
        ArrayMap<String, MutableInt> mActiveOverflow;
        T mCurOverflow;
        long mLastCleanupTimeMs;
        long mLastClearTimeMs;
        long mLastOverflowFinishTimeMs;
        long mLastOverflowTimeMs;
        final ArrayMap<String, T> mMap = new ArrayMap<>();
        final int mUid;

        public abstract T instantiateObject();

        public OverflowArrayMap(int uid) {
            this.mUid = uid;
        }

        public ArrayMap<String, T> getMap() {
            return this.mMap;
        }

        public void clear() {
            this.mLastClearTimeMs = SystemClock.elapsedRealtime();
            this.mMap.clear();
            this.mCurOverflow = null;
            this.mActiveOverflow = null;
        }

        public void add(String name, T obj) {
            if (name == null) {
                name = "";
            }
            this.mMap.put(name, obj);
            if (OVERFLOW_NAME.equals(name)) {
                this.mCurOverflow = obj;
            }
        }

        public void cleanup(long elapsedRealtimeMs) {
            this.mLastCleanupTimeMs = elapsedRealtimeMs;
            ArrayMap<String, MutableInt> arrayMap = this.mActiveOverflow;
            if (arrayMap != null && arrayMap.size() == 0) {
                this.mActiveOverflow = null;
            }
            if (this.mActiveOverflow == null) {
                if (this.mMap.containsKey(OVERFLOW_NAME)) {
                    Slog.wtf(BatteryStatsImpl.TAG, "Cleaning up with no active overflow, but have overflow entry " + this.mMap.get(OVERFLOW_NAME));
                    this.mMap.remove(OVERFLOW_NAME);
                }
                this.mCurOverflow = null;
            } else if (this.mCurOverflow == null || !this.mMap.containsKey(OVERFLOW_NAME)) {
                Slog.wtf(BatteryStatsImpl.TAG, "Cleaning up with active overflow, but no overflow entry: cur=" + this.mCurOverflow + " map=" + this.mMap.get(OVERFLOW_NAME));
            }
        }

        public T startObject(String name, long elapsedRealtimeMs) {
            MutableInt over;
            if (name == null) {
                name = "";
            }
            T obj = this.mMap.get(name);
            if (obj != null) {
                return obj;
            }
            ArrayMap<String, MutableInt> arrayMap = this.mActiveOverflow;
            if (arrayMap != null && (over = arrayMap.get(name)) != null) {
                T obj2 = this.mCurOverflow;
                if (obj2 == null) {
                    Slog.wtf(BatteryStatsImpl.TAG, "Have active overflow " + name + " but null overflow");
                    T instantiateObject = instantiateObject();
                    this.mCurOverflow = instantiateObject;
                    obj2 = instantiateObject;
                    this.mMap.put(OVERFLOW_NAME, obj2);
                }
                over.value++;
                return obj2;
            }
            int N = this.mMap.size();
            if (N >= BatteryStatsImpl.MAX_WAKELOCKS_PER_UID) {
                T obj3 = this.mCurOverflow;
                if (obj3 == null) {
                    T instantiateObject2 = instantiateObject();
                    this.mCurOverflow = instantiateObject2;
                    obj3 = instantiateObject2;
                    this.mMap.put(OVERFLOW_NAME, obj3);
                }
                if (this.mActiveOverflow == null) {
                    this.mActiveOverflow = new ArrayMap<>();
                }
                this.mActiveOverflow.put(name, new MutableInt(1));
                this.mLastOverflowTimeMs = elapsedRealtimeMs;
                return obj3;
            }
            T obj4 = instantiateObject();
            this.mMap.put(name, obj4);
            return obj4;
        }

        public T stopObject(String name, long elapsedRealtimeMs) {
            MutableInt over;
            T obj;
            if (name == null) {
                name = "";
            }
            T obj2 = this.mMap.get(name);
            if (obj2 != null) {
                return obj2;
            }
            ArrayMap<String, MutableInt> arrayMap = this.mActiveOverflow;
            if (arrayMap != null && (over = arrayMap.get(name)) != null && (obj = this.mCurOverflow) != null) {
                over.value--;
                if (over.value <= 0) {
                    this.mActiveOverflow.remove(name);
                    this.mLastOverflowFinishTimeMs = elapsedRealtimeMs;
                }
                return obj;
            }
            StringBuilder sb = new StringBuilder();
            sb.append("Unable to find object for ");
            sb.append(name);
            sb.append(" in uid ");
            sb.append(this.mUid);
            sb.append(" mapsize=");
            sb.append(this.mMap.size());
            sb.append(" activeoverflow=");
            sb.append(this.mActiveOverflow);
            sb.append(" curoverflow=");
            sb.append(this.mCurOverflow);
            if (this.mLastOverflowTimeMs != 0) {
                sb.append(" lastOverflowTime=");
                TimeUtils.formatDuration(this.mLastOverflowTimeMs - elapsedRealtimeMs, sb);
            }
            if (this.mLastOverflowFinishTimeMs != 0) {
                sb.append(" lastOverflowFinishTime=");
                TimeUtils.formatDuration(this.mLastOverflowFinishTimeMs - elapsedRealtimeMs, sb);
            }
            if (this.mLastClearTimeMs != 0) {
                sb.append(" lastClearTime=");
                TimeUtils.formatDuration(this.mLastClearTimeMs - elapsedRealtimeMs, sb);
            }
            if (this.mLastCleanupTimeMs != 0) {
                sb.append(" lastCleanupTime=");
                TimeUtils.formatDuration(this.mLastCleanupTimeMs - elapsedRealtimeMs, sb);
            }
            Slog.wtf(BatteryStatsImpl.TAG, sb.toString());
            return null;
        }
    }

    /* loaded from: classes4.dex */
    public static class ControllerActivityCounterImpl extends BatteryStats.ControllerActivityCounter implements Parcelable {
        private final Clock mClock;
        private TimeMultiStateCounter mIdleTimeMillis;
        private final LongSamplingCounter mMonitoredRailChargeConsumedMaMs;
        private int mNumTxStates;
        private final LongSamplingCounter mPowerDrainMaMs;
        private int mProcessState;
        private TimeMultiStateCounter mRxTimeMillis;
        private final LongSamplingCounter mScanTimeMillis;
        private final LongSamplingCounter mSleepTimeMillis;
        private final TimeBase mTimeBase;
        private TimeMultiStateCounter[] mTxTimeMillis;

        public ControllerActivityCounterImpl(Clock clock, TimeBase timeBase, int numTxStates) {
            this.mClock = clock;
            this.mTimeBase = timeBase;
            this.mNumTxStates = numTxStates;
            this.mScanTimeMillis = new LongSamplingCounter(timeBase);
            this.mSleepTimeMillis = new LongSamplingCounter(timeBase);
            this.mPowerDrainMaMs = new LongSamplingCounter(timeBase);
            this.mMonitoredRailChargeConsumedMaMs = new LongSamplingCounter(timeBase);
        }

        public ControllerActivityCounterImpl(Clock clock, TimeBase timeBase, int numTxStates, Parcel in) {
            this.mClock = clock;
            this.mTimeBase = timeBase;
            this.mNumTxStates = numTxStates;
            this.mIdleTimeMillis = readTimeMultiStateCounter(in, timeBase);
            this.mScanTimeMillis = new LongSamplingCounter(timeBase, in);
            this.mSleepTimeMillis = new LongSamplingCounter(timeBase, in);
            this.mRxTimeMillis = readTimeMultiStateCounter(in, timeBase);
            this.mTxTimeMillis = readTimeMultiStateCounters(in, timeBase, numTxStates);
            this.mPowerDrainMaMs = new LongSamplingCounter(timeBase, in);
            this.mMonitoredRailChargeConsumedMaMs = new LongSamplingCounter(timeBase, in);
        }

        public void readSummaryFromParcel(Parcel in) {
            this.mIdleTimeMillis = readTimeMultiStateCounter(in, this.mTimeBase);
            this.mScanTimeMillis.readSummaryFromParcelLocked(in);
            this.mSleepTimeMillis.readSummaryFromParcelLocked(in);
            this.mRxTimeMillis = readTimeMultiStateCounter(in, this.mTimeBase);
            this.mTxTimeMillis = readTimeMultiStateCounters(in, this.mTimeBase, this.mNumTxStates);
            this.mPowerDrainMaMs.readSummaryFromParcelLocked(in);
            this.mMonitoredRailChargeConsumedMaMs.readSummaryFromParcelLocked(in);
        }

        @Override // android.os.Parcelable
        public int describeContents() {
            return 0;
        }

        public void writeSummaryToParcel(Parcel dest) {
            writeTimeMultiStateCounter(dest, this.mIdleTimeMillis);
            this.mScanTimeMillis.writeSummaryFromParcelLocked(dest);
            this.mSleepTimeMillis.writeSummaryFromParcelLocked(dest);
            writeTimeMultiStateCounter(dest, this.mRxTimeMillis);
            writeTimeMultiStateCounters(dest, this.mTxTimeMillis);
            this.mPowerDrainMaMs.writeSummaryFromParcelLocked(dest);
            this.mMonitoredRailChargeConsumedMaMs.writeSummaryFromParcelLocked(dest);
        }

        @Override // android.os.Parcelable
        public void writeToParcel(Parcel dest, int flags) {
            writeTimeMultiStateCounter(dest, this.mIdleTimeMillis);
            this.mScanTimeMillis.writeToParcel(dest);
            this.mSleepTimeMillis.writeToParcel(dest);
            writeTimeMultiStateCounter(dest, this.mRxTimeMillis);
            writeTimeMultiStateCounters(dest, this.mTxTimeMillis);
            this.mPowerDrainMaMs.writeToParcel(dest);
            this.mMonitoredRailChargeConsumedMaMs.writeToParcel(dest);
        }

        private TimeMultiStateCounter readTimeMultiStateCounter(Parcel in, TimeBase timeBase) {
            if (in.readBoolean()) {
                return TimeMultiStateCounter.readFromParcel(in, timeBase, 5, this.mClock.elapsedRealtime());
            }
            return null;
        }

        private void writeTimeMultiStateCounter(Parcel dest, TimeMultiStateCounter counter) {
            if (counter != null) {
                dest.writeBoolean(true);
                counter.writeToParcel(dest);
                return;
            }
            dest.writeBoolean(false);
        }

        private TimeMultiStateCounter[] readTimeMultiStateCounters(Parcel in, TimeBase timeBase, int expectedNumCounters) {
            if (in.readBoolean()) {
                int numCounters = in.readInt();
                boolean valid = numCounters == expectedNumCounters;
                TimeMultiStateCounter[] counters = new TimeMultiStateCounter[numCounters];
                for (int i = 0; i < numCounters; i++) {
                    TimeMultiStateCounter counter = TimeMultiStateCounter.readFromParcel(in, timeBase, 5, this.mClock.elapsedRealtime());
                    if (counter != null) {
                        counters[i] = counter;
                    } else {
                        valid = false;
                    }
                }
                if (valid) {
                    return counters;
                }
                return null;
            }
            return null;
        }

        private void writeTimeMultiStateCounters(Parcel dest, TimeMultiStateCounter[] counters) {
            if (counters != null) {
                dest.writeBoolean(true);
                dest.writeInt(counters.length);
                for (TimeMultiStateCounter counter : counters) {
                    counter.writeToParcel(dest);
                }
                return;
            }
            dest.writeBoolean(false);
        }

        public void reset(boolean detachIfReset, long elapsedRealtimeUs) {
            BatteryStatsImpl.resetIfNotNull(this.mIdleTimeMillis, detachIfReset, elapsedRealtimeUs);
            this.mScanTimeMillis.reset(detachIfReset, elapsedRealtimeUs);
            this.mSleepTimeMillis.reset(detachIfReset, elapsedRealtimeUs);
            BatteryStatsImpl.resetIfNotNull(this.mRxTimeMillis, detachIfReset, elapsedRealtimeUs);
            BatteryStatsImpl.resetIfNotNull(this.mTxTimeMillis, detachIfReset, elapsedRealtimeUs);
            this.mPowerDrainMaMs.reset(detachIfReset, elapsedRealtimeUs);
            this.mMonitoredRailChargeConsumedMaMs.reset(detachIfReset, elapsedRealtimeUs);
        }

        public void detach() {
            BatteryStatsImpl.detachIfNotNull(this.mIdleTimeMillis);
            this.mScanTimeMillis.detach();
            this.mSleepTimeMillis.detach();
            BatteryStatsImpl.detachIfNotNull(this.mRxTimeMillis);
            BatteryStatsImpl.detachIfNotNull(this.mTxTimeMillis);
            this.mPowerDrainMaMs.detach();
            this.mMonitoredRailChargeConsumedMaMs.detach();
        }

        @Override // android.os.BatteryStats.ControllerActivityCounter
        public BatteryStats.LongCounter getIdleTimeCounter() {
            TimeMultiStateCounter timeMultiStateCounter = this.mIdleTimeMillis;
            if (timeMultiStateCounter == null) {
                return BatteryStatsImpl.ZERO_LONG_COUNTER;
            }
            return timeMultiStateCounter;
        }

        /* JADX INFO: Access modifiers changed from: private */
        public TimeMultiStateCounter getOrCreateIdleTimeCounter() {
            if (this.mIdleTimeMillis == null) {
                this.mIdleTimeMillis = createTimeMultiStateCounter();
            }
            return this.mIdleTimeMillis;
        }

        /* JADX DEBUG: Method merged with bridge method */
        @Override // android.os.BatteryStats.ControllerActivityCounter
        public LongSamplingCounter getScanTimeCounter() {
            return this.mScanTimeMillis;
        }

        /* JADX DEBUG: Method merged with bridge method */
        @Override // android.os.BatteryStats.ControllerActivityCounter
        public LongSamplingCounter getSleepTimeCounter() {
            return this.mSleepTimeMillis;
        }

        @Override // android.os.BatteryStats.ControllerActivityCounter
        public BatteryStats.LongCounter getRxTimeCounter() {
            TimeMultiStateCounter timeMultiStateCounter = this.mRxTimeMillis;
            if (timeMultiStateCounter == null) {
                return BatteryStatsImpl.ZERO_LONG_COUNTER;
            }
            return timeMultiStateCounter;
        }

        /* JADX INFO: Access modifiers changed from: private */
        public TimeMultiStateCounter getOrCreateRxTimeCounter() {
            if (this.mRxTimeMillis == null) {
                this.mRxTimeMillis = createTimeMultiStateCounter();
            }
            return this.mRxTimeMillis;
        }

        @Override // android.os.BatteryStats.ControllerActivityCounter
        public BatteryStats.LongCounter[] getTxTimeCounters() {
            TimeMultiStateCounter[] timeMultiStateCounterArr = this.mTxTimeMillis;
            if (timeMultiStateCounterArr == null) {
                return BatteryStatsImpl.ZERO_LONG_COUNTER_ARRAY;
            }
            return timeMultiStateCounterArr;
        }

        /* JADX INFO: Access modifiers changed from: private */
        public TimeMultiStateCounter[] getOrCreateTxTimeCounters() {
            if (this.mTxTimeMillis == null) {
                this.mTxTimeMillis = new TimeMultiStateCounter[this.mNumTxStates];
                for (int i = 0; i < this.mNumTxStates; i++) {
                    this.mTxTimeMillis[i] = createTimeMultiStateCounter();
                }
            }
            return this.mTxTimeMillis;
        }

        private TimeMultiStateCounter createTimeMultiStateCounter() {
            long timestampMs = this.mClock.elapsedRealtime();
            TimeMultiStateCounter counter = new TimeMultiStateCounter(this.mTimeBase, 5, timestampMs);
            counter.setState(BatteryStats.mapUidProcessStateToBatteryConsumerProcessState(this.mProcessState), timestampMs);
            counter.update(0L, timestampMs);
            return counter;
        }

        /* JADX DEBUG: Method merged with bridge method */
        @Override // android.os.BatteryStats.ControllerActivityCounter
        public LongSamplingCounter getPowerCounter() {
            return this.mPowerDrainMaMs;
        }

        /* JADX DEBUG: Method merged with bridge method */
        @Override // android.os.BatteryStats.ControllerActivityCounter
        public LongSamplingCounter getMonitoredRailChargeConsumedMaMs() {
            return this.mMonitoredRailChargeConsumedMaMs;
        }

        /* JADX INFO: Access modifiers changed from: private */
        public void setState(int processState, long elapsedTimeMs) {
            this.mProcessState = processState;
            TimeMultiStateCounter timeMultiStateCounter = this.mIdleTimeMillis;
            if (timeMultiStateCounter != null) {
                timeMultiStateCounter.setState(processState, elapsedTimeMs);
            }
            TimeMultiStateCounter timeMultiStateCounter2 = this.mRxTimeMillis;
            if (timeMultiStateCounter2 != null) {
                timeMultiStateCounter2.setState(processState, elapsedTimeMs);
            }
            if (this.mTxTimeMillis != null) {
                int i = 0;
                while (true) {
                    TimeMultiStateCounter[] timeMultiStateCounterArr = this.mTxTimeMillis;
                    if (i < timeMultiStateCounterArr.length) {
                        timeMultiStateCounterArr[i].setState(processState, elapsedTimeMs);
                        i++;
                    } else {
                        return;
                    }
                }
            }
        }
    }

    public SamplingTimer getRpmTimerLocked(String name) {
        SamplingTimer rpmt = this.mRpmStats.get(name);
        if (rpmt == null) {
            SamplingTimer rpmt2 = new SamplingTimer(this.mClock, this.mOnBatteryTimeBase);
            this.mRpmStats.put(name, rpmt2);
            return rpmt2;
        }
        return rpmt;
    }

    public SamplingTimer getScreenOffRpmTimerLocked(String name) {
        SamplingTimer rpmt = this.mScreenOffRpmStats.get(name);
        if (rpmt == null) {
            SamplingTimer rpmt2 = new SamplingTimer(this.mClock, this.mOnBatteryScreenOffTimeBase);
            this.mScreenOffRpmStats.put(name, rpmt2);
            return rpmt2;
        }
        return rpmt;
    }

    public SamplingTimer getWakeupReasonTimerLocked(String name) {
        SamplingTimer timer;
        synchronized (mLock) {
            timer = this.mWakeupReasonStats.get(name);
            if (timer == null) {
                timer = new SamplingTimer(this.mClock, this.mOnBatteryTimeBase);
                this.mWakeupReasonStats.put(name, timer);
            }
        }
        return timer;
    }

    public SamplingTimer getKernelWakelockTimerLocked(String name) {
        SamplingTimer kwlt = this.mKernelWakelockStats.get(name);
        if (kwlt == null) {
            SamplingTimer kwlt2 = new SamplingTimer(this.mClock, this.mOnBatteryScreenOffTimeBase);
            this.mKernelWakelockStats.put(name, kwlt2);
            return kwlt2;
        }
        return kwlt;
    }

    public SamplingTimer getKernelMemoryTimerLocked(long bucket) {
        SamplingTimer kmt = this.mKernelMemoryStats.get(bucket);
        if (kmt == null) {
            SamplingTimer kmt2 = new SamplingTimer(this.mClock, this.mOnBatteryTimeBase);
            this.mKernelMemoryStats.put(bucket, kmt2);
            return kmt2;
        }
        return kmt;
    }

    private int writeHistoryTag(BatteryStats.HistoryTag tag) {
        if (tag.string == null) {
            Slog.wtfStack(TAG, "writeHistoryTag called with null name");
        }
        int stringLength = tag.string.length();
        if (stringLength > 1024) {
            Slog.e(TAG, "Long battery history tag: " + tag.string);
            tag.string = tag.string.substring(0, 1024);
        }
        Integer idxObj = this.mHistoryTagPool.get(tag);
        if (idxObj != null) {
            int idx = idxObj.intValue();
            if ((32768 & idx) != 0) {
                this.mHistoryTagPool.put(tag, Integer.valueOf((-32769) & idx));
            }
            return idx;
        } else if (this.mNextHistoryTagIdx < HISTORY_TAG_INDEX_LIMIT) {
            int idx2 = this.mNextHistoryTagIdx;
            BatteryStats.HistoryTag key = new BatteryStats.HistoryTag();
            key.setTo(tag);
            tag.poolIdx = idx2;
            this.mHistoryTagPool.put(key, Integer.valueOf(idx2));
            this.mNextHistoryTagIdx++;
            this.mNumHistoryTagChars += stringLength + 1;
            SparseArray<BatteryStats.HistoryTag> sparseArray = this.mHistoryTags;
            if (sparseArray != null) {
                sparseArray.put(idx2, key);
            }
            return 32768 | idx2;
        } else {
            return Configuration.DENSITY_DPI_ANY;
        }
    }

    public void writeHistoryDelta(Parcel dest, BatteryStats.HistoryItem cur, BatteryStats.HistoryItem last) {
        int deltaTimeToken;
        int wakeLockIndex;
        int wakeReasonIndex;
        if (last == null || cur.cmd != 0) {
            dest.writeInt(DELTA_TIME_ABS);
            cur.writeToParcel(dest, 0);
            return;
        }
        long deltaTime = cur.time - last.time;
        int lastBatteryLevelInt = buildBatteryLevelInt(last);
        int lastStateInt = buildStateInt(last);
        if (deltaTime < 0 || deltaTime > 2147483647L) {
            deltaTimeToken = 524287;
        } else if (deltaTime >= 524285) {
            deltaTimeToken = DELTA_TIME_INT;
        } else {
            deltaTimeToken = (int) deltaTime;
        }
        int firstToken = (cur.states & DELTA_STATE_MASK) | deltaTimeToken;
        int includeStepDetails = this.mLastHistoryStepLevel > cur.batteryLevel ? 1 : 0;
        boolean computeStepDetails = includeStepDetails != 0 || this.mLastHistoryStepDetails == null;
        int batteryLevelInt = buildBatteryLevelInt(cur) | includeStepDetails;
        boolean batteryLevelIntChanged = batteryLevelInt != lastBatteryLevelInt;
        if (batteryLevelIntChanged) {
            firstToken |= 524288;
        }
        int stateInt = buildStateInt(cur);
        boolean stateIntChanged = stateInt != lastStateInt;
        if (stateIntChanged) {
            firstToken |= 1048576;
        }
        int i = cur.states2;
        int lastBatteryLevelInt2 = last.states2;
        boolean state2IntChanged = i != lastBatteryLevelInt2;
        if (state2IntChanged) {
            firstToken |= 2097152;
        }
        if (cur.wakelockTag != null || cur.wakeReasonTag != null) {
            firstToken |= 4194304;
        }
        if (cur.eventCode != 0) {
            firstToken |= 8388608;
        }
        int i2 = cur.batteryChargeUah;
        int lastStateInt2 = last.batteryChargeUah;
        boolean batteryChargeChanged = i2 != lastStateInt2;
        if (batteryChargeChanged) {
            firstToken |= 16777216;
        }
        dest.writeInt(firstToken);
        if (deltaTimeToken >= DELTA_TIME_INT) {
            if (deltaTimeToken == DELTA_TIME_INT) {
                dest.writeInt((int) deltaTime);
            } else {
                dest.writeLong(deltaTime);
            }
        }
        if (batteryLevelIntChanged) {
            dest.writeInt(batteryLevelInt);
        }
        if (stateIntChanged) {
            dest.writeInt(stateInt);
        }
        if (state2IntChanged) {
            dest.writeInt(cur.states2);
        }
        if (cur.wakelockTag != null || cur.wakeReasonTag != null) {
            if (cur.wakelockTag != null) {
                wakeLockIndex = writeHistoryTag(cur.wakelockTag);
            } else {
                wakeLockIndex = 65535;
            }
            if (cur.wakeReasonTag != null) {
                wakeReasonIndex = writeHistoryTag(cur.wakeReasonTag);
            } else {
                wakeReasonIndex = 65535;
            }
            dest.writeInt((wakeReasonIndex << 16) | wakeLockIndex);
            if (cur.wakelockTag != null && (wakeLockIndex & 32768) != 0) {
                cur.wakelockTag.writeToParcel(dest, 0);
                cur.tagsFirstOccurrence = true;
            }
            if (cur.wakeReasonTag != null && (wakeReasonIndex & 32768) != 0) {
                cur.wakeReasonTag.writeToParcel(dest, 0);
                cur.tagsFirstOccurrence = true;
            }
        }
        int wakeReasonIndex2 = cur.eventCode;
        if (wakeReasonIndex2 != 0) {
            int index = writeHistoryTag(cur.eventTag);
            int codeAndIndex = (cur.eventCode & 65535) | (index << 16);
            dest.writeInt(codeAndIndex);
            if ((index & 32768) != 0) {
                cur.eventTag.writeToParcel(dest, 0);
                cur.tagsFirstOccurrence = true;
            }
        }
        if (computeStepDetails) {
            PlatformIdleStateCallback platformIdleStateCallback = this.mPlatformIdleStateCallback;
            if (platformIdleStateCallback != null) {
                this.mCurHistoryStepDetails.statSubsystemPowerState = platformIdleStateCallback.getSubsystemLowPowerStats();
            }
            computeHistoryStepDetails(this.mCurHistoryStepDetails, this.mLastHistoryStepDetails);
            if (includeStepDetails != 0) {
                this.mCurHistoryStepDetails.writeToParcel(dest);
            }
            cur.stepDetails = this.mCurHistoryStepDetails;
            this.mLastHistoryStepDetails = this.mCurHistoryStepDetails;
        } else {
            cur.stepDetails = null;
        }
        if (this.mLastHistoryStepLevel < cur.batteryLevel) {
            this.mLastHistoryStepDetails = null;
        }
        this.mLastHistoryStepLevel = cur.batteryLevel;
        if (batteryChargeChanged) {
            dest.writeInt(cur.batteryChargeUah);
        }
        dest.writeDouble(cur.modemRailChargeMah);
        dest.writeDouble(cur.wifiRailChargeMah);
    }

    private int buildBatteryLevelInt(BatteryStats.HistoryItem h) {
        return ((h.batteryLevel << 25) & DELTA_STATE_MASK) | ((h.batteryTemperature << 15) & 33521664) | ((h.batteryVoltage << 1) & HISTORY_TAG_INDEX_LIMIT);
    }

    private void readBatteryLevelInt(int batteryLevelInt, BatteryStats.HistoryItem out) {
        out.batteryLevel = (byte) ((DELTA_STATE_MASK & batteryLevelInt) >>> 25);
        out.batteryTemperature = (short) ((33521664 & batteryLevelInt) >>> 15);
        out.batteryVoltage = (char) ((batteryLevelInt & HISTORY_TAG_INDEX_LIMIT) >>> 1);
    }

    private int buildStateInt(BatteryStats.HistoryItem h) {
        int plugType = 0;
        if ((h.batteryPlugType & 1) != 0) {
            plugType = 1;
        } else if ((h.batteryPlugType & 2) != 0) {
            plugType = 2;
        } else if ((h.batteryPlugType & 4) != 0) {
            plugType = 3;
        }
        return ((h.batteryStatus & 7) << 29) | ((h.batteryHealth & 7) << 26) | ((plugType & 3) << 24) | (h.states & 16777215);
    }

    private void computeHistoryStepDetails(BatteryStats.HistoryStepDetails out, BatteryStats.HistoryStepDetails last) {
        BatteryStats.HistoryStepDetails tmp = last != null ? this.mTmpHistoryStepDetails : out;
        requestImmediateCpuUpdate();
        if (last == null) {
            int NU = this.mUidStats.size();
            for (int i = 0; i < NU; i++) {
                Uid uid = this.mUidStats.valueAt(i);
                uid.mLastStepUserTimeMs = uid.mCurStepUserTimeMs;
                uid.mLastStepSystemTimeMs = uid.mCurStepSystemTimeMs;
            }
            this.mLastStepCpuUserTimeMs = this.mCurStepCpuUserTimeMs;
            this.mLastStepCpuSystemTimeMs = this.mCurStepCpuSystemTimeMs;
            this.mLastStepStatUserTimeMs = this.mCurStepStatUserTimeMs;
            this.mLastStepStatSystemTimeMs = this.mCurStepStatSystemTimeMs;
            this.mLastStepStatIOWaitTimeMs = this.mCurStepStatIOWaitTimeMs;
            this.mLastStepStatIrqTimeMs = this.mCurStepStatIrqTimeMs;
            this.mLastStepStatSoftIrqTimeMs = this.mCurStepStatSoftIrqTimeMs;
            this.mLastStepStatIdleTimeMs = this.mCurStepStatIdleTimeMs;
            tmp.clear();
            return;
        }
        out.userTime = (int) (this.mCurStepCpuUserTimeMs - this.mLastStepCpuUserTimeMs);
        out.systemTime = (int) (this.mCurStepCpuSystemTimeMs - this.mLastStepCpuSystemTimeMs);
        out.statUserTime = (int) (this.mCurStepStatUserTimeMs - this.mLastStepStatUserTimeMs);
        out.statSystemTime = (int) (this.mCurStepStatSystemTimeMs - this.mLastStepStatSystemTimeMs);
        out.statIOWaitTime = (int) (this.mCurStepStatIOWaitTimeMs - this.mLastStepStatIOWaitTimeMs);
        out.statIrqTime = (int) (this.mCurStepStatIrqTimeMs - this.mLastStepStatIrqTimeMs);
        out.statSoftIrqTime = (int) (this.mCurStepStatSoftIrqTimeMs - this.mLastStepStatSoftIrqTimeMs);
        out.statIdlTime = (int) (this.mCurStepStatIdleTimeMs - this.mLastStepStatIdleTimeMs);
        out.appCpuUid3 = -1;
        out.appCpuUid2 = -1;
        out.appCpuUid1 = -1;
        out.appCpuUTime3 = 0;
        out.appCpuUTime2 = 0;
        out.appCpuUTime1 = 0;
        out.appCpuSTime3 = 0;
        out.appCpuSTime2 = 0;
        out.appCpuSTime1 = 0;
        int NU2 = this.mUidStats.size();
        for (int i2 = 0; i2 < NU2; i2++) {
            Uid uid2 = this.mUidStats.valueAt(i2);
            int totalUTimeMs = (int) (uid2.mCurStepUserTimeMs - uid2.mLastStepUserTimeMs);
            int totalSTimeMs = (int) (uid2.mCurStepSystemTimeMs - uid2.mLastStepSystemTimeMs);
            int totalTimeMs = totalUTimeMs + totalSTimeMs;
            uid2.mLastStepUserTimeMs = uid2.mCurStepUserTimeMs;
            uid2.mLastStepSystemTimeMs = uid2.mCurStepSystemTimeMs;
            if (totalTimeMs > out.appCpuUTime3 + out.appCpuSTime3) {
                if (totalTimeMs <= out.appCpuUTime2 + out.appCpuSTime2) {
                    out.appCpuUid3 = uid2.mUid;
                    out.appCpuUTime3 = totalUTimeMs;
                    out.appCpuSTime3 = totalSTimeMs;
                } else {
                    out.appCpuUid3 = out.appCpuUid2;
                    out.appCpuUTime3 = out.appCpuUTime2;
                    out.appCpuSTime3 = out.appCpuSTime2;
                    if (totalTimeMs <= out.appCpuUTime1 + out.appCpuSTime1) {
                        out.appCpuUid2 = uid2.mUid;
                        out.appCpuUTime2 = totalUTimeMs;
                        out.appCpuSTime2 = totalSTimeMs;
                    } else {
                        out.appCpuUid2 = out.appCpuUid1;
                        out.appCpuUTime2 = out.appCpuUTime1;
                        out.appCpuSTime2 = out.appCpuSTime1;
                        out.appCpuUid1 = uid2.mUid;
                        out.appCpuUTime1 = totalUTimeMs;
                        out.appCpuSTime1 = totalSTimeMs;
                    }
                }
            }
        }
        this.mLastStepCpuUserTimeMs = this.mCurStepCpuUserTimeMs;
        this.mLastStepCpuSystemTimeMs = this.mCurStepCpuSystemTimeMs;
        this.mLastStepStatUserTimeMs = this.mCurStepStatUserTimeMs;
        this.mLastStepStatSystemTimeMs = this.mCurStepStatSystemTimeMs;
        this.mLastStepStatIOWaitTimeMs = this.mCurStepStatIOWaitTimeMs;
        this.mLastStepStatIrqTimeMs = this.mCurStepStatIrqTimeMs;
        this.mLastStepStatSoftIrqTimeMs = this.mCurStepStatSoftIrqTimeMs;
        this.mLastStepStatIdleTimeMs = this.mCurStepStatIdleTimeMs;
    }

    @Override // android.os.BatteryStats
    public void commitCurrentHistoryBatchLocked() {
        this.mHistoryLastWritten.cmd = (byte) -1;
    }

    public void createFakeHistoryEvents(long numEvents) {
        long elapsedRealtimeMs = this.mClock.elapsedRealtime();
        long uptimeMs = this.mClock.uptimeMillis();
        for (long i = 0; i < numEvents; i++) {
            noteLongPartialWakelockStart("name1", "historyName1", 1000, elapsedRealtimeMs, uptimeMs);
            noteLongPartialWakelockFinish("name1", "historyName1", 1000, elapsedRealtimeMs, uptimeMs);
        }
    }

    void addHistoryBufferLocked(long elapsedRealtimeMs, long uptimeMs, BatteryStats.HistoryItem cur) {
        long elapsedRealtimeMs2;
        if (this.mHaveBatteryLevel && this.mRecordingHistory) {
            long timeDiffMs = (this.mHistoryBaseTimeMs + elapsedRealtimeMs) - this.mHistoryLastWritten.time;
            int diffStates = this.mHistoryLastWritten.states ^ (cur.states & this.mActiveHistoryStates);
            int diffStates2 = this.mHistoryLastWritten.states2 ^ (cur.states2 & this.mActiveHistoryStates2);
            int lastDiffStates = this.mHistoryLastWritten.states ^ this.mHistoryLastLastWritten.states;
            int lastDiffStates2 = this.mHistoryLastWritten.states2 ^ this.mHistoryLastLastWritten.states2;
            if (this.mHistoryBufferLastPos >= 0 && this.mHistoryLastWritten.cmd == 0 && timeDiffMs < 1000 && (diffStates & lastDiffStates) == 0 && (diffStates2 & lastDiffStates2) == 0 && !this.mHistoryLastWritten.tagsFirstOccurrence && !cur.tagsFirstOccurrence && ((this.mHistoryLastWritten.wakelockTag == null || cur.wakelockTag == null) && ((this.mHistoryLastWritten.wakeReasonTag == null || cur.wakeReasonTag == null) && this.mHistoryLastWritten.stepDetails == null && ((this.mHistoryLastWritten.eventCode == 0 || cur.eventCode == 0) && this.mHistoryLastWritten.batteryLevel == cur.batteryLevel && this.mHistoryLastWritten.batteryStatus == cur.batteryStatus && this.mHistoryLastWritten.batteryHealth == cur.batteryHealth && this.mHistoryLastWritten.batteryPlugType == cur.batteryPlugType && this.mHistoryLastWritten.batteryTemperature == cur.batteryTemperature && this.mHistoryLastWritten.batteryVoltage == cur.batteryVoltage)))) {
                this.mHistoryBuffer.setDataSize(this.mHistoryBufferLastPos);
                this.mHistoryBuffer.setDataPosition(this.mHistoryBufferLastPos);
                this.mHistoryBufferLastPos = -1;
                long elapsedRealtimeMs3 = this.mHistoryLastWritten.time - this.mHistoryBaseTimeMs;
                if (this.mHistoryLastWritten.wakelockTag != null) {
                    cur.wakelockTag = cur.localWakelockTag;
                    cur.wakelockTag.setTo(this.mHistoryLastWritten.wakelockTag);
                }
                if (this.mHistoryLastWritten.wakeReasonTag != null) {
                    cur.wakeReasonTag = cur.localWakeReasonTag;
                    cur.wakeReasonTag.setTo(this.mHistoryLastWritten.wakeReasonTag);
                }
                if (this.mHistoryLastWritten.eventCode != 0) {
                    cur.eventCode = this.mHistoryLastWritten.eventCode;
                    cur.eventTag = cur.localEventTag;
                    cur.eventTag.setTo(this.mHistoryLastWritten.eventTag);
                }
                this.mHistoryLastWritten.setTo(this.mHistoryLastLastWritten);
                elapsedRealtimeMs2 = elapsedRealtimeMs3;
            } else {
                elapsedRealtimeMs2 = elapsedRealtimeMs;
            }
            int dataSize = this.mHistoryBuffer.dataSize();
            if (dataSize >= this.mConstants.MAX_HISTORY_BUFFER) {
                SystemClock.uptimeMillis();
                writeHistoryLocked(true);
                this.mBatteryStatsHistory.startNextFile();
                this.mHistoryBuffer.setDataSize(0);
                this.mHistoryBuffer.setDataPosition(0);
                this.mHistoryBuffer.setDataCapacity(this.mConstants.MAX_HISTORY_BUFFER / 2);
                this.mHistoryBufferLastPos = -1;
                this.mHistoryLastWritten.clear();
                this.mHistoryLastLastWritten.clear();
                for (Map.Entry<BatteryStats.HistoryTag, Integer> entry : this.mHistoryTagPool.entrySet()) {
                    entry.setValue(Integer.valueOf(entry.getValue().intValue() | 32768));
                }
                BatteryStats.HistoryItem copy = new BatteryStats.HistoryItem();
                copy.setTo(cur);
                startRecordingHistory(elapsedRealtimeMs2, uptimeMs, false);
                addHistoryBufferLocked(elapsedRealtimeMs2, (byte) 0, copy);
                return;
            }
            if (dataSize == 0) {
                cur.currentTime = this.mClock.currentTimeMillis();
                addHistoryBufferLocked(elapsedRealtimeMs2, (byte) 7, cur);
            }
            addHistoryBufferLocked(elapsedRealtimeMs2, (byte) 0, cur);
        }
    }

    private void addHistoryBufferLocked(long elapsedRealtimeMs, byte cmd, BatteryStats.HistoryItem cur) {
        if (this.mBatteryStatsHistoryIterator != null) {
            throw new IllegalStateException("Can't do this while iterating history!");
        }
        this.mHistoryBufferLastPos = this.mHistoryBuffer.dataPosition();
        this.mHistoryLastLastWritten.setTo(this.mHistoryLastWritten);
        boolean hasTags = this.mHistoryLastWritten.tagsFirstOccurrence || cur.tagsFirstOccurrence;
        this.mHistoryLastWritten.setTo(this.mHistoryBaseTimeMs + elapsedRealtimeMs, cmd, cur);
        this.mHistoryLastWritten.tagsFirstOccurrence = hasTags;
        this.mHistoryLastWritten.states &= this.mActiveHistoryStates;
        this.mHistoryLastWritten.states2 &= this.mActiveHistoryStates2;
        writeHistoryDelta(this.mHistoryBuffer, this.mHistoryLastWritten, this.mHistoryLastLastWritten);
        this.mLastHistoryElapsedRealtimeMs = elapsedRealtimeMs;
        cur.wakelockTag = null;
        cur.wakeReasonTag = null;
        cur.eventCode = 0;
        cur.eventTag = null;
        cur.tagsFirstOccurrence = false;
    }

    void addHistoryRecordLocked(long elapsedRealtimeMs, long uptimeMs) {
        long j = this.mTrackRunningHistoryElapsedRealtimeMs;
        if (j != 0) {
            long diffElapsedMs = elapsedRealtimeMs - j;
            long diffUptimeMs = uptimeMs - this.mTrackRunningHistoryUptimeMs;
            if (diffUptimeMs < diffElapsedMs - 20) {
                long wakeElapsedTimeMs = elapsedRealtimeMs - (diffElapsedMs - diffUptimeMs);
                this.mHistoryAddTmp.setTo(this.mHistoryLastWritten);
                this.mHistoryAddTmp.wakelockTag = null;
                this.mHistoryAddTmp.wakeReasonTag = null;
                this.mHistoryAddTmp.eventCode = 0;
                this.mHistoryAddTmp.states &= Integer.MAX_VALUE;
                addHistoryRecordInnerLocked(wakeElapsedTimeMs, uptimeMs, this.mHistoryAddTmp);
            }
        }
        this.mHistoryCur.states |= Integer.MIN_VALUE;
        this.mTrackRunningHistoryElapsedRealtimeMs = elapsedRealtimeMs;
        this.mTrackRunningHistoryUptimeMs = uptimeMs;
        addHistoryRecordInnerLocked(elapsedRealtimeMs, uptimeMs, this.mHistoryCur);
    }

    void addHistoryRecordInnerLocked(long elapsedRealtimeMs, long uptimeMs, BatteryStats.HistoryItem cur) {
        addHistoryBufferLocked(elapsedRealtimeMs, uptimeMs, cur);
    }

    public void addHistoryEventLocked(long elapsedRealtimeMs, long uptimeMs, int code, String name, int uid) {
        this.mHistoryCur.eventCode = code;
        BatteryStats.HistoryItem historyItem = this.mHistoryCur;
        historyItem.eventTag = historyItem.localEventTag;
        this.mHistoryCur.eventTag.string = name;
        this.mHistoryCur.eventTag.uid = uid;
        addHistoryRecordLocked(elapsedRealtimeMs, uptimeMs);
    }

    void addHistoryRecordLocked(long elapsedRealtimeMs, long uptimeMs, byte cmd, BatteryStats.HistoryItem cur) {
        BatteryStats.HistoryItem rec = this.mHistoryCache;
        if (rec != null) {
            this.mHistoryCache = rec.next;
        } else {
            rec = new BatteryStats.HistoryItem();
        }
        rec.setTo(this.mHistoryBaseTimeMs + elapsedRealtimeMs, cmd, cur);
        addHistoryRecordLocked(rec);
    }

    void addHistoryRecordLocked(BatteryStats.HistoryItem rec) {
        this.mNumHistoryItems++;
        rec.next = null;
        BatteryStats.HistoryItem historyItem = this.mHistoryEnd;
        this.mHistoryLastEnd = historyItem;
        if (historyItem != null) {
            historyItem.next = rec;
            this.mHistoryEnd = rec;
            return;
        }
        this.mHistoryEnd = rec;
        this.mHistory = rec;
    }

    void clearHistoryLocked() {
        this.mHistoryBaseTimeMs = 0L;
        this.mLastHistoryElapsedRealtimeMs = 0L;
        this.mTrackRunningHistoryElapsedRealtimeMs = 0L;
        this.mTrackRunningHistoryUptimeMs = 0L;
        this.mHistoryBuffer.setDataSize(0);
        this.mHistoryBuffer.setDataPosition(0);
        this.mHistoryBuffer.setDataCapacity(this.mConstants.MAX_HISTORY_BUFFER / 2);
        this.mHistoryLastLastWritten.clear();
        this.mHistoryLastWritten.clear();
        this.mHistoryTagPool.clear();
        this.mNextHistoryTagIdx = 0;
        this.mNumHistoryTagChars = 0;
        this.mHistoryBufferLastPos = -1;
        this.mActiveHistoryStates = -1;
        this.mActiveHistoryStates2 = -1;
    }

    public void updateTimeBasesLocked(boolean unplugged, int screenState, long uptimeUs, long realtimeUs) {
        boolean screenOff = !Display.isOnState(screenState);
        boolean updateOnBatteryTimeBase = unplugged != this.mOnBatteryTimeBase.isRunning();
        boolean updateOnBatteryScreenOffTimeBase = (unplugged && screenOff) != this.mOnBatteryScreenOffTimeBase.isRunning();
        if (updateOnBatteryScreenOffTimeBase || updateOnBatteryTimeBase) {
            if (updateOnBatteryScreenOffTimeBase) {
                updateKernelWakelocksLocked(realtimeUs);
                updateBatteryPropertiesLocked();
            }
            if (updateOnBatteryTimeBase) {
                updateRpmStatsLocked(realtimeUs);
            }
            this.mOnBatteryTimeBase.setRunning(unplugged, uptimeUs, realtimeUs);
            if (updateOnBatteryTimeBase) {
                for (int i = this.mUidStats.size() - 1; i >= 0; i--) {
                    this.mUidStats.valueAt(i).updateOnBatteryBgTimeBase(uptimeUs, realtimeUs);
                }
            }
            if (updateOnBatteryScreenOffTimeBase) {
                this.mOnBatteryScreenOffTimeBase.setRunning(unplugged && screenOff, uptimeUs, realtimeUs);
                for (int i2 = this.mUidStats.size() - 1; i2 >= 0; i2--) {
                    this.mUidStats.valueAt(i2).updateOnBatteryScreenOffBgTimeBase(uptimeUs, realtimeUs);
                }
            }
        }
    }

    private void updateBatteryPropertiesLocked() {
        try {
            IBatteryPropertiesRegistrar registrar = IBatteryPropertiesRegistrar.Stub.asInterface(ServiceManager.getService("batteryproperties"));
            if (registrar != null) {
                registrar.scheduleUpdate();
            }
        } catch (RemoteException e) {
        }
    }

    public void addIsolatedUidLocked(int isolatedUid, int appUid) {
        addIsolatedUidLocked(isolatedUid, appUid, this.mClock.elapsedRealtime(), this.mClock.uptimeMillis());
    }

    public void addIsolatedUidLocked(int isolatedUid, int appUid, long elapsedRealtimeMs, long uptimeMs) {
        this.mIsolatedUids.put(isolatedUid, appUid);
        this.mIsolatedUidRefCounts.put(isolatedUid, 1);
        Uid u = getUidStatsLocked(appUid, elapsedRealtimeMs, uptimeMs);
        u.addIsolatedUid(isolatedUid);
    }

    public void scheduleRemoveIsolatedUidLocked(int isolatedUid, int appUid) {
        ExternalStatsSync externalStatsSync;
        int curUid = this.mIsolatedUids.get(isolatedUid, -1);
        if (curUid == appUid && (externalStatsSync = this.mExternalSync) != null) {
            externalStatsSync.scheduleCpuSyncDueToRemovedUid(isolatedUid);
        }
    }

    public boolean maybeRemoveIsolatedUidLocked(int isolatedUid, long elapsedRealtimeMs, long uptimeMs) {
        int refCount = this.mIsolatedUidRefCounts.get(isolatedUid, 0) - 1;
        if (refCount > 0) {
            this.mIsolatedUidRefCounts.put(isolatedUid, refCount);
            return false;
        }
        int idx = this.mIsolatedUids.indexOfKey(isolatedUid);
        if (idx >= 0) {
            int ownerUid = this.mIsolatedUids.valueAt(idx);
            Uid u = getUidStatsLocked(ownerUid, elapsedRealtimeMs, uptimeMs);
            u.removeIsolatedUid(isolatedUid);
            this.mIsolatedUids.removeAt(idx);
            this.mIsolatedUidRefCounts.delete(isolatedUid);
        } else {
            Slog.w(TAG, "Attempted to remove untracked isolated uid (" + isolatedUid + NavigationBarInflaterView.KEY_CODE_END);
        }
        this.mPendingRemovedUids.add(new UidToRemove(this, isolatedUid, elapsedRealtimeMs));
        return true;
    }

    public void incrementIsolatedUidRefCount(int uid) {
        int refCount = this.mIsolatedUidRefCounts.get(uid, 0);
        if (refCount <= 0) {
            Slog.w(TAG, "Attempted to increment ref counted of untracked isolated uid (" + uid + NavigationBarInflaterView.KEY_CODE_END);
        } else {
            this.mIsolatedUidRefCounts.put(uid, refCount + 1);
        }
    }

    private int mapUid(int uid) {
        if (Process.isSdkSandboxUid(uid)) {
            return Process.getAppUidForSdkSandboxUid(uid);
        }
        return mapIsolatedUid(uid);
    }

    private int mapIsolatedUid(int uid) {
        return this.mIsolatedUids.get(uid, uid);
    }

    public void noteEventLocked(int code, String name, int uid) {
        noteEventLocked(code, name, uid, this.mClock.elapsedRealtime(), this.mClock.uptimeMillis());
    }

    public void noteEventLocked(int code, String name, int uid, long elapsedRealtimeMs, long uptimeMs) {
        int uid2 = mapUid(uid);
        if (!this.mActiveEvents.updateState(code, name, uid2, 0)) {
            return;
        }
        addHistoryEventLocked(elapsedRealtimeMs, uptimeMs, code, name, uid2);
    }

    public void noteCurrentTimeChangedLocked() {
        long currentTime = this.mClock.currentTimeMillis();
        long elapsedRealtime = this.mClock.elapsedRealtime();
        long uptime = this.mClock.uptimeMillis();
        noteCurrentTimeChangedLocked(currentTime, elapsedRealtime, uptime);
    }

    public void noteCurrentTimeChangedLocked(long currentTimeMs, long elapsedRealtimeMs, long uptimeMs) {
        recordCurrentTimeChangeLocked(currentTimeMs, elapsedRealtimeMs, uptimeMs);
    }

    public void noteProcessStartLocked(String name, int uid) {
        noteProcessStartLocked(name, uid, this.mClock.elapsedRealtime(), this.mClock.uptimeMillis());
    }

    public void noteProcessStartLocked(String name, int uid, long elapsedRealtimeMs, long uptimeMs) {
        int uid2 = mapUid(uid);
        if (isOnBattery()) {
            Uid u = getUidStatsLocked(uid2, elapsedRealtimeMs, uptimeMs);
            u.getProcessStatsLocked(name).incStartsLocked();
        }
        if (!this.mActiveEvents.updateState(32769, name, uid2, 0) || !this.mRecordAllHistory) {
            return;
        }
        addHistoryEventLocked(elapsedRealtimeMs, uptimeMs, 32769, name, uid2);
    }

    public void noteProcessCrashLocked(String name, int uid) {
        noteProcessCrashLocked(name, uid, this.mClock.elapsedRealtime(), this.mClock.uptimeMillis());
    }

    public void noteProcessCrashLocked(String name, int uid, long elapsedRealtimeMs, long uptimeMs) {
        int uid2 = mapUid(uid);
        if (isOnBattery()) {
            Uid u = getUidStatsLocked(uid2, elapsedRealtimeMs, uptimeMs);
            u.getProcessStatsLocked(name).incNumCrashesLocked();
        }
    }

    public void noteProcessAnrLocked(String name, int uid) {
        noteProcessAnrLocked(name, uid, this.mClock.elapsedRealtime(), this.mClock.uptimeMillis());
    }

    public void noteProcessAnrLocked(String name, int uid, long elapsedRealtimeMs, long uptimeMs) {
        int uid2 = mapUid(uid);
        if (isOnBattery()) {
            Uid u = getUidStatsLocked(uid2, elapsedRealtimeMs, uptimeMs);
            u.getProcessStatsLocked(name).incNumAnrsLocked();
        }
    }

    public void noteUidProcessStateLocked(int uid, int state) {
        noteUidProcessStateLocked(uid, state, this.mClock.elapsedRealtime(), this.mClock.uptimeMillis());
    }

    public void noteUidProcessStateLocked(int uid, int state, long elapsedRealtimeMs, long uptimeMs) {
        int parentUid = mapUid(uid);
        if (uid != parentUid && Process.isIsolated(uid)) {
            return;
        }
        FrameworkStatsLog.write(27, uid, ActivityManager.processStateAmToProto(state));
        getUidStatsLocked(parentUid, elapsedRealtimeMs, uptimeMs).updateUidProcessStateLocked(state, elapsedRealtimeMs, uptimeMs);
    }

    public void noteProcessFinishLocked(String name, int uid) {
        noteProcessFinishLocked(name, uid, this.mClock.elapsedRealtime(), this.mClock.uptimeMillis());
    }

    public void noteProcessFinishLocked(String name, int uid, long elapsedRealtimeMs, long uptimeMs) {
        int uid2 = mapUid(uid);
        if (!this.mActiveEvents.updateState(16385, name, uid2, 0) || !this.mRecordAllHistory) {
            return;
        }
        addHistoryEventLocked(elapsedRealtimeMs, uptimeMs, 16385, name, uid2);
    }

    public void noteSyncStartLocked(String name, int uid) {
        noteSyncStartLocked(name, uid, this.mClock.elapsedRealtime(), this.mClock.uptimeMillis());
    }

    public void noteSyncStartLocked(String name, int uid, long elapsedRealtimeMs, long uptimeMs) {
        int uid2 = mapUid(uid);
        getUidStatsLocked(uid2, elapsedRealtimeMs, uptimeMs).noteStartSyncLocked(name, elapsedRealtimeMs);
        if (!this.mActiveEvents.updateState(32772, name, uid2, 0)) {
            return;
        }
        addHistoryEventLocked(elapsedRealtimeMs, uptimeMs, 32772, name, uid2);
    }

    public void noteSyncFinishLocked(String name, int uid) {
        noteSyncFinishLocked(name, uid, this.mClock.elapsedRealtime(), this.mClock.uptimeMillis());
    }

    public void noteSyncFinishLocked(String name, int uid, long elapsedRealtimeMs, long uptimeMs) {
        int uid2 = mapUid(uid);
        getUidStatsLocked(uid2, elapsedRealtimeMs, uptimeMs).noteStopSyncLocked(name, elapsedRealtimeMs);
        if (!this.mActiveEvents.updateState(16388, name, uid2, 0)) {
            return;
        }
        addHistoryEventLocked(elapsedRealtimeMs, uptimeMs, 16388, name, uid2);
    }

    public void noteJobStartLocked(String name, int uid) {
        noteJobStartLocked(name, uid, this.mClock.elapsedRealtime(), this.mClock.uptimeMillis());
    }

    public void noteJobStartLocked(String name, int uid, long elapsedRealtimeMs, long uptimeMs) {
        int uid2 = mapUid(uid);
        getUidStatsLocked(uid2, elapsedRealtimeMs, uptimeMs).noteStartJobLocked(name, elapsedRealtimeMs);
        if (!this.mActiveEvents.updateState(32774, name, uid2, 0)) {
            return;
        }
        addHistoryEventLocked(elapsedRealtimeMs, uptimeMs, 32774, name, uid2);
    }

    public void noteJobFinishLocked(String name, int uid, int stopReason) {
        noteJobFinishLocked(name, uid, stopReason, this.mClock.elapsedRealtime(), this.mClock.uptimeMillis());
    }

    public void noteJobFinishLocked(String name, int uid, int stopReason, long elapsedRealtimeMs, long uptimeMs) {
        int uid2 = mapUid(uid);
        getUidStatsLocked(uid2, elapsedRealtimeMs, uptimeMs).noteStopJobLocked(name, elapsedRealtimeMs, stopReason);
        if (!this.mActiveEvents.updateState(16390, name, uid2, 0)) {
            return;
        }
        addHistoryEventLocked(elapsedRealtimeMs, uptimeMs, 16390, name, uid2);
    }

    public void noteJobsDeferredLocked(int uid, int numDeferred, long sinceLast) {
        noteJobsDeferredLocked(uid, numDeferred, sinceLast, this.mClock.elapsedRealtime(), this.mClock.uptimeMillis());
    }

    public void noteJobsDeferredLocked(int uid, int numDeferred, long sinceLast, long elapsedRealtimeMs, long uptimeMs) {
        getUidStatsLocked(mapUid(uid), elapsedRealtimeMs, uptimeMs).noteJobsDeferredLocked(numDeferred, sinceLast);
    }

    public void noteAlarmStartLocked(String name, WorkSource workSource, int uid) {
        noteAlarmStartLocked(name, workSource, uid, this.mClock.elapsedRealtime(), this.mClock.uptimeMillis());
    }

    public void noteAlarmStartLocked(String name, WorkSource workSource, int uid, long elapsedRealtimeMs, long uptimeMs) {
        noteAlarmStartOrFinishLocked(BatteryStats.HistoryItem.EVENT_ALARM_START, name, workSource, uid, elapsedRealtimeMs, uptimeMs);
    }

    public void noteAlarmFinishLocked(String name, WorkSource workSource, int uid) {
        noteAlarmFinishLocked(name, workSource, uid, this.mClock.elapsedRealtime(), this.mClock.uptimeMillis());
    }

    public void noteAlarmFinishLocked(String name, WorkSource workSource, int uid, long elapsedRealtimeMs, long uptimeMs) {
        noteAlarmStartOrFinishLocked(16397, name, workSource, uid, elapsedRealtimeMs, uptimeMs);
    }

    private void noteAlarmStartOrFinishLocked(int historyItem, String name, WorkSource workSource, int uid, long elapsedRealtimeMs, long uptimeMs) {
        if (this.mRecordAllHistory) {
            if (workSource == null) {
                int uid2 = mapUid(uid);
                if (this.mActiveEvents.updateState(historyItem, name, uid2, 0)) {
                    addHistoryEventLocked(elapsedRealtimeMs, uptimeMs, historyItem, name, uid2);
                }
                return;
            }
            for (int i = 0; i < workSource.size(); i++) {
                int uid3 = mapUid(workSource.getUid(i));
                if (this.mActiveEvents.updateState(historyItem, name, uid3, 0)) {
                    addHistoryEventLocked(elapsedRealtimeMs, uptimeMs, historyItem, name, uid3);
                }
            }
            List<WorkSource.WorkChain> workChains = workSource.getWorkChains();
            if (workChains != null) {
                for (int i2 = 0; i2 < workChains.size(); i2++) {
                    int uid4 = mapUid(workChains.get(i2).getAttributionUid());
                    if (this.mActiveEvents.updateState(historyItem, name, uid4, 0)) {
                        addHistoryEventLocked(elapsedRealtimeMs, uptimeMs, historyItem, name, uid4);
                    }
                }
            }
        }
    }

    public void noteWakupAlarmLocked(String packageName, int uid, WorkSource workSource, String tag) {
        noteWakupAlarmLocked(packageName, uid, workSource, tag, this.mClock.elapsedRealtime(), this.mClock.uptimeMillis());
    }

    public void noteWakupAlarmLocked(String packageName, int uid, WorkSource workSource, String tag, long elapsedRealtimeMs, long uptimeMs) {
        if (workSource != null) {
            for (int i = 0; i < workSource.size(); i++) {
                int uid2 = workSource.getUid(i);
                String workSourceName = workSource.getPackageName(i);
                if (isOnBattery()) {
                    Uid.Pkg pkg = getPackageStatsLocked(uid2, workSourceName != null ? workSourceName : packageName, elapsedRealtimeMs, uptimeMs);
                    pkg.noteWakeupAlarmLocked(tag);
                }
            }
            List<WorkSource.WorkChain> workChains = workSource.getWorkChains();
            if (workChains != null) {
                for (int i2 = 0; i2 < workChains.size(); i2++) {
                    WorkSource.WorkChain wc = workChains.get(i2);
                    int uid3 = wc.getAttributionUid();
                    if (isOnBattery()) {
                        Uid.Pkg pkg2 = getPackageStatsLocked(uid3, packageName, elapsedRealtimeMs, uptimeMs);
                        pkg2.noteWakeupAlarmLocked(tag);
                    }
                }
                return;
            }
            return;
        }
        if (isOnBattery()) {
            Uid.Pkg pkg3 = getPackageStatsLocked(uid, packageName, elapsedRealtimeMs, uptimeMs);
            pkg3.noteWakeupAlarmLocked(tag);
        }
    }

    private void requestWakelockCpuUpdate() {
        this.mExternalSync.scheduleCpuSyncDueToWakelockChange(60000L);
    }

    private void requestImmediateCpuUpdate() {
        this.mExternalSync.scheduleCpuSyncDueToWakelockChange(0L);
    }

    public void setRecordAllHistoryLocked(boolean enabled) {
        this.mRecordAllHistory = enabled;
        if (enabled) {
            HashMap<String, SparseIntArray> active = this.mActiveEvents.getStateForEvent(1);
            if (active != null) {
                long mSecRealtime = this.mClock.elapsedRealtime();
                long mSecUptime = this.mClock.uptimeMillis();
                for (Map.Entry<String, SparseIntArray> ent : active.entrySet()) {
                    int j = 0;
                    for (SparseIntArray uids = ent.getValue(); j < uids.size(); uids = uids) {
                        addHistoryEventLocked(mSecRealtime, mSecUptime, 32769, ent.getKey(), uids.keyAt(j));
                        j++;
                    }
                }
                return;
            }
            return;
        }
        this.mActiveEvents.removeEvents(5);
        this.mActiveEvents.removeEvents(13);
        HashMap<String, SparseIntArray> active2 = this.mActiveEvents.getStateForEvent(1);
        if (active2 != null) {
            long mSecRealtime2 = this.mClock.elapsedRealtime();
            long mSecUptime2 = this.mClock.uptimeMillis();
            for (Map.Entry<String, SparseIntArray> ent2 : active2.entrySet()) {
                int j2 = 0;
                for (SparseIntArray uids2 = ent2.getValue(); j2 < uids2.size(); uids2 = uids2) {
                    addHistoryEventLocked(mSecRealtime2, mSecUptime2, 16385, ent2.getKey(), uids2.keyAt(j2));
                    j2++;
                }
            }
        }
    }

    public void setNoAutoReset(boolean enabled) {
        this.mNoAutoReset = enabled;
    }

    public void setPretendScreenOff(boolean pretendScreenOff) {
        if (this.mPretendScreenOff != pretendScreenOff) {
            this.mPretendScreenOff = pretendScreenOff;
            int primaryScreenState = this.mPerDisplayBatteryStats[0].screenState;
            noteScreenStateLocked(0, primaryScreenState, this.mClock.elapsedRealtime(), this.mClock.uptimeMillis(), this.mClock.currentTimeMillis());
        }
    }

    public void noteStartWakeLocked(int uid, int pid, WorkSource.WorkChain wc, String name, String historyName, int type, boolean unimportantForLogging) {
        noteStartWakeLocked(uid, pid, wc, name, historyName, type, unimportantForLogging, this.mClock.elapsedRealtime(), this.mClock.uptimeMillis());
    }

    public void noteStartWakeLocked(int uid, int pid, WorkSource.WorkChain wc, String name, String historyName, int type, boolean unimportantForLogging, long elapsedRealtimeMs, long uptimeMs) {
        String historyName2;
        int mappedUid = mapUid(uid);
        if (type == 0) {
            aggregateLastWakeupUptimeLocked(elapsedRealtimeMs, uptimeMs);
            if (historyName != null) {
                historyName2 = historyName;
            } else {
                historyName2 = name;
            }
            if (this.mRecordAllHistory && this.mActiveEvents.updateState(32773, historyName2, mappedUid, 0)) {
                addHistoryEventLocked(elapsedRealtimeMs, uptimeMs, 32773, historyName2, mappedUid);
            }
            if (this.mWakeLockNesting == 0) {
                this.mHistoryCur.states |= 1073741824;
                BatteryStats.HistoryItem historyItem = this.mHistoryCur;
                historyItem.wakelockTag = historyItem.localWakelockTag;
                BatteryStats.HistoryTag historyTag = this.mHistoryCur.wakelockTag;
                this.mInitialAcquireWakeName = historyName2;
                historyTag.string = historyName2;
                BatteryStats.HistoryTag historyTag2 = this.mHistoryCur.wakelockTag;
                this.mInitialAcquireWakeUid = mappedUid;
                historyTag2.uid = mappedUid;
                this.mWakeLockImportant = !unimportantForLogging;
                addHistoryRecordLocked(elapsedRealtimeMs, uptimeMs);
            } else if (!this.mWakeLockImportant && !unimportantForLogging && this.mHistoryLastWritten.cmd == 0) {
                if (this.mHistoryLastWritten.wakelockTag != null) {
                    this.mHistoryLastWritten.wakelockTag = null;
                    BatteryStats.HistoryItem historyItem2 = this.mHistoryCur;
                    historyItem2.wakelockTag = historyItem2.localWakelockTag;
                    BatteryStats.HistoryTag historyTag3 = this.mHistoryCur.wakelockTag;
                    this.mInitialAcquireWakeName = historyName2;
                    historyTag3.string = historyName2;
                    BatteryStats.HistoryTag historyTag4 = this.mHistoryCur.wakelockTag;
                    this.mInitialAcquireWakeUid = mappedUid;
                    historyTag4.uid = mappedUid;
                    addHistoryRecordLocked(elapsedRealtimeMs, uptimeMs);
                }
                this.mWakeLockImportant = true;
            }
            this.mWakeLockNesting++;
        }
        if (mappedUid >= 0) {
            if (mappedUid != uid) {
                incrementIsolatedUidRefCount(uid);
            }
            if (this.mOnBatteryScreenOffTimeBase.isRunning()) {
                requestWakelockCpuUpdate();
            }
            getUidStatsLocked(mappedUid, elapsedRealtimeMs, uptimeMs).noteStartWakeLocked(pid, name, type, elapsedRealtimeMs);
            if (wc != null) {
                FrameworkStatsLog.write(10, wc.getUids(), wc.getTags(), getPowerManagerWakeLockLevel(type), name, 1);
            } else {
                FrameworkStatsLog.write_non_chained(10, mapIsolatedUid(uid), (String) null, getPowerManagerWakeLockLevel(type), name, 1);
            }
        }
    }

    public void noteStopWakeLocked(int uid, int pid, WorkSource.WorkChain wc, String name, String historyName, int type) {
        noteStopWakeLocked(uid, pid, wc, name, historyName, type, this.mClock.elapsedRealtime(), this.mClock.uptimeMillis());
    }

    public void noteStopWakeLocked(int uid, int pid, WorkSource.WorkChain wc, String name, String historyName, int type, long elapsedRealtimeMs, long uptimeMs) {
        String historyName2;
        int mappedUid = mapUid(uid);
        if (type == 0) {
            this.mWakeLockNesting--;
            if (this.mRecordAllHistory) {
                if (historyName != null) {
                    historyName2 = historyName;
                } else {
                    historyName2 = name;
                }
                if (this.mActiveEvents.updateState(16389, historyName2, mappedUid, 0)) {
                    addHistoryEventLocked(elapsedRealtimeMs, uptimeMs, 16389, historyName2, mappedUid);
                }
            }
            if (this.mWakeLockNesting == 0) {
                this.mHistoryCur.states &= -1073741825;
                this.mInitialAcquireWakeName = null;
                this.mInitialAcquireWakeUid = -1;
                addHistoryRecordLocked(elapsedRealtimeMs, uptimeMs);
            }
        }
        if (mappedUid >= 0) {
            if (this.mOnBatteryScreenOffTimeBase.isRunning()) {
                requestWakelockCpuUpdate();
            }
            getUidStatsLocked(mappedUid, elapsedRealtimeMs, uptimeMs).noteStopWakeLocked(pid, name, type, elapsedRealtimeMs);
            if (wc != null) {
                FrameworkStatsLog.write(10, wc.getUids(), wc.getTags(), getPowerManagerWakeLockLevel(type), name, 0);
            } else {
                FrameworkStatsLog.write_non_chained(10, mapIsolatedUid(uid), (String) null, getPowerManagerWakeLockLevel(type), name, 0);
            }
            if (mappedUid != uid) {
                maybeRemoveIsolatedUidLocked(uid, elapsedRealtimeMs, uptimeMs);
            }
        }
    }

    private int getPowerManagerWakeLockLevel(int battertStatsWakelockType) {
        switch (battertStatsWakelockType) {
            case 0:
                return 1;
            case 1:
                return 26;
            case 2:
                Slog.e(TAG, "Illegal window wakelock type observed in batterystats.");
                return -1;
            case 18:
                return 128;
            default:
                Slog.e(TAG, "Illegal wakelock type in batterystats: " + battertStatsWakelockType);
                return -1;
        }
    }

    public void noteStartWakeFromSourceLocked(WorkSource ws, int pid, String name, String historyName, int type, boolean unimportantForLogging) {
        noteStartWakeFromSourceLocked(ws, pid, name, historyName, type, unimportantForLogging, this.mClock.elapsedRealtime(), this.mClock.uptimeMillis());
    }

    public void noteStartWakeFromSourceLocked(WorkSource ws, int pid, String name, String historyName, int type, boolean unimportantForLogging, long elapsedRealtimeMs, long uptimeMs) {
        int N = ws.size();
        for (int i = 0; i < N; i++) {
            noteStartWakeLocked(ws.getUid(i), pid, null, name, historyName, type, unimportantForLogging, elapsedRealtimeMs, uptimeMs);
        }
        List<WorkSource.WorkChain> wcs = ws.getWorkChains();
        if (wcs != null) {
            for (int i2 = 0; i2 < wcs.size(); i2++) {
                WorkSource.WorkChain wc = wcs.get(i2);
                noteStartWakeLocked(wc.getAttributionUid(), pid, wc, name, historyName, type, unimportantForLogging, elapsedRealtimeMs, uptimeMs);
            }
        }
    }

    public void noteChangeWakelockFromSourceLocked(WorkSource ws, int pid, String name, String historyName, int type, WorkSource newWs, int newPid, String newName, String newHistoryName, int newType, boolean newUnimportantForLogging) {
        noteChangeWakelockFromSourceLocked(ws, pid, name, historyName, type, newWs, newPid, newName, newHistoryName, newType, newUnimportantForLogging, this.mClock.elapsedRealtime(), this.mClock.uptimeMillis());
    }

    public void noteChangeWakelockFromSourceLocked(WorkSource ws, int pid, String name, String historyName, int type, WorkSource newWs, int newPid, String newName, String newHistoryName, int newType, boolean newUnimportantForLogging, long elapsedRealtimeMs, long uptimeMs) {
        List<WorkSource.WorkChain> goneChains;
        List<WorkSource.WorkChain> newChains;
        List<WorkSource.WorkChain>[] wcs = WorkSource.diffChains(ws, newWs);
        int NN = newWs.size();
        for (int i = 0; i < NN; i++) {
            noteStartWakeLocked(newWs.getUid(i), newPid, null, newName, newHistoryName, newType, newUnimportantForLogging, elapsedRealtimeMs, uptimeMs);
        }
        if (wcs != null && (newChains = wcs[0]) != null) {
            for (int i2 = 0; i2 < newChains.size(); i2++) {
                WorkSource.WorkChain newChain = newChains.get(i2);
                noteStartWakeLocked(newChain.getAttributionUid(), newPid, newChain, newName, newHistoryName, newType, newUnimportantForLogging, elapsedRealtimeMs, uptimeMs);
            }
        }
        int NO = ws.size();
        for (int i3 = 0; i3 < NO; i3++) {
            noteStopWakeLocked(ws.getUid(i3), pid, null, name, historyName, type, elapsedRealtimeMs, uptimeMs);
        }
        if (wcs != null && (goneChains = wcs[1]) != null) {
            for (int i4 = 0; i4 < goneChains.size(); i4++) {
                WorkSource.WorkChain goneChain = goneChains.get(i4);
                noteStopWakeLocked(goneChain.getAttributionUid(), pid, goneChain, name, historyName, type, elapsedRealtimeMs, uptimeMs);
            }
        }
    }

    public void noteStopWakeFromSourceLocked(WorkSource ws, int pid, String name, String historyName, int type) {
        noteStopWakeFromSourceLocked(ws, pid, name, historyName, type, this.mClock.elapsedRealtime(), this.mClock.uptimeMillis());
    }

    public void noteStopWakeFromSourceLocked(WorkSource ws, int pid, String name, String historyName, int type, long elapsedRealtimeMs, long uptimeMs) {
        int N = ws.size();
        for (int i = 0; i < N; i++) {
            noteStopWakeLocked(ws.getUid(i), pid, null, name, historyName, type, elapsedRealtimeMs, uptimeMs);
        }
        List<WorkSource.WorkChain> wcs = ws.getWorkChains();
        if (wcs != null) {
            for (int i2 = 0; i2 < wcs.size(); i2++) {
                WorkSource.WorkChain wc = wcs.get(i2);
                noteStopWakeLocked(wc.getAttributionUid(), pid, wc, name, historyName, type, elapsedRealtimeMs, uptimeMs);
            }
        }
    }

    public void noteLongPartialWakelockStart(String name, String historyName, int uid) {
        noteLongPartialWakelockStart(name, historyName, uid, this.mClock.elapsedRealtime(), this.mClock.uptimeMillis());
    }

    public void noteLongPartialWakelockStart(String name, String historyName, int uid, long elapsedRealtimeMs, long uptimeMs) {
        noteLongPartialWakeLockStartInternal(name, historyName, uid, elapsedRealtimeMs, uptimeMs);
    }

    public void noteLongPartialWakelockStartFromSource(String name, String historyName, WorkSource workSource) {
        noteLongPartialWakelockStartFromSource(name, historyName, workSource, this.mClock.elapsedRealtime(), this.mClock.uptimeMillis());
    }

    public void noteLongPartialWakelockStartFromSource(String name, String historyName, WorkSource workSource, long elapsedRealtimeMs, long uptimeMs) {
        int N = workSource.size();
        for (int i = 0; i < N; i++) {
            int uid = mapUid(workSource.getUid(i));
            noteLongPartialWakeLockStartInternal(name, historyName, uid, elapsedRealtimeMs, uptimeMs);
        }
        List<WorkSource.WorkChain> workChains = workSource.getWorkChains();
        if (workChains != null) {
            for (int i2 = 0; i2 < workChains.size(); i2++) {
                WorkSource.WorkChain workChain = workChains.get(i2);
                int uid2 = workChain.getAttributionUid();
                noteLongPartialWakeLockStartInternal(name, historyName, uid2, elapsedRealtimeMs, uptimeMs);
            }
        }
    }

    private void noteLongPartialWakeLockStartInternal(String name, String historyName, int uid, long elapsedRealtimeMs, long uptimeMs) {
        String historyName2;
        int mappedUid = mapUid(uid);
        if (historyName != null) {
            historyName2 = historyName;
        } else {
            historyName2 = name;
        }
        if (!this.mActiveEvents.updateState(BatteryStats.HistoryItem.EVENT_LONG_WAKE_LOCK_START, historyName2, mappedUid, 0)) {
            return;
        }
        addHistoryEventLocked(elapsedRealtimeMs, uptimeMs, BatteryStats.HistoryItem.EVENT_LONG_WAKE_LOCK_START, historyName2, mappedUid);
        if (mappedUid != uid) {
            incrementIsolatedUidRefCount(uid);
        }
    }

    public void noteLongPartialWakelockFinish(String name, String historyName, int uid) {
        noteLongPartialWakelockFinish(name, historyName, uid, this.mClock.elapsedRealtime(), this.mClock.uptimeMillis());
    }

    public void noteLongPartialWakelockFinish(String name, String historyName, int uid, long elapsedRealtimeMs, long uptimeMs) {
        noteLongPartialWakeLockFinishInternal(name, historyName, uid, elapsedRealtimeMs, uptimeMs);
    }

    public void noteLongPartialWakelockFinishFromSource(String name, String historyName, WorkSource workSource) {
        noteLongPartialWakelockFinishFromSource(name, historyName, workSource, this.mClock.elapsedRealtime(), this.mClock.uptimeMillis());
    }

    public void noteLongPartialWakelockFinishFromSource(String name, String historyName, WorkSource workSource, long elapsedRealtimeMs, long uptimeMs) {
        int N = workSource.size();
        for (int i = 0; i < N; i++) {
            int uid = mapUid(workSource.getUid(i));
            noteLongPartialWakeLockFinishInternal(name, historyName, uid, elapsedRealtimeMs, uptimeMs);
        }
        List<WorkSource.WorkChain> workChains = workSource.getWorkChains();
        if (workChains != null) {
            for (int i2 = 0; i2 < workChains.size(); i2++) {
                WorkSource.WorkChain workChain = workChains.get(i2);
                int uid2 = workChain.getAttributionUid();
                noteLongPartialWakeLockFinishInternal(name, historyName, uid2, elapsedRealtimeMs, uptimeMs);
            }
        }
    }

    private void noteLongPartialWakeLockFinishInternal(String name, String historyName, int uid, long elapsedRealtimeMs, long uptimeMs) {
        String historyName2;
        int mappedUid = mapUid(uid);
        if (historyName != null) {
            historyName2 = historyName;
        } else {
            historyName2 = name;
        }
        if (!this.mActiveEvents.updateState(BatteryStats.HistoryItem.EVENT_LONG_WAKE_LOCK_FINISH, historyName2, mappedUid, 0)) {
            return;
        }
        addHistoryEventLocked(elapsedRealtimeMs, uptimeMs, BatteryStats.HistoryItem.EVENT_LONG_WAKE_LOCK_FINISH, historyName2, mappedUid);
        if (mappedUid != uid) {
            maybeRemoveIsolatedUidLocked(uid, elapsedRealtimeMs, uptimeMs);
        }
    }

    void aggregateLastWakeupUptimeLocked(long elapsedRealtimeMs, long uptimeMs) {
        String str = this.mLastWakeupReason;
        if (str != null) {
            long deltaUptimeMs = uptimeMs - this.mLastWakeupUptimeMs;
            SamplingTimer timer = getWakeupReasonTimerLocked(str);
            timer.add(deltaUptimeMs * 1000, 1, elapsedRealtimeMs);
            FrameworkStatsLog.write(36, this.mLastWakeupReason, 1000 * deltaUptimeMs);
            this.mLastWakeupReason = null;
        }
    }

    public void noteWakeupReasonLocked(String reason) {
        noteWakeupReasonLocked(reason, this.mClock.elapsedRealtime(), this.mClock.uptimeMillis());
    }

    public void noteWakeupReasonLocked(String reason, long elapsedRealtimeMs, long uptimeMs) {
        aggregateLastWakeupUptimeLocked(elapsedRealtimeMs, uptimeMs);
        BatteryStats.HistoryItem historyItem = this.mHistoryCur;
        historyItem.wakeReasonTag = historyItem.localWakeReasonTag;
        this.mHistoryCur.wakeReasonTag.string = reason;
        this.mHistoryCur.wakeReasonTag.uid = 0;
        this.mLastWakeupReason = reason;
        this.mLastWakeupUptimeMs = uptimeMs;
        addHistoryRecordLocked(elapsedRealtimeMs, uptimeMs);
    }

    public boolean startAddingCpuLocked() {
        this.mExternalSync.cancelCpuSyncDueToWakelockChange();
        return this.mOnBatteryInternal;
    }

    public void finishAddingCpuLocked(int totalUTimeMs, int totalSTimeMs, int statUserTimeMs, int statSystemTimeMs, int statIOWaitTimeMs, int statIrqTimeMs, int statSoftIrqTimeMs, int statIdleTimeMs) {
        this.mCurStepCpuUserTimeMs += totalUTimeMs;
        this.mCurStepCpuSystemTimeMs += totalSTimeMs;
        this.mCurStepStatUserTimeMs += statUserTimeMs;
        this.mCurStepStatSystemTimeMs += statSystemTimeMs;
        this.mCurStepStatIOWaitTimeMs += statIOWaitTimeMs;
        this.mCurStepStatIrqTimeMs += statIrqTimeMs;
        this.mCurStepStatSoftIrqTimeMs += statSoftIrqTimeMs;
        this.mCurStepStatIdleTimeMs += statIdleTimeMs;
    }

    public void noteProcessDiedLocked(int uid, int pid) {
        Uid u = this.mUidStats.get(mapUid(uid));
        if (u != null) {
            u.mPids.remove(pid);
        }
    }

    public long getProcessWakeTime(int uid, int pid, long realtimeMs) {
        BatteryStats.Uid.Pid p;
        Uid u = this.mUidStats.get(mapUid(uid));
        if (u == null || (p = u.mPids.get(pid)) == null) {
            return 0L;
        }
        return p.mWakeSumMs + (p.mWakeNesting > 0 ? realtimeMs - p.mWakeStartMs : 0L);
    }

    public void reportExcessiveCpuLocked(int uid, String proc, long overTimeMs, long usedTimeMs) {
        Uid u = this.mUidStats.get(mapUid(uid));
        if (u != null) {
            u.reportExcessiveCpuLocked(proc, overTimeMs, usedTimeMs);
        }
    }

    public void noteStartSensorLocked(int uid, int sensor) {
        noteStartSensorLocked(uid, sensor, this.mClock.elapsedRealtime(), this.mClock.uptimeMillis());
    }

    public void noteStartSensorLocked(int uid, int sensor, long elapsedRealtimeMs, long uptimeMs) {
        int uid2 = mapUid(uid);
        if (this.mSensorNesting == 0) {
            this.mHistoryCur.states |= 8388608;
            addHistoryRecordLocked(elapsedRealtimeMs, uptimeMs);
        }
        this.mSensorNesting++;
        getUidStatsLocked(uid2, elapsedRealtimeMs, uptimeMs).noteStartSensor(sensor, elapsedRealtimeMs);
    }

    public void noteStopSensorLocked(int uid, int sensor) {
        noteStopSensorLocked(uid, sensor, this.mClock.elapsedRealtime(), this.mClock.uptimeMillis());
    }

    public void noteStopSensorLocked(int uid, int sensor, long elapsedRealtimeMs, long uptimeMs) {
        int uid2 = mapUid(uid);
        int i = this.mSensorNesting - 1;
        this.mSensorNesting = i;
        if (i == 0) {
            this.mHistoryCur.states &= -8388609;
            addHistoryRecordLocked(elapsedRealtimeMs, uptimeMs);
        }
        getUidStatsLocked(uid2, elapsedRealtimeMs, uptimeMs).noteStopSensor(sensor, elapsedRealtimeMs);
    }

    public void noteGpsChangedLocked(WorkSource oldWs, WorkSource newWs) {
        noteGpsChangedLocked(oldWs, newWs, this.mClock.elapsedRealtime(), this.mClock.uptimeMillis());
    }

    public void noteGpsChangedLocked(WorkSource oldWs, WorkSource newWs, long elapsedRealtimeMs, long uptimeMs) {
        for (int i = 0; i < newWs.size(); i++) {
            noteStartGpsLocked(newWs.getUid(i), null, elapsedRealtimeMs, uptimeMs);
        }
        for (int i2 = 0; i2 < oldWs.size(); i2++) {
            noteStopGpsLocked(oldWs.getUid(i2), null, elapsedRealtimeMs, uptimeMs);
        }
        List<WorkSource.WorkChain>[] wcs = WorkSource.diffChains(oldWs, newWs);
        if (wcs != null) {
            if (wcs[0] != null) {
                List<WorkSource.WorkChain> newChains = wcs[0];
                for (int i3 = 0; i3 < newChains.size(); i3++) {
                    noteStartGpsLocked(-1, newChains.get(i3), elapsedRealtimeMs, uptimeMs);
                }
            }
            if (wcs[1] != null) {
                List<WorkSource.WorkChain> goneChains = wcs[1];
                for (int i4 = 0; i4 < goneChains.size(); i4++) {
                    noteStopGpsLocked(-1, goneChains.get(i4), elapsedRealtimeMs, uptimeMs);
                }
            }
        }
    }

    private void noteStartGpsLocked(int uid, WorkSource.WorkChain workChain, long elapsedRealtimeMs, long uptimeMs) {
        if (workChain != null) {
            uid = workChain.getAttributionUid();
        }
        int mappedUid = mapUid(uid);
        if (this.mGpsNesting == 0) {
            this.mHistoryCur.states |= 536870912;
            addHistoryRecordLocked(elapsedRealtimeMs, uptimeMs);
        }
        this.mGpsNesting++;
        if (workChain == null) {
            FrameworkStatsLog.write_non_chained(6, mapIsolatedUid(uid), null, 1);
        } else {
            FrameworkStatsLog.write(6, workChain.getUids(), workChain.getTags(), 1);
        }
        getUidStatsLocked(mappedUid, elapsedRealtimeMs, uptimeMs).noteStartGps(elapsedRealtimeMs);
    }

    private void noteStopGpsLocked(int uid, WorkSource.WorkChain workChain, long elapsedRealtimeMs, long uptimeMs) {
        if (workChain != null) {
            uid = workChain.getAttributionUid();
        }
        int mappedUid = mapUid(uid);
        int i = this.mGpsNesting - 1;
        this.mGpsNesting = i;
        if (i == 0) {
            this.mHistoryCur.states &= -536870913;
            addHistoryRecordLocked(elapsedRealtimeMs, uptimeMs);
            stopAllGpsSignalQualityTimersLocked(-1, elapsedRealtimeMs);
            this.mGpsSignalQualityBin = -1;
        }
        if (workChain == null) {
            FrameworkStatsLog.write_non_chained(6, mapIsolatedUid(uid), null, 0);
        } else {
            FrameworkStatsLog.write(6, workChain.getUids(), workChain.getTags(), 0);
        }
        getUidStatsLocked(mappedUid, elapsedRealtimeMs, uptimeMs).noteStopGps(elapsedRealtimeMs);
    }

    public void noteGpsSignalQualityLocked(int signalLevel) {
        noteGpsSignalQualityLocked(signalLevel, this.mClock.elapsedRealtime(), this.mClock.uptimeMillis());
    }

    public void noteGpsSignalQualityLocked(int signalLevel, long elapsedRealtimeMs, long uptimeMs) {
        if (this.mGpsNesting == 0) {
            return;
        }
        if (signalLevel >= 0) {
            StopwatchTimer[] stopwatchTimerArr = this.mGpsSignalQualityTimer;
            if (signalLevel < stopwatchTimerArr.length) {
                int i = this.mGpsSignalQualityBin;
                if (i != signalLevel) {
                    if (i >= 0) {
                        stopwatchTimerArr[i].stopRunningLocked(elapsedRealtimeMs);
                    }
                    if (!this.mGpsSignalQualityTimer[signalLevel].isRunningLocked()) {
                        this.mGpsSignalQualityTimer[signalLevel].startRunningLocked(elapsedRealtimeMs);
                    }
                    BatteryStats.HistoryItem historyItem = this.mHistoryCur;
                    historyItem.states2 = (historyItem.states2 & (-129)) | (signalLevel << 7);
                    addHistoryRecordLocked(elapsedRealtimeMs, uptimeMs);
                    this.mGpsSignalQualityBin = signalLevel;
                    return;
                }
                return;
            }
        }
        stopAllGpsSignalQualityTimersLocked(-1, elapsedRealtimeMs);
    }

    public void noteScreenStateLocked(int display, int state) {
        noteScreenStateLocked(display, state, this.mClock.elapsedRealtime(), this.mClock.uptimeMillis(), this.mClock.currentTimeMillis());
    }

    public void noteScreenStateLocked(int display, int displayState, long elapsedRealtimeMs, long uptimeMs, long currentTimeMs) {
        int displayState2;
        int state;
        int overallBin;
        MeasuredEnergyStats measuredEnergyStats;
        boolean shouldScheduleSync;
        int externalUpdateFlag;
        boolean updateHistory;
        int oldState;
        int state2;
        if (displayState <= 4) {
            displayState2 = displayState;
        } else if (Display.isOnState(displayState)) {
            displayState2 = 2;
        } else if (Display.isDozeState(displayState)) {
            if (Display.isSuspendedState(displayState)) {
                displayState2 = 4;
            } else {
                displayState2 = 3;
            }
        } else if (Display.isOffState(displayState)) {
            displayState2 = 1;
        } else {
            Slog.wtf(TAG, "Unknown screen state (not mapped): " + displayState);
            displayState2 = 0;
        }
        int overallBin2 = this.mScreenBrightnessBin;
        int externalUpdateFlag2 = 0;
        boolean shouldScheduleSync2 = false;
        DisplayBatteryStats[] displayBatteryStatsArr = this.mPerDisplayBatteryStats;
        int numDisplay = displayBatteryStatsArr.length;
        if (display >= 0 && display < numDisplay) {
            DisplayBatteryStats displayStats = displayBatteryStatsArr[display];
            int oldDisplayState = displayStats.screenState;
            if (oldDisplayState == displayState2) {
                state = this.mScreenState;
                overallBin = overallBin2;
            } else {
                displayStats.screenState = displayState2;
                switch (oldDisplayState) {
                    case 0:
                    case 1:
                        break;
                    case 2:
                        displayStats.screenOnTimer.stopRunningLocked(elapsedRealtimeMs);
                        int bin = displayStats.screenBrightnessBin;
                        if (bin >= 0) {
                            displayStats.screenBrightnessTimers[bin].stopRunningLocked(elapsedRealtimeMs);
                        }
                        overallBin2 = evaluateOverallScreenBrightnessBinLocked();
                        shouldScheduleSync2 = true;
                        break;
                    case 3:
                        if (displayState2 != 4) {
                            displayStats.screenDozeTimer.stopRunningLocked(elapsedRealtimeMs);
                            shouldScheduleSync2 = true;
                            break;
                        }
                        break;
                    case 4:
                        if (displayState2 != 3) {
                            displayStats.screenDozeTimer.stopRunningLocked(elapsedRealtimeMs);
                            shouldScheduleSync2 = true;
                            break;
                        }
                        break;
                    default:
                        Slog.wtf(TAG, "Attempted to stop timer for unexpected display state " + display);
                        break;
                }
                switch (displayState2) {
                    case 0:
                    case 1:
                        break;
                    case 2:
                        displayStats.screenOnTimer.startRunningLocked(elapsedRealtimeMs);
                        int bin2 = displayStats.screenBrightnessBin;
                        if (bin2 >= 0) {
                            displayStats.screenBrightnessTimers[bin2].startRunningLocked(elapsedRealtimeMs);
                        }
                        overallBin2 = evaluateOverallScreenBrightnessBinLocked();
                        shouldScheduleSync2 = true;
                        break;
                    case 3:
                        if (oldDisplayState != 4) {
                            displayStats.screenDozeTimer.startRunningLocked(elapsedRealtimeMs);
                            shouldScheduleSync2 = true;
                            break;
                        }
                        break;
                    case 4:
                        if (oldDisplayState != 3) {
                            displayStats.screenDozeTimer.startRunningLocked(elapsedRealtimeMs);
                            shouldScheduleSync2 = true;
                            break;
                        }
                        break;
                    default:
                        Slog.wtf(TAG, "Attempted to start timer for unexpected display state " + displayState2 + " for display " + display);
                        break;
                }
                if (shouldScheduleSync2 && (measuredEnergyStats = this.mGlobalMeasuredEnergyStats) != null && measuredEnergyStats.isStandardBucketSupported(0)) {
                    externalUpdateFlag2 = 0 | 32;
                }
                state = 0;
                int i = 0;
                while (i < numDisplay) {
                    int overallBin3 = overallBin2;
                    int tempState = this.mPerDisplayBatteryStats[i].screenState;
                    int externalUpdateFlag3 = externalUpdateFlag2;
                    if (tempState == 2 || state == 2) {
                        state = 2;
                    } else if (tempState == 3 || state == 3) {
                        state = 3;
                    } else if (tempState == 4 || state == 4) {
                        state = 4;
                    } else if (tempState == 1 || state == 1) {
                        state = 1;
                    }
                    i++;
                    overallBin2 = overallBin3;
                    externalUpdateFlag2 = externalUpdateFlag3;
                }
                overallBin = overallBin2;
            }
            boolean batteryRunning = this.mOnBatteryTimeBase.isRunning();
            boolean batteryScreenOffRunning = this.mOnBatteryScreenOffTimeBase.isRunning();
            int state3 = this.mPretendScreenOff ? 1 : state;
            if (this.mScreenState != state3) {
                recordDailyStatsIfNeededLocked(true, currentTimeMs);
                int oldState2 = this.mScreenState;
                this.mScreenState = state3;
                if (state3 != 0) {
                    int stepState = state3 - 1;
                    if ((stepState & 3) == stepState) {
                        int i2 = this.mModStepMode;
                        int i3 = this.mCurStepMode;
                        this.mModStepMode = i2 | ((i3 & 3) ^ stepState);
                        this.mCurStepMode = (i3 & (-4)) | stepState;
                    } else {
                        Slog.wtf(TAG, "Unexpected screen state: " + state3);
                    }
                }
                boolean updateHistory2 = false;
                if (Display.isDozeState(state3) && !Display.isDozeState(oldState2)) {
                    this.mHistoryCur.states |= 262144;
                    this.mScreenDozeTimer.startRunningLocked(elapsedRealtimeMs);
                    updateHistory2 = true;
                } else if (Display.isDozeState(oldState2) && !Display.isDozeState(state3)) {
                    this.mHistoryCur.states &= -262145;
                    this.mScreenDozeTimer.stopRunningLocked(elapsedRealtimeMs);
                    updateHistory2 = true;
                }
                if (Display.isOnState(state3)) {
                    this.mHistoryCur.states |= 1048576;
                    this.mScreenOnTimer.startRunningLocked(elapsedRealtimeMs);
                    int i4 = this.mScreenBrightnessBin;
                    if (i4 >= 0) {
                        this.mScreenBrightnessTimer[i4].startRunningLocked(elapsedRealtimeMs);
                    }
                    updateHistory = true;
                } else if (!Display.isOnState(oldState2)) {
                    updateHistory = updateHistory2;
                } else {
                    this.mHistoryCur.states &= -1048577;
                    this.mScreenOnTimer.stopRunningLocked(elapsedRealtimeMs);
                    int i5 = this.mScreenBrightnessBin;
                    if (i5 >= 0) {
                        this.mScreenBrightnessTimer[i5].stopRunningLocked(elapsedRealtimeMs);
                    }
                    updateHistory = true;
                }
                if (updateHistory) {
                    addHistoryRecordLocked(elapsedRealtimeMs, uptimeMs);
                }
                int externalUpdateFlag4 = externalUpdateFlag2 | 1;
                if (Display.isOnState(state3)) {
                    oldState = oldState2;
                    state2 = state3;
                    updateTimeBasesLocked(this.mOnBatteryTimeBase.isRunning(), state3, uptimeMs * 1000, 1000 * elapsedRealtimeMs);
                    noteStartWakeLocked(-1, -1, null, "screen", null, 0, false, elapsedRealtimeMs, uptimeMs);
                } else {
                    oldState = oldState2;
                    state2 = state3;
                    if (Display.isOnState(oldState)) {
                        noteStopWakeLocked(-1, -1, null, "screen", "screen", 0, elapsedRealtimeMs, uptimeMs);
                        updateTimeBasesLocked(this.mOnBatteryTimeBase.isRunning(), state2, uptimeMs * 1000, elapsedRealtimeMs * 1000);
                    }
                }
                if (this.mOnBatteryInternal) {
                    state3 = state2;
                    updateDischargeScreenLevelsLocked(oldState, state3);
                } else {
                    state3 = state2;
                }
                externalUpdateFlag = externalUpdateFlag4;
                shouldScheduleSync = true;
            } else {
                shouldScheduleSync = shouldScheduleSync2;
                externalUpdateFlag = externalUpdateFlag2;
            }
            maybeUpdateOverallScreenBrightness(overallBin, elapsedRealtimeMs, uptimeMs);
            if (shouldScheduleSync) {
                int numDisplays = this.mPerDisplayBatteryStats.length;
                int[] displayStates = new int[numDisplays];
                for (int i6 = 0; i6 < numDisplays; i6++) {
                    displayStates[i6] = this.mPerDisplayBatteryStats[i6].screenState;
                }
                this.mExternalSync.scheduleSyncDueToScreenStateChange(externalUpdateFlag, batteryRunning, batteryScreenOffRunning, state3, displayStates);
                return;
            }
            return;
        }
        Slog.wtf(TAG, "Unexpected note screen state for display " + display + " (only " + this.mPerDisplayBatteryStats.length + " displays exist...)");
    }

    public void noteScreenBrightnessLocked(int brightness) {
        noteScreenBrightnessLocked(0, brightness);
    }

    public void noteScreenBrightnessLocked(int display, int brightness) {
        noteScreenBrightnessLocked(display, brightness, this.mClock.elapsedRealtime(), this.mClock.uptimeMillis());
    }

    public void noteScreenBrightnessLocked(int display, int brightness, long elapsedRealtimeMs, long uptimeMs) {
        int bin;
        int overallBin;
        int bin2 = brightness / 51;
        if (bin2 < 0) {
            bin = 0;
        } else {
            if (bin2 >= 5) {
                bin2 = 4;
            }
            bin = bin2;
        }
        DisplayBatteryStats[] displayBatteryStatsArr = this.mPerDisplayBatteryStats;
        int numDisplays = displayBatteryStatsArr.length;
        if (display < 0 || display >= numDisplays) {
            Slog.wtf(TAG, "Unexpected note screen brightness for display " + display + " (only " + this.mPerDisplayBatteryStats.length + " displays exist...)");
            return;
        }
        DisplayBatteryStats displayStats = displayBatteryStatsArr[display];
        int oldBin = displayStats.screenBrightnessBin;
        if (oldBin == bin) {
            overallBin = this.mScreenBrightnessBin;
        } else {
            displayStats.screenBrightnessBin = bin;
            if (displayStats.screenState == 2) {
                if (oldBin >= 0) {
                    displayStats.screenBrightnessTimers[oldBin].stopRunningLocked(elapsedRealtimeMs);
                }
                displayStats.screenBrightnessTimers[bin].startRunningLocked(elapsedRealtimeMs);
            }
            overallBin = evaluateOverallScreenBrightnessBinLocked();
        }
        maybeUpdateOverallScreenBrightness(overallBin, elapsedRealtimeMs, uptimeMs);
    }

    private int evaluateOverallScreenBrightnessBinLocked() {
        int displayBrightnessBin;
        int overallBin = -1;
        int numDisplays = getDisplayCount();
        for (int display = 0; display < numDisplays; display++) {
            if (this.mPerDisplayBatteryStats[display].screenState == 2) {
                displayBrightnessBin = this.mPerDisplayBatteryStats[display].screenBrightnessBin;
            } else {
                displayBrightnessBin = -1;
            }
            if (displayBrightnessBin > overallBin) {
                overallBin = displayBrightnessBin;
            }
        }
        return overallBin;
    }

    private void maybeUpdateOverallScreenBrightness(int overallBin, long elapsedRealtimeMs, long uptimeMs) {
        if (this.mScreenBrightnessBin != overallBin) {
            if (overallBin >= 0) {
                BatteryStats.HistoryItem historyItem = this.mHistoryCur;
                historyItem.states = (historyItem.states & (-8)) | (overallBin << 0);
                addHistoryRecordLocked(elapsedRealtimeMs, uptimeMs);
            }
            if (this.mScreenState == 2) {
                int i = this.mScreenBrightnessBin;
                if (i >= 0) {
                    this.mScreenBrightnessTimer[i].stopRunningLocked(elapsedRealtimeMs);
                }
                if (overallBin >= 0) {
                    this.mScreenBrightnessTimer[overallBin].startRunningLocked(elapsedRealtimeMs);
                }
            }
            this.mScreenBrightnessBin = overallBin;
        }
    }

    public void noteUserActivityLocked(int uid, int event) {
        noteUserActivityLocked(uid, event, this.mClock.elapsedRealtime(), this.mClock.uptimeMillis());
    }

    public void noteUserActivityLocked(int uid, int event, long elapsedRealtimeMs, long uptimeMs) {
        if (this.mOnBatteryInternal) {
            getUidStatsLocked(mapUid(uid), elapsedRealtimeMs, uptimeMs).noteUserActivityLocked(event);
        }
    }

    public void noteWakeUpLocked(String reason, int reasonUid) {
        noteWakeUpLocked(reason, reasonUid, this.mClock.elapsedRealtime(), this.mClock.uptimeMillis());
    }

    public void noteWakeUpLocked(String reason, int reasonUid, long elapsedRealtimeMs, long uptimeMs) {
        addHistoryEventLocked(elapsedRealtimeMs, uptimeMs, 18, reason, reasonUid);
    }

    public void noteInteractiveLocked(boolean interactive) {
        noteInteractiveLocked(interactive, this.mClock.elapsedRealtime());
    }

    public void noteInteractiveLocked(boolean interactive, long elapsedRealtimeMs) {
        if (this.mInteractive != interactive) {
            this.mInteractive = interactive;
            if (interactive) {
                this.mInteractiveTimer.startRunningLocked(elapsedRealtimeMs);
            } else {
                this.mInteractiveTimer.stopRunningLocked(elapsedRealtimeMs);
            }
        }
    }

    public void noteConnectivityChangedLocked(int type, String extra) {
        noteConnectivityChangedLocked(type, extra, this.mClock.elapsedRealtime(), this.mClock.uptimeMillis());
    }

    public void noteConnectivityChangedLocked(int type, String extra, long elapsedRealtimeMs, long uptimeMs) {
        addHistoryEventLocked(elapsedRealtimeMs, uptimeMs, 9, extra, type);
        this.mNumConnectivityChange++;
    }

    private void noteMobileRadioApWakeupLocked(long elapsedRealtimeMillis, long uptimeMillis, int uid) {
        int uid2 = mapUid(uid);
        addHistoryEventLocked(elapsedRealtimeMillis, uptimeMillis, 19, "", uid2);
        getUidStatsLocked(uid2, elapsedRealtimeMillis, uptimeMillis).noteMobileRadioApWakeupLocked();
    }

    public boolean noteMobileRadioPowerStateLocked(int powerState, long timestampNs, int uid) {
        return noteMobileRadioPowerStateLocked(powerState, timestampNs, uid, this.mClock.elapsedRealtime(), this.mClock.uptimeMillis());
    }

    public boolean noteMobileRadioPowerStateLocked(int powerState, long timestampNs, int uid, long elapsedRealtimeMs, long uptimeMs) {
        long lastUpdateTimeMs;
        if (this.mMobileRadioPowerState != powerState) {
            boolean active = isActiveRadioPowerState(powerState);
            if (active) {
                if (uid > 0) {
                    noteMobileRadioApWakeupLocked(elapsedRealtimeMs, uptimeMs, uid);
                }
                long j = timestampNs / TimeUtils.NANOS_PER_MS;
                lastUpdateTimeMs = j;
                this.mMobileRadioActiveStartTimeMs = j;
                this.mHistoryCur.states |= 33554432;
            } else {
                long realElapsedRealtimeMs = timestampNs / TimeUtils.NANOS_PER_MS;
                long lastUpdateTimeMs2 = this.mMobileRadioActiveStartTimeMs;
                if (realElapsedRealtimeMs < lastUpdateTimeMs2) {
                    Slog.wtf(TAG, "Data connection inactive timestamp " + realElapsedRealtimeMs + " is before start time " + lastUpdateTimeMs2);
                    realElapsedRealtimeMs = elapsedRealtimeMs;
                } else if (realElapsedRealtimeMs < elapsedRealtimeMs) {
                    this.mMobileRadioActiveAdjustedTime.addCountLocked(elapsedRealtimeMs - realElapsedRealtimeMs);
                }
                this.mHistoryCur.states &= -33554433;
                lastUpdateTimeMs = realElapsedRealtimeMs;
            }
            addHistoryRecordLocked(elapsedRealtimeMs, uptimeMs);
            this.mMobileRadioPowerState = powerState;
            getRatBatteryStatsLocked(this.mActiveRat).noteActive(active, elapsedRealtimeMs);
            if (active) {
                this.mMobileRadioActiveTimer.startRunningLocked(elapsedRealtimeMs);
                this.mMobileRadioActivePerAppTimer.startRunningLocked(elapsedRealtimeMs);
                return false;
            }
            this.mMobileRadioActiveTimer.stopRunningLocked(lastUpdateTimeMs);
            this.mMobileRadioActivePerAppTimer.stopRunningLocked(lastUpdateTimeMs);
            return true;
        }
        return false;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public static boolean isActiveRadioPowerState(int powerState) {
        return powerState == 2 || powerState == 3;
    }

    public void notePowerSaveModeLocked(boolean enabled) {
        notePowerSaveModeLocked(enabled, this.mClock.elapsedRealtime(), this.mClock.uptimeMillis());
    }

    public void notePowerSaveModeLockedInit(boolean enabled, long elapsedRealtimeMs, long uptimeMs) {
        int i;
        if (this.mPowerSaveModeEnabled != enabled) {
            notePowerSaveModeLocked(enabled, elapsedRealtimeMs, uptimeMs);
            return;
        }
        if (enabled) {
            i = 1;
        } else {
            i = 0;
        }
        FrameworkStatsLog.write(20, i);
    }

    public void notePowerSaveModeLocked(boolean enabled, long elapsedRealtimeMs, long uptimeMs) {
        if (this.mPowerSaveModeEnabled != enabled) {
            int i = 0;
            int stepState = enabled ? 4 : 0;
            int i2 = this.mModStepMode;
            int i3 = this.mCurStepMode;
            this.mModStepMode = i2 | ((i3 & 4) ^ stepState);
            this.mCurStepMode = (i3 & (-5)) | stepState;
            this.mPowerSaveModeEnabled = enabled;
            if (enabled) {
                this.mHistoryCur.states2 |= Integer.MIN_VALUE;
                this.mPowerSaveModeEnabledTimer.startRunningLocked(elapsedRealtimeMs);
            } else {
                this.mHistoryCur.states2 &= Integer.MAX_VALUE;
                this.mPowerSaveModeEnabledTimer.stopRunningLocked(elapsedRealtimeMs);
            }
            addHistoryRecordLocked(elapsedRealtimeMs, uptimeMs);
            if (enabled) {
                i = 1;
            }
            FrameworkStatsLog.write(20, i);
        }
    }

    public void noteDeviceIdleModeLocked(int mode, String activeReason, int activeUid) {
        noteDeviceIdleModeLocked(mode, activeReason, activeUid, this.mClock.elapsedRealtime(), this.mClock.uptimeMillis());
    }

    public void noteDeviceIdleModeLocked(int mode, String activeReason, int activeUid, long elapsedRealtimeMs, long uptimeMs) {
        boolean nowIdling;
        boolean nowLightIdling;
        boolean nowLightIdling2;
        int statsmode;
        boolean nowIdling2 = mode == 2;
        boolean z = this.mDeviceIdling;
        if (z && !nowIdling2 && activeReason == null) {
            nowIdling = true;
        } else {
            nowIdling = nowIdling2;
        }
        boolean nowLightIdling3 = mode == 1;
        boolean z2 = this.mDeviceLightIdling;
        if (z2 && !nowLightIdling3 && !nowIdling && activeReason == null) {
            nowLightIdling = true;
        } else {
            nowLightIdling = nowLightIdling3;
        }
        if (activeReason == null) {
            nowLightIdling2 = nowLightIdling;
        } else if (z || z2) {
            nowLightIdling2 = nowLightIdling;
            addHistoryEventLocked(elapsedRealtimeMs, uptimeMs, 10, activeReason, activeUid);
        } else {
            nowLightIdling2 = nowLightIdling;
        }
        if (this.mDeviceIdling != nowIdling || this.mDeviceLightIdling != nowLightIdling2) {
            if (nowIdling) {
                statsmode = 2;
            } else {
                statsmode = nowLightIdling2 ? 1 : 0;
            }
            FrameworkStatsLog.write(22, statsmode);
        }
        if (this.mDeviceIdling != nowIdling) {
            this.mDeviceIdling = nowIdling;
            int stepState = nowIdling ? 8 : 0;
            int i = this.mModStepMode;
            int i2 = this.mCurStepMode;
            this.mModStepMode = i | ((i2 & 8) ^ stepState);
            this.mCurStepMode = (i2 & (-9)) | stepState;
            if (nowIdling) {
                this.mDeviceIdlingTimer.startRunningLocked(elapsedRealtimeMs);
            } else {
                this.mDeviceIdlingTimer.stopRunningLocked(elapsedRealtimeMs);
            }
        }
        if (this.mDeviceLightIdling != nowLightIdling2) {
            this.mDeviceLightIdling = nowLightIdling2;
            if (nowLightIdling2) {
                this.mDeviceLightIdlingTimer.startRunningLocked(elapsedRealtimeMs);
            } else {
                this.mDeviceLightIdlingTimer.stopRunningLocked(elapsedRealtimeMs);
            }
        }
        if (this.mDeviceIdleMode != mode) {
            BatteryStats.HistoryItem historyItem = this.mHistoryCur;
            historyItem.states2 = (historyItem.states2 & (-100663297)) | (mode << 25);
            addHistoryRecordLocked(elapsedRealtimeMs, uptimeMs);
            long lastDuration = elapsedRealtimeMs - this.mLastIdleTimeStartMs;
            this.mLastIdleTimeStartMs = elapsedRealtimeMs;
            int i3 = this.mDeviceIdleMode;
            if (i3 == 1) {
                if (lastDuration > this.mLongestLightIdleTimeMs) {
                    this.mLongestLightIdleTimeMs = lastDuration;
                }
                this.mDeviceIdleModeLightTimer.stopRunningLocked(elapsedRealtimeMs);
            } else if (i3 == 2) {
                if (lastDuration > this.mLongestFullIdleTimeMs) {
                    this.mLongestFullIdleTimeMs = lastDuration;
                }
                this.mDeviceIdleModeFullTimer.stopRunningLocked(elapsedRealtimeMs);
            }
            if (mode == 1) {
                this.mDeviceIdleModeLightTimer.startRunningLocked(elapsedRealtimeMs);
            } else if (mode == 2) {
                this.mDeviceIdleModeFullTimer.startRunningLocked(elapsedRealtimeMs);
            }
            this.mDeviceIdleMode = mode;
            FrameworkStatsLog.write(21, mode);
        }
    }

    public void notePackageInstalledLocked(String pkgName, long versionCode) {
        notePackageInstalledLocked(pkgName, versionCode, this.mClock.elapsedRealtime(), this.mClock.uptimeMillis());
    }

    public void notePackageInstalledLocked(String pkgName, long versionCode, long elapsedRealtimeMs, long uptimeMs) {
        addHistoryEventLocked(elapsedRealtimeMs, uptimeMs, 11, pkgName, (int) versionCode);
        BatteryStats.PackageChange pc = new BatteryStats.PackageChange();
        pc.mPackageName = pkgName;
        pc.mUpdate = true;
        pc.mVersionCode = versionCode;
        addPackageChange(pc);
    }

    public void notePackageUninstalledLocked(String pkgName) {
        notePackageUninstalledLocked(pkgName, this.mClock.elapsedRealtime(), this.mClock.uptimeMillis());
    }

    public void notePackageUninstalledLocked(String pkgName, long elapsedRealtimeMs, long uptimeMs) {
        addHistoryEventLocked(elapsedRealtimeMs, uptimeMs, 12, pkgName, 0);
        BatteryStats.PackageChange pc = new BatteryStats.PackageChange();
        pc.mPackageName = pkgName;
        pc.mUpdate = true;
        addPackageChange(pc);
    }

    private void addPackageChange(BatteryStats.PackageChange pc) {
        if (this.mDailyPackageChanges == null) {
            this.mDailyPackageChanges = new ArrayList<>();
        }
        this.mDailyPackageChanges.add(pc);
    }

    void stopAllGpsSignalQualityTimersLocked(int except) {
        stopAllGpsSignalQualityTimersLocked(except, this.mClock.elapsedRealtime());
    }

    void stopAllGpsSignalQualityTimersLocked(int except, long elapsedRealtimeMs) {
        for (int i = 0; i < this.mGpsSignalQualityTimer.length; i++) {
            if (i != except) {
                while (this.mGpsSignalQualityTimer[i].isRunningLocked()) {
                    this.mGpsSignalQualityTimer[i].stopRunningLocked(elapsedRealtimeMs);
                }
            }
        }
    }

    public void notePhoneOnLocked() {
        notePhoneOnLocked(this.mClock.elapsedRealtime(), this.mClock.uptimeMillis());
    }

    public void notePhoneOnLocked(long elapsedRealtimeMs, long uptimeMs) {
        if (!this.mPhoneOn) {
            this.mHistoryCur.states2 |= 8388608;
            addHistoryRecordLocked(elapsedRealtimeMs, uptimeMs);
            this.mPhoneOn = true;
            this.mPhoneOnTimer.startRunningLocked(elapsedRealtimeMs);
        }
    }

    public void notePhoneOffLocked() {
        notePhoneOffLocked(this.mClock.elapsedRealtime(), this.mClock.uptimeMillis());
    }

    public void notePhoneOffLocked(long elapsedRealtimeMs, long uptimeMs) {
        if (this.mPhoneOn) {
            this.mHistoryCur.states2 &= -8388609;
            addHistoryRecordLocked(elapsedRealtimeMs, uptimeMs);
            this.mPhoneOn = false;
            this.mPhoneOnTimer.stopRunningLocked(elapsedRealtimeMs);
        }
    }

    private void registerUsbStateReceiver(Context context) {
        IntentFilter usbStateFilter = new IntentFilter();
        usbStateFilter.addAction(UsbManager.ACTION_USB_STATE);
        context.registerReceiver(new BroadcastReceiver() { // from class: com.android.internal.os.BatteryStatsImpl.3
            @Override // android.content.BroadcastReceiver
            public void onReceive(Context context2, Intent intent) {
                boolean state = intent.getBooleanExtra("connected", false);
                synchronized (BatteryStatsImpl.this) {
                    BatteryStatsImpl batteryStatsImpl = BatteryStatsImpl.this;
                    batteryStatsImpl.noteUsbConnectionStateLocked(state, batteryStatsImpl.mClock.elapsedRealtime(), BatteryStatsImpl.this.mClock.uptimeMillis());
                }
            }
        }, usbStateFilter);
        synchronized (this) {
            if (this.mUsbDataState == 0) {
                Intent usbState = context.registerReceiver(null, usbStateFilter);
                boolean z = false;
                if (usbState != null && usbState.getBooleanExtra("connected", false)) {
                    z = true;
                }
                boolean initState = z;
                noteUsbConnectionStateLocked(initState, this.mClock.elapsedRealtime(), this.mClock.uptimeMillis());
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void noteUsbConnectionStateLocked(boolean connected, long elapsedRealtimeMs, long uptimeMs) {
        int newState = connected ? 2 : 1;
        if (this.mUsbDataState != newState) {
            this.mUsbDataState = newState;
            if (connected) {
                this.mHistoryCur.states2 |= 262144;
            } else {
                this.mHistoryCur.states2 &= -262145;
            }
            addHistoryRecordLocked(elapsedRealtimeMs, uptimeMs);
        }
    }

    void stopAllPhoneSignalStrengthTimersLocked(int except, long elapsedRealtimeMs) {
        for (int i = 0; i < CellSignalStrength.getNumSignalStrengthLevels() + 1; i++) {
            if (i != except) {
                while (this.mPhoneSignalStrengthsTimer[i].isRunningLocked()) {
                    this.mPhoneSignalStrengthsTimer[i].stopRunningLocked(elapsedRealtimeMs);
                }
            }
        }
    }

    private int fixPhoneServiceState(int state, int signalBin) {
        if (this.mPhoneSimStateRaw == 1 && state == 1 && signalBin > 0) {
            return 0;
        }
        return state;
    }

    private void updateAllPhoneStateLocked(int state, int simState, int strengthBin, long elapsedRealtimeMs, long uptimeMs) {
        boolean scanning = false;
        boolean newHistory = false;
        this.mPhoneServiceStateRaw = state;
        this.mPhoneSimStateRaw = simState;
        this.mPhoneSignalStrengthBinRaw = strengthBin;
        if (simState == 1 && state == 1 && strengthBin > 0) {
            state = 0;
        }
        if (state == 3) {
            strengthBin = -1;
        } else if (state != 0 && state == 1) {
            scanning = true;
            strengthBin = 0;
            if (!this.mPhoneSignalScanningTimer.isRunningLocked()) {
                this.mHistoryCur.states |= 2097152;
                newHistory = true;
                this.mPhoneSignalScanningTimer.startRunningLocked(elapsedRealtimeMs);
                FrameworkStatsLog.write(94, state, simState, 0);
            }
        }
        if (!scanning && this.mPhoneSignalScanningTimer.isRunningLocked()) {
            this.mHistoryCur.states &= -2097153;
            newHistory = true;
            this.mPhoneSignalScanningTimer.stopRunningLocked(elapsedRealtimeMs);
            FrameworkStatsLog.write(94, state, simState, strengthBin);
        }
        if (this.mPhoneServiceState != state) {
            BatteryStats.HistoryItem historyItem = this.mHistoryCur;
            historyItem.states = (historyItem.states & (-449)) | (state << 6);
            newHistory = true;
            this.mPhoneServiceState = state;
        }
        int i = this.mPhoneSignalStrengthBin;
        if (i != strengthBin) {
            if (i >= 0) {
                this.mPhoneSignalStrengthsTimer[i].stopRunningLocked(elapsedRealtimeMs);
            }
            if (strengthBin >= 0) {
                if (!this.mPhoneSignalStrengthsTimer[strengthBin].isRunningLocked()) {
                    this.mPhoneSignalStrengthsTimer[strengthBin].startRunningLocked(elapsedRealtimeMs);
                }
                BatteryStats.HistoryItem historyItem2 = this.mHistoryCur;
                historyItem2.states = (historyItem2.states & (-57)) | (strengthBin << 3);
                newHistory = true;
                FrameworkStatsLog.write(40, strengthBin);
            } else {
                stopAllPhoneSignalStrengthTimersLocked(-1, elapsedRealtimeMs);
            }
            this.mPhoneSignalStrengthBin = strengthBin;
        }
        if (newHistory) {
            addHistoryRecordLocked(elapsedRealtimeMs, uptimeMs);
        }
    }

    public void notePhoneStateLocked(int state, int simState) {
        notePhoneStateLocked(state, simState, this.mClock.elapsedRealtime(), this.mClock.uptimeMillis());
    }

    public void notePhoneStateLocked(int state, int simState, long elapsedRealtimeMs, long uptimeMs) {
        updateAllPhoneStateLocked(state, simState, this.mPhoneSignalStrengthBinRaw, elapsedRealtimeMs, uptimeMs);
    }

    public void notePhoneSignalStrengthLocked(SignalStrength signalStrength) {
        notePhoneSignalStrengthLocked(signalStrength, this.mClock.elapsedRealtime(), this.mClock.uptimeMillis());
    }

    public void notePhoneSignalStrengthLocked(SignalStrength signalStrength, long elapsedRealtimeMs, long uptimeMs) {
        int ratType;
        int level;
        int overallSignalStrength = signalStrength.getLevel();
        SparseIntArray perRatSignalStrength = new SparseIntArray(3);
        List<CellSignalStrength> cellSignalStrengths = signalStrength.getCellSignalStrengths();
        int size = cellSignalStrengths.size();
        for (int i = 0; i < size; i++) {
            CellSignalStrength cellSignalStrength = cellSignalStrengths.get(i);
            if (cellSignalStrength instanceof CellSignalStrengthNr) {
                ratType = 2;
                level = cellSignalStrength.getLevel();
            } else if (cellSignalStrength instanceof CellSignalStrengthLte) {
                ratType = 1;
                level = cellSignalStrength.getLevel();
            } else {
                ratType = 0;
                level = cellSignalStrength.getLevel();
            }
            if (perRatSignalStrength.get(ratType, -1) < level) {
                perRatSignalStrength.put(ratType, level);
            }
        }
        notePhoneSignalStrengthLocked(overallSignalStrength, perRatSignalStrength, elapsedRealtimeMs, uptimeMs);
    }

    public void notePhoneSignalStrengthLocked(int signalStrength, SparseIntArray perRatSignalStrength) {
        notePhoneSignalStrengthLocked(signalStrength, perRatSignalStrength, this.mClock.elapsedRealtime(), this.mClock.uptimeMillis());
    }

    public void notePhoneSignalStrengthLocked(int signalStrength, SparseIntArray perRatSignalStrength, long elapsedRealtimeMs, long uptimeMs) {
        int size = perRatSignalStrength.size();
        for (int i = 0; i < size; i++) {
            int rat = perRatSignalStrength.keyAt(i);
            int ratSignalStrength = perRatSignalStrength.valueAt(i);
            getRatBatteryStatsLocked(rat).noteSignalStrength(ratSignalStrength, elapsedRealtimeMs);
        }
        updateAllPhoneStateLocked(this.mPhoneServiceStateRaw, this.mPhoneSimStateRaw, signalStrength, elapsedRealtimeMs, uptimeMs);
    }

    public void notePhoneDataConnectionStateLocked(int dataType, boolean hasData, int serviceType, int nrFrequency) {
        notePhoneDataConnectionStateLocked(dataType, hasData, serviceType, nrFrequency, this.mClock.elapsedRealtime(), this.mClock.uptimeMillis());
    }

    public void notePhoneDataConnectionStateLocked(int dataType, boolean hasData, int serviceType, int nrFrequency, long elapsedRealtimeMs, long uptimeMs) {
        int bin = 0;
        if (hasData) {
            if (dataType > 0 && dataType <= TelephonyManager.getAllNetworkTypes().length) {
                bin = dataType;
            } else {
                switch (serviceType) {
                    case 1:
                        bin = 0;
                        break;
                    case 2:
                        bin = DATA_CONNECTION_EMERGENCY_SERVICE;
                        break;
                    default:
                        bin = DATA_CONNECTION_OTHER;
                        break;
                }
            }
        }
        int newRat = mapNetworkTypeToRadioAccessTechnology(bin);
        if (newRat == 2) {
            getRatBatteryStatsLocked(newRat).noteFrequencyRange(nrFrequency, elapsedRealtimeMs);
        }
        if (this.mPhoneDataConnectionType != bin) {
            BatteryStats.HistoryItem historyItem = this.mHistoryCur;
            historyItem.states = (historyItem.states & (-15873)) | (bin << 9);
            addHistoryRecordLocked(elapsedRealtimeMs, uptimeMs);
            int i = this.mPhoneDataConnectionType;
            if (i >= 0) {
                this.mPhoneDataConnectionsTimer[i].stopRunningLocked(elapsedRealtimeMs);
            }
            this.mPhoneDataConnectionType = bin;
            this.mPhoneDataConnectionsTimer[bin].startRunningLocked(elapsedRealtimeMs);
            int i2 = this.mActiveRat;
            if (i2 != newRat) {
                getRatBatteryStatsLocked(i2).noteActive(false, elapsedRealtimeMs);
                this.mActiveRat = newRat;
            }
            boolean modemActive = this.mMobileRadioActiveTimer.isRunningLocked();
            getRatBatteryStatsLocked(newRat).noteActive(modemActive, elapsedRealtimeMs);
        }
    }

    private static int mapNetworkTypeToRadioAccessTechnology(int dataType) {
        switch (dataType) {
            case 0:
            case 1:
            case 2:
            case 3:
            case 4:
            case 5:
            case 6:
            case 7:
            case 8:
            case 9:
            case 10:
            case 11:
            case 12:
            case 14:
            case 15:
            case 16:
            case 17:
            case 18:
                return 0;
            case 13:
                return 1;
            case 19:
            default:
                Slog.w(TAG, "Unhandled NetworkType (" + dataType + "), mapping to OTHER");
                return 0;
            case 20:
                return 2;
        }
    }

    private static int mapRadioAccessNetworkTypeToRadioAccessTechnology(int dataType) {
        switch (dataType) {
            case 0:
            case 1:
            case 2:
            case 4:
            case 5:
                return 0;
            case 3:
                return 1;
            case 6:
                return 2;
            default:
                Slog.w(TAG, "Unhandled RadioAccessNetworkType (" + dataType + "), mapping to OTHER");
                return 0;
        }
    }

    public void noteWifiOnLocked() {
        noteWifiOnLocked(this.mClock.elapsedRealtime(), this.mClock.uptimeMillis());
    }

    public void noteWifiOnLocked(long elapsedRealtimeMs, long uptimeMs) {
        if (!this.mWifiOn) {
            this.mHistoryCur.states2 |= 268435456;
            addHistoryRecordLocked(elapsedRealtimeMs, uptimeMs);
            this.mWifiOn = true;
            this.mWifiOnTimer.startRunningLocked(elapsedRealtimeMs);
            scheduleSyncExternalStatsLocked("wifi-off", 2);
        }
    }

    public void noteWifiOffLocked() {
        noteWifiOffLocked(this.mClock.elapsedRealtime(), this.mClock.uptimeMillis());
    }

    public void noteWifiOffLocked(long elapsedRealtimeMs, long uptimeMs) {
        if (this.mWifiOn) {
            this.mHistoryCur.states2 &= -268435457;
            addHistoryRecordLocked(elapsedRealtimeMs, uptimeMs);
            this.mWifiOn = false;
            this.mWifiOnTimer.stopRunningLocked(elapsedRealtimeMs);
            scheduleSyncExternalStatsLocked("wifi-on", 2);
        }
    }

    public void noteAudioOnLocked(int uid) {
        noteAudioOnLocked(uid, this.mClock.elapsedRealtime(), this.mClock.uptimeMillis());
    }

    public void noteAudioOnLocked(int uid, long elapsedRealtimeMs, long uptimeMs) {
        int uid2 = mapUid(uid);
        if (this.mAudioOnNesting == 0) {
            this.mHistoryCur.states |= 4194304;
            addHistoryRecordLocked(elapsedRealtimeMs, uptimeMs);
            this.mAudioOnTimer.startRunningLocked(elapsedRealtimeMs);
        }
        this.mAudioOnNesting++;
        getUidStatsLocked(uid2, elapsedRealtimeMs, uptimeMs).noteAudioTurnedOnLocked(elapsedRealtimeMs);
    }

    public void noteAudioOffLocked(int uid) {
        noteAudioOffLocked(uid, this.mClock.elapsedRealtime(), this.mClock.uptimeMillis());
    }

    public void noteAudioOffLocked(int uid, long elapsedRealtimeMs, long uptimeMs) {
        if (this.mAudioOnNesting == 0) {
            return;
        }
        int uid2 = mapUid(uid);
        int i = this.mAudioOnNesting - 1;
        this.mAudioOnNesting = i;
        if (i == 0) {
            this.mHistoryCur.states &= -4194305;
            addHistoryRecordLocked(elapsedRealtimeMs, uptimeMs);
            this.mAudioOnTimer.stopRunningLocked(elapsedRealtimeMs);
        }
        getUidStatsLocked(uid2, elapsedRealtimeMs, uptimeMs).noteAudioTurnedOffLocked(elapsedRealtimeMs);
    }

    public void noteVideoOnLocked(int uid) {
        noteVideoOnLocked(uid, this.mClock.elapsedRealtime(), this.mClock.uptimeMillis());
    }

    public void noteVideoOnLocked(int uid, long elapsedRealtimeMs, long uptimeMs) {
        int uid2 = mapUid(uid);
        if (this.mVideoOnNesting == 0) {
            this.mHistoryCur.states2 |= 1073741824;
            addHistoryRecordLocked(elapsedRealtimeMs, uptimeMs);
            this.mVideoOnTimer.startRunningLocked(elapsedRealtimeMs);
        }
        this.mVideoOnNesting++;
        getUidStatsLocked(uid2, elapsedRealtimeMs, uptimeMs).noteVideoTurnedOnLocked(elapsedRealtimeMs);
    }

    public void noteVideoOffLocked(int uid) {
        noteVideoOffLocked(uid, this.mClock.elapsedRealtime(), this.mClock.uptimeMillis());
    }

    public void noteVideoOffLocked(int uid, long elapsedRealtimeMs, long uptimeMs) {
        if (this.mVideoOnNesting == 0) {
            return;
        }
        int uid2 = mapUid(uid);
        int i = this.mVideoOnNesting - 1;
        this.mVideoOnNesting = i;
        if (i == 0) {
            this.mHistoryCur.states2 &= -1073741825;
            addHistoryRecordLocked(elapsedRealtimeMs, uptimeMs);
            this.mVideoOnTimer.stopRunningLocked(elapsedRealtimeMs);
        }
        getUidStatsLocked(uid2, elapsedRealtimeMs, uptimeMs).noteVideoTurnedOffLocked(elapsedRealtimeMs);
    }

    public void noteResetAudioLocked() {
        noteResetAudioLocked(this.mClock.elapsedRealtime(), this.mClock.uptimeMillis());
    }

    public void noteResetAudioLocked(long elapsedRealtimeMs, long uptimeMs) {
        if (this.mAudioOnNesting > 0) {
            this.mAudioOnNesting = 0;
            this.mHistoryCur.states &= -4194305;
            addHistoryRecordLocked(elapsedRealtimeMs, uptimeMs);
            this.mAudioOnTimer.stopAllRunningLocked(elapsedRealtimeMs);
            for (int i = 0; i < this.mUidStats.size(); i++) {
                Uid uid = this.mUidStats.valueAt(i);
                uid.noteResetAudioLocked(elapsedRealtimeMs);
            }
        }
    }

    public void noteResetVideoLocked() {
        noteResetVideoLocked(this.mClock.elapsedRealtime(), this.mClock.uptimeMillis());
    }

    public void noteResetVideoLocked(long elapsedRealtimeMs, long uptimeMs) {
        if (this.mVideoOnNesting > 0) {
            this.mVideoOnNesting = 0;
            this.mHistoryCur.states2 &= -1073741825;
            addHistoryRecordLocked(elapsedRealtimeMs, uptimeMs);
            this.mVideoOnTimer.stopAllRunningLocked(elapsedRealtimeMs);
            for (int i = 0; i < this.mUidStats.size(); i++) {
                Uid uid = this.mUidStats.valueAt(i);
                uid.noteResetVideoLocked(elapsedRealtimeMs);
            }
        }
    }

    public void noteActivityResumedLocked(int uid) {
        noteActivityResumedLocked(uid, this.mClock.elapsedRealtime(), this.mClock.uptimeMillis());
    }

    public void noteActivityResumedLocked(int uid, long elapsedRealtimeMs, long uptimeMs) {
        getUidStatsLocked(mapUid(uid), elapsedRealtimeMs, uptimeMs).noteActivityResumedLocked(elapsedRealtimeMs);
    }

    public void noteActivityPausedLocked(int uid) {
        noteActivityPausedLocked(uid, this.mClock.elapsedRealtime(), this.mClock.uptimeMillis());
    }

    public void noteActivityPausedLocked(int uid, long elapsedRealtimeMs, long uptimeMs) {
        getUidStatsLocked(mapUid(uid), elapsedRealtimeMs, uptimeMs).noteActivityPausedLocked(elapsedRealtimeMs);
    }

    public void noteVibratorOnLocked(int uid, long durationMillis) {
        noteVibratorOnLocked(uid, durationMillis, this.mClock.elapsedRealtime(), this.mClock.uptimeMillis());
    }

    public void noteVibratorOnLocked(int uid, long durationMillis, long elapsedRealtimeMs, long uptimeMs) {
        getUidStatsLocked(mapUid(uid), elapsedRealtimeMs, uptimeMs).noteVibratorOnLocked(durationMillis, elapsedRealtimeMs);
    }

    public void noteVibratorOffLocked(int uid) {
        noteVibratorOffLocked(uid, this.mClock.elapsedRealtime(), this.mClock.uptimeMillis());
    }

    public void noteVibratorOffLocked(int uid, long elapsedRealtimeMs, long uptimeMs) {
        getUidStatsLocked(mapUid(uid), elapsedRealtimeMs, uptimeMs).noteVibratorOffLocked(elapsedRealtimeMs);
    }

    public void noteFlashlightOnLocked(int uid) {
        noteFlashlightOnLocked(uid, this.mClock.elapsedRealtime(), this.mClock.uptimeMillis());
    }

    public void noteFlashlightOnLocked(int uid, long elapsedRealtimeMs, long uptimeMs) {
        int uid2 = mapUid(uid);
        int i = this.mFlashlightOnNesting;
        this.mFlashlightOnNesting = i + 1;
        if (i == 0) {
            this.mHistoryCur.states2 |= 134217728;
            addHistoryRecordLocked(elapsedRealtimeMs, uptimeMs);
            this.mFlashlightOnTimer.startRunningLocked(elapsedRealtimeMs);
        }
        getUidStatsLocked(uid2, elapsedRealtimeMs, uptimeMs).noteFlashlightTurnedOnLocked(elapsedRealtimeMs);
    }

    public void noteFlashlightOffLocked(int uid) {
        noteFlashlightOffLocked(uid, this.mClock.elapsedRealtime(), this.mClock.uptimeMillis());
    }

    public void noteFlashlightOffLocked(int uid, long elapsedRealtimeMs, long uptimeMs) {
        if (this.mFlashlightOnNesting == 0) {
            return;
        }
        int uid2 = mapUid(uid);
        int i = this.mFlashlightOnNesting - 1;
        this.mFlashlightOnNesting = i;
        if (i == 0) {
            this.mHistoryCur.states2 &= -134217729;
            addHistoryRecordLocked(elapsedRealtimeMs, uptimeMs);
            this.mFlashlightOnTimer.stopRunningLocked(elapsedRealtimeMs);
        }
        getUidStatsLocked(uid2, elapsedRealtimeMs, uptimeMs).noteFlashlightTurnedOffLocked(elapsedRealtimeMs);
    }

    public void noteCameraOnLocked(int uid) {
        noteCameraOnLocked(uid, this.mClock.elapsedRealtime(), this.mClock.uptimeMillis());
    }

    public void noteCameraOnLocked(int uid, long elapsedRealtimeMs, long uptimeMs) {
        int uid2 = mapUid(uid);
        int i = this.mCameraOnNesting;
        this.mCameraOnNesting = i + 1;
        if (i == 0) {
            this.mHistoryCur.states2 |= 2097152;
            addHistoryRecordLocked(elapsedRealtimeMs, uptimeMs);
            this.mCameraOnTimer.startRunningLocked(elapsedRealtimeMs);
        }
        getUidStatsLocked(uid2, elapsedRealtimeMs, uptimeMs).noteCameraTurnedOnLocked(elapsedRealtimeMs);
    }

    public void noteCameraOffLocked(int uid) {
        noteCameraOffLocked(uid, this.mClock.elapsedRealtime(), this.mClock.uptimeMillis());
    }

    public void noteCameraOffLocked(int uid, long elapsedRealtimeMs, long uptimeMs) {
        if (this.mCameraOnNesting == 0) {
            return;
        }
        int uid2 = mapUid(uid);
        int i = this.mCameraOnNesting - 1;
        this.mCameraOnNesting = i;
        if (i == 0) {
            this.mHistoryCur.states2 &= -2097153;
            addHistoryRecordLocked(elapsedRealtimeMs, uptimeMs);
            this.mCameraOnTimer.stopRunningLocked(elapsedRealtimeMs);
        }
        getUidStatsLocked(uid2, elapsedRealtimeMs, uptimeMs).noteCameraTurnedOffLocked(elapsedRealtimeMs);
    }

    public void noteResetCameraLocked() {
        noteResetCameraLocked(this.mClock.elapsedRealtime(), this.mClock.uptimeMillis());
    }

    public void noteResetCameraLocked(long elapsedRealtimeMs, long uptimeMs) {
        if (this.mCameraOnNesting > 0) {
            this.mCameraOnNesting = 0;
            this.mHistoryCur.states2 &= -2097153;
            addHistoryRecordLocked(elapsedRealtimeMs, uptimeMs);
            this.mCameraOnTimer.stopAllRunningLocked(elapsedRealtimeMs);
            for (int i = 0; i < this.mUidStats.size(); i++) {
                Uid uid = this.mUidStats.valueAt(i);
                uid.noteResetCameraLocked(elapsedRealtimeMs);
            }
        }
    }

    public void noteResetFlashlightLocked() {
        noteResetFlashlightLocked(this.mClock.elapsedRealtime(), this.mClock.uptimeMillis());
    }

    public void noteResetFlashlightLocked(long elapsedRealtimeMs, long uptimeMs) {
        if (this.mFlashlightOnNesting > 0) {
            this.mFlashlightOnNesting = 0;
            this.mHistoryCur.states2 &= -134217729;
            addHistoryRecordLocked(elapsedRealtimeMs, uptimeMs);
            this.mFlashlightOnTimer.stopAllRunningLocked(elapsedRealtimeMs);
            for (int i = 0; i < this.mUidStats.size(); i++) {
                Uid uid = this.mUidStats.valueAt(i);
                uid.noteResetFlashlightLocked(elapsedRealtimeMs);
            }
        }
    }

    private void noteBluetoothScanStartedLocked(WorkSource.WorkChain workChain, int uid, boolean isUnoptimized, long elapsedRealtimeMs, long uptimeMs) {
        if (workChain != null) {
            uid = workChain.getAttributionUid();
        }
        int uid2 = mapUid(uid);
        if (this.mBluetoothScanNesting == 0) {
            this.mHistoryCur.states2 |= 1048576;
            addHistoryRecordLocked(elapsedRealtimeMs, uptimeMs);
            this.mBluetoothScanTimer.startRunningLocked(elapsedRealtimeMs);
        }
        this.mBluetoothScanNesting++;
        getUidStatsLocked(uid2, elapsedRealtimeMs, uptimeMs).noteBluetoothScanStartedLocked(elapsedRealtimeMs, isUnoptimized);
    }

    public void noteBluetoothScanStartedFromSourceLocked(WorkSource ws, boolean isUnoptimized) {
        noteBluetoothScanStartedFromSourceLocked(ws, isUnoptimized, this.mClock.elapsedRealtime(), this.mClock.uptimeMillis());
    }

    public void noteBluetoothScanStartedFromSourceLocked(WorkSource ws, boolean isUnoptimized, long elapsedRealtimeMs, long uptimeMs) {
        int N = ws.size();
        for (int i = 0; i < N; i++) {
            noteBluetoothScanStartedLocked(null, ws.getUid(i), isUnoptimized, elapsedRealtimeMs, uptimeMs);
        }
        List<WorkSource.WorkChain> workChains = ws.getWorkChains();
        if (workChains != null) {
            for (int i2 = 0; i2 < workChains.size(); i2++) {
                noteBluetoothScanStartedLocked(workChains.get(i2), -1, isUnoptimized, elapsedRealtimeMs, uptimeMs);
            }
        }
    }

    private void noteBluetoothScanStoppedLocked(WorkSource.WorkChain workChain, int uid, boolean isUnoptimized, long elapsedRealtimeMs, long uptimeMs) {
        if (workChain != null) {
            uid = workChain.getAttributionUid();
        }
        int uid2 = mapUid(uid);
        int i = this.mBluetoothScanNesting - 1;
        this.mBluetoothScanNesting = i;
        if (i == 0) {
            this.mHistoryCur.states2 &= -1048577;
            addHistoryRecordLocked(elapsedRealtimeMs, uptimeMs);
            this.mBluetoothScanTimer.stopRunningLocked(elapsedRealtimeMs);
        }
        getUidStatsLocked(uid2, elapsedRealtimeMs, uptimeMs).noteBluetoothScanStoppedLocked(elapsedRealtimeMs, isUnoptimized);
    }

    public void noteBluetoothScanStoppedFromSourceLocked(WorkSource ws, boolean isUnoptimized) {
        noteBluetoothScanStoppedFromSourceLocked(ws, isUnoptimized, this.mClock.elapsedRealtime(), this.mClock.uptimeMillis());
    }

    public void noteBluetoothScanStoppedFromSourceLocked(WorkSource ws, boolean isUnoptimized, long elapsedRealtimeMs, long uptimeMs) {
        int N = ws.size();
        for (int i = 0; i < N; i++) {
            noteBluetoothScanStoppedLocked(null, ws.getUid(i), isUnoptimized, elapsedRealtimeMs, uptimeMs);
        }
        List<WorkSource.WorkChain> workChains = ws.getWorkChains();
        if (workChains != null) {
            for (int i2 = 0; i2 < workChains.size(); i2++) {
                noteBluetoothScanStoppedLocked(workChains.get(i2), -1, isUnoptimized, elapsedRealtimeMs, uptimeMs);
            }
        }
    }

    public void noteResetBluetoothScanLocked() {
        noteResetBluetoothScanLocked(this.mClock.elapsedRealtime(), this.mClock.uptimeMillis());
    }

    public void noteResetBluetoothScanLocked(long elapsedRealtimeMs, long uptimeMs) {
        if (this.mBluetoothScanNesting > 0) {
            this.mBluetoothScanNesting = 0;
            this.mHistoryCur.states2 &= -1048577;
            addHistoryRecordLocked(elapsedRealtimeMs, uptimeMs);
            this.mBluetoothScanTimer.stopAllRunningLocked(elapsedRealtimeMs);
            for (int i = 0; i < this.mUidStats.size(); i++) {
                Uid uid = this.mUidStats.valueAt(i);
                uid.noteResetBluetoothScanLocked(elapsedRealtimeMs);
            }
        }
    }

    public void noteBluetoothScanResultsFromSourceLocked(WorkSource ws, int numNewResults) {
        noteBluetoothScanResultsFromSourceLocked(ws, numNewResults, this.mClock.elapsedRealtime(), this.mClock.uptimeMillis());
    }

    public void noteBluetoothScanResultsFromSourceLocked(WorkSource ws, int numNewResults, long elapsedRealtimeMs, long uptimeMs) {
        int N = ws.size();
        for (int i = 0; i < N; i++) {
            int uid = mapUid(ws.getUid(i));
            getUidStatsLocked(uid, elapsedRealtimeMs, uptimeMs).noteBluetoothScanResultsLocked(numNewResults);
        }
        List<WorkSource.WorkChain> workChains = ws.getWorkChains();
        if (workChains != null) {
            for (int i2 = 0; i2 < workChains.size(); i2++) {
                WorkSource.WorkChain wc = workChains.get(i2);
                int uid2 = mapUid(wc.getAttributionUid());
                getUidStatsLocked(uid2, elapsedRealtimeMs, uptimeMs).noteBluetoothScanResultsLocked(numNewResults);
            }
        }
    }

    private void noteWifiRadioApWakeupLocked(long elapsedRealtimeMillis, long uptimeMillis, int uid) {
        int uid2 = mapUid(uid);
        addHistoryEventLocked(elapsedRealtimeMillis, uptimeMillis, 19, "", uid2);
        getUidStatsLocked(uid2, elapsedRealtimeMillis, uptimeMillis).noteWifiRadioApWakeupLocked();
    }

    public void noteWifiRadioPowerState(int powerState, long timestampNs, int uid) {
        noteWifiRadioPowerState(powerState, timestampNs, uid, this.mClock.elapsedRealtime(), this.mClock.uptimeMillis());
    }

    public void noteWifiRadioPowerState(int powerState, long timestampNs, int uid, long elapsedRealtimeMs, long uptimeMs) {
        if (this.mWifiRadioPowerState != powerState) {
            boolean active = powerState == 2 || powerState == 3;
            if (active) {
                if (uid > 0) {
                    noteWifiRadioApWakeupLocked(elapsedRealtimeMs, uptimeMs, uid);
                }
                this.mHistoryCur.states |= 67108864;
                this.mWifiActiveTimer.startRunningLocked(elapsedRealtimeMs);
            } else {
                this.mHistoryCur.states &= -67108865;
                this.mWifiActiveTimer.stopRunningLocked(timestampNs / TimeUtils.NANOS_PER_MS);
            }
            addHistoryRecordLocked(elapsedRealtimeMs, uptimeMs);
            this.mWifiRadioPowerState = powerState;
        }
    }

    public void noteWifiRunningLocked(WorkSource ws) {
        noteWifiRunningLocked(ws, this.mClock.elapsedRealtime(), this.mClock.uptimeMillis());
    }

    public void noteWifiRunningLocked(WorkSource ws, long elapsedRealtimeMs, long uptimeMs) {
        if (!this.mGlobalWifiRunning) {
            this.mHistoryCur.states2 |= 536870912;
            addHistoryRecordLocked(elapsedRealtimeMs, uptimeMs);
            this.mGlobalWifiRunning = true;
            this.mGlobalWifiRunningTimer.startRunningLocked(elapsedRealtimeMs);
            int N = ws.size();
            for (int i = 0; i < N; i++) {
                int uid = mapUid(ws.getUid(i));
                getUidStatsLocked(uid, elapsedRealtimeMs, uptimeMs).noteWifiRunningLocked(elapsedRealtimeMs);
            }
            List<WorkSource.WorkChain> workChains = ws.getWorkChains();
            if (workChains != null) {
                for (int i2 = 0; i2 < workChains.size(); i2++) {
                    int uid2 = mapUid(workChains.get(i2).getAttributionUid());
                    getUidStatsLocked(uid2, elapsedRealtimeMs, uptimeMs).noteWifiRunningLocked(elapsedRealtimeMs);
                }
            }
            scheduleSyncExternalStatsLocked("wifi-running", 2);
            return;
        }
        Log.w(TAG, "noteWifiRunningLocked -- called while WIFI running");
    }

    public void noteWifiRunningChangedLocked(WorkSource oldWs, WorkSource newWs) {
        noteWifiRunningChangedLocked(oldWs, newWs, this.mClock.elapsedRealtime(), this.mClock.uptimeMillis());
    }

    public void noteWifiRunningChangedLocked(WorkSource oldWs, WorkSource newWs, long elapsedRealtimeMs, long uptimeMs) {
        if (this.mGlobalWifiRunning) {
            int N = oldWs.size();
            for (int i = 0; i < N; i++) {
                int uid = mapUid(oldWs.getUid(i));
                getUidStatsLocked(uid, elapsedRealtimeMs, uptimeMs).noteWifiStoppedLocked(elapsedRealtimeMs);
            }
            List<WorkSource.WorkChain> workChains = oldWs.getWorkChains();
            if (workChains != null) {
                for (int i2 = 0; i2 < workChains.size(); i2++) {
                    int uid2 = mapUid(workChains.get(i2).getAttributionUid());
                    getUidStatsLocked(uid2, elapsedRealtimeMs, uptimeMs).noteWifiStoppedLocked(elapsedRealtimeMs);
                }
            }
            int N2 = newWs.size();
            for (int i3 = 0; i3 < N2; i3++) {
                int uid3 = mapUid(newWs.getUid(i3));
                getUidStatsLocked(uid3, elapsedRealtimeMs, uptimeMs).noteWifiRunningLocked(elapsedRealtimeMs);
            }
            List<WorkSource.WorkChain> workChains2 = newWs.getWorkChains();
            if (workChains2 != null) {
                for (int i4 = 0; i4 < workChains2.size(); i4++) {
                    int uid4 = mapUid(workChains2.get(i4).getAttributionUid());
                    getUidStatsLocked(uid4, elapsedRealtimeMs, uptimeMs).noteWifiRunningLocked(elapsedRealtimeMs);
                }
                return;
            }
            return;
        }
        Log.w(TAG, "noteWifiRunningChangedLocked -- called while WIFI not running");
    }

    public void noteWifiStoppedLocked(WorkSource ws) {
        noteWifiStoppedLocked(ws, this.mClock.elapsedRealtime(), this.mClock.uptimeMillis());
    }

    public void noteWifiStoppedLocked(WorkSource ws, long elapsedRealtimeMs, long uptimeMs) {
        if (this.mGlobalWifiRunning) {
            this.mHistoryCur.states2 &= -536870913;
            addHistoryRecordLocked(elapsedRealtimeMs, uptimeMs);
            this.mGlobalWifiRunning = false;
            this.mGlobalWifiRunningTimer.stopRunningLocked(elapsedRealtimeMs);
            int N = ws.size();
            for (int i = 0; i < N; i++) {
                int uid = mapUid(ws.getUid(i));
                getUidStatsLocked(uid, elapsedRealtimeMs, uptimeMs).noteWifiStoppedLocked(elapsedRealtimeMs);
            }
            List<WorkSource.WorkChain> workChains = ws.getWorkChains();
            if (workChains != null) {
                for (int i2 = 0; i2 < workChains.size(); i2++) {
                    int uid2 = mapUid(workChains.get(i2).getAttributionUid());
                    getUidStatsLocked(uid2, elapsedRealtimeMs, uptimeMs).noteWifiStoppedLocked(elapsedRealtimeMs);
                }
            }
            scheduleSyncExternalStatsLocked("wifi-stopped", 2);
            return;
        }
        Log.w(TAG, "noteWifiStoppedLocked -- called while WIFI not running");
    }

    public void noteWifiStateLocked(int wifiState, String accessPoint) {
        noteWifiStateLocked(wifiState, accessPoint, this.mClock.elapsedRealtime());
    }

    public void noteWifiStateLocked(int wifiState, String accessPoint, long elapsedRealtimeMs) {
        int i = this.mWifiState;
        if (i != wifiState) {
            if (i >= 0) {
                this.mWifiStateTimer[i].stopRunningLocked(elapsedRealtimeMs);
            }
            this.mWifiState = wifiState;
            this.mWifiStateTimer[wifiState].startRunningLocked(elapsedRealtimeMs);
            scheduleSyncExternalStatsLocked("wifi-state", 2);
        }
    }

    public void noteWifiSupplicantStateChangedLocked(int supplState, boolean failedAuth) {
        noteWifiSupplicantStateChangedLocked(supplState, failedAuth, this.mClock.elapsedRealtime(), this.mClock.uptimeMillis());
    }

    public void noteWifiSupplicantStateChangedLocked(int supplState, boolean failedAuth, long elapsedRealtimeMs, long uptimeMs) {
        int i = this.mWifiSupplState;
        if (i != supplState) {
            if (i >= 0) {
                this.mWifiSupplStateTimer[i].stopRunningLocked(elapsedRealtimeMs);
            }
            this.mWifiSupplState = supplState;
            this.mWifiSupplStateTimer[supplState].startRunningLocked(elapsedRealtimeMs);
            BatteryStats.HistoryItem historyItem = this.mHistoryCur;
            historyItem.states2 = (historyItem.states2 & (-16)) | (supplState << 0);
            addHistoryRecordLocked(elapsedRealtimeMs, uptimeMs);
        }
    }

    void stopAllWifiSignalStrengthTimersLocked(int except, long elapsedRealtimeMs) {
        for (int i = 0; i < 5; i++) {
            if (i != except) {
                while (this.mWifiSignalStrengthsTimer[i].isRunningLocked()) {
                    this.mWifiSignalStrengthsTimer[i].stopRunningLocked(elapsedRealtimeMs);
                }
            }
        }
    }

    public void noteWifiRssiChangedLocked(int newRssi) {
        noteWifiRssiChangedLocked(newRssi, this.mClock.elapsedRealtime(), this.mClock.uptimeMillis());
    }

    public void noteWifiRssiChangedLocked(int newRssi, long elapsedRealtimeMs, long uptimeMs) {
        int strengthBin = WifiManager.calculateSignalLevel(newRssi, 5);
        int i = this.mWifiSignalStrengthBin;
        if (i != strengthBin) {
            if (i >= 0) {
                this.mWifiSignalStrengthsTimer[i].stopRunningLocked(elapsedRealtimeMs);
            }
            if (strengthBin >= 0) {
                if (!this.mWifiSignalStrengthsTimer[strengthBin].isRunningLocked()) {
                    this.mWifiSignalStrengthsTimer[strengthBin].startRunningLocked(elapsedRealtimeMs);
                }
                BatteryStats.HistoryItem historyItem = this.mHistoryCur;
                historyItem.states2 = (historyItem.states2 & (-113)) | (strengthBin << 4);
                addHistoryRecordLocked(elapsedRealtimeMs, uptimeMs);
            } else {
                stopAllWifiSignalStrengthTimersLocked(-1, elapsedRealtimeMs);
            }
            this.mWifiSignalStrengthBin = strengthBin;
        }
    }

    public void noteFullWifiLockAcquiredLocked(int uid) {
        noteFullWifiLockAcquiredLocked(uid, this.mClock.elapsedRealtime(), this.mClock.uptimeMillis());
    }

    public void noteFullWifiLockAcquiredLocked(int uid, long elapsedRealtimeMs, long uptimeMs) {
        if (this.mWifiFullLockNesting == 0) {
            this.mHistoryCur.states |= 268435456;
            addHistoryRecordLocked(elapsedRealtimeMs, uptimeMs);
        }
        this.mWifiFullLockNesting++;
        getUidStatsLocked(uid, elapsedRealtimeMs, uptimeMs).noteFullWifiLockAcquiredLocked(elapsedRealtimeMs);
    }

    public void noteFullWifiLockReleasedLocked(int uid) {
        noteFullWifiLockReleasedLocked(uid, this.mClock.elapsedRealtime(), this.mClock.uptimeMillis());
    }

    public void noteFullWifiLockReleasedLocked(int uid, long elapsedRealtimeMs, long uptimeMs) {
        int i = this.mWifiFullLockNesting - 1;
        this.mWifiFullLockNesting = i;
        if (i == 0) {
            this.mHistoryCur.states &= -268435457;
            addHistoryRecordLocked(elapsedRealtimeMs, uptimeMs);
        }
        getUidStatsLocked(uid, elapsedRealtimeMs, uptimeMs).noteFullWifiLockReleasedLocked(elapsedRealtimeMs);
    }

    public void noteWifiScanStartedLocked(int uid) {
        noteWifiScanStartedLocked(uid, this.mClock.elapsedRealtime(), this.mClock.uptimeMillis());
    }

    public void noteWifiScanStartedLocked(int uid, long elapsedRealtimeMs, long uptimeMs) {
        if (this.mWifiScanNesting == 0) {
            this.mHistoryCur.states |= 134217728;
            addHistoryRecordLocked(elapsedRealtimeMs, uptimeMs);
        }
        this.mWifiScanNesting++;
        getUidStatsLocked(uid, elapsedRealtimeMs, uptimeMs).noteWifiScanStartedLocked(elapsedRealtimeMs);
    }

    public void noteWifiScanStoppedLocked(int uid) {
        noteWifiScanStoppedLocked(uid, this.mClock.elapsedRealtime(), this.mClock.uptimeMillis());
    }

    public void noteWifiScanStoppedLocked(int uid, long elapsedRealtimeMs, long uptimeMs) {
        int i = this.mWifiScanNesting - 1;
        this.mWifiScanNesting = i;
        if (i == 0) {
            this.mHistoryCur.states &= -134217729;
            addHistoryRecordLocked(elapsedRealtimeMs, uptimeMs);
        }
        getUidStatsLocked(uid, elapsedRealtimeMs, uptimeMs).noteWifiScanStoppedLocked(elapsedRealtimeMs);
    }

    public void noteWifiBatchedScanStartedLocked(int uid, int csph) {
        noteWifiBatchedScanStartedLocked(uid, csph, this.mClock.elapsedRealtime(), this.mClock.uptimeMillis());
    }

    public void noteWifiBatchedScanStartedLocked(int uid, int csph, long elapsedRealtimeMs, long uptimeMs) {
        getUidStatsLocked(mapUid(uid), elapsedRealtimeMs, uptimeMs).noteWifiBatchedScanStartedLocked(csph, elapsedRealtimeMs);
    }

    public void noteWifiBatchedScanStoppedLocked(int uid) {
        noteWifiBatchedScanStoppedLocked(uid, this.mClock.elapsedRealtime(), this.mClock.uptimeMillis());
    }

    public void noteWifiBatchedScanStoppedLocked(int uid, long elapsedRealtimeMs, long uptimeMs) {
        getUidStatsLocked(mapUid(uid), elapsedRealtimeMs, uptimeMs).noteWifiBatchedScanStoppedLocked(elapsedRealtimeMs);
    }

    public void noteWifiMulticastEnabledLocked(int uid) {
        noteWifiMulticastEnabledLocked(uid, this.mClock.elapsedRealtime(), this.mClock.uptimeMillis());
    }

    public void noteWifiMulticastEnabledLocked(int uid, long elapsedRealtimeMs, long uptimeMs) {
        int uid2 = mapUid(uid);
        if (this.mWifiMulticastNesting == 0) {
            this.mHistoryCur.states |= 65536;
            addHistoryRecordLocked(elapsedRealtimeMs, uptimeMs);
            if (!this.mWifiMulticastWakelockTimer.isRunningLocked()) {
                this.mWifiMulticastWakelockTimer.startRunningLocked(elapsedRealtimeMs);
            }
        }
        this.mWifiMulticastNesting++;
        getUidStatsLocked(uid2, elapsedRealtimeMs, uptimeMs).noteWifiMulticastEnabledLocked(elapsedRealtimeMs);
    }

    public void noteWifiMulticastDisabledLocked(int uid) {
        noteWifiMulticastDisabledLocked(uid, this.mClock.elapsedRealtime(), this.mClock.uptimeMillis());
    }

    public void noteWifiMulticastDisabledLocked(int uid, long elapsedRealtimeMs, long uptimeMs) {
        int uid2 = mapUid(uid);
        int i = this.mWifiMulticastNesting - 1;
        this.mWifiMulticastNesting = i;
        if (i == 0) {
            this.mHistoryCur.states &= -65537;
            addHistoryRecordLocked(elapsedRealtimeMs, uptimeMs);
            if (this.mWifiMulticastWakelockTimer.isRunningLocked()) {
                this.mWifiMulticastWakelockTimer.stopRunningLocked(elapsedRealtimeMs);
            }
        }
        getUidStatsLocked(uid2, elapsedRealtimeMs, uptimeMs).noteWifiMulticastDisabledLocked(elapsedRealtimeMs);
    }

    public void noteFullWifiLockAcquiredFromSourceLocked(WorkSource ws) {
        noteFullWifiLockAcquiredFromSourceLocked(ws, this.mClock.elapsedRealtime(), this.mClock.uptimeMillis());
    }

    public void noteFullWifiLockAcquiredFromSourceLocked(WorkSource ws, long elapsedRealtimeMs, long uptimeMs) {
        int N = ws.size();
        for (int i = 0; i < N; i++) {
            int uid = mapUid(ws.getUid(i));
            noteFullWifiLockAcquiredLocked(uid, elapsedRealtimeMs, uptimeMs);
        }
        List<WorkSource.WorkChain> workChains = ws.getWorkChains();
        if (workChains != null) {
            for (int i2 = 0; i2 < workChains.size(); i2++) {
                WorkSource.WorkChain workChain = workChains.get(i2);
                int uid2 = mapUid(workChain.getAttributionUid());
                noteFullWifiLockAcquiredLocked(uid2, elapsedRealtimeMs, uptimeMs);
            }
        }
    }

    public void noteFullWifiLockReleasedFromSourceLocked(WorkSource ws) {
        noteFullWifiLockReleasedFromSourceLocked(ws, this.mClock.elapsedRealtime(), this.mClock.uptimeMillis());
    }

    public void noteFullWifiLockReleasedFromSourceLocked(WorkSource ws, long elapsedRealtimeMs, long uptimeMs) {
        int N = ws.size();
        for (int i = 0; i < N; i++) {
            int uid = mapUid(ws.getUid(i));
            noteFullWifiLockReleasedLocked(uid, elapsedRealtimeMs, uptimeMs);
        }
        List<WorkSource.WorkChain> workChains = ws.getWorkChains();
        if (workChains != null) {
            for (int i2 = 0; i2 < workChains.size(); i2++) {
                WorkSource.WorkChain workChain = workChains.get(i2);
                int uid2 = mapUid(workChain.getAttributionUid());
                noteFullWifiLockReleasedLocked(uid2, elapsedRealtimeMs, uptimeMs);
            }
        }
    }

    public void noteWifiScanStartedFromSourceLocked(WorkSource ws) {
        noteWifiScanStartedFromSourceLocked(ws, this.mClock.elapsedRealtime(), this.mClock.uptimeMillis());
    }

    public void noteWifiScanStartedFromSourceLocked(WorkSource ws, long elapsedRealtimeMs, long uptimeMs) {
        int N = ws.size();
        for (int i = 0; i < N; i++) {
            int uid = mapUid(ws.getUid(i));
            noteWifiScanStartedLocked(uid, elapsedRealtimeMs, uptimeMs);
        }
        List<WorkSource.WorkChain> workChains = ws.getWorkChains();
        if (workChains != null) {
            for (int i2 = 0; i2 < workChains.size(); i2++) {
                WorkSource.WorkChain workChain = workChains.get(i2);
                int uid2 = mapUid(workChain.getAttributionUid());
                noteWifiScanStartedLocked(uid2, elapsedRealtimeMs, uptimeMs);
            }
        }
    }

    public void noteWifiScanStoppedFromSourceLocked(WorkSource ws) {
        noteWifiScanStoppedFromSourceLocked(ws, this.mClock.elapsedRealtime(), this.mClock.uptimeMillis());
    }

    public void noteWifiScanStoppedFromSourceLocked(WorkSource ws, long elapsedRealtimeMs, long uptimeMs) {
        int N = ws.size();
        for (int i = 0; i < N; i++) {
            int uid = mapUid(ws.getUid(i));
            noteWifiScanStoppedLocked(uid, elapsedRealtimeMs, uptimeMs);
        }
        List<WorkSource.WorkChain> workChains = ws.getWorkChains();
        if (workChains != null) {
            for (int i2 = 0; i2 < workChains.size(); i2++) {
                WorkSource.WorkChain workChain = workChains.get(i2);
                int uid2 = mapUid(workChain.getAttributionUid());
                noteWifiScanStoppedLocked(uid2, elapsedRealtimeMs, uptimeMs);
            }
        }
    }

    public void noteWifiBatchedScanStartedFromSourceLocked(WorkSource ws, int csph) {
        noteWifiBatchedScanStartedFromSourceLocked(ws, csph, this.mClock.elapsedRealtime(), this.mClock.uptimeMillis());
    }

    public void noteWifiBatchedScanStartedFromSourceLocked(WorkSource ws, int csph, long elapsedRealtimeMs, long uptimeMs) {
        int N = ws.size();
        for (int i = 0; i < N; i++) {
            noteWifiBatchedScanStartedLocked(ws.getUid(i), csph, elapsedRealtimeMs, uptimeMs);
        }
        List<WorkSource.WorkChain> workChains = ws.getWorkChains();
        if (workChains != null) {
            for (int i2 = 0; i2 < workChains.size(); i2++) {
                noteWifiBatchedScanStartedLocked(workChains.get(i2).getAttributionUid(), csph, elapsedRealtimeMs, uptimeMs);
            }
        }
    }

    public void noteWifiBatchedScanStoppedFromSourceLocked(WorkSource ws) {
        noteWifiBatchedScanStoppedFromSourceLocked(ws, this.mClock.elapsedRealtime(), this.mClock.uptimeMillis());
    }

    public void noteWifiBatchedScanStoppedFromSourceLocked(WorkSource ws, long elapsedRealtimeMs, long uptimeMs) {
        int N = ws.size();
        for (int i = 0; i < N; i++) {
            noteWifiBatchedScanStoppedLocked(ws.getUid(i), elapsedRealtimeMs, uptimeMs);
        }
        List<WorkSource.WorkChain> workChains = ws.getWorkChains();
        if (workChains != null) {
            for (int i2 = 0; i2 < workChains.size(); i2++) {
                noteWifiBatchedScanStoppedLocked(workChains.get(i2).getAttributionUid(), elapsedRealtimeMs, uptimeMs);
            }
        }
    }

    private static String[] includeInStringArray(String[] array, String str) {
        if (ArrayUtils.indexOf(array, str) >= 0) {
            return array;
        }
        String[] newArray = new String[array.length + 1];
        System.arraycopy(array, 0, newArray, 0, array.length);
        newArray[array.length] = str;
        return newArray;
    }

    private static String[] excludeFromStringArray(String[] array, String str) {
        int index = ArrayUtils.indexOf(array, str);
        if (index >= 0) {
            String[] newArray = new String[array.length - 1];
            if (index > 0) {
                System.arraycopy(array, 0, newArray, 0, index);
            }
            if (index < array.length - 1) {
                System.arraycopy(array, index + 1, newArray, index, (array.length - index) - 1);
            }
            return newArray;
        }
        return array;
    }

    public void noteNetworkInterfaceForTransports(String iface, int[] transportTypes) {
        if (TextUtils.isEmpty(iface)) {
            return;
        }
        int displayTransport = NetworkCapabilitiesUtils.getDisplayTransport(transportTypes);
        synchronized (this.mModemNetworkLock) {
            if (displayTransport == 0) {
                this.mModemIfaces = includeInStringArray(this.mModemIfaces, iface);
            } else {
                this.mModemIfaces = excludeFromStringArray(this.mModemIfaces, iface);
            }
        }
        synchronized (this.mWifiNetworkLock) {
            if (displayTransport == 1) {
                this.mWifiIfaces = includeInStringArray(this.mWifiIfaces, iface);
            } else {
                this.mWifiIfaces = excludeFromStringArray(this.mWifiIfaces, iface);
            }
        }
    }

    public void noteBinderCallStats(int workSourceUid, long incrementalCallCount, Collection<BinderCallsStats.CallStat> callStats) {
        noteBinderCallStats(workSourceUid, incrementalCallCount, callStats, this.mClock.elapsedRealtime(), this.mClock.uptimeMillis());
    }

    public void noteBinderCallStats(int workSourceUid, long incrementalCallCount, Collection<BinderCallsStats.CallStat> callStats, long elapsedRealtimeMs, long uptimeMs) {
        synchronized (this) {
            getUidStatsLocked(workSourceUid, elapsedRealtimeMs, uptimeMs).noteBinderCallStatsLocked(incrementalCallCount, callStats);
        }
    }

    public void noteBinderThreadNativeIds(int[] binderThreadNativeTids) {
        this.mSystemServerCpuThreadReader.setBinderThreadNativeTids(binderThreadNativeTids);
    }

    public void updateSystemServiceCallStats() {
        long totalSystemServiceTimeMicros;
        int totalRecordedCallCount = 0;
        long totalRecordedCallTimeMicros = 0;
        for (int i = 0; i < this.mUidStats.size(); i++) {
            ArraySet<BinderCallStats> binderCallStats = this.mUidStats.valueAt(i).mBinderCallStats;
            for (int j = binderCallStats.size() - 1; j >= 0; j--) {
                BinderCallStats stats = binderCallStats.valueAt(j);
                totalRecordedCallCount = (int) (totalRecordedCallCount + stats.recordedCallCount);
                totalRecordedCallTimeMicros += stats.recordedCpuTimeMicros;
            }
        }
        long totalSystemServiceTimeMicros2 = 0;
        for (int i2 = 0; i2 < this.mUidStats.size(); i2++) {
            Uid uid = this.mUidStats.valueAt(i2);
            long totalTimeForUidUs = 0;
            int totalCallCountForUid = 0;
            ArraySet<BinderCallStats> binderCallStats2 = uid.mBinderCallStats;
            int j2 = binderCallStats2.size() - 1;
            while (j2 >= 0) {
                BinderCallStats stats2 = binderCallStats2.valueAt(j2);
                long totalSystemServiceTimeMicros3 = totalSystemServiceTimeMicros2;
                long totalSystemServiceTimeMicros4 = stats2.callCount;
                totalCallCountForUid = (int) (totalCallCountForUid + totalSystemServiceTimeMicros4);
                if (stats2.recordedCallCount > 0) {
                    totalTimeForUidUs += (stats2.callCount * stats2.recordedCpuTimeMicros) / stats2.recordedCallCount;
                } else if (totalRecordedCallCount > 0) {
                    totalTimeForUidUs += (stats2.callCount * totalRecordedCallTimeMicros) / totalRecordedCallCount;
                }
                j2--;
                totalSystemServiceTimeMicros2 = totalSystemServiceTimeMicros3;
            }
            long totalSystemServiceTimeMicros5 = totalSystemServiceTimeMicros2;
            long totalSystemServiceTimeMicros6 = totalCallCountForUid;
            if (totalSystemServiceTimeMicros6 < uid.mBinderCallCount && totalRecordedCallCount > 0) {
                totalTimeForUidUs += ((uid.mBinderCallCount - totalCallCountForUid) * totalRecordedCallTimeMicros) / totalRecordedCallCount;
            }
            uid.mSystemServiceTimeUs = totalTimeForUidUs;
            totalSystemServiceTimeMicros2 = totalSystemServiceTimeMicros5 + totalTimeForUidUs;
        }
        long totalSystemServiceTimeMicros7 = totalSystemServiceTimeMicros2;
        int i3 = 0;
        while (i3 < this.mUidStats.size()) {
            Uid uid2 = this.mUidStats.valueAt(i3);
            if (totalSystemServiceTimeMicros7 > 0) {
                totalSystemServiceTimeMicros = totalSystemServiceTimeMicros7;
                uid2.mProportionalSystemServiceUsage = uid2.mSystemServiceTimeUs / totalSystemServiceTimeMicros;
            } else {
                totalSystemServiceTimeMicros = totalSystemServiceTimeMicros7;
                uid2.mProportionalSystemServiceUsage = 0.0d;
            }
            i3++;
            totalSystemServiceTimeMicros7 = totalSystemServiceTimeMicros;
        }
    }

    public String[] getWifiIfaces() {
        String[] strArr;
        synchronized (this.mWifiNetworkLock) {
            strArr = this.mWifiIfaces;
        }
        return strArr;
    }

    public String[] getMobileIfaces() {
        String[] strArr;
        synchronized (this.mModemNetworkLock) {
            strArr = this.mModemIfaces;
        }
        return strArr;
    }

    @Override // android.os.BatteryStats
    public long getScreenOnTime(long elapsedRealtimeUs, int which) {
        return this.mScreenOnTimer.getTotalTimeLocked(elapsedRealtimeUs, which);
    }

    @Override // android.os.BatteryStats
    public int getScreenOnCount(int which) {
        return this.mScreenOnTimer.getCountLocked(which);
    }

    @Override // android.os.BatteryStats
    public long getScreenDozeTime(long elapsedRealtimeUs, int which) {
        return this.mScreenDozeTimer.getTotalTimeLocked(elapsedRealtimeUs, which);
    }

    @Override // android.os.BatteryStats
    public int getScreenDozeCount(int which) {
        return this.mScreenDozeTimer.getCountLocked(which);
    }

    @Override // android.os.BatteryStats
    public long getScreenBrightnessTime(int brightnessBin, long elapsedRealtimeUs, int which) {
        return this.mScreenBrightnessTimer[brightnessBin].getTotalTimeLocked(elapsedRealtimeUs, which);
    }

    /* JADX DEBUG: Method merged with bridge method */
    @Override // android.os.BatteryStats
    public Timer getScreenBrightnessTimer(int brightnessBin) {
        return this.mScreenBrightnessTimer[brightnessBin];
    }

    @Override // android.os.BatteryStats
    public int getDisplayCount() {
        return this.mPerDisplayBatteryStats.length;
    }

    @Override // android.os.BatteryStats
    public long getDisplayScreenOnTime(int display, long elapsedRealtimeUs) {
        return this.mPerDisplayBatteryStats[display].screenOnTimer.getTotalTimeLocked(elapsedRealtimeUs, 0);
    }

    @Override // android.os.BatteryStats
    public long getDisplayScreenDozeTime(int display, long elapsedRealtimeUs) {
        return this.mPerDisplayBatteryStats[display].screenDozeTimer.getTotalTimeLocked(elapsedRealtimeUs, 0);
    }

    @Override // android.os.BatteryStats
    public long getDisplayScreenBrightnessTime(int display, int brightnessBin, long elapsedRealtimeUs) {
        DisplayBatteryStats displayStats = this.mPerDisplayBatteryStats[display];
        return displayStats.screenBrightnessTimers[brightnessBin].getTotalTimeLocked(elapsedRealtimeUs, 0);
    }

    @Override // android.os.BatteryStats
    public long getInteractiveTime(long elapsedRealtimeUs, int which) {
        return this.mInteractiveTimer.getTotalTimeLocked(elapsedRealtimeUs, which);
    }

    @Override // android.os.BatteryStats
    public long getPowerSaveModeEnabledTime(long elapsedRealtimeUs, int which) {
        return this.mPowerSaveModeEnabledTimer.getTotalTimeLocked(elapsedRealtimeUs, which);
    }

    @Override // android.os.BatteryStats
    public int getPowerSaveModeEnabledCount(int which) {
        return this.mPowerSaveModeEnabledTimer.getCountLocked(which);
    }

    @Override // android.os.BatteryStats
    public long getDeviceIdleModeTime(int mode, long elapsedRealtimeUs, int which) {
        switch (mode) {
            case 1:
                return this.mDeviceIdleModeLightTimer.getTotalTimeLocked(elapsedRealtimeUs, which);
            case 2:
                return this.mDeviceIdleModeFullTimer.getTotalTimeLocked(elapsedRealtimeUs, which);
            default:
                return 0L;
        }
    }

    @Override // android.os.BatteryStats
    public int getDeviceIdleModeCount(int mode, int which) {
        switch (mode) {
            case 1:
                return this.mDeviceIdleModeLightTimer.getCountLocked(which);
            case 2:
                return this.mDeviceIdleModeFullTimer.getCountLocked(which);
            default:
                return 0;
        }
    }

    @Override // android.os.BatteryStats
    public long getLongestDeviceIdleModeTime(int mode) {
        switch (mode) {
            case 1:
                return this.mLongestLightIdleTimeMs;
            case 2:
                return this.mLongestFullIdleTimeMs;
            default:
                return 0L;
        }
    }

    @Override // android.os.BatteryStats
    public long getDeviceIdlingTime(int mode, long elapsedRealtimeUs, int which) {
        switch (mode) {
            case 1:
                return this.mDeviceLightIdlingTimer.getTotalTimeLocked(elapsedRealtimeUs, which);
            case 2:
                return this.mDeviceIdlingTimer.getTotalTimeLocked(elapsedRealtimeUs, which);
            default:
                return 0L;
        }
    }

    @Override // android.os.BatteryStats
    public int getDeviceIdlingCount(int mode, int which) {
        switch (mode) {
            case 1:
                return this.mDeviceLightIdlingTimer.getCountLocked(which);
            case 2:
                return this.mDeviceIdlingTimer.getCountLocked(which);
            default:
                return 0;
        }
    }

    @Override // android.os.BatteryStats
    public int getNumConnectivityChange(int which) {
        return this.mNumConnectivityChange;
    }

    @Override // android.os.BatteryStats
    public long getGpsSignalQualityTime(int strengthBin, long elapsedRealtimeUs, int which) {
        if (strengthBin >= 0) {
            StopwatchTimer[] stopwatchTimerArr = this.mGpsSignalQualityTimer;
            if (strengthBin >= stopwatchTimerArr.length) {
                return 0L;
            }
            return stopwatchTimerArr[strengthBin].getTotalTimeLocked(elapsedRealtimeUs, which);
        }
        return 0L;
    }

    @Override // android.os.BatteryStats
    public long getGpsBatteryDrainMaMs() {
        double opVolt = this.mPowerProfile.getAveragePower(PowerProfile.POWER_GPS_OPERATING_VOLTAGE) / 1000.0d;
        if (opVolt == 0.0d) {
            return 0L;
        }
        double energyUsedMaMs = 0.0d;
        long rawRealtimeUs = SystemClock.elapsedRealtime() * 1000;
        for (int i = 0; i < this.mGpsSignalQualityTimer.length; i++) {
            energyUsedMaMs += this.mPowerProfile.getAveragePower(PowerProfile.POWER_GPS_SIGNAL_QUALITY_BASED, i) * (getGpsSignalQualityTime(i, rawRealtimeUs, 0) / 1000);
        }
        return (long) energyUsedMaMs;
    }

    @Override // android.os.BatteryStats
    public long getPhoneOnTime(long elapsedRealtimeUs, int which) {
        return this.mPhoneOnTimer.getTotalTimeLocked(elapsedRealtimeUs, which);
    }

    @Override // android.os.BatteryStats
    public int getPhoneOnCount(int which) {
        return this.mPhoneOnTimer.getCountLocked(which);
    }

    @Override // android.os.BatteryStats
    public long getPhoneSignalStrengthTime(int strengthBin, long elapsedRealtimeUs, int which) {
        return this.mPhoneSignalStrengthsTimer[strengthBin].getTotalTimeLocked(elapsedRealtimeUs, which);
    }

    @Override // android.os.BatteryStats
    public long getPhoneSignalScanningTime(long elapsedRealtimeUs, int which) {
        return this.mPhoneSignalScanningTimer.getTotalTimeLocked(elapsedRealtimeUs, which);
    }

    /* JADX DEBUG: Method merged with bridge method */
    @Override // android.os.BatteryStats
    public Timer getPhoneSignalScanningTimer() {
        return this.mPhoneSignalScanningTimer;
    }

    @Override // android.os.BatteryStats
    public int getPhoneSignalStrengthCount(int strengthBin, int which) {
        return this.mPhoneSignalStrengthsTimer[strengthBin].getCountLocked(which);
    }

    /* JADX DEBUG: Method merged with bridge method */
    @Override // android.os.BatteryStats
    public Timer getPhoneSignalStrengthTimer(int strengthBin) {
        return this.mPhoneSignalStrengthsTimer[strengthBin];
    }

    @Override // android.os.BatteryStats
    public long getPhoneDataConnectionTime(int dataType, long elapsedRealtimeUs, int which) {
        return this.mPhoneDataConnectionsTimer[dataType].getTotalTimeLocked(elapsedRealtimeUs, which);
    }

    @Override // android.os.BatteryStats
    public int getPhoneDataConnectionCount(int dataType, int which) {
        return this.mPhoneDataConnectionsTimer[dataType].getCountLocked(which);
    }

    /* JADX DEBUG: Method merged with bridge method */
    @Override // android.os.BatteryStats
    public Timer getPhoneDataConnectionTimer(int dataType) {
        return this.mPhoneDataConnectionsTimer[dataType];
    }

    @Override // android.os.BatteryStats
    public long getActiveRadioDurationMs(int rat, int frequencyRange, int signalStrength, long elapsedRealtimeMs) {
        RadioAccessTechnologyBatteryStats stats = this.mPerRatBatteryStats[rat];
        if (stats == null) {
            return 0L;
        }
        int freqCount = stats.perStateTimers.length;
        if (frequencyRange < 0 || frequencyRange >= freqCount) {
            return 0L;
        }
        StopwatchTimer[] strengthTimers = stats.perStateTimers[frequencyRange];
        int strengthCount = strengthTimers.length;
        if (signalStrength < 0 || signalStrength >= strengthCount) {
            return 0L;
        }
        return stats.perStateTimers[frequencyRange][signalStrength].getTotalTimeLocked(elapsedRealtimeMs * 1000, 0) / 1000;
    }

    @Override // android.os.BatteryStats
    public long getActiveTxRadioDurationMs(int rat, int frequencyRange, int signalStrength, long elapsedRealtimeMs) {
        LongSamplingCounter counter;
        RadioAccessTechnologyBatteryStats stats = this.mPerRatBatteryStats[rat];
        if (stats == null || (counter = stats.getTxDurationCounter(frequencyRange, signalStrength, false)) == null) {
            return -1L;
        }
        return counter.getCountLocked(0);
    }

    @Override // android.os.BatteryStats
    public long getActiveRxRadioDurationMs(int rat, int frequencyRange, long elapsedRealtimeMs) {
        LongSamplingCounter counter;
        RadioAccessTechnologyBatteryStats stats = this.mPerRatBatteryStats[rat];
        if (stats == null || (counter = stats.getRxDurationCounter(frequencyRange, false)) == null) {
            return -1L;
        }
        return counter.getCountLocked(0);
    }

    @Override // android.os.BatteryStats
    public long getMobileRadioActiveTime(long elapsedRealtimeUs, int which) {
        return this.mMobileRadioActiveTimer.getTotalTimeLocked(elapsedRealtimeUs, which);
    }

    @Override // android.os.BatteryStats
    public int getMobileRadioActiveCount(int which) {
        return this.mMobileRadioActiveTimer.getCountLocked(which);
    }

    @Override // android.os.BatteryStats
    public long getMobileRadioActiveAdjustedTime(int which) {
        return this.mMobileRadioActiveAdjustedTime.getCountLocked(which);
    }

    @Override // android.os.BatteryStats
    public long getMobileRadioActiveUnknownTime(int which) {
        return this.mMobileRadioActiveUnknownTime.getCountLocked(which);
    }

    @Override // android.os.BatteryStats
    public int getMobileRadioActiveUnknownCount(int which) {
        return (int) this.mMobileRadioActiveUnknownCount.getCountLocked(which);
    }

    @Override // android.os.BatteryStats
    public long getWifiMulticastWakelockTime(long elapsedRealtimeUs, int which) {
        return this.mWifiMulticastWakelockTimer.getTotalTimeLocked(elapsedRealtimeUs, which);
    }

    @Override // android.os.BatteryStats
    public int getWifiMulticastWakelockCount(int which) {
        return this.mWifiMulticastWakelockTimer.getCountLocked(which);
    }

    @Override // android.os.BatteryStats
    public long getWifiOnTime(long elapsedRealtimeUs, int which) {
        return this.mWifiOnTimer.getTotalTimeLocked(elapsedRealtimeUs, which);
    }

    @Override // android.os.BatteryStats
    public long getWifiActiveTime(long elapsedRealtimeUs, int which) {
        return this.mWifiActiveTimer.getTotalTimeLocked(elapsedRealtimeUs, which);
    }

    @Override // android.os.BatteryStats
    public long getGlobalWifiRunningTime(long elapsedRealtimeUs, int which) {
        return this.mGlobalWifiRunningTimer.getTotalTimeLocked(elapsedRealtimeUs, which);
    }

    @Override // android.os.BatteryStats
    public long getWifiStateTime(int wifiState, long elapsedRealtimeUs, int which) {
        return this.mWifiStateTimer[wifiState].getTotalTimeLocked(elapsedRealtimeUs, which);
    }

    @Override // android.os.BatteryStats
    public int getWifiStateCount(int wifiState, int which) {
        return this.mWifiStateTimer[wifiState].getCountLocked(which);
    }

    /* JADX DEBUG: Method merged with bridge method */
    @Override // android.os.BatteryStats
    public Timer getWifiStateTimer(int wifiState) {
        return this.mWifiStateTimer[wifiState];
    }

    @Override // android.os.BatteryStats
    public long getWifiSupplStateTime(int state, long elapsedRealtimeUs, int which) {
        return this.mWifiSupplStateTimer[state].getTotalTimeLocked(elapsedRealtimeUs, which);
    }

    @Override // android.os.BatteryStats
    public int getWifiSupplStateCount(int state, int which) {
        return this.mWifiSupplStateTimer[state].getCountLocked(which);
    }

    /* JADX DEBUG: Method merged with bridge method */
    @Override // android.os.BatteryStats
    public Timer getWifiSupplStateTimer(int state) {
        return this.mWifiSupplStateTimer[state];
    }

    @Override // android.os.BatteryStats
    public long getWifiSignalStrengthTime(int strengthBin, long elapsedRealtimeUs, int which) {
        return this.mWifiSignalStrengthsTimer[strengthBin].getTotalTimeLocked(elapsedRealtimeUs, which);
    }

    @Override // android.os.BatteryStats
    public int getWifiSignalStrengthCount(int strengthBin, int which) {
        return this.mWifiSignalStrengthsTimer[strengthBin].getCountLocked(which);
    }

    /* JADX DEBUG: Method merged with bridge method */
    @Override // android.os.BatteryStats
    public Timer getWifiSignalStrengthTimer(int strengthBin) {
        return this.mWifiSignalStrengthsTimer[strengthBin];
    }

    @Override // android.os.BatteryStats
    public BatteryStats.ControllerActivityCounter getBluetoothControllerActivity() {
        return this.mBluetoothActivity;
    }

    @Override // android.os.BatteryStats
    public BatteryStats.ControllerActivityCounter getWifiControllerActivity() {
        return this.mWifiActivity;
    }

    @Override // android.os.BatteryStats
    public BatteryStats.ControllerActivityCounter getModemControllerActivity() {
        return this.mModemActivity;
    }

    @Override // android.os.BatteryStats
    public boolean hasBluetoothActivityReporting() {
        return this.mHasBluetoothReporting;
    }

    @Override // android.os.BatteryStats
    public boolean hasWifiActivityReporting() {
        return this.mHasWifiReporting;
    }

    @Override // android.os.BatteryStats
    public boolean hasModemActivityReporting() {
        return this.mHasModemReporting;
    }

    @Override // android.os.BatteryStats
    public long getFlashlightOnTime(long elapsedRealtimeUs, int which) {
        return this.mFlashlightOnTimer.getTotalTimeLocked(elapsedRealtimeUs, which);
    }

    @Override // android.os.BatteryStats
    public long getFlashlightOnCount(int which) {
        return this.mFlashlightOnTimer.getCountLocked(which);
    }

    @Override // android.os.BatteryStats
    public long getCameraOnTime(long elapsedRealtimeUs, int which) {
        return this.mCameraOnTimer.getTotalTimeLocked(elapsedRealtimeUs, which);
    }

    @Override // android.os.BatteryStats
    public long getBluetoothScanTime(long elapsedRealtimeUs, int which) {
        return this.mBluetoothScanTimer.getTotalTimeLocked(elapsedRealtimeUs, which);
    }

    @Override // android.os.BatteryStats
    public long getNetworkActivityBytes(int type, int which) {
        if (type >= 0) {
            LongSamplingCounter[] longSamplingCounterArr = this.mNetworkByteActivityCounters;
            if (type < longSamplingCounterArr.length) {
                return longSamplingCounterArr[type].getCountLocked(which);
            }
            return 0L;
        }
        return 0L;
    }

    @Override // android.os.BatteryStats
    public long getNetworkActivityPackets(int type, int which) {
        if (type >= 0) {
            LongSamplingCounter[] longSamplingCounterArr = this.mNetworkPacketActivityCounters;
            if (type < longSamplingCounterArr.length) {
                return longSamplingCounterArr[type].getCountLocked(which);
            }
            return 0L;
        }
        return 0L;
    }

    @Override // android.os.BatteryStats
    public long getBluetoothMeasuredBatteryConsumptionUC() {
        return getPowerBucketConsumptionUC(5);
    }

    @Override // android.os.BatteryStats
    public long getCpuMeasuredBatteryConsumptionUC() {
        return getPowerBucketConsumptionUC(3);
    }

    @Override // android.os.BatteryStats
    public long getGnssMeasuredBatteryConsumptionUC() {
        return getPowerBucketConsumptionUC(6);
    }

    @Override // android.os.BatteryStats
    public long getMobileRadioMeasuredBatteryConsumptionUC() {
        return getPowerBucketConsumptionUC(7);
    }

    @Override // android.os.BatteryStats
    public long getScreenOnMeasuredBatteryConsumptionUC() {
        return getPowerBucketConsumptionUC(0);
    }

    @Override // android.os.BatteryStats
    public long getScreenDozeMeasuredBatteryConsumptionUC() {
        return getPowerBucketConsumptionUC(1);
    }

    @Override // android.os.BatteryStats
    public long getWifiMeasuredBatteryConsumptionUC() {
        return getPowerBucketConsumptionUC(4);
    }

    private long getPowerBucketConsumptionUC(int bucket) {
        MeasuredEnergyStats measuredEnergyStats = this.mGlobalMeasuredEnergyStats;
        if (measuredEnergyStats == null) {
            return -1L;
        }
        return measuredEnergyStats.getAccumulatedStandardBucketCharge(bucket);
    }

    @Override // android.os.BatteryStats
    public long[] getCustomConsumerMeasuredBatteryConsumptionUC() {
        MeasuredEnergyStats measuredEnergyStats = this.mGlobalMeasuredEnergyStats;
        if (measuredEnergyStats == null) {
            return null;
        }
        return measuredEnergyStats.getAccumulatedCustomBucketCharges();
    }

    @Override // android.os.BatteryStats
    public String[] getCustomEnergyConsumerNames() {
        MeasuredEnergyStats.Config config = this.mMeasuredEnergyStatsConfig;
        if (config == null) {
            return new String[0];
        }
        String[] names = config.getCustomBucketNames();
        for (int i = 0; i < names.length; i++) {
            if (TextUtils.isEmpty(names[i])) {
                names[i] = "CUSTOM_1000" + i;
            }
        }
        return names;
    }

    @Override // android.os.BatteryStats
    public long getStartClockTime() {
        long currentTimeMs = this.mClock.currentTimeMillis();
        if (currentTimeMs <= MILLISECONDS_IN_YEAR || this.mStartClockTimeMs >= currentTimeMs - MILLISECONDS_IN_YEAR) {
            long j = this.mStartClockTimeMs;
            if (j <= currentTimeMs) {
                return j;
            }
        }
        recordCurrentTimeChangeLocked(currentTimeMs, this.mClock.elapsedRealtime(), this.mClock.uptimeMillis());
        return currentTimeMs - (this.mClock.elapsedRealtime() - (this.mRealtimeStartUs / 1000));
    }

    @Override // android.os.BatteryStats
    public String getStartPlatformVersion() {
        return this.mStartPlatformVersion;
    }

    @Override // android.os.BatteryStats
    public String getEndPlatformVersion() {
        return this.mEndPlatformVersion;
    }

    @Override // android.os.BatteryStats
    public int getParcelVersion() {
        return 208;
    }

    @Override // android.os.BatteryStats
    public boolean getIsOnBattery() {
        return this.mOnBattery;
    }

    @Override // android.os.BatteryStats
    public long getStatsStartRealtime() {
        return this.mRealtimeStartUs;
    }

    @Override // android.os.BatteryStats
    public SparseArray<? extends BatteryStats.Uid> getUidStats() {
        return this.mUidStats;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public static <T extends TimeBaseObs> boolean resetIfNotNull(T t, boolean detachIfReset, long elapsedRealtimeUs) {
        if (t != null) {
            return t.reset(detachIfReset, elapsedRealtimeUs);
        }
        return true;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public static <T extends TimeBaseObs> boolean resetIfNotNull(T[] t, boolean detachIfReset, long elapsedRealtimeUs) {
        if (t != null) {
            boolean ret = true;
            for (T t2 : t) {
                ret &= resetIfNotNull(t2, detachIfReset, elapsedRealtimeUs);
            }
            return ret;
        }
        return true;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public static <T extends TimeBaseObs> boolean resetIfNotNull(T[][] t, boolean detachIfReset, long elapsedRealtimeUs) {
        if (t != null) {
            boolean ret = true;
            for (T[] tArr : t) {
                ret &= resetIfNotNull(tArr, detachIfReset, elapsedRealtimeUs);
            }
            return ret;
        }
        return true;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public static boolean resetIfNotNull(ControllerActivityCounterImpl counter, boolean detachIfReset, long elapsedRealtimeUs) {
        if (counter != null) {
            counter.reset(detachIfReset, elapsedRealtimeUs);
            return true;
        }
        return true;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public static <T extends TimeBaseObs> void detachIfNotNull(T t) {
        if (t != null) {
            t.detach();
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public static <T extends TimeBaseObs> void detachIfNotNull(T[] t) {
        if (t != null) {
            for (T t2 : t) {
                detachIfNotNull(t2);
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public static <T extends TimeBaseObs> void detachIfNotNull(T[][] t) {
        if (t != null) {
            for (T[] tArr : t) {
                detachIfNotNull(tArr);
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public static void detachIfNotNull(ControllerActivityCounterImpl counter) {
        if (counter != null) {
            counter.detach();
        }
    }

    /* JADX INFO: Access modifiers changed from: protected */
    /* loaded from: classes4.dex */
    public static class BinderCallStats {
        static final Comparator<BinderCallStats> COMPARATOR = Comparator.comparing(new Function() { // from class: com.android.internal.os.BatteryStatsImpl$BinderCallStats$$ExternalSyntheticLambda0
            @Override // java.util.function.Function
            public final Object apply(Object obj) {
                return ((BatteryStatsImpl.BinderCallStats) obj).getClassName();
            }
        }).thenComparing(new Function() { // from class: com.android.internal.os.BatteryStatsImpl$BinderCallStats$$ExternalSyntheticLambda1
            @Override // java.util.function.Function
            public final Object apply(Object obj) {
                return ((BatteryStatsImpl.BinderCallStats) obj).getMethodName();
            }
        });
        public Class<? extends Binder> binderClass;
        public long callCount;
        public String methodName;
        public long recordedCallCount;
        public long recordedCpuTimeMicros;
        public int transactionCode;

        protected BinderCallStats() {
        }

        public int hashCode() {
            return (this.binderClass.hashCode() * 31) + this.transactionCode;
        }

        public boolean equals(Object obj) {
            if (obj instanceof BinderCallStats) {
                BinderCallStats bcsk = (BinderCallStats) obj;
                return this.binderClass.equals(bcsk.binderClass) && this.transactionCode == bcsk.transactionCode;
            }
            return false;
        }

        public String getClassName() {
            return this.binderClass.getName();
        }

        public String getMethodName() {
            return this.methodName;
        }

        public void ensureMethodName(BinderTransactionNameResolver resolver) {
            if (this.methodName == null) {
                this.methodName = resolver.getMethodName(this.binderClass, this.transactionCode);
            }
        }

        public String toString() {
            return "BinderCallStats{" + this.binderClass + " transaction=" + this.transactionCode + " callCount=" + this.callCount + " recordedCallCount=" + this.recordedCallCount + " recorderCpuTimeMicros=" + this.recordedCpuTimeMicros + "}";
        }
    }

    /* loaded from: classes4.dex */
    public static class Uid extends BatteryStats.Uid {
        static final int NO_BATCHED_SCAN_STARTED = -1;
        private static BinderCallStats sTempBinderCallStats = new BinderCallStats();
        DualTimer mAggregatedPartialWakelockTimer;
        StopwatchTimer mAudioTurnedOnTimer;
        private long mBinderCallCount;
        private final ArraySet<BinderCallStats> mBinderCallStats;
        private ControllerActivityCounterImpl mBluetoothControllerActivity;
        Counter mBluetoothScanResultBgCounter;
        Counter mBluetoothScanResultCounter;
        DualTimer mBluetoothScanTimer;
        DualTimer mBluetoothUnoptimizedScanTimer;
        protected BatteryStatsImpl mBsi;
        StopwatchTimer mCameraTurnedOnTimer;
        SparseArray<ChildUid> mChildUids;
        TimeMultiStateCounter mCpuActiveTimeMs;
        LongSamplingCounter[][] mCpuClusterSpeedTimesUs;
        LongSamplingCounterArray mCpuClusterTimesMs;
        LongSamplingCounterArray mCpuFreqTimeMs;
        long mCurStepSystemTimeMs;
        long mCurStepUserTimeMs;
        StopwatchTimer mFlashlightTurnedOnTimer;
        StopwatchTimer mForegroundActivityTimer;
        StopwatchTimer mForegroundServiceTimer;
        boolean mFullWifiLockOut;
        StopwatchTimer mFullWifiLockTimer;
        boolean mInForegroundService;
        final ArrayMap<String, SparseIntArray> mJobCompletions;
        final OverflowArrayMap<DualTimer> mJobStats;
        Counter mJobsDeferredCount;
        Counter mJobsDeferredEventCount;
        final Counter[] mJobsFreshnessBuckets;
        LongSamplingCounter mJobsFreshnessTimeMs;
        long mLastStepSystemTimeMs;
        long mLastStepUserTimeMs;
        LongSamplingCounter mMobileRadioActiveCount;
        TimeMultiStateCounter mMobileRadioActiveTime;
        private LongSamplingCounter mMobileRadioApWakeupCount;
        private ControllerActivityCounterImpl mModemControllerActivity;
        LongSamplingCounter[] mNetworkByteActivityCounters;
        LongSamplingCounter[] mNetworkPacketActivityCounters;
        public final TimeBase mOnBatteryBackgroundTimeBase;
        public final TimeBase mOnBatteryScreenOffBackgroundTimeBase;
        final ArrayMap<String, Pkg> mPackageStats;
        final SparseArray<BatteryStats.Uid.Pid> mPids;
        TimeInFreqMultiStateCounter mProcStateScreenOffTimeMs;
        TimeInFreqMultiStateCounter mProcStateTimeMs;
        int mProcessState;
        StopwatchTimer[] mProcessStateTimer;
        final ArrayMap<String, Proc> mProcessStats;
        private double mProportionalSystemServiceUsage;
        LongSamplingCounterArray mScreenOffCpuFreqTimeMs;
        final SparseArray<Sensor> mSensorStats;
        final OverflowArrayMap<DualTimer> mSyncStats;
        LongSamplingCounter mSystemCpuTime;
        private long mSystemServiceTimeUs;
        final int mUid;
        private MeasuredEnergyStats mUidMeasuredEnergyStats;
        Counter[] mUserActivityCounters;
        LongSamplingCounter mUserCpuTime;
        BatchTimer mVibratorOnTimer;
        StopwatchTimer mVideoTurnedOnTimer;
        final OverflowArrayMap<Wakelock> mWakelockStats;
        int mWifiBatchedScanBinStarted;
        StopwatchTimer[] mWifiBatchedScanTimer;
        private ControllerActivityCounterImpl mWifiControllerActivity;
        StopwatchTimer mWifiMulticastTimer;
        int mWifiMulticastWakelockCount;
        private LongSamplingCounter mWifiRadioApWakeupCount;
        boolean mWifiRunning;
        StopwatchTimer mWifiRunningTimer;
        boolean mWifiScanStarted;
        DualTimer mWifiScanTimer;

        public Uid(BatteryStatsImpl bsi, int uid) {
            this(bsi, uid, bsi.mClock.elapsedRealtime(), bsi.mClock.uptimeMillis());
        }

        public Uid(BatteryStatsImpl bsi, int uid, long elapsedRealtimeMs, long uptimeMs) {
            this.mWifiBatchedScanBinStarted = -1;
            this.mProcessState = 7;
            this.mInForegroundService = false;
            this.mJobCompletions = new ArrayMap<>();
            this.mSensorStats = new SparseArray<>();
            this.mProcessStats = new ArrayMap<>();
            this.mPackageStats = new ArrayMap<>();
            this.mPids = new SparseArray<>();
            this.mBinderCallStats = new ArraySet<>();
            this.mBsi = bsi;
            this.mUid = uid;
            TimeBase timeBase = new TimeBase(false);
            this.mOnBatteryBackgroundTimeBase = timeBase;
            timeBase.init(uptimeMs * 1000, elapsedRealtimeMs * 1000);
            TimeBase timeBase2 = new TimeBase(false);
            this.mOnBatteryScreenOffBackgroundTimeBase = timeBase2;
            timeBase2.init(uptimeMs * 1000, elapsedRealtimeMs * 1000);
            this.mUserCpuTime = new LongSamplingCounter(this.mBsi.mOnBatteryTimeBase);
            this.mSystemCpuTime = new LongSamplingCounter(this.mBsi.mOnBatteryTimeBase);
            this.mCpuClusterTimesMs = new LongSamplingCounterArray(this.mBsi.mOnBatteryTimeBase);
            BatteryStatsImpl batteryStatsImpl = this.mBsi;
            Objects.requireNonNull(batteryStatsImpl);
            this.mWakelockStats = new OverflowArrayMap<Wakelock>(batteryStatsImpl, uid) { // from class: com.android.internal.os.BatteryStatsImpl.Uid.1
                /* JADX WARN: 'super' call moved to the top of the method (can break code semantics) */
                {
                    super(uid);
                    Objects.requireNonNull(batteryStatsImpl);
                }

                /* JADX DEBUG: Method merged with bridge method */
                /* JADX WARN: Can't rename method to resolve collision */
                @Override // com.android.internal.os.BatteryStatsImpl.OverflowArrayMap
                public Wakelock instantiateObject() {
                    return new Wakelock(Uid.this.mBsi, Uid.this);
                }
            };
            BatteryStatsImpl batteryStatsImpl2 = this.mBsi;
            Objects.requireNonNull(batteryStatsImpl2);
            this.mSyncStats = new OverflowArrayMap<DualTimer>(batteryStatsImpl2, uid) { // from class: com.android.internal.os.BatteryStatsImpl.Uid.2
                /* JADX WARN: 'super' call moved to the top of the method (can break code semantics) */
                {
                    super(uid);
                    Objects.requireNonNull(batteryStatsImpl2);
                }

                /* JADX DEBUG: Method merged with bridge method */
                /* JADX WARN: Can't rename method to resolve collision */
                @Override // com.android.internal.os.BatteryStatsImpl.OverflowArrayMap
                public DualTimer instantiateObject() {
                    Clock clock = Uid.this.mBsi.mClock;
                    Uid uid2 = Uid.this;
                    return new DualTimer(clock, uid2, 13, null, uid2.mBsi.mOnBatteryTimeBase, Uid.this.mOnBatteryBackgroundTimeBase);
                }
            };
            BatteryStatsImpl batteryStatsImpl3 = this.mBsi;
            Objects.requireNonNull(batteryStatsImpl3);
            this.mJobStats = new OverflowArrayMap<DualTimer>(batteryStatsImpl3, uid) { // from class: com.android.internal.os.BatteryStatsImpl.Uid.3
                /* JADX WARN: 'super' call moved to the top of the method (can break code semantics) */
                {
                    super(uid);
                    Objects.requireNonNull(batteryStatsImpl3);
                }

                /* JADX DEBUG: Method merged with bridge method */
                /* JADX WARN: Can't rename method to resolve collision */
                @Override // com.android.internal.os.BatteryStatsImpl.OverflowArrayMap
                public DualTimer instantiateObject() {
                    Clock clock = Uid.this.mBsi.mClock;
                    Uid uid2 = Uid.this;
                    return new DualTimer(clock, uid2, 14, null, uid2.mBsi.mOnBatteryTimeBase, Uid.this.mOnBatteryBackgroundTimeBase);
                }
            };
            this.mWifiRunningTimer = new StopwatchTimer(this.mBsi.mClock, this, 4, this.mBsi.mWifiRunningTimers, this.mBsi.mOnBatteryTimeBase);
            this.mFullWifiLockTimer = new StopwatchTimer(this.mBsi.mClock, this, 5, this.mBsi.mFullWifiLockTimers, this.mBsi.mOnBatteryTimeBase);
            this.mWifiScanTimer = new DualTimer(this.mBsi.mClock, this, 6, this.mBsi.mWifiScanTimers, this.mBsi.mOnBatteryTimeBase, timeBase);
            this.mWifiBatchedScanTimer = new StopwatchTimer[5];
            this.mWifiMulticastTimer = new StopwatchTimer(this.mBsi.mClock, this, 7, this.mBsi.mWifiMulticastTimers, this.mBsi.mOnBatteryTimeBase);
            this.mProcessStateTimer = new StopwatchTimer[7];
            this.mJobsDeferredEventCount = new Counter(this.mBsi.mOnBatteryTimeBase);
            this.mJobsDeferredCount = new Counter(this.mBsi.mOnBatteryTimeBase);
            this.mJobsFreshnessTimeMs = new LongSamplingCounter(this.mBsi.mOnBatteryTimeBase);
            this.mJobsFreshnessBuckets = new Counter[BatteryStats.JOB_FRESHNESS_BUCKETS.length];
        }

        public void setProcessStateForTest(int procState, long elapsedTimeMs) {
            this.mProcessState = procState;
            getProcStateTimeCounter(elapsedTimeMs).setState(procState, elapsedTimeMs);
            getProcStateScreenOffTimeCounter(elapsedTimeMs).setState(procState, elapsedTimeMs);
            int batteryConsumerProcessState = BatteryStats.mapUidProcessStateToBatteryConsumerProcessState(procState);
            getCpuActiveTimeCounter().setState(batteryConsumerProcessState, elapsedTimeMs);
            getMobileRadioActiveTimeCounter().setState(batteryConsumerProcessState, elapsedTimeMs);
            ControllerActivityCounterImpl wifiControllerActivity = getWifiControllerActivity();
            if (wifiControllerActivity != null) {
                wifiControllerActivity.setState(batteryConsumerProcessState, elapsedTimeMs);
            }
            ControllerActivityCounterImpl bluetoothControllerActivity = getBluetoothControllerActivity();
            if (bluetoothControllerActivity != null) {
                bluetoothControllerActivity.setState(batteryConsumerProcessState, elapsedTimeMs);
            }
            MeasuredEnergyStats energyStats = getOrCreateMeasuredEnergyStatsIfSupportedLocked();
            if (energyStats != null) {
                energyStats.setState(batteryConsumerProcessState, elapsedTimeMs);
            }
        }

        @Override // android.os.BatteryStats.Uid
        public long[] getCpuFreqTimes(int which) {
            return nullIfAllZeros(this.mCpuFreqTimeMs, which);
        }

        @Override // android.os.BatteryStats.Uid
        public long[] getScreenOffCpuFreqTimes(int which) {
            return nullIfAllZeros(this.mScreenOffCpuFreqTimeMs, which);
        }

        /* JADX INFO: Access modifiers changed from: private */
        public TimeMultiStateCounter getCpuActiveTimeCounter() {
            if (this.mCpuActiveTimeMs == null) {
                long timestampMs = this.mBsi.mClock.elapsedRealtime();
                TimeMultiStateCounter timeMultiStateCounter = new TimeMultiStateCounter(this.mBsi.mOnBatteryTimeBase, 5, timestampMs);
                this.mCpuActiveTimeMs = timeMultiStateCounter;
                timeMultiStateCounter.setState(BatteryStats.mapUidProcessStateToBatteryConsumerProcessState(this.mProcessState), timestampMs);
            }
            return this.mCpuActiveTimeMs;
        }

        @Override // android.os.BatteryStats.Uid
        public long getCpuActiveTime() {
            if (this.mCpuActiveTimeMs == null) {
                return 0L;
            }
            long activeTime = 0;
            for (int procState = 0; procState < 5; procState++) {
                activeTime += this.mCpuActiveTimeMs.getCountForProcessState(procState);
            }
            return activeTime;
        }

        @Override // android.os.BatteryStats.Uid
        public long getCpuActiveTime(int procState) {
            TimeMultiStateCounter timeMultiStateCounter = this.mCpuActiveTimeMs;
            if (timeMultiStateCounter == null || procState < 0 || procState >= 5) {
                return 0L;
            }
            return timeMultiStateCounter.getCountForProcessState(procState);
        }

        @Override // android.os.BatteryStats.Uid
        public long[] getCpuClusterTimes() {
            return nullIfAllZeros(this.mCpuClusterTimesMs, 0);
        }

        @Override // android.os.BatteryStats.Uid
        public boolean getCpuFreqTimes(long[] timesInFreqMs, int procState) {
            if (procState < 0 || procState >= 7 || this.mProcStateTimeMs == null) {
                return false;
            }
            if (!this.mBsi.mPerProcStateCpuTimesAvailable) {
                this.mProcStateTimeMs = null;
                return false;
            }
            return this.mProcStateTimeMs.getCountsLocked(timesInFreqMs, procState);
        }

        @Override // android.os.BatteryStats.Uid
        public boolean getScreenOffCpuFreqTimes(long[] timesInFreqMs, int procState) {
            if (procState < 0 || procState >= 7 || this.mProcStateScreenOffTimeMs == null) {
                return false;
            }
            if (!this.mBsi.mPerProcStateCpuTimesAvailable) {
                this.mProcStateScreenOffTimeMs = null;
                return false;
            }
            return this.mProcStateScreenOffTimeMs.getCountsLocked(timesInFreqMs, procState);
        }

        public long getBinderCallCount() {
            return this.mBinderCallCount;
        }

        public ArraySet<BinderCallStats> getBinderCallStats() {
            return this.mBinderCallStats;
        }

        @Override // android.os.BatteryStats.Uid
        public double getProportionalSystemServiceUsage() {
            return this.mProportionalSystemServiceUsage;
        }

        public void addIsolatedUid(int isolatedUid) {
            SparseArray<ChildUid> sparseArray = this.mChildUids;
            if (sparseArray == null) {
                this.mChildUids = new SparseArray<>();
            } else if (sparseArray.indexOfKey(isolatedUid) >= 0) {
                return;
            }
            this.mChildUids.put(isolatedUid, new ChildUid());
        }

        public void removeIsolatedUid(int isolatedUid) {
            SparseArray<ChildUid> sparseArray = this.mChildUids;
            int idx = sparseArray == null ? -1 : sparseArray.indexOfKey(isolatedUid);
            if (idx < 0) {
                return;
            }
            this.mChildUids.remove(idx);
        }

        ChildUid getChildUid(int childUid) {
            SparseArray<ChildUid> sparseArray = this.mChildUids;
            if (sparseArray == null) {
                return null;
            }
            return sparseArray.get(childUid);
        }

        private long[] nullIfAllZeros(LongSamplingCounterArray cpuTimesMs, int which) {
            long[] counts;
            if (cpuTimesMs == null || (counts = cpuTimesMs.getCountsLocked(which)) == null) {
                return null;
            }
            for (int i = counts.length - 1; i >= 0; i--) {
                if (counts[i] != 0) {
                    return counts;
                }
            }
            return null;
        }

        private void ensureMultiStateCounters(long timestampMs) {
            if (this.mProcStateTimeMs == null) {
                this.mProcStateTimeMs = new TimeInFreqMultiStateCounter(this.mBsi.mOnBatteryTimeBase, 8, this.mBsi.getCpuFreqCount(), timestampMs);
                this.mProcStateScreenOffTimeMs = new TimeInFreqMultiStateCounter(this.mBsi.mOnBatteryScreenOffTimeBase, 8, this.mBsi.getCpuFreqCount(), timestampMs);
            }
        }

        /* JADX INFO: Access modifiers changed from: private */
        public TimeInFreqMultiStateCounter getProcStateTimeCounter(long timestampMs) {
            ensureMultiStateCounters(timestampMs);
            return this.mProcStateTimeMs;
        }

        /* JADX INFO: Access modifiers changed from: private */
        public TimeInFreqMultiStateCounter getProcStateScreenOffTimeCounter(long timestampMs) {
            ensureMultiStateCounters(timestampMs);
            return this.mProcStateScreenOffTimeMs;
        }

        /* JADX DEBUG: Method merged with bridge method */
        @Override // android.os.BatteryStats.Uid
        public Timer getAggregatedPartialWakelockTimer() {
            return this.mAggregatedPartialWakelockTimer;
        }

        @Override // android.os.BatteryStats.Uid
        public ArrayMap<String, ? extends BatteryStats.Uid.Wakelock> getWakelockStats() {
            return this.mWakelockStats.getMap();
        }

        /* JADX DEBUG: Method merged with bridge method */
        @Override // android.os.BatteryStats.Uid
        public Timer getMulticastWakelockStats() {
            return this.mWifiMulticastTimer;
        }

        @Override // android.os.BatteryStats.Uid
        public ArrayMap<String, ? extends BatteryStats.Timer> getSyncStats() {
            return this.mSyncStats.getMap();
        }

        @Override // android.os.BatteryStats.Uid
        public ArrayMap<String, ? extends BatteryStats.Timer> getJobStats() {
            return this.mJobStats.getMap();
        }

        @Override // android.os.BatteryStats.Uid
        public ArrayMap<String, SparseIntArray> getJobCompletionStats() {
            return this.mJobCompletions;
        }

        @Override // android.os.BatteryStats.Uid
        public SparseArray<? extends BatteryStats.Uid.Sensor> getSensorStats() {
            return this.mSensorStats;
        }

        @Override // android.os.BatteryStats.Uid
        public ArrayMap<String, ? extends BatteryStats.Uid.Proc> getProcessStats() {
            return this.mProcessStats;
        }

        @Override // android.os.BatteryStats.Uid
        public ArrayMap<String, ? extends BatteryStats.Uid.Pkg> getPackageStats() {
            return this.mPackageStats;
        }

        @Override // android.os.BatteryStats.Uid
        public int getUid() {
            return this.mUid;
        }

        @Override // android.os.BatteryStats.Uid
        public void noteWifiRunningLocked(long elapsedRealtimeMs) {
            if (!this.mWifiRunning) {
                this.mWifiRunning = true;
                if (this.mWifiRunningTimer == null) {
                    this.mWifiRunningTimer = new StopwatchTimer(this.mBsi.mClock, this, 4, this.mBsi.mWifiRunningTimers, this.mBsi.mOnBatteryTimeBase);
                }
                this.mWifiRunningTimer.startRunningLocked(elapsedRealtimeMs);
            }
        }

        @Override // android.os.BatteryStats.Uid
        public void noteWifiStoppedLocked(long elapsedRealtimeMs) {
            if (this.mWifiRunning) {
                this.mWifiRunning = false;
                this.mWifiRunningTimer.stopRunningLocked(elapsedRealtimeMs);
            }
        }

        @Override // android.os.BatteryStats.Uid
        public void noteFullWifiLockAcquiredLocked(long elapsedRealtimeMs) {
            if (!this.mFullWifiLockOut) {
                this.mFullWifiLockOut = true;
                if (this.mFullWifiLockTimer == null) {
                    this.mFullWifiLockTimer = new StopwatchTimer(this.mBsi.mClock, this, 5, this.mBsi.mFullWifiLockTimers, this.mBsi.mOnBatteryTimeBase);
                }
                this.mFullWifiLockTimer.startRunningLocked(elapsedRealtimeMs);
            }
        }

        @Override // android.os.BatteryStats.Uid
        public void noteFullWifiLockReleasedLocked(long elapsedRealtimeMs) {
            if (this.mFullWifiLockOut) {
                this.mFullWifiLockOut = false;
                this.mFullWifiLockTimer.stopRunningLocked(elapsedRealtimeMs);
            }
        }

        @Override // android.os.BatteryStats.Uid
        public void noteWifiScanStartedLocked(long elapsedRealtimeMs) {
            if (!this.mWifiScanStarted) {
                this.mWifiScanStarted = true;
                if (this.mWifiScanTimer == null) {
                    this.mWifiScanTimer = new DualTimer(this.mBsi.mClock, this, 6, this.mBsi.mWifiScanTimers, this.mBsi.mOnBatteryTimeBase, this.mOnBatteryBackgroundTimeBase);
                }
                this.mWifiScanTimer.startRunningLocked(elapsedRealtimeMs);
            }
        }

        @Override // android.os.BatteryStats.Uid
        public void noteWifiScanStoppedLocked(long elapsedRealtimeMs) {
            if (this.mWifiScanStarted) {
                this.mWifiScanStarted = false;
                this.mWifiScanTimer.stopRunningLocked(elapsedRealtimeMs);
            }
        }

        @Override // android.os.BatteryStats.Uid
        public void noteWifiBatchedScanStartedLocked(int csph, long elapsedRealtimeMs) {
            int bin = 0;
            while (csph > 8 && bin < 4) {
                csph >>= 3;
                bin++;
            }
            int i = this.mWifiBatchedScanBinStarted;
            if (i == bin) {
                return;
            }
            if (i != -1) {
                this.mWifiBatchedScanTimer[i].stopRunningLocked(elapsedRealtimeMs);
            }
            this.mWifiBatchedScanBinStarted = bin;
            if (this.mWifiBatchedScanTimer[bin] == null) {
                makeWifiBatchedScanBin(bin, null);
            }
            this.mWifiBatchedScanTimer[bin].startRunningLocked(elapsedRealtimeMs);
        }

        @Override // android.os.BatteryStats.Uid
        public void noteWifiBatchedScanStoppedLocked(long elapsedRealtimeMs) {
            int i = this.mWifiBatchedScanBinStarted;
            if (i != -1) {
                this.mWifiBatchedScanTimer[i].stopRunningLocked(elapsedRealtimeMs);
                this.mWifiBatchedScanBinStarted = -1;
            }
        }

        @Override // android.os.BatteryStats.Uid
        public void noteWifiMulticastEnabledLocked(long elapsedRealtimeMs) {
            if (this.mWifiMulticastWakelockCount == 0) {
                if (this.mWifiMulticastTimer == null) {
                    this.mWifiMulticastTimer = new StopwatchTimer(this.mBsi.mClock, this, 7, this.mBsi.mWifiMulticastTimers, this.mBsi.mOnBatteryTimeBase);
                }
                this.mWifiMulticastTimer.startRunningLocked(elapsedRealtimeMs);
            }
            this.mWifiMulticastWakelockCount++;
        }

        @Override // android.os.BatteryStats.Uid
        public void noteWifiMulticastDisabledLocked(long elapsedRealtimeMs) {
            int i = this.mWifiMulticastWakelockCount;
            if (i == 0) {
                return;
            }
            int i2 = i - 1;
            this.mWifiMulticastWakelockCount = i2;
            if (i2 == 0) {
                this.mWifiMulticastTimer.stopRunningLocked(elapsedRealtimeMs);
            }
        }

        /* JADX DEBUG: Method merged with bridge method */
        @Override // android.os.BatteryStats.Uid
        public ControllerActivityCounterImpl getWifiControllerActivity() {
            return this.mWifiControllerActivity;
        }

        /* JADX DEBUG: Method merged with bridge method */
        @Override // android.os.BatteryStats.Uid
        public ControllerActivityCounterImpl getBluetoothControllerActivity() {
            return this.mBluetoothControllerActivity;
        }

        @Override // android.os.BatteryStats.Uid
        public BatteryStats.ControllerActivityCounter getModemControllerActivity() {
            return this.mModemControllerActivity;
        }

        public ControllerActivityCounterImpl getOrCreateWifiControllerActivityLocked() {
            if (this.mWifiControllerActivity == null) {
                this.mWifiControllerActivity = new ControllerActivityCounterImpl(this.mBsi.mClock, this.mBsi.mOnBatteryTimeBase, 1);
            }
            return this.mWifiControllerActivity;
        }

        public ControllerActivityCounterImpl getOrCreateBluetoothControllerActivityLocked() {
            if (this.mBluetoothControllerActivity == null) {
                this.mBluetoothControllerActivity = new ControllerActivityCounterImpl(this.mBsi.mClock, this.mBsi.mOnBatteryTimeBase, 1);
            }
            return this.mBluetoothControllerActivity;
        }

        public ControllerActivityCounterImpl getOrCreateModemControllerActivityLocked() {
            if (this.mModemControllerActivity == null) {
                this.mModemControllerActivity = new ControllerActivityCounterImpl(this.mBsi.mClock, this.mBsi.mOnBatteryTimeBase, ModemActivityInfo.getNumTxPowerLevels());
            }
            return this.mModemControllerActivity;
        }

        private MeasuredEnergyStats getOrCreateMeasuredEnergyStatsLocked() {
            if (this.mUidMeasuredEnergyStats == null) {
                this.mUidMeasuredEnergyStats = new MeasuredEnergyStats(this.mBsi.mMeasuredEnergyStatsConfig);
            }
            return this.mUidMeasuredEnergyStats;
        }

        private MeasuredEnergyStats getOrCreateMeasuredEnergyStatsIfSupportedLocked() {
            if (this.mUidMeasuredEnergyStats == null && this.mBsi.mMeasuredEnergyStatsConfig != null) {
                this.mUidMeasuredEnergyStats = new MeasuredEnergyStats(this.mBsi.mMeasuredEnergyStatsConfig);
            }
            return this.mUidMeasuredEnergyStats;
        }

        /* JADX INFO: Access modifiers changed from: private */
        public void addChargeToStandardBucketLocked(long chargeDeltaUC, int powerBucket, long timestampMs) {
            MeasuredEnergyStats measuredEnergyStats = getOrCreateMeasuredEnergyStatsLocked();
            measuredEnergyStats.updateStandardBucket(powerBucket, chargeDeltaUC, timestampMs);
        }

        /* JADX INFO: Access modifiers changed from: private */
        public void addChargeToCustomBucketLocked(long chargeDeltaUC, int powerBucket) {
            getOrCreateMeasuredEnergyStatsLocked().updateCustomBucket(powerBucket, chargeDeltaUC, this.mBsi.mClock.elapsedRealtime());
        }

        public long getMeasuredBatteryConsumptionUC(int bucket) {
            if (this.mBsi.mGlobalMeasuredEnergyStats == null || !this.mBsi.mGlobalMeasuredEnergyStats.isStandardBucketSupported(bucket)) {
                return -1L;
            }
            MeasuredEnergyStats measuredEnergyStats = this.mUidMeasuredEnergyStats;
            if (measuredEnergyStats == null) {
                return 0L;
            }
            return measuredEnergyStats.getAccumulatedStandardBucketCharge(bucket);
        }

        public long getMeasuredBatteryConsumptionUC(int bucket, int processState) {
            if (this.mBsi.mGlobalMeasuredEnergyStats == null || !this.mBsi.mGlobalMeasuredEnergyStats.isStandardBucketSupported(bucket)) {
                return -1L;
            }
            MeasuredEnergyStats measuredEnergyStats = this.mUidMeasuredEnergyStats;
            if (measuredEnergyStats == null) {
                return 0L;
            }
            return measuredEnergyStats.getAccumulatedStandardBucketCharge(bucket, processState);
        }

        @Override // android.os.BatteryStats.Uid
        public long[] getCustomConsumerMeasuredBatteryConsumptionUC() {
            if (this.mBsi.mGlobalMeasuredEnergyStats == null) {
                return null;
            }
            MeasuredEnergyStats measuredEnergyStats = this.mUidMeasuredEnergyStats;
            if (measuredEnergyStats == null) {
                return new long[this.mBsi.mGlobalMeasuredEnergyStats.getNumberCustomPowerBuckets()];
            }
            return measuredEnergyStats.getAccumulatedCustomBucketCharges();
        }

        @Override // android.os.BatteryStats.Uid
        public long getBluetoothMeasuredBatteryConsumptionUC() {
            return getMeasuredBatteryConsumptionUC(5);
        }

        @Override // android.os.BatteryStats.Uid
        public long getBluetoothMeasuredBatteryConsumptionUC(int processState) {
            return getMeasuredBatteryConsumptionUC(5, processState);
        }

        @Override // android.os.BatteryStats.Uid
        public long getCpuMeasuredBatteryConsumptionUC() {
            return getMeasuredBatteryConsumptionUC(3);
        }

        @Override // android.os.BatteryStats.Uid
        public long getCpuMeasuredBatteryConsumptionUC(int processState) {
            return getMeasuredBatteryConsumptionUC(3, processState);
        }

        @Override // android.os.BatteryStats.Uid
        public long getGnssMeasuredBatteryConsumptionUC() {
            return getMeasuredBatteryConsumptionUC(6);
        }

        @Override // android.os.BatteryStats.Uid
        public long getMobileRadioMeasuredBatteryConsumptionUC() {
            return getMeasuredBatteryConsumptionUC(7);
        }

        @Override // android.os.BatteryStats.Uid
        public long getMobileRadioMeasuredBatteryConsumptionUC(int processState) {
            return getMeasuredBatteryConsumptionUC(7, processState);
        }

        @Override // android.os.BatteryStats.Uid
        public long getScreenOnMeasuredBatteryConsumptionUC() {
            return getMeasuredBatteryConsumptionUC(0);
        }

        @Override // android.os.BatteryStats.Uid
        public long getWifiMeasuredBatteryConsumptionUC() {
            return getMeasuredBatteryConsumptionUC(4);
        }

        @Override // android.os.BatteryStats.Uid
        public long getWifiMeasuredBatteryConsumptionUC(int processState) {
            return getMeasuredBatteryConsumptionUC(4, processState);
        }

        /* JADX INFO: Access modifiers changed from: private */
        public long markProcessForegroundTimeUs(long elapsedRealtimeMs, boolean doCalc) {
            long fgTimeUs = 0;
            StopwatchTimer fgTimer = this.mForegroundActivityTimer;
            if (fgTimer != null) {
                if (doCalc) {
                    fgTimeUs = fgTimer.getTimeSinceMarkLocked(elapsedRealtimeMs * 1000);
                }
                fgTimer.setMark(elapsedRealtimeMs);
            }
            long topTimeUs = 0;
            StopwatchTimer topTimer = this.mProcessStateTimer[0];
            if (topTimer != null) {
                if (doCalc) {
                    topTimeUs = topTimer.getTimeSinceMarkLocked(1000 * elapsedRealtimeMs);
                }
                topTimer.setMark(elapsedRealtimeMs);
            }
            return topTimeUs < fgTimeUs ? topTimeUs : fgTimeUs;
        }

        /* JADX INFO: Access modifiers changed from: private */
        public long markGnssTimeUs(long elapsedRealtimeMs) {
            StopwatchTimer timer;
            Sensor sensor = this.mSensorStats.get(-10000);
            if (sensor == null || (timer = sensor.mTimer) == null) {
                return 0L;
            }
            long gnssTimeUs = timer.getTimeSinceMarkLocked(1000 * elapsedRealtimeMs);
            timer.setMark(elapsedRealtimeMs);
            return gnssTimeUs;
        }

        public StopwatchTimer createAudioTurnedOnTimerLocked() {
            if (this.mAudioTurnedOnTimer == null) {
                this.mAudioTurnedOnTimer = new StopwatchTimer(this.mBsi.mClock, this, 15, this.mBsi.mAudioTurnedOnTimers, this.mBsi.mOnBatteryTimeBase);
            }
            return this.mAudioTurnedOnTimer;
        }

        public void noteAudioTurnedOnLocked(long elapsedRealtimeMs) {
            createAudioTurnedOnTimerLocked().startRunningLocked(elapsedRealtimeMs);
        }

        public void noteAudioTurnedOffLocked(long elapsedRealtimeMs) {
            StopwatchTimer stopwatchTimer = this.mAudioTurnedOnTimer;
            if (stopwatchTimer != null) {
                stopwatchTimer.stopRunningLocked(elapsedRealtimeMs);
            }
        }

        public void noteResetAudioLocked(long elapsedRealtimeMs) {
            StopwatchTimer stopwatchTimer = this.mAudioTurnedOnTimer;
            if (stopwatchTimer != null) {
                stopwatchTimer.stopAllRunningLocked(elapsedRealtimeMs);
            }
        }

        public StopwatchTimer createVideoTurnedOnTimerLocked() {
            if (this.mVideoTurnedOnTimer == null) {
                this.mVideoTurnedOnTimer = new StopwatchTimer(this.mBsi.mClock, this, 8, this.mBsi.mVideoTurnedOnTimers, this.mBsi.mOnBatteryTimeBase);
            }
            return this.mVideoTurnedOnTimer;
        }

        public void noteVideoTurnedOnLocked(long elapsedRealtimeMs) {
            createVideoTurnedOnTimerLocked().startRunningLocked(elapsedRealtimeMs);
        }

        public void noteVideoTurnedOffLocked(long elapsedRealtimeMs) {
            StopwatchTimer stopwatchTimer = this.mVideoTurnedOnTimer;
            if (stopwatchTimer != null) {
                stopwatchTimer.stopRunningLocked(elapsedRealtimeMs);
            }
        }

        public void noteResetVideoLocked(long elapsedRealtimeMs) {
            StopwatchTimer stopwatchTimer = this.mVideoTurnedOnTimer;
            if (stopwatchTimer != null) {
                stopwatchTimer.stopAllRunningLocked(elapsedRealtimeMs);
            }
        }

        public StopwatchTimer createFlashlightTurnedOnTimerLocked() {
            if (this.mFlashlightTurnedOnTimer == null) {
                this.mFlashlightTurnedOnTimer = new StopwatchTimer(this.mBsi.mClock, this, 16, this.mBsi.mFlashlightTurnedOnTimers, this.mBsi.mOnBatteryTimeBase);
            }
            return this.mFlashlightTurnedOnTimer;
        }

        public void noteFlashlightTurnedOnLocked(long elapsedRealtimeMs) {
            createFlashlightTurnedOnTimerLocked().startRunningLocked(elapsedRealtimeMs);
        }

        public void noteFlashlightTurnedOffLocked(long elapsedRealtimeMs) {
            StopwatchTimer stopwatchTimer = this.mFlashlightTurnedOnTimer;
            if (stopwatchTimer != null) {
                stopwatchTimer.stopRunningLocked(elapsedRealtimeMs);
            }
        }

        public void noteResetFlashlightLocked(long elapsedRealtimeMs) {
            StopwatchTimer stopwatchTimer = this.mFlashlightTurnedOnTimer;
            if (stopwatchTimer != null) {
                stopwatchTimer.stopAllRunningLocked(elapsedRealtimeMs);
            }
        }

        public StopwatchTimer createCameraTurnedOnTimerLocked() {
            if (this.mCameraTurnedOnTimer == null) {
                this.mCameraTurnedOnTimer = new StopwatchTimer(this.mBsi.mClock, this, 17, this.mBsi.mCameraTurnedOnTimers, this.mBsi.mOnBatteryTimeBase);
            }
            return this.mCameraTurnedOnTimer;
        }

        public void noteCameraTurnedOnLocked(long elapsedRealtimeMs) {
            createCameraTurnedOnTimerLocked().startRunningLocked(elapsedRealtimeMs);
        }

        public void noteCameraTurnedOffLocked(long elapsedRealtimeMs) {
            StopwatchTimer stopwatchTimer = this.mCameraTurnedOnTimer;
            if (stopwatchTimer != null) {
                stopwatchTimer.stopRunningLocked(elapsedRealtimeMs);
            }
        }

        public void noteResetCameraLocked(long elapsedRealtimeMs) {
            StopwatchTimer stopwatchTimer = this.mCameraTurnedOnTimer;
            if (stopwatchTimer != null) {
                stopwatchTimer.stopAllRunningLocked(elapsedRealtimeMs);
            }
        }

        public StopwatchTimer createForegroundActivityTimerLocked() {
            if (this.mForegroundActivityTimer == null) {
                this.mForegroundActivityTimer = new StopwatchTimer(this.mBsi.mClock, this, 10, null, this.mBsi.mOnBatteryTimeBase);
            }
            return this.mForegroundActivityTimer;
        }

        public StopwatchTimer createForegroundServiceTimerLocked() {
            if (this.mForegroundServiceTimer == null) {
                this.mForegroundServiceTimer = new StopwatchTimer(this.mBsi.mClock, this, 22, null, this.mBsi.mOnBatteryTimeBase);
            }
            return this.mForegroundServiceTimer;
        }

        public DualTimer createAggregatedPartialWakelockTimerLocked() {
            if (this.mAggregatedPartialWakelockTimer == null) {
                this.mAggregatedPartialWakelockTimer = new DualTimer(this.mBsi.mClock, this, 20, null, this.mBsi.mOnBatteryScreenOffTimeBase, this.mOnBatteryScreenOffBackgroundTimeBase);
            }
            return this.mAggregatedPartialWakelockTimer;
        }

        public DualTimer createBluetoothScanTimerLocked() {
            if (this.mBluetoothScanTimer == null) {
                this.mBluetoothScanTimer = new DualTimer(this.mBsi.mClock, this, 19, this.mBsi.mBluetoothScanOnTimers, this.mBsi.mOnBatteryTimeBase, this.mOnBatteryBackgroundTimeBase);
            }
            return this.mBluetoothScanTimer;
        }

        public DualTimer createBluetoothUnoptimizedScanTimerLocked() {
            if (this.mBluetoothUnoptimizedScanTimer == null) {
                this.mBluetoothUnoptimizedScanTimer = new DualTimer(this.mBsi.mClock, this, 21, null, this.mBsi.mOnBatteryTimeBase, this.mOnBatteryBackgroundTimeBase);
            }
            return this.mBluetoothUnoptimizedScanTimer;
        }

        public void noteBluetoothScanStartedLocked(long elapsedRealtimeMs, boolean isUnoptimized) {
            createBluetoothScanTimerLocked().startRunningLocked(elapsedRealtimeMs);
            if (isUnoptimized) {
                createBluetoothUnoptimizedScanTimerLocked().startRunningLocked(elapsedRealtimeMs);
            }
        }

        public void noteBluetoothScanStoppedLocked(long elapsedRealtimeMs, boolean isUnoptimized) {
            DualTimer dualTimer;
            DualTimer dualTimer2 = this.mBluetoothScanTimer;
            if (dualTimer2 != null) {
                dualTimer2.stopRunningLocked(elapsedRealtimeMs);
            }
            if (isUnoptimized && (dualTimer = this.mBluetoothUnoptimizedScanTimer) != null) {
                dualTimer.stopRunningLocked(elapsedRealtimeMs);
            }
        }

        public void noteResetBluetoothScanLocked(long elapsedRealtimeMs) {
            DualTimer dualTimer = this.mBluetoothScanTimer;
            if (dualTimer != null) {
                dualTimer.stopAllRunningLocked(elapsedRealtimeMs);
            }
            DualTimer dualTimer2 = this.mBluetoothUnoptimizedScanTimer;
            if (dualTimer2 != null) {
                dualTimer2.stopAllRunningLocked(elapsedRealtimeMs);
            }
        }

        public Counter createBluetoothScanResultCounterLocked() {
            if (this.mBluetoothScanResultCounter == null) {
                this.mBluetoothScanResultCounter = new Counter(this.mBsi.mOnBatteryTimeBase);
            }
            return this.mBluetoothScanResultCounter;
        }

        public Counter createBluetoothScanResultBgCounterLocked() {
            if (this.mBluetoothScanResultBgCounter == null) {
                this.mBluetoothScanResultBgCounter = new Counter(this.mOnBatteryBackgroundTimeBase);
            }
            return this.mBluetoothScanResultBgCounter;
        }

        public void noteBluetoothScanResultsLocked(int numNewResults) {
            createBluetoothScanResultCounterLocked().addAtomic(numNewResults);
            createBluetoothScanResultBgCounterLocked().addAtomic(numNewResults);
        }

        @Override // android.os.BatteryStats.Uid
        public void noteActivityResumedLocked(long elapsedRealtimeMs) {
            createForegroundActivityTimerLocked().startRunningLocked(elapsedRealtimeMs);
        }

        @Override // android.os.BatteryStats.Uid
        public void noteActivityPausedLocked(long elapsedRealtimeMs) {
            StopwatchTimer stopwatchTimer = this.mForegroundActivityTimer;
            if (stopwatchTimer != null) {
                stopwatchTimer.stopRunningLocked(elapsedRealtimeMs);
            }
        }

        public void noteForegroundServiceResumedLocked(long elapsedRealtimeMs) {
            createForegroundServiceTimerLocked().startRunningLocked(elapsedRealtimeMs);
        }

        public void noteForegroundServicePausedLocked(long elapsedRealtimeMs) {
            StopwatchTimer stopwatchTimer = this.mForegroundServiceTimer;
            if (stopwatchTimer != null) {
                stopwatchTimer.stopRunningLocked(elapsedRealtimeMs);
            }
        }

        public BatchTimer createVibratorOnTimerLocked() {
            if (this.mVibratorOnTimer == null) {
                this.mVibratorOnTimer = new BatchTimer(this.mBsi.mClock, this, 9, this.mBsi.mOnBatteryTimeBase);
            }
            return this.mVibratorOnTimer;
        }

        public void noteVibratorOnLocked(long durationMillis, long elapsedRealtimeMs) {
            createVibratorOnTimerLocked().addDuration(this.mBsi, durationMillis, elapsedRealtimeMs);
        }

        public void noteVibratorOffLocked(long elapsedRealtimeMs) {
            BatchTimer batchTimer = this.mVibratorOnTimer;
            if (batchTimer != null) {
                batchTimer.abortLastDuration(this.mBsi, elapsedRealtimeMs);
            }
        }

        @Override // android.os.BatteryStats.Uid
        public long getWifiRunningTime(long elapsedRealtimeUs, int which) {
            StopwatchTimer stopwatchTimer = this.mWifiRunningTimer;
            if (stopwatchTimer == null) {
                return 0L;
            }
            return stopwatchTimer.getTotalTimeLocked(elapsedRealtimeUs, which);
        }

        @Override // android.os.BatteryStats.Uid
        public long getFullWifiLockTime(long elapsedRealtimeUs, int which) {
            StopwatchTimer stopwatchTimer = this.mFullWifiLockTimer;
            if (stopwatchTimer == null) {
                return 0L;
            }
            return stopwatchTimer.getTotalTimeLocked(elapsedRealtimeUs, which);
        }

        @Override // android.os.BatteryStats.Uid
        public long getWifiScanTime(long elapsedRealtimeUs, int which) {
            DualTimer dualTimer = this.mWifiScanTimer;
            if (dualTimer == null) {
                return 0L;
            }
            return dualTimer.getTotalTimeLocked(elapsedRealtimeUs, which);
        }

        @Override // android.os.BatteryStats.Uid
        public int getWifiScanCount(int which) {
            DualTimer dualTimer = this.mWifiScanTimer;
            if (dualTimer == null) {
                return 0;
            }
            return dualTimer.getCountLocked(which);
        }

        /* JADX DEBUG: Method merged with bridge method */
        @Override // android.os.BatteryStats.Uid
        public Timer getWifiScanTimer() {
            return this.mWifiScanTimer;
        }

        @Override // android.os.BatteryStats.Uid
        public int getWifiScanBackgroundCount(int which) {
            DualTimer dualTimer = this.mWifiScanTimer;
            if (dualTimer == null || dualTimer.getSubTimer() == null) {
                return 0;
            }
            return this.mWifiScanTimer.getSubTimer().getCountLocked(which);
        }

        @Override // android.os.BatteryStats.Uid
        public long getWifiScanActualTime(long elapsedRealtimeUs) {
            DualTimer dualTimer = this.mWifiScanTimer;
            if (dualTimer == null) {
                return 0L;
            }
            long elapsedRealtimeMs = (500 + elapsedRealtimeUs) / 1000;
            return dualTimer.getTotalDurationMsLocked(elapsedRealtimeMs) * 1000;
        }

        @Override // android.os.BatteryStats.Uid
        public long getWifiScanBackgroundTime(long elapsedRealtimeUs) {
            DualTimer dualTimer = this.mWifiScanTimer;
            if (dualTimer == null || dualTimer.getSubTimer() == null) {
                return 0L;
            }
            long elapsedRealtimeMs = (500 + elapsedRealtimeUs) / 1000;
            return this.mWifiScanTimer.getSubTimer().getTotalDurationMsLocked(elapsedRealtimeMs) * 1000;
        }

        /* JADX DEBUG: Method merged with bridge method */
        @Override // android.os.BatteryStats.Uid
        public Timer getWifiScanBackgroundTimer() {
            DualTimer dualTimer = this.mWifiScanTimer;
            if (dualTimer == null) {
                return null;
            }
            return dualTimer.getSubTimer();
        }

        @Override // android.os.BatteryStats.Uid
        public long getWifiBatchedScanTime(int csphBin, long elapsedRealtimeUs, int which) {
            StopwatchTimer stopwatchTimer;
            if (csphBin < 0 || csphBin >= 5 || (stopwatchTimer = this.mWifiBatchedScanTimer[csphBin]) == null) {
                return 0L;
            }
            return stopwatchTimer.getTotalTimeLocked(elapsedRealtimeUs, which);
        }

        @Override // android.os.BatteryStats.Uid
        public int getWifiBatchedScanCount(int csphBin, int which) {
            StopwatchTimer stopwatchTimer;
            if (csphBin < 0 || csphBin >= 5 || (stopwatchTimer = this.mWifiBatchedScanTimer[csphBin]) == null) {
                return 0;
            }
            return stopwatchTimer.getCountLocked(which);
        }

        @Override // android.os.BatteryStats.Uid
        public long getWifiMulticastTime(long elapsedRealtimeUs, int which) {
            StopwatchTimer stopwatchTimer = this.mWifiMulticastTimer;
            if (stopwatchTimer == null) {
                return 0L;
            }
            return stopwatchTimer.getTotalTimeLocked(elapsedRealtimeUs, which);
        }

        /* JADX DEBUG: Method merged with bridge method */
        @Override // android.os.BatteryStats.Uid
        public Timer getAudioTurnedOnTimer() {
            return this.mAudioTurnedOnTimer;
        }

        /* JADX DEBUG: Method merged with bridge method */
        @Override // android.os.BatteryStats.Uid
        public Timer getVideoTurnedOnTimer() {
            return this.mVideoTurnedOnTimer;
        }

        /* JADX DEBUG: Method merged with bridge method */
        @Override // android.os.BatteryStats.Uid
        public Timer getFlashlightTurnedOnTimer() {
            return this.mFlashlightTurnedOnTimer;
        }

        /* JADX DEBUG: Method merged with bridge method */
        @Override // android.os.BatteryStats.Uid
        public Timer getCameraTurnedOnTimer() {
            return this.mCameraTurnedOnTimer;
        }

        /* JADX DEBUG: Method merged with bridge method */
        @Override // android.os.BatteryStats.Uid
        public Timer getForegroundActivityTimer() {
            return this.mForegroundActivityTimer;
        }

        /* JADX DEBUG: Method merged with bridge method */
        @Override // android.os.BatteryStats.Uid
        public Timer getForegroundServiceTimer() {
            return this.mForegroundServiceTimer;
        }

        /* JADX DEBUG: Method merged with bridge method */
        @Override // android.os.BatteryStats.Uid
        public Timer getBluetoothScanTimer() {
            return this.mBluetoothScanTimer;
        }

        /* JADX DEBUG: Method merged with bridge method */
        @Override // android.os.BatteryStats.Uid
        public Timer getBluetoothScanBackgroundTimer() {
            DualTimer dualTimer = this.mBluetoothScanTimer;
            if (dualTimer == null) {
                return null;
            }
            return dualTimer.getSubTimer();
        }

        /* JADX DEBUG: Method merged with bridge method */
        @Override // android.os.BatteryStats.Uid
        public Timer getBluetoothUnoptimizedScanTimer() {
            return this.mBluetoothUnoptimizedScanTimer;
        }

        /* JADX DEBUG: Method merged with bridge method */
        @Override // android.os.BatteryStats.Uid
        public Timer getBluetoothUnoptimizedScanBackgroundTimer() {
            DualTimer dualTimer = this.mBluetoothUnoptimizedScanTimer;
            if (dualTimer == null) {
                return null;
            }
            return dualTimer.getSubTimer();
        }

        /* JADX DEBUG: Method merged with bridge method */
        @Override // android.os.BatteryStats.Uid
        public Counter getBluetoothScanResultCounter() {
            return this.mBluetoothScanResultCounter;
        }

        /* JADX DEBUG: Method merged with bridge method */
        @Override // android.os.BatteryStats.Uid
        public Counter getBluetoothScanResultBgCounter() {
            return this.mBluetoothScanResultBgCounter;
        }

        void makeProcessState(int i, Parcel in) {
            if (i < 0 || i >= 7) {
                return;
            }
            BatteryStatsImpl.detachIfNotNull(this.mProcessStateTimer[i]);
            if (in == null) {
                this.mProcessStateTimer[i] = new StopwatchTimer(this.mBsi.mClock, this, 12, null, this.mBsi.mOnBatteryTimeBase);
            } else {
                this.mProcessStateTimer[i] = new StopwatchTimer(this.mBsi.mClock, this, 12, null, this.mBsi.mOnBatteryTimeBase, in);
            }
        }

        @Override // android.os.BatteryStats.Uid
        public long getProcessStateTime(int state, long elapsedRealtimeUs, int which) {
            StopwatchTimer stopwatchTimer;
            if (state < 0 || state >= 7 || (stopwatchTimer = this.mProcessStateTimer[state]) == null) {
                return 0L;
            }
            return stopwatchTimer.getTotalTimeLocked(elapsedRealtimeUs, which);
        }

        /* JADX DEBUG: Method merged with bridge method */
        @Override // android.os.BatteryStats.Uid
        public Timer getProcessStateTimer(int state) {
            if (state < 0 || state >= 7) {
                return null;
            }
            return this.mProcessStateTimer[state];
        }

        /* JADX DEBUG: Method merged with bridge method */
        @Override // android.os.BatteryStats.Uid
        public Timer getVibratorOnTimer() {
            return this.mVibratorOnTimer;
        }

        @Override // android.os.BatteryStats.Uid
        public void noteUserActivityLocked(int type) {
            if (this.mUserActivityCounters == null) {
                initUserActivityLocked();
            }
            if (type >= 0 && type < NUM_USER_ACTIVITY_TYPES) {
                this.mUserActivityCounters[type].stepAtomic();
            } else {
                Slog.w(BatteryStatsImpl.TAG, "Unknown user activity type " + type + " was specified.", new Throwable());
            }
        }

        @Override // android.os.BatteryStats.Uid
        public boolean hasUserActivity() {
            return this.mUserActivityCounters != null;
        }

        @Override // android.os.BatteryStats.Uid
        public int getUserActivityCount(int type, int which) {
            Counter[] counterArr = this.mUserActivityCounters;
            if (counterArr == null) {
                return 0;
            }
            return counterArr[type].getCountLocked(which);
        }

        void makeWifiBatchedScanBin(int i, Parcel in) {
            if (i < 0 || i >= 5) {
                return;
            }
            ArrayList<StopwatchTimer> collected = this.mBsi.mWifiBatchedScanTimers.get(i);
            if (collected == null) {
                collected = new ArrayList<>();
                this.mBsi.mWifiBatchedScanTimers.put(i, collected);
            }
            BatteryStatsImpl.detachIfNotNull(this.mWifiBatchedScanTimer[i]);
            if (in == null) {
                this.mWifiBatchedScanTimer[i] = new StopwatchTimer(this.mBsi.mClock, this, 11, collected, this.mBsi.mOnBatteryTimeBase);
                return;
            }
            this.mWifiBatchedScanTimer[i] = new StopwatchTimer(this.mBsi.mClock, this, 11, collected, this.mBsi.mOnBatteryTimeBase, in);
        }

        void initUserActivityLocked() {
            BatteryStatsImpl.detachIfNotNull(this.mUserActivityCounters);
            this.mUserActivityCounters = new Counter[NUM_USER_ACTIVITY_TYPES];
            for (int i = 0; i < NUM_USER_ACTIVITY_TYPES; i++) {
                this.mUserActivityCounters[i] = new Counter(this.mBsi.mOnBatteryTimeBase);
            }
        }

        void noteNetworkActivityLocked(int type, long deltaBytes, long deltaPackets) {
            ensureNetworkActivityLocked();
            if (type >= 0 && type < 10) {
                this.mNetworkByteActivityCounters[type].addCountLocked(deltaBytes);
                this.mNetworkPacketActivityCounters[type].addCountLocked(deltaPackets);
                return;
            }
            Slog.w(BatteryStatsImpl.TAG, "Unknown network activity type " + type + " was specified.", new Throwable());
        }

        void noteMobileRadioActiveTimeLocked(long batteryUptimeDeltaUs, long elapsedTimeMs) {
            ensureNetworkActivityLocked();
            getMobileRadioActiveTimeCounter().increment(batteryUptimeDeltaUs, elapsedTimeMs);
            this.mMobileRadioActiveCount.addCountLocked(1L);
        }

        private TimeMultiStateCounter getMobileRadioActiveTimeCounter() {
            if (this.mMobileRadioActiveTime == null) {
                long timestampMs = this.mBsi.mClock.elapsedRealtime();
                TimeMultiStateCounter timeMultiStateCounter = new TimeMultiStateCounter(this.mBsi.mOnBatteryTimeBase, 5, timestampMs);
                this.mMobileRadioActiveTime = timeMultiStateCounter;
                timeMultiStateCounter.setState(BatteryStats.mapUidProcessStateToBatteryConsumerProcessState(this.mProcessState), timestampMs);
                this.mMobileRadioActiveTime.update(0L, timestampMs);
            }
            return this.mMobileRadioActiveTime;
        }

        @Override // android.os.BatteryStats.Uid
        public boolean hasNetworkActivity() {
            return this.mNetworkByteActivityCounters != null;
        }

        @Override // android.os.BatteryStats.Uid
        public long getNetworkActivityBytes(int type, int which) {
            LongSamplingCounter[] longSamplingCounterArr = this.mNetworkByteActivityCounters;
            if (longSamplingCounterArr != null && type >= 0 && type < longSamplingCounterArr.length) {
                return longSamplingCounterArr[type].getCountLocked(which);
            }
            return 0L;
        }

        @Override // android.os.BatteryStats.Uid
        public long getNetworkActivityPackets(int type, int which) {
            LongSamplingCounter[] longSamplingCounterArr = this.mNetworkPacketActivityCounters;
            if (longSamplingCounterArr != null && type >= 0 && type < longSamplingCounterArr.length) {
                return longSamplingCounterArr[type].getCountLocked(which);
            }
            return 0L;
        }

        @Override // android.os.BatteryStats.Uid
        public long getMobileRadioActiveTime(int which) {
            return getMobileRadioActiveTimeInProcessState(0);
        }

        @Override // android.os.BatteryStats.Uid
        public long getMobileRadioActiveTimeInProcessState(int processState) {
            TimeMultiStateCounter timeMultiStateCounter = this.mMobileRadioActiveTime;
            if (timeMultiStateCounter == null) {
                return 0L;
            }
            if (processState == 0) {
                return timeMultiStateCounter.getTotalCountLocked();
            }
            return timeMultiStateCounter.getCountForProcessState(processState);
        }

        @Override // android.os.BatteryStats.Uid
        public int getMobileRadioActiveCount(int which) {
            LongSamplingCounter longSamplingCounter = this.mMobileRadioActiveCount;
            if (longSamplingCounter != null) {
                return (int) longSamplingCounter.getCountLocked(which);
            }
            return 0;
        }

        @Override // android.os.BatteryStats.Uid
        public long getUserCpuTimeUs(int which) {
            return this.mUserCpuTime.getCountLocked(which);
        }

        @Override // android.os.BatteryStats.Uid
        public long getSystemCpuTimeUs(int which) {
            return this.mSystemCpuTime.getCountLocked(which);
        }

        @Override // android.os.BatteryStats.Uid
        public long getTimeAtCpuSpeed(int cluster, int step, int which) {
            LongSamplingCounter[] cpuSpeedTimesUs;
            LongSamplingCounter c;
            LongSamplingCounter[][] longSamplingCounterArr = this.mCpuClusterSpeedTimesUs;
            if (longSamplingCounterArr != null && cluster >= 0 && cluster < longSamplingCounterArr.length && (cpuSpeedTimesUs = longSamplingCounterArr[cluster]) != null && step >= 0 && step < cpuSpeedTimesUs.length && (c = cpuSpeedTimesUs[step]) != null) {
                return c.getCountLocked(which);
            }
            return 0L;
        }

        public void noteMobileRadioApWakeupLocked() {
            if (this.mMobileRadioApWakeupCount == null) {
                this.mMobileRadioApWakeupCount = new LongSamplingCounter(this.mBsi.mOnBatteryTimeBase);
            }
            this.mMobileRadioApWakeupCount.addCountLocked(1L);
        }

        @Override // android.os.BatteryStats.Uid
        public long getMobileRadioApWakeupCount(int which) {
            LongSamplingCounter longSamplingCounter = this.mMobileRadioApWakeupCount;
            if (longSamplingCounter != null) {
                return longSamplingCounter.getCountLocked(which);
            }
            return 0L;
        }

        public void noteWifiRadioApWakeupLocked() {
            if (this.mWifiRadioApWakeupCount == null) {
                this.mWifiRadioApWakeupCount = new LongSamplingCounter(this.mBsi.mOnBatteryTimeBase);
            }
            this.mWifiRadioApWakeupCount.addCountLocked(1L);
        }

        @Override // android.os.BatteryStats.Uid
        public long getWifiRadioApWakeupCount(int which) {
            LongSamplingCounter longSamplingCounter = this.mWifiRadioApWakeupCount;
            if (longSamplingCounter != null) {
                return longSamplingCounter.getCountLocked(which);
            }
            return 0L;
        }

        @Override // android.os.BatteryStats.Uid
        public void getDeferredJobsCheckinLineLocked(StringBuilder sb, int which) {
            sb.setLength(0);
            int deferredEventCount = this.mJobsDeferredEventCount.getCountLocked(which);
            if (deferredEventCount == 0) {
                return;
            }
            int deferredCount = this.mJobsDeferredCount.getCountLocked(which);
            long totalLatency = this.mJobsFreshnessTimeMs.getCountLocked(which);
            sb.append(deferredEventCount);
            sb.append(',');
            sb.append(deferredCount);
            sb.append(',');
            sb.append(totalLatency);
            for (int i = 0; i < BatteryStats.JOB_FRESHNESS_BUCKETS.length; i++) {
                if (this.mJobsFreshnessBuckets[i] == null) {
                    sb.append(",0");
                } else {
                    sb.append(",");
                    sb.append(this.mJobsFreshnessBuckets[i].getCountLocked(which));
                }
            }
        }

        @Override // android.os.BatteryStats.Uid
        public void getDeferredJobsLineLocked(StringBuilder sb, int which) {
            sb.setLength(0);
            int deferredEventCount = this.mJobsDeferredEventCount.getCountLocked(which);
            if (deferredEventCount == 0) {
                return;
            }
            int deferredCount = this.mJobsDeferredCount.getCountLocked(which);
            long totalLatency = this.mJobsFreshnessTimeMs.getCountLocked(which);
            sb.append("times=");
            sb.append(deferredEventCount);
            sb.append(", ");
            sb.append("count=");
            sb.append(deferredCount);
            sb.append(", ");
            sb.append("totalLatencyMs=");
            sb.append(totalLatency);
            sb.append(", ");
            for (int i = 0; i < BatteryStats.JOB_FRESHNESS_BUCKETS.length; i++) {
                sb.append("<");
                sb.append(BatteryStats.JOB_FRESHNESS_BUCKETS[i]);
                sb.append("ms=");
                Counter counter = this.mJobsFreshnessBuckets[i];
                if (counter == null) {
                    sb.append(AudioSystem.LEGACY_REMOTE_SUBMIX_ADDRESS);
                } else {
                    sb.append(counter.getCountLocked(which));
                }
                sb.append(" ");
            }
        }

        void ensureNetworkActivityLocked() {
            if (this.mNetworkByteActivityCounters != null) {
                return;
            }
            this.mNetworkByteActivityCounters = new LongSamplingCounter[10];
            this.mNetworkPacketActivityCounters = new LongSamplingCounter[10];
            for (int i = 0; i < 10; i++) {
                this.mNetworkByteActivityCounters[i] = new LongSamplingCounter(this.mBsi.mOnBatteryTimeBase);
                this.mNetworkPacketActivityCounters[i] = new LongSamplingCounter(this.mBsi.mOnBatteryTimeBase);
            }
            this.mMobileRadioActiveCount = new LongSamplingCounter(this.mBsi.mOnBatteryTimeBase);
        }

        public boolean reset(long uptimeUs, long realtimeUs, int resetReason) {
            StopwatchTimer stopwatchTimer;
            StopwatchTimer stopwatchTimer2;
            DualTimer dualTimer;
            StopwatchTimer stopwatchTimer3;
            StopwatchTimer stopwatchTimer4;
            boolean active = false;
            this.mOnBatteryBackgroundTimeBase.init(uptimeUs, realtimeUs);
            this.mOnBatteryScreenOffBackgroundTimeBase.init(uptimeUs, realtimeUs);
            if (this.mWifiRunningTimer != null) {
                boolean active2 = false | (!stopwatchTimer.reset(false, realtimeUs));
                active = active2 | this.mWifiRunning;
            }
            if (this.mFullWifiLockTimer != null) {
                active = active | (!stopwatchTimer2.reset(false, realtimeUs)) | this.mFullWifiLockOut;
            }
            if (this.mWifiScanTimer != null) {
                active = active | (!dualTimer.reset(false, realtimeUs)) | this.mWifiScanStarted;
            }
            if (this.mWifiBatchedScanTimer != null) {
                for (int i = 0; i < 5; i++) {
                    if (this.mWifiBatchedScanTimer[i] != null) {
                        active |= !stopwatchTimer4.reset(false, realtimeUs);
                    }
                }
                int i2 = this.mWifiBatchedScanBinStarted;
                active |= i2 != -1;
            }
            if (this.mWifiMulticastTimer != null) {
                active = active | (!stopwatchTimer3.reset(false, realtimeUs)) | (this.mWifiMulticastWakelockCount > 0);
            }
            boolean active3 = active | (!BatteryStatsImpl.resetIfNotNull(this.mAudioTurnedOnTimer, false, realtimeUs)) | (!BatteryStatsImpl.resetIfNotNull(this.mVideoTurnedOnTimer, false, realtimeUs)) | (!BatteryStatsImpl.resetIfNotNull(this.mFlashlightTurnedOnTimer, false, realtimeUs)) | (!BatteryStatsImpl.resetIfNotNull(this.mCameraTurnedOnTimer, false, realtimeUs)) | (!BatteryStatsImpl.resetIfNotNull(this.mForegroundActivityTimer, false, realtimeUs)) | (!BatteryStatsImpl.resetIfNotNull(this.mForegroundServiceTimer, false, realtimeUs)) | (!BatteryStatsImpl.resetIfNotNull(this.mAggregatedPartialWakelockTimer, false, realtimeUs)) | (!BatteryStatsImpl.resetIfNotNull(this.mBluetoothScanTimer, false, realtimeUs)) | (!BatteryStatsImpl.resetIfNotNull(this.mBluetoothUnoptimizedScanTimer, false, realtimeUs));
            BatteryStatsImpl.resetIfNotNull(this.mBluetoothScanResultCounter, false, realtimeUs);
            BatteryStatsImpl.resetIfNotNull(this.mBluetoothScanResultBgCounter, false, realtimeUs);
            if (this.mProcessStateTimer != null) {
                for (int i3 = 0; i3 < 7; i3++) {
                    active3 |= !BatteryStatsImpl.resetIfNotNull(this.mProcessStateTimer[i3], false, realtimeUs);
                }
                int i4 = this.mProcessState;
                active3 |= i4 != 7;
            }
            BatchTimer batchTimer = this.mVibratorOnTimer;
            if (batchTimer != null) {
                if (batchTimer.reset(false, realtimeUs)) {
                    this.mVibratorOnTimer.detach();
                    this.mVibratorOnTimer = null;
                } else {
                    active3 = true;
                }
            }
            BatteryStatsImpl.resetIfNotNull((TimeBaseObs[]) this.mUserActivityCounters, false, realtimeUs);
            BatteryStatsImpl.resetIfNotNull((TimeBaseObs[]) this.mNetworkByteActivityCounters, false, realtimeUs);
            BatteryStatsImpl.resetIfNotNull((TimeBaseObs[]) this.mNetworkPacketActivityCounters, false, realtimeUs);
            BatteryStatsImpl.resetIfNotNull(this.mMobileRadioActiveTime, false, realtimeUs);
            BatteryStatsImpl.resetIfNotNull(this.mMobileRadioActiveCount, false, realtimeUs);
            BatteryStatsImpl.resetIfNotNull(this.mWifiControllerActivity, false, realtimeUs);
            BatteryStatsImpl.resetIfNotNull(this.mBluetoothControllerActivity, false, realtimeUs);
            BatteryStatsImpl.resetIfNotNull(this.mModemControllerActivity, false, realtimeUs);
            if (resetReason == 4) {
                this.mUidMeasuredEnergyStats = null;
            } else {
                MeasuredEnergyStats.resetIfNotNull(this.mUidMeasuredEnergyStats);
            }
            BatteryStatsImpl.resetIfNotNull(this.mUserCpuTime, false, realtimeUs);
            BatteryStatsImpl.resetIfNotNull(this.mSystemCpuTime, false, realtimeUs);
            BatteryStatsImpl.resetIfNotNull((TimeBaseObs[][]) this.mCpuClusterSpeedTimesUs, false, realtimeUs);
            BatteryStatsImpl.resetIfNotNull(this.mCpuFreqTimeMs, false, realtimeUs);
            BatteryStatsImpl.resetIfNotNull(this.mScreenOffCpuFreqTimeMs, false, realtimeUs);
            BatteryStatsImpl.resetIfNotNull(this.mCpuActiveTimeMs, false, realtimeUs);
            BatteryStatsImpl.resetIfNotNull(this.mCpuClusterTimesMs, false, realtimeUs);
            BatteryStatsImpl.resetIfNotNull(this.mProcStateTimeMs, false, realtimeUs);
            BatteryStatsImpl.resetIfNotNull(this.mProcStateScreenOffTimeMs, false, realtimeUs);
            BatteryStatsImpl.resetIfNotNull(this.mMobileRadioApWakeupCount, false, realtimeUs);
            BatteryStatsImpl.resetIfNotNull(this.mWifiRadioApWakeupCount, false, realtimeUs);
            ArrayMap<String, Wakelock> wakeStats = this.mWakelockStats.getMap();
            for (int iw = wakeStats.size() - 1; iw >= 0; iw--) {
                Wakelock wl = wakeStats.valueAt(iw);
                if (wl.reset(realtimeUs)) {
                    wakeStats.removeAt(iw);
                } else {
                    active3 = true;
                }
            }
            long realtimeMs = realtimeUs / 1000;
            this.mWakelockStats.cleanup(realtimeMs);
            ArrayMap<String, DualTimer> syncStats = this.mSyncStats.getMap();
            for (int is = syncStats.size() - 1; is >= 0; is--) {
                DualTimer timer = syncStats.valueAt(is);
                if (timer.reset(false, realtimeUs)) {
                    syncStats.removeAt(is);
                    timer.detach();
                } else {
                    active3 = true;
                }
            }
            this.mSyncStats.cleanup(realtimeMs);
            ArrayMap<String, DualTimer> jobStats = this.mJobStats.getMap();
            for (int ij = jobStats.size() - 1; ij >= 0; ij--) {
                DualTimer timer2 = jobStats.valueAt(ij);
                if (timer2.reset(false, realtimeUs)) {
                    jobStats.removeAt(ij);
                    timer2.detach();
                } else {
                    active3 = true;
                }
            }
            this.mJobStats.cleanup(realtimeMs);
            this.mJobCompletions.clear();
            BatteryStatsImpl.resetIfNotNull(this.mJobsDeferredEventCount, false, realtimeUs);
            BatteryStatsImpl.resetIfNotNull(this.mJobsDeferredCount, false, realtimeUs);
            BatteryStatsImpl.resetIfNotNull(this.mJobsFreshnessTimeMs, false, realtimeUs);
            BatteryStatsImpl.resetIfNotNull((TimeBaseObs[]) this.mJobsFreshnessBuckets, false, realtimeUs);
            for (int ise = this.mSensorStats.size() - 1; ise >= 0; ise--) {
                Sensor s = this.mSensorStats.valueAt(ise);
                if (s.reset(realtimeUs)) {
                    this.mSensorStats.removeAt(ise);
                } else {
                    active3 = true;
                }
            }
            for (int ip = this.mProcessStats.size() - 1; ip >= 0; ip--) {
                Proc proc = this.mProcessStats.valueAt(ip);
                proc.detach();
            }
            this.mProcessStats.clear();
            for (int i5 = this.mPids.size() - 1; i5 >= 0; i5--) {
                BatteryStats.Uid.Pid pid = this.mPids.valueAt(i5);
                if (pid.mWakeNesting > 0) {
                    active3 = true;
                } else {
                    this.mPids.removeAt(i5);
                }
            }
            for (int i6 = this.mPackageStats.size() - 1; i6 >= 0; i6--) {
                Pkg p = this.mPackageStats.valueAt(i6);
                p.detach();
            }
            this.mPackageStats.clear();
            this.mBinderCallCount = 0L;
            this.mBinderCallStats.clear();
            this.mProportionalSystemServiceUsage = 0.0d;
            this.mLastStepSystemTimeMs = 0L;
            this.mLastStepUserTimeMs = 0L;
            this.mCurStepSystemTimeMs = 0L;
            this.mCurStepUserTimeMs = 0L;
            return !active3;
        }

        void detachFromTimeBase() {
            BatteryStatsImpl.detachIfNotNull(this.mWifiRunningTimer);
            BatteryStatsImpl.detachIfNotNull(this.mFullWifiLockTimer);
            BatteryStatsImpl.detachIfNotNull(this.mWifiScanTimer);
            BatteryStatsImpl.detachIfNotNull(this.mWifiBatchedScanTimer);
            BatteryStatsImpl.detachIfNotNull(this.mWifiMulticastTimer);
            BatteryStatsImpl.detachIfNotNull(this.mAudioTurnedOnTimer);
            BatteryStatsImpl.detachIfNotNull(this.mVideoTurnedOnTimer);
            BatteryStatsImpl.detachIfNotNull(this.mFlashlightTurnedOnTimer);
            BatteryStatsImpl.detachIfNotNull(this.mCameraTurnedOnTimer);
            BatteryStatsImpl.detachIfNotNull(this.mForegroundActivityTimer);
            BatteryStatsImpl.detachIfNotNull(this.mForegroundServiceTimer);
            BatteryStatsImpl.detachIfNotNull(this.mAggregatedPartialWakelockTimer);
            BatteryStatsImpl.detachIfNotNull(this.mBluetoothScanTimer);
            BatteryStatsImpl.detachIfNotNull(this.mBluetoothUnoptimizedScanTimer);
            BatteryStatsImpl.detachIfNotNull(this.mBluetoothScanResultCounter);
            BatteryStatsImpl.detachIfNotNull(this.mBluetoothScanResultBgCounter);
            BatteryStatsImpl.detachIfNotNull(this.mProcessStateTimer);
            BatteryStatsImpl.detachIfNotNull(this.mVibratorOnTimer);
            BatteryStatsImpl.detachIfNotNull(this.mUserActivityCounters);
            BatteryStatsImpl.detachIfNotNull(this.mNetworkByteActivityCounters);
            BatteryStatsImpl.detachIfNotNull(this.mNetworkPacketActivityCounters);
            BatteryStatsImpl.detachIfNotNull(this.mMobileRadioActiveTime);
            BatteryStatsImpl.detachIfNotNull(this.mMobileRadioActiveCount);
            BatteryStatsImpl.detachIfNotNull(this.mMobileRadioApWakeupCount);
            BatteryStatsImpl.detachIfNotNull(this.mWifiRadioApWakeupCount);
            BatteryStatsImpl.detachIfNotNull(this.mWifiControllerActivity);
            BatteryStatsImpl.detachIfNotNull(this.mBluetoothControllerActivity);
            BatteryStatsImpl.detachIfNotNull(this.mModemControllerActivity);
            this.mPids.clear();
            BatteryStatsImpl.detachIfNotNull(this.mUserCpuTime);
            BatteryStatsImpl.detachIfNotNull(this.mSystemCpuTime);
            BatteryStatsImpl.detachIfNotNull(this.mCpuClusterSpeedTimesUs);
            BatteryStatsImpl.detachIfNotNull(this.mCpuActiveTimeMs);
            BatteryStatsImpl.detachIfNotNull(this.mCpuFreqTimeMs);
            BatteryStatsImpl.detachIfNotNull(this.mScreenOffCpuFreqTimeMs);
            BatteryStatsImpl.detachIfNotNull(this.mCpuClusterTimesMs);
            BatteryStatsImpl.detachIfNotNull(this.mProcStateTimeMs);
            BatteryStatsImpl.detachIfNotNull(this.mProcStateScreenOffTimeMs);
            ArrayMap<String, Wakelock> wakeStats = this.mWakelockStats.getMap();
            for (int iw = wakeStats.size() - 1; iw >= 0; iw--) {
                Wakelock wl = wakeStats.valueAt(iw);
                wl.detachFromTimeBase();
            }
            ArrayMap<String, DualTimer> syncStats = this.mSyncStats.getMap();
            for (int is = syncStats.size() - 1; is >= 0; is--) {
                DualTimer timer = syncStats.valueAt(is);
                BatteryStatsImpl.detachIfNotNull(timer);
            }
            ArrayMap<String, DualTimer> jobStats = this.mJobStats.getMap();
            for (int ij = jobStats.size() - 1; ij >= 0; ij--) {
                DualTimer timer2 = jobStats.valueAt(ij);
                BatteryStatsImpl.detachIfNotNull(timer2);
            }
            BatteryStatsImpl.detachIfNotNull(this.mJobsDeferredEventCount);
            BatteryStatsImpl.detachIfNotNull(this.mJobsDeferredCount);
            BatteryStatsImpl.detachIfNotNull(this.mJobsFreshnessTimeMs);
            BatteryStatsImpl.detachIfNotNull(this.mJobsFreshnessBuckets);
            for (int ise = this.mSensorStats.size() - 1; ise >= 0; ise--) {
                Sensor s = this.mSensorStats.valueAt(ise);
                s.detachFromTimeBase();
            }
            for (int ip = this.mProcessStats.size() - 1; ip >= 0; ip--) {
                Proc proc = this.mProcessStats.valueAt(ip);
                proc.detach();
            }
            this.mProcessStats.clear();
            for (int i = this.mPackageStats.size() - 1; i >= 0; i--) {
                Pkg p = this.mPackageStats.valueAt(i);
                p.detach();
            }
            this.mPackageStats.clear();
        }

        void writeJobCompletionsToParcelLocked(Parcel out) {
            int NJC = this.mJobCompletions.size();
            out.writeInt(NJC);
            for (int ijc = 0; ijc < NJC; ijc++) {
                out.writeString(this.mJobCompletions.keyAt(ijc));
                SparseIntArray types = this.mJobCompletions.valueAt(ijc);
                int NT = types.size();
                out.writeInt(NT);
                for (int it = 0; it < NT; it++) {
                    out.writeInt(types.keyAt(it));
                    out.writeInt(types.valueAt(it));
                }
            }
        }

        void writeToParcelLocked(Parcel out, long uptimeUs, long elapsedRealtimeUs) {
            this.mOnBatteryBackgroundTimeBase.writeToParcel(out, uptimeUs, elapsedRealtimeUs);
            this.mOnBatteryScreenOffBackgroundTimeBase.writeToParcel(out, uptimeUs, elapsedRealtimeUs);
            ArrayMap<String, Wakelock> wakeStats = this.mWakelockStats.getMap();
            int NW = wakeStats.size();
            out.writeInt(NW);
            for (int iw = 0; iw < NW; iw++) {
                out.writeString(wakeStats.keyAt(iw));
                Wakelock wakelock = wakeStats.valueAt(iw);
                wakelock.writeToParcelLocked(out, elapsedRealtimeUs);
            }
            ArrayMap<String, DualTimer> syncStats = this.mSyncStats.getMap();
            int NS = syncStats.size();
            out.writeInt(NS);
            for (int is = 0; is < NS; is++) {
                out.writeString(syncStats.keyAt(is));
                DualTimer timer = syncStats.valueAt(is);
                Timer.writeTimerToParcel(out, timer, elapsedRealtimeUs);
            }
            ArrayMap<String, DualTimer> jobStats = this.mJobStats.getMap();
            int NJ = jobStats.size();
            out.writeInt(NJ);
            for (int ij = 0; ij < NJ; ij++) {
                out.writeString(jobStats.keyAt(ij));
                DualTimer timer2 = jobStats.valueAt(ij);
                Timer.writeTimerToParcel(out, timer2, elapsedRealtimeUs);
            }
            writeJobCompletionsToParcelLocked(out);
            this.mJobsDeferredEventCount.writeToParcel(out);
            this.mJobsDeferredCount.writeToParcel(out);
            this.mJobsFreshnessTimeMs.writeToParcel(out);
            for (int i = 0; i < BatteryStats.JOB_FRESHNESS_BUCKETS.length; i++) {
                Counter.writeCounterToParcel(out, this.mJobsFreshnessBuckets[i]);
            }
            int NSE = this.mSensorStats.size();
            out.writeInt(NSE);
            for (int ise = 0; ise < NSE; ise++) {
                out.writeInt(this.mSensorStats.keyAt(ise));
                Sensor sensor = this.mSensorStats.valueAt(ise);
                sensor.writeToParcelLocked(out, elapsedRealtimeUs);
            }
            int NP = this.mProcessStats.size();
            out.writeInt(NP);
            for (int ip = 0; ip < NP; ip++) {
                out.writeString(this.mProcessStats.keyAt(ip));
                Proc proc = this.mProcessStats.valueAt(ip);
                proc.writeToParcelLocked(out);
            }
            out.writeInt(this.mPackageStats.size());
            for (Map.Entry<String, Pkg> pkgEntry : this.mPackageStats.entrySet()) {
                out.writeString(pkgEntry.getKey());
                Pkg pkg = pkgEntry.getValue();
                pkg.writeToParcelLocked(out);
            }
            if (this.mWifiRunningTimer != null) {
                out.writeInt(1);
                this.mWifiRunningTimer.writeToParcel(out, elapsedRealtimeUs);
            } else {
                out.writeInt(0);
            }
            if (this.mFullWifiLockTimer != null) {
                out.writeInt(1);
                this.mFullWifiLockTimer.writeToParcel(out, elapsedRealtimeUs);
            } else {
                out.writeInt(0);
            }
            if (this.mWifiScanTimer != null) {
                out.writeInt(1);
                this.mWifiScanTimer.writeToParcel(out, elapsedRealtimeUs);
            } else {
                out.writeInt(0);
            }
            for (int i2 = 0; i2 < 5; i2++) {
                if (this.mWifiBatchedScanTimer[i2] != null) {
                    out.writeInt(1);
                    this.mWifiBatchedScanTimer[i2].writeToParcel(out, elapsedRealtimeUs);
                } else {
                    out.writeInt(0);
                }
            }
            if (this.mWifiMulticastTimer != null) {
                out.writeInt(1);
                this.mWifiMulticastTimer.writeToParcel(out, elapsedRealtimeUs);
            } else {
                out.writeInt(0);
            }
            if (this.mAudioTurnedOnTimer != null) {
                out.writeInt(1);
                this.mAudioTurnedOnTimer.writeToParcel(out, elapsedRealtimeUs);
            } else {
                out.writeInt(0);
            }
            if (this.mVideoTurnedOnTimer != null) {
                out.writeInt(1);
                this.mVideoTurnedOnTimer.writeToParcel(out, elapsedRealtimeUs);
            } else {
                out.writeInt(0);
            }
            if (this.mFlashlightTurnedOnTimer != null) {
                out.writeInt(1);
                this.mFlashlightTurnedOnTimer.writeToParcel(out, elapsedRealtimeUs);
            } else {
                out.writeInt(0);
            }
            if (this.mCameraTurnedOnTimer != null) {
                out.writeInt(1);
                this.mCameraTurnedOnTimer.writeToParcel(out, elapsedRealtimeUs);
            } else {
                out.writeInt(0);
            }
            if (this.mForegroundActivityTimer != null) {
                out.writeInt(1);
                this.mForegroundActivityTimer.writeToParcel(out, elapsedRealtimeUs);
            } else {
                out.writeInt(0);
            }
            if (this.mForegroundServiceTimer != null) {
                out.writeInt(1);
                this.mForegroundServiceTimer.writeToParcel(out, elapsedRealtimeUs);
            } else {
                out.writeInt(0);
            }
            if (this.mAggregatedPartialWakelockTimer != null) {
                out.writeInt(1);
                this.mAggregatedPartialWakelockTimer.writeToParcel(out, elapsedRealtimeUs);
            } else {
                out.writeInt(0);
            }
            if (this.mBluetoothScanTimer != null) {
                out.writeInt(1);
                this.mBluetoothScanTimer.writeToParcel(out, elapsedRealtimeUs);
            } else {
                out.writeInt(0);
            }
            if (this.mBluetoothUnoptimizedScanTimer != null) {
                out.writeInt(1);
                this.mBluetoothUnoptimizedScanTimer.writeToParcel(out, elapsedRealtimeUs);
            } else {
                out.writeInt(0);
            }
            if (this.mBluetoothScanResultCounter != null) {
                out.writeInt(1);
                this.mBluetoothScanResultCounter.writeToParcel(out);
            } else {
                out.writeInt(0);
            }
            if (this.mBluetoothScanResultBgCounter != null) {
                out.writeInt(1);
                this.mBluetoothScanResultBgCounter.writeToParcel(out);
            } else {
                out.writeInt(0);
            }
            for (int i3 = 0; i3 < 7; i3++) {
                if (this.mProcessStateTimer[i3] != null) {
                    out.writeInt(1);
                    this.mProcessStateTimer[i3].writeToParcel(out, elapsedRealtimeUs);
                } else {
                    out.writeInt(0);
                }
            }
            if (this.mVibratorOnTimer != null) {
                out.writeInt(1);
                this.mVibratorOnTimer.writeToParcel(out, elapsedRealtimeUs);
            } else {
                out.writeInt(0);
            }
            if (this.mUserActivityCounters != null) {
                out.writeInt(1);
                for (int i4 = 0; i4 < NUM_USER_ACTIVITY_TYPES; i4++) {
                    this.mUserActivityCounters[i4].writeToParcel(out);
                }
            } else {
                out.writeInt(0);
            }
            if (this.mNetworkByteActivityCounters != null) {
                out.writeInt(1);
                for (int i5 = 0; i5 < 10; i5++) {
                    this.mNetworkByteActivityCounters[i5].writeToParcel(out);
                    this.mNetworkPacketActivityCounters[i5].writeToParcel(out);
                }
                if (this.mMobileRadioActiveTime != null) {
                    out.writeBoolean(true);
                    this.mMobileRadioActiveTime.writeToParcel(out);
                } else {
                    out.writeBoolean(false);
                }
                this.mMobileRadioActiveCount.writeToParcel(out);
            } else {
                out.writeInt(0);
            }
            if (this.mWifiControllerActivity != null) {
                out.writeInt(1);
                this.mWifiControllerActivity.writeToParcel(out, 0);
            } else {
                out.writeInt(0);
            }
            if (this.mBluetoothControllerActivity != null) {
                out.writeInt(1);
                this.mBluetoothControllerActivity.writeToParcel(out, 0);
            } else {
                out.writeInt(0);
            }
            if (this.mModemControllerActivity != null) {
                out.writeInt(1);
                this.mModemControllerActivity.writeToParcel(out, 0);
            } else {
                out.writeInt(0);
            }
            if (this.mUidMeasuredEnergyStats != null) {
                out.writeInt(1);
                this.mUidMeasuredEnergyStats.writeToParcel(out);
            } else {
                out.writeInt(0);
            }
            this.mUserCpuTime.writeToParcel(out);
            this.mSystemCpuTime.writeToParcel(out);
            this.mBsi.writeCpuSpeedCountersToParcel(out, this.mCpuClusterSpeedTimesUs);
            LongSamplingCounterArray.writeToParcel(out, this.mCpuFreqTimeMs);
            LongSamplingCounterArray.writeToParcel(out, this.mScreenOffCpuFreqTimeMs);
            TimeMultiStateCounter timeMultiStateCounter = this.mCpuActiveTimeMs;
            if (timeMultiStateCounter != null) {
                out.writeInt(timeMultiStateCounter.getStateCount());
                this.mCpuActiveTimeMs.writeToParcel(out);
            } else {
                out.writeInt(0);
            }
            this.mCpuClusterTimesMs.writeToParcel(out);
            TimeInFreqMultiStateCounter timeInFreqMultiStateCounter = this.mProcStateTimeMs;
            if (timeInFreqMultiStateCounter != null) {
                out.writeInt(timeInFreqMultiStateCounter.getStateCount());
                this.mProcStateTimeMs.writeToParcel(out);
            } else {
                out.writeInt(0);
            }
            TimeInFreqMultiStateCounter timeInFreqMultiStateCounter2 = this.mProcStateScreenOffTimeMs;
            if (timeInFreqMultiStateCounter2 != null) {
                out.writeInt(timeInFreqMultiStateCounter2.getStateCount());
                this.mProcStateScreenOffTimeMs.writeToParcel(out);
            } else {
                out.writeInt(0);
            }
            if (this.mMobileRadioApWakeupCount != null) {
                out.writeInt(1);
                this.mMobileRadioApWakeupCount.writeToParcel(out);
            } else {
                out.writeInt(0);
            }
            if (this.mWifiRadioApWakeupCount != null) {
                out.writeInt(1);
                this.mWifiRadioApWakeupCount.writeToParcel(out);
            } else {
                out.writeInt(0);
            }
            out.writeDouble(this.mProportionalSystemServiceUsage);
        }

        void readJobCompletionsFromParcelLocked(Parcel in) {
            int numJobCompletions = in.readInt();
            this.mJobCompletions.clear();
            for (int j = 0; j < numJobCompletions; j++) {
                String jobName = in.readString();
                int numTypes = in.readInt();
                if (numTypes > 0) {
                    SparseIntArray types = new SparseIntArray();
                    for (int k = 0; k < numTypes; k++) {
                        int type = in.readInt();
                        int count = in.readInt();
                        types.put(type, count);
                    }
                    this.mJobCompletions.put(jobName, types);
                }
            }
        }

        /* JADX DEBUG: Multi-variable search result rejected for r13v1, resolved type: com.android.internal.os.BatteryStatsImpl$DualTimer */
        /* JADX DEBUG: Multi-variable search result rejected for r1v55, resolved type: com.android.internal.os.BatteryStatsImpl$StopwatchTimer[] */
        /* JADX DEBUG: Multi-variable search result rejected for r2v16, resolved type: com.android.internal.os.BatteryStatsImpl$StopwatchTimer[] */
        /* JADX WARN: Multi-variable type inference failed */
        /* JADX WARN: Type inference failed for: r13v2, types: [com.android.internal.os.BatteryStatsImpl$DualTimer, com.android.internal.os.BatteryStatsImpl$StopwatchTimer, com.android.internal.os.BatteryStatsImpl$BatchTimer, com.android.internal.os.BatteryStatsImpl$ControllerActivityCounterImpl, com.android.internal.os.BatteryStatsImpl$Counter, com.android.internal.os.BatteryStatsImpl$Counter[], com.android.internal.os.BatteryStatsImpl$LongSamplingCounterArray-IA, com.android.internal.os.BatteryStatsImpl$LongSamplingCounter[], com.android.internal.os.BatteryStatsImpl$TimeInFreqMultiStateCounter, com.android.internal.os.BatteryStatsImpl$LongSamplingCounter] */
        /* JADX WARN: Type inference failed for: r13v3 */
        /* JADX WARN: Type inference failed for: r13v4 */
        void readFromParcelLocked(TimeBase timeBase, TimeBase screenOffTimeBase, Parcel in) {
            int numProcs;
            boolean z;
            Object obj;
            ?? r13;
            int j;
            int numJobs;
            int numWakelocks;
            int j2;
            long timestampMs = this.mBsi.mClock.elapsedRealtime();
            this.mOnBatteryBackgroundTimeBase.readFromParcel(in);
            this.mOnBatteryScreenOffBackgroundTimeBase.readFromParcel(in);
            int numWakelocks2 = in.readInt();
            this.mWakelockStats.clear();
            for (int j3 = 0; j3 < numWakelocks2; j3++) {
                String wakelockName = in.readString();
                Wakelock wakelock = new Wakelock(this.mBsi, this);
                wakelock.readFromParcelLocked(timeBase, screenOffTimeBase, this.mOnBatteryScreenOffBackgroundTimeBase, in);
                this.mWakelockStats.add(wakelockName, wakelock);
            }
            int numSyncs = in.readInt();
            this.mSyncStats.clear();
            int j4 = 0;
            while (j4 < numSyncs) {
                String syncName = in.readString();
                if (in.readInt() != 0) {
                    j2 = j4;
                    this.mSyncStats.add(syncName, new DualTimer(this.mBsi.mClock, this, 13, null, this.mBsi.mOnBatteryTimeBase, this.mOnBatteryBackgroundTimeBase, in));
                } else {
                    j2 = j4;
                }
                j4 = j2 + 1;
            }
            int numJobs2 = in.readInt();
            this.mJobStats.clear();
            int j5 = 0;
            while (j5 < numJobs2) {
                String jobName = in.readString();
                if (in.readInt() != 0) {
                    numWakelocks = numWakelocks2;
                    j = j5;
                    numJobs = numJobs2;
                    this.mJobStats.add(jobName, new DualTimer(this.mBsi.mClock, this, 14, null, this.mBsi.mOnBatteryTimeBase, this.mOnBatteryBackgroundTimeBase, in));
                } else {
                    j = j5;
                    numJobs = numJobs2;
                    numWakelocks = numWakelocks2;
                }
                j5 = j + 1;
                numJobs2 = numJobs;
                numWakelocks2 = numWakelocks;
            }
            readJobCompletionsFromParcelLocked(in);
            this.mJobsDeferredEventCount = new Counter(this.mBsi.mOnBatteryTimeBase, in);
            this.mJobsDeferredCount = new Counter(this.mBsi.mOnBatteryTimeBase, in);
            this.mJobsFreshnessTimeMs = new LongSamplingCounter(this.mBsi.mOnBatteryTimeBase, in);
            for (int i = 0; i < BatteryStats.JOB_FRESHNESS_BUCKETS.length; i++) {
                this.mJobsFreshnessBuckets[i] = Counter.readCounterFromParcel(this.mBsi.mOnBatteryTimeBase, in);
            }
            int numSensors = in.readInt();
            this.mSensorStats.clear();
            for (int k = 0; k < numSensors; k++) {
                int sensorNumber = in.readInt();
                Sensor sensor = new Sensor(this.mBsi, this, sensorNumber);
                sensor.readFromParcelLocked(this.mBsi.mOnBatteryTimeBase, this.mOnBatteryBackgroundTimeBase, in);
                this.mSensorStats.put(sensorNumber, sensor);
            }
            int numProcs2 = in.readInt();
            this.mProcessStats.clear();
            for (int k2 = 0; k2 < numProcs2; k2++) {
                String processName = in.readString();
                Proc proc = new Proc(this.mBsi, processName);
                proc.readFromParcelLocked(in);
                this.mProcessStats.put(processName, proc);
            }
            int numPkgs = in.readInt();
            this.mPackageStats.clear();
            for (int l = 0; l < numPkgs; l++) {
                String packageName = in.readString();
                Pkg pkg = new Pkg(this.mBsi);
                pkg.readFromParcelLocked(in);
                this.mPackageStats.put(packageName, pkg);
            }
            this.mWifiRunning = false;
            if (in.readInt() != 0) {
                numProcs = numProcs2;
                this.mWifiRunningTimer = new StopwatchTimer(this.mBsi.mClock, this, 4, this.mBsi.mWifiRunningTimers, this.mBsi.mOnBatteryTimeBase, in);
            } else {
                numProcs = numProcs2;
                this.mWifiRunningTimer = null;
            }
            this.mFullWifiLockOut = false;
            if (in.readInt() != 0) {
                z = false;
                this.mFullWifiLockTimer = new StopwatchTimer(this.mBsi.mClock, this, 5, this.mBsi.mFullWifiLockTimers, this.mBsi.mOnBatteryTimeBase, in);
                obj = null;
            } else {
                z = false;
                obj = null;
                this.mFullWifiLockTimer = null;
            }
            this.mWifiScanStarted = z;
            if (in.readInt() != 0) {
                r13 = obj;
                this.mWifiScanTimer = new DualTimer(this.mBsi.mClock, this, 6, this.mBsi.mWifiScanTimers, this.mBsi.mOnBatteryTimeBase, this.mOnBatteryBackgroundTimeBase, in);
            } else {
                DualTimer dualTimer = obj;
                this.mWifiScanTimer = dualTimer;
                r13 = dualTimer;
            }
            this.mWifiBatchedScanBinStarted = -1;
            for (int i2 = 0; i2 < 5; i2++) {
                if (in.readInt() != 0) {
                    makeWifiBatchedScanBin(i2, in);
                } else {
                    this.mWifiBatchedScanTimer[i2] = r13;
                }
            }
            this.mWifiMulticastWakelockCount = 0;
            if (in.readInt() != 0) {
                this.mWifiMulticastTimer = new StopwatchTimer(this.mBsi.mClock, this, 7, this.mBsi.mWifiMulticastTimers, this.mBsi.mOnBatteryTimeBase, in);
            } else {
                this.mWifiMulticastTimer = r13;
            }
            if (in.readInt() != 0) {
                this.mAudioTurnedOnTimer = new StopwatchTimer(this.mBsi.mClock, this, 15, this.mBsi.mAudioTurnedOnTimers, this.mBsi.mOnBatteryTimeBase, in);
            } else {
                this.mAudioTurnedOnTimer = r13;
            }
            if (in.readInt() != 0) {
                this.mVideoTurnedOnTimer = new StopwatchTimer(this.mBsi.mClock, this, 8, this.mBsi.mVideoTurnedOnTimers, this.mBsi.mOnBatteryTimeBase, in);
            } else {
                this.mVideoTurnedOnTimer = r13;
            }
            if (in.readInt() != 0) {
                this.mFlashlightTurnedOnTimer = new StopwatchTimer(this.mBsi.mClock, this, 16, this.mBsi.mFlashlightTurnedOnTimers, this.mBsi.mOnBatteryTimeBase, in);
            } else {
                this.mFlashlightTurnedOnTimer = r13;
            }
            if (in.readInt() != 0) {
                this.mCameraTurnedOnTimer = new StopwatchTimer(this.mBsi.mClock, this, 17, this.mBsi.mCameraTurnedOnTimers, this.mBsi.mOnBatteryTimeBase, in);
            } else {
                this.mCameraTurnedOnTimer = r13;
            }
            if (in.readInt() != 0) {
                this.mForegroundActivityTimer = new StopwatchTimer(this.mBsi.mClock, this, 10, null, this.mBsi.mOnBatteryTimeBase, in);
            } else {
                this.mForegroundActivityTimer = r13;
            }
            if (in.readInt() != 0) {
                this.mForegroundServiceTimer = new StopwatchTimer(this.mBsi.mClock, this, 22, null, this.mBsi.mOnBatteryTimeBase, in);
            } else {
                this.mForegroundServiceTimer = r13;
            }
            if (in.readInt() != 0) {
                this.mAggregatedPartialWakelockTimer = new DualTimer(this.mBsi.mClock, this, 20, null, this.mBsi.mOnBatteryScreenOffTimeBase, this.mOnBatteryScreenOffBackgroundTimeBase, in);
            } else {
                this.mAggregatedPartialWakelockTimer = r13;
            }
            if (in.readInt() != 0) {
                this.mBluetoothScanTimer = new DualTimer(this.mBsi.mClock, this, 19, this.mBsi.mBluetoothScanOnTimers, this.mBsi.mOnBatteryTimeBase, this.mOnBatteryBackgroundTimeBase, in);
            } else {
                this.mBluetoothScanTimer = r13;
            }
            if (in.readInt() != 0) {
                this.mBluetoothUnoptimizedScanTimer = new DualTimer(this.mBsi.mClock, this, 21, null, this.mBsi.mOnBatteryTimeBase, this.mOnBatteryBackgroundTimeBase, in);
            } else {
                this.mBluetoothUnoptimizedScanTimer = r13;
            }
            if (in.readInt() != 0) {
                this.mBluetoothScanResultCounter = new Counter(this.mBsi.mOnBatteryTimeBase, in);
            } else {
                this.mBluetoothScanResultCounter = r13;
            }
            if (in.readInt() != 0) {
                this.mBluetoothScanResultBgCounter = new Counter(this.mOnBatteryBackgroundTimeBase, in);
            } else {
                this.mBluetoothScanResultBgCounter = r13;
            }
            this.mProcessState = 7;
            for (int i3 = 0; i3 < 7; i3++) {
                if (in.readInt() != 0) {
                    makeProcessState(i3, in);
                } else {
                    this.mProcessStateTimer[i3] = r13;
                }
            }
            if (in.readInt() != 0) {
                this.mVibratorOnTimer = new BatchTimer(this.mBsi.mClock, this, 9, this.mBsi.mOnBatteryTimeBase, in);
            } else {
                this.mVibratorOnTimer = r13;
            }
            if (in.readInt() != 0) {
                this.mUserActivityCounters = new Counter[NUM_USER_ACTIVITY_TYPES];
                for (int i4 = 0; i4 < NUM_USER_ACTIVITY_TYPES; i4++) {
                    this.mUserActivityCounters[i4] = new Counter(this.mBsi.mOnBatteryTimeBase, in);
                }
            } else {
                this.mUserActivityCounters = r13;
            }
            if (in.readInt() != 0) {
                this.mNetworkByteActivityCounters = new LongSamplingCounter[10];
                this.mNetworkPacketActivityCounters = new LongSamplingCounter[10];
                for (int i5 = 0; i5 < 10; i5++) {
                    this.mNetworkByteActivityCounters[i5] = new LongSamplingCounter(this.mBsi.mOnBatteryTimeBase, in);
                    this.mNetworkPacketActivityCounters[i5] = new LongSamplingCounter(this.mBsi.mOnBatteryTimeBase, in);
                }
                if (in.readBoolean()) {
                    this.mMobileRadioActiveTime = TimeMultiStateCounter.readFromParcel(in, this.mBsi.mOnBatteryTimeBase, 5, timestampMs);
                }
                this.mMobileRadioActiveCount = new LongSamplingCounter(this.mBsi.mOnBatteryTimeBase, in);
            } else {
                this.mNetworkByteActivityCounters = r13;
                this.mNetworkPacketActivityCounters = r13;
            }
            if (in.readInt() != 0) {
                this.mWifiControllerActivity = new ControllerActivityCounterImpl(this.mBsi.mClock, this.mBsi.mOnBatteryTimeBase, 1, in);
            } else {
                this.mWifiControllerActivity = r13;
            }
            if (in.readInt() != 0) {
                this.mBluetoothControllerActivity = new ControllerActivityCounterImpl(this.mBsi.mClock, this.mBsi.mOnBatteryTimeBase, 1, in);
            } else {
                this.mBluetoothControllerActivity = r13;
            }
            if (in.readInt() != 0) {
                this.mModemControllerActivity = new ControllerActivityCounterImpl(this.mBsi.mClock, this.mBsi.mOnBatteryTimeBase, ModemActivityInfo.getNumTxPowerLevels(), in);
            } else {
                this.mModemControllerActivity = r13;
            }
            if (in.readInt() != 0) {
                this.mUidMeasuredEnergyStats = new MeasuredEnergyStats(this.mBsi.mMeasuredEnergyStatsConfig, in);
            }
            this.mUserCpuTime = new LongSamplingCounter(this.mBsi.mOnBatteryTimeBase, in);
            this.mSystemCpuTime = new LongSamplingCounter(this.mBsi.mOnBatteryTimeBase, in);
            this.mCpuClusterSpeedTimesUs = this.mBsi.readCpuSpeedCountersFromParcel(in);
            this.mCpuFreqTimeMs = LongSamplingCounterArray.readFromParcel(in, this.mBsi.mOnBatteryTimeBase);
            this.mScreenOffCpuFreqTimeMs = LongSamplingCounterArray.readFromParcel(in, this.mBsi.mOnBatteryScreenOffTimeBase);
            int stateCount = in.readInt();
            if (stateCount != 0) {
                this.mCpuActiveTimeMs = TimeMultiStateCounter.readFromParcel(in, this.mBsi.mOnBatteryTimeBase, 5, timestampMs);
            }
            this.mCpuClusterTimesMs = new LongSamplingCounterArray(this.mBsi.mOnBatteryTimeBase, in);
            int stateCount2 = in.readInt();
            if (stateCount2 != 0) {
                this.mProcStateTimeMs = TimeInFreqMultiStateCounter.readFromParcel(in, this.mBsi.mOnBatteryTimeBase, 8, this.mBsi.getCpuFreqCount(), this.mBsi.mClock.elapsedRealtime());
            } else {
                this.mProcStateTimeMs = r13;
            }
            int stateCount3 = in.readInt();
            if (stateCount3 != 0) {
                this.mProcStateScreenOffTimeMs = TimeInFreqMultiStateCounter.readFromParcel(in, this.mBsi.mOnBatteryScreenOffTimeBase, 8, this.mBsi.getCpuFreqCount(), this.mBsi.mClock.elapsedRealtime());
            } else {
                this.mProcStateScreenOffTimeMs = r13;
            }
            if (in.readInt() != 0) {
                this.mMobileRadioApWakeupCount = new LongSamplingCounter(this.mBsi.mOnBatteryTimeBase, in);
            } else {
                this.mMobileRadioApWakeupCount = r13;
            }
            if (in.readInt() != 0) {
                this.mWifiRadioApWakeupCount = new LongSamplingCounter(this.mBsi.mOnBatteryTimeBase, in);
            } else {
                this.mWifiRadioApWakeupCount = r13;
            }
            this.mProportionalSystemServiceUsage = in.readDouble();
        }

        public void noteJobsDeferredLocked(int numDeferred, long sinceLast) {
            this.mJobsDeferredEventCount.addAtomic(1);
            this.mJobsDeferredCount.addAtomic(numDeferred);
            if (sinceLast != 0) {
                this.mJobsFreshnessTimeMs.addCountLocked(sinceLast);
                for (int i = 0; i < BatteryStats.JOB_FRESHNESS_BUCKETS.length; i++) {
                    if (sinceLast < BatteryStats.JOB_FRESHNESS_BUCKETS[i]) {
                        Counter[] counterArr = this.mJobsFreshnessBuckets;
                        if (counterArr[i] == null) {
                            counterArr[i] = new Counter(this.mBsi.mOnBatteryTimeBase);
                        }
                        this.mJobsFreshnessBuckets[i].addAtomic(1);
                        return;
                    }
                }
            }
        }

        public void noteBinderCallStatsLocked(long incrementalCallCount, Collection<BinderCallsStats.CallStat> callStats) {
            BinderCallStats bcs;
            this.mBinderCallCount += incrementalCallCount;
            for (BinderCallsStats.CallStat stat : callStats) {
                sTempBinderCallStats.binderClass = stat.binderClass;
                sTempBinderCallStats.transactionCode = stat.transactionCode;
                int index = this.mBinderCallStats.indexOf(sTempBinderCallStats);
                if (index >= 0) {
                    bcs = this.mBinderCallStats.valueAt(index);
                } else {
                    bcs = new BinderCallStats();
                    bcs.binderClass = stat.binderClass;
                    bcs.transactionCode = stat.transactionCode;
                    this.mBinderCallStats.add(bcs);
                }
                bcs.callCount += stat.incrementalCallCount;
                bcs.recordedCallCount = stat.recordedCallCount;
                bcs.recordedCpuTimeMicros = stat.cpuTimeMicros;
            }
        }

        /* loaded from: classes4.dex */
        public static class Wakelock extends BatteryStats.Uid.Wakelock {
            protected BatteryStatsImpl mBsi;
            StopwatchTimer mTimerDraw;
            StopwatchTimer mTimerFull;
            DualTimer mTimerPartial;
            StopwatchTimer mTimerWindow;
            protected Uid mUid;

            public Wakelock(BatteryStatsImpl bsi, Uid uid) {
                this.mBsi = bsi;
                this.mUid = uid;
            }

            private StopwatchTimer readStopwatchTimerFromParcel(int type, ArrayList<StopwatchTimer> pool, TimeBase timeBase, Parcel in) {
                if (in.readInt() == 0) {
                    return null;
                }
                return new StopwatchTimer(this.mBsi.mClock, this.mUid, type, pool, timeBase, in);
            }

            private DualTimer readDualTimerFromParcel(int type, ArrayList<StopwatchTimer> pool, TimeBase timeBase, TimeBase bgTimeBase, Parcel in) {
                if (in.readInt() == 0) {
                    return null;
                }
                return new DualTimer(this.mBsi.mClock, this.mUid, type, pool, timeBase, bgTimeBase, in);
            }

            boolean reset(long elapsedRealtimeUs) {
                boolean wlactive = false | (!BatteryStatsImpl.resetIfNotNull(this.mTimerFull, false, elapsedRealtimeUs)) | (!BatteryStatsImpl.resetIfNotNull(this.mTimerPartial, false, elapsedRealtimeUs)) | (!BatteryStatsImpl.resetIfNotNull(this.mTimerWindow, false, elapsedRealtimeUs)) | (!BatteryStatsImpl.resetIfNotNull(this.mTimerDraw, false, elapsedRealtimeUs));
                if (!wlactive) {
                    BatteryStatsImpl.detachIfNotNull(this.mTimerFull);
                    this.mTimerFull = null;
                    BatteryStatsImpl.detachIfNotNull(this.mTimerPartial);
                    this.mTimerPartial = null;
                    BatteryStatsImpl.detachIfNotNull(this.mTimerWindow);
                    this.mTimerWindow = null;
                    BatteryStatsImpl.detachIfNotNull(this.mTimerDraw);
                    this.mTimerDraw = null;
                }
                return !wlactive;
            }

            void readFromParcelLocked(TimeBase timeBase, TimeBase screenOffTimeBase, TimeBase screenOffBgTimeBase, Parcel in) {
                this.mTimerPartial = readDualTimerFromParcel(0, this.mBsi.mPartialTimers, screenOffTimeBase, screenOffBgTimeBase, in);
                this.mTimerFull = readStopwatchTimerFromParcel(1, this.mBsi.mFullTimers, timeBase, in);
                this.mTimerWindow = readStopwatchTimerFromParcel(2, this.mBsi.mWindowTimers, timeBase, in);
                this.mTimerDraw = readStopwatchTimerFromParcel(18, this.mBsi.mDrawTimers, timeBase, in);
            }

            void writeToParcelLocked(Parcel out, long elapsedRealtimeUs) {
                Timer.writeTimerToParcel(out, this.mTimerPartial, elapsedRealtimeUs);
                Timer.writeTimerToParcel(out, this.mTimerFull, elapsedRealtimeUs);
                Timer.writeTimerToParcel(out, this.mTimerWindow, elapsedRealtimeUs);
                Timer.writeTimerToParcel(out, this.mTimerDraw, elapsedRealtimeUs);
            }

            /* JADX DEBUG: Method merged with bridge method */
            @Override // android.os.BatteryStats.Uid.Wakelock
            public Timer getWakeTime(int type) {
                switch (type) {
                    case 0:
                        return this.mTimerPartial;
                    case 1:
                        return this.mTimerFull;
                    case 2:
                        return this.mTimerWindow;
                    case 18:
                        return this.mTimerDraw;
                    default:
                        throw new IllegalArgumentException("type = " + type);
                }
            }

            public void detachFromTimeBase() {
                BatteryStatsImpl.detachIfNotNull(this.mTimerPartial);
                BatteryStatsImpl.detachIfNotNull(this.mTimerFull);
                BatteryStatsImpl.detachIfNotNull(this.mTimerWindow);
                BatteryStatsImpl.detachIfNotNull(this.mTimerDraw);
            }
        }

        /* loaded from: classes4.dex */
        public static class Sensor extends BatteryStats.Uid.Sensor {
            protected BatteryStatsImpl mBsi;
            final int mHandle;
            DualTimer mTimer;
            protected Uid mUid;

            public Sensor(BatteryStatsImpl bsi, Uid uid, int handle) {
                this.mBsi = bsi;
                this.mUid = uid;
                this.mHandle = handle;
            }

            private DualTimer readTimersFromParcel(TimeBase timeBase, TimeBase bgTimeBase, Parcel in) {
                if (in.readInt() == 0) {
                    return null;
                }
                ArrayList<StopwatchTimer> pool = this.mBsi.mSensorTimers.get(this.mHandle);
                if (pool == null) {
                    pool = new ArrayList<>();
                    this.mBsi.mSensorTimers.put(this.mHandle, pool);
                }
                return new DualTimer(this.mBsi.mClock, this.mUid, 0, pool, timeBase, bgTimeBase, in);
            }

            boolean reset(long elapsedRealtimeUs) {
                if (this.mTimer.reset(true, elapsedRealtimeUs)) {
                    this.mTimer = null;
                    return true;
                }
                return false;
            }

            void readFromParcelLocked(TimeBase timeBase, TimeBase bgTimeBase, Parcel in) {
                this.mTimer = readTimersFromParcel(timeBase, bgTimeBase, in);
            }

            void writeToParcelLocked(Parcel out, long elapsedRealtimeUs) {
                Timer.writeTimerToParcel(out, this.mTimer, elapsedRealtimeUs);
            }

            /* JADX DEBUG: Method merged with bridge method */
            @Override // android.os.BatteryStats.Uid.Sensor
            public Timer getSensorTime() {
                return this.mTimer;
            }

            /* JADX DEBUG: Method merged with bridge method */
            @Override // android.os.BatteryStats.Uid.Sensor
            public Timer getSensorBackgroundTime() {
                DualTimer dualTimer = this.mTimer;
                if (dualTimer == null) {
                    return null;
                }
                return dualTimer.getSubTimer();
            }

            @Override // android.os.BatteryStats.Uid.Sensor
            public int getHandle() {
                return this.mHandle;
            }

            public void detachFromTimeBase() {
                BatteryStatsImpl.detachIfNotNull(this.mTimer);
            }
        }

        /* loaded from: classes4.dex */
        public static class Proc extends BatteryStats.Uid.Proc implements TimeBaseObs {
            boolean mActive = true;
            protected BatteryStatsImpl mBsi;
            ArrayList<BatteryStats.Uid.Proc.ExcessivePower> mExcessivePower;
            long mForegroundTimeMs;
            final String mName;
            int mNumAnrs;
            int mNumCrashes;
            int mStarts;
            long mSystemTimeMs;
            long mUserTimeMs;

            public Proc(BatteryStatsImpl bsi, String name) {
                this.mBsi = bsi;
                this.mName = name;
                bsi.mOnBatteryTimeBase.add(this);
            }

            @Override // com.android.internal.os.BatteryStatsImpl.TimeBaseObs
            public void onTimeStarted(long elapsedRealtimeUs, long baseUptimeUs, long baseRealtimeUs) {
            }

            @Override // com.android.internal.os.BatteryStatsImpl.TimeBaseObs
            public void onTimeStopped(long elapsedRealtimeUs, long baseUptimeUs, long baseRealtimeUs) {
            }

            @Override // com.android.internal.os.BatteryStatsImpl.TimeBaseObs
            public boolean reset(boolean detachIfReset, long elapsedRealtimeUs) {
                if (detachIfReset) {
                    detach();
                    return true;
                }
                return true;
            }

            @Override // com.android.internal.os.BatteryStatsImpl.TimeBaseObs
            public void detach() {
                this.mActive = false;
                this.mBsi.mOnBatteryTimeBase.remove(this);
            }

            @Override // android.os.BatteryStats.Uid.Proc
            public int countExcessivePowers() {
                ArrayList<BatteryStats.Uid.Proc.ExcessivePower> arrayList = this.mExcessivePower;
                if (arrayList != null) {
                    return arrayList.size();
                }
                return 0;
            }

            @Override // android.os.BatteryStats.Uid.Proc
            public BatteryStats.Uid.Proc.ExcessivePower getExcessivePower(int i) {
                ArrayList<BatteryStats.Uid.Proc.ExcessivePower> arrayList = this.mExcessivePower;
                if (arrayList != null) {
                    return arrayList.get(i);
                }
                return null;
            }

            public void addExcessiveCpu(long overTimeMs, long usedTimeMs) {
                if (this.mExcessivePower == null) {
                    this.mExcessivePower = new ArrayList<>();
                }
                BatteryStats.Uid.Proc.ExcessivePower ew = new BatteryStats.Uid.Proc.ExcessivePower();
                ew.type = 2;
                ew.overTime = overTimeMs;
                ew.usedTime = usedTimeMs;
                this.mExcessivePower.add(ew);
            }

            void writeExcessivePowerToParcelLocked(Parcel out) {
                ArrayList<BatteryStats.Uid.Proc.ExcessivePower> arrayList = this.mExcessivePower;
                if (arrayList == null) {
                    out.writeInt(0);
                    return;
                }
                int N = arrayList.size();
                out.writeInt(N);
                for (int i = 0; i < N; i++) {
                    BatteryStats.Uid.Proc.ExcessivePower ew = this.mExcessivePower.get(i);
                    out.writeInt(ew.type);
                    out.writeLong(ew.overTime);
                    out.writeLong(ew.usedTime);
                }
            }

            void readExcessivePowerFromParcelLocked(Parcel in) {
                int N = in.readInt();
                if (N == 0) {
                    this.mExcessivePower = null;
                } else if (N > 10000) {
                    throw new ParcelFormatException("File corrupt: too many excessive power entries " + N);
                } else {
                    this.mExcessivePower = new ArrayList<>();
                    for (int i = 0; i < N; i++) {
                        BatteryStats.Uid.Proc.ExcessivePower ew = new BatteryStats.Uid.Proc.ExcessivePower();
                        ew.type = in.readInt();
                        ew.overTime = in.readLong();
                        ew.usedTime = in.readLong();
                        this.mExcessivePower.add(ew);
                    }
                }
            }

            void writeToParcelLocked(Parcel out) {
                out.writeLong(this.mUserTimeMs);
                out.writeLong(this.mSystemTimeMs);
                out.writeLong(this.mForegroundTimeMs);
                out.writeInt(this.mStarts);
                out.writeInt(this.mNumCrashes);
                out.writeInt(this.mNumAnrs);
                writeExcessivePowerToParcelLocked(out);
            }

            void readFromParcelLocked(Parcel in) {
                this.mUserTimeMs = in.readLong();
                this.mSystemTimeMs = in.readLong();
                this.mForegroundTimeMs = in.readLong();
                this.mStarts = in.readInt();
                this.mNumCrashes = in.readInt();
                this.mNumAnrs = in.readInt();
                readExcessivePowerFromParcelLocked(in);
            }

            public void addCpuTimeLocked(int utimeMs, int stimeMs) {
                addCpuTimeLocked(utimeMs, stimeMs, this.mBsi.mOnBatteryTimeBase.isRunning());
            }

            public void addCpuTimeLocked(int utimeMs, int stimeMs, boolean isRunning) {
                if (isRunning) {
                    this.mUserTimeMs += utimeMs;
                    this.mSystemTimeMs += stimeMs;
                }
            }

            public void addForegroundTimeLocked(long ttimeMs) {
                this.mForegroundTimeMs += ttimeMs;
            }

            public void incStartsLocked() {
                this.mStarts++;
            }

            public void incNumCrashesLocked() {
                this.mNumCrashes++;
            }

            public void incNumAnrsLocked() {
                this.mNumAnrs++;
            }

            @Override // android.os.BatteryStats.Uid.Proc
            public boolean isActive() {
                return this.mActive;
            }

            @Override // android.os.BatteryStats.Uid.Proc
            public long getUserTime(int which) {
                return this.mUserTimeMs;
            }

            @Override // android.os.BatteryStats.Uid.Proc
            public long getSystemTime(int which) {
                return this.mSystemTimeMs;
            }

            @Override // android.os.BatteryStats.Uid.Proc
            public long getForegroundTime(int which) {
                return this.mForegroundTimeMs;
            }

            @Override // android.os.BatteryStats.Uid.Proc
            public int getStarts(int which) {
                return this.mStarts;
            }

            @Override // android.os.BatteryStats.Uid.Proc
            public int getNumCrashes(int which) {
                return this.mNumCrashes;
            }

            @Override // android.os.BatteryStats.Uid.Proc
            public int getNumAnrs(int which) {
                return this.mNumAnrs;
            }
        }

        /* loaded from: classes4.dex */
        public static class Pkg extends BatteryStats.Uid.Pkg implements TimeBaseObs {
            protected BatteryStatsImpl mBsi;
            ArrayMap<String, Counter> mWakeupAlarms = new ArrayMap<>();
            final ArrayMap<String, Serv> mServiceStats = new ArrayMap<>();

            public Pkg(BatteryStatsImpl bsi) {
                this.mBsi = bsi;
                bsi.mOnBatteryScreenOffTimeBase.add(this);
            }

            @Override // com.android.internal.os.BatteryStatsImpl.TimeBaseObs
            public void onTimeStarted(long elapsedRealtimeUs, long baseUptimeUs, long baseRealtimeUs) {
            }

            @Override // com.android.internal.os.BatteryStatsImpl.TimeBaseObs
            public void onTimeStopped(long elapsedRealtimeUs, long baseUptimeUs, long baseRealtimeUs) {
            }

            @Override // com.android.internal.os.BatteryStatsImpl.TimeBaseObs
            public boolean reset(boolean detachIfReset, long elapsedRealtimeUs) {
                if (detachIfReset) {
                    detach();
                    return true;
                }
                return true;
            }

            @Override // com.android.internal.os.BatteryStatsImpl.TimeBaseObs
            public void detach() {
                this.mBsi.mOnBatteryScreenOffTimeBase.remove(this);
                for (int j = this.mWakeupAlarms.size() - 1; j >= 0; j--) {
                    BatteryStatsImpl.detachIfNotNull(this.mWakeupAlarms.valueAt(j));
                }
                for (int j2 = this.mServiceStats.size() - 1; j2 >= 0; j2--) {
                    BatteryStatsImpl.detachIfNotNull(this.mServiceStats.valueAt(j2));
                }
            }

            void readFromParcelLocked(Parcel in) {
                int numWA = in.readInt();
                this.mWakeupAlarms.clear();
                for (int i = 0; i < numWA; i++) {
                    String tag = in.readString();
                    this.mWakeupAlarms.put(tag, new Counter(this.mBsi.mOnBatteryScreenOffTimeBase, in));
                }
                int numServs = in.readInt();
                this.mServiceStats.clear();
                for (int m = 0; m < numServs; m++) {
                    String serviceName = in.readString();
                    Serv serv = new Serv(this.mBsi);
                    this.mServiceStats.put(serviceName, serv);
                    serv.readFromParcelLocked(in);
                }
            }

            void writeToParcelLocked(Parcel out) {
                int numWA = this.mWakeupAlarms.size();
                out.writeInt(numWA);
                for (int i = 0; i < numWA; i++) {
                    out.writeString(this.mWakeupAlarms.keyAt(i));
                    this.mWakeupAlarms.valueAt(i).writeToParcel(out);
                }
                int NS = this.mServiceStats.size();
                out.writeInt(NS);
                for (int i2 = 0; i2 < NS; i2++) {
                    out.writeString(this.mServiceStats.keyAt(i2));
                    Serv serv = this.mServiceStats.valueAt(i2);
                    serv.writeToParcelLocked(out);
                }
            }

            @Override // android.os.BatteryStats.Uid.Pkg
            public ArrayMap<String, ? extends BatteryStats.Counter> getWakeupAlarmStats() {
                return this.mWakeupAlarms;
            }

            public void noteWakeupAlarmLocked(String tag) {
                Counter c = this.mWakeupAlarms.get(tag);
                if (c == null) {
                    c = new Counter(this.mBsi.mOnBatteryScreenOffTimeBase);
                    this.mWakeupAlarms.put(tag, c);
                }
                c.stepAtomic();
            }

            @Override // android.os.BatteryStats.Uid.Pkg
            public ArrayMap<String, ? extends BatteryStats.Uid.Pkg.Serv> getServiceStats() {
                return this.mServiceStats;
            }

            /* loaded from: classes4.dex */
            public static class Serv extends BatteryStats.Uid.Pkg.Serv implements TimeBaseObs {
                protected BatteryStatsImpl mBsi;
                protected boolean mLaunched;
                protected long mLaunchedSinceMs;
                protected long mLaunchedTimeMs;
                protected int mLaunches;
                protected Pkg mPkg;
                protected boolean mRunning;
                protected long mRunningSinceMs;
                protected long mStartTimeMs;
                protected int mStarts;

                public Serv(BatteryStatsImpl bsi) {
                    this.mBsi = bsi;
                    bsi.mOnBatteryTimeBase.add(this);
                }

                @Override // com.android.internal.os.BatteryStatsImpl.TimeBaseObs
                public void onTimeStarted(long elapsedRealtimeUs, long baseUptimeUs, long baseRealtimeUs) {
                }

                @Override // com.android.internal.os.BatteryStatsImpl.TimeBaseObs
                public void onTimeStopped(long elapsedRealtimeUs, long baseUptimeUs, long baseRealtimeUs) {
                }

                @Override // com.android.internal.os.BatteryStatsImpl.TimeBaseObs
                public boolean reset(boolean detachIfReset, long elapsedRealtimeUs) {
                    if (detachIfReset) {
                        detach();
                        return true;
                    }
                    return true;
                }

                @Override // com.android.internal.os.BatteryStatsImpl.TimeBaseObs
                public void detach() {
                    this.mBsi.mOnBatteryTimeBase.remove(this);
                }

                public void readFromParcelLocked(Parcel in) {
                    this.mStartTimeMs = in.readLong();
                    this.mRunningSinceMs = in.readLong();
                    this.mRunning = in.readInt() != 0;
                    this.mStarts = in.readInt();
                    this.mLaunchedTimeMs = in.readLong();
                    this.mLaunchedSinceMs = in.readLong();
                    this.mLaunched = in.readInt() != 0;
                    this.mLaunches = in.readInt();
                }

                public void writeToParcelLocked(Parcel out) {
                    out.writeLong(this.mStartTimeMs);
                    out.writeLong(this.mRunningSinceMs);
                    out.writeInt(this.mRunning ? 1 : 0);
                    out.writeInt(this.mStarts);
                    out.writeLong(this.mLaunchedTimeMs);
                    out.writeLong(this.mLaunchedSinceMs);
                    out.writeInt(this.mLaunched ? 1 : 0);
                    out.writeInt(this.mLaunches);
                }

                public long getLaunchTimeToNowLocked(long batteryUptimeMs) {
                    return !this.mLaunched ? this.mLaunchedTimeMs : (this.mLaunchedTimeMs + batteryUptimeMs) - this.mLaunchedSinceMs;
                }

                public long getStartTimeToNowLocked(long batteryUptimeMs) {
                    return !this.mRunning ? this.mStartTimeMs : (this.mStartTimeMs + batteryUptimeMs) - this.mRunningSinceMs;
                }

                public void startLaunchedLocked() {
                    startLaunchedLocked(this.mBsi.mClock.uptimeMillis());
                }

                public void startLaunchedLocked(long uptimeMs) {
                    if (!this.mLaunched) {
                        this.mLaunches++;
                        this.mLaunchedSinceMs = this.mBsi.getBatteryUptimeLocked(uptimeMs) / 1000;
                        this.mLaunched = true;
                    }
                }

                public void stopLaunchedLocked() {
                    stopLaunchedLocked(this.mBsi.mClock.uptimeMillis());
                }

                public void stopLaunchedLocked(long uptimeMs) {
                    if (this.mLaunched) {
                        long timeMs = (this.mBsi.getBatteryUptimeLocked(uptimeMs) / 1000) - this.mLaunchedSinceMs;
                        if (timeMs > 0) {
                            this.mLaunchedTimeMs += timeMs;
                        } else {
                            this.mLaunches--;
                        }
                        this.mLaunched = false;
                    }
                }

                public void startRunningLocked() {
                    startRunningLocked(this.mBsi.mClock.uptimeMillis());
                }

                public void startRunningLocked(long uptimeMs) {
                    if (!this.mRunning) {
                        this.mStarts++;
                        this.mRunningSinceMs = this.mBsi.getBatteryUptimeLocked(uptimeMs) / 1000;
                        this.mRunning = true;
                    }
                }

                public void stopRunningLocked() {
                    stopRunningLocked(this.mBsi.mClock.uptimeMillis());
                }

                public void stopRunningLocked(long uptimeMs) {
                    if (this.mRunning) {
                        long timeMs = (this.mBsi.getBatteryUptimeLocked(uptimeMs) / 1000) - this.mRunningSinceMs;
                        if (timeMs > 0) {
                            this.mStartTimeMs += timeMs;
                        } else {
                            this.mStarts--;
                        }
                        this.mRunning = false;
                    }
                }

                public BatteryStatsImpl getBatteryStats() {
                    return this.mBsi;
                }

                @Override // android.os.BatteryStats.Uid.Pkg.Serv
                public int getLaunches(int which) {
                    return this.mLaunches;
                }

                @Override // android.os.BatteryStats.Uid.Pkg.Serv
                public long getStartTime(long now, int which) {
                    return getStartTimeToNowLocked(now);
                }

                @Override // android.os.BatteryStats.Uid.Pkg.Serv
                public int getStarts(int which) {
                    return this.mStarts;
                }
            }

            final Serv newServiceStatsLocked() {
                return new Serv(this.mBsi);
            }
        }

        /* JADX INFO: Access modifiers changed from: private */
        /* loaded from: classes4.dex */
        public class ChildUid {
            public final TimeMultiStateCounter cpuActiveCounter;
            public final LongArrayMultiStateCounter cpuTimeInFreqCounter;

            ChildUid() {
                long timestampMs = Uid.this.mBsi.mClock.elapsedRealtime();
                TimeMultiStateCounter timeMultiStateCounter = new TimeMultiStateCounter(Uid.this.mBsi.mOnBatteryTimeBase, 1, timestampMs);
                this.cpuActiveCounter = timeMultiStateCounter;
                timeMultiStateCounter.setState(0, timestampMs);
                if (Uid.this.mBsi.trackPerProcStateCpuTimes()) {
                    int cpuFreqCount = Uid.this.mBsi.getCpuFreqCount();
                    LongArrayMultiStateCounter longArrayMultiStateCounter = new LongArrayMultiStateCounter(1, cpuFreqCount);
                    this.cpuTimeInFreqCounter = longArrayMultiStateCounter;
                    longArrayMultiStateCounter.updateValues(new LongArrayMultiStateCounter.LongArrayContainer(cpuFreqCount), timestampMs);
                    return;
                }
                this.cpuTimeInFreqCounter = null;
            }
        }

        public Proc getProcessStatsLocked(String name) {
            Proc ps = this.mProcessStats.get(name);
            if (ps == null) {
                Proc ps2 = new Proc(this.mBsi, name);
                this.mProcessStats.put(name, ps2);
                return ps2;
            }
            return ps;
        }

        public void updateUidProcessStateLocked(int procState, long elapsedRealtimeMs, long uptimeMs) {
            boolean userAwareService = ActivityManager.isForegroundService(procState);
            int uidRunningState = BatteryStats.mapToInternalProcessState(procState);
            int i = this.mProcessState;
            if (i == uidRunningState && userAwareService == this.mInForegroundService) {
                return;
            }
            if (i != uidRunningState) {
                if (i != 7) {
                    this.mProcessStateTimer[i].stopRunningLocked(elapsedRealtimeMs);
                }
                if (uidRunningState != 7) {
                    if (this.mProcessStateTimer[uidRunningState] == null) {
                        makeProcessState(uidRunningState, null);
                    }
                    this.mProcessStateTimer[uidRunningState].startRunningLocked(elapsedRealtimeMs);
                }
                if (this.mBsi.trackPerProcStateCpuTimes()) {
                    this.mBsi.updateProcStateCpuTimesLocked(this.mUid, elapsedRealtimeMs);
                    LongArrayMultiStateCounter onBatteryCounter = getProcStateTimeCounter(elapsedRealtimeMs).getCounter();
                    LongArrayMultiStateCounter onBatteryScreenOffCounter = getProcStateScreenOffTimeCounter(elapsedRealtimeMs).getCounter();
                    onBatteryCounter.setState(uidRunningState, elapsedRealtimeMs);
                    onBatteryScreenOffCounter.setState(uidRunningState, elapsedRealtimeMs);
                }
                int prevBatteryConsumerProcessState = BatteryStats.mapUidProcessStateToBatteryConsumerProcessState(this.mProcessState);
                this.mProcessState = uidRunningState;
                updateOnBatteryBgTimeBase(uptimeMs * 1000, elapsedRealtimeMs * 1000);
                updateOnBatteryScreenOffBgTimeBase(uptimeMs * 1000, 1000 * elapsedRealtimeMs);
                int batteryConsumerProcessState = BatteryStats.mapUidProcessStateToBatteryConsumerProcessState(uidRunningState);
                getCpuActiveTimeCounter().setState(batteryConsumerProcessState, elapsedRealtimeMs);
                getMobileRadioActiveTimeCounter().setState(batteryConsumerProcessState, elapsedRealtimeMs);
                ControllerActivityCounterImpl wifiControllerActivity = getWifiControllerActivity();
                if (wifiControllerActivity != null) {
                    wifiControllerActivity.setState(batteryConsumerProcessState, elapsedRealtimeMs);
                }
                ControllerActivityCounterImpl bluetoothControllerActivity = getBluetoothControllerActivity();
                if (bluetoothControllerActivity != null) {
                    bluetoothControllerActivity.setState(batteryConsumerProcessState, elapsedRealtimeMs);
                }
                MeasuredEnergyStats energyStats = getOrCreateMeasuredEnergyStatsIfSupportedLocked();
                if (energyStats != null) {
                    energyStats.setState(batteryConsumerProcessState, elapsedRealtimeMs);
                }
                maybeScheduleExternalStatsSync(prevBatteryConsumerProcessState, batteryConsumerProcessState);
            }
            if (userAwareService != this.mInForegroundService) {
                if (userAwareService) {
                    noteForegroundServiceResumedLocked(elapsedRealtimeMs);
                } else {
                    noteForegroundServicePausedLocked(elapsedRealtimeMs);
                }
                this.mInForegroundService = userAwareService;
            }
        }

        private void maybeScheduleExternalStatsSync(int oldProcessState, int newProcessState) {
            if (oldProcessState == newProcessState) {
                return;
            }
            if (oldProcessState != 0 || newProcessState != 2) {
                if (oldProcessState == 2 && newProcessState == 0) {
                    return;
                }
                int flags = 14;
                if (!BatteryStatsImpl.isActiveRadioPowerState(this.mBsi.mMobileRadioPowerState)) {
                    flags = 14 & (-5);
                }
                this.mBsi.mExternalSync.scheduleSyncDueToProcessStateChange(flags, this.mBsi.mConstants.PROC_STATE_CHANGE_COLLECTION_DELAY_MS);
            }
        }

        public boolean isInBackground() {
            return this.mProcessState >= 3;
        }

        public boolean updateOnBatteryBgTimeBase(long uptimeUs, long realtimeUs) {
            boolean on = this.mBsi.mOnBatteryTimeBase.isRunning() && isInBackground();
            return this.mOnBatteryBackgroundTimeBase.setRunning(on, uptimeUs, realtimeUs);
        }

        public boolean updateOnBatteryScreenOffBgTimeBase(long uptimeUs, long realtimeUs) {
            boolean on = this.mBsi.mOnBatteryScreenOffTimeBase.isRunning() && isInBackground();
            return this.mOnBatteryScreenOffBackgroundTimeBase.setRunning(on, uptimeUs, realtimeUs);
        }

        @Override // android.os.BatteryStats.Uid
        public SparseArray<? extends BatteryStats.Uid.Pid> getPidStats() {
            return this.mPids;
        }

        public BatteryStats.Uid.Pid getPidStatsLocked(int pid) {
            BatteryStats.Uid.Pid p = this.mPids.get(pid);
            if (p == null) {
                BatteryStats.Uid.Pid p2 = new BatteryStats.Uid.Pid();
                this.mPids.put(pid, p2);
                return p2;
            }
            return p;
        }

        public Pkg getPackageStatsLocked(String name) {
            Pkg ps = this.mPackageStats.get(name);
            if (ps == null) {
                Pkg ps2 = new Pkg(this.mBsi);
                this.mPackageStats.put(name, ps2);
                return ps2;
            }
            return ps;
        }

        public Pkg.Serv getServiceStatsLocked(String pkg, String serv) {
            Pkg ps = getPackageStatsLocked(pkg);
            Pkg.Serv ss = ps.mServiceStats.get(serv);
            if (ss == null) {
                Pkg.Serv ss2 = ps.newServiceStatsLocked();
                ps.mServiceStats.put(serv, ss2);
                return ss2;
            }
            return ss;
        }

        public void readSyncSummaryFromParcelLocked(String name, Parcel in) {
            DualTimer timer = this.mSyncStats.instantiateObject();
            timer.readSummaryFromParcelLocked(in);
            this.mSyncStats.add(name, timer);
        }

        public void readJobSummaryFromParcelLocked(String name, Parcel in) {
            DualTimer timer = this.mJobStats.instantiateObject();
            timer.readSummaryFromParcelLocked(in);
            this.mJobStats.add(name, timer);
        }

        public void readWakeSummaryFromParcelLocked(String wlName, Parcel in) {
            Wakelock wl = new Wakelock(this.mBsi, this);
            this.mWakelockStats.add(wlName, wl);
            if (in.readInt() != 0) {
                getWakelockTimerLocked(wl, 1).readSummaryFromParcelLocked(in);
            }
            if (in.readInt() != 0) {
                getWakelockTimerLocked(wl, 0).readSummaryFromParcelLocked(in);
            }
            if (in.readInt() != 0) {
                getWakelockTimerLocked(wl, 2).readSummaryFromParcelLocked(in);
            }
            if (in.readInt() != 0) {
                getWakelockTimerLocked(wl, 18).readSummaryFromParcelLocked(in);
            }
        }

        public DualTimer getSensorTimerLocked(int sensor, boolean create) {
            Sensor se = this.mSensorStats.get(sensor);
            if (se == null) {
                if (!create) {
                    return null;
                }
                se = new Sensor(this.mBsi, this, sensor);
                this.mSensorStats.put(sensor, se);
            }
            DualTimer t = se.mTimer;
            if (t != null) {
                return t;
            }
            ArrayList<StopwatchTimer> timers = this.mBsi.mSensorTimers.get(sensor);
            if (timers == null) {
                timers = new ArrayList<>();
                this.mBsi.mSensorTimers.put(sensor, timers);
            }
            DualTimer t2 = new DualTimer(this.mBsi.mClock, this, 3, timers, this.mBsi.mOnBatteryTimeBase, this.mOnBatteryBackgroundTimeBase);
            se.mTimer = t2;
            return t2;
        }

        public void noteStartSyncLocked(String name, long elapsedRealtimeMs) {
            DualTimer t = this.mSyncStats.startObject(name, elapsedRealtimeMs);
            if (t != null) {
                t.startRunningLocked(elapsedRealtimeMs);
            }
        }

        public void noteStopSyncLocked(String name, long elapsedRealtimeMs) {
            DualTimer t = this.mSyncStats.stopObject(name, elapsedRealtimeMs);
            if (t != null) {
                t.stopRunningLocked(elapsedRealtimeMs);
            }
        }

        public void noteStartJobLocked(String name, long elapsedRealtimeMs) {
            DualTimer t = this.mJobStats.startObject(name, elapsedRealtimeMs);
            if (t != null) {
                t.startRunningLocked(elapsedRealtimeMs);
            }
        }

        public void noteStopJobLocked(String name, long elapsedRealtimeMs, int stopReason) {
            DualTimer t = this.mJobStats.stopObject(name, elapsedRealtimeMs);
            if (t != null) {
                t.stopRunningLocked(elapsedRealtimeMs);
            }
            if (this.mBsi.mOnBatteryTimeBase.isRunning()) {
                SparseIntArray types = this.mJobCompletions.get(name);
                if (types == null) {
                    types = new SparseIntArray();
                    this.mJobCompletions.put(name, types);
                }
                int last = types.get(stopReason, 0);
                types.put(stopReason, last + 1);
            }
        }

        public StopwatchTimer getWakelockTimerLocked(Wakelock wl, int type) {
            if (wl == null) {
                return null;
            }
            switch (type) {
                case 0:
                    StopwatchTimer t = wl.mTimerPartial;
                    if (t == null) {
                        DualTimer t2 = new DualTimer(this.mBsi.mClock, this, 0, this.mBsi.mPartialTimers, this.mBsi.mOnBatteryScreenOffTimeBase, this.mOnBatteryScreenOffBackgroundTimeBase);
                        wl.mTimerPartial = t2;
                        return t2;
                    }
                    return t;
                case 1:
                    StopwatchTimer t3 = wl.mTimerFull;
                    if (t3 == null) {
                        StopwatchTimer t4 = new StopwatchTimer(this.mBsi.mClock, this, 1, this.mBsi.mFullTimers, this.mBsi.mOnBatteryTimeBase);
                        wl.mTimerFull = t4;
                        return t4;
                    }
                    return t3;
                case 2:
                    StopwatchTimer t5 = wl.mTimerWindow;
                    if (t5 == null) {
                        StopwatchTimer t6 = new StopwatchTimer(this.mBsi.mClock, this, 2, this.mBsi.mWindowTimers, this.mBsi.mOnBatteryTimeBase);
                        wl.mTimerWindow = t6;
                        return t6;
                    }
                    return t5;
                case 18:
                    StopwatchTimer t7 = wl.mTimerDraw;
                    if (t7 == null) {
                        StopwatchTimer t8 = new StopwatchTimer(this.mBsi.mClock, this, 18, this.mBsi.mDrawTimers, this.mBsi.mOnBatteryTimeBase);
                        wl.mTimerDraw = t8;
                        return t8;
                    }
                    return t7;
                default:
                    throw new IllegalArgumentException("type=" + type);
            }
        }

        public void noteStartWakeLocked(int pid, String name, int type, long elapsedRealtimeMs) {
            Wakelock wl = this.mWakelockStats.startObject(name, elapsedRealtimeMs);
            if (wl != null) {
                getWakelockTimerLocked(wl, type).startRunningLocked(elapsedRealtimeMs);
            }
            if (type == 0) {
                createAggregatedPartialWakelockTimerLocked().startRunningLocked(elapsedRealtimeMs);
                if (pid >= 0) {
                    BatteryStats.Uid.Pid p = getPidStatsLocked(pid);
                    int i = p.mWakeNesting;
                    p.mWakeNesting = i + 1;
                    if (i == 0) {
                        p.mWakeStartMs = elapsedRealtimeMs;
                    }
                }
            }
        }

        public void noteStopWakeLocked(int pid, String name, int type, long elapsedRealtimeMs) {
            BatteryStats.Uid.Pid p;
            Wakelock wl = this.mWakelockStats.stopObject(name, elapsedRealtimeMs);
            if (wl != null) {
                StopwatchTimer wlt = getWakelockTimerLocked(wl, type);
                wlt.stopRunningLocked(elapsedRealtimeMs);
            }
            if (type == 0) {
                DualTimer dualTimer = this.mAggregatedPartialWakelockTimer;
                if (dualTimer != null) {
                    dualTimer.stopRunningLocked(elapsedRealtimeMs);
                }
                if (pid >= 0 && (p = this.mPids.get(pid)) != null && p.mWakeNesting > 0) {
                    int i = p.mWakeNesting;
                    p.mWakeNesting = i - 1;
                    if (i == 1) {
                        p.mWakeSumMs += elapsedRealtimeMs - p.mWakeStartMs;
                        p.mWakeStartMs = 0L;
                    }
                }
            }
        }

        public void reportExcessiveCpuLocked(String proc, long overTimeMs, long usedTimeMs) {
            Proc p = getProcessStatsLocked(proc);
            if (p != null) {
                p.addExcessiveCpu(overTimeMs, usedTimeMs);
            }
        }

        public void noteStartSensor(int sensor, long elapsedRealtimeMs) {
            DualTimer t = getSensorTimerLocked(sensor, true);
            t.startRunningLocked(elapsedRealtimeMs);
        }

        public void noteStopSensor(int sensor, long elapsedRealtimeMs) {
            DualTimer t = getSensorTimerLocked(sensor, false);
            if (t != null) {
                t.stopRunningLocked(elapsedRealtimeMs);
            }
        }

        public void noteStartGps(long elapsedRealtimeMs) {
            noteStartSensor(-10000, elapsedRealtimeMs);
        }

        public void noteStopGps(long elapsedRealtimeMs) {
            noteStopSensor(-10000, elapsedRealtimeMs);
        }

        public BatteryStatsImpl getBatteryStats() {
            return this.mBsi;
        }
    }

    @Override // android.os.BatteryStats
    public long[] getCpuFreqs() {
        if (!this.mCpuFreqsInitialized) {
            this.mCpuFreqs = this.mCpuUidFreqTimeReader.readFreqs(this.mPowerProfile);
            this.mCpuFreqsInitialized = true;
        }
        return this.mCpuFreqs;
    }

    @Override // android.os.BatteryStats
    public int getCpuFreqCount() {
        long[] cpuFreqs = getCpuFreqs();
        if (cpuFreqs != null) {
            return cpuFreqs.length;
        }
        return 0;
    }

    private LongArrayMultiStateCounter.LongArrayContainer getCpuTimeInFreqContainer() {
        if (this.mTmpCpuTimeInFreq == null) {
            this.mTmpCpuTimeInFreq = new LongArrayMultiStateCounter.LongArrayContainer(getCpuFreqCount());
        }
        return this.mTmpCpuTimeInFreq;
    }

    public BatteryStatsImpl(File systemDir, Handler handler, PlatformIdleStateCallback cb, MeasuredEnergyRetriever energyStatsCb, UserInfoProvider userInfoProvider) {
        this(Clock.SYSTEM_CLOCK, systemDir, handler, cb, energyStatsCb, userInfoProvider);
    }

    private BatteryStatsImpl(Clock clock, File systemDir, Handler handler, PlatformIdleStateCallback cb, MeasuredEnergyRetriever energyStatsCb, UserInfoProvider userInfoProvider) {
        this.mKernelWakelockReader = new KernelWakelockReader();
        this.mTmpWakelockStats = new KernelWakelockStats();
        this.mSystemServerCpuThreadReader = SystemServerCpuThreadReader.create();
        this.mKernelMemoryBandwidthStats = new KernelMemoryBandwidthStats();
        this.mKernelMemoryStats = new LongSparseArray<>();
        this.mPerProcStateCpuTimesAvailable = true;
        this.mCpuTimeReadsTrackingStartTimeMs = SystemClock.uptimeMillis();
        this.mTmpRpmStats = null;
        this.mLastRpmStatsUpdateTimeMs = -1000L;
        this.mTmpRailStats = new RailStats();
        this.mPendingRemovedUids = new LinkedList();
        this.mDeferSetCharging = new Runnable() { // from class: com.android.internal.os.BatteryStatsImpl.2
            @Override // java.lang.Runnable
            public void run() {
                synchronized (BatteryStatsImpl.this) {
                    if (BatteryStatsImpl.this.mOnBattery) {
                        return;
                    }
                    boolean changed = BatteryStatsImpl.this.setChargingLocked(true);
                    if (changed) {
                        long uptimeMs = BatteryStatsImpl.this.mClock.uptimeMillis();
                        long elapsedRealtimeMs = BatteryStatsImpl.this.mClock.elapsedRealtime();
                        BatteryStatsImpl.this.addHistoryRecordLocked(elapsedRealtimeMs, uptimeMs);
                    }
                }
            }
        };
        this.mExternalSync = null;
        this.mUserInfoProvider = null;
        this.mIsolatedUids = new SparseIntArray();
        this.mIsolatedUidRefCounts = new SparseIntArray();
        this.mUidStats = new SparseArray<>();
        this.mPartialTimers = new ArrayList<>();
        this.mFullTimers = new ArrayList<>();
        this.mWindowTimers = new ArrayList<>();
        this.mDrawTimers = new ArrayList<>();
        this.mSensorTimers = new SparseArray<>();
        this.mWifiRunningTimers = new ArrayList<>();
        this.mFullWifiLockTimers = new ArrayList<>();
        this.mWifiMulticastTimers = new ArrayList<>();
        this.mWifiScanTimers = new ArrayList<>();
        this.mWifiBatchedScanTimers = new SparseArray<>();
        this.mAudioTurnedOnTimers = new ArrayList<>();
        this.mVideoTurnedOnTimers = new ArrayList<>();
        this.mFlashlightTurnedOnTimers = new ArrayList<>();
        this.mCameraTurnedOnTimers = new ArrayList<>();
        this.mBluetoothScanOnTimers = new ArrayList<>();
        this.mLastPartialTimers = new ArrayList<>();
        this.mOnBatteryTimeBase = new TimeBase(true);
        this.mOnBatteryScreenOffTimeBase = new TimeBase(true);
        this.mActiveEvents = new BatteryStats.HistoryEventTracker();
        this.mHaveBatteryLevel = false;
        this.mRecordingHistory = false;
        this.mHistoryTagPool = new HashMap<>();
        Parcel obtain = Parcel.obtain();
        this.mHistoryBuffer = obtain;
        this.mHistoryLastWritten = new BatteryStats.HistoryItem();
        this.mHistoryLastLastWritten = new BatteryStats.HistoryItem();
        this.mHistoryAddTmp = new BatteryStats.HistoryItem();
        this.mNextHistoryTagIdx = 0;
        this.mNumHistoryTagChars = 0;
        this.mHistoryBufferLastPos = -1;
        this.mActiveHistoryStates = -1;
        this.mActiveHistoryStates2 = -1;
        this.mLastHistoryElapsedRealtimeMs = 0L;
        this.mTrackRunningHistoryElapsedRealtimeMs = 0L;
        this.mTrackRunningHistoryUptimeMs = 0L;
        this.mHistoryCur = new BatteryStats.HistoryItem();
        this.mLastHistoryStepDetails = null;
        this.mLastHistoryStepLevel = (byte) 0;
        this.mCurHistoryStepDetails = new BatteryStats.HistoryStepDetails();
        this.mReadHistoryStepDetails = new BatteryStats.HistoryStepDetails();
        this.mTmpHistoryStepDetails = new BatteryStats.HistoryStepDetails();
        this.mIgnoreNextExternalStats = false;
        this.mScreenState = 0;
        this.mScreenBrightnessBin = -1;
        this.mScreenBrightnessTimer = new StopwatchTimer[5];
        this.mDisplayMismatchWtfCount = 0;
        this.mUsbDataState = 0;
        this.mGpsSignalQualityBin = -1;
        this.mGpsSignalQualityTimer = new StopwatchTimer[2];
        this.mPhoneSignalStrengthBin = -1;
        this.mPhoneSignalStrengthBinRaw = -1;
        this.mPhoneSignalStrengthsTimer = new StopwatchTimer[CellSignalStrength.getNumSignalStrengthLevels() + 1];
        this.mPhoneDataConnectionType = -1;
        this.mPhoneDataConnectionsTimer = new StopwatchTimer[NUM_DATA_CONNECTION_TYPES];
        this.mActiveRat = 0;
        this.mPerRatBatteryStats = new RadioAccessTechnologyBatteryStats[3];
        this.mNetworkByteActivityCounters = new LongSamplingCounter[10];
        this.mNetworkPacketActivityCounters = new LongSamplingCounter[10];
        this.mHasWifiReporting = false;
        this.mHasBluetoothReporting = false;
        this.mHasModemReporting = false;
        this.mWifiState = -1;
        this.mWifiStateTimer = new StopwatchTimer[8];
        this.mWifiSupplState = -1;
        this.mWifiSupplStateTimer = new StopwatchTimer[13];
        this.mWifiSignalStrengthBin = -1;
        this.mWifiSignalStrengthsTimer = new StopwatchTimer[5];
        this.mMobileRadioPowerState = 1;
        this.mWifiRadioPowerState = 1;
        this.mBluetoothPowerCalculator = null;
        this.mCpuPowerCalculator = null;
        this.mMobileRadioPowerCalculator = null;
        this.mWifiPowerCalculator = null;
        this.mCharging = true;
        this.mInitStepMode = 0;
        this.mCurStepMode = 0;
        this.mModStepMode = 0;
        this.mDischargeStepTracker = new BatteryStats.LevelStepTracker(200);
        this.mDailyDischargeStepTracker = new BatteryStats.LevelStepTracker(400);
        this.mChargeStepTracker = new BatteryStats.LevelStepTracker(200);
        this.mDailyChargeStepTracker = new BatteryStats.LevelStepTracker(400);
        this.mDailyStartTimeMs = 0L;
        this.mNextMinDailyDeadlineMs = 0L;
        this.mNextMaxDailyDeadlineMs = 0L;
        this.mDailyItems = new ArrayList<>();
        this.mLastWriteTimeMs = 0L;
        this.mPhoneServiceState = -1;
        this.mPhoneServiceStateRaw = -1;
        this.mPhoneSimStateRaw = -1;
        this.mBatteryVoltageMv = -1;
        this.mEstimatedBatteryCapacityMah = -1;
        this.mLastLearnedBatteryCapacityUah = -1;
        this.mMinLearnedBatteryCapacityUah = -1;
        this.mMaxLearnedBatteryCapacityUah = -1;
        this.mBatteryTimeToFullSeconds = -1L;
        this.mRpmStats = new HashMap<>();
        this.mScreenOffRpmStats = new HashMap<>();
        this.mKernelWakelockStats = new HashMap<>();
        this.mLastWakeupReason = null;
        this.mLastWakeupUptimeMs = 0L;
        this.mWakeupReasonStats = new HashMap<>();
        this.mChangedStates = 0;
        this.mChangedStates2 = 0;
        this.mInitialAcquireWakeUid = -1;
        this.mWifiFullLockNesting = 0;
        this.mWifiScanNesting = 0;
        this.mWifiMulticastNesting = 0;
        this.mWifiNetworkLock = new Object();
        this.mWifiIfaces = EmptyArray.STRING;
        this.mLastWifiNetworkStats = new NetworkStats(0L, -1);
        this.mModemNetworkLock = new Object();
        this.mModemIfaces = EmptyArray.STRING;
        this.mLastModemNetworkStats = new NetworkStats(0L, -1);
        this.mLastModemActivityInfo = null;
        this.mLastBluetoothActivityInfo = new BluetoothActivityInfoCache();
        this.mWriteLock = new ReentrantLock();
        init(clock);
        if (systemDir != null) {
            this.mStatsFile = new AtomicFile(new File(systemDir, "batterystats.bin"));
            this.mBatteryStatsHistory = new BatteryStatsHistory(this, systemDir, obtain);
        } else {
            this.mStatsFile = null;
            this.mBatteryStatsHistory = new BatteryStatsHistory(obtain);
        }
        this.mCheckinFile = new AtomicFile(new File(systemDir, "batterystats-checkin.bin"));
        this.mDailyFile = new AtomicFile(new File(systemDir, "batterystats-daily.xml"));
        MyHandler myHandler = new MyHandler(handler.getLooper());
        this.mHandler = myHandler;
        this.mConstants = new Constants(myHandler);
        this.mStartCount++;
        initTimersAndCounters();
        this.mOnBatteryInternal = false;
        this.mOnBattery = false;
        long uptimeUs = this.mClock.uptimeMillis() * 1000;
        long realtimeUs = this.mClock.elapsedRealtime() * 1000;
        initTimes(uptimeUs, realtimeUs);
        String str = Build.ID;
        this.mEndPlatformVersion = str;
        this.mStartPlatformVersion = str;
        initDischarge(realtimeUs);
        clearHistoryLocked();
        updateDailyDeadlineLocked();
        this.mPlatformIdleStateCallback = cb;
        this.mMeasuredEnergyRetriever = energyStatsCb;
        this.mUserInfoProvider = userInfoProvider;
        this.mDeviceIdleMode = 0;
        FrameworkStatsLog.write(21, 0);
    }

    protected void initTimersAndCounters() {
        this.mScreenOnTimer = new StopwatchTimer(this.mClock, null, -1, null, this.mOnBatteryTimeBase);
        this.mScreenDozeTimer = new StopwatchTimer(this.mClock, null, -1, null, this.mOnBatteryTimeBase);
        for (int i = 0; i < 5; i++) {
            this.mScreenBrightnessTimer[i] = new StopwatchTimer(this.mClock, null, (-100) - i, null, this.mOnBatteryTimeBase);
        }
        this.mPerDisplayBatteryStats = r3;
        DisplayBatteryStats[] displayBatteryStatsArr = {new DisplayBatteryStats(this.mClock, this.mOnBatteryTimeBase)};
        this.mInteractiveTimer = new StopwatchTimer(this.mClock, null, -10, null, this.mOnBatteryTimeBase);
        this.mPowerSaveModeEnabledTimer = new StopwatchTimer(this.mClock, null, -2, null, this.mOnBatteryTimeBase);
        this.mDeviceIdleModeLightTimer = new StopwatchTimer(this.mClock, null, -11, null, this.mOnBatteryTimeBase);
        this.mDeviceIdleModeFullTimer = new StopwatchTimer(this.mClock, null, -14, null, this.mOnBatteryTimeBase);
        this.mDeviceLightIdlingTimer = new StopwatchTimer(this.mClock, null, -15, null, this.mOnBatteryTimeBase);
        this.mDeviceIdlingTimer = new StopwatchTimer(this.mClock, null, -12, null, this.mOnBatteryTimeBase);
        this.mPhoneOnTimer = new StopwatchTimer(this.mClock, null, -3, null, this.mOnBatteryTimeBase);
        for (int i2 = 0; i2 < CellSignalStrength.getNumSignalStrengthLevels() + 1; i2++) {
            this.mPhoneSignalStrengthsTimer[i2] = new StopwatchTimer(this.mClock, null, (-200) - i2, null, this.mOnBatteryTimeBase);
        }
        this.mPhoneSignalScanningTimer = new StopwatchTimer(this.mClock, null, -199, null, this.mOnBatteryTimeBase);
        for (int i3 = 0; i3 < NUM_DATA_CONNECTION_TYPES; i3++) {
            this.mPhoneDataConnectionsTimer[i3] = new StopwatchTimer(this.mClock, null, (-300) - i3, null, this.mOnBatteryTimeBase);
        }
        for (int i4 = 0; i4 < 10; i4++) {
            this.mNetworkByteActivityCounters[i4] = new LongSamplingCounter(this.mOnBatteryTimeBase);
            this.mNetworkPacketActivityCounters[i4] = new LongSamplingCounter(this.mOnBatteryTimeBase);
        }
        this.mWifiActivity = new ControllerActivityCounterImpl(this.mClock, this.mOnBatteryTimeBase, 1);
        this.mBluetoothActivity = new ControllerActivityCounterImpl(this.mClock, this.mOnBatteryTimeBase, 1);
        this.mModemActivity = new ControllerActivityCounterImpl(this.mClock, this.mOnBatteryTimeBase, ModemActivityInfo.getNumTxPowerLevels());
        this.mMobileRadioActiveTimer = new StopwatchTimer(this.mClock, null, -400, null, this.mOnBatteryTimeBase);
        this.mMobileRadioActivePerAppTimer = new StopwatchTimer(this.mClock, null, -401, null, this.mOnBatteryTimeBase);
        this.mMobileRadioActiveAdjustedTime = new LongSamplingCounter(this.mOnBatteryTimeBase);
        this.mMobileRadioActiveUnknownTime = new LongSamplingCounter(this.mOnBatteryTimeBase);
        this.mMobileRadioActiveUnknownCount = new LongSamplingCounter(this.mOnBatteryTimeBase);
        this.mWifiMulticastWakelockTimer = new StopwatchTimer(this.mClock, null, 23, null, this.mOnBatteryTimeBase);
        this.mWifiOnTimer = new StopwatchTimer(this.mClock, null, -4, null, this.mOnBatteryTimeBase);
        this.mGlobalWifiRunningTimer = new StopwatchTimer(this.mClock, null, -5, null, this.mOnBatteryTimeBase);
        for (int i5 = 0; i5 < 8; i5++) {
            this.mWifiStateTimer[i5] = new StopwatchTimer(this.mClock, null, (-600) - i5, null, this.mOnBatteryTimeBase);
        }
        for (int i6 = 0; i6 < 13; i6++) {
            this.mWifiSupplStateTimer[i6] = new StopwatchTimer(this.mClock, null, (-700) - i6, null, this.mOnBatteryTimeBase);
        }
        for (int i7 = 0; i7 < 5; i7++) {
            this.mWifiSignalStrengthsTimer[i7] = new StopwatchTimer(this.mClock, null, (-800) - i7, null, this.mOnBatteryTimeBase);
        }
        this.mWifiActiveTimer = new StopwatchTimer(this.mClock, null, -900, null, this.mOnBatteryTimeBase);
        int i8 = 0;
        while (true) {
            StopwatchTimer[] stopwatchTimerArr = this.mGpsSignalQualityTimer;
            if (i8 >= stopwatchTimerArr.length) {
                this.mAudioOnTimer = new StopwatchTimer(this.mClock, null, -7, null, this.mOnBatteryTimeBase);
                this.mVideoOnTimer = new StopwatchTimer(this.mClock, null, -8, null, this.mOnBatteryTimeBase);
                this.mFlashlightOnTimer = new StopwatchTimer(this.mClock, null, -9, null, this.mOnBatteryTimeBase);
                this.mCameraOnTimer = new StopwatchTimer(this.mClock, null, -13, null, this.mOnBatteryTimeBase);
                this.mBluetoothScanTimer = new StopwatchTimer(this.mClock, null, -14, null, this.mOnBatteryTimeBase);
                this.mDischargeScreenOffCounter = new LongSamplingCounter(this.mOnBatteryScreenOffTimeBase);
                this.mDischargeScreenDozeCounter = new LongSamplingCounter(this.mOnBatteryTimeBase);
                this.mDischargeLightDozeCounter = new LongSamplingCounter(this.mOnBatteryTimeBase);
                this.mDischargeDeepDozeCounter = new LongSamplingCounter(this.mOnBatteryTimeBase);
                this.mDischargeCounter = new LongSamplingCounter(this.mOnBatteryTimeBase);
                this.mDischargeStartLevel = 0;
                this.mDischargeUnplugLevel = 0;
                this.mDischargePlugLevel = -1;
                this.mDischargeCurrentLevel = 0;
                this.mCurrentBatteryLevel = 0;
                return;
            }
            stopwatchTimerArr[i8] = new StopwatchTimer(this.mClock, null, (-1000) - i8, null, this.mOnBatteryTimeBase);
            i8++;
        }
    }

    public BatteryStatsImpl(Parcel p) {
        this(Clock.SYSTEM_CLOCK, p);
    }

    public BatteryStatsImpl(Clock clock, Parcel p) {
        this.mKernelWakelockReader = new KernelWakelockReader();
        this.mTmpWakelockStats = new KernelWakelockStats();
        this.mSystemServerCpuThreadReader = SystemServerCpuThreadReader.create();
        this.mKernelMemoryBandwidthStats = new KernelMemoryBandwidthStats();
        this.mKernelMemoryStats = new LongSparseArray<>();
        this.mPerProcStateCpuTimesAvailable = true;
        this.mCpuTimeReadsTrackingStartTimeMs = SystemClock.uptimeMillis();
        this.mTmpRpmStats = null;
        this.mLastRpmStatsUpdateTimeMs = -1000L;
        this.mTmpRailStats = new RailStats();
        this.mPendingRemovedUids = new LinkedList();
        this.mDeferSetCharging = new Runnable() { // from class: com.android.internal.os.BatteryStatsImpl.2
            @Override // java.lang.Runnable
            public void run() {
                synchronized (BatteryStatsImpl.this) {
                    if (BatteryStatsImpl.this.mOnBattery) {
                        return;
                    }
                    boolean changed = BatteryStatsImpl.this.setChargingLocked(true);
                    if (changed) {
                        long uptimeMs = BatteryStatsImpl.this.mClock.uptimeMillis();
                        long elapsedRealtimeMs = BatteryStatsImpl.this.mClock.elapsedRealtime();
                        BatteryStatsImpl.this.addHistoryRecordLocked(elapsedRealtimeMs, uptimeMs);
                    }
                }
            }
        };
        this.mExternalSync = null;
        this.mUserInfoProvider = null;
        this.mIsolatedUids = new SparseIntArray();
        this.mIsolatedUidRefCounts = new SparseIntArray();
        this.mUidStats = new SparseArray<>();
        this.mPartialTimers = new ArrayList<>();
        this.mFullTimers = new ArrayList<>();
        this.mWindowTimers = new ArrayList<>();
        this.mDrawTimers = new ArrayList<>();
        this.mSensorTimers = new SparseArray<>();
        this.mWifiRunningTimers = new ArrayList<>();
        this.mFullWifiLockTimers = new ArrayList<>();
        this.mWifiMulticastTimers = new ArrayList<>();
        this.mWifiScanTimers = new ArrayList<>();
        this.mWifiBatchedScanTimers = new SparseArray<>();
        this.mAudioTurnedOnTimers = new ArrayList<>();
        this.mVideoTurnedOnTimers = new ArrayList<>();
        this.mFlashlightTurnedOnTimers = new ArrayList<>();
        this.mCameraTurnedOnTimers = new ArrayList<>();
        this.mBluetoothScanOnTimers = new ArrayList<>();
        this.mLastPartialTimers = new ArrayList<>();
        this.mOnBatteryTimeBase = new TimeBase(true);
        this.mOnBatteryScreenOffTimeBase = new TimeBase(true);
        this.mActiveEvents = new BatteryStats.HistoryEventTracker();
        this.mHaveBatteryLevel = false;
        this.mRecordingHistory = false;
        this.mHistoryTagPool = new HashMap<>();
        Parcel obtain = Parcel.obtain();
        this.mHistoryBuffer = obtain;
        this.mHistoryLastWritten = new BatteryStats.HistoryItem();
        this.mHistoryLastLastWritten = new BatteryStats.HistoryItem();
        this.mHistoryAddTmp = new BatteryStats.HistoryItem();
        this.mNextHistoryTagIdx = 0;
        this.mNumHistoryTagChars = 0;
        this.mHistoryBufferLastPos = -1;
        this.mActiveHistoryStates = -1;
        this.mActiveHistoryStates2 = -1;
        this.mLastHistoryElapsedRealtimeMs = 0L;
        this.mTrackRunningHistoryElapsedRealtimeMs = 0L;
        this.mTrackRunningHistoryUptimeMs = 0L;
        this.mHistoryCur = new BatteryStats.HistoryItem();
        this.mLastHistoryStepDetails = null;
        this.mLastHistoryStepLevel = (byte) 0;
        this.mCurHistoryStepDetails = new BatteryStats.HistoryStepDetails();
        this.mReadHistoryStepDetails = new BatteryStats.HistoryStepDetails();
        this.mTmpHistoryStepDetails = new BatteryStats.HistoryStepDetails();
        this.mIgnoreNextExternalStats = false;
        this.mScreenState = 0;
        this.mScreenBrightnessBin = -1;
        this.mScreenBrightnessTimer = new StopwatchTimer[5];
        this.mDisplayMismatchWtfCount = 0;
        this.mUsbDataState = 0;
        this.mGpsSignalQualityBin = -1;
        this.mGpsSignalQualityTimer = new StopwatchTimer[2];
        this.mPhoneSignalStrengthBin = -1;
        this.mPhoneSignalStrengthBinRaw = -1;
        this.mPhoneSignalStrengthsTimer = new StopwatchTimer[CellSignalStrength.getNumSignalStrengthLevels() + 1];
        this.mPhoneDataConnectionType = -1;
        this.mPhoneDataConnectionsTimer = new StopwatchTimer[NUM_DATA_CONNECTION_TYPES];
        this.mActiveRat = 0;
        this.mPerRatBatteryStats = new RadioAccessTechnologyBatteryStats[3];
        this.mNetworkByteActivityCounters = new LongSamplingCounter[10];
        this.mNetworkPacketActivityCounters = new LongSamplingCounter[10];
        this.mHasWifiReporting = false;
        this.mHasBluetoothReporting = false;
        this.mHasModemReporting = false;
        this.mWifiState = -1;
        this.mWifiStateTimer = new StopwatchTimer[8];
        this.mWifiSupplState = -1;
        this.mWifiSupplStateTimer = new StopwatchTimer[13];
        this.mWifiSignalStrengthBin = -1;
        this.mWifiSignalStrengthsTimer = new StopwatchTimer[5];
        this.mMobileRadioPowerState = 1;
        this.mWifiRadioPowerState = 1;
        this.mBluetoothPowerCalculator = null;
        this.mCpuPowerCalculator = null;
        this.mMobileRadioPowerCalculator = null;
        this.mWifiPowerCalculator = null;
        this.mCharging = true;
        this.mInitStepMode = 0;
        this.mCurStepMode = 0;
        this.mModStepMode = 0;
        this.mDischargeStepTracker = new BatteryStats.LevelStepTracker(200);
        this.mDailyDischargeStepTracker = new BatteryStats.LevelStepTracker(400);
        this.mChargeStepTracker = new BatteryStats.LevelStepTracker(200);
        this.mDailyChargeStepTracker = new BatteryStats.LevelStepTracker(400);
        this.mDailyStartTimeMs = 0L;
        this.mNextMinDailyDeadlineMs = 0L;
        this.mNextMaxDailyDeadlineMs = 0L;
        this.mDailyItems = new ArrayList<>();
        this.mLastWriteTimeMs = 0L;
        this.mPhoneServiceState = -1;
        this.mPhoneServiceStateRaw = -1;
        this.mPhoneSimStateRaw = -1;
        this.mBatteryVoltageMv = -1;
        this.mEstimatedBatteryCapacityMah = -1;
        this.mLastLearnedBatteryCapacityUah = -1;
        this.mMinLearnedBatteryCapacityUah = -1;
        this.mMaxLearnedBatteryCapacityUah = -1;
        this.mBatteryTimeToFullSeconds = -1L;
        this.mRpmStats = new HashMap<>();
        this.mScreenOffRpmStats = new HashMap<>();
        this.mKernelWakelockStats = new HashMap<>();
        this.mLastWakeupReason = null;
        this.mLastWakeupUptimeMs = 0L;
        this.mWakeupReasonStats = new HashMap<>();
        this.mChangedStates = 0;
        this.mChangedStates2 = 0;
        this.mInitialAcquireWakeUid = -1;
        this.mWifiFullLockNesting = 0;
        this.mWifiScanNesting = 0;
        this.mWifiMulticastNesting = 0;
        this.mWifiNetworkLock = new Object();
        this.mWifiIfaces = EmptyArray.STRING;
        this.mLastWifiNetworkStats = new NetworkStats(0L, -1);
        this.mModemNetworkLock = new Object();
        this.mModemIfaces = EmptyArray.STRING;
        this.mLastModemNetworkStats = new NetworkStats(0L, -1);
        this.mLastModemActivityInfo = null;
        this.mLastBluetoothActivityInfo = new BluetoothActivityInfoCache();
        this.mWriteLock = new ReentrantLock();
        init(clock);
        this.mStatsFile = null;
        this.mCheckinFile = null;
        this.mDailyFile = null;
        this.mHandler = null;
        this.mExternalSync = null;
        this.mConstants = new Constants(null);
        clearHistoryLocked();
        this.mBatteryStatsHistory = new BatteryStatsHistory(obtain);
        readFromParcel(p);
        this.mPlatformIdleStateCallback = null;
        this.mMeasuredEnergyRetriever = null;
    }

    public void setPowerProfileLocked(PowerProfile profile) {
        this.mPowerProfile = profile;
        int numClusters = profile.getNumCpuClusters();
        this.mKernelCpuSpeedReaders = new KernelCpuSpeedReader[numClusters];
        int firstCpuOfCluster = 0;
        for (int i = 0; i < numClusters; i++) {
            int numSpeedSteps = this.mPowerProfile.getNumSpeedStepsInCpuCluster(i);
            this.mKernelCpuSpeedReaders[i] = new KernelCpuSpeedReader(firstCpuOfCluster, numSpeedSteps);
            firstCpuOfCluster += this.mPowerProfile.getNumCoresInCpuCluster(i);
        }
        int i2 = this.mEstimatedBatteryCapacityMah;
        if (i2 == -1) {
            this.mEstimatedBatteryCapacityMah = (int) this.mPowerProfile.getBatteryCapacity();
        }
        setDisplayCountLocked(this.mPowerProfile.getNumDisplays());
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public PowerProfile getPowerProfile() {
        return this.mPowerProfile;
    }

    public void startTrackingSystemServerCpuTime() {
        this.mSystemServerCpuThreadReader.startTrackingThreadCpuTime();
    }

    public SystemServerCpuThreadReader.SystemServiceCpuThreadTimes getSystemServiceCpuThreadTimes() {
        return this.mSystemServerCpuThreadReader.readAbsolute();
    }

    public void setCallback(BatteryCallback cb) {
        this.mCallback = cb;
    }

    public void setRadioScanningTimeoutLocked(long timeoutUs) {
        StopwatchTimer stopwatchTimer = this.mPhoneSignalScanningTimer;
        if (stopwatchTimer != null) {
            stopwatchTimer.setTimeout(timeoutUs);
        }
    }

    public void setExternalStatsSyncLocked(ExternalStatsSync sync) {
        this.mExternalSync = sync;
    }

    public void setDisplayCountLocked(int numDisplays) {
        this.mPerDisplayBatteryStats = new DisplayBatteryStats[numDisplays];
        for (int i = 0; i < numDisplays; i++) {
            this.mPerDisplayBatteryStats[i] = new DisplayBatteryStats(this.mClock, this.mOnBatteryTimeBase);
        }
    }

    public void updateDailyDeadlineLocked() {
        long currentTimeMs = this.mClock.currentTimeMillis();
        this.mDailyStartTimeMs = currentTimeMs;
        Calendar calDeadline = Calendar.getInstance();
        calDeadline.setTimeInMillis(currentTimeMs);
        calDeadline.set(6, calDeadline.get(6) + 1);
        calDeadline.set(14, 0);
        calDeadline.set(13, 0);
        calDeadline.set(12, 0);
        calDeadline.set(11, 1);
        this.mNextMinDailyDeadlineMs = calDeadline.getTimeInMillis();
        calDeadline.set(11, 3);
        this.mNextMaxDailyDeadlineMs = calDeadline.getTimeInMillis();
    }

    public void recordDailyStatsIfNeededLocked(boolean settled, long currentTimeMs) {
        if (currentTimeMs >= this.mNextMaxDailyDeadlineMs) {
            recordDailyStatsLocked();
        } else if (settled && currentTimeMs >= this.mNextMinDailyDeadlineMs) {
            recordDailyStatsLocked();
        } else if (currentTimeMs < this.mDailyStartTimeMs - 86400000) {
            recordDailyStatsLocked();
        }
    }

    public void recordDailyStatsLocked() {
        BatteryStats.DailyItem item = new BatteryStats.DailyItem();
        item.mStartTime = this.mDailyStartTimeMs;
        item.mEndTime = this.mClock.currentTimeMillis();
        boolean hasData = false;
        if (this.mDailyDischargeStepTracker.mNumStepDurations > 0) {
            hasData = true;
            item.mDischargeSteps = new BatteryStats.LevelStepTracker(this.mDailyDischargeStepTracker.mNumStepDurations, this.mDailyDischargeStepTracker.mStepDurations);
        }
        if (this.mDailyChargeStepTracker.mNumStepDurations > 0) {
            hasData = true;
            item.mChargeSteps = new BatteryStats.LevelStepTracker(this.mDailyChargeStepTracker.mNumStepDurations, this.mDailyChargeStepTracker.mStepDurations);
        }
        ArrayList<BatteryStats.PackageChange> arrayList = this.mDailyPackageChanges;
        if (arrayList != null) {
            hasData = true;
            item.mPackageChanges = arrayList;
            this.mDailyPackageChanges = null;
        }
        this.mDailyDischargeStepTracker.init();
        this.mDailyChargeStepTracker.init();
        updateDailyDeadlineLocked();
        if (hasData) {
            long startTimeMs = SystemClock.uptimeMillis();
            this.mDailyItems.add(item);
            while (this.mDailyItems.size() > 10) {
                this.mDailyItems.remove(0);
            }
            final ByteArrayOutputStream memStream = new ByteArrayOutputStream();
            try {
                TypedXmlSerializer out = Xml.resolveSerializer(memStream);
                writeDailyItemsLocked(out);
                final long initialTimeMs = SystemClock.uptimeMillis() - startTimeMs;
                BackgroundThread.getHandler().post(new Runnable() { // from class: com.android.internal.os.BatteryStatsImpl.4
                    @Override // java.lang.Runnable
                    public void run() {
                        synchronized (BatteryStatsImpl.this.mCheckinFile) {
                            long startTimeMs2 = SystemClock.uptimeMillis();
                            FileOutputStream stream = null;
                            try {
                                stream = BatteryStatsImpl.this.mDailyFile.startWrite();
                                memStream.writeTo(stream);
                                stream.flush();
                                BatteryStatsImpl.this.mDailyFile.finishWrite(stream);
                                EventLogTags.writeCommitSysConfigFile("batterystats-daily", (initialTimeMs + SystemClock.uptimeMillis()) - startTimeMs2);
                            } catch (IOException e) {
                                Slog.w("BatteryStats", "Error writing battery daily items", e);
                                BatteryStatsImpl.this.mDailyFile.failWrite(stream);
                            }
                        }
                    }
                });
            } catch (IOException e) {
            }
        }
    }

    private void writeDailyItemsLocked(TypedXmlSerializer out) throws IOException {
        StringBuilder sb = new StringBuilder(64);
        out.startDocument(null, true);
        out.startTag(null, "daily-items");
        for (int i = 0; i < this.mDailyItems.size(); i++) {
            BatteryStats.DailyItem dit = this.mDailyItems.get(i);
            out.startTag(null, ImsConfig.EXTRA_CHANGED_ITEM);
            out.attributeLong(null, "start", dit.mStartTime);
            out.attributeLong(null, "end", dit.mEndTime);
            writeDailyLevelSteps(out, "dis", dit.mDischargeSteps, sb);
            writeDailyLevelSteps(out, "chg", dit.mChargeSteps, sb);
            if (dit.mPackageChanges != null) {
                for (int j = 0; j < dit.mPackageChanges.size(); j++) {
                    BatteryStats.PackageChange pc = dit.mPackageChanges.get(j);
                    if (pc.mUpdate) {
                        out.startTag(null, "upd");
                        out.attribute(null, "pkg", pc.mPackageName);
                        out.attributeLong(null, "ver", pc.mVersionCode);
                        out.endTag(null, "upd");
                    } else {
                        out.startTag(null, "rem");
                        out.attribute(null, "pkg", pc.mPackageName);
                        out.endTag(null, "rem");
                    }
                }
            }
            out.endTag(null, ImsConfig.EXTRA_CHANGED_ITEM);
        }
        out.endTag(null, "daily-items");
        out.endDocument();
    }

    private void writeDailyLevelSteps(TypedXmlSerializer out, String tag, BatteryStats.LevelStepTracker steps, StringBuilder tmpBuilder) throws IOException {
        if (steps != null) {
            out.startTag(null, tag);
            out.attributeInt(null, "n", steps.mNumStepDurations);
            for (int i = 0; i < steps.mNumStepDurations; i++) {
                out.startTag(null, XmlTags.TAG_SESSION);
                tmpBuilder.setLength(0);
                steps.encodeEntryAt(i, tmpBuilder);
                out.attribute(null, "v", tmpBuilder.toString());
                out.endTag(null, XmlTags.TAG_SESSION);
            }
            out.endTag(null, tag);
        }
    }

    /* JADX WARN: Unsupported multi-entry loop pattern (BACK_EDGE: B:7:0x0035 -> B:17:0x0043). Please submit an issue!!! */
    public void readDailyStatsLocked() {
        Slog.d(TAG, "Reading daily items from " + this.mDailyFile.getBaseFile());
        this.mDailyItems.clear();
        try {
            FileInputStream stream = this.mDailyFile.openRead();
            try {
                try {
                    TypedXmlPullParser parser = Xml.resolvePullParser(stream);
                    readDailyItemsLocked(parser);
                    stream.close();
                } catch (IOException e) {
                }
            } catch (IOException e2) {
                stream.close();
            } catch (Throwable th) {
                try {
                    stream.close();
                } catch (IOException e3) {
                }
                throw th;
            }
        } catch (FileNotFoundException e4) {
        }
    }

    private void readDailyItemsLocked(TypedXmlPullParser parser) {
        int type;
        while (true) {
            try {
                type = parser.next();
                if (type == 2 || type == 1) {
                    break;
                }
            } catch (IOException e) {
                Slog.w(TAG, "Failed parsing daily " + e);
                return;
            } catch (IllegalStateException e2) {
                Slog.w(TAG, "Failed parsing daily " + e2);
                return;
            } catch (IndexOutOfBoundsException e3) {
                Slog.w(TAG, "Failed parsing daily " + e3);
                return;
            } catch (NullPointerException e4) {
                Slog.w(TAG, "Failed parsing daily " + e4);
                return;
            } catch (NumberFormatException e5) {
                Slog.w(TAG, "Failed parsing daily " + e5);
                return;
            } catch (XmlPullParserException e6) {
                Slog.w(TAG, "Failed parsing daily " + e6);
                return;
            }
        }
        if (type != 2) {
            throw new IllegalStateException("no start tag found");
        }
        int outerDepth = parser.getDepth();
        while (true) {
            int type2 = parser.next();
            if (type2 != 1) {
                if (type2 != 3 || parser.getDepth() > outerDepth) {
                    if (type2 != 3 && type2 != 4) {
                        String tagName = parser.getName();
                        if (tagName.equals(ImsConfig.EXTRA_CHANGED_ITEM)) {
                            readDailyItemTagLocked(parser);
                        } else {
                            Slog.w(TAG, "Unknown element under <daily-items>: " + parser.getName());
                            XmlUtils.skipCurrentTag(parser);
                        }
                    }
                } else {
                    return;
                }
            } else {
                return;
            }
        }
    }

    void readDailyItemTagLocked(TypedXmlPullParser parser) throws NumberFormatException, XmlPullParserException, IOException {
        BatteryStats.DailyItem dit = new BatteryStats.DailyItem();
        dit.mStartTime = parser.getAttributeLong(null, "start", 0L);
        dit.mEndTime = parser.getAttributeLong(null, "end", 0L);
        int outerDepth = parser.getDepth();
        while (true) {
            int type = parser.next();
            if (type == 1 || (type == 3 && parser.getDepth() <= outerDepth)) {
                break;
            } else if (type != 3 && type != 4) {
                String tagName = parser.getName();
                if (tagName.equals("dis")) {
                    readDailyItemTagDetailsLocked(parser, dit, false, "dis");
                } else if (tagName.equals("chg")) {
                    readDailyItemTagDetailsLocked(parser, dit, true, "chg");
                } else if (tagName.equals("upd")) {
                    if (dit.mPackageChanges == null) {
                        dit.mPackageChanges = new ArrayList<>();
                    }
                    BatteryStats.PackageChange pc = new BatteryStats.PackageChange();
                    pc.mUpdate = true;
                    pc.mPackageName = parser.getAttributeValue(null, "pkg");
                    pc.mVersionCode = parser.getAttributeLong(null, "ver", 0L);
                    dit.mPackageChanges.add(pc);
                    XmlUtils.skipCurrentTag(parser);
                } else if (tagName.equals("rem")) {
                    if (dit.mPackageChanges == null) {
                        dit.mPackageChanges = new ArrayList<>();
                    }
                    BatteryStats.PackageChange pc2 = new BatteryStats.PackageChange();
                    pc2.mUpdate = false;
                    pc2.mPackageName = parser.getAttributeValue(null, "pkg");
                    dit.mPackageChanges.add(pc2);
                    XmlUtils.skipCurrentTag(parser);
                } else {
                    Slog.w(TAG, "Unknown element under <item>: " + parser.getName());
                    XmlUtils.skipCurrentTag(parser);
                }
            }
        }
        this.mDailyItems.add(dit);
    }

    void readDailyItemTagDetailsLocked(TypedXmlPullParser parser, BatteryStats.DailyItem dit, boolean isCharge, String tag) throws NumberFormatException, XmlPullParserException, IOException {
        String valueAttr;
        int num = parser.getAttributeInt(null, "n", -1);
        if (num == -1) {
            Slog.w(TAG, "Missing 'n' attribute at " + parser.getPositionDescription());
            XmlUtils.skipCurrentTag(parser);
            return;
        }
        BatteryStats.LevelStepTracker steps = new BatteryStats.LevelStepTracker(num);
        if (isCharge) {
            dit.mChargeSteps = steps;
        } else {
            dit.mDischargeSteps = steps;
        }
        int i = 0;
        int outerDepth = parser.getDepth();
        while (true) {
            int type = parser.next();
            if (type == 1 || (type == 3 && parser.getDepth() <= outerDepth)) {
                break;
            } else if (type != 3 && type != 4) {
                String tagName = parser.getName();
                if (XmlTags.TAG_SESSION.equals(tagName)) {
                    if (i < num && (valueAttr = parser.getAttributeValue(null, "v")) != null) {
                        steps.decodeEntryAt(i, valueAttr);
                        i++;
                    }
                } else {
                    Slog.w(TAG, "Unknown element under <" + tag + ">: " + parser.getName());
                    XmlUtils.skipCurrentTag(parser);
                }
            }
        }
        steps.mNumStepDurations = i;
    }

    @Override // android.os.BatteryStats
    public BatteryStats.DailyItem getDailyItemLocked(int daysAgo) {
        int index = (this.mDailyItems.size() - 1) - daysAgo;
        if (index >= 0) {
            return this.mDailyItems.get(index);
        }
        return null;
    }

    @Override // android.os.BatteryStats
    public long getCurrentDailyStartTime() {
        return this.mDailyStartTimeMs;
    }

    @Override // android.os.BatteryStats
    public long getNextMinDailyDeadline() {
        return this.mNextMinDailyDeadlineMs;
    }

    @Override // android.os.BatteryStats
    public long getNextMaxDailyDeadline() {
        return this.mNextMaxDailyDeadlineMs;
    }

    @Override // android.os.BatteryStats
    public int getHistoryTotalSize() {
        return this.mConstants.MAX_HISTORY_BUFFER * this.mConstants.MAX_HISTORY_FILES;
    }

    @Override // android.os.BatteryStats
    public int getHistoryUsedSize() {
        return this.mBatteryStatsHistory.getHistoryUsedSize();
    }

    @Override // android.os.BatteryStats
    public boolean startIteratingHistoryLocked() {
        this.mBatteryStatsHistoryIterator = createBatteryStatsHistoryIterator();
        return true;
    }

    public BatteryStatsHistoryIterator createBatteryStatsHistoryIterator() {
        return new BatteryStatsHistoryIterator(this.mBatteryStatsHistory);
    }

    @Override // android.os.BatteryStats
    public int getHistoryStringPoolSize() {
        return this.mHistoryTagPool.size();
    }

    @Override // android.os.BatteryStats
    public int getHistoryStringPoolBytes() {
        return this.mNumHistoryTagChars;
    }

    @Override // android.os.BatteryStats
    public String getHistoryTagPoolString(int index) {
        ensureHistoryTagArray();
        BatteryStats.HistoryTag historyTag = this.mHistoryTags.get(index);
        if (historyTag != null) {
            return historyTag.string;
        }
        return null;
    }

    @Override // android.os.BatteryStats
    public int getHistoryTagPoolUid(int index) {
        ensureHistoryTagArray();
        BatteryStats.HistoryTag historyTag = this.mHistoryTags.get(index);
        if (historyTag != null) {
            return historyTag.uid;
        }
        return -1;
    }

    private void ensureHistoryTagArray() {
        if (this.mHistoryTags != null) {
            return;
        }
        this.mHistoryTags = new SparseArray<>(this.mHistoryTagPool.size());
        for (Map.Entry<BatteryStats.HistoryTag, Integer> entry : this.mHistoryTagPool.entrySet()) {
            this.mHistoryTags.put(entry.getValue().intValue() & (-32769), entry.getKey());
        }
    }

    @Override // android.os.BatteryStats
    public boolean getNextHistoryLocked(BatteryStats.HistoryItem out) {
        return this.mBatteryStatsHistoryIterator.next(out);
    }

    @Override // android.os.BatteryStats
    public void finishIteratingHistoryLocked() {
        this.mBatteryStatsHistoryIterator = null;
    }

    @Override // android.os.BatteryStats
    public long getHistoryBaseTime() {
        return this.mHistoryBaseTimeMs;
    }

    @Override // android.os.BatteryStats
    public int getStartCount() {
        return this.mStartCount;
    }

    public boolean isOnBattery() {
        return this.mOnBattery;
    }

    public boolean isCharging() {
        return this.mCharging;
    }

    void initTimes(long uptimeUs, long realtimeUs) {
        this.mStartClockTimeMs = this.mClock.currentTimeMillis();
        this.mOnBatteryTimeBase.init(uptimeUs, realtimeUs);
        this.mOnBatteryScreenOffTimeBase.init(uptimeUs, realtimeUs);
        this.mRealtimeUs = 0L;
        this.mUptimeUs = 0L;
        this.mRealtimeStartUs = realtimeUs;
        this.mUptimeStartUs = uptimeUs;
    }

    void initDischarge(long elapsedRealtimeUs) {
        this.mLowDischargeAmountSinceCharge = 0;
        this.mHighDischargeAmountSinceCharge = 0;
        this.mDischargeAmountScreenOn = 0;
        this.mDischargeAmountScreenOnSinceCharge = 0;
        this.mDischargeAmountScreenOff = 0;
        this.mDischargeAmountScreenOffSinceCharge = 0;
        this.mDischargeAmountScreenDoze = 0;
        this.mDischargeAmountScreenDozeSinceCharge = 0;
        this.mDischargeStepTracker.init();
        this.mChargeStepTracker.init();
        this.mDischargeScreenOffCounter.reset(false, elapsedRealtimeUs);
        this.mDischargeScreenDozeCounter.reset(false, elapsedRealtimeUs);
        this.mDischargeLightDozeCounter.reset(false, elapsedRealtimeUs);
        this.mDischargeDeepDozeCounter.reset(false, elapsedRealtimeUs);
        this.mDischargeCounter.reset(false, elapsedRealtimeUs);
    }

    public void setBatteryResetListener(BatteryResetListener batteryResetListener) {
        this.mBatteryResetListener = batteryResetListener;
    }

    public void resetAllStatsCmdLocked() {
        long mSecUptime = this.mClock.uptimeMillis();
        long uptimeUs = mSecUptime * 1000;
        long mSecRealtime = this.mClock.elapsedRealtime();
        long realtimeUs = mSecRealtime * 1000;
        resetAllStatsLocked(mSecUptime, mSecRealtime, 2);
        this.mDischargeStartLevel = this.mHistoryCur.batteryLevel;
        pullPendingStateUpdatesLocked();
        addHistoryRecordLocked(mSecRealtime, mSecUptime);
        byte b = this.mHistoryCur.batteryLevel;
        this.mCurrentBatteryLevel = b;
        this.mDischargePlugLevel = b;
        this.mDischargeUnplugLevel = b;
        this.mDischargeCurrentLevel = b;
        this.mOnBatteryTimeBase.reset(uptimeUs, realtimeUs);
        this.mOnBatteryScreenOffTimeBase.reset(uptimeUs, realtimeUs);
        if ((this.mHistoryCur.states & 524288) == 0) {
            if (Display.isOnState(this.mScreenState)) {
                this.mDischargeScreenOnUnplugLevel = this.mHistoryCur.batteryLevel;
                this.mDischargeScreenDozeUnplugLevel = 0;
                this.mDischargeScreenOffUnplugLevel = 0;
            } else if (Display.isDozeState(this.mScreenState)) {
                this.mDischargeScreenOnUnplugLevel = 0;
                this.mDischargeScreenDozeUnplugLevel = this.mHistoryCur.batteryLevel;
                this.mDischargeScreenOffUnplugLevel = 0;
            } else {
                this.mDischargeScreenOnUnplugLevel = 0;
                this.mDischargeScreenDozeUnplugLevel = 0;
                this.mDischargeScreenOffUnplugLevel = this.mHistoryCur.batteryLevel;
            }
            this.mDischargeAmountScreenOn = 0;
            this.mDischargeAmountScreenOff = 0;
            this.mDischargeAmountScreenDoze = 0;
        }
        initActiveHistoryEventsLocked(mSecRealtime, mSecUptime);
    }

    /* JADX DEBUG: Multi-variable search result rejected for r2v1, resolved type: boolean */
    /* JADX WARN: Multi-variable type inference failed */
    private void resetAllStatsLocked(long uptimeMillis, long elapsedRealtimeMillis, int resetReason) {
        int i;
        int i2;
        BatteryResetListener batteryResetListener = this.mBatteryResetListener;
        if (batteryResetListener != null) {
            batteryResetListener.prepareForBatteryStatsReset(resetReason);
        }
        long uptimeUs = uptimeMillis * 1000;
        long elapsedRealtimeUs = elapsedRealtimeMillis * 1000;
        this.mStartCount = 0;
        initTimes(uptimeUs, elapsedRealtimeUs);
        this.mScreenOnTimer.reset(false, elapsedRealtimeUs);
        this.mScreenDozeTimer.reset(false, elapsedRealtimeUs);
        for (int i3 = 0; i3 < 5; i3++) {
            this.mScreenBrightnessTimer[i3].reset(false, elapsedRealtimeUs);
        }
        int numDisplays = this.mPerDisplayBatteryStats.length;
        for (int i4 = 0; i4 < numDisplays; i4++) {
            this.mPerDisplayBatteryStats[i4].reset(elapsedRealtimeUs);
        }
        PowerProfile powerProfile = this.mPowerProfile;
        if (powerProfile != null) {
            this.mEstimatedBatteryCapacityMah = (int) powerProfile.getBatteryCapacity();
        } else {
            this.mEstimatedBatteryCapacityMah = -1;
        }
        this.mLastLearnedBatteryCapacityUah = -1;
        this.mMinLearnedBatteryCapacityUah = -1;
        this.mMaxLearnedBatteryCapacityUah = -1;
        this.mInteractiveTimer.reset(false, elapsedRealtimeUs);
        this.mPowerSaveModeEnabledTimer.reset(false, elapsedRealtimeUs);
        this.mLastIdleTimeStartMs = elapsedRealtimeMillis;
        long j = 0;
        this.mLongestLightIdleTimeMs = 0L;
        this.mLongestFullIdleTimeMs = 0L;
        this.mDeviceIdleModeLightTimer.reset(false, elapsedRealtimeUs);
        this.mDeviceIdleModeFullTimer.reset(false, elapsedRealtimeUs);
        this.mDeviceLightIdlingTimer.reset(false, elapsedRealtimeUs);
        this.mDeviceIdlingTimer.reset(false, elapsedRealtimeUs);
        this.mPhoneOnTimer.reset(false, elapsedRealtimeUs);
        this.mAudioOnTimer.reset(false, elapsedRealtimeUs);
        this.mVideoOnTimer.reset(false, elapsedRealtimeUs);
        this.mFlashlightOnTimer.reset(false, elapsedRealtimeUs);
        this.mCameraOnTimer.reset(false, elapsedRealtimeUs);
        this.mBluetoothScanTimer.reset(false, elapsedRealtimeUs);
        int i5 = 0;
        while (true) {
            i = 1;
            if (i5 >= CellSignalStrength.getNumSignalStrengthLevels() + 1) {
                break;
            }
            this.mPhoneSignalStrengthsTimer[i5].reset(false, elapsedRealtimeUs);
            i5++;
        }
        this.mPhoneSignalScanningTimer.reset(false, elapsedRealtimeUs);
        for (int i6 = 0; i6 < NUM_DATA_CONNECTION_TYPES; i6++) {
            this.mPhoneDataConnectionsTimer[i6].reset(false, elapsedRealtimeUs);
        }
        for (int i7 = 0; i7 < 10; i7++) {
            this.mNetworkByteActivityCounters[i7].reset(false, elapsedRealtimeUs);
            this.mNetworkPacketActivityCounters[i7].reset(false, elapsedRealtimeUs);
        }
        for (int i8 = 0; i8 < 3; i8++) {
            RadioAccessTechnologyBatteryStats stats = this.mPerRatBatteryStats[i8];
            if (stats != null) {
                stats.reset(elapsedRealtimeUs);
            }
        }
        this.mMobileRadioActiveTimer.reset(false, elapsedRealtimeUs);
        this.mMobileRadioActivePerAppTimer.reset(false, elapsedRealtimeUs);
        this.mMobileRadioActiveAdjustedTime.reset(false, elapsedRealtimeUs);
        this.mMobileRadioActiveUnknownTime.reset(false, elapsedRealtimeUs);
        this.mMobileRadioActiveUnknownCount.reset(false, elapsedRealtimeUs);
        this.mWifiOnTimer.reset(false, elapsedRealtimeUs);
        this.mGlobalWifiRunningTimer.reset(false, elapsedRealtimeUs);
        for (int i9 = 0; i9 < 8; i9++) {
            this.mWifiStateTimer[i9].reset(false, elapsedRealtimeUs);
        }
        for (int i10 = 0; i10 < 13; i10++) {
            this.mWifiSupplStateTimer[i10].reset(false, elapsedRealtimeUs);
        }
        for (int i11 = 0; i11 < 5; i11++) {
            this.mWifiSignalStrengthsTimer[i11].reset(false, elapsedRealtimeUs);
        }
        this.mWifiMulticastWakelockTimer.reset(false, elapsedRealtimeUs);
        this.mWifiActiveTimer.reset(false, elapsedRealtimeUs);
        this.mWifiActivity.reset(false, elapsedRealtimeUs);
        int i12 = 0;
        while (true) {
            StopwatchTimer[] stopwatchTimerArr = this.mGpsSignalQualityTimer;
            if (i12 >= stopwatchTimerArr.length) {
                break;
            }
            stopwatchTimerArr[i12].reset(false, elapsedRealtimeUs);
            i12++;
        }
        this.mBluetoothActivity.reset(false, elapsedRealtimeUs);
        this.mModemActivity.reset(false, elapsedRealtimeUs);
        this.mNumConnectivityChange = 0;
        int i13 = 0;
        while (i13 < this.mUidStats.size()) {
            int i14 = i13;
            int i15 = i;
            if (!this.mUidStats.valueAt(i13).reset(uptimeUs, elapsedRealtimeUs, resetReason)) {
                i2 = i14;
            } else {
                this.mUidStats.valueAt(i14).detachFromTimeBase();
                SparseArray<Uid> sparseArray = this.mUidStats;
                sparseArray.remove(sparseArray.keyAt(i14));
                i2 = i14 - 1;
            }
            i13 = i2 + i15;
            i = i15;
            j = 0;
        }
        boolean z = i;
        if (this.mRpmStats.size() > 0) {
            for (SamplingTimer timer : this.mRpmStats.values()) {
                this.mOnBatteryTimeBase.remove(timer);
            }
            this.mRpmStats.clear();
        }
        if (this.mScreenOffRpmStats.size() > 0) {
            for (SamplingTimer timer2 : this.mScreenOffRpmStats.values()) {
                this.mOnBatteryScreenOffTimeBase.remove(timer2);
            }
            this.mScreenOffRpmStats.clear();
        }
        if (this.mKernelWakelockStats.size() > 0) {
            for (SamplingTimer timer3 : this.mKernelWakelockStats.values()) {
                this.mOnBatteryScreenOffTimeBase.remove(timer3);
            }
            this.mKernelWakelockStats.clear();
        }
        if (this.mKernelMemoryStats.size() > 0) {
            for (int i16 = 0; i16 < this.mKernelMemoryStats.size(); i16++) {
                this.mOnBatteryTimeBase.remove(this.mKernelMemoryStats.valueAt(i16));
            }
            this.mKernelMemoryStats.clear();
        }
        if (this.mWakeupReasonStats.size() > 0) {
            synchronized (mLock) {
                for (SamplingTimer timer4 : this.mWakeupReasonStats.values()) {
                    this.mOnBatteryTimeBase.remove(timer4);
                }
                this.mWakeupReasonStats.clear();
            }
        }
        this.mTmpRailStats.reset();
        MeasuredEnergyStats.resetIfNotNull(this.mGlobalMeasuredEnergyStats);
        resetIfNotNull(this.mBinderThreadCpuTimesUs, false, elapsedRealtimeUs);
        this.mLastHistoryStepDetails = null;
        this.mLastStepCpuSystemTimeMs = 0L;
        this.mLastStepCpuUserTimeMs = 0L;
        this.mCurStepCpuSystemTimeMs = 0L;
        this.mCurStepCpuUserTimeMs = 0L;
        this.mCurStepCpuUserTimeMs = 0L;
        this.mLastStepCpuUserTimeMs = 0L;
        this.mCurStepCpuSystemTimeMs = 0L;
        this.mLastStepCpuSystemTimeMs = 0L;
        this.mCurStepStatUserTimeMs = 0L;
        this.mLastStepStatUserTimeMs = 0L;
        this.mCurStepStatSystemTimeMs = 0L;
        this.mLastStepStatSystemTimeMs = 0L;
        this.mCurStepStatIOWaitTimeMs = 0L;
        this.mLastStepStatIOWaitTimeMs = 0L;
        this.mCurStepStatIrqTimeMs = 0L;
        this.mLastStepStatIrqTimeMs = 0L;
        this.mCurStepStatSoftIrqTimeMs = 0L;
        this.mLastStepStatSoftIrqTimeMs = 0L;
        this.mCurStepStatIdleTimeMs = 0L;
        this.mLastStepStatIdleTimeMs = 0L;
        this.mNumAllUidCpuTimeReads = 0;
        this.mNumUidsRemoved = 0;
        initDischarge(elapsedRealtimeUs);
        clearHistoryLocked();
        BatteryStatsHistory batteryStatsHistory = this.mBatteryStatsHistory;
        if (batteryStatsHistory != null) {
            batteryStatsHistory.resetAllFiles();
        }
        this.mIgnoreNextExternalStats = z;
        this.mExternalSync.scheduleSync("reset", 127);
        this.mHandler.sendEmptyMessage(4);
    }

    private void initActiveHistoryEventsLocked(long elapsedRealtimeMs, long uptimeMs) {
        HashMap<String, SparseIntArray> active;
        for (int i = 0; i < 22; i++) {
            if ((this.mRecordAllHistory || i != 1) && (active = this.mActiveEvents.getStateForEvent(i)) != null) {
                for (Map.Entry<String, SparseIntArray> ent : active.entrySet()) {
                    SparseIntArray uids = ent.getValue();
                    for (int j = 0; j < uids.size(); j++) {
                        addHistoryEventLocked(elapsedRealtimeMs, uptimeMs, i, ent.getKey(), uids.keyAt(j));
                    }
                }
            }
        }
    }

    void updateDischargeScreenLevelsLocked(int oldState, int newState) {
        updateOldDischargeScreenLevelLocked(oldState);
        updateNewDischargeScreenLevelLocked(newState);
    }

    private void updateOldDischargeScreenLevelLocked(int state) {
        int diff;
        if (Display.isOnState(state)) {
            int diff2 = this.mDischargeScreenOnUnplugLevel - this.mDischargeCurrentLevel;
            if (diff2 > 0) {
                this.mDischargeAmountScreenOn += diff2;
                this.mDischargeAmountScreenOnSinceCharge += diff2;
            }
        } else if (Display.isDozeState(state)) {
            int diff3 = this.mDischargeScreenDozeUnplugLevel - this.mDischargeCurrentLevel;
            if (diff3 > 0) {
                this.mDischargeAmountScreenDoze += diff3;
                this.mDischargeAmountScreenDozeSinceCharge += diff3;
            }
        } else if (Display.isOffState(state) && (diff = this.mDischargeScreenOffUnplugLevel - this.mDischargeCurrentLevel) > 0) {
            this.mDischargeAmountScreenOff += diff;
            this.mDischargeAmountScreenOffSinceCharge += diff;
        }
    }

    private void updateNewDischargeScreenLevelLocked(int state) {
        if (Display.isOnState(state)) {
            this.mDischargeScreenOnUnplugLevel = this.mDischargeCurrentLevel;
            this.mDischargeScreenOffUnplugLevel = 0;
            this.mDischargeScreenDozeUnplugLevel = 0;
        } else if (Display.isDozeState(state)) {
            this.mDischargeScreenOnUnplugLevel = 0;
            this.mDischargeScreenDozeUnplugLevel = this.mDischargeCurrentLevel;
            this.mDischargeScreenOffUnplugLevel = 0;
        } else if (Display.isOffState(state)) {
            this.mDischargeScreenOnUnplugLevel = 0;
            this.mDischargeScreenDozeUnplugLevel = 0;
            this.mDischargeScreenOffUnplugLevel = this.mDischargeCurrentLevel;
        }
    }

    public void pullPendingStateUpdatesLocked() {
        if (this.mOnBatteryInternal) {
            int i = this.mScreenState;
            updateDischargeScreenLevelsLocked(i, i);
        }
    }

    protected NetworkStats readMobileNetworkStatsLocked(NetworkStatsManager networkStatsManager) {
        return networkStatsManager.getMobileUidStats();
    }

    protected NetworkStats readWifiNetworkStatsLocked(NetworkStatsManager networkStatsManager) {
        return networkStatsManager.getWifiUidStats();
    }

    /* JADX DEBUG: Don't trust debug lines info. Repeating lines: [13706=6] */
    /* JADX WARN: Removed duplicated region for block: B:167:0x04bd  */
    /* JADX WARN: Removed duplicated region for block: B:169:0x04c8 A[Catch: all -> 0x04ff, TryCatch #8 {all -> 0x04ff, blocks: (B:177:0x04fd, B:80:0x0197, B:82:0x019d, B:84:0x01af, B:85:0x01b6, B:86:0x01b8, B:87:0x01bf, B:100:0x0242, B:103:0x0278, B:105:0x0293, B:107:0x029e, B:110:0x02a7, B:112:0x02b0, B:114:0x02d7, B:116:0x02eb, B:119:0x030c, B:126:0x0349, B:131:0x0362, B:137:0x037d, B:139:0x03a7, B:144:0x03c7, B:149:0x03f0, B:158:0x045c, B:161:0x0493, B:163:0x049d, B:169:0x04c8, B:171:0x04d4, B:172:0x04eb, B:173:0x04f8), top: B:207:0x0022 }] */
    /* JADX WARN: Removed duplicated region for block: B:30:0x0063 A[Catch: all -> 0x0503, TryCatch #1 {all -> 0x0503, blocks: (B:12:0x0020, B:14:0x0024, B:17:0x002c, B:28:0x0043, B:30:0x0063, B:31:0x006b, B:33:0x0071, B:40:0x008a, B:50:0x0100, B:62:0x0159, B:64:0x015d, B:68:0x0174), top: B:193:0x0020 }] */
    /* JADX WARN: Removed duplicated region for block: B:94:0x0200  */
    /* JADX WARN: Removed duplicated region for block: B:96:0x020d  */
    /* JADX WARN: Unsupported multi-entry loop pattern (BACK_EDGE: B:185:0x0508 -> B:183:0x0506). Please submit an issue!!! */
    /*
        Code decompiled incorrectly, please refer to instructions dump.
    */
    public void updateWifiState(WifiActivityEnergyInfo info, long consumedChargeUC, long elapsedRealtimeMs, long uptimeMs, NetworkStatsManager networkStatsManager) {
        NetworkStats delta;
        SparseDoubleArray uidEstimatedConsumptionMah;
        SparseLongArray rxTimesMs;
        SparseLongArray txTimesMs;
        SparseLongArray txPackets;
        NetworkStats delta2;
        long totalRxPackets;
        long totalTxPackets;
        int uidStatsSize;
        long txTimeMs;
        SparseLongArray rxTimesMs2;
        SparseLongArray txTimesMs2;
        long wifiLockTimeSinceMarkMs;
        int uid;
        SparseLongArray txPackets2;
        NetworkStats delta3;
        BatteryStatsImpl batteryStatsImpl = this;
        synchronized (batteryStatsImpl.mWifiNetworkLock) {
            try {
                NetworkStats latestStats = batteryStatsImpl.readWifiNetworkStatsLocked(networkStatsManager);
                if (latestStats != null) {
                    NetworkStats delta4 = latestStats.subtract(batteryStatsImpl.mLastWifiNetworkStats);
                    batteryStatsImpl.mLastWifiNetworkStats = latestStats;
                    delta = delta4;
                } else {
                    delta = null;
                }
            } catch (Throwable th) {
                th = th;
            }
            try {
                synchronized (this) {
                    try {
                        try {
                            if (batteryStatsImpl.mOnBatteryInternal && !batteryStatsImpl.mIgnoreNextExternalStats) {
                                long j = 0;
                                if (batteryStatsImpl.mGlobalMeasuredEnergyStats != null) {
                                    try {
                                        if (batteryStatsImpl.mWifiPowerCalculator != null && consumedChargeUC > 0) {
                                            uidEstimatedConsumptionMah = new SparseDoubleArray();
                                            double totalEstimatedConsumptionMah = 0.0d;
                                            SparseLongArray rxPackets = new SparseLongArray();
                                            SparseLongArray txPackets3 = new SparseLongArray();
                                            SparseLongArray rxTimesMs3 = new SparseLongArray();
                                            SparseLongArray txTimesMs3 = new SparseLongArray();
                                            if (delta == null) {
                                                Iterator it = delta.iterator();
                                                totalRxPackets = 0;
                                                totalTxPackets = 0;
                                                while (it.hasNext()) {
                                                    NetworkStats.Entry entry = (NetworkStats.Entry) it.next();
                                                    if (entry.getRxBytes() != j || entry.getTxBytes() != j) {
                                                        int uid2 = batteryStatsImpl.mapUid(entry.getUid());
                                                        SparseLongArray rxTimesMs4 = rxTimesMs3;
                                                        SparseLongArray txTimesMs4 = txTimesMs3;
                                                        SparseLongArray rxPackets2 = rxPackets;
                                                        SparseLongArray txPackets4 = txPackets3;
                                                        Uid u = getUidStatsLocked(uid2, elapsedRealtimeMs, uptimeMs);
                                                        if (entry.getRxBytes() != j) {
                                                            u.noteNetworkActivityLocked(2, entry.getRxBytes(), entry.getRxPackets());
                                                            if (entry.getSet() == 0) {
                                                                u.noteNetworkActivityLocked(8, entry.getRxBytes(), entry.getRxPackets());
                                                            }
                                                            batteryStatsImpl.mNetworkByteActivityCounters[2].addCountLocked(entry.getRxBytes());
                                                            batteryStatsImpl.mNetworkPacketActivityCounters[2].addCountLocked(entry.getRxPackets());
                                                            uid = uid2;
                                                            rxPackets = rxPackets2;
                                                            rxPackets.incrementValue(uid, entry.getRxPackets());
                                                            totalRxPackets += entry.getRxPackets();
                                                        } else {
                                                            uid = uid2;
                                                            rxPackets = rxPackets2;
                                                        }
                                                        if (entry.getTxBytes() != j) {
                                                            u.noteNetworkActivityLocked(3, entry.getTxBytes(), entry.getTxPackets());
                                                            if (entry.getSet() == 0) {
                                                                u.noteNetworkActivityLocked(9, entry.getTxBytes(), entry.getTxPackets());
                                                            }
                                                            batteryStatsImpl.mNetworkByteActivityCounters[3].addCountLocked(entry.getTxBytes());
                                                            batteryStatsImpl.mNetworkPacketActivityCounters[3].addCountLocked(entry.getTxPackets());
                                                            txPackets2 = txPackets4;
                                                            txPackets2.incrementValue(uid, entry.getTxPackets());
                                                            totalTxPackets += entry.getTxPackets();
                                                        } else {
                                                            txPackets2 = txPackets4;
                                                        }
                                                        if (uidEstimatedConsumptionMah == null || info != null || batteryStatsImpl.mHasWifiReporting) {
                                                            delta3 = delta;
                                                        } else {
                                                            long uidRunningMs = u.mWifiRunningTimer.getTimeSinceMarkLocked(elapsedRealtimeMs * 1000) / 1000;
                                                            if (uidRunningMs > 0) {
                                                                u.mWifiRunningTimer.setMark(elapsedRealtimeMs);
                                                            }
                                                            NetworkStats delta5 = delta;
                                                            try {
                                                                long uidScanMs = u.mWifiScanTimer.getTimeSinceMarkLocked(elapsedRealtimeMs * 1000) / 1000;
                                                                if (uidScanMs > 0) {
                                                                    try {
                                                                        u.mWifiScanTimer.setMark(elapsedRealtimeMs);
                                                                    } catch (Throwable th2) {
                                                                        th = th2;
                                                                        throw th;
                                                                    }
                                                                }
                                                                long uidBatchScanMs = 0;
                                                                int bn = 0;
                                                                while (true) {
                                                                    delta3 = delta5;
                                                                    if (bn >= 5) {
                                                                        break;
                                                                    }
                                                                    if (u.mWifiBatchedScanTimer[bn] != null) {
                                                                        long bnMs = u.mWifiBatchedScanTimer[bn].getTimeSinceMarkLocked(elapsedRealtimeMs * 1000) / 1000;
                                                                        if (bnMs > 0) {
                                                                            u.mWifiBatchedScanTimer[bn].setMark(elapsedRealtimeMs);
                                                                        }
                                                                        uidBatchScanMs += bnMs;
                                                                    }
                                                                    bn++;
                                                                    delta5 = delta3;
                                                                }
                                                                int bn2 = u.getUid();
                                                                uidEstimatedConsumptionMah.incrementValue(bn2, batteryStatsImpl.mWifiPowerCalculator.calcPowerWithoutControllerDataMah(entry.getRxPackets(), entry.getTxPackets(), uidRunningMs, uidScanMs, uidBatchScanMs));
                                                            } catch (Throwable th3) {
                                                                th = th3;
                                                            }
                                                        }
                                                        txPackets3 = txPackets2;
                                                        rxTimesMs3 = rxTimesMs4;
                                                        txTimesMs3 = txTimesMs4;
                                                        delta = delta3;
                                                        j = 0;
                                                    }
                                                }
                                                rxTimesMs = rxTimesMs3;
                                                txTimesMs = txTimesMs3;
                                                txPackets = txPackets3;
                                                delta2 = null;
                                            } else {
                                                rxTimesMs = rxTimesMs3;
                                                txTimesMs = txTimesMs3;
                                                txPackets = txPackets3;
                                                delta2 = delta;
                                                totalRxPackets = 0;
                                                totalTxPackets = 0;
                                            }
                                            if (info == null) {
                                                try {
                                                    batteryStatsImpl.mHasWifiReporting = true;
                                                    long txTimeMs2 = info.getControllerTxDurationMillis();
                                                    long rxTimeMs = info.getControllerRxDurationMillis();
                                                    info.getControllerScanDurationMillis();
                                                    long idleTimeMs = info.getControllerIdleDurationMillis();
                                                    long j2 = txTimeMs2 + rxTimeMs + idleTimeMs;
                                                    long leftOverRxTimeMs = rxTimeMs;
                                                    long leftOverTxTimeMs = txTimeMs2;
                                                    int uidStatsSize2 = batteryStatsImpl.mUidStats.size();
                                                    long totalWifiLockTimeMs = 0;
                                                    int i = 0;
                                                    long totalScanTimeMs = 0;
                                                    while (i < uidStatsSize2) {
                                                        Uid uid3 = batteryStatsImpl.mUidStats.valueAt(i);
                                                        totalScanTimeMs += uid3.mWifiScanTimer.getTimeSinceMarkLocked(elapsedRealtimeMs * 1000) / 1000;
                                                        totalWifiLockTimeMs += uid3.mFullWifiLockTimer.getTimeSinceMarkLocked(elapsedRealtimeMs * 1000) / 1000;
                                                        i++;
                                                        leftOverRxTimeMs = leftOverRxTimeMs;
                                                        leftOverTxTimeMs = leftOverTxTimeMs;
                                                    }
                                                    long leftOverRxTimeMs2 = leftOverRxTimeMs;
                                                    int i2 = 0;
                                                    long leftOverRxTimeMs3 = leftOverRxTimeMs2;
                                                    long leftOverTxTimeMs2 = leftOverTxTimeMs;
                                                    while (i2 < uidStatsSize2) {
                                                        Uid uid4 = batteryStatsImpl.mUidStats.valueAt(i2);
                                                        long scanTimeSinceMarkMs = uid4.mWifiScanTimer.getTimeSinceMarkLocked(elapsedRealtimeMs * 1000) / 1000;
                                                        long scanRxTimeSinceMarkMs = scanTimeSinceMarkMs;
                                                        long scanTxTimeSinceMarkMs = scanTimeSinceMarkMs;
                                                        if (scanTimeSinceMarkMs > 0) {
                                                            uid4.mWifiScanTimer.setMark(elapsedRealtimeMs);
                                                            if (totalScanTimeMs > rxTimeMs) {
                                                                scanRxTimeSinceMarkMs = (rxTimeMs * scanRxTimeSinceMarkMs) / totalScanTimeMs;
                                                            }
                                                            long scanTxTimeSinceMarkMs2 = totalScanTimeMs > txTimeMs2 ? (txTimeMs2 * scanTxTimeSinceMarkMs) / totalScanTimeMs : scanTxTimeSinceMarkMs;
                                                            uidStatsSize = uidStatsSize2;
                                                            rxTimesMs2 = rxTimesMs;
                                                            rxTimesMs2.incrementValue(uid4.getUid(), scanRxTimeSinceMarkMs);
                                                            txTimeMs = txTimeMs2;
                                                            txTimesMs2 = txTimesMs;
                                                            txTimesMs2.incrementValue(uid4.getUid(), scanTxTimeSinceMarkMs2);
                                                            leftOverRxTimeMs3 -= scanRxTimeSinceMarkMs;
                                                            leftOverTxTimeMs2 -= scanTxTimeSinceMarkMs2;
                                                            scanTxTimeSinceMarkMs = scanTxTimeSinceMarkMs2;
                                                        } else {
                                                            uidStatsSize = uidStatsSize2;
                                                            txTimeMs = txTimeMs2;
                                                            rxTimesMs2 = rxTimesMs;
                                                            txTimesMs2 = txTimesMs;
                                                        }
                                                        long myIdleTimeMs = elapsedRealtimeMs * 1000;
                                                        long wifiLockTimeSinceMarkMs2 = uid4.mFullWifiLockTimer.getTimeSinceMarkLocked(myIdleTimeMs) / 1000;
                                                        if (wifiLockTimeSinceMarkMs2 > 0) {
                                                            uid4.mFullWifiLockTimer.setMark(elapsedRealtimeMs);
                                                            long myIdleTimeMs2 = (wifiLockTimeSinceMarkMs2 * idleTimeMs) / totalWifiLockTimeMs;
                                                            wifiLockTimeSinceMarkMs = myIdleTimeMs2;
                                                            uid4.getOrCreateWifiControllerActivityLocked().getOrCreateIdleTimeCounter().increment(wifiLockTimeSinceMarkMs, elapsedRealtimeMs);
                                                        } else {
                                                            wifiLockTimeSinceMarkMs = 0;
                                                        }
                                                        if (uidEstimatedConsumptionMah != null) {
                                                            double uidEstMah = batteryStatsImpl.mWifiPowerCalculator.calcPowerFromControllerDataMah(scanRxTimeSinceMarkMs, scanTxTimeSinceMarkMs, wifiLockTimeSinceMarkMs);
                                                            uidEstimatedConsumptionMah.incrementValue(uid4.getUid(), uidEstMah);
                                                        }
                                                        i2++;
                                                        txTimesMs = txTimesMs2;
                                                        rxTimesMs = rxTimesMs2;
                                                        uidStatsSize2 = uidStatsSize;
                                                        txTimeMs2 = txTimeMs;
                                                    }
                                                    long txTimeMs3 = txTimeMs2;
                                                    SparseLongArray rxTimesMs5 = rxTimesMs;
                                                    SparseLongArray txTimesMs5 = txTimesMs;
                                                    for (int i3 = 0; i3 < txPackets.size(); i3++) {
                                                        int uid5 = txPackets.keyAt(i3);
                                                        txTimesMs5.incrementValue(uid5, (txPackets.valueAt(i3) * leftOverTxTimeMs2) / totalTxPackets);
                                                    }
                                                    for (int i4 = 0; i4 < rxPackets.size(); i4++) {
                                                        int uid6 = rxPackets.keyAt(i4);
                                                        rxTimesMs5.incrementValue(uid6, (rxPackets.valueAt(i4) * leftOverRxTimeMs3) / totalRxPackets);
                                                    }
                                                    int i5 = 0;
                                                    while (i5 < txTimesMs5.size()) {
                                                        int uid7 = txTimesMs5.keyAt(i5);
                                                        long myTxTimeMs = txTimesMs5.valueAt(i5);
                                                        SparseLongArray rxPackets3 = rxPackets;
                                                        getUidStatsLocked(uid7, elapsedRealtimeMs, uptimeMs).getOrCreateWifiControllerActivityLocked().getOrCreateTxTimeCounters()[0].increment(myTxTimeMs, elapsedRealtimeMs);
                                                        if (uidEstimatedConsumptionMah != null) {
                                                            uidEstimatedConsumptionMah.incrementValue(uid7, batteryStatsImpl.mWifiPowerCalculator.calcPowerFromControllerDataMah(0L, myTxTimeMs, 0L));
                                                        }
                                                        i5++;
                                                        rxPackets = rxPackets3;
                                                    }
                                                    for (int i6 = 0; i6 < rxTimesMs5.size(); i6++) {
                                                        int uid8 = rxTimesMs5.keyAt(i6);
                                                        long myRxTimeMs = rxTimesMs5.valueAt(i6);
                                                        try {
                                                            getUidStatsLocked(rxTimesMs5.keyAt(i6), elapsedRealtimeMs, uptimeMs).getOrCreateWifiControllerActivityLocked().getOrCreateRxTimeCounter().increment(myRxTimeMs, elapsedRealtimeMs);
                                                            if (uidEstimatedConsumptionMah != null) {
                                                                batteryStatsImpl = this;
                                                                uidEstimatedConsumptionMah.incrementValue(uid8, batteryStatsImpl.mWifiPowerCalculator.calcPowerFromControllerDataMah(myRxTimeMs, 0L, 0L));
                                                            } else {
                                                                batteryStatsImpl = this;
                                                            }
                                                        } catch (Throwable th4) {
                                                            th = th4;
                                                            throw th;
                                                        }
                                                    }
                                                    batteryStatsImpl.mWifiActivity.getOrCreateRxTimeCounter().increment(info.getControllerRxDurationMillis(), elapsedRealtimeMs);
                                                    batteryStatsImpl.mWifiActivity.getOrCreateTxTimeCounters()[0].increment(info.getControllerTxDurationMillis(), elapsedRealtimeMs);
                                                    batteryStatsImpl.mWifiActivity.getScanTimeCounter().addCountLocked(info.getControllerScanDurationMillis());
                                                    batteryStatsImpl.mWifiActivity.getOrCreateIdleTimeCounter().increment(info.getControllerIdleDurationMillis(), elapsedRealtimeMs);
                                                    double opVolt = batteryStatsImpl.mPowerProfile.getAveragePower(PowerProfile.POWER_WIFI_CONTROLLER_OPERATING_VOLTAGE) / 1000.0d;
                                                    double controllerMaMs = 0.0d;
                                                    if (opVolt != 0.0d) {
                                                        controllerMaMs = info.getControllerEnergyUsedMicroJoules() / opVolt;
                                                        batteryStatsImpl.mWifiActivity.getPowerCounter().addCountLocked((long) controllerMaMs);
                                                    }
                                                    long monitoredRailChargeConsumedMaMs = (long) (batteryStatsImpl.mTmpRailStats.getWifiTotalEnergyUseduWs() / opVolt);
                                                    batteryStatsImpl.mWifiActivity.getMonitoredRailChargeConsumedMaMs().addCountLocked(monitoredRailChargeConsumedMaMs);
                                                    batteryStatsImpl.mHistoryCur.wifiRailChargeMah += monitoredRailChargeConsumedMaMs / 3600000.0d;
                                                    batteryStatsImpl.addHistoryRecordLocked(elapsedRealtimeMs, uptimeMs);
                                                    batteryStatsImpl.mTmpRailStats.resetWifiTotalEnergyUsed();
                                                    if (uidEstimatedConsumptionMah != null) {
                                                        double d = controllerMaMs / 3600000.0d;
                                                        double controllerMaMs2 = batteryStatsImpl.mWifiPowerCalculator.calcPowerFromControllerDataMah(rxTimeMs, txTimeMs3, idleTimeMs);
                                                        totalEstimatedConsumptionMah = Math.max(d, controllerMaMs2);
                                                    }
                                                } catch (Throwable th5) {
                                                    th = th5;
                                                }
                                            }
                                            if (uidEstimatedConsumptionMah != null) {
                                                batteryStatsImpl.mGlobalMeasuredEnergyStats.updateStandardBucket(4, consumedChargeUC);
                                                if (!batteryStatsImpl.mHasWifiReporting) {
                                                    long globalTimeMs = batteryStatsImpl.mGlobalWifiRunningTimer.getTimeSinceMarkLocked(elapsedRealtimeMs * 1000) / 1000;
                                                    batteryStatsImpl.mGlobalWifiRunningTimer.setMark(elapsedRealtimeMs);
                                                    totalEstimatedConsumptionMah = batteryStatsImpl.mWifiPowerCalculator.calcGlobalPowerWithoutControllerDataMah(globalTimeMs);
                                                }
                                                distributeEnergyToUidsLocked(4, consumedChargeUC, uidEstimatedConsumptionMah, totalEstimatedConsumptionMah, elapsedRealtimeMs);
                                            }
                                        }
                                    } catch (Throwable th6) {
                                        th = th6;
                                        throw th;
                                    }
                                }
                                uidEstimatedConsumptionMah = null;
                                double totalEstimatedConsumptionMah2 = 0.0d;
                                SparseLongArray rxPackets4 = new SparseLongArray();
                                SparseLongArray txPackets32 = new SparseLongArray();
                                SparseLongArray rxTimesMs32 = new SparseLongArray();
                                SparseLongArray txTimesMs32 = new SparseLongArray();
                                if (delta == null) {
                                }
                                if (info == null) {
                                }
                                if (uidEstimatedConsumptionMah != null) {
                                }
                            }
                        } catch (Throwable th7) {
                            th = th7;
                        }
                    } catch (Throwable th8) {
                        th = th8;
                    }
                }
            } catch (Throwable th9) {
                th = th9;
                throw th;
            }
        }
    }

    /* JADX DEBUG: Don't trust debug lines info. Repeating lines: [13948=4] */
    /* JADX WARN: Removed duplicated region for block: B:121:0x0385  */
    /* JADX WARN: Removed duplicated region for block: B:153:0x006d A[EXC_TOP_SPLITTER, SYNTHETIC] */
    /* JADX WARN: Removed duplicated region for block: B:56:0x0164  */
    /* JADX WARN: Removed duplicated region for block: B:59:0x017d A[Catch: all -> 0x038d, TryCatch #5 {all -> 0x038d, blocks: (B:18:0x0040, B:20:0x0044, B:57:0x0166, B:59:0x017d, B:60:0x0185, B:110:0x0314, B:116:0x0355), top: B:141:0x0040 }] */
    /*
        Code decompiled incorrectly, please refer to instructions dump.
    */
    public void noteModemControllerActivity(ModemActivityInfo activityInfo, long consumedChargeUC, long elapsedRealtimeMs, long uptimeMs, NetworkStatsManager networkStatsManager) {
        NetworkStats delta;
        MeasuredEnergyStats measuredEnergyStats;
        SparseDoubleArray uidEstimatedConsumptionMah;
        long totalAppRadioTimeUs;
        ModemActivityInfo modemActivityInfo = this.mLastModemActivityInfo;
        ModemActivityInfo deltaInfo = modemActivityInfo == null ? activityInfo : modemActivityInfo.getDelta(activityInfo);
        this.mLastModemActivityInfo = activityInfo;
        addModemTxPowerToHistory(deltaInfo, elapsedRealtimeMs, uptimeMs);
        synchronized (this.mModemNetworkLock) {
            try {
                NetworkStats latestStats = readMobileNetworkStatsLocked(networkStatsManager);
                if (latestStats != null) {
                    try {
                        NetworkStats delta2 = latestStats.subtract(this.mLastModemNetworkStats);
                        this.mLastModemNetworkStats = latestStats;
                        delta = delta2;
                    } catch (Throwable th) {
                        th = th;
                        while (true) {
                            try {
                                break;
                            } catch (Throwable th2) {
                                th = th2;
                            }
                        }
                        throw th;
                    }
                } else {
                    delta = null;
                }
                try {
                    synchronized (this) {
                        try {
                            try {
                                if (this.mOnBatteryInternal && !this.mIgnoreNextExternalStats) {
                                    if (consumedChargeUC > 0) {
                                        try {
                                            if (this.mMobileRadioPowerCalculator != null && (measuredEnergyStats = this.mGlobalMeasuredEnergyStats) != null) {
                                                measuredEnergyStats.updateStandardBucket(7, consumedChargeUC);
                                                uidEstimatedConsumptionMah = new SparseDoubleArray();
                                                if (deltaInfo == null) {
                                                    try {
                                                        this.mHasModemReporting = true;
                                                        this.mModemActivity.getOrCreateIdleTimeCounter().increment(deltaInfo.getIdleTimeMillis(), elapsedRealtimeMs);
                                                        this.mModemActivity.getSleepTimeCounter().addCountLocked(deltaInfo.getSleepTimeMillis());
                                                        this.mModemActivity.getOrCreateRxTimeCounter().increment(deltaInfo.getReceiveTimeMillis(), elapsedRealtimeMs);
                                                        for (int lvl = 0; lvl < ModemActivityInfo.getNumTxPowerLevels(); lvl++) {
                                                            this.mModemActivity.getOrCreateTxTimeCounters()[lvl].increment(deltaInfo.getTransmitDurationMillisAtPowerLevel(lvl), elapsedRealtimeMs);
                                                        }
                                                        double opVolt = this.mPowerProfile.getAveragePower(PowerProfile.POWER_MODEM_CONTROLLER_OPERATING_VOLTAGE) / 1000.0d;
                                                        if (opVolt != 0.0d) {
                                                            double energyUsed = (deltaInfo.getSleepTimeMillis() * this.mPowerProfile.getAveragePower(PowerProfile.POWER_MODEM_CONTROLLER_SLEEP)) + (deltaInfo.getIdleTimeMillis() * this.mPowerProfile.getAveragePower(PowerProfile.POWER_MODEM_CONTROLLER_IDLE)) + (deltaInfo.getReceiveTimeMillis() * this.mPowerProfile.getAveragePower(PowerProfile.POWER_MODEM_CONTROLLER_RX));
                                                            for (int i = 0; i < Math.min(ModemActivityInfo.getNumTxPowerLevels(), CellSignalStrength.getNumSignalStrengthLevels() + 1); i++) {
                                                                energyUsed += deltaInfo.getTransmitDurationMillisAtPowerLevel(i) * this.mPowerProfile.getAveragePower(PowerProfile.POWER_MODEM_CONTROLLER_TX, i);
                                                            }
                                                            this.mModemActivity.getPowerCounter().addCountLocked((long) energyUsed);
                                                            long monitoredRailChargeConsumedMaMs = (long) (this.mTmpRailStats.getCellularTotalEnergyUseduWs() / opVolt);
                                                            this.mModemActivity.getMonitoredRailChargeConsumedMaMs().addCountLocked(monitoredRailChargeConsumedMaMs);
                                                            double energyUsed2 = monitoredRailChargeConsumedMaMs;
                                                            this.mHistoryCur.modemRailChargeMah += energyUsed2 / 3600000.0d;
                                                            addHistoryRecordLocked(elapsedRealtimeMs, uptimeMs);
                                                            this.mTmpRailStats.resetCellularTotalEnergyUsed();
                                                        }
                                                        incrementPerRatDataLocked(deltaInfo, elapsedRealtimeMs);
                                                    } catch (Throwable th3) {
                                                        th = th3;
                                                        throw th;
                                                    }
                                                }
                                                long totalAppRadioTimeUs2 = this.mMobileRadioActivePerAppTimer.getTimeSinceMarkLocked(elapsedRealtimeMs * 1000);
                                                this.mMobileRadioActivePerAppTimer.setMark(elapsedRealtimeMs);
                                                if (delta == null) {
                                                    Iterator it = delta.iterator();
                                                    long totalRxPackets = 0;
                                                    long totalTxPackets = 0;
                                                    while (it.hasNext()) {
                                                        NetworkStats.Entry entry = (NetworkStats.Entry) it.next();
                                                        if (entry.getRxPackets() != 0 || entry.getTxPackets() != 0) {
                                                            totalRxPackets += entry.getRxPackets();
                                                            totalTxPackets += entry.getTxPackets();
                                                            Uid u = getUidStatsLocked(mapUid(entry.getUid()), elapsedRealtimeMs, uptimeMs);
                                                            u.noteNetworkActivityLocked(0, entry.getRxBytes(), entry.getRxPackets());
                                                            u.noteNetworkActivityLocked(1, entry.getTxBytes(), entry.getTxPackets());
                                                            if (entry.getSet() == 0) {
                                                                u.noteNetworkActivityLocked(6, entry.getRxBytes(), entry.getRxPackets());
                                                                u.noteNetworkActivityLocked(7, entry.getTxBytes(), entry.getTxPackets());
                                                            }
                                                            this.mNetworkByteActivityCounters[0].addCountLocked(entry.getRxBytes());
                                                            this.mNetworkByteActivityCounters[1].addCountLocked(entry.getTxBytes());
                                                            this.mNetworkPacketActivityCounters[0].addCountLocked(entry.getRxPackets());
                                                            this.mNetworkPacketActivityCounters[1].addCountLocked(entry.getTxPackets());
                                                        }
                                                    }
                                                    long totalPackets = totalRxPackets + totalTxPackets;
                                                    if (totalPackets > 0) {
                                                        Iterator it2 = delta.iterator();
                                                        long totalAppRadioTimeUs3 = totalAppRadioTimeUs2;
                                                        long totalAppRadioTimeUs4 = totalPackets;
                                                        while (it2.hasNext()) {
                                                            NetworkStats.Entry entry2 = (NetworkStats.Entry) it2.next();
                                                            if (entry2.getRxPackets() != 0 || entry2.getTxPackets() != 0) {
                                                                Uid u2 = getUidStatsLocked(mapUid(entry2.getUid()), elapsedRealtimeMs, uptimeMs);
                                                                long appPackets = entry2.getRxPackets() + entry2.getTxPackets();
                                                                long appRadioTimeUs = (totalAppRadioTimeUs3 * appPackets) / totalAppRadioTimeUs4;
                                                                u2.noteMobileRadioActiveTimeLocked(appRadioTimeUs, elapsedRealtimeMs);
                                                                if (uidEstimatedConsumptionMah != null) {
                                                                    uidEstimatedConsumptionMah.incrementValue(u2.getUid(), this.mMobileRadioPowerCalculator.calcPowerFromRadioActiveDurationMah(appRadioTimeUs / 1000));
                                                                }
                                                                totalAppRadioTimeUs3 -= appRadioTimeUs;
                                                                totalAppRadioTimeUs4 -= appPackets;
                                                                if (deltaInfo != null) {
                                                                    ControllerActivityCounterImpl activityCounter = u2.getOrCreateModemControllerActivityLocked();
                                                                    if (totalRxPackets > 0 && entry2.getRxPackets() > 0) {
                                                                        long rxMs = (entry2.getRxPackets() * deltaInfo.getReceiveTimeMillis()) / totalRxPackets;
                                                                        activityCounter.getOrCreateRxTimeCounter().increment(rxMs, elapsedRealtimeMs);
                                                                    }
                                                                    if (totalTxPackets > 0 && entry2.getTxPackets() > 0) {
                                                                        int lvl2 = 0;
                                                                        while (lvl2 < ModemActivityInfo.getNumTxPowerLevels()) {
                                                                            long txMs = entry2.getTxPackets() * deltaInfo.getTransmitDurationMillisAtPowerLevel(lvl2);
                                                                            activityCounter.getOrCreateTxTimeCounters()[lvl2].increment(txMs / totalTxPackets, elapsedRealtimeMs);
                                                                            lvl2++;
                                                                            u2 = u2;
                                                                        }
                                                                    }
                                                                }
                                                            }
                                                        }
                                                        totalAppRadioTimeUs = totalAppRadioTimeUs3;
                                                    } else {
                                                        totalAppRadioTimeUs = totalAppRadioTimeUs2;
                                                    }
                                                    if (totalAppRadioTimeUs > 0) {
                                                        this.mMobileRadioActiveUnknownTime.addCountLocked(totalAppRadioTimeUs);
                                                        this.mMobileRadioActiveUnknownCount.addCountLocked(1L);
                                                    }
                                                    if (uidEstimatedConsumptionMah != null) {
                                                        long totalRadioTimeMs = this.mMobileRadioActiveTimer.getTimeSinceMarkLocked(elapsedRealtimeMs * 1000) / 1000;
                                                        this.mMobileRadioActiveTimer.setMark(elapsedRealtimeMs);
                                                        double totalEstimatedConsumptionMah = 0.0d + this.mMobileRadioPowerCalculator.calcPowerFromRadioActiveDurationMah(totalRadioTimeMs);
                                                        int numSignalStrengthLevels = this.mPhoneSignalStrengthsTimer.length;
                                                        int strengthLevel = 0;
                                                        while (strengthLevel < numSignalStrengthLevels) {
                                                            long totalRadioTimeMs2 = totalRadioTimeMs;
                                                            long strengthLevelDurationMs = this.mPhoneSignalStrengthsTimer[strengthLevel].getTimeSinceMarkLocked(elapsedRealtimeMs * 1000) / 1000;
                                                            this.mPhoneSignalStrengthsTimer[strengthLevel].setMark(elapsedRealtimeMs);
                                                            totalEstimatedConsumptionMah += this.mMobileRadioPowerCalculator.calcIdlePowerAtSignalStrengthMah(strengthLevelDurationMs, strengthLevel);
                                                            strengthLevel++;
                                                            totalRadioTimeMs = totalRadioTimeMs2;
                                                        }
                                                        long scanTimeMs = this.mPhoneSignalScanningTimer.getTimeSinceMarkLocked(elapsedRealtimeMs * 1000) / 1000;
                                                        this.mPhoneSignalScanningTimer.setMark(elapsedRealtimeMs);
                                                        distributeEnergyToUidsLocked(7, consumedChargeUC, uidEstimatedConsumptionMah, totalEstimatedConsumptionMah + this.mMobileRadioPowerCalculator.calcScanTimePowerMah(scanTimeMs), elapsedRealtimeMs);
                                                    }
                                                }
                                            }
                                        } catch (Throwable th4) {
                                            th = th4;
                                            throw th;
                                        }
                                    }
                                    uidEstimatedConsumptionMah = null;
                                    if (deltaInfo == null) {
                                    }
                                    long totalAppRadioTimeUs22 = this.mMobileRadioActivePerAppTimer.getTimeSinceMarkLocked(elapsedRealtimeMs * 1000);
                                    this.mMobileRadioActivePerAppTimer.setMark(elapsedRealtimeMs);
                                    if (delta == null) {
                                    }
                                }
                            } catch (Throwable th5) {
                                th = th5;
                            }
                        } catch (Throwable th6) {
                            th = th6;
                        }
                    }
                } catch (Throwable th7) {
                    th = th7;
                    while (true) {
                        break;
                        break;
                    }
                    throw th;
                }
            } catch (Throwable th8) {
                th = th8;
            }
        }
    }

    private void incrementPerRatDataLocked(ModemActivityInfo deltaInfo, long elapsedRealtimeMs) {
        int infoSize = deltaInfo.getSpecificInfoLength();
        int i = 3;
        if (infoSize == 1 && deltaInfo.getSpecificInfoRat(0) == 0 && deltaInfo.getSpecificInfoFrequencyRange(0) == 0) {
            int levelCount = ModemActivityInfo.getNumTxPowerLevels();
            long[] perSignalStrengthActiveTimeMs = new long[levelCount];
            long totalActiveTimeMs = 0;
            for (int rat = 0; rat < 3; rat++) {
                RadioAccessTechnologyBatteryStats ratStats = this.mPerRatBatteryStats[rat];
                if (ratStats != null) {
                    int freqCount = ratStats.getFrequencyRangeCount();
                    for (int freq = 0; freq < freqCount; freq++) {
                        for (int level = 0; level < levelCount; level++) {
                            long durationMs = ratStats.getTimeSinceMark(freq, level, elapsedRealtimeMs);
                            perSignalStrengthActiveTimeMs[level] = perSignalStrengthActiveTimeMs[level] + durationMs;
                            totalActiveTimeMs += durationMs;
                        }
                    }
                }
            }
            long j = 0;
            if (totalActiveTimeMs != 0) {
                int rat2 = 0;
                while (rat2 < i) {
                    RadioAccessTechnologyBatteryStats ratStats2 = this.mPerRatBatteryStats[rat2];
                    if (ratStats2 != null) {
                        int freqCount2 = ratStats2.getFrequencyRangeCount();
                        int freq2 = 0;
                        while (freq2 < freqCount2) {
                            long frequencyDurationMs = 0;
                            int level2 = 0;
                            while (level2 < levelCount) {
                                long durationMs2 = ratStats2.getTimeSinceMark(freq2, level2, elapsedRealtimeMs);
                                long totalLvlDurationMs = perSignalStrengthActiveTimeMs[level2];
                                if (totalLvlDurationMs != j) {
                                    long totalTxLvlDurations = deltaInfo.getTransmitDurationMillisAtPowerLevel(level2);
                                    long proportionalTxDurationMs = ((durationMs2 * totalTxLvlDurations) + (totalLvlDurationMs / 2)) / totalLvlDurationMs;
                                    ratStats2.incrementTxDuration(freq2, level2, proportionalTxDurationMs);
                                    frequencyDurationMs += durationMs2;
                                }
                                level2++;
                                j = 0;
                            }
                            long totalRxDuration = deltaInfo.getReceiveTimeMillis();
                            long proportionalRxDurationMs = ((frequencyDurationMs * totalRxDuration) + (totalActiveTimeMs / 2)) / totalActiveTimeMs;
                            ratStats2.incrementRxDuration(freq2, proportionalRxDurationMs);
                            freq2++;
                            levelCount = levelCount;
                            j = 0;
                        }
                    }
                    rat2++;
                    levelCount = levelCount;
                    i = 3;
                    j = 0;
                }
            }
        } else {
            for (int index = 0; index < infoSize; index++) {
                int rat3 = deltaInfo.getSpecificInfoRat(index);
                int freq3 = deltaInfo.getSpecificInfoFrequencyRange(index);
                int ratBucket = mapRadioAccessNetworkTypeToRadioAccessTechnology(rat3);
                RadioAccessTechnologyBatteryStats ratStats3 = getRatBatteryStatsLocked(ratBucket);
                long rxTimeMs = deltaInfo.getReceiveTimeMillis(rat3, freq3);
                int[] txTimesMs = deltaInfo.getTransmitTimeMillis(rat3, freq3);
                ratStats3.incrementRxDuration(freq3, rxTimeMs);
                int numTxLvl = txTimesMs.length;
                int lvl = 0;
                while (lvl < numTxLvl) {
                    ratStats3.incrementTxDuration(freq3, lvl, txTimesMs[lvl]);
                    lvl++;
                    rxTimeMs = rxTimeMs;
                }
            }
        }
        for (int rat4 = 0; rat4 < 3; rat4++) {
            RadioAccessTechnologyBatteryStats ratStats4 = this.mPerRatBatteryStats[rat4];
            if (ratStats4 != null) {
                ratStats4.setMark(elapsedRealtimeMs);
            }
        }
    }

    private synchronized void addModemTxPowerToHistory(ModemActivityInfo activityInfo, long elapsedRealtimeMs, long uptimeMs) {
        if (activityInfo == null) {
            return;
        }
        int levelMaxTimeSpent = 0;
        for (int i = 1; i < ModemActivityInfo.getNumTxPowerLevels(); i++) {
            if (activityInfo.getTransmitDurationMillisAtPowerLevel(i) > activityInfo.getTransmitDurationMillisAtPowerLevel(levelMaxTimeSpent)) {
                levelMaxTimeSpent = i;
            }
        }
        int i2 = ModemActivityInfo.getNumTxPowerLevels();
        if (levelMaxTimeSpent == i2 - 1) {
            this.mHistoryCur.states2 |= 524288;
            addHistoryRecordLocked(elapsedRealtimeMs, uptimeMs);
        }
    }

    /* loaded from: classes4.dex */
    private final class BluetoothActivityInfoCache {
        long energy;
        long idleTimeMs;
        long rxTimeMs;
        long txTimeMs;
        SparseLongArray uidRxBytes;
        SparseLongArray uidTxBytes;

        private BluetoothActivityInfoCache() {
            this.uidRxBytes = new SparseLongArray();
            this.uidTxBytes = new SparseLongArray();
        }

        void set(BluetoothActivityEnergyInfo info) {
            this.idleTimeMs = info.getControllerIdleTimeMillis();
            this.rxTimeMs = info.getControllerRxTimeMillis();
            this.txTimeMs = info.getControllerTxTimeMillis();
            this.energy = info.getControllerEnergyUsed();
            if (!info.getUidTraffic().isEmpty()) {
                for (UidTraffic traffic : info.getUidTraffic()) {
                    this.uidRxBytes.incrementValue(traffic.getUid(), traffic.getRxBytes());
                    this.uidTxBytes.incrementValue(traffic.getUid(), traffic.getTxBytes());
                }
            }
        }

        void reset() {
            this.idleTimeMs = 0L;
            this.rxTimeMs = 0L;
            this.txTimeMs = 0L;
            this.energy = 0L;
            this.uidRxBytes.clear();
            this.uidTxBytes.clear();
        }
    }

    public void updateBluetoothStateLocked(BluetoothActivityEnergyInfo info, long consumedChargeUC, long elapsedRealtimeMs, long uptimeMs) {
        BluetoothActivityEnergyInfo bluetoothActivityEnergyInfo;
        double controllerMaMs;
        SparseLongArray txTimesMs;
        SparseLongArray rxTimesMs;
        long rxTimeMs;
        long scanTimeSinceMarkMs;
        long rxTimeMs2;
        long idleTimeMs;
        if (info == null) {
            return;
        }
        if (this.mOnBatteryInternal) {
            if (!this.mIgnoreNextExternalStats) {
                this.mHasBluetoothReporting = true;
                if (info.getControllerRxTimeMillis() < this.mLastBluetoothActivityInfo.rxTimeMs || info.getControllerTxTimeMillis() < this.mLastBluetoothActivityInfo.txTimeMs || info.getControllerIdleTimeMillis() < this.mLastBluetoothActivityInfo.idleTimeMs || info.getControllerEnergyUsed() < this.mLastBluetoothActivityInfo.energy) {
                    this.mLastBluetoothActivityInfo.reset();
                }
                long rxTimeMs3 = info.getControllerRxTimeMillis() - this.mLastBluetoothActivityInfo.rxTimeMs;
                long txTimeMs = info.getControllerTxTimeMillis() - this.mLastBluetoothActivityInfo.txTimeMs;
                long idleTimeMs2 = info.getControllerIdleTimeMillis() - this.mLastBluetoothActivityInfo.idleTimeMs;
                SparseDoubleArray uidEstimatedConsumptionMah = (this.mGlobalMeasuredEnergyStats == null || this.mBluetoothPowerCalculator == null || consumedChargeUC <= 0) ? null : new SparseDoubleArray();
                int uidCount = this.mUidStats.size();
                long totalScanTimeMs = 0;
                int i = 0;
                while (i < uidCount) {
                    Uid u = this.mUidStats.valueAt(i);
                    if (u.mBluetoothScanTimer == null) {
                        idleTimeMs = idleTimeMs2;
                    } else {
                        idleTimeMs = idleTimeMs2;
                        long idleTimeMs3 = elapsedRealtimeMs * 1000;
                        totalScanTimeMs += u.mBluetoothScanTimer.getTimeSinceMarkLocked(idleTimeMs3) / 1000;
                    }
                    i++;
                    idleTimeMs2 = idleTimeMs;
                }
                long idleTimeMs4 = idleTimeMs2;
                boolean normalizeScanRxTime = totalScanTimeMs > rxTimeMs3;
                boolean normalizeScanTxTime = totalScanTimeMs > txTimeMs;
                SparseLongArray rxTimesMs2 = new SparseLongArray(uidCount);
                long leftOverRxTimeMs = rxTimeMs3;
                SparseLongArray txTimesMs2 = new SparseLongArray(uidCount);
                int i2 = 0;
                long leftOverTxTimeMs = txTimeMs;
                while (i2 < uidCount) {
                    Uid u2 = this.mUidStats.valueAt(i2);
                    if (u2.mBluetoothScanTimer == null) {
                        txTimesMs = txTimesMs2;
                        rxTimesMs = rxTimesMs2;
                        rxTimeMs = rxTimeMs3;
                    } else {
                        long scanTimeSinceMarkMs2 = u2.mBluetoothScanTimer.getTimeSinceMarkLocked(elapsedRealtimeMs * 1000) / 1000;
                        if (scanTimeSinceMarkMs2 <= 0) {
                            txTimesMs = txTimesMs2;
                            rxTimesMs = rxTimesMs2;
                            rxTimeMs = rxTimeMs3;
                        } else {
                            u2.mBluetoothScanTimer.setMark(elapsedRealtimeMs);
                            if (!normalizeScanRxTime) {
                                scanTimeSinceMarkMs = scanTimeSinceMarkMs2;
                            } else {
                                long scanTimeRxSinceMarkMs = (rxTimeMs3 * scanTimeSinceMarkMs2) / totalScanTimeMs;
                                scanTimeSinceMarkMs = scanTimeRxSinceMarkMs;
                            }
                            if (!normalizeScanTxTime) {
                                rxTimeMs = rxTimeMs3;
                                rxTimeMs2 = scanTimeSinceMarkMs2;
                            } else {
                                long scanTimeTxSinceMarkMs = (txTimeMs * scanTimeSinceMarkMs2) / totalScanTimeMs;
                                rxTimeMs = rxTimeMs3;
                                rxTimeMs2 = scanTimeTxSinceMarkMs;
                            }
                            rxTimesMs2.incrementValue(u2.getUid(), scanTimeSinceMarkMs);
                            txTimesMs2.incrementValue(u2.getUid(), rxTimeMs2);
                            if (uidEstimatedConsumptionMah == null) {
                                txTimesMs = txTimesMs2;
                                rxTimesMs = rxTimesMs2;
                            } else {
                                txTimesMs = txTimesMs2;
                                rxTimesMs = rxTimesMs2;
                                uidEstimatedConsumptionMah.incrementValue(u2.getUid(), this.mBluetoothPowerCalculator.calculatePowerMah(scanTimeSinceMarkMs, rxTimeMs2, 0L));
                            }
                            leftOverRxTimeMs -= scanTimeSinceMarkMs;
                            leftOverTxTimeMs -= rxTimeMs2;
                        }
                    }
                    i2++;
                    rxTimesMs2 = rxTimesMs;
                    txTimesMs2 = txTimesMs;
                    rxTimeMs3 = rxTimeMs;
                }
                SparseLongArray txTimesMs3 = txTimesMs2;
                SparseLongArray txTimesMs4 = rxTimesMs2;
                long rxTimeMs4 = rxTimeMs3;
                List<UidTraffic> uidTraffic = info.getUidTraffic();
                int numUids = uidTraffic.size();
                long totalTxBytes = 0;
                int i3 = 0;
                long totalRxBytes = 0;
                while (i3 < numUids) {
                    UidTraffic traffic = uidTraffic.get(i3);
                    long rxBytes = traffic.getRxBytes() - this.mLastBluetoothActivityInfo.uidRxBytes.get(traffic.getUid());
                    long txBytes = traffic.getTxBytes() - this.mLastBluetoothActivityInfo.uidTxBytes.get(traffic.getUid());
                    this.mNetworkByteActivityCounters[4].addCountLocked(rxBytes);
                    this.mNetworkByteActivityCounters[5].addCountLocked(txBytes);
                    SparseLongArray rxTimesMs3 = txTimesMs4;
                    Uid u3 = getUidStatsLocked(mapUid(traffic.getUid()), elapsedRealtimeMs, uptimeMs);
                    u3.noteNetworkActivityLocked(4, rxBytes, 0L);
                    u3.noteNetworkActivityLocked(5, txBytes, 0L);
                    totalRxBytes += rxBytes;
                    totalTxBytes += txBytes;
                    i3++;
                    txTimesMs3 = txTimesMs3;
                    uidCount = uidCount;
                    txTimeMs = txTimeMs;
                    txTimesMs4 = rxTimesMs3;
                }
                SparseLongArray rxTimesMs4 = txTimesMs4;
                long txTimeMs2 = txTimeMs;
                SparseLongArray txTimesMs5 = txTimesMs3;
                if ((totalTxBytes != 0 || totalRxBytes != 0) && (leftOverRxTimeMs != 0 || leftOverTxTimeMs != 0)) {
                    int i4 = 0;
                    while (i4 < numUids) {
                        UidTraffic traffic2 = uidTraffic.get(i4);
                        int uid = traffic2.getUid();
                        long rxBytes2 = traffic2.getRxBytes() - this.mLastBluetoothActivityInfo.uidRxBytes.get(uid);
                        long txBytes2 = traffic2.getTxBytes() - this.mLastBluetoothActivityInfo.uidTxBytes.get(uid);
                        List<UidTraffic> uidTraffic2 = uidTraffic;
                        getUidStatsLocked(mapUid(uid), elapsedRealtimeMs, uptimeMs).getOrCreateBluetoothControllerActivityLocked();
                        if (totalRxBytes > 0 && rxBytes2 > 0) {
                            long timeRxMs = (leftOverRxTimeMs * rxBytes2) / totalRxBytes;
                            rxTimesMs4.incrementValue(uid, timeRxMs);
                        }
                        if (totalTxBytes > 0 && txBytes2 > 0) {
                            long timeTxMs = (leftOverTxTimeMs * txBytes2) / totalTxBytes;
                            txTimesMs5.incrementValue(uid, timeTxMs);
                        }
                        i4++;
                        uidTraffic = uidTraffic2;
                    }
                    int i5 = 0;
                    while (i5 < txTimesMs5.size()) {
                        int uid2 = txTimesMs5.keyAt(i5);
                        long myTxTimeMs = txTimesMs5.valueAt(i5);
                        int numUids2 = numUids;
                        SparseLongArray txTimesMs6 = txTimesMs5;
                        getUidStatsLocked(uid2, elapsedRealtimeMs, uptimeMs).getOrCreateBluetoothControllerActivityLocked().getOrCreateTxTimeCounters()[0].increment(myTxTimeMs, elapsedRealtimeMs);
                        if (uidEstimatedConsumptionMah != null) {
                            uidEstimatedConsumptionMah.incrementValue(uid2, this.mBluetoothPowerCalculator.calculatePowerMah(0L, myTxTimeMs, 0L));
                        }
                        i5++;
                        numUids = numUids2;
                        txTimesMs5 = txTimesMs6;
                    }
                    for (int i6 = 0; i6 < rxTimesMs4.size(); i6++) {
                        int uid3 = rxTimesMs4.keyAt(i6);
                        long myRxTimeMs = rxTimesMs4.valueAt(i6);
                        getUidStatsLocked(rxTimesMs4.keyAt(i6), elapsedRealtimeMs, uptimeMs).getOrCreateBluetoothControllerActivityLocked().getOrCreateRxTimeCounter().increment(myRxTimeMs, elapsedRealtimeMs);
                        if (uidEstimatedConsumptionMah != null) {
                            uidEstimatedConsumptionMah.incrementValue(uid3, this.mBluetoothPowerCalculator.calculatePowerMah(myRxTimeMs, 0L, 0L));
                        }
                    }
                }
                this.mBluetoothActivity.getOrCreateRxTimeCounter().increment(rxTimeMs4, elapsedRealtimeMs);
                this.mBluetoothActivity.getOrCreateTxTimeCounters()[0].increment(txTimeMs2, elapsedRealtimeMs);
                this.mBluetoothActivity.getOrCreateIdleTimeCounter().increment(idleTimeMs4, elapsedRealtimeMs);
                double opVolt = this.mPowerProfile.getAveragePower(PowerProfile.POWER_BLUETOOTH_CONTROLLER_OPERATING_VOLTAGE) / 1000.0d;
                if (opVolt != 0.0d) {
                    double controllerMaMs2 = (info.getControllerEnergyUsed() - this.mLastBluetoothActivityInfo.energy) / opVolt;
                    this.mBluetoothActivity.getPowerCounter().addCountLocked((long) controllerMaMs2);
                    controllerMaMs = controllerMaMs2;
                } else {
                    controllerMaMs = 0.0d;
                }
                if (uidEstimatedConsumptionMah != null) {
                    this.mGlobalMeasuredEnergyStats.updateStandardBucket(5, consumedChargeUC);
                    double totalEstimatedMah = this.mBluetoothPowerCalculator.calculatePowerMah(rxTimeMs4, txTimeMs2, idleTimeMs4);
                    distributeEnergyToUidsLocked(5, consumedChargeUC, uidEstimatedConsumptionMah, Math.max(totalEstimatedMah, controllerMaMs / 3600000.0d), elapsedRealtimeMs);
                }
                this.mLastBluetoothActivityInfo.set(info);
                return;
            }
            bluetoothActivityEnergyInfo = info;
        } else {
            bluetoothActivityEnergyInfo = info;
        }
        this.mLastBluetoothActivityInfo.set(bluetoothActivityEnergyInfo);
    }

    public void fillLowPowerStats() {
        if (this.mPlatformIdleStateCallback == null) {
            return;
        }
        RpmStats rpmStats = new RpmStats();
        long now = SystemClock.elapsedRealtime();
        if (now - this.mLastRpmStatsUpdateTimeMs >= 1000) {
            this.mPlatformIdleStateCallback.fillLowPowerStats(rpmStats);
            synchronized (this) {
                this.mTmpRpmStats = rpmStats;
                this.mLastRpmStatsUpdateTimeMs = now;
            }
        }
    }

    public void updateRpmStatsLocked(long elapsedRealtimeUs) {
        RpmStats rpmStats = this.mTmpRpmStats;
        if (rpmStats == null) {
            return;
        }
        for (Map.Entry<String, RpmStats.PowerStatePlatformSleepState> pstate : rpmStats.mPlatformLowPowerStats.entrySet()) {
            String pName = pstate.getKey();
            long pTimeUs = pstate.getValue().mTimeMs * 1000;
            int pCount = pstate.getValue().mCount;
            getRpmTimerLocked(pName).update(pTimeUs, pCount, elapsedRealtimeUs);
            for (Map.Entry<String, RpmStats.PowerStateElement> voter : pstate.getValue().mVoters.entrySet()) {
                String vName = pName + MediaMetrics.SEPARATOR + voter.getKey();
                long vTimeUs = voter.getValue().mTimeMs * 1000;
                int vCount = voter.getValue().mCount;
                getRpmTimerLocked(vName).update(vTimeUs, vCount, elapsedRealtimeUs);
            }
        }
        for (Map.Entry<String, RpmStats.PowerStateSubsystem> subsys : this.mTmpRpmStats.mSubsystemLowPowerStats.entrySet()) {
            String subsysName = subsys.getKey();
            for (Map.Entry<String, RpmStats.PowerStateElement> sstate : subsys.getValue().mStates.entrySet()) {
                String name = subsysName + MediaMetrics.SEPARATOR + sstate.getKey();
                long timeUs = sstate.getValue().mTimeMs * 1000;
                getRpmTimerLocked(name).update(timeUs, sstate.getValue().mCount, elapsedRealtimeUs);
            }
        }
    }

    private void updateCpuMeasuredEnergyStatsLocked(long[] clusterChargeUC, CpuDeltaPowerAccumulator accumulator) {
        int i;
        if (this.mGlobalMeasuredEnergyStats == null) {
            return;
        }
        int numClusters = clusterChargeUC.length;
        long totalCpuChargeUC = 0;
        for (long j : clusterChargeUC) {
            totalCpuChargeUC += j;
        }
        if (totalCpuChargeUC <= 0) {
            return;
        }
        long timestampMs = this.mClock.elapsedRealtime();
        this.mGlobalMeasuredEnergyStats.updateStandardBucket(3, totalCpuChargeUC, timestampMs);
        double[] clusterChargeRatio = new double[numClusters];
        for (int cluster = 0; cluster < numClusters; cluster++) {
            double totalClusterChargeMah = accumulator.totalClusterChargesMah[cluster];
            if (totalClusterChargeMah <= 0.0d) {
                clusterChargeRatio[cluster] = 0.0d;
            } else {
                clusterChargeRatio[cluster] = clusterChargeUC[cluster] / accumulator.totalClusterChargesMah[cluster];
            }
        }
        long uidChargeArraySize = accumulator.perUidCpuClusterChargesMah.size();
        int i2 = 0;
        while (i2 < uidChargeArraySize) {
            Uid uid = accumulator.perUidCpuClusterChargesMah.keyAt(i2);
            double[] uidClusterChargesMah = accumulator.perUidCpuClusterChargesMah.valueAt(i2);
            long uidChargeArraySize2 = uidChargeArraySize;
            int i3 = i2;
            long uidCpuChargeUC = 0;
            for (int cluster2 = 0; cluster2 < numClusters; cluster2++) {
                double uidClusterChargeMah = uidClusterChargesMah[cluster2];
                long uidClusterChargeUC = (long) ((clusterChargeRatio[cluster2] * uidClusterChargeMah) + 0.5d);
                uidCpuChargeUC += uidClusterChargeUC;
            }
            if (uidCpuChargeUC < 0) {
                Slog.wtf(TAG, "Unexpected proportional measured charge (" + uidCpuChargeUC + ") for uid " + uid.mUid);
                i = i3;
            } else {
                i = i3;
                uid.addChargeToStandardBucketLocked(uidCpuChargeUC, 3, timestampMs);
            }
            i2 = i + 1;
            uidChargeArraySize = uidChargeArraySize2;
        }
    }

    public void updateDisplayMeasuredEnergyStatsLocked(long[] chargesUC, int[] screenStates, long elapsedRealtimeMs) {
        int numDisplays;
        long totalScreenOnChargeUC;
        if (this.mGlobalMeasuredEnergyStats == null) {
            return;
        }
        if (this.mPerDisplayBatteryStats.length == screenStates.length) {
            numDisplays = screenStates.length;
        } else {
            int numDisplays2 = this.mDisplayMismatchWtfCount;
            this.mDisplayMismatchWtfCount = numDisplays2 + 1;
            if (numDisplays2 % 100 == 0) {
                Slog.wtf(TAG, "Mismatch between PowerProfile reported display count (" + this.mPerDisplayBatteryStats.length + ") and PowerStatsHal reported display count (" + screenStates.length + NavigationBarInflaterView.KEY_CODE_END);
            }
            DisplayBatteryStats[] displayBatteryStatsArr = this.mPerDisplayBatteryStats;
            numDisplays = displayBatteryStatsArr.length < screenStates.length ? displayBatteryStatsArr.length : screenStates.length;
        }
        int[] oldScreenStates = new int[numDisplays];
        for (int i = 0; i < numDisplays; i++) {
            int screenState = screenStates[i];
            oldScreenStates[i] = this.mPerDisplayBatteryStats[i].screenStateAtLastEnergyMeasurement;
            this.mPerDisplayBatteryStats[i].screenStateAtLastEnergyMeasurement = screenState;
        }
        if (!this.mOnBatteryInternal) {
            return;
        }
        if (this.mIgnoreNextExternalStats) {
            int uidStatsSize = this.mUidStats.size();
            for (int i2 = 0; i2 < uidStatsSize; i2++) {
                this.mUidStats.valueAt(i2).markProcessForegroundTimeUs(elapsedRealtimeMs, false);
            }
            return;
        }
        int i3 = 0;
        long totalScreenOnChargeUC2 = 0;
        while (true) {
            totalScreenOnChargeUC = 0;
            if (i3 >= numDisplays) {
                break;
            }
            long chargeUC = chargesUC[i3];
            if (chargeUC > 0) {
                int powerBucket = MeasuredEnergyStats.getDisplayPowerBucket(oldScreenStates[i3]);
                this.mGlobalMeasuredEnergyStats.updateStandardBucket(powerBucket, chargeUC);
                if (powerBucket == 0) {
                    totalScreenOnChargeUC2 += chargeUC;
                }
            }
            i3++;
        }
        int i4 = (totalScreenOnChargeUC2 > 0L ? 1 : (totalScreenOnChargeUC2 == 0L ? 0 : -1));
        if (i4 <= 0) {
            return;
        }
        SparseDoubleArray fgTimeUsArray = new SparseDoubleArray();
        long j = elapsedRealtimeMs * 1000;
        int uidStatsSize2 = this.mUidStats.size();
        int i5 = 0;
        while (i5 < uidStatsSize2) {
            Uid uid = this.mUidStats.valueAt(i5);
            long fgTimeUs = uid.markProcessForegroundTimeUs(elapsedRealtimeMs, true);
            if (fgTimeUs != totalScreenOnChargeUC) {
                fgTimeUsArray.put(uid.getUid(), fgTimeUs);
            }
            i5++;
            totalScreenOnChargeUC = 0;
        }
        distributeEnergyToUidsLocked(0, totalScreenOnChargeUC2, fgTimeUsArray, 0.0d, elapsedRealtimeMs);
    }

    public void updateGnssMeasuredEnergyStatsLocked(long chargeUC, long elapsedRealtimeMs) {
        MeasuredEnergyStats measuredEnergyStats = this.mGlobalMeasuredEnergyStats;
        if (measuredEnergyStats == null || !this.mOnBatteryInternal || chargeUC <= 0) {
            return;
        }
        if (this.mIgnoreNextExternalStats) {
            int uidStatsSize = this.mUidStats.size();
            for (int i = 0; i < uidStatsSize; i++) {
                this.mUidStats.valueAt(i).markGnssTimeUs(elapsedRealtimeMs);
            }
            return;
        }
        measuredEnergyStats.updateStandardBucket(6, chargeUC);
        SparseDoubleArray gnssTimeUsArray = new SparseDoubleArray();
        int uidStatsSize2 = this.mUidStats.size();
        for (int i2 = 0; i2 < uidStatsSize2; i2++) {
            Uid uid = this.mUidStats.valueAt(i2);
            long gnssTimeUs = uid.markGnssTimeUs(elapsedRealtimeMs);
            if (gnssTimeUs != 0) {
                gnssTimeUsArray.put(uid.getUid(), gnssTimeUs);
            }
        }
        distributeEnergyToUidsLocked(6, chargeUC, gnssTimeUsArray, 0.0d, elapsedRealtimeMs);
    }

    public void updateCustomMeasuredEnergyStatsLocked(int customPowerBucket, long totalChargeUC, SparseLongArray uidCharges) {
        MeasuredEnergyStats measuredEnergyStats = this.mGlobalMeasuredEnergyStats;
        if (measuredEnergyStats == null || !this.mOnBatteryInternal || this.mIgnoreNextExternalStats || totalChargeUC <= 0) {
            return;
        }
        measuredEnergyStats.updateCustomBucket(customPowerBucket, totalChargeUC, this.mClock.elapsedRealtime());
        if (uidCharges == null) {
            return;
        }
        int numUids = uidCharges.size();
        for (int i = 0; i < numUids; i++) {
            int uidInt = mapUid(uidCharges.keyAt(i));
            long uidChargeUC = uidCharges.valueAt(i);
            if (uidChargeUC != 0) {
                Uid uidObj = getAvailableUidStatsLocked(uidInt);
                if (uidObj != null) {
                    uidObj.addChargeToCustomBucketLocked(uidChargeUC, customPowerBucket);
                } else if (!Process.isIsolated(uidInt)) {
                    Slog.w(TAG, "Received measured charge " + totalChargeUC + " for custom bucket " + customPowerBucket + " for non-existent uid " + uidInt);
                }
            }
        }
    }

    private void distributeEnergyToUidsLocked(int bucket, long totalConsumedChargeUC, SparseDoubleArray ratioNumerators, double minRatioDenominator, long timestampMs) {
        double sumRatioNumerators = 0.0d;
        for (int i = ratioNumerators.size() - 1; i >= 0; i--) {
            sumRatioNumerators += ratioNumerators.valueAt(i);
        }
        double ratioDenominator = Math.max(sumRatioNumerators, minRatioDenominator);
        if (ratioDenominator <= 0.0d) {
            return;
        }
        for (int i2 = ratioNumerators.size() - 1; i2 >= 0; i2--) {
            Uid uid = getAvailableUidStatsLocked(ratioNumerators.keyAt(i2));
            double ratioNumerator = ratioNumerators.valueAt(i2);
            uid.addChargeToStandardBucketLocked((long) (((totalConsumedChargeUC * ratioNumerator) / ratioDenominator) + 0.5d), bucket, timestampMs);
        }
    }

    public void updateRailStatsLocked() {
        if (this.mMeasuredEnergyRetriever == null || !this.mTmpRailStats.isRailStatsAvailable()) {
            return;
        }
        this.mMeasuredEnergyRetriever.fillRailDataStats(this.mTmpRailStats);
    }

    public void informThatAllExternalStatsAreFlushed() {
        synchronized (this) {
            this.mIgnoreNextExternalStats = false;
        }
    }

    public void updateKernelWakelocksLocked() {
        updateKernelWakelocksLocked(this.mClock.elapsedRealtime() * 1000);
    }

    public void updateKernelWakelocksLocked(long elapsedRealtimeUs) {
        KernelWakelockStats wakelockStats = this.mKernelWakelockReader.readKernelWakelockStats(this.mTmpWakelockStats);
        if (wakelockStats == null) {
            Slog.w(TAG, "Couldn't get kernel wake lock stats");
            return;
        }
        for (Map.Entry<String, KernelWakelockStats.Entry> ent : wakelockStats.entrySet()) {
            String name = ent.getKey();
            KernelWakelockStats.Entry kws = ent.getValue();
            SamplingTimer kwlt = this.mKernelWakelockStats.get(name);
            if (kwlt == null) {
                kwlt = new SamplingTimer(this.mClock, this.mOnBatteryScreenOffTimeBase);
                this.mKernelWakelockStats.put(name, kwlt);
            }
            kwlt.update(kws.mTotalTime, kws.mCount, elapsedRealtimeUs);
            kwlt.setUpdateVersion(kws.mVersion);
        }
        int numWakelocksSetStale = 0;
        for (Map.Entry<String, SamplingTimer> ent2 : this.mKernelWakelockStats.entrySet()) {
            SamplingTimer st = ent2.getValue();
            if (st.getUpdateVersion() != wakelockStats.kernelWakelockVersion) {
                st.endSample(elapsedRealtimeUs);
                numWakelocksSetStale++;
            }
        }
        if (wakelockStats.isEmpty()) {
            Slog.wtf(TAG, "All kernel wakelocks had time of zero");
        }
        if (numWakelocksSetStale == this.mKernelWakelockStats.size()) {
            Slog.wtf(TAG, "All kernel wakelocks were set stale. new version=" + wakelockStats.kernelWakelockVersion);
        }
    }

    public void updateKernelMemoryBandwidthLocked() {
        updateKernelMemoryBandwidthLocked(this.mClock.elapsedRealtime() * 1000);
    }

    public void updateKernelMemoryBandwidthLocked(long elapsedRealtimeUs) {
        SamplingTimer timer;
        this.mKernelMemoryBandwidthStats.updateStats();
        LongSparseLongArray bandwidthEntries = this.mKernelMemoryBandwidthStats.getBandwidthEntries();
        int bandwidthEntryCount = bandwidthEntries.size();
        for (int i = 0; i < bandwidthEntryCount; i++) {
            int index = this.mKernelMemoryStats.indexOfKey(bandwidthEntries.keyAt(i));
            if (index >= 0) {
                timer = this.mKernelMemoryStats.valueAt(index);
            } else {
                timer = new SamplingTimer(this.mClock, this.mOnBatteryTimeBase);
                this.mKernelMemoryStats.put(bandwidthEntries.keyAt(i), timer);
            }
            timer.update(bandwidthEntries.valueAt(i), 1, elapsedRealtimeUs);
        }
    }

    public boolean isOnBatteryLocked() {
        return this.mOnBatteryTimeBase.isRunning();
    }

    public boolean isOnBatteryScreenOffLocked() {
        return this.mOnBatteryScreenOffTimeBase.isRunning();
    }

    /* loaded from: classes4.dex */
    public static class CpuDeltaPowerAccumulator {
        private final CpuPowerCalculator mCalculator;
        public final double[] totalClusterChargesMah;
        private Uid mCachedUid = null;
        private double[] mUidClusterCache = null;
        public final ArrayMap<Uid, double[]> perUidCpuClusterChargesMah = new ArrayMap<>();

        CpuDeltaPowerAccumulator(CpuPowerCalculator calculator, int nClusters) {
            this.mCalculator = calculator;
            this.totalClusterChargesMah = new double[nClusters];
        }

        public void addCpuClusterDurationsMs(Uid uid, long[] durationsMs) {
            double[] uidChargesMah = getOrCreateUidCpuClusterCharges(uid);
            for (int cluster = 0; cluster < durationsMs.length; cluster++) {
                double estimatedDeltaMah = this.mCalculator.calculatePerCpuClusterPowerMah(cluster, durationsMs[cluster]);
                uidChargesMah[cluster] = uidChargesMah[cluster] + estimatedDeltaMah;
                double[] dArr = this.totalClusterChargesMah;
                dArr[cluster] = dArr[cluster] + estimatedDeltaMah;
            }
        }

        public void addCpuClusterSpeedDurationsMs(Uid uid, int cluster, int speed, long durationsMs) {
            double[] uidChargesMah = getOrCreateUidCpuClusterCharges(uid);
            double estimatedDeltaMah = this.mCalculator.calculatePerCpuFreqPowerMah(cluster, speed, durationsMs);
            uidChargesMah[cluster] = uidChargesMah[cluster] + estimatedDeltaMah;
            double[] dArr = this.totalClusterChargesMah;
            dArr[cluster] = dArr[cluster] + estimatedDeltaMah;
        }

        private double[] getOrCreateUidCpuClusterCharges(Uid uid) {
            if (uid == this.mCachedUid) {
                return this.mUidClusterCache;
            }
            double[] uidChargesMah = this.perUidCpuClusterChargesMah.get(uid);
            if (uidChargesMah == null) {
                uidChargesMah = new double[this.totalClusterChargesMah.length];
                this.perUidCpuClusterChargesMah.put(uid, uidChargesMah);
            }
            this.mCachedUid = uid;
            this.mUidClusterCache = uidChargesMah;
            return uidChargesMah;
        }
    }

    public void updateCpuTimeLocked(boolean onBattery, boolean onBatteryScreenOff, long[] measuredCpuClusterChargeUC) {
        CpuDeltaPowerAccumulator powerAccumulator;
        PowerProfile powerProfile = this.mPowerProfile;
        if (powerProfile == null) {
            return;
        }
        if (this.mCpuFreqs == null) {
            this.mCpuFreqs = this.mCpuUidFreqTimeReader.readFreqs(powerProfile);
        }
        ArrayList<StopwatchTimer> partialTimersToConsider = null;
        if (onBatteryScreenOff) {
            partialTimersToConsider = new ArrayList<>();
            for (int i = this.mPartialTimers.size() - 1; i >= 0; i--) {
                StopwatchTimer timer = this.mPartialTimers.get(i);
                if (timer.mInList && timer.mUid != null && timer.mUid.mUid != 1000) {
                    partialTimersToConsider.add(timer);
                }
            }
        }
        markPartialTimersAsEligible();
        if (!onBattery) {
            this.mCpuUidUserSysTimeReader.readDelta(false, null);
            this.mCpuUidFreqTimeReader.readDelta(false, null);
            this.mNumAllUidCpuTimeReads += 2;
            if (this.mConstants.TRACK_CPU_ACTIVE_CLUSTER_TIME) {
                this.mCpuUidActiveTimeReader.readDelta(false, null);
                this.mCpuUidClusterTimeReader.readDelta(false, null);
                this.mNumAllUidCpuTimeReads += 2;
            }
            for (int cluster = this.mKernelCpuSpeedReaders.length - 1; cluster >= 0; cluster--) {
                this.mKernelCpuSpeedReaders[cluster].readDelta();
            }
            this.mSystemServerCpuThreadReader.readDelta();
            return;
        }
        this.mUserInfoProvider.refreshUserIds();
        SparseLongArray updatedUids = this.mCpuUidFreqTimeReader.perClusterTimesAvailable() ? null : new SparseLongArray();
        MeasuredEnergyStats measuredEnergyStats = this.mGlobalMeasuredEnergyStats;
        if (measuredEnergyStats != null && measuredEnergyStats.isStandardBucketSupported(3) && this.mCpuPowerCalculator != null) {
            if (measuredCpuClusterChargeUC == null) {
                Slog.wtf(TAG, "POWER_BUCKET_CPU supported but no measured Cpu Cluster charge reported on updateCpuTimeLocked!");
                powerAccumulator = null;
            } else {
                int numClusters = this.mPowerProfile.getNumCpuClusters();
                powerAccumulator = new CpuDeltaPowerAccumulator(this.mCpuPowerCalculator, numClusters);
            }
        } else {
            powerAccumulator = null;
        }
        readKernelUidCpuTimesLocked(partialTimersToConsider, updatedUids, onBattery);
        if (updatedUids != null) {
            updateClusterSpeedTimes(updatedUids, onBattery, powerAccumulator);
        }
        readKernelUidCpuFreqTimesLocked(partialTimersToConsider, onBattery, onBatteryScreenOff, powerAccumulator);
        this.mNumAllUidCpuTimeReads += 2;
        if (this.mConstants.TRACK_CPU_ACTIVE_CLUSTER_TIME) {
            readKernelUidCpuActiveTimesLocked(onBattery);
            readKernelUidCpuClusterTimesLocked(onBattery, powerAccumulator);
            this.mNumAllUidCpuTimeReads += 2;
        }
        updateSystemServerThreadStats();
        if (powerAccumulator != null) {
            updateCpuMeasuredEnergyStatsLocked(measuredCpuClusterChargeUC, powerAccumulator);
        }
    }

    public void updateSystemServerThreadStats() {
        SystemServerCpuThreadReader.SystemServiceCpuThreadTimes systemServiceCpuThreadTimes = this.mSystemServerCpuThreadReader.readDelta();
        if (systemServiceCpuThreadTimes == null) {
            return;
        }
        if (this.mBinderThreadCpuTimesUs == null) {
            this.mBinderThreadCpuTimesUs = new LongSamplingCounterArray(this.mOnBatteryTimeBase);
        }
        this.mBinderThreadCpuTimesUs.addCountLocked(systemServiceCpuThreadTimes.binderThreadCpuTimesUs);
    }

    public void markPartialTimersAsEligible() {
        if (ArrayUtils.referenceEquals(this.mPartialTimers, this.mLastPartialTimers)) {
            for (int i = this.mPartialTimers.size() - 1; i >= 0; i--) {
                this.mPartialTimers.get(i).mInList = true;
            }
            return;
        }
        for (int i2 = this.mLastPartialTimers.size() - 1; i2 >= 0; i2--) {
            this.mLastPartialTimers.get(i2).mInList = false;
        }
        this.mLastPartialTimers.clear();
        int numPartialTimers = this.mPartialTimers.size();
        for (int i3 = 0; i3 < numPartialTimers; i3++) {
            StopwatchTimer timer = this.mPartialTimers.get(i3);
            timer.mInList = true;
            this.mLastPartialTimers.add(timer);
        }
    }

    public void updateClusterSpeedTimes(SparseLongArray updatedUids, boolean onBattery, CpuDeltaPowerAccumulator powerAccumulator) {
        int speedsInCluster;
        long elapsedRealtimeMs;
        SparseLongArray sparseLongArray = updatedUids;
        long[][] clusterSpeedTimesMs = new long[this.mKernelCpuSpeedReaders.length];
        int cluster = 0;
        long totalCpuClustersTimeMs = 0;
        while (true) {
            KernelCpuSpeedReader[] kernelCpuSpeedReaderArr = this.mKernelCpuSpeedReaders;
            if (cluster >= kernelCpuSpeedReaderArr.length) {
                break;
            }
            clusterSpeedTimesMs[cluster] = kernelCpuSpeedReaderArr[cluster].readDelta();
            if (clusterSpeedTimesMs[cluster] != null) {
                for (int speed = clusterSpeedTimesMs[cluster].length - 1; speed >= 0; speed--) {
                    totalCpuClustersTimeMs += clusterSpeedTimesMs[cluster][speed];
                }
            }
            cluster++;
        }
        if (totalCpuClustersTimeMs != 0) {
            int i = updatedUids.size();
            long elapsedRealtimeMs2 = this.mClock.elapsedRealtime();
            long uptimeMs = this.mClock.uptimeMillis();
            int cluster2 = 0;
            while (cluster2 < i) {
                int updatedUidsCount = i;
                int updatedUidsCount2 = cluster2;
                Uid u = getUidStatsLocked(sparseLongArray.keyAt(cluster2), elapsedRealtimeMs2, uptimeMs);
                long appCpuTimeUs = sparseLongArray.valueAt(updatedUidsCount2);
                int numClusters = this.mPowerProfile.getNumCpuClusters();
                if (u.mCpuClusterSpeedTimesUs == null || u.mCpuClusterSpeedTimesUs.length != numClusters) {
                    u.mCpuClusterSpeedTimesUs = new LongSamplingCounter[numClusters];
                }
                int cluster3 = 0;
                while (cluster3 < clusterSpeedTimesMs.length) {
                    int speedsInCluster2 = clusterSpeedTimesMs[cluster3].length;
                    int numClusters2 = numClusters;
                    if (u.mCpuClusterSpeedTimesUs[cluster3] == null || speedsInCluster2 != u.mCpuClusterSpeedTimesUs[cluster3].length) {
                        u.mCpuClusterSpeedTimesUs[cluster3] = new LongSamplingCounter[speedsInCluster2];
                    }
                    LongSamplingCounter[] cpuSpeeds = u.mCpuClusterSpeedTimesUs[cluster3];
                    int speed2 = 0;
                    while (speed2 < speedsInCluster2) {
                        if (cpuSpeeds[speed2] != null) {
                            speedsInCluster = speedsInCluster2;
                            elapsedRealtimeMs = elapsedRealtimeMs2;
                        } else {
                            speedsInCluster = speedsInCluster2;
                            elapsedRealtimeMs = elapsedRealtimeMs2;
                            cpuSpeeds[speed2] = new LongSamplingCounter(this.mOnBatteryTimeBase);
                        }
                        long deltaSpeedCount = (clusterSpeedTimesMs[cluster3][speed2] * appCpuTimeUs) / totalCpuClustersTimeMs;
                        long appCpuTimeUs2 = appCpuTimeUs;
                        cpuSpeeds[speed2].addCountLocked(deltaSpeedCount, onBattery);
                        if (powerAccumulator != null) {
                            powerAccumulator.addCpuClusterSpeedDurationsMs(u, cluster3, speed2, deltaSpeedCount);
                        }
                        speed2++;
                        speedsInCluster2 = speedsInCluster;
                        elapsedRealtimeMs2 = elapsedRealtimeMs;
                        appCpuTimeUs = appCpuTimeUs2;
                    }
                    cluster3++;
                    numClusters = numClusters2;
                    appCpuTimeUs = appCpuTimeUs;
                }
                cluster2 = updatedUidsCount2 + 1;
                sparseLongArray = updatedUids;
                i = updatedUidsCount;
            }
        }
    }

    public void readKernelUidCpuTimesLocked(ArrayList<StopwatchTimer> partialTimers, final SparseLongArray updatedUids, final boolean onBattery) {
        this.mTempTotalCpuSystemTimeUs = 0L;
        this.mTempTotalCpuUserTimeUs = 0L;
        final int numWakelocks = partialTimers == null ? 0 : partialTimers.size();
        final long startTimeMs = this.mClock.uptimeMillis();
        final long elapsedRealtimeMs = this.mClock.elapsedRealtime();
        this.mCpuUidUserSysTimeReader.readDelta(false, new KernelCpuUidTimeReader.Callback() { // from class: com.android.internal.os.BatteryStatsImpl$$ExternalSyntheticLambda0
            @Override // com.android.internal.os.KernelCpuUidTimeReader.Callback
            public final void onUidCpuTime(int i, Object obj) {
                BatteryStatsImpl.this.m6797x7deb9e25(elapsedRealtimeMs, startTimeMs, numWakelocks, onBattery, updatedUids, i, (long[]) obj);
            }
        });
        long elapsedTimeMs = this.mClock.uptimeMillis() - startTimeMs;
        if (elapsedTimeMs >= 100) {
            Slog.d(TAG, "Reading cpu stats took " + elapsedTimeMs + "ms");
        }
        if (numWakelocks > 0) {
            this.mTempTotalCpuUserTimeUs = (this.mTempTotalCpuUserTimeUs * 50) / 100;
            this.mTempTotalCpuSystemTimeUs = (this.mTempTotalCpuSystemTimeUs * 50) / 100;
            int i = 0;
            while (i < numWakelocks) {
                StopwatchTimer timer = partialTimers.get(i);
                int userTimeUs = (int) (this.mTempTotalCpuUserTimeUs / (numWakelocks - i));
                int systemTimeUs = (int) (this.mTempTotalCpuSystemTimeUs / (numWakelocks - i));
                timer.mUid.mUserCpuTime.addCountLocked(userTimeUs, onBattery);
                timer.mUid.mSystemCpuTime.addCountLocked(systemTimeUs, onBattery);
                if (updatedUids != null) {
                    int uid = timer.mUid.getUid();
                    updatedUids.put(uid, updatedUids.get(uid, 0L) + userTimeUs + systemTimeUs);
                }
                Uid.Proc proc = timer.mUid.getProcessStatsLocked("*wakelock*");
                proc.addCpuTimeLocked(userTimeUs / 1000, systemTimeUs / 1000, onBattery);
                this.mTempTotalCpuUserTimeUs -= userTimeUs;
                this.mTempTotalCpuSystemTimeUs -= systemTimeUs;
                i++;
                elapsedTimeMs = elapsedTimeMs;
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$readKernelUidCpuTimesLocked$0$com-android-internal-os-BatteryStatsImpl  reason: not valid java name */
    public /* synthetic */ void m6797x7deb9e25(long elapsedRealtimeMs, long startTimeMs, int numWakelocks, boolean onBattery, SparseLongArray updatedUids, int uid, long[] timesUs) {
        long userTimeUs = timesUs[0];
        long systemTimeUs = timesUs[1];
        int uid2 = mapUid(uid);
        if (Process.isIsolated(uid2) || !this.mUserInfoProvider.exists(UserHandle.getUserId(uid2))) {
            return;
        }
        Uid u = getUidStatsLocked(uid2, elapsedRealtimeMs, startTimeMs);
        this.mTempTotalCpuUserTimeUs += userTimeUs;
        this.mTempTotalCpuSystemTimeUs += systemTimeUs;
        StringBuilder sb = null;
        if (numWakelocks > 0) {
            userTimeUs = (userTimeUs * 50) / 100;
            systemTimeUs = (50 * systemTimeUs) / 100;
        }
        if (0 != 0) {
            sb.append("  adding to uid=").append(u.mUid).append(": u=");
            TimeUtils.formatDuration(userTimeUs / 1000, (StringBuilder) null);
            sb.append(" s=");
            TimeUtils.formatDuration(systemTimeUs / 1000, (StringBuilder) null);
            Slog.d(TAG, sb.toString());
        }
        u.mUserCpuTime.addCountLocked(userTimeUs, onBattery);
        u.mSystemCpuTime.addCountLocked(systemTimeUs, onBattery);
        if (updatedUids != null) {
            updatedUids.put(u.getUid(), userTimeUs + systemTimeUs);
        }
    }

    /* JADX WARN: Code restructure failed: missing block: B:19:0x0099, code lost:
        if (r12.mCpuClusterSpeedTimesUs.length != r13) goto L41;
     */
    /*
        Code decompiled incorrectly, please refer to instructions dump.
    */
    public void readKernelUidCpuFreqTimesLocked(ArrayList<StopwatchTimer> partialTimers, final boolean onBattery, final boolean onBatteryScreenOff, final CpuDeltaPowerAccumulator powerAccumulator) {
        int numClusters;
        int speedsInCluster;
        int speed;
        final boolean perClusterTimesAvailable = this.mCpuUidFreqTimeReader.perClusterTimesAvailable();
        final int numWakelocks = partialTimers == null ? 0 : partialTimers.size();
        final int numClusters2 = this.mPowerProfile.getNumCpuClusters();
        this.mWakeLockAllocationsUs = null;
        final long startTimeMs = this.mClock.uptimeMillis();
        final long elapsedRealtimeMs = this.mClock.elapsedRealtime();
        boolean forceRead = powerAccumulator != null;
        int numClusters3 = numClusters2;
        this.mCpuUidFreqTimeReader.readDelta(forceRead, new KernelCpuUidTimeReader.Callback() { // from class: com.android.internal.os.BatteryStatsImpl$$ExternalSyntheticLambda3
            @Override // com.android.internal.os.KernelCpuUidTimeReader.Callback
            public final void onUidCpuTime(int i, Object obj) {
                BatteryStatsImpl.this.m6796x351657dc(elapsedRealtimeMs, startTimeMs, onBattery, onBatteryScreenOff, perClusterTimesAvailable, numClusters2, numWakelocks, powerAccumulator, i, (long[]) obj);
            }
        });
        long elapsedTimeMs = this.mClock.uptimeMillis() - startTimeMs;
        if (elapsedTimeMs >= 100) {
            Slog.d(TAG, "Reading cpu freq times took " + elapsedTimeMs + "ms");
        }
        if (this.mWakeLockAllocationsUs != null) {
            int i = 0;
            while (i < numWakelocks) {
                Uid u = partialTimers.get(i).mUid;
                if (u.mCpuClusterSpeedTimesUs != null) {
                    numClusters = numClusters3;
                } else {
                    numClusters = numClusters3;
                }
                detachIfNotNull(u.mCpuClusterSpeedTimesUs);
                u.mCpuClusterSpeedTimesUs = new LongSamplingCounter[numClusters];
                for (int cluster = 0; cluster < numClusters; cluster++) {
                    int speedsInCluster2 = this.mPowerProfile.getNumSpeedStepsInCpuCluster(cluster);
                    if (u.mCpuClusterSpeedTimesUs[cluster] == null || u.mCpuClusterSpeedTimesUs[cluster].length != speedsInCluster2) {
                        detachIfNotNull(u.mCpuClusterSpeedTimesUs[cluster]);
                        u.mCpuClusterSpeedTimesUs[cluster] = new LongSamplingCounter[speedsInCluster2];
                    }
                    LongSamplingCounter[] cpuTimeUs = u.mCpuClusterSpeedTimesUs[cluster];
                    int speed2 = 0;
                    while (speed2 < speedsInCluster2) {
                        if (cpuTimeUs[speed2] == null) {
                            cpuTimeUs[speed2] = new LongSamplingCounter(this.mOnBatteryTimeBase);
                        }
                        long allocationUs = this.mWakeLockAllocationsUs[cluster][speed2] / (numWakelocks - i);
                        cpuTimeUs[speed2].addCountLocked(allocationUs, onBattery);
                        long[] jArr = this.mWakeLockAllocationsUs[cluster];
                        jArr[speed2] = jArr[speed2] - allocationUs;
                        if (powerAccumulator != null) {
                            speedsInCluster = speedsInCluster2;
                            speed = speed2;
                            powerAccumulator.addCpuClusterSpeedDurationsMs(u, cluster, speed2, allocationUs / 1000);
                        } else {
                            speedsInCluster = speedsInCluster2;
                            speed = speed2;
                        }
                        speed2 = speed + 1;
                        speedsInCluster2 = speedsInCluster;
                    }
                }
                i++;
                numClusters3 = numClusters;
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$readKernelUidCpuFreqTimesLocked$1$com-android-internal-os-BatteryStatsImpl  reason: not valid java name */
    public /* synthetic */ void m6796x351657dc(long elapsedRealtimeMs, long startTimeMs, boolean onBattery, boolean onBatteryScreenOff, boolean perClusterTimesAvailable, int numClusters, int numWakelocks, CpuDeltaPowerAccumulator powerAccumulator, int uid, long[] cpuFreqTimeMs) {
        long appAllocationUs;
        int speed;
        int uid2 = mapUid(uid);
        if (Process.isIsolated(uid2) || !this.mUserInfoProvider.exists(UserHandle.getUserId(uid2))) {
            return;
        }
        Uid u = getUidStatsLocked(uid2, elapsedRealtimeMs, startTimeMs);
        if (u.mCpuFreqTimeMs == null || u.mCpuFreqTimeMs.getSize() != cpuFreqTimeMs.length) {
            detachIfNotNull(u.mCpuFreqTimeMs);
            u.mCpuFreqTimeMs = new LongSamplingCounterArray(this.mOnBatteryTimeBase);
        }
        u.mCpuFreqTimeMs.addCountLocked(cpuFreqTimeMs, onBattery);
        if (u.mScreenOffCpuFreqTimeMs == null || u.mScreenOffCpuFreqTimeMs.getSize() != cpuFreqTimeMs.length) {
            detachIfNotNull(u.mScreenOffCpuFreqTimeMs);
            u.mScreenOffCpuFreqTimeMs = new LongSamplingCounterArray(this.mOnBatteryScreenOffTimeBase);
        }
        u.mScreenOffCpuFreqTimeMs.addCountLocked(cpuFreqTimeMs, onBatteryScreenOff);
        if (perClusterTimesAvailable) {
            if (u.mCpuClusterSpeedTimesUs == null || u.mCpuClusterSpeedTimesUs.length != numClusters) {
                detachIfNotNull(u.mCpuClusterSpeedTimesUs);
                u.mCpuClusterSpeedTimesUs = new LongSamplingCounter[numClusters];
            }
            if (numWakelocks > 0 && this.mWakeLockAllocationsUs == null) {
                this.mWakeLockAllocationsUs = new long[numClusters];
            }
            int freqIndex = 0;
            for (int cluster = 0; cluster < numClusters; cluster++) {
                int speedsInCluster = this.mPowerProfile.getNumSpeedStepsInCpuCluster(cluster);
                if (u.mCpuClusterSpeedTimesUs[cluster] == null || u.mCpuClusterSpeedTimesUs[cluster].length != speedsInCluster) {
                    detachIfNotNull(u.mCpuClusterSpeedTimesUs[cluster]);
                    u.mCpuClusterSpeedTimesUs[cluster] = new LongSamplingCounter[speedsInCluster];
                }
                if (numWakelocks > 0) {
                    long[][] jArr = this.mWakeLockAllocationsUs;
                    if (jArr[cluster] == null) {
                        jArr[cluster] = new long[speedsInCluster];
                    }
                }
                LongSamplingCounter[] cpuTimesUs = u.mCpuClusterSpeedTimesUs[cluster];
                int speed2 = 0;
                while (speed2 < speedsInCluster) {
                    if (cpuTimesUs[speed2] == null) {
                        cpuTimesUs[speed2] = new LongSamplingCounter(this.mOnBatteryTimeBase);
                    }
                    long[][] jArr2 = this.mWakeLockAllocationsUs;
                    if (jArr2 != null) {
                        long appAllocationUs2 = ((cpuFreqTimeMs[freqIndex] * 1000) * 50) / 100;
                        long[] jArr3 = jArr2[cluster];
                        jArr3[speed2] = jArr3[speed2] + ((cpuFreqTimeMs[freqIndex] * 1000) - appAllocationUs2);
                        appAllocationUs = appAllocationUs2;
                    } else {
                        long appAllocationUs3 = cpuFreqTimeMs[freqIndex];
                        appAllocationUs = appAllocationUs3 * 1000;
                    }
                    cpuTimesUs[speed2].addCountLocked(appAllocationUs, onBattery);
                    if (powerAccumulator != null) {
                        speed = speed2;
                        powerAccumulator.addCpuClusterSpeedDurationsMs(u, cluster, speed2, appAllocationUs / 1000);
                    } else {
                        speed = speed2;
                    }
                    freqIndex++;
                    speed2 = speed + 1;
                }
            }
        }
    }

    public void readKernelUidCpuActiveTimesLocked(boolean onBattery) {
        final long startTimeMs = this.mClock.uptimeMillis();
        final long elapsedRealtimeMs = this.mClock.elapsedRealtime();
        this.mCpuUidActiveTimeReader.readAbsolute(new KernelCpuUidTimeReader.Callback() { // from class: com.android.internal.os.BatteryStatsImpl$$ExternalSyntheticLambda2
            @Override // com.android.internal.os.KernelCpuUidTimeReader.Callback
            public final void onUidCpuTime(int i, Object obj) {
                BatteryStatsImpl.this.m6794xdedee509(elapsedRealtimeMs, startTimeMs, i, (Long) obj);
            }
        });
        long elapsedTimeMs = this.mClock.uptimeMillis() - startTimeMs;
        if (elapsedTimeMs >= 100) {
            Slog.d(TAG, "Reading cpu active times took " + elapsedTimeMs + "ms");
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$readKernelUidCpuActiveTimesLocked$2$com-android-internal-os-BatteryStatsImpl  reason: not valid java name */
    public /* synthetic */ void m6794xdedee509(long elapsedRealtimeMs, long startTimeMs, int uid, Long cpuActiveTimesMs) {
        Uid.ChildUid childUid;
        int parentUid = mapUid(uid);
        if (Process.isIsolated(parentUid) || !this.mUserInfoProvider.exists(UserHandle.getUserId(uid))) {
            return;
        }
        Uid u = getUidStatsLocked(parentUid, elapsedRealtimeMs, startTimeMs);
        if (parentUid == uid) {
            u.getCpuActiveTimeCounter().update(cpuActiveTimesMs.longValue(), elapsedRealtimeMs);
            return;
        }
        SparseArray<Uid.ChildUid> childUids = u.mChildUids;
        if (childUids != null && (childUid = childUids.get(uid)) != null) {
            long delta = childUid.cpuActiveCounter.update(cpuActiveTimesMs.longValue(), elapsedRealtimeMs);
            u.getCpuActiveTimeCounter().increment(delta, elapsedRealtimeMs);
        }
    }

    public void readKernelUidCpuClusterTimesLocked(final boolean onBattery, final CpuDeltaPowerAccumulator powerAccumulator) {
        final long startTimeMs = this.mClock.uptimeMillis();
        final long elapsedRealtimeMs = this.mClock.elapsedRealtime();
        boolean forceRead = powerAccumulator != null;
        this.mCpuUidClusterTimeReader.readDelta(forceRead, new KernelCpuUidTimeReader.Callback() { // from class: com.android.internal.os.BatteryStatsImpl$$ExternalSyntheticLambda1
            @Override // com.android.internal.os.KernelCpuUidTimeReader.Callback
            public final void onUidCpuTime(int i, Object obj) {
                BatteryStatsImpl.this.m6795x3d98b74c(elapsedRealtimeMs, startTimeMs, onBattery, powerAccumulator, i, (long[]) obj);
            }
        });
        long elapsedTimeMs = this.mClock.uptimeMillis() - startTimeMs;
        if (elapsedTimeMs >= 100) {
            Slog.d(TAG, "Reading cpu cluster times took " + elapsedTimeMs + "ms");
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$readKernelUidCpuClusterTimesLocked$3$com-android-internal-os-BatteryStatsImpl  reason: not valid java name */
    public /* synthetic */ void m6795x3d98b74c(long elapsedRealtimeMs, long startTimeMs, boolean onBattery, CpuDeltaPowerAccumulator powerAccumulator, int uid, long[] cpuClusterTimesMs) {
        int uid2 = mapUid(uid);
        if (Process.isIsolated(uid2) || !this.mUserInfoProvider.exists(UserHandle.getUserId(uid2))) {
            return;
        }
        Uid u = getUidStatsLocked(uid2, elapsedRealtimeMs, startTimeMs);
        u.mCpuClusterTimesMs.addCountLocked(cpuClusterTimesMs, onBattery);
        if (powerAccumulator != null) {
            powerAccumulator.addCpuClusterDurationsMs(u, cpuClusterTimesMs);
        }
    }

    boolean setChargingLocked(boolean charging) {
        this.mHandler.removeCallbacks(this.mDeferSetCharging);
        if (this.mCharging != charging) {
            this.mCharging = charging;
            if (charging) {
                this.mHistoryCur.states2 |= 16777216;
            } else {
                this.mHistoryCur.states2 &= -16777217;
            }
            this.mHandler.sendEmptyMessage(3);
            return true;
        }
        return false;
    }

    public void onSystemReady() {
        this.mSystemReady = true;
    }

    /* JADX DEBUG: Multi-variable search result rejected for r23v0, resolved type: com.android.internal.os.BatteryStatsImpl */
    /* JADX WARN: Multi-variable type inference failed */
    /* JADX WARN: Type inference failed for: r8v2 */
    /* JADX WARN: Type inference failed for: r8v3, types: [boolean, int] */
    /* JADX WARN: Type inference failed for: r8v5 */
    /* JADX WARN: Type inference failed for: r8v6 */
    protected void setOnBatteryLocked(long mSecRealtime, long mSecUptime, boolean onBattery, int oldStatus, int level, int chargeUah) {
        ?? r8;
        boolean doWrite = false;
        Message m = this.mHandler.obtainMessage(2);
        m.arg1 = onBattery ? 1 : 0;
        this.mHandler.sendMessage(m);
        long uptimeUs = mSecUptime * 1000;
        long realtimeUs = mSecRealtime * 1000;
        int screenState = this.mScreenState;
        if (!onBattery) {
            this.mLastChargingStateLevel = level;
            this.mOnBatteryInternal = false;
            this.mOnBattery = false;
            pullPendingStateUpdatesLocked();
            this.mHistoryCur.batteryLevel = (byte) level;
            this.mHistoryCur.states |= 524288;
            addHistoryRecordLocked(mSecRealtime, mSecUptime);
            this.mDischargePlugLevel = level;
            this.mDischargeCurrentLevel = level;
            int i = this.mDischargeUnplugLevel;
            if (level < i) {
                this.mLowDischargeAmountSinceCharge += (i - level) - 1;
                this.mHighDischargeAmountSinceCharge += i - level;
            }
            updateDischargeScreenLevelsLocked(screenState, screenState);
            updateTimeBasesLocked(false, screenState, uptimeUs, realtimeUs);
            this.mChargeStepTracker.init();
            this.mLastChargeStepLevel = level;
            this.mMaxChargeStepLevel = level;
            this.mInitStepMode = this.mCurStepMode;
            this.mModStepMode = 0;
        } else {
            boolean reset = false;
            if (!this.mNoAutoReset && this.mSystemReady) {
                if (oldStatus == 5 || level >= 90 || ((this.mDischargeCurrentLevel < 20 && level >= 80) || getHighDischargeAmountSinceCharge() >= 200)) {
                    Slog.i(TAG, "Resetting battery stats: level=" + level + " status=" + oldStatus + " dischargeLevel=" + this.mDischargeCurrentLevel + " lowAmount=" + getLowDischargeAmountSinceCharge() + " highAmount=" + getHighDischargeAmountSinceCharge());
                    if (getLowDischargeAmountSinceCharge() >= 20) {
                        long startTimeMs = SystemClock.uptimeMillis();
                        final Parcel parcel = Parcel.obtain();
                        writeSummaryToParcel(parcel, true);
                        final long initialTimeMs = SystemClock.uptimeMillis() - startTimeMs;
                        BackgroundThread.getHandler().post(new Runnable() { // from class: com.android.internal.os.BatteryStatsImpl.5
                            @Override // java.lang.Runnable
                            public void run() {
                                Parcel parcel2;
                                synchronized (BatteryStatsImpl.this.mCheckinFile) {
                                    long startTimeMs2 = SystemClock.uptimeMillis();
                                    FileOutputStream stream = null;
                                    try {
                                        stream = BatteryStatsImpl.this.mCheckinFile.startWrite();
                                        stream.write(parcel.marshall());
                                        stream.flush();
                                        BatteryStatsImpl.this.mCheckinFile.finishWrite(stream);
                                        EventLogTags.writeCommitSysConfigFile("batterystats-checkin", (initialTimeMs + SystemClock.uptimeMillis()) - startTimeMs2);
                                        parcel2 = parcel;
                                    } catch (IOException e) {
                                        Slog.w("BatteryStats", "Error writing checkin battery statistics", e);
                                        BatteryStatsImpl.this.mCheckinFile.failWrite(stream);
                                        parcel2 = parcel;
                                    }
                                    parcel2.recycle();
                                }
                            }
                        });
                    }
                    doWrite = true;
                    r8 = 0;
                    resetAllStatsLocked(mSecUptime, mSecRealtime, 3);
                    if (chargeUah > 0 && level > 0) {
                        this.mEstimatedBatteryCapacityMah = (int) ((chargeUah / 1000) / (level / 100.0d));
                    }
                    this.mDischargeStartLevel = level;
                    reset = true;
                    this.mDischargeStepTracker.init();
                } else {
                    r8 = 0;
                }
            } else {
                r8 = 0;
            }
            if (this.mCharging) {
                setChargingLocked(r8);
            }
            this.mLastChargingStateLevel = level;
            this.mOnBatteryInternal = true;
            this.mOnBattery = true;
            this.mLastDischargeStepLevel = level;
            this.mMinDischargeStepLevel = level;
            this.mDischargeStepTracker.clearTime();
            this.mDailyDischargeStepTracker.clearTime();
            this.mInitStepMode = this.mCurStepMode;
            this.mModStepMode = r8;
            pullPendingStateUpdatesLocked();
            this.mHistoryCur.batteryLevel = (byte) level;
            this.mHistoryCur.states &= -524289;
            if (reset) {
                this.mRecordingHistory = true;
                startRecordingHistory(mSecRealtime, mSecUptime, reset);
            }
            addHistoryRecordLocked(mSecRealtime, mSecUptime);
            this.mDischargeUnplugLevel = level;
            this.mDischargeCurrentLevel = level;
            if (Display.isOnState(screenState)) {
                this.mDischargeScreenOnUnplugLevel = level;
                this.mDischargeScreenDozeUnplugLevel = r8;
                this.mDischargeScreenOffUnplugLevel = r8;
            } else if (Display.isDozeState(screenState)) {
                this.mDischargeScreenOnUnplugLevel = r8;
                this.mDischargeScreenDozeUnplugLevel = level;
                this.mDischargeScreenOffUnplugLevel = r8;
            } else {
                this.mDischargeScreenOnUnplugLevel = r8;
                this.mDischargeScreenDozeUnplugLevel = r8;
                this.mDischargeScreenOffUnplugLevel = level;
            }
            this.mDischargeAmountScreenOn = r8;
            this.mDischargeAmountScreenDoze = r8;
            this.mDischargeAmountScreenOff = r8;
            updateTimeBasesLocked(true, screenState, uptimeUs, realtimeUs);
        }
        if ((doWrite || this.mLastWriteTimeMs + 60000 < mSecRealtime) && this.mStatsFile != null && this.mBatteryStatsHistory.getActiveFile() != null) {
            writeAsyncLocked();
        }
    }

    private void startRecordingHistory(long elapsedRealtimeMs, long uptimeMs, boolean reset) {
        this.mRecordingHistory = true;
        this.mHistoryCur.currentTime = this.mClock.currentTimeMillis();
        addHistoryBufferLocked(elapsedRealtimeMs, reset ? (byte) 7 : (byte) 5, this.mHistoryCur);
        this.mHistoryCur.currentTime = 0L;
        if (reset) {
            initActiveHistoryEventsLocked(elapsedRealtimeMs, uptimeMs);
        }
    }

    private void recordCurrentTimeChangeLocked(long currentTimeMs, long elapsedRealtimeMs, long uptimeMs) {
        if (this.mRecordingHistory) {
            this.mHistoryCur.currentTime = currentTimeMs;
            addHistoryBufferLocked(elapsedRealtimeMs, (byte) 5, this.mHistoryCur);
            this.mHistoryCur.currentTime = 0L;
        }
    }

    private void recordShutdownLocked(long currentTimeMs, long elapsedRealtimeMs) {
        if (this.mRecordingHistory) {
            this.mHistoryCur.currentTime = currentTimeMs;
            addHistoryBufferLocked(elapsedRealtimeMs, (byte) 8, this.mHistoryCur);
            this.mHistoryCur.currentTime = 0L;
        }
    }

    private void scheduleSyncExternalStatsLocked(String reason, int updateFlags) {
        ExternalStatsSync externalStatsSync = this.mExternalSync;
        if (externalStatsSync != null) {
            externalStatsSync.scheduleSync(reason, updateFlags);
        }
    }

    public void setBatteryStateLocked(int status, int health, int plugType, int level, int temp, int voltageMv, int chargeUah, int chargeFullUah, long chargeTimeToFullSeconds) {
        setBatteryStateLocked(status, health, plugType, level, temp, voltageMv, chargeUah, chargeFullUah, chargeTimeToFullSeconds, this.mClock.elapsedRealtime(), this.mClock.uptimeMillis(), this.mClock.currentTimeMillis());
    }

    public void setBatteryStateLocked(int status, int health, int plugType, int level, int temp, int voltageMv, int chargeUah, int chargeFullUah, long chargeTimeToFullSeconds, long elapsedRealtimeMs, long uptimeMs, long currentTimeMs) {
        int i;
        int oldStatus;
        boolean onBattery;
        boolean onBattery2;
        boolean z;
        int temp2 = Math.max(0, temp);
        reportChangesToStatsLog(this.mHaveBatteryLevel ? this.mHistoryCur : null, status, plugType, level);
        boolean onBattery3 = isOnBattery(plugType, status);
        if (!this.mHaveBatteryLevel) {
            this.mHaveBatteryLevel = true;
            if (onBattery3 == this.mOnBattery) {
                if (onBattery3) {
                    this.mHistoryCur.states &= -524289;
                } else {
                    this.mHistoryCur.states |= 524288;
                }
            }
            this.mHistoryCur.states2 |= 16777216;
            this.mHistoryCur.batteryStatus = (byte) status;
            this.mHistoryCur.batteryLevel = (byte) level;
            this.mHistoryCur.batteryChargeUah = chargeUah;
            this.mLastDischargeStepLevel = level;
            this.mLastChargeStepLevel = level;
            this.mMinDischargeStepLevel = level;
            this.mMaxChargeStepLevel = level;
            this.mLastChargingStateLevel = level;
        } else if (this.mCurrentBatteryLevel != level || this.mOnBattery != onBattery3) {
            recordDailyStatsIfNeededLocked(level >= 100 && onBattery3, currentTimeMs);
        }
        int oldStatus2 = this.mHistoryCur.batteryStatus;
        if (onBattery3) {
            this.mDischargeCurrentLevel = level;
            if (this.mRecordingHistory) {
                i = 1;
                oldStatus = oldStatus2;
                onBattery = onBattery3;
            } else {
                this.mRecordingHistory = true;
                i = 1;
                oldStatus = oldStatus2;
                onBattery = onBattery3;
                startRecordingHistory(elapsedRealtimeMs, uptimeMs, true);
            }
        } else {
            i = 1;
            oldStatus = oldStatus2;
            onBattery = onBattery3;
            if (level < 96 && status != 1 && !this.mRecordingHistory) {
                this.mRecordingHistory = true;
                startRecordingHistory(elapsedRealtimeMs, uptimeMs, true);
            }
        }
        this.mBatteryVoltageMv = voltageMv;
        this.mCurrentBatteryLevel = level;
        if (this.mDischargePlugLevel < 0) {
            this.mDischargePlugLevel = level;
        }
        boolean onBattery4 = onBattery;
        if (onBattery4 != this.mOnBattery) {
            this.mHistoryCur.batteryLevel = (byte) level;
            this.mHistoryCur.batteryStatus = (byte) status;
            this.mHistoryCur.batteryHealth = (byte) health;
            this.mHistoryCur.batteryPlugType = (byte) plugType;
            this.mHistoryCur.batteryTemperature = (short) temp2;
            this.mHistoryCur.batteryVoltage = (char) voltageMv;
            if (chargeUah < this.mHistoryCur.batteryChargeUah) {
                long chargeDiff = this.mHistoryCur.batteryChargeUah - chargeUah;
                this.mDischargeCounter.addCountLocked(chargeDiff);
                this.mDischargeScreenOffCounter.addCountLocked(chargeDiff);
                if (Display.isDozeState(this.mScreenState)) {
                    this.mDischargeScreenDozeCounter.addCountLocked(chargeDiff);
                }
                int i2 = this.mDeviceIdleMode;
                if (i2 == i) {
                    this.mDischargeLightDozeCounter.addCountLocked(chargeDiff);
                } else if (i2 == 2) {
                    this.mDischargeDeepDozeCounter.addCountLocked(chargeDiff);
                }
            }
            this.mHistoryCur.batteryChargeUah = chargeUah;
            onBattery2 = onBattery4;
            int temp3 = oldStatus;
            setOnBatteryLocked(elapsedRealtimeMs, uptimeMs, onBattery4, temp3, level, chargeUah);
            z = true;
        } else {
            onBattery2 = onBattery4;
            boolean changed = false;
            if (this.mHistoryCur.batteryLevel != level) {
                this.mHistoryCur.batteryLevel = (byte) level;
                changed = true;
                this.mExternalSync.scheduleSyncDueToBatteryLevelChange(this.mConstants.BATTERY_LEVEL_COLLECTION_DELAY_MS);
            }
            if (this.mHistoryCur.batteryStatus != status) {
                this.mHistoryCur.batteryStatus = (byte) status;
                changed = true;
            }
            if (this.mHistoryCur.batteryHealth != health) {
                this.mHistoryCur.batteryHealth = (byte) health;
                changed = true;
            }
            if (this.mHistoryCur.batteryPlugType != plugType) {
                this.mHistoryCur.batteryPlugType = (byte) plugType;
                changed = true;
            }
            if (temp2 >= this.mHistoryCur.batteryTemperature + 10 || temp2 <= this.mHistoryCur.batteryTemperature - 10) {
                this.mHistoryCur.batteryTemperature = (short) temp2;
                changed = true;
            }
            if (voltageMv > this.mHistoryCur.batteryVoltage + 20 || voltageMv < this.mHistoryCur.batteryVoltage - 20) {
                this.mHistoryCur.batteryVoltage = (char) voltageMv;
                changed = true;
            }
            if (chargeUah >= this.mHistoryCur.batteryChargeUah + 10 || chargeUah <= this.mHistoryCur.batteryChargeUah - 10) {
                if (chargeUah >= this.mHistoryCur.batteryChargeUah) {
                    z = true;
                } else {
                    long chargeDiff2 = this.mHistoryCur.batteryChargeUah - chargeUah;
                    this.mDischargeCounter.addCountLocked(chargeDiff2);
                    this.mDischargeScreenOffCounter.addCountLocked(chargeDiff2);
                    if (Display.isDozeState(this.mScreenState)) {
                        this.mDischargeScreenDozeCounter.addCountLocked(chargeDiff2);
                    }
                    int i3 = this.mDeviceIdleMode;
                    z = true;
                    if (i3 == 1) {
                        this.mDischargeLightDozeCounter.addCountLocked(chargeDiff2);
                    } else if (i3 == 2) {
                        this.mDischargeDeepDozeCounter.addCountLocked(chargeDiff2);
                    }
                }
                this.mHistoryCur.batteryChargeUah = chargeUah;
                changed = true;
            } else {
                z = true;
            }
            long modeBits = (this.mInitStepMode << 48) | (this.mModStepMode << 56) | ((level & 255) << 40);
            if (onBattery2) {
                changed |= setChargingLocked(false);
                int i4 = this.mLastDischargeStepLevel;
                if (i4 != level && this.mMinDischargeStepLevel > level) {
                    this.mDischargeStepTracker.addLevelSteps(i4 - level, modeBits, elapsedRealtimeMs);
                    this.mDailyDischargeStepTracker.addLevelSteps(this.mLastDischargeStepLevel - level, modeBits, elapsedRealtimeMs);
                    this.mLastDischargeStepLevel = level;
                    this.mMinDischargeStepLevel = level;
                    this.mInitStepMode = this.mCurStepMode;
                    this.mModStepMode = 0;
                }
            } else {
                if (level >= 90) {
                    changed |= setChargingLocked(z);
                } else if (this.mCharging) {
                    if (this.mLastChargeStepLevel > level) {
                        changed |= setChargingLocked(false);
                    }
                } else {
                    int i5 = this.mLastChargeStepLevel;
                    if (i5 < level) {
                        if (!this.mHandler.hasCallbacks(this.mDeferSetCharging)) {
                            this.mHandler.postDelayed(this.mDeferSetCharging, this.mConstants.BATTERY_CHARGED_DELAY_MS);
                        }
                    } else if (i5 > level) {
                        this.mHandler.removeCallbacks(this.mDeferSetCharging);
                    }
                }
                int i6 = this.mLastChargeStepLevel;
                if (i6 != level && this.mMaxChargeStepLevel < level) {
                    this.mChargeStepTracker.addLevelSteps(level - i6, modeBits, elapsedRealtimeMs);
                    this.mDailyChargeStepTracker.addLevelSteps(level - this.mLastChargeStepLevel, modeBits, elapsedRealtimeMs);
                    this.mMaxChargeStepLevel = level;
                    this.mInitStepMode = this.mCurStepMode;
                    this.mModStepMode = 0;
                }
                this.mLastChargeStepLevel = level;
            }
            if (changed) {
                addHistoryRecordLocked(elapsedRealtimeMs, uptimeMs);
            }
        }
        if (!onBattery2 && (status == 5 || status == z)) {
            this.mRecordingHistory = false;
        }
        this.mLastLearnedBatteryCapacityUah = chargeFullUah;
        int i7 = this.mMinLearnedBatteryCapacityUah;
        if (i7 != -1) {
            this.mMinLearnedBatteryCapacityUah = Math.min(i7, chargeFullUah);
        } else {
            this.mMinLearnedBatteryCapacityUah = chargeFullUah;
        }
        this.mMaxLearnedBatteryCapacityUah = Math.max(this.mMaxLearnedBatteryCapacityUah, chargeFullUah);
        this.mBatteryTimeToFullSeconds = chargeTimeToFullSeconds;
    }

    public static boolean isOnBattery(int plugType, int status) {
        return plugType == 0 && status != 1;
    }

    private void reportChangesToStatsLog(BatteryStats.HistoryItem recentPast, int status, int plugType, int level) {
        if (recentPast == null || recentPast.batteryStatus != status) {
            FrameworkStatsLog.write(31, status);
        }
        if (recentPast == null || recentPast.batteryPlugType != plugType) {
            FrameworkStatsLog.write(32, plugType);
        }
        if (recentPast == null || recentPast.batteryLevel != level) {
            FrameworkStatsLog.write(30, level);
        }
    }

    public long getAwakeTimeBattery() {
        return getBatteryUptimeLocked(this.mClock.uptimeMillis());
    }

    public long getAwakeTimePlugged() {
        return (this.mClock.uptimeMillis() * 1000) - getAwakeTimeBattery();
    }

    @Override // android.os.BatteryStats
    public long computeUptime(long curTimeUs, int which) {
        return this.mUptimeUs + (curTimeUs - this.mUptimeStartUs);
    }

    @Override // android.os.BatteryStats
    public long computeRealtime(long curTimeUs, int which) {
        return this.mRealtimeUs + (curTimeUs - this.mRealtimeStartUs);
    }

    @Override // android.os.BatteryStats
    public long computeBatteryUptime(long curTimeUs, int which) {
        return this.mOnBatteryTimeBase.computeUptime(curTimeUs, which);
    }

    @Override // android.os.BatteryStats
    public long computeBatteryRealtime(long curTimeUs, int which) {
        return this.mOnBatteryTimeBase.computeRealtime(curTimeUs, which);
    }

    @Override // android.os.BatteryStats
    public long computeBatteryScreenOffUptime(long curTimeUs, int which) {
        return this.mOnBatteryScreenOffTimeBase.computeUptime(curTimeUs, which);
    }

    @Override // android.os.BatteryStats
    public long computeBatteryScreenOffRealtime(long curTimeUs, int which) {
        return this.mOnBatteryScreenOffTimeBase.computeRealtime(curTimeUs, which);
    }

    private long computeTimePerLevel(long[] steps, int numSteps) {
        if (numSteps <= 0) {
            return -1L;
        }
        long total = 0;
        for (int i = 0; i < numSteps; i++) {
            total += steps[i] & BatteryStats.STEP_LEVEL_TIME_MASK;
        }
        return total / numSteps;
    }

    @Override // android.os.BatteryStats
    public long computeBatteryTimeRemaining(long curTime) {
        if (this.mOnBattery && this.mDischargeStepTracker.mNumStepDurations >= 1) {
            long msPerLevel = this.mDischargeStepTracker.computeTimePerLevel();
            if (msPerLevel <= 0) {
                return -1L;
            }
            return this.mCurrentBatteryLevel * msPerLevel * 1000;
        }
        return -1L;
    }

    @Override // android.os.BatteryStats
    public BatteryStats.LevelStepTracker getDischargeLevelStepTracker() {
        return this.mDischargeStepTracker;
    }

    @Override // android.os.BatteryStats
    public BatteryStats.LevelStepTracker getDailyDischargeLevelStepTracker() {
        return this.mDailyDischargeStepTracker;
    }

    @Override // android.os.BatteryStats
    public long computeChargeTimeRemaining(long curTime) {
        if (this.mOnBattery) {
            return -1L;
        }
        long j = this.mBatteryTimeToFullSeconds;
        if (j >= 0) {
            return j * TimeUtils.NANOS_PER_MS;
        }
        if (this.mChargeStepTracker.mNumStepDurations < 1) {
            return -1L;
        }
        long msPerLevel = this.mChargeStepTracker.computeTimePerLevel();
        if (msPerLevel <= 0) {
            return -1L;
        }
        return (100 - this.mCurrentBatteryLevel) * msPerLevel * 1000;
    }

    public CellularBatteryStats getCellularBatteryStats() {
        long rawRealTimeUs = SystemClock.elapsedRealtime() * 1000;
        BatteryStats.ControllerActivityCounter counter = getModemControllerActivity();
        long sleepTimeMs = counter.getSleepTimeCounter().getCountLocked(0);
        long idleTimeMs = counter.getIdleTimeCounter().getCountLocked(0);
        long rxTimeMs = counter.getRxTimeCounter().getCountLocked(0);
        long energyConsumedMaMs = counter.getPowerCounter().getCountLocked(0);
        long monitoredRailChargeConsumedMaMs = counter.getMonitoredRailChargeConsumedMaMs().getCountLocked(0);
        long[] timeInRatMs = new long[BatteryStats.NUM_DATA_CONNECTION_TYPES];
        for (int i = 0; i < timeInRatMs.length; i++) {
            timeInRatMs[i] = getPhoneDataConnectionTime(i, rawRealTimeUs, 0) / 1000;
        }
        int i2 = CellSignalStrength.getNumSignalStrengthLevels();
        long[] timeInRxSignalStrengthLevelMs = new long[i2 + 1];
        for (int i3 = 0; i3 < timeInRxSignalStrengthLevelMs.length; i3++) {
            timeInRxSignalStrengthLevelMs[i3] = getPhoneSignalStrengthTime(i3, rawRealTimeUs, 0) / 1000;
        }
        int i4 = ModemActivityInfo.getNumTxPowerLevels();
        long[] txTimeMs = new long[Math.min(i4, counter.getTxTimeCounters().length)];
        long totalTxTimeMs = 0;
        for (int i5 = 0; i5 < txTimeMs.length; i5++) {
            txTimeMs[i5] = counter.getTxTimeCounters()[i5].getCountLocked(0);
            totalTxTimeMs += txTimeMs[i5];
        }
        return new CellularBatteryStats(computeBatteryRealtime(rawRealTimeUs, 0) / 1000, getMobileRadioActiveTime(rawRealTimeUs, 0) / 1000, getNetworkActivityPackets(1, 0), getNetworkActivityBytes(1, 0), getNetworkActivityPackets(0, 0), getNetworkActivityBytes(0, 0), sleepTimeMs, idleTimeMs, rxTimeMs, Long.valueOf(energyConsumedMaMs), timeInRatMs, timeInRxSignalStrengthLevelMs, txTimeMs, monitoredRailChargeConsumedMaMs);
    }

    public WifiBatteryStats getWifiBatteryStats() {
        long rawRealTimeUs = SystemClock.elapsedRealtime() * 1000;
        BatteryStats.ControllerActivityCounter counter = getWifiControllerActivity();
        long idleTimeMs = counter.getIdleTimeCounter().getCountLocked(0);
        long scanTimeMs = counter.getScanTimeCounter().getCountLocked(0);
        long rxTimeMs = counter.getRxTimeCounter().getCountLocked(0);
        long txTimeMs = counter.getTxTimeCounters()[0].getCountLocked(0);
        long totalControllerActivityTimeMs = computeBatteryRealtime(SystemClock.elapsedRealtime() * 1000, 0) / 1000;
        long sleepTimeMs = totalControllerActivityTimeMs - ((idleTimeMs + rxTimeMs) + txTimeMs);
        long energyConsumedMaMs = counter.getPowerCounter().getCountLocked(0);
        long monitoredRailChargeConsumedMaMs = counter.getMonitoredRailChargeConsumedMaMs().getCountLocked(0);
        long numAppScanRequest = 0;
        for (int i = 0; i < this.mUidStats.size(); i++) {
            numAppScanRequest += this.mUidStats.valueAt(i).mWifiScanTimer.getCountLocked(0);
        }
        long[] timeInStateMs = new long[8];
        for (int i2 = 0; i2 < 8; i2++) {
            timeInStateMs[i2] = getWifiStateTime(i2, rawRealTimeUs, 0) / 1000;
        }
        long[] timeInSupplStateMs = new long[13];
        for (int i3 = 0; i3 < 13; i3++) {
            timeInSupplStateMs[i3] = getWifiSupplStateTime(i3, rawRealTimeUs, 0) / 1000;
        }
        long[] timeSignalStrengthTimeMs = new long[5];
        for (int i4 = 0; i4 < 5; i4++) {
            timeSignalStrengthTimeMs[i4] = getWifiSignalStrengthTime(i4, rawRealTimeUs, 0) / 1000;
        }
        return new WifiBatteryStats(computeBatteryRealtime(rawRealTimeUs, 0) / 1000, getWifiActiveTime(rawRealTimeUs, 0) / 1000, getNetworkActivityPackets(3, 0), getNetworkActivityBytes(3, 0), getNetworkActivityPackets(2, 0), getNetworkActivityBytes(2, 0), sleepTimeMs, scanTimeMs, idleTimeMs, rxTimeMs, txTimeMs, energyConsumedMaMs, numAppScanRequest, timeInStateMs, timeSignalStrengthTimeMs, timeInSupplStateMs, monitoredRailChargeConsumedMaMs);
    }

    public GpsBatteryStats getGpsBatteryStats() {
        GpsBatteryStats s = new GpsBatteryStats();
        long rawRealTimeUs = SystemClock.elapsedRealtime() * 1000;
        s.setLoggingDurationMs(computeBatteryRealtime(rawRealTimeUs, 0) / 1000);
        s.setEnergyConsumedMaMs(getGpsBatteryDrainMaMs());
        long[] time = new long[this.mGpsSignalQualityTimer.length];
        for (int i = 0; i < time.length; i++) {
            time[i] = getGpsSignalQualityTime(i, rawRealTimeUs, 0) / 1000;
        }
        s.setTimeInGpsSignalQualityLevel(time);
        return s;
    }

    @Override // android.os.BatteryStats
    public BatteryStats.LevelStepTracker getChargeLevelStepTracker() {
        return this.mChargeStepTracker;
    }

    @Override // android.os.BatteryStats
    public BatteryStats.LevelStepTracker getDailyChargeLevelStepTracker() {
        return this.mDailyChargeStepTracker;
    }

    @Override // android.os.BatteryStats
    public ArrayList<BatteryStats.PackageChange> getDailyPackageChanges() {
        return this.mDailyPackageChanges;
    }

    protected long getBatteryUptimeLocked() {
        return getBatteryUptimeLocked(this.mClock.uptimeMillis());
    }

    protected long getBatteryUptimeLocked(long uptimeMs) {
        return this.mOnBatteryTimeBase.getUptime(1000 * uptimeMs);
    }

    @Override // android.os.BatteryStats
    public long getBatteryUptime(long curTimeUs) {
        return this.mOnBatteryTimeBase.getUptime(curTimeUs);
    }

    @Override // android.os.BatteryStats
    public long getBatteryRealtime(long curTimeUs) {
        return this.mOnBatteryTimeBase.getRealtime(curTimeUs);
    }

    @Override // android.os.BatteryStats
    public int getDischargeStartLevel() {
        int dischargeStartLevelLocked;
        synchronized (this) {
            dischargeStartLevelLocked = getDischargeStartLevelLocked();
        }
        return dischargeStartLevelLocked;
    }

    public int getDischargeStartLevelLocked() {
        return this.mDischargeUnplugLevel;
    }

    @Override // android.os.BatteryStats
    public int getDischargeCurrentLevel() {
        int dischargeCurrentLevelLocked;
        synchronized (this) {
            dischargeCurrentLevelLocked = getDischargeCurrentLevelLocked();
        }
        return dischargeCurrentLevelLocked;
    }

    public int getDischargeCurrentLevelLocked() {
        return this.mDischargeCurrentLevel;
    }

    @Override // android.os.BatteryStats
    public int getLowDischargeAmountSinceCharge() {
        int val;
        int i;
        int i2;
        synchronized (this) {
            val = this.mLowDischargeAmountSinceCharge;
            if (this.mOnBattery && (i = this.mDischargeCurrentLevel) < (i2 = this.mDischargeUnplugLevel)) {
                val += (i2 - i) - 1;
            }
        }
        return val;
    }

    @Override // android.os.BatteryStats
    public int getHighDischargeAmountSinceCharge() {
        int val;
        int i;
        int i2;
        synchronized (this) {
            val = this.mHighDischargeAmountSinceCharge;
            if (this.mOnBattery && (i = this.mDischargeCurrentLevel) < (i2 = this.mDischargeUnplugLevel)) {
                val += i2 - i;
            }
        }
        return val;
    }

    @Override // android.os.BatteryStats
    public int getDischargeAmount(int which) {
        int dischargeAmount;
        if (which == 0) {
            dischargeAmount = getHighDischargeAmountSinceCharge();
        } else {
            dischargeAmount = getDischargeStartLevel() - getDischargeCurrentLevel();
        }
        if (dischargeAmount < 0) {
            return 0;
        }
        return dischargeAmount;
    }

    @Override // android.os.BatteryStats
    public int getDischargeAmountScreenOn() {
        int val;
        int i;
        int i2;
        synchronized (this) {
            val = this.mDischargeAmountScreenOn;
            if (this.mOnBattery && Display.isOnState(this.mScreenState) && (i = this.mDischargeCurrentLevel) < (i2 = this.mDischargeScreenOnUnplugLevel)) {
                val += i2 - i;
            }
        }
        return val;
    }

    @Override // android.os.BatteryStats
    public int getDischargeAmountScreenOnSinceCharge() {
        int val;
        int i;
        int i2;
        synchronized (this) {
            val = this.mDischargeAmountScreenOnSinceCharge;
            if (this.mOnBattery && Display.isOnState(this.mScreenState) && (i = this.mDischargeCurrentLevel) < (i2 = this.mDischargeScreenOnUnplugLevel)) {
                val += i2 - i;
            }
        }
        return val;
    }

    @Override // android.os.BatteryStats
    public int getDischargeAmountScreenOff() {
        int dischargeAmountScreenDoze;
        int i;
        int i2;
        synchronized (this) {
            int val = this.mDischargeAmountScreenOff;
            if (this.mOnBattery && Display.isOffState(this.mScreenState) && (i = this.mDischargeCurrentLevel) < (i2 = this.mDischargeScreenOffUnplugLevel)) {
                val += i2 - i;
            }
            dischargeAmountScreenDoze = getDischargeAmountScreenDoze() + val;
        }
        return dischargeAmountScreenDoze;
    }

    @Override // android.os.BatteryStats
    public int getDischargeAmountScreenOffSinceCharge() {
        int dischargeAmountScreenDozeSinceCharge;
        int i;
        int i2;
        synchronized (this) {
            int val = this.mDischargeAmountScreenOffSinceCharge;
            if (this.mOnBattery && Display.isOffState(this.mScreenState) && (i = this.mDischargeCurrentLevel) < (i2 = this.mDischargeScreenOffUnplugLevel)) {
                val += i2 - i;
            }
            dischargeAmountScreenDozeSinceCharge = getDischargeAmountScreenDozeSinceCharge() + val;
        }
        return dischargeAmountScreenDozeSinceCharge;
    }

    @Override // android.os.BatteryStats
    public int getDischargeAmountScreenDoze() {
        int val;
        int i;
        int i2;
        synchronized (this) {
            val = this.mDischargeAmountScreenDoze;
            if (this.mOnBattery && Display.isDozeState(this.mScreenState) && (i = this.mDischargeCurrentLevel) < (i2 = this.mDischargeScreenDozeUnplugLevel)) {
                val += i2 - i;
            }
        }
        return val;
    }

    @Override // android.os.BatteryStats
    public int getDischargeAmountScreenDozeSinceCharge() {
        int val;
        int i;
        int i2;
        synchronized (this) {
            val = this.mDischargeAmountScreenDozeSinceCharge;
            if (this.mOnBattery && Display.isDozeState(this.mScreenState) && (i = this.mDischargeCurrentLevel) < (i2 = this.mDischargeScreenDozeUnplugLevel)) {
                val += i2 - i;
            }
        }
        return val;
    }

    @Override // android.os.BatteryStats
    public long[] getSystemServiceTimeAtCpuSpeeds() {
        LongSamplingCounterArray longSamplingCounterArray = this.mBinderThreadCpuTimesUs;
        if (longSamplingCounterArray == null) {
            return null;
        }
        return longSamplingCounterArray.getCountsLocked(0);
    }

    public Uid getUidStatsLocked(int uid) {
        return getUidStatsLocked(uid, this.mClock.elapsedRealtime(), this.mClock.uptimeMillis());
    }

    public Uid getUidStatsLocked(int uid, long elapsedRealtimeMs, long uptimeMs) {
        Uid u = this.mUidStats.get(uid);
        if (u == null) {
            if (Process.isSdkSandboxUid(uid)) {
                Log.wtf(TAG, "Tracking an SDK Sandbox UID");
            }
            Uid u2 = new Uid(this, uid, elapsedRealtimeMs, uptimeMs);
            this.mUidStats.put(uid, u2);
            return u2;
        }
        return u;
    }

    public Uid getAvailableUidStatsLocked(int uid) {
        Uid u = this.mUidStats.get(uid);
        return u;
    }

    public void onCleanupUserLocked(int userId, long elapsedRealtimeMs) {
        int firstUidForUser = UserHandle.getUid(userId, 0);
        int lastUidForUser = UserHandle.getUid(userId, Process.LAST_ISOLATED_UID);
        this.mPendingRemovedUids.add(new UidToRemove(firstUidForUser, lastUidForUser, elapsedRealtimeMs));
    }

    public void onUserRemovedLocked(int userId) {
        ExternalStatsSync externalStatsSync = this.mExternalSync;
        if (externalStatsSync != null) {
            externalStatsSync.scheduleCleanupDueToRemovedUser(userId);
        }
    }

    public void clearRemovedUserUidsLocked(int userId) {
        int firstUidForUser = UserHandle.getUid(userId, 0);
        int lastUidForUser = UserHandle.getUid(userId, Process.LAST_ISOLATED_UID);
        this.mUidStats.put(firstUidForUser, null);
        this.mUidStats.put(lastUidForUser, null);
        int firstIndex = this.mUidStats.indexOfKey(firstUidForUser);
        int lastIndex = this.mUidStats.indexOfKey(lastUidForUser);
        for (int i = firstIndex; i <= lastIndex; i++) {
            Uid uid = this.mUidStats.valueAt(i);
            if (uid != null) {
                uid.detachFromTimeBase();
            }
        }
        this.mUidStats.removeAtRange(firstIndex, (lastIndex - firstIndex) + 1);
        removeCpuStatsForUidRangeLocked(firstUidForUser, lastUidForUser);
    }

    public void removeUidStatsLocked(int uid) {
        removeUidStatsLocked(uid, this.mClock.elapsedRealtime());
    }

    public void removeUidStatsLocked(int uid, long elapsedRealtimeMs) {
        Uid u = this.mUidStats.get(uid);
        if (u != null) {
            u.detachFromTimeBase();
        }
        this.mUidStats.remove(uid);
        this.mPendingRemovedUids.add(new UidToRemove(this, uid, elapsedRealtimeMs));
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void removeCpuStatsForUidRangeLocked(int startUid, int endUid) {
        if (startUid == endUid) {
            this.mCpuUidUserSysTimeReader.removeUid(startUid);
            this.mCpuUidFreqTimeReader.removeUid(startUid);
            if (this.mConstants.TRACK_CPU_ACTIVE_CLUSTER_TIME) {
                this.mCpuUidActiveTimeReader.removeUid(startUid);
                this.mCpuUidClusterTimeReader.removeUid(startUid);
            }
            KernelSingleUidTimeReader kernelSingleUidTimeReader = this.mKernelSingleUidTimeReader;
            if (kernelSingleUidTimeReader != null) {
                kernelSingleUidTimeReader.removeUid(startUid);
            }
            this.mNumUidsRemoved++;
        } else if (startUid < endUid) {
            this.mCpuUidFreqTimeReader.removeUidsInRange(startUid, endUid);
            this.mCpuUidUserSysTimeReader.removeUidsInRange(startUid, endUid);
            if (this.mConstants.TRACK_CPU_ACTIVE_CLUSTER_TIME) {
                this.mCpuUidActiveTimeReader.removeUidsInRange(startUid, endUid);
                this.mCpuUidClusterTimeReader.removeUidsInRange(startUid, endUid);
            }
            KernelSingleUidTimeReader kernelSingleUidTimeReader2 = this.mKernelSingleUidTimeReader;
            if (kernelSingleUidTimeReader2 != null) {
                kernelSingleUidTimeReader2.removeUidsInRange(startUid, endUid);
            }
            this.mNumUidsRemoved++;
        } else {
            Slog.w(TAG, "End UID " + endUid + " is smaller than start UID " + startUid);
        }
    }

    public Uid.Proc getProcessStatsLocked(int uid, String name) {
        return getProcessStatsLocked(uid, name, this.mClock.elapsedRealtime(), this.mClock.uptimeMillis());
    }

    public Uid.Proc getProcessStatsLocked(int uid, String name, long elapsedRealtimeMs, long uptimeMs) {
        Uid u = getUidStatsLocked(mapUid(uid), elapsedRealtimeMs, uptimeMs);
        return u.getProcessStatsLocked(name);
    }

    public Uid.Pkg getPackageStatsLocked(int uid, String pkg) {
        return getPackageStatsLocked(uid, pkg, this.mClock.elapsedRealtime(), this.mClock.uptimeMillis());
    }

    public Uid.Pkg getPackageStatsLocked(int uid, String pkg, long elapsedRealtimeMs, long uptimeMs) {
        Uid u = getUidStatsLocked(mapUid(uid), elapsedRealtimeMs, uptimeMs);
        return u.getPackageStatsLocked(pkg);
    }

    public Uid.Pkg.Serv getServiceStatsLocked(int uid, String pkg, String name) {
        return getServiceStatsLocked(uid, pkg, name, this.mClock.elapsedRealtime(), this.mClock.uptimeMillis());
    }

    public Uid.Pkg.Serv getServiceStatsLocked(int uid, String pkg, String name, long elapsedRealtimeMs, long uptimeMs) {
        Uid u = getUidStatsLocked(mapUid(uid), elapsedRealtimeMs, uptimeMs);
        return u.getServiceStatsLocked(pkg, name);
    }

    public void shutdownLocked() {
        recordShutdownLocked(this.mClock.currentTimeMillis(), this.mClock.elapsedRealtime());
        writeSyncLocked();
        this.mShuttingDown = true;
    }

    @Override // android.os.BatteryStats
    public boolean isProcessStateDataAvailable() {
        return trackPerProcStateCpuTimes();
    }

    public boolean trackPerProcStateCpuTimes() {
        return this.mCpuUidFreqTimeReader.isFastCpuTimesReader();
    }

    public void systemServicesReady(Context context) {
        this.mConstants.startObserving(context.getContentResolver());
        registerUsbStateReceiver(context);
    }

    public void initMeasuredEnergyStatsLocked(boolean[] supportedStandardBuckets, String[] customBucketNames) {
        boolean compatibleConfig;
        int numDisplays = this.mPerDisplayBatteryStats.length;
        for (int i = 0; i < numDisplays; i++) {
            int screenState = this.mPerDisplayBatteryStats[i].screenState;
            this.mPerDisplayBatteryStats[i].screenStateAtLastEnergyMeasurement = screenState;
        }
        if (supportedStandardBuckets != null) {
            MeasuredEnergyStats.Config config = new MeasuredEnergyStats.Config(supportedStandardBuckets, customBucketNames, SUPPORTED_PER_PROCESS_STATE_STANDARD_ENERGY_BUCKETS, getBatteryConsumerProcessStateNames());
            MeasuredEnergyStats.Config config2 = this.mMeasuredEnergyStatsConfig;
            if (config2 == null) {
                compatibleConfig = true;
            } else {
                compatibleConfig = config2.isCompatible(config);
            }
            this.mMeasuredEnergyStatsConfig = config;
            this.mGlobalMeasuredEnergyStats = new MeasuredEnergyStats(config);
            if (supportedStandardBuckets[5]) {
                this.mBluetoothPowerCalculator = new BluetoothPowerCalculator(this.mPowerProfile);
            }
            if (supportedStandardBuckets[3]) {
                this.mCpuPowerCalculator = new CpuPowerCalculator(this.mPowerProfile);
            }
            if (supportedStandardBuckets[7]) {
                this.mMobileRadioPowerCalculator = new MobileRadioPowerCalculator(this.mPowerProfile);
            }
            if (supportedStandardBuckets[4]) {
                this.mWifiPowerCalculator = new WifiPowerCalculator(this.mPowerProfile);
            }
        } else {
            compatibleConfig = this.mMeasuredEnergyStatsConfig == null;
            this.mMeasuredEnergyStatsConfig = null;
            this.mGlobalMeasuredEnergyStats = null;
        }
        if (!compatibleConfig) {
            resetAllStatsLocked(SystemClock.uptimeMillis(), SystemClock.elapsedRealtime(), 4);
        }
    }

    private static String[] getBatteryConsumerProcessStateNames() {
        String[] procStateNames = new String[5];
        for (int procState = 0; procState < 5; procState++) {
            procStateNames[procState] = BatteryConsumer.processStateToString(procState);
        }
        return procStateNames;
    }

    public int getBatteryVoltageMvLocked() {
        return this.mBatteryVoltageMv;
    }

    /* loaded from: classes4.dex */
    public final class Constants extends ContentObserver {
        private static final int DEFAULT_BATTERY_CHARGED_DELAY_MS = 900000;
        private static final long DEFAULT_BATTERY_LEVEL_COLLECTION_DELAY_MS = 300000;
        private static final long DEFAULT_EXTERNAL_STATS_COLLECTION_RATE_LIMIT_MS = 600000;
        private static final long DEFAULT_KERNEL_UID_READERS_THROTTLE_TIME = 1000;
        private static final int DEFAULT_MAX_HISTORY_BUFFER_KB = 128;
        private static final int DEFAULT_MAX_HISTORY_BUFFER_LOW_RAM_DEVICE_KB = 64;
        private static final int DEFAULT_MAX_HISTORY_FILES = 32;
        private static final int DEFAULT_MAX_HISTORY_FILES_LOW_RAM_DEVICE = 64;
        private static final long DEFAULT_PROC_STATE_CHANGE_COLLECTION_DELAY_MS = 60000;
        private static final boolean DEFAULT_TRACK_CPU_ACTIVE_CLUSTER_TIME = true;
        private static final long DEFAULT_UID_REMOVE_DELAY_MS = 300000;
        public static final String KEY_BATTERY_CHARGED_DELAY_MS = "battery_charged_delay_ms";
        public static final String KEY_BATTERY_LEVEL_COLLECTION_DELAY_MS = "battery_level_collection_delay_ms";
        public static final String KEY_EXTERNAL_STATS_COLLECTION_RATE_LIMIT_MS = "external_stats_collection_rate_limit_ms";
        public static final String KEY_KERNEL_UID_READERS_THROTTLE_TIME = "kernel_uid_readers_throttle_time";
        public static final String KEY_MAX_HISTORY_BUFFER_KB = "max_history_buffer_kb";
        public static final String KEY_MAX_HISTORY_FILES = "max_history_files";
        public static final String KEY_PROC_STATE_CHANGE_COLLECTION_DELAY_MS = "procstate_change_collection_delay_ms";
        public static final String KEY_PROC_STATE_CPU_TIMES_READ_DELAY_MS = "proc_state_cpu_times_read_delay_ms";
        public static final String KEY_TRACK_CPU_ACTIVE_CLUSTER_TIME = "track_cpu_active_cluster_time";
        public static final String KEY_UID_REMOVE_DELAY_MS = "uid_remove_delay_ms";
        public int BATTERY_CHARGED_DELAY_MS;
        public long BATTERY_LEVEL_COLLECTION_DELAY_MS;
        public long EXTERNAL_STATS_COLLECTION_RATE_LIMIT_MS;
        public long KERNEL_UID_READERS_THROTTLE_TIME;
        public int MAX_HISTORY_BUFFER;
        public int MAX_HISTORY_FILES;
        public long PROC_STATE_CHANGE_COLLECTION_DELAY_MS;
        public boolean TRACK_CPU_ACTIVE_CLUSTER_TIME;
        public long UID_REMOVE_DELAY_MS;
        private final KeyValueListParser mParser;
        private ContentResolver mResolver;

        public Constants(Handler handler) {
            super(handler);
            this.TRACK_CPU_ACTIVE_CLUSTER_TIME = true;
            this.UID_REMOVE_DELAY_MS = ParcelableCallAnalytics.MILLIS_IN_5_MINUTES;
            this.EXTERNAL_STATS_COLLECTION_RATE_LIMIT_MS = 600000L;
            this.BATTERY_LEVEL_COLLECTION_DELAY_MS = ParcelableCallAnalytics.MILLIS_IN_5_MINUTES;
            this.PROC_STATE_CHANGE_COLLECTION_DELAY_MS = 60000L;
            this.BATTERY_CHARGED_DELAY_MS = DEFAULT_BATTERY_CHARGED_DELAY_MS;
            this.mParser = new KeyValueListParser(',');
            if (ActivityManager.isLowRamDeviceStatic()) {
                this.MAX_HISTORY_FILES = 64;
                this.MAX_HISTORY_BUFFER = 65536;
                return;
            }
            this.MAX_HISTORY_FILES = 32;
            this.MAX_HISTORY_BUFFER = 131072;
        }

        public void startObserving(ContentResolver resolver) {
            this.mResolver = resolver;
            resolver.registerContentObserver(Settings.Global.getUriFor(Settings.Global.BATTERY_STATS_CONSTANTS), false, this);
            this.mResolver.registerContentObserver(Settings.Global.getUriFor(Settings.Global.BATTERY_CHARGING_STATE_UPDATE_DELAY), false, this);
            updateConstants();
        }

        @Override // android.database.ContentObserver
        public void onChange(boolean selfChange, Uri uri) {
            if (uri.equals(Settings.Global.getUriFor(Settings.Global.BATTERY_CHARGING_STATE_UPDATE_DELAY))) {
                synchronized (BatteryStatsImpl.this) {
                    updateBatteryChargedDelayMsLocked();
                }
                return;
            }
            updateConstants();
        }

        private void updateConstants() {
            int i;
            synchronized (BatteryStatsImpl.this) {
                try {
                    this.mParser.setString(Settings.Global.getString(this.mResolver, Settings.Global.BATTERY_STATS_CONSTANTS));
                } catch (IllegalArgumentException e) {
                    Slog.e(BatteryStatsImpl.TAG, "Bad batterystats settings", e);
                }
                this.TRACK_CPU_ACTIVE_CLUSTER_TIME = this.mParser.getBoolean(KEY_TRACK_CPU_ACTIVE_CLUSTER_TIME, true);
                updateKernelUidReadersThrottleTime(this.KERNEL_UID_READERS_THROTTLE_TIME, this.mParser.getLong(KEY_KERNEL_UID_READERS_THROTTLE_TIME, 1000L));
                updateUidRemoveDelay(this.mParser.getLong(KEY_UID_REMOVE_DELAY_MS, ParcelableCallAnalytics.MILLIS_IN_5_MINUTES));
                this.EXTERNAL_STATS_COLLECTION_RATE_LIMIT_MS = this.mParser.getLong(KEY_EXTERNAL_STATS_COLLECTION_RATE_LIMIT_MS, 600000L);
                this.BATTERY_LEVEL_COLLECTION_DELAY_MS = this.mParser.getLong(KEY_BATTERY_LEVEL_COLLECTION_DELAY_MS, ParcelableCallAnalytics.MILLIS_IN_5_MINUTES);
                this.PROC_STATE_CHANGE_COLLECTION_DELAY_MS = this.mParser.getLong(KEY_PROC_STATE_CHANGE_COLLECTION_DELAY_MS, 60000L);
                KeyValueListParser keyValueListParser = this.mParser;
                int i2 = 64;
                if (ActivityManager.isLowRamDeviceStatic()) {
                    i = 64;
                } else {
                    i = 32;
                }
                this.MAX_HISTORY_FILES = keyValueListParser.getInt(KEY_MAX_HISTORY_FILES, i);
                KeyValueListParser keyValueListParser2 = this.mParser;
                if (!ActivityManager.isLowRamDeviceStatic()) {
                    i2 = 128;
                }
                this.MAX_HISTORY_BUFFER = keyValueListParser2.getInt(KEY_MAX_HISTORY_BUFFER_KB, i2) * 1024;
                updateBatteryChargedDelayMsLocked();
            }
        }

        private void updateBatteryChargedDelayMsLocked() {
            int delay = Settings.Global.getInt(this.mResolver, Settings.Global.BATTERY_CHARGING_STATE_UPDATE_DELAY, -1);
            this.BATTERY_CHARGED_DELAY_MS = delay >= 0 ? delay : this.mParser.getInt(KEY_BATTERY_CHARGED_DELAY_MS, DEFAULT_BATTERY_CHARGED_DELAY_MS);
            if (BatteryStatsImpl.this.mHandler.hasCallbacks(BatteryStatsImpl.this.mDeferSetCharging)) {
                BatteryStatsImpl.this.mHandler.removeCallbacks(BatteryStatsImpl.this.mDeferSetCharging);
                BatteryStatsImpl.this.mHandler.postDelayed(BatteryStatsImpl.this.mDeferSetCharging, this.BATTERY_CHARGED_DELAY_MS);
            }
        }

        private void updateKernelUidReadersThrottleTime(long oldTimeMs, long newTimeMs) {
            this.KERNEL_UID_READERS_THROTTLE_TIME = newTimeMs;
            if (oldTimeMs != newTimeMs) {
                BatteryStatsImpl.this.mCpuUidUserSysTimeReader.setThrottle(this.KERNEL_UID_READERS_THROTTLE_TIME);
                BatteryStatsImpl.this.mCpuUidFreqTimeReader.setThrottle(this.KERNEL_UID_READERS_THROTTLE_TIME);
                BatteryStatsImpl.this.mCpuUidActiveTimeReader.setThrottle(this.KERNEL_UID_READERS_THROTTLE_TIME);
                BatteryStatsImpl.this.mCpuUidClusterTimeReader.setThrottle(this.KERNEL_UID_READERS_THROTTLE_TIME);
            }
        }

        private void updateUidRemoveDelay(long newTimeMs) {
            this.UID_REMOVE_DELAY_MS = newTimeMs;
            BatteryStatsImpl.this.clearPendingRemovedUidsLocked();
        }

        public void dumpLocked(PrintWriter pw) {
            pw.print(KEY_TRACK_CPU_ACTIVE_CLUSTER_TIME);
            pw.print("=");
            pw.println(this.TRACK_CPU_ACTIVE_CLUSTER_TIME);
            pw.print(KEY_KERNEL_UID_READERS_THROTTLE_TIME);
            pw.print("=");
            pw.println(this.KERNEL_UID_READERS_THROTTLE_TIME);
            pw.print(KEY_EXTERNAL_STATS_COLLECTION_RATE_LIMIT_MS);
            pw.print("=");
            pw.println(this.EXTERNAL_STATS_COLLECTION_RATE_LIMIT_MS);
            pw.print(KEY_BATTERY_LEVEL_COLLECTION_DELAY_MS);
            pw.print("=");
            pw.println(this.BATTERY_LEVEL_COLLECTION_DELAY_MS);
            pw.print(KEY_PROC_STATE_CHANGE_COLLECTION_DELAY_MS);
            pw.print("=");
            pw.println(this.PROC_STATE_CHANGE_COLLECTION_DELAY_MS);
            pw.print(KEY_MAX_HISTORY_FILES);
            pw.print("=");
            pw.println(this.MAX_HISTORY_FILES);
            pw.print(KEY_MAX_HISTORY_BUFFER_KB);
            pw.print("=");
            pw.println(this.MAX_HISTORY_BUFFER / 1024);
            pw.print(KEY_BATTERY_CHARGED_DELAY_MS);
            pw.print("=");
            pw.println(this.BATTERY_CHARGED_DELAY_MS);
        }
    }

    public long getExternalStatsCollectionRateLimitMs() {
        long j;
        synchronized (this) {
            j = this.mConstants.EXTERNAL_STATS_COLLECTION_RATE_LIMIT_MS;
        }
        return j;
    }

    public void dumpConstantsLocked(PrintWriter pw) {
        IndentingPrintWriter iPw = new IndentingPrintWriter(pw, "    ");
        iPw.println("BatteryStats constants:");
        iPw.increaseIndent();
        this.mConstants.dumpLocked(iPw);
        iPw.decreaseIndent();
    }

    public void dumpCpuStatsLocked(PrintWriter pw) {
        int size = this.mUidStats.size();
        pw.println("Per UID CPU user & system time in ms:");
        for (int i = 0; i < size; i++) {
            int u = this.mUidStats.keyAt(i);
            Uid uid = this.mUidStats.get(u);
            pw.print("  ");
            pw.print(u);
            pw.print(": ");
            pw.print(uid.getUserCpuTimeUs(0) / 1000);
            pw.print(" ");
            pw.println(uid.getSystemCpuTimeUs(0) / 1000);
        }
        pw.println("Per UID CPU active time in ms:");
        for (int i2 = 0; i2 < size; i2++) {
            int u2 = this.mUidStats.keyAt(i2);
            Uid uid2 = this.mUidStats.get(u2);
            if (uid2.getCpuActiveTime() > 0) {
                pw.print("  ");
                pw.print(u2);
                pw.print(": ");
                pw.println(uid2.getCpuActiveTime());
            }
        }
        pw.println("Per UID CPU cluster time in ms:");
        for (int i3 = 0; i3 < size; i3++) {
            int u3 = this.mUidStats.keyAt(i3);
            long[] times = this.mUidStats.get(u3).getCpuClusterTimes();
            if (times != null) {
                pw.print("  ");
                pw.print(u3);
                pw.print(": ");
                pw.println(Arrays.toString(times));
            }
        }
        pw.println("Per UID CPU frequency time in ms:");
        for (int i4 = 0; i4 < size; i4++) {
            int u4 = this.mUidStats.keyAt(i4);
            long[] times2 = this.mUidStats.get(u4).getCpuFreqTimes(0);
            if (times2 != null) {
                pw.print("  ");
                pw.print(u4);
                pw.print(": ");
                pw.println(Arrays.toString(times2));
            }
        }
        updateSystemServiceCallStats();
        if (this.mBinderThreadCpuTimesUs != null) {
            pw.println("Per UID System server binder time in ms:");
            long[] systemServiceTimeAtCpuSpeeds = getSystemServiceTimeAtCpuSpeeds();
            for (int i5 = 0; i5 < size; i5++) {
                int u5 = this.mUidStats.keyAt(i5);
                double proportionalSystemServiceUsage = this.mUidStats.get(u5).getProportionalSystemServiceUsage();
                long timeUs = 0;
                for (int j = systemServiceTimeAtCpuSpeeds.length - 1; j >= 0; j--) {
                    double d = timeUs;
                    long timeUs2 = systemServiceTimeAtCpuSpeeds[j];
                    timeUs = (long) (d + (timeUs2 * proportionalSystemServiceUsage));
                }
                pw.print("  ");
                pw.print(u5);
                pw.print(": ");
                pw.println(timeUs / 1000);
            }
        }
    }

    public void dumpMeasuredEnergyStatsLocked(PrintWriter pw) {
        pw.printf("On battery measured charge stats (microcoulombs) \n", new Object[0]);
        MeasuredEnergyStats measuredEnergyStats = this.mGlobalMeasuredEnergyStats;
        if (measuredEnergyStats == null) {
            pw.printf("    Not supported on this device.\n", new Object[0]);
            return;
        }
        dumpMeasuredEnergyStatsLocked(pw, "global usage", measuredEnergyStats);
        int size = this.mUidStats.size();
        for (int i = 0; i < size; i++) {
            int u = this.mUidStats.keyAt(i);
            Uid uid = this.mUidStats.get(u);
            String name = "uid " + uid.mUid;
            dumpMeasuredEnergyStatsLocked(pw, name, uid.mUidMeasuredEnergyStats);
        }
    }

    private void dumpMeasuredEnergyStatsLocked(PrintWriter pw, String name, MeasuredEnergyStats stats) {
        if (stats == null) {
            return;
        }
        IndentingPrintWriter iPw = new IndentingPrintWriter(pw, "    ");
        iPw.increaseIndent();
        iPw.printf("%s:\n", name);
        iPw.increaseIndent();
        stats.dump(iPw);
        iPw.decreaseIndent();
    }

    public void dumpPowerProfileLocked(PrintWriter pw) {
        IndentingPrintWriter iPw = new IndentingPrintWriter(pw, "    ");
        iPw.printf("Power Profile: \n", new Object[0]);
        iPw.increaseIndent();
        this.mPowerProfile.dump(iPw);
        iPw.decreaseIndent();
    }

    public void writeAsyncLocked() {
        writeStatsLocked(false);
        writeHistoryLocked(false);
    }

    public void writeSyncLocked() {
        writeStatsLocked(true);
        writeHistoryLocked(true);
    }

    void writeStatsLocked(boolean sync) {
        if (this.mStatsFile == null) {
            Slog.w(TAG, "writeStatsLocked: no file associated with this instance");
        } else if (this.mShuttingDown) {
        } else {
            Parcel p = Parcel.obtain();
            SystemClock.uptimeMillis();
            writeSummaryToParcel(p, false);
            this.mLastWriteTimeMs = this.mClock.elapsedRealtime();
            writeParcelToFileLocked(p, this.mStatsFile, sync);
        }
    }

    void writeHistoryLocked(boolean sync) {
        if (this.mBatteryStatsHistory.getActiveFile() == null) {
            Slog.w(TAG, "writeHistoryLocked: no history file associated with this instance");
        } else if (this.mShuttingDown) {
        } else {
            Parcel p = Parcel.obtain();
            SystemClock.uptimeMillis();
            writeHistoryBuffer(p, true);
            writeParcelToFileLocked(p, this.mBatteryStatsHistory.getActiveFile(), sync);
        }
    }

    void writeParcelToFileLocked(final Parcel p, final AtomicFile file, boolean sync) {
        if (sync) {
            commitPendingDataToDisk(p, file);
        } else {
            BackgroundThread.getHandler().post(new Runnable() { // from class: com.android.internal.os.BatteryStatsImpl.6
                @Override // java.lang.Runnable
                public void run() {
                    BatteryStatsImpl.this.commitPendingDataToDisk(p, file);
                }
            });
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void commitPendingDataToDisk(Parcel p, AtomicFile file) {
        this.mWriteLock.lock();
        FileOutputStream fos = null;
        try {
            try {
                long startTimeMs = SystemClock.uptimeMillis();
                fos = file.startWrite();
                fos.write(p.marshall());
                fos.flush();
                file.finishWrite(fos);
                EventLogTags.writeCommitSysConfigFile("batterystats", SystemClock.uptimeMillis() - startTimeMs);
            } catch (IOException e) {
                Slog.w(TAG, "Error writing battery statistics", e);
                file.failWrite(fos);
            }
        } finally {
            p.recycle();
            this.mWriteLock.unlock();
        }
    }

    public void readLocked() {
        if (this.mDailyFile != null) {
            readDailyStatsLocked();
        }
        if (this.mStatsFile == null) {
            Slog.w(TAG, "readLocked: no file associated with this instance");
            return;
        }
        AtomicFile activeHistoryFile = this.mBatteryStatsHistory.getActiveFile();
        if (activeHistoryFile == null) {
            Slog.w(TAG, "readLocked: no history file associated with this instance");
            return;
        }
        this.mUidStats.clear();
        Parcel history = Parcel.obtain();
        try {
            try {
                SystemClock.uptimeMillis();
                if (this.mStatsFile.exists()) {
                    byte[] raw = this.mStatsFile.readFully();
                    history.unmarshall(raw, 0, raw.length);
                    history.setDataPosition(0);
                    readSummaryFromParcel(history);
                }
            } catch (Exception e) {
                Slog.e(TAG, "Error reading battery statistics", e);
                resetAllStatsLocked(SystemClock.uptimeMillis(), SystemClock.elapsedRealtime(), 1);
            }
            history.recycle();
            history = Parcel.obtain();
            try {
                try {
                    SystemClock.uptimeMillis();
                    if (activeHistoryFile.exists()) {
                        byte[] raw2 = activeHistoryFile.readFully();
                        if (raw2.length > 0) {
                            history.unmarshall(raw2, 0, raw2.length);
                            history.setDataPosition(0);
                            readHistoryBuffer(history);
                        }
                    }
                } catch (Exception e2) {
                    Slog.e(TAG, "Error reading battery history", e2);
                    clearHistoryLocked();
                    this.mBatteryStatsHistory.resetAllFiles();
                }
                this.mEndPlatformVersion = Build.ID;
                if (this.mHistoryBuffer.dataPosition() > 0 || this.mBatteryStatsHistory.getFilesNumbers().size() > 1) {
                    this.mRecordingHistory = true;
                    long elapsedRealtimeMs = this.mClock.elapsedRealtime();
                    long uptimeMs = this.mClock.uptimeMillis();
                    addHistoryBufferLocked(elapsedRealtimeMs, (byte) 4, this.mHistoryCur);
                    startRecordingHistory(elapsedRealtimeMs, uptimeMs, false);
                }
                recordDailyStatsIfNeededLocked(false, this.mClock.currentTimeMillis());
            } finally {
            }
        } finally {
        }
    }

    @Override // android.os.Parcelable
    public int describeContents() {
        return 0;
    }

    void readHistoryBuffer(Parcel in) throws ParcelFormatException {
        int version = in.readInt();
        if (version != 208) {
            Slog.w("BatteryStats", "readHistoryBuffer: version got " + version + ", expected 208; erasing old stats");
            return;
        }
        long historyBaseTime = in.readLong();
        this.mHistoryBuffer.setDataSize(0);
        this.mHistoryBuffer.setDataPosition(0);
        int bufSize = in.readInt();
        int curPos = in.dataPosition();
        if (bufSize >= this.mConstants.MAX_HISTORY_BUFFER * 100) {
            throw new ParcelFormatException("File corrupt: history data buffer too large " + bufSize);
        }
        if ((bufSize & (-4)) != bufSize) {
            throw new ParcelFormatException("File corrupt: history data buffer not aligned " + bufSize);
        }
        this.mHistoryBuffer.appendFrom(in, curPos, bufSize);
        in.setDataPosition(curPos + bufSize);
        this.mHistoryBaseTimeMs = historyBaseTime;
        if (historyBaseTime > 0) {
            long oldnow = this.mClock.elapsedRealtime();
            this.mHistoryBaseTimeMs = (this.mHistoryBaseTimeMs - oldnow) + 1;
        }
    }

    void writeHistoryBuffer(Parcel out, boolean inclData) {
        out.writeInt(208);
        out.writeLong(this.mHistoryBaseTimeMs + this.mLastHistoryElapsedRealtimeMs);
        if (!inclData) {
            out.writeInt(0);
            out.writeInt(0);
            return;
        }
        out.writeInt(this.mHistoryBuffer.dataSize());
        Parcel parcel = this.mHistoryBuffer;
        out.appendFrom(parcel, 0, parcel.dataSize());
    }

    public void readSummaryFromParcel(Parcel in) throws ParcelFormatException {
        int NPKG;
        long elapsedRealtimeMs;
        int numClusters;
        int numClusters2;
        int numRat;
        int NRPMS;
        int version = in.readInt();
        if (version != 208) {
            Slog.w("BatteryStats", "readFromParcel: version got " + version + ", expected 208; erasing old stats");
            return;
        }
        boolean inclHistory = in.readBoolean();
        if (inclHistory) {
            readHistoryBuffer(in);
            this.mBatteryStatsHistory.readFromParcel(in);
        }
        this.mHistoryTagPool.clear();
        this.mNextHistoryTagIdx = 0;
        this.mNumHistoryTagChars = 0;
        int numTags = in.readInt();
        for (int i = 0; i < numTags; i++) {
            int idx = in.readInt();
            String str = in.readString();
            int uid = in.readInt();
            BatteryStats.HistoryTag tag = new BatteryStats.HistoryTag();
            tag.string = str;
            tag.uid = uid;
            tag.poolIdx = idx;
            this.mHistoryTagPool.put(tag, Integer.valueOf(idx));
            if (idx >= this.mNextHistoryTagIdx) {
                this.mNextHistoryTagIdx = idx + 1;
            }
            this.mNumHistoryTagChars += tag.string.length() + 1;
        }
        int i2 = in.readInt();
        this.mStartCount = i2;
        this.mUptimeUs = in.readLong();
        this.mRealtimeUs = in.readLong();
        this.mStartClockTimeMs = in.readLong();
        this.mStartPlatformVersion = in.readString();
        this.mEndPlatformVersion = in.readString();
        this.mOnBatteryTimeBase.readSummaryFromParcel(in);
        this.mOnBatteryScreenOffTimeBase.readSummaryFromParcel(in);
        this.mDischargeUnplugLevel = in.readInt();
        this.mDischargePlugLevel = in.readInt();
        this.mDischargeCurrentLevel = in.readInt();
        this.mCurrentBatteryLevel = in.readInt();
        this.mEstimatedBatteryCapacityMah = in.readInt();
        this.mLastLearnedBatteryCapacityUah = in.readInt();
        this.mMinLearnedBatteryCapacityUah = in.readInt();
        this.mMaxLearnedBatteryCapacityUah = in.readInt();
        this.mLowDischargeAmountSinceCharge = in.readInt();
        this.mHighDischargeAmountSinceCharge = in.readInt();
        this.mDischargeAmountScreenOnSinceCharge = in.readInt();
        this.mDischargeAmountScreenOffSinceCharge = in.readInt();
        this.mDischargeAmountScreenDozeSinceCharge = in.readInt();
        this.mDischargeStepTracker.readFromParcel(in);
        this.mChargeStepTracker.readFromParcel(in);
        this.mDailyDischargeStepTracker.readFromParcel(in);
        this.mDailyChargeStepTracker.readFromParcel(in);
        this.mDischargeCounter.readSummaryFromParcelLocked(in);
        this.mDischargeScreenOffCounter.readSummaryFromParcelLocked(in);
        this.mDischargeScreenDozeCounter.readSummaryFromParcelLocked(in);
        this.mDischargeLightDozeCounter.readSummaryFromParcelLocked(in);
        this.mDischargeDeepDozeCounter.readSummaryFromParcelLocked(in);
        int NPKG2 = in.readInt();
        if (NPKG2 > 0) {
            this.mDailyPackageChanges = new ArrayList<>(NPKG2);
            while (NPKG2 > 0) {
                NPKG2--;
                BatteryStats.PackageChange pc = new BatteryStats.PackageChange();
                pc.mPackageName = in.readString();
                pc.mUpdate = in.readInt() != 0;
                pc.mVersionCode = in.readLong();
                this.mDailyPackageChanges.add(pc);
            }
            NPKG = NPKG2;
        } else {
            this.mDailyPackageChanges = null;
            NPKG = NPKG2;
        }
        this.mDailyStartTimeMs = in.readLong();
        this.mNextMinDailyDeadlineMs = in.readLong();
        this.mNextMaxDailyDeadlineMs = in.readLong();
        this.mBatteryTimeToFullSeconds = in.readLong();
        MeasuredEnergyStats.Config config = MeasuredEnergyStats.Config.createFromParcel(in);
        MeasuredEnergyStats measuredEnergyStats = MeasuredEnergyStats.createAndReadSummaryFromParcel(this.mMeasuredEnergyStatsConfig, in);
        if (config != null && Arrays.equals(config.getStateNames(), getBatteryConsumerProcessStateNames())) {
            this.mMeasuredEnergyStatsConfig = config;
            this.mGlobalMeasuredEnergyStats = measuredEnergyStats;
        }
        this.mStartCount++;
        this.mScreenState = 0;
        this.mScreenOnTimer.readSummaryFromParcelLocked(in);
        this.mScreenDozeTimer.readSummaryFromParcelLocked(in);
        for (int i3 = 0; i3 < 5; i3++) {
            this.mScreenBrightnessTimer[i3].readSummaryFromParcelLocked(in);
        }
        this.mInteractive = false;
        this.mInteractiveTimer.readSummaryFromParcelLocked(in);
        this.mPhoneOn = false;
        this.mPowerSaveModeEnabledTimer.readSummaryFromParcelLocked(in);
        this.mLongestLightIdleTimeMs = in.readLong();
        this.mLongestFullIdleTimeMs = in.readLong();
        this.mDeviceIdleModeLightTimer.readSummaryFromParcelLocked(in);
        this.mDeviceIdleModeFullTimer.readSummaryFromParcelLocked(in);
        this.mDeviceLightIdlingTimer.readSummaryFromParcelLocked(in);
        this.mDeviceIdlingTimer.readSummaryFromParcelLocked(in);
        this.mPhoneOnTimer.readSummaryFromParcelLocked(in);
        for (int i4 = 0; i4 < CellSignalStrength.getNumSignalStrengthLevels() + 1; i4++) {
            this.mPhoneSignalStrengthsTimer[i4].readSummaryFromParcelLocked(in);
        }
        this.mPhoneSignalScanningTimer.readSummaryFromParcelLocked(in);
        for (int i5 = 0; i5 < NUM_DATA_CONNECTION_TYPES; i5++) {
            this.mPhoneDataConnectionsTimer[i5].readSummaryFromParcelLocked(in);
        }
        for (int i6 = 0; i6 < 10; i6++) {
            this.mNetworkByteActivityCounters[i6].readSummaryFromParcelLocked(in);
            this.mNetworkPacketActivityCounters[i6].readSummaryFromParcelLocked(in);
        }
        int numRat2 = in.readInt();
        for (int i7 = 0; i7 < numRat2; i7++) {
            if (in.readInt() != 0) {
                getRatBatteryStatsLocked(i7).readSummaryFromParcel(in);
            }
        }
        this.mMobileRadioPowerState = 1;
        this.mMobileRadioActiveTimer.readSummaryFromParcelLocked(in);
        this.mMobileRadioActivePerAppTimer.readSummaryFromParcelLocked(in);
        this.mMobileRadioActiveAdjustedTime.readSummaryFromParcelLocked(in);
        this.mMobileRadioActiveUnknownTime.readSummaryFromParcelLocked(in);
        this.mMobileRadioActiveUnknownCount.readSummaryFromParcelLocked(in);
        this.mWifiMulticastWakelockTimer.readSummaryFromParcelLocked(in);
        this.mWifiRadioPowerState = 1;
        this.mWifiOn = false;
        this.mWifiOnTimer.readSummaryFromParcelLocked(in);
        this.mGlobalWifiRunning = false;
        this.mGlobalWifiRunningTimer.readSummaryFromParcelLocked(in);
        for (int i8 = 0; i8 < 8; i8++) {
            this.mWifiStateTimer[i8].readSummaryFromParcelLocked(in);
        }
        for (int i9 = 0; i9 < 13; i9++) {
            this.mWifiSupplStateTimer[i9].readSummaryFromParcelLocked(in);
        }
        for (int i10 = 0; i10 < 5; i10++) {
            this.mWifiSignalStrengthsTimer[i10].readSummaryFromParcelLocked(in);
        }
        this.mWifiActiveTimer.readSummaryFromParcelLocked(in);
        this.mWifiActivity.readSummaryFromParcel(in);
        int i11 = 0;
        while (true) {
            StopwatchTimer[] stopwatchTimerArr = this.mGpsSignalQualityTimer;
            if (i11 >= stopwatchTimerArr.length) {
                break;
            }
            stopwatchTimerArr[i11].readSummaryFromParcelLocked(in);
            i11++;
        }
        this.mBluetoothActivity.readSummaryFromParcel(in);
        this.mModemActivity.readSummaryFromParcel(in);
        this.mHasWifiReporting = in.readInt() != 0;
        this.mHasBluetoothReporting = in.readInt() != 0;
        this.mHasModemReporting = in.readInt() != 0;
        this.mNumConnectivityChange = in.readInt();
        this.mFlashlightOnNesting = 0;
        this.mFlashlightOnTimer.readSummaryFromParcelLocked(in);
        this.mCameraOnNesting = 0;
        this.mCameraOnTimer.readSummaryFromParcelLocked(in);
        this.mBluetoothScanNesting = 0;
        this.mBluetoothScanTimer.readSummaryFromParcelLocked(in);
        int NRPMS2 = in.readInt();
        if (NRPMS2 > 10000) {
            throw new ParcelFormatException("File corrupt: too many rpm stats " + NRPMS2);
        }
        for (int irpm = 0; irpm < NRPMS2; irpm++) {
            if (in.readInt() != 0) {
                String rpmName = in.readString();
                getRpmTimerLocked(rpmName).readSummaryFromParcelLocked(in);
            }
        }
        int NSORPMS = in.readInt();
        if (NSORPMS > 10000) {
            throw new ParcelFormatException("File corrupt: too many screen-off rpm stats " + NSORPMS);
        }
        for (int irpm2 = 0; irpm2 < NSORPMS; irpm2++) {
            if (in.readInt() != 0) {
                String rpmName2 = in.readString();
                getScreenOffRpmTimerLocked(rpmName2).readSummaryFromParcelLocked(in);
            }
        }
        int NKW = in.readInt();
        if (NKW > 10000) {
            throw new ParcelFormatException("File corrupt: too many kernel wake locks " + NKW);
        }
        for (int ikw = 0; ikw < NKW; ikw++) {
            if (in.readInt() != 0) {
                String kwltName = in.readString();
                getKernelWakelockTimerLocked(kwltName).readSummaryFromParcelLocked(in);
            }
        }
        int NWR = in.readInt();
        if (NWR > 10000) {
            throw new ParcelFormatException("File corrupt: too many wakeup reasons " + NWR);
        }
        for (int iwr = 0; iwr < NWR; iwr++) {
            if (in.readInt() != 0) {
                String reasonName = in.readString();
                getWakeupReasonTimerLocked(reasonName).readSummaryFromParcelLocked(in);
            }
        }
        int NMS = in.readInt();
        int ims = 0;
        while (ims < NMS) {
            if (in.readInt() == 0) {
                numRat = numRat2;
                NRPMS = NRPMS2;
            } else {
                numRat = numRat2;
                long kmstName = in.readLong();
                NRPMS = NRPMS2;
                getKernelMemoryTimerLocked(kmstName).readSummaryFromParcelLocked(in);
            }
            ims++;
            numRat2 = numRat;
            NRPMS2 = NRPMS;
        }
        int NRPMS3 = NRPMS2;
        int NU = in.readInt();
        char c = 10000;
        if (NU > 10000) {
            throw new ParcelFormatException("File corrupt: too many uids " + NU);
        }
        long elapsedRealtimeMs2 = this.mClock.elapsedRealtime();
        long uptimeMs = this.mClock.uptimeMillis();
        int NS = 0;
        while (NS < NU) {
            int uid2 = in.readInt();
            int numTags2 = numTags;
            int iu = NS;
            int NU2 = NU;
            int NMS2 = NMS;
            int NRPMS4 = NRPMS3;
            MeasuredEnergyStats.Config config2 = config;
            MeasuredEnergyStats measuredEnergyStats2 = measuredEnergyStats;
            Uid u = new Uid(this, uid2, elapsedRealtimeMs2, uptimeMs);
            this.mUidStats.put(uid2, u);
            u.mOnBatteryBackgroundTimeBase.readSummaryFromParcel(in);
            u.mOnBatteryScreenOffBackgroundTimeBase.readSummaryFromParcel(in);
            u.mWifiRunning = false;
            if (in.readInt() != 0) {
                u.mWifiRunningTimer.readSummaryFromParcelLocked(in);
            }
            u.mFullWifiLockOut = false;
            if (in.readInt() != 0) {
                u.mFullWifiLockTimer.readSummaryFromParcelLocked(in);
            }
            u.mWifiScanStarted = false;
            if (in.readInt() != 0) {
                u.mWifiScanTimer.readSummaryFromParcelLocked(in);
            }
            u.mWifiBatchedScanBinStarted = -1;
            for (int i12 = 0; i12 < 5; i12++) {
                if (in.readInt() != 0) {
                    u.makeWifiBatchedScanBin(i12, null);
                    u.mWifiBatchedScanTimer[i12].readSummaryFromParcelLocked(in);
                }
            }
            u.mWifiMulticastWakelockCount = 0;
            if (in.readInt() != 0) {
                u.mWifiMulticastTimer.readSummaryFromParcelLocked(in);
            }
            if (in.readInt() != 0) {
                u.createAudioTurnedOnTimerLocked().readSummaryFromParcelLocked(in);
            }
            if (in.readInt() != 0) {
                u.createVideoTurnedOnTimerLocked().readSummaryFromParcelLocked(in);
            }
            if (in.readInt() != 0) {
                u.createFlashlightTurnedOnTimerLocked().readSummaryFromParcelLocked(in);
            }
            if (in.readInt() != 0) {
                u.createCameraTurnedOnTimerLocked().readSummaryFromParcelLocked(in);
            }
            if (in.readInt() != 0) {
                u.createForegroundActivityTimerLocked().readSummaryFromParcelLocked(in);
            }
            if (in.readInt() != 0) {
                u.createForegroundServiceTimerLocked().readSummaryFromParcelLocked(in);
            }
            if (in.readInt() != 0) {
                u.createAggregatedPartialWakelockTimerLocked().readSummaryFromParcelLocked(in);
            }
            if (in.readInt() != 0) {
                u.createBluetoothScanTimerLocked().readSummaryFromParcelLocked(in);
            }
            if (in.readInt() != 0) {
                u.createBluetoothUnoptimizedScanTimerLocked().readSummaryFromParcelLocked(in);
            }
            if (in.readInt() != 0) {
                u.createBluetoothScanResultCounterLocked().readSummaryFromParcelLocked(in);
            }
            if (in.readInt() != 0) {
                u.createBluetoothScanResultBgCounterLocked().readSummaryFromParcelLocked(in);
            }
            u.mProcessState = 7;
            for (int i13 = 0; i13 < 7; i13++) {
                if (in.readInt() != 0) {
                    u.makeProcessState(i13, null);
                    u.mProcessStateTimer[i13].readSummaryFromParcelLocked(in);
                }
            }
            if (in.readInt() != 0) {
                u.createVibratorOnTimerLocked().readSummaryFromParcelLocked(in);
            }
            if (in.readInt() != 0) {
                if (u.mUserActivityCounters == null) {
                    u.initUserActivityLocked();
                }
                for (int i14 = 0; i14 < Uid.NUM_USER_ACTIVITY_TYPES; i14++) {
                    u.mUserActivityCounters[i14].readSummaryFromParcelLocked(in);
                }
            }
            int i15 = in.readInt();
            if (i15 != 0) {
                u.ensureNetworkActivityLocked();
                for (int i16 = 0; i16 < 10; i16++) {
                    u.mNetworkByteActivityCounters[i16].readSummaryFromParcelLocked(in);
                    u.mNetworkPacketActivityCounters[i16].readSummaryFromParcelLocked(in);
                }
                if (in.readBoolean()) {
                    u.mMobileRadioActiveTime = TimeMultiStateCounter.readFromParcel(in, this.mOnBatteryTimeBase, 5, elapsedRealtimeMs2);
                }
                u.mMobileRadioActiveCount.readSummaryFromParcelLocked(in);
            }
            u.mUserCpuTime.readSummaryFromParcelLocked(in);
            u.mSystemCpuTime.readSummaryFromParcelLocked(in);
            if (in.readInt() != 0) {
                int numClusters3 = in.readInt();
                PowerProfile powerProfile = this.mPowerProfile;
                if (powerProfile != null && powerProfile.getNumCpuClusters() != numClusters3) {
                    throw new ParcelFormatException("Incompatible cpu cluster arrangement");
                }
                detachIfNotNull(u.mCpuClusterSpeedTimesUs);
                u.mCpuClusterSpeedTimesUs = new LongSamplingCounter[numClusters3];
                int cluster = 0;
                while (cluster < numClusters3) {
                    if (in.readInt() != 0) {
                        int NSB = in.readInt();
                        PowerProfile powerProfile2 = this.mPowerProfile;
                        if (powerProfile2 != null && powerProfile2.getNumSpeedStepsInCpuCluster(cluster) != NSB) {
                            throw new ParcelFormatException("File corrupt: too many speed bins " + NSB);
                        }
                        u.mCpuClusterSpeedTimesUs[cluster] = new LongSamplingCounter[NSB];
                        int speed = 0;
                        while (speed < NSB) {
                            if (in.readInt() != 0) {
                                numClusters2 = numClusters3;
                                u.mCpuClusterSpeedTimesUs[cluster][speed] = new LongSamplingCounter(this.mOnBatteryTimeBase);
                                u.mCpuClusterSpeedTimesUs[cluster][speed].readSummaryFromParcelLocked(in);
                            } else {
                                numClusters2 = numClusters3;
                            }
                            speed++;
                            numClusters3 = numClusters2;
                        }
                        numClusters = numClusters3;
                    } else {
                        numClusters = numClusters3;
                        u.mCpuClusterSpeedTimesUs[cluster] = null;
                    }
                    cluster++;
                    numClusters3 = numClusters;
                }
            } else {
                detachIfNotNull(u.mCpuClusterSpeedTimesUs);
                u.mCpuClusterSpeedTimesUs = null;
            }
            detachIfNotNull(u.mCpuFreqTimeMs);
            u.mCpuFreqTimeMs = LongSamplingCounterArray.readSummaryFromParcelLocked(in, this.mOnBatteryTimeBase);
            detachIfNotNull(u.mScreenOffCpuFreqTimeMs);
            u.mScreenOffCpuFreqTimeMs = LongSamplingCounterArray.readSummaryFromParcelLocked(in, this.mOnBatteryScreenOffTimeBase);
            int stateCount = in.readInt();
            if (stateCount != 0) {
                u.mCpuActiveTimeMs = TimeMultiStateCounter.readFromParcel(in, this.mOnBatteryTimeBase, 5, this.mClock.elapsedRealtime());
            }
            u.mCpuClusterTimesMs.readSummaryFromParcelLocked(in);
            detachIfNotNull(u.mProcStateTimeMs);
            u.mProcStateTimeMs = null;
            int stateCount2 = in.readInt();
            if (stateCount2 != 0) {
                detachIfNotNull(u.mProcStateTimeMs);
                u.mProcStateTimeMs = TimeInFreqMultiStateCounter.readFromParcel(in, this.mOnBatteryTimeBase, 8, getCpuFreqCount(), this.mClock.elapsedRealtime());
            }
            detachIfNotNull(u.mProcStateScreenOffTimeMs);
            u.mProcStateScreenOffTimeMs = null;
            int stateCount3 = in.readInt();
            if (stateCount3 != 0) {
                detachIfNotNull(u.mProcStateScreenOffTimeMs);
                u.mProcStateScreenOffTimeMs = TimeInFreqMultiStateCounter.readFromParcel(in, this.mOnBatteryScreenOffTimeBase, 8, getCpuFreqCount(), this.mClock.elapsedRealtime());
            }
            if (in.readInt() != 0) {
                detachIfNotNull(u.mMobileRadioApWakeupCount);
                u.mMobileRadioApWakeupCount = new LongSamplingCounter(this.mOnBatteryTimeBase);
                u.mMobileRadioApWakeupCount.readSummaryFromParcelLocked(in);
            } else {
                detachIfNotNull(u.mMobileRadioApWakeupCount);
                u.mMobileRadioApWakeupCount = null;
            }
            if (in.readInt() != 0) {
                detachIfNotNull(u.mWifiRadioApWakeupCount);
                u.mWifiRadioApWakeupCount = new LongSamplingCounter(this.mOnBatteryTimeBase);
                u.mWifiRadioApWakeupCount.readSummaryFromParcelLocked(in);
            } else {
                detachIfNotNull(u.mWifiRadioApWakeupCount);
                u.mWifiRadioApWakeupCount = null;
            }
            u.mUidMeasuredEnergyStats = MeasuredEnergyStats.createAndReadSummaryFromParcel(this.mMeasuredEnergyStatsConfig, in);
            int NW = in.readInt();
            if (NW > MAX_WAKELOCKS_PER_UID + 1) {
                throw new ParcelFormatException("File corrupt: too many wake locks " + NW);
            }
            for (int iw = 0; iw < NW; iw++) {
                String wlName = in.readString();
                u.readWakeSummaryFromParcelLocked(wlName, in);
            }
            int NS2 = in.readInt();
            if (NS2 > MAX_WAKELOCKS_PER_UID + 1) {
                throw new ParcelFormatException("File corrupt: too many syncs " + NS2);
            }
            for (int is = 0; is < NS2; is++) {
                String name = in.readString();
                u.readSyncSummaryFromParcelLocked(name, in);
            }
            int NJ = in.readInt();
            if (NJ > MAX_WAKELOCKS_PER_UID + 1) {
                throw new ParcelFormatException("File corrupt: too many job timers " + NJ);
            }
            for (int ij = 0; ij < NJ; ij++) {
                String name2 = in.readString();
                u.readJobSummaryFromParcelLocked(name2, in);
            }
            u.readJobCompletionsFromParcelLocked(in);
            u.mJobsDeferredEventCount.readSummaryFromParcelLocked(in);
            u.mJobsDeferredCount.readSummaryFromParcelLocked(in);
            u.mJobsFreshnessTimeMs.readSummaryFromParcelLocked(in);
            detachIfNotNull(u.mJobsFreshnessBuckets);
            for (int i17 = 0; i17 < JOB_FRESHNESS_BUCKETS.length; i17++) {
                if (in.readInt() != 0) {
                    u.mJobsFreshnessBuckets[i17] = new Counter(u.mBsi.mOnBatteryTimeBase);
                    u.mJobsFreshnessBuckets[i17].readSummaryFromParcelLocked(in);
                }
            }
            int NP = in.readInt();
            if (NP > 1000) {
                throw new ParcelFormatException("File corrupt: too many sensors " + NP);
            }
            int is2 = 0;
            while (is2 < NP) {
                int seNumber = in.readInt();
                if (in.readInt() == 0) {
                    elapsedRealtimeMs = elapsedRealtimeMs2;
                } else {
                    elapsedRealtimeMs = elapsedRealtimeMs2;
                    u.getSensorTimerLocked(seNumber, true).readSummaryFromParcelLocked(in);
                }
                is2++;
                elapsedRealtimeMs2 = elapsedRealtimeMs;
            }
            long elapsedRealtimeMs3 = elapsedRealtimeMs2;
            int NP2 = in.readInt();
            if (NP2 > 1000) {
                throw new ParcelFormatException("File corrupt: too many processes " + NP2);
            }
            for (int ip = 0; ip < NP2; ip++) {
                String procName = in.readString();
                Uid.Proc p = u.getProcessStatsLocked(procName);
                p.mUserTimeMs = in.readLong();
                p.mSystemTimeMs = in.readLong();
                p.mForegroundTimeMs = in.readLong();
                p.mStarts = in.readInt();
                p.mNumCrashes = in.readInt();
                p.mNumAnrs = in.readInt();
                p.readExcessivePowerFromParcelLocked(in);
            }
            int NP3 = in.readInt();
            if (NP3 > 10000) {
                throw new ParcelFormatException("File corrupt: too many packages " + NP3);
            }
            int ip2 = 0;
            while (ip2 < NP3) {
                String pkgName = in.readString();
                detachIfNotNull(u.mPackageStats.get(pkgName));
                Uid.Pkg p2 = u.getPackageStatsLocked(pkgName);
                int NWA = in.readInt();
                if (NWA > 10000) {
                    throw new ParcelFormatException("File corrupt: too many wakeup alarms " + NWA);
                }
                p2.mWakeupAlarms.clear();
                int iwa = 0;
                while (iwa < NWA) {
                    int NS3 = NS2;
                    String tag2 = in.readString();
                    int NPKG3 = NPKG;
                    Counter c2 = new Counter(this.mOnBatteryScreenOffTimeBase);
                    c2.readSummaryFromParcelLocked(in);
                    p2.mWakeupAlarms.put(tag2, c2);
                    iwa++;
                    NS2 = NS3;
                    NPKG = NPKG3;
                    NSORPMS = NSORPMS;
                }
                int NSORPMS2 = NSORPMS;
                int NPKG4 = NPKG;
                NS2 = in.readInt();
                if (NS2 > 10000) {
                    throw new ParcelFormatException("File corrupt: too many services " + NS2);
                }
                int is3 = 0;
                while (is3 < NS2) {
                    String servName = in.readString();
                    Uid.Pkg.Serv s = u.getServiceStatsLocked(pkgName, servName);
                    s.mStartTimeMs = in.readLong();
                    s.mStarts = in.readInt();
                    s.mLaunches = in.readInt();
                    is3++;
                    u = u;
                    pkgName = pkgName;
                }
                ip2++;
                NPKG = NPKG4;
                NSORPMS = NSORPMS2;
            }
            NS = iu + 1;
            NMS = NMS2;
            numTags = numTags2;
            NRPMS3 = NRPMS4;
            NU = NU2;
            config = config2;
            measuredEnergyStats = measuredEnergyStats2;
            elapsedRealtimeMs2 = elapsedRealtimeMs3;
            c = 10000;
        }
        this.mBinderThreadCpuTimesUs = LongSamplingCounterArray.readSummaryFromParcelLocked(in, this.mOnBatteryTimeBase);
    }

    /* JADX DEBUG: Failed to insert an additional move for type inference into block B:340:0x05b7 */
    public void writeSummaryToParcel(Parcel out, boolean inclHistory) {
        int i;
        boolean z;
        pullPendingStateUpdatesLocked();
        getStartClockTime();
        long nowUptime = this.mClock.uptimeMillis() * 1000;
        long nowRealtime = this.mClock.elapsedRealtime() * 1000;
        out.writeInt(208);
        out.writeBoolean(inclHistory);
        boolean z2 = true;
        if (inclHistory) {
            writeHistoryBuffer(out, true);
            this.mBatteryStatsHistory.writeToParcel(out);
        }
        out.writeInt(this.mHistoryTagPool.size());
        for (Map.Entry<BatteryStats.HistoryTag, Integer> ent : this.mHistoryTagPool.entrySet()) {
            BatteryStats.HistoryTag tag = ent.getKey();
            out.writeInt(ent.getValue().intValue());
            out.writeString(tag.string);
            out.writeInt(tag.uid);
        }
        out.writeInt(this.mStartCount);
        boolean z3 = false;
        out.writeLong(computeUptime(nowUptime, 0));
        out.writeLong(computeRealtime(nowRealtime, 0));
        out.writeLong(this.mStartClockTimeMs);
        out.writeString(this.mStartPlatformVersion);
        out.writeString(this.mEndPlatformVersion);
        this.mOnBatteryTimeBase.writeSummaryToParcel(out, nowUptime, nowRealtime);
        this.mOnBatteryScreenOffTimeBase.writeSummaryToParcel(out, nowUptime, nowRealtime);
        out.writeInt(this.mDischargeUnplugLevel);
        out.writeInt(this.mDischargePlugLevel);
        out.writeInt(this.mDischargeCurrentLevel);
        out.writeInt(this.mCurrentBatteryLevel);
        out.writeInt(this.mEstimatedBatteryCapacityMah);
        out.writeInt(this.mLastLearnedBatteryCapacityUah);
        out.writeInt(this.mMinLearnedBatteryCapacityUah);
        out.writeInt(this.mMaxLearnedBatteryCapacityUah);
        out.writeInt(getLowDischargeAmountSinceCharge());
        out.writeInt(getHighDischargeAmountSinceCharge());
        out.writeInt(getDischargeAmountScreenOnSinceCharge());
        out.writeInt(getDischargeAmountScreenOffSinceCharge());
        out.writeInt(getDischargeAmountScreenDozeSinceCharge());
        this.mDischargeStepTracker.writeToParcel(out);
        this.mChargeStepTracker.writeToParcel(out);
        this.mDailyDischargeStepTracker.writeToParcel(out);
        this.mDailyChargeStepTracker.writeToParcel(out);
        this.mDischargeCounter.writeSummaryFromParcelLocked(out);
        this.mDischargeScreenOffCounter.writeSummaryFromParcelLocked(out);
        this.mDischargeScreenDozeCounter.writeSummaryFromParcelLocked(out);
        this.mDischargeLightDozeCounter.writeSummaryFromParcelLocked(out);
        this.mDischargeDeepDozeCounter.writeSummaryFromParcelLocked(out);
        ArrayList<BatteryStats.PackageChange> arrayList = this.mDailyPackageChanges;
        if (arrayList != null) {
            int NPKG = arrayList.size();
            out.writeInt(NPKG);
            for (int i2 = 0; i2 < NPKG; i2++) {
                BatteryStats.PackageChange pc = this.mDailyPackageChanges.get(i2);
                out.writeString(pc.mPackageName);
                out.writeInt(pc.mUpdate ? 1 : 0);
                out.writeLong(pc.mVersionCode);
            }
        } else {
            out.writeInt(0);
        }
        out.writeLong(this.mDailyStartTimeMs);
        out.writeLong(this.mNextMinDailyDeadlineMs);
        out.writeLong(this.mNextMaxDailyDeadlineMs);
        out.writeLong(this.mBatteryTimeToFullSeconds);
        MeasuredEnergyStats.Config.writeToParcel(this.mMeasuredEnergyStatsConfig, out);
        MeasuredEnergyStats.writeSummaryToParcel(this.mGlobalMeasuredEnergyStats, out);
        this.mScreenOnTimer.writeSummaryFromParcelLocked(out, nowRealtime);
        this.mScreenDozeTimer.writeSummaryFromParcelLocked(out, nowRealtime);
        for (int i3 = 0; i3 < 5; i3++) {
            this.mScreenBrightnessTimer[i3].writeSummaryFromParcelLocked(out, nowRealtime);
        }
        this.mInteractiveTimer.writeSummaryFromParcelLocked(out, nowRealtime);
        this.mPowerSaveModeEnabledTimer.writeSummaryFromParcelLocked(out, nowRealtime);
        out.writeLong(this.mLongestLightIdleTimeMs);
        out.writeLong(this.mLongestFullIdleTimeMs);
        this.mDeviceIdleModeLightTimer.writeSummaryFromParcelLocked(out, nowRealtime);
        this.mDeviceIdleModeFullTimer.writeSummaryFromParcelLocked(out, nowRealtime);
        this.mDeviceLightIdlingTimer.writeSummaryFromParcelLocked(out, nowRealtime);
        this.mDeviceIdlingTimer.writeSummaryFromParcelLocked(out, nowRealtime);
        this.mPhoneOnTimer.writeSummaryFromParcelLocked(out, nowRealtime);
        for (int i4 = 0; i4 < CellSignalStrength.getNumSignalStrengthLevels() + 1; i4++) {
            this.mPhoneSignalStrengthsTimer[i4].writeSummaryFromParcelLocked(out, nowRealtime);
        }
        this.mPhoneSignalScanningTimer.writeSummaryFromParcelLocked(out, nowRealtime);
        for (int i5 = 0; i5 < NUM_DATA_CONNECTION_TYPES; i5++) {
            this.mPhoneDataConnectionsTimer[i5].writeSummaryFromParcelLocked(out, nowRealtime);
        }
        for (int i6 = 0; i6 < 10; i6++) {
            this.mNetworkByteActivityCounters[i6].writeSummaryFromParcelLocked(out);
            this.mNetworkPacketActivityCounters[i6].writeSummaryFromParcelLocked(out);
        }
        int numRat = this.mPerRatBatteryStats.length;
        out.writeInt(numRat);
        for (int i7 = 0; i7 < numRat; i7++) {
            RadioAccessTechnologyBatteryStats ratStat = this.mPerRatBatteryStats[i7];
            if (ratStat == null) {
                out.writeInt(0);
            } else {
                out.writeInt(1);
                ratStat.writeSummaryToParcel(out, nowRealtime);
            }
        }
        this.mMobileRadioActiveTimer.writeSummaryFromParcelLocked(out, nowRealtime);
        this.mMobileRadioActivePerAppTimer.writeSummaryFromParcelLocked(out, nowRealtime);
        this.mMobileRadioActiveAdjustedTime.writeSummaryFromParcelLocked(out);
        this.mMobileRadioActiveUnknownTime.writeSummaryFromParcelLocked(out);
        this.mMobileRadioActiveUnknownCount.writeSummaryFromParcelLocked(out);
        this.mWifiMulticastWakelockTimer.writeSummaryFromParcelLocked(out, nowRealtime);
        this.mWifiOnTimer.writeSummaryFromParcelLocked(out, nowRealtime);
        this.mGlobalWifiRunningTimer.writeSummaryFromParcelLocked(out, nowRealtime);
        for (int i8 = 0; i8 < 8; i8++) {
            this.mWifiStateTimer[i8].writeSummaryFromParcelLocked(out, nowRealtime);
        }
        for (int i9 = 0; i9 < 13; i9++) {
            this.mWifiSupplStateTimer[i9].writeSummaryFromParcelLocked(out, nowRealtime);
        }
        for (int i10 = 0; i10 < 5; i10++) {
            this.mWifiSignalStrengthsTimer[i10].writeSummaryFromParcelLocked(out, nowRealtime);
        }
        this.mWifiActiveTimer.writeSummaryFromParcelLocked(out, nowRealtime);
        this.mWifiActivity.writeSummaryToParcel(out);
        int i11 = 0;
        while (true) {
            StopwatchTimer[] stopwatchTimerArr = this.mGpsSignalQualityTimer;
            if (i11 >= stopwatchTimerArr.length) {
                break;
            }
            stopwatchTimerArr[i11].writeSummaryFromParcelLocked(out, nowRealtime);
            i11++;
        }
        this.mBluetoothActivity.writeSummaryToParcel(out);
        this.mModemActivity.writeSummaryToParcel(out);
        out.writeInt(this.mHasWifiReporting ? 1 : 0);
        out.writeInt(this.mHasBluetoothReporting ? 1 : 0);
        out.writeInt(this.mHasModemReporting ? 1 : 0);
        out.writeInt(this.mNumConnectivityChange);
        this.mFlashlightOnTimer.writeSummaryFromParcelLocked(out, nowRealtime);
        this.mCameraOnTimer.writeSummaryFromParcelLocked(out, nowRealtime);
        this.mBluetoothScanTimer.writeSummaryFromParcelLocked(out, nowRealtime);
        out.writeInt(this.mRpmStats.size());
        for (Map.Entry<String, SamplingTimer> ent2 : this.mRpmStats.entrySet()) {
            Timer rpmt = ent2.getValue();
            if (rpmt != null) {
                out.writeInt(1);
                out.writeString(ent2.getKey());
                rpmt.writeSummaryFromParcelLocked(out, nowRealtime);
            } else {
                out.writeInt(0);
            }
        }
        out.writeInt(this.mScreenOffRpmStats.size());
        for (Map.Entry<String, SamplingTimer> ent3 : this.mScreenOffRpmStats.entrySet()) {
            Timer rpmt2 = ent3.getValue();
            if (rpmt2 != null) {
                out.writeInt(1);
                out.writeString(ent3.getKey());
                rpmt2.writeSummaryFromParcelLocked(out, nowRealtime);
            } else {
                out.writeInt(0);
            }
        }
        out.writeInt(this.mKernelWakelockStats.size());
        for (Map.Entry<String, SamplingTimer> ent4 : this.mKernelWakelockStats.entrySet()) {
            Timer kwlt = ent4.getValue();
            if (kwlt != null) {
                out.writeInt(1);
                out.writeString(ent4.getKey());
                kwlt.writeSummaryFromParcelLocked(out, nowRealtime);
            } else {
                out.writeInt(0);
            }
        }
        out.writeInt(this.mWakeupReasonStats.size());
        for (Map.Entry<String, SamplingTimer> ent5 : this.mWakeupReasonStats.entrySet()) {
            SamplingTimer timer = ent5.getValue();
            if (timer != null) {
                out.writeInt(1);
                out.writeString(ent5.getKey());
                timer.writeSummaryFromParcelLocked(out, nowRealtime);
            } else {
                out.writeInt(0);
            }
        }
        out.writeInt(this.mKernelMemoryStats.size());
        for (int i12 = 0; i12 < this.mKernelMemoryStats.size(); i12++) {
            Timer kmt = this.mKernelMemoryStats.valueAt(i12);
            if (kmt != null) {
                out.writeInt(1);
                out.writeLong(this.mKernelMemoryStats.keyAt(i12));
                kmt.writeSummaryFromParcelLocked(out, nowRealtime);
            } else {
                out.writeInt(0);
            }
        }
        int NU = this.mUidStats.size();
        out.writeInt(NU);
        int iu = 0;
        while (iu < NU) {
            out.writeInt(this.mUidStats.keyAt(iu));
            Uid u = this.mUidStats.valueAt(iu);
            int numRat2 = numRat;
            int iu2 = iu;
            int NU2 = NU;
            u.mOnBatteryBackgroundTimeBase.writeSummaryToParcel(out, nowUptime, nowRealtime);
            u.mOnBatteryScreenOffBackgroundTimeBase.writeSummaryToParcel(out, nowUptime, nowRealtime);
            if (u.mWifiRunningTimer != null) {
                out.writeInt(z2 ? 1 : 0);
                u.mWifiRunningTimer.writeSummaryFromParcelLocked(out, nowRealtime);
            } else {
                out.writeInt(z3 ? 1 : 0);
            }
            if (u.mFullWifiLockTimer != null) {
                out.writeInt(z2 ? 1 : 0);
                u.mFullWifiLockTimer.writeSummaryFromParcelLocked(out, nowRealtime);
            } else {
                out.writeInt(z3 ? 1 : 0);
            }
            if (u.mWifiScanTimer != null) {
                out.writeInt(z2 ? 1 : 0);
                u.mWifiScanTimer.writeSummaryFromParcelLocked(out, nowRealtime);
            } else {
                out.writeInt(z3 ? 1 : 0);
            }
            for (int i13 = 0; i13 < 5; i13++) {
                if (u.mWifiBatchedScanTimer[i13] != null) {
                    out.writeInt(z2 ? 1 : 0);
                    u.mWifiBatchedScanTimer[i13].writeSummaryFromParcelLocked(out, nowRealtime);
                } else {
                    out.writeInt(z3 ? 1 : 0);
                }
            }
            if (u.mWifiMulticastTimer != null) {
                out.writeInt(z2 ? 1 : 0);
                u.mWifiMulticastTimer.writeSummaryFromParcelLocked(out, nowRealtime);
            } else {
                out.writeInt(z3 ? 1 : 0);
            }
            if (u.mAudioTurnedOnTimer != null) {
                out.writeInt(z2 ? 1 : 0);
                u.mAudioTurnedOnTimer.writeSummaryFromParcelLocked(out, nowRealtime);
            } else {
                out.writeInt(z3 ? 1 : 0);
            }
            if (u.mVideoTurnedOnTimer != null) {
                out.writeInt(z2 ? 1 : 0);
                u.mVideoTurnedOnTimer.writeSummaryFromParcelLocked(out, nowRealtime);
            } else {
                out.writeInt(z3 ? 1 : 0);
            }
            if (u.mFlashlightTurnedOnTimer != null) {
                out.writeInt(z2 ? 1 : 0);
                u.mFlashlightTurnedOnTimer.writeSummaryFromParcelLocked(out, nowRealtime);
            } else {
                out.writeInt(z3 ? 1 : 0);
            }
            if (u.mCameraTurnedOnTimer != null) {
                out.writeInt(z2 ? 1 : 0);
                u.mCameraTurnedOnTimer.writeSummaryFromParcelLocked(out, nowRealtime);
            } else {
                out.writeInt(z3 ? 1 : 0);
            }
            if (u.mForegroundActivityTimer != null) {
                out.writeInt(z2 ? 1 : 0);
                u.mForegroundActivityTimer.writeSummaryFromParcelLocked(out, nowRealtime);
            } else {
                out.writeInt(z3 ? 1 : 0);
            }
            if (u.mForegroundServiceTimer != null) {
                out.writeInt(z2 ? 1 : 0);
                u.mForegroundServiceTimer.writeSummaryFromParcelLocked(out, nowRealtime);
            } else {
                out.writeInt(z3 ? 1 : 0);
            }
            if (u.mAggregatedPartialWakelockTimer != null) {
                out.writeInt(z2 ? 1 : 0);
                u.mAggregatedPartialWakelockTimer.writeSummaryFromParcelLocked(out, nowRealtime);
            } else {
                out.writeInt(z3 ? 1 : 0);
            }
            if (u.mBluetoothScanTimer != null) {
                out.writeInt(z2 ? 1 : 0);
                u.mBluetoothScanTimer.writeSummaryFromParcelLocked(out, nowRealtime);
            } else {
                out.writeInt(z3 ? 1 : 0);
            }
            if (u.mBluetoothUnoptimizedScanTimer != null) {
                out.writeInt(z2 ? 1 : 0);
                u.mBluetoothUnoptimizedScanTimer.writeSummaryFromParcelLocked(out, nowRealtime);
            } else {
                out.writeInt(z3 ? 1 : 0);
            }
            if (u.mBluetoothScanResultCounter != null) {
                out.writeInt(z2 ? 1 : 0);
                u.mBluetoothScanResultCounter.writeSummaryFromParcelLocked(out);
            } else {
                out.writeInt(z3 ? 1 : 0);
            }
            if (u.mBluetoothScanResultBgCounter != null) {
                out.writeInt(z2 ? 1 : 0);
                u.mBluetoothScanResultBgCounter.writeSummaryFromParcelLocked(out);
            } else {
                out.writeInt(z3 ? 1 : 0);
            }
            for (int i14 = 0; i14 < 7; i14++) {
                if (u.mProcessStateTimer[i14] != null) {
                    out.writeInt(z2 ? 1 : 0);
                    u.mProcessStateTimer[i14].writeSummaryFromParcelLocked(out, nowRealtime);
                } else {
                    out.writeInt(z3 ? 1 : 0);
                }
            }
            if (u.mVibratorOnTimer != null) {
                out.writeInt(z2 ? 1 : 0);
                u.mVibratorOnTimer.writeSummaryFromParcelLocked(out, nowRealtime);
            } else {
                out.writeInt(z3 ? 1 : 0);
            }
            if (u.mUserActivityCounters == null) {
                out.writeInt(z3 ? 1 : 0);
            } else {
                out.writeInt(z2 ? 1 : 0);
                for (int i15 = 0; i15 < Uid.NUM_USER_ACTIVITY_TYPES; i15++) {
                    u.mUserActivityCounters[i15].writeSummaryFromParcelLocked(out);
                }
            }
            if (u.mNetworkByteActivityCounters == null) {
                out.writeInt(z3 ? 1 : 0);
            } else {
                out.writeInt(z2 ? 1 : 0);
                for (int i16 = 0; i16 < 10; i16++) {
                    u.mNetworkByteActivityCounters[i16].writeSummaryFromParcelLocked(out);
                    u.mNetworkPacketActivityCounters[i16].writeSummaryFromParcelLocked(out);
                }
                if (u.mMobileRadioActiveTime != null) {
                    out.writeBoolean(z2);
                    u.mMobileRadioActiveTime.writeToParcel(out);
                } else {
                    out.writeBoolean(z3);
                }
                u.mMobileRadioActiveCount.writeSummaryFromParcelLocked(out);
            }
            u.mUserCpuTime.writeSummaryFromParcelLocked(out);
            u.mSystemCpuTime.writeSummaryFromParcelLocked(out);
            if (u.mCpuClusterSpeedTimesUs != null) {
                out.writeInt(z2 ? 1 : 0);
                out.writeInt(u.mCpuClusterSpeedTimesUs.length);
                LongSamplingCounter[][] longSamplingCounterArr = u.mCpuClusterSpeedTimesUs;
                int length = longSamplingCounterArr.length;
                int i17 = z3 ? 1 : 0;
                boolean z4 = z2;
                while (i17 < length) {
                    LongSamplingCounter[] cpuSpeeds = longSamplingCounterArr[i17];
                    if (cpuSpeeds != null) {
                        out.writeInt(z4 ? 1 : 0);
                        out.writeInt(cpuSpeeds.length);
                        int length2 = cpuSpeeds.length;
                        int i18 = z3 ? 1 : 0;
                        int i19 = z4;
                        while (i18 < length2) {
                            LongSamplingCounter c = cpuSpeeds[i18];
                            if (c != null) {
                                out.writeInt(i19);
                                c.writeSummaryFromParcelLocked(out);
                                z = false;
                            } else {
                                z = false;
                                out.writeInt(0);
                            }
                            i18++;
                            z3 = z;
                            i19 = 1;
                        }
                        i = z3;
                    } else {
                        i = z3 ? 1 : 0;
                        out.writeInt(i);
                    }
                    i17++;
                    z3 = i;
                    z4 = true;
                }
                boolean z5 = z3 ? 1 : 0;
            } else {
                out.writeInt(z3 ? 1 : 0);
            }
            LongSamplingCounterArray.writeSummaryToParcelLocked(out, u.mCpuFreqTimeMs);
            LongSamplingCounterArray.writeSummaryToParcelLocked(out, u.mScreenOffCpuFreqTimeMs);
            if (u.mCpuActiveTimeMs != null) {
                out.writeInt(u.mCpuActiveTimeMs.getStateCount());
                u.mCpuActiveTimeMs.writeToParcel(out);
            } else {
                out.writeInt(0);
            }
            u.mCpuClusterTimesMs.writeSummaryToParcelLocked(out);
            if (u.mProcStateTimeMs != null) {
                out.writeInt(u.mProcStateTimeMs.getStateCount());
                u.mProcStateTimeMs.writeToParcel(out);
            } else {
                out.writeInt(0);
            }
            if (u.mProcStateScreenOffTimeMs != null) {
                out.writeInt(u.mProcStateScreenOffTimeMs.getStateCount());
                u.mProcStateScreenOffTimeMs.writeToParcel(out);
            } else {
                out.writeInt(0);
            }
            if (u.mMobileRadioApWakeupCount != null) {
                out.writeInt(1);
                u.mMobileRadioApWakeupCount.writeSummaryFromParcelLocked(out);
            } else {
                out.writeInt(0);
            }
            if (u.mWifiRadioApWakeupCount != null) {
                out.writeInt(1);
                u.mWifiRadioApWakeupCount.writeSummaryFromParcelLocked(out);
            } else {
                out.writeInt(0);
            }
            MeasuredEnergyStats.writeSummaryToParcel(u.mUidMeasuredEnergyStats, out);
            ArrayMap<String, Uid.Wakelock> wakeStats = u.mWakelockStats.getMap();
            int NW = wakeStats.size();
            out.writeInt(NW);
            for (int iw = 0; iw < NW; iw++) {
                out.writeString(wakeStats.keyAt(iw));
                Uid.Wakelock wl = wakeStats.valueAt(iw);
                if (wl.mTimerFull != null) {
                    out.writeInt(1);
                    wl.mTimerFull.writeSummaryFromParcelLocked(out, nowRealtime);
                } else {
                    out.writeInt(0);
                }
                if (wl.mTimerPartial != null) {
                    out.writeInt(1);
                    wl.mTimerPartial.writeSummaryFromParcelLocked(out, nowRealtime);
                } else {
                    out.writeInt(0);
                }
                if (wl.mTimerWindow != null) {
                    out.writeInt(1);
                    wl.mTimerWindow.writeSummaryFromParcelLocked(out, nowRealtime);
                } else {
                    out.writeInt(0);
                }
                if (wl.mTimerDraw != null) {
                    out.writeInt(1);
                    wl.mTimerDraw.writeSummaryFromParcelLocked(out, nowRealtime);
                } else {
                    out.writeInt(0);
                }
            }
            ArrayMap<String, DualTimer> syncStats = u.mSyncStats.getMap();
            int NS = syncStats.size();
            out.writeInt(NS);
            for (int is = 0; is < NS; is++) {
                out.writeString(syncStats.keyAt(is));
                syncStats.valueAt(is).writeSummaryFromParcelLocked(out, nowRealtime);
            }
            ArrayMap<String, DualTimer> jobStats = u.mJobStats.getMap();
            int NJ = jobStats.size();
            out.writeInt(NJ);
            for (int ij = 0; ij < NJ; ij++) {
                out.writeString(jobStats.keyAt(ij));
                jobStats.valueAt(ij).writeSummaryFromParcelLocked(out, nowRealtime);
            }
            u.writeJobCompletionsToParcelLocked(out);
            u.mJobsDeferredEventCount.writeSummaryFromParcelLocked(out);
            u.mJobsDeferredCount.writeSummaryFromParcelLocked(out);
            u.mJobsFreshnessTimeMs.writeSummaryFromParcelLocked(out);
            for (int i20 = 0; i20 < JOB_FRESHNESS_BUCKETS.length; i20++) {
                if (u.mJobsFreshnessBuckets[i20] != null) {
                    out.writeInt(1);
                    u.mJobsFreshnessBuckets[i20].writeSummaryFromParcelLocked(out);
                } else {
                    out.writeInt(0);
                }
            }
            int NSE = u.mSensorStats.size();
            out.writeInt(NSE);
            int ise = 0;
            while (ise < NSE) {
                ArrayMap<String, Uid.Wakelock> wakeStats2 = wakeStats;
                out.writeInt(u.mSensorStats.keyAt(ise));
                Uid.Sensor se = u.mSensorStats.valueAt(ise);
                int NW2 = NW;
                if (se.mTimer != null) {
                    out.writeInt(1);
                    se.mTimer.writeSummaryFromParcelLocked(out, nowRealtime);
                } else {
                    out.writeInt(0);
                }
                ise++;
                wakeStats = wakeStats2;
                NW = NW2;
            }
            int NP = u.mProcessStats.size();
            out.writeInt(NP);
            int ip = 0;
            while (ip < NP) {
                out.writeString(u.mProcessStats.keyAt(ip));
                Uid.Proc ps = u.mProcessStats.valueAt(ip);
                out.writeLong(ps.mUserTimeMs);
                out.writeLong(ps.mSystemTimeMs);
                out.writeLong(ps.mForegroundTimeMs);
                out.writeInt(ps.mStarts);
                out.writeInt(ps.mNumCrashes);
                out.writeInt(ps.mNumAnrs);
                ps.writeExcessivePowerToParcelLocked(out);
                ip++;
                syncStats = syncStats;
                NS = NS;
            }
            int is2 = u.mPackageStats.size();
            out.writeInt(is2);
            if (is2 > 0) {
                Iterator<Map.Entry<String, Uid.Pkg>> it = u.mPackageStats.entrySet().iterator();
                while (it.hasNext()) {
                    Map.Entry<String, Uid.Pkg> ent6 = it.next();
                    out.writeString(ent6.getKey());
                    Uid.Pkg ps2 = ent6.getValue();
                    int NP2 = is2;
                    int NWA = ps2.mWakeupAlarms.size();
                    out.writeInt(NWA);
                    Iterator<Map.Entry<String, Uid.Pkg>> it2 = it;
                    int iwa = 0;
                    while (iwa < NWA) {
                        out.writeString(ps2.mWakeupAlarms.keyAt(iwa));
                        ps2.mWakeupAlarms.valueAt(iwa).writeSummaryFromParcelLocked(out);
                        iwa++;
                        NWA = NWA;
                    }
                    int NS2 = ps2.mServiceStats.size();
                    out.writeInt(NS2);
                    int is3 = 0;
                    while (is3 < NS2) {
                        out.writeString(ps2.mServiceStats.keyAt(is3));
                        Uid.Pkg.Serv ss = ps2.mServiceStats.valueAt(is3);
                        Map.Entry<String, Uid.Pkg> ent7 = ent6;
                        long time = ss.getStartTimeToNowLocked(this.mOnBatteryTimeBase.getUptime(nowUptime) / 1000);
                        out.writeLong(time);
                        out.writeInt(ss.mStarts);
                        out.writeInt(ss.mLaunches);
                        is3++;
                        ent6 = ent7;
                        NS2 = NS2;
                    }
                    is2 = NP2;
                    it = it2;
                }
            }
            iu = iu2 + 1;
            numRat = numRat2;
            NU = NU2;
            z2 = true;
            z3 = false;
        }
        LongSamplingCounterArray.writeSummaryToParcelLocked(out, this.mBinderThreadCpuTimesUs);
    }

    public void readFromParcel(Parcel in) {
        readFromParcelLocked(in);
    }

    void readFromParcelLocked(Parcel in) {
        int magic = in.readInt();
        if (magic != MAGIC) {
            throw new ParcelFormatException("Bad magic number: #" + Integer.toHexString(magic));
        }
        readHistoryBuffer(in);
        this.mBatteryStatsHistory.readFromParcel(in);
        this.mStartCount = in.readInt();
        this.mStartClockTimeMs = in.readLong();
        this.mStartPlatformVersion = in.readString();
        this.mEndPlatformVersion = in.readString();
        this.mUptimeUs = in.readLong();
        this.mUptimeStartUs = in.readLong();
        this.mRealtimeUs = in.readLong();
        this.mRealtimeStartUs = in.readLong();
        this.mOnBattery = in.readInt() != 0;
        this.mEstimatedBatteryCapacityMah = in.readInt();
        this.mLastLearnedBatteryCapacityUah = in.readInt();
        this.mMinLearnedBatteryCapacityUah = in.readInt();
        this.mMaxLearnedBatteryCapacityUah = in.readInt();
        this.mOnBatteryInternal = false;
        this.mOnBatteryTimeBase.readFromParcel(in);
        this.mOnBatteryScreenOffTimeBase.readFromParcel(in);
        this.mScreenState = 0;
        this.mScreenOnTimer = new StopwatchTimer(this.mClock, null, -1, null, this.mOnBatteryTimeBase, in);
        this.mScreenDozeTimer = new StopwatchTimer(this.mClock, null, -1, null, this.mOnBatteryTimeBase, in);
        for (int i = 0; i < 5; i++) {
            this.mScreenBrightnessTimer[i] = new StopwatchTimer(this.mClock, null, (-100) - i, null, this.mOnBatteryTimeBase, in);
        }
        this.mInteractive = false;
        this.mInteractiveTimer = new StopwatchTimer(this.mClock, null, -10, null, this.mOnBatteryTimeBase, in);
        this.mPhoneOn = false;
        this.mPowerSaveModeEnabledTimer = new StopwatchTimer(this.mClock, null, -2, null, this.mOnBatteryTimeBase, in);
        this.mLongestLightIdleTimeMs = in.readLong();
        this.mLongestFullIdleTimeMs = in.readLong();
        this.mDeviceIdleModeLightTimer = new StopwatchTimer(this.mClock, null, -14, null, this.mOnBatteryTimeBase, in);
        this.mDeviceIdleModeFullTimer = new StopwatchTimer(this.mClock, null, -11, null, this.mOnBatteryTimeBase, in);
        this.mDeviceLightIdlingTimer = new StopwatchTimer(this.mClock, null, -15, null, this.mOnBatteryTimeBase, in);
        this.mDeviceIdlingTimer = new StopwatchTimer(this.mClock, null, -12, null, this.mOnBatteryTimeBase, in);
        this.mPhoneOnTimer = new StopwatchTimer(this.mClock, null, -3, null, this.mOnBatteryTimeBase, in);
        for (int i2 = 0; i2 < CellSignalStrength.getNumSignalStrengthLevels() + 1; i2++) {
            this.mPhoneSignalStrengthsTimer[i2] = new StopwatchTimer(this.mClock, null, (-200) - i2, null, this.mOnBatteryTimeBase, in);
        }
        this.mPhoneSignalScanningTimer = new StopwatchTimer(this.mClock, null, -199, null, this.mOnBatteryTimeBase, in);
        for (int i3 = 0; i3 < NUM_DATA_CONNECTION_TYPES; i3++) {
            this.mPhoneDataConnectionsTimer[i3] = new StopwatchTimer(this.mClock, null, (-300) - i3, null, this.mOnBatteryTimeBase, in);
        }
        for (int i4 = 0; i4 < 10; i4++) {
            this.mNetworkByteActivityCounters[i4] = new LongSamplingCounter(this.mOnBatteryTimeBase, in);
            this.mNetworkPacketActivityCounters[i4] = new LongSamplingCounter(this.mOnBatteryTimeBase, in);
        }
        this.mMobileRadioPowerState = 1;
        this.mMobileRadioActiveTimer = new StopwatchTimer(this.mClock, null, -400, null, this.mOnBatteryTimeBase, in);
        this.mMobileRadioActivePerAppTimer = new StopwatchTimer(this.mClock, null, -401, null, this.mOnBatteryTimeBase, in);
        this.mMobileRadioActiveAdjustedTime = new LongSamplingCounter(this.mOnBatteryTimeBase, in);
        this.mMobileRadioActiveUnknownTime = new LongSamplingCounter(this.mOnBatteryTimeBase, in);
        this.mMobileRadioActiveUnknownCount = new LongSamplingCounter(this.mOnBatteryTimeBase, in);
        this.mWifiMulticastWakelockTimer = new StopwatchTimer(this.mClock, null, -4, null, this.mOnBatteryTimeBase, in);
        this.mWifiRadioPowerState = 1;
        this.mWifiOn = false;
        this.mWifiOnTimer = new StopwatchTimer(this.mClock, null, -4, null, this.mOnBatteryTimeBase, in);
        this.mGlobalWifiRunning = false;
        this.mGlobalWifiRunningTimer = new StopwatchTimer(this.mClock, null, -5, null, this.mOnBatteryTimeBase, in);
        for (int i5 = 0; i5 < 8; i5++) {
            this.mWifiStateTimer[i5] = new StopwatchTimer(this.mClock, null, (-600) - i5, null, this.mOnBatteryTimeBase, in);
        }
        for (int i6 = 0; i6 < 13; i6++) {
            this.mWifiSupplStateTimer[i6] = new StopwatchTimer(this.mClock, null, (-700) - i6, null, this.mOnBatteryTimeBase, in);
        }
        for (int i7 = 0; i7 < 5; i7++) {
            this.mWifiSignalStrengthsTimer[i7] = new StopwatchTimer(this.mClock, null, (-800) - i7, null, this.mOnBatteryTimeBase, in);
        }
        this.mWifiActiveTimer = new StopwatchTimer(this.mClock, null, -900, null, this.mOnBatteryTimeBase, in);
        this.mWifiActivity = new ControllerActivityCounterImpl(this.mClock, this.mOnBatteryTimeBase, 1, in);
        int i8 = 0;
        while (true) {
            StopwatchTimer[] stopwatchTimerArr = this.mGpsSignalQualityTimer;
            if (i8 >= stopwatchTimerArr.length) {
                break;
            }
            stopwatchTimerArr[i8] = new StopwatchTimer(this.mClock, null, (-1000) - i8, null, this.mOnBatteryTimeBase, in);
            i8++;
        }
        this.mBluetoothActivity = new ControllerActivityCounterImpl(this.mClock, this.mOnBatteryTimeBase, 1, in);
        this.mModemActivity = new ControllerActivityCounterImpl(this.mClock, this.mOnBatteryTimeBase, ModemActivityInfo.getNumTxPowerLevels(), in);
        this.mHasWifiReporting = in.readInt() != 0;
        this.mHasBluetoothReporting = in.readInt() != 0;
        this.mHasModemReporting = in.readInt() != 0;
        this.mNumConnectivityChange = in.readInt();
        this.mAudioOnNesting = 0;
        this.mAudioOnTimer = new StopwatchTimer(this.mClock, null, -7, null, this.mOnBatteryTimeBase);
        this.mVideoOnNesting = 0;
        this.mVideoOnTimer = new StopwatchTimer(this.mClock, null, -8, null, this.mOnBatteryTimeBase);
        this.mFlashlightOnNesting = 0;
        this.mFlashlightOnTimer = new StopwatchTimer(this.mClock, null, -9, null, this.mOnBatteryTimeBase, in);
        this.mCameraOnNesting = 0;
        this.mCameraOnTimer = new StopwatchTimer(this.mClock, null, -13, null, this.mOnBatteryTimeBase, in);
        this.mBluetoothScanNesting = 0;
        this.mBluetoothScanTimer = new StopwatchTimer(this.mClock, null, -14, null, this.mOnBatteryTimeBase, in);
        this.mDischargeUnplugLevel = in.readInt();
        this.mDischargePlugLevel = in.readInt();
        this.mDischargeCurrentLevel = in.readInt();
        this.mCurrentBatteryLevel = in.readInt();
        this.mLowDischargeAmountSinceCharge = in.readInt();
        this.mHighDischargeAmountSinceCharge = in.readInt();
        this.mDischargeAmountScreenOn = in.readInt();
        this.mDischargeAmountScreenOnSinceCharge = in.readInt();
        this.mDischargeAmountScreenOff = in.readInt();
        this.mDischargeAmountScreenOffSinceCharge = in.readInt();
        this.mDischargeAmountScreenDoze = in.readInt();
        this.mDischargeAmountScreenDozeSinceCharge = in.readInt();
        this.mDischargeStepTracker.readFromParcel(in);
        this.mChargeStepTracker.readFromParcel(in);
        this.mDischargeCounter = new LongSamplingCounter(this.mOnBatteryTimeBase, in);
        this.mDischargeScreenOffCounter = new LongSamplingCounter(this.mOnBatteryScreenOffTimeBase, in);
        this.mDischargeScreenDozeCounter = new LongSamplingCounter(this.mOnBatteryTimeBase, in);
        this.mDischargeLightDozeCounter = new LongSamplingCounter(this.mOnBatteryTimeBase, in);
        this.mDischargeDeepDozeCounter = new LongSamplingCounter(this.mOnBatteryTimeBase, in);
        this.mLastWriteTimeMs = in.readLong();
        this.mBatteryTimeToFullSeconds = in.readLong();
        MeasuredEnergyStats.Config config = MeasuredEnergyStats.Config.createFromParcel(in);
        MeasuredEnergyStats measuredEnergyStats = MeasuredEnergyStats.createFromParcel(this.mMeasuredEnergyStatsConfig, in);
        if (config != null && Arrays.equals(config.getStateNames(), getBatteryConsumerProcessStateNames())) {
            this.mMeasuredEnergyStatsConfig = config;
            this.mGlobalMeasuredEnergyStats = measuredEnergyStats;
        }
        this.mRpmStats.clear();
        int NRPMS = in.readInt();
        for (int irpm = 0; irpm < NRPMS; irpm++) {
            if (in.readInt() != 0) {
                String rpmName = in.readString();
                SamplingTimer rpmt = new SamplingTimer(this.mClock, this.mOnBatteryTimeBase, in);
                this.mRpmStats.put(rpmName, rpmt);
            }
        }
        this.mScreenOffRpmStats.clear();
        int NSORPMS = in.readInt();
        for (int irpm2 = 0; irpm2 < NSORPMS; irpm2++) {
            if (in.readInt() != 0) {
                String rpmName2 = in.readString();
                SamplingTimer rpmt2 = new SamplingTimer(this.mClock, this.mOnBatteryScreenOffTimeBase, in);
                this.mScreenOffRpmStats.put(rpmName2, rpmt2);
            }
        }
        this.mKernelWakelockStats.clear();
        int NKW = in.readInt();
        for (int ikw = 0; ikw < NKW; ikw++) {
            if (in.readInt() != 0) {
                String wakelockName = in.readString();
                SamplingTimer kwlt = new SamplingTimer(this.mClock, this.mOnBatteryScreenOffTimeBase, in);
                this.mKernelWakelockStats.put(wakelockName, kwlt);
            }
        }
        synchronized (mLock) {
            this.mWakeupReasonStats.clear();
            int NWR = in.readInt();
            for (int iwr = 0; iwr < NWR; iwr++) {
                if (in.readInt() != 0) {
                    String reasonName = in.readString();
                    SamplingTimer timer = new SamplingTimer(this.mClock, this.mOnBatteryTimeBase, in);
                    this.mWakeupReasonStats.put(reasonName, timer);
                }
            }
        }
        this.mKernelMemoryStats.clear();
        int nmt = in.readInt();
        for (int imt = 0; imt < nmt; imt++) {
            if (in.readInt() != 0) {
                Long bucket = Long.valueOf(in.readLong());
                SamplingTimer kmt = new SamplingTimer(this.mClock, this.mOnBatteryTimeBase, in);
                this.mKernelMemoryStats.put(bucket.longValue(), kmt);
            }
        }
        this.mPartialTimers.clear();
        this.mFullTimers.clear();
        this.mWindowTimers.clear();
        this.mWifiRunningTimers.clear();
        this.mFullWifiLockTimers.clear();
        this.mWifiScanTimers.clear();
        this.mWifiBatchedScanTimers.clear();
        this.mWifiMulticastTimers.clear();
        this.mAudioTurnedOnTimers.clear();
        this.mVideoTurnedOnTimers.clear();
        this.mFlashlightTurnedOnTimers.clear();
        this.mCameraTurnedOnTimers.clear();
        this.mUidStats.clear();
        long elapsedRealtimeMs = this.mClock.elapsedRealtime();
        long uptimeMs = this.mClock.uptimeMillis();
        int i9 = 0;
        for (int numUids = in.readInt(); i9 < numUids; numUids = numUids) {
            int uid = in.readInt();
            Uid u = new Uid(this, uid, elapsedRealtimeMs, uptimeMs);
            u.readFromParcelLocked(this.mOnBatteryTimeBase, this.mOnBatteryScreenOffTimeBase, in);
            this.mUidStats.append(uid, u);
            i9++;
            nmt = nmt;
        }
        this.mBinderThreadCpuTimesUs = LongSamplingCounterArray.readFromParcel(in, this.mOnBatteryTimeBase);
    }

    @Override // android.os.Parcelable
    public void writeToParcel(Parcel out, int flags) {
        writeToParcelLocked(out, true, flags);
    }

    @Override // android.os.BatteryStats
    public void writeToParcelWithoutUids(Parcel out, int flags) {
        writeToParcelLocked(out, false, flags);
    }

    void writeToParcelLocked(Parcel out, boolean inclUids, int flags) {
        pullPendingStateUpdatesLocked();
        updateSystemServiceCallStats();
        getStartClockTime();
        long uSecUptime = this.mClock.uptimeMillis() * 1000;
        long uSecRealtime = this.mClock.elapsedRealtime() * 1000;
        this.mOnBatteryTimeBase.getRealtime(uSecRealtime);
        this.mOnBatteryScreenOffTimeBase.getRealtime(uSecRealtime);
        out.writeInt(MAGIC);
        writeHistoryBuffer(out, true);
        this.mBatteryStatsHistory.writeToParcel(out);
        out.writeInt(this.mStartCount);
        out.writeLong(this.mStartClockTimeMs);
        out.writeString(this.mStartPlatformVersion);
        out.writeString(this.mEndPlatformVersion);
        out.writeLong(this.mUptimeUs);
        out.writeLong(this.mUptimeStartUs);
        out.writeLong(this.mRealtimeUs);
        out.writeLong(this.mRealtimeStartUs);
        out.writeInt(this.mOnBattery ? 1 : 0);
        out.writeInt(this.mEstimatedBatteryCapacityMah);
        out.writeInt(this.mLastLearnedBatteryCapacityUah);
        out.writeInt(this.mMinLearnedBatteryCapacityUah);
        out.writeInt(this.mMaxLearnedBatteryCapacityUah);
        this.mOnBatteryTimeBase.writeToParcel(out, uSecUptime, uSecRealtime);
        this.mOnBatteryScreenOffTimeBase.writeToParcel(out, uSecUptime, uSecRealtime);
        this.mScreenOnTimer.writeToParcel(out, uSecRealtime);
        this.mScreenDozeTimer.writeToParcel(out, uSecRealtime);
        for (int i = 0; i < 5; i++) {
            this.mScreenBrightnessTimer[i].writeToParcel(out, uSecRealtime);
        }
        this.mInteractiveTimer.writeToParcel(out, uSecRealtime);
        this.mPowerSaveModeEnabledTimer.writeToParcel(out, uSecRealtime);
        out.writeLong(this.mLongestLightIdleTimeMs);
        out.writeLong(this.mLongestFullIdleTimeMs);
        this.mDeviceIdleModeLightTimer.writeToParcel(out, uSecRealtime);
        this.mDeviceIdleModeFullTimer.writeToParcel(out, uSecRealtime);
        this.mDeviceLightIdlingTimer.writeToParcel(out, uSecRealtime);
        this.mDeviceIdlingTimer.writeToParcel(out, uSecRealtime);
        this.mPhoneOnTimer.writeToParcel(out, uSecRealtime);
        for (int i2 = 0; i2 < CellSignalStrength.getNumSignalStrengthLevels() + 1; i2++) {
            this.mPhoneSignalStrengthsTimer[i2].writeToParcel(out, uSecRealtime);
        }
        this.mPhoneSignalScanningTimer.writeToParcel(out, uSecRealtime);
        for (int i3 = 0; i3 < NUM_DATA_CONNECTION_TYPES; i3++) {
            this.mPhoneDataConnectionsTimer[i3].writeToParcel(out, uSecRealtime);
        }
        for (int i4 = 0; i4 < 10; i4++) {
            this.mNetworkByteActivityCounters[i4].writeToParcel(out);
            this.mNetworkPacketActivityCounters[i4].writeToParcel(out);
        }
        this.mMobileRadioActiveTimer.writeToParcel(out, uSecRealtime);
        this.mMobileRadioActivePerAppTimer.writeToParcel(out, uSecRealtime);
        this.mMobileRadioActiveAdjustedTime.writeToParcel(out);
        this.mMobileRadioActiveUnknownTime.writeToParcel(out);
        this.mMobileRadioActiveUnknownCount.writeToParcel(out);
        this.mWifiMulticastWakelockTimer.writeToParcel(out, uSecRealtime);
        this.mWifiOnTimer.writeToParcel(out, uSecRealtime);
        this.mGlobalWifiRunningTimer.writeToParcel(out, uSecRealtime);
        for (int i5 = 0; i5 < 8; i5++) {
            this.mWifiStateTimer[i5].writeToParcel(out, uSecRealtime);
        }
        for (int i6 = 0; i6 < 13; i6++) {
            this.mWifiSupplStateTimer[i6].writeToParcel(out, uSecRealtime);
        }
        for (int i7 = 0; i7 < 5; i7++) {
            this.mWifiSignalStrengthsTimer[i7].writeToParcel(out, uSecRealtime);
        }
        this.mWifiActiveTimer.writeToParcel(out, uSecRealtime);
        this.mWifiActivity.writeToParcel(out, 0);
        int i8 = 0;
        while (true) {
            StopwatchTimer[] stopwatchTimerArr = this.mGpsSignalQualityTimer;
            if (i8 >= stopwatchTimerArr.length) {
                break;
            }
            stopwatchTimerArr[i8].writeToParcel(out, uSecRealtime);
            i8++;
        }
        this.mBluetoothActivity.writeToParcel(out, 0);
        this.mModemActivity.writeToParcel(out, 0);
        out.writeInt(this.mHasWifiReporting ? 1 : 0);
        out.writeInt(this.mHasBluetoothReporting ? 1 : 0);
        out.writeInt(this.mHasModemReporting ? 1 : 0);
        out.writeInt(this.mNumConnectivityChange);
        this.mFlashlightOnTimer.writeToParcel(out, uSecRealtime);
        this.mCameraOnTimer.writeToParcel(out, uSecRealtime);
        this.mBluetoothScanTimer.writeToParcel(out, uSecRealtime);
        out.writeInt(this.mDischargeUnplugLevel);
        out.writeInt(this.mDischargePlugLevel);
        out.writeInt(this.mDischargeCurrentLevel);
        out.writeInt(this.mCurrentBatteryLevel);
        out.writeInt(this.mLowDischargeAmountSinceCharge);
        out.writeInt(this.mHighDischargeAmountSinceCharge);
        out.writeInt(this.mDischargeAmountScreenOn);
        out.writeInt(this.mDischargeAmountScreenOnSinceCharge);
        out.writeInt(this.mDischargeAmountScreenOff);
        out.writeInt(this.mDischargeAmountScreenOffSinceCharge);
        out.writeInt(this.mDischargeAmountScreenDoze);
        out.writeInt(this.mDischargeAmountScreenDozeSinceCharge);
        this.mDischargeStepTracker.writeToParcel(out);
        this.mChargeStepTracker.writeToParcel(out);
        this.mDischargeCounter.writeToParcel(out);
        this.mDischargeScreenOffCounter.writeToParcel(out);
        this.mDischargeScreenDozeCounter.writeToParcel(out);
        this.mDischargeLightDozeCounter.writeToParcel(out);
        this.mDischargeDeepDozeCounter.writeToParcel(out);
        out.writeLong(this.mLastWriteTimeMs);
        out.writeLong(this.mBatteryTimeToFullSeconds);
        MeasuredEnergyStats.Config.writeToParcel(this.mMeasuredEnergyStatsConfig, out);
        if (this.mGlobalMeasuredEnergyStats != null) {
            out.writeInt(1);
            this.mGlobalMeasuredEnergyStats.writeToParcel(out);
        } else {
            out.writeInt(0);
        }
        out.writeInt(this.mRpmStats.size());
        for (Map.Entry<String, SamplingTimer> ent : this.mRpmStats.entrySet()) {
            SamplingTimer rpmt = ent.getValue();
            if (rpmt != null) {
                out.writeInt(1);
                out.writeString(ent.getKey());
                rpmt.writeToParcel(out, uSecRealtime);
            } else {
                out.writeInt(0);
            }
        }
        out.writeInt(this.mScreenOffRpmStats.size());
        for (Map.Entry<String, SamplingTimer> ent2 : this.mScreenOffRpmStats.entrySet()) {
            SamplingTimer rpmt2 = ent2.getValue();
            if (rpmt2 != null) {
                out.writeInt(1);
                out.writeString(ent2.getKey());
                rpmt2.writeToParcel(out, uSecRealtime);
            } else {
                out.writeInt(0);
            }
        }
        if (inclUids) {
            out.writeInt(this.mKernelWakelockStats.size());
            for (Map.Entry<String, SamplingTimer> ent3 : this.mKernelWakelockStats.entrySet()) {
                SamplingTimer kwlt = ent3.getValue();
                if (kwlt != null) {
                    out.writeInt(1);
                    out.writeString(ent3.getKey());
                    kwlt.writeToParcel(out, uSecRealtime);
                } else {
                    out.writeInt(0);
                }
            }
            out.writeInt(this.mWakeupReasonStats.size());
            for (Map.Entry<String, SamplingTimer> ent4 : this.mWakeupReasonStats.entrySet()) {
                SamplingTimer timer = ent4.getValue();
                if (timer != null) {
                    out.writeInt(1);
                    out.writeString(ent4.getKey());
                    timer.writeToParcel(out, uSecRealtime);
                } else {
                    out.writeInt(0);
                }
            }
        } else {
            out.writeInt(0);
            out.writeInt(0);
        }
        out.writeInt(this.mKernelMemoryStats.size());
        for (int i9 = 0; i9 < this.mKernelMemoryStats.size(); i9++) {
            SamplingTimer kmt = this.mKernelMemoryStats.valueAt(i9);
            if (kmt != null) {
                out.writeInt(1);
                out.writeLong(this.mKernelMemoryStats.keyAt(i9));
                kmt.writeToParcel(out, uSecRealtime);
            } else {
                out.writeInt(0);
            }
        }
        if (inclUids) {
            int size = this.mUidStats.size();
            out.writeInt(size);
            for (int i10 = 0; i10 < size; i10++) {
                out.writeInt(this.mUidStats.keyAt(i10));
                Uid uid = this.mUidStats.valueAt(i10);
                uid.writeToParcelLocked(out, uSecUptime, uSecRealtime);
            }
        } else {
            out.writeInt(0);
        }
        LongSamplingCounterArray.writeToParcel(out, this.mBinderThreadCpuTimesUs);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void writeCpuSpeedCountersToParcel(Parcel out, LongSamplingCounter[][] counters) {
        if (counters == null) {
            out.writeInt(0);
            return;
        }
        out.writeInt(1);
        out.writeInt(counters.length);
        for (LongSamplingCounter[] counterArray : counters) {
            if (counterArray == null) {
                out.writeInt(0);
            } else {
                out.writeInt(1);
                out.writeInt(counterArray.length);
                for (LongSamplingCounter c : counterArray) {
                    if (c != null) {
                        out.writeInt(1);
                        c.writeToParcel(out);
                    } else {
                        out.writeInt(0);
                    }
                }
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public LongSamplingCounter[][] readCpuSpeedCountersFromParcel(Parcel in) {
        if (in.readInt() != 0) {
            int numCpuClusters = in.readInt();
            PowerProfile powerProfile = this.mPowerProfile;
            if (powerProfile != null && powerProfile.getNumCpuClusters() != numCpuClusters) {
                throw new ParcelFormatException("Incompatible number of cpu clusters");
            }
            LongSamplingCounter[][] counters = new LongSamplingCounter[numCpuClusters];
            for (int cluster = 0; cluster < numCpuClusters; cluster++) {
                if (in.readInt() != 0) {
                    int numSpeeds = in.readInt();
                    PowerProfile powerProfile2 = this.mPowerProfile;
                    if (powerProfile2 != null && powerProfile2.getNumSpeedStepsInCpuCluster(cluster) != numSpeeds) {
                        throw new ParcelFormatException("Incompatible number of cpu speeds");
                    }
                    LongSamplingCounter[] cpuSpeeds = new LongSamplingCounter[numSpeeds];
                    counters[cluster] = cpuSpeeds;
                    for (int speed = 0; speed < numSpeeds; speed++) {
                        if (in.readInt() != 0) {
                            cpuSpeeds[speed] = new LongSamplingCounter(this.mOnBatteryTimeBase, in);
                        }
                    }
                } else {
                    counters[cluster] = null;
                }
            }
            return counters;
        }
        return null;
    }

    @Override // android.os.BatteryStats
    public void prepareForDumpLocked() {
        pullPendingStateUpdatesLocked();
        getStartClockTime();
        updateSystemServiceCallStats();
    }

    @Override // android.os.BatteryStats
    public void dumpLocked(Context context, PrintWriter pw, int flags, int reqUid, long histStart) {
        super.dumpLocked(context, pw, flags, reqUid, histStart);
        pw.print("Per process state tracking available: ");
        pw.println(trackPerProcStateCpuTimes());
        pw.print("Total cpu time reads: ");
        pw.println(this.mNumSingleUidCpuTimeReads);
        pw.print("Batching Duration (min): ");
        pw.println((this.mClock.uptimeMillis() - this.mCpuTimeReadsTrackingStartTimeMs) / 60000);
        pw.print("All UID cpu time reads since the later of device start or stats reset: ");
        pw.println(this.mNumAllUidCpuTimeReads);
        pw.print("UIDs removed since the later of device start or stats reset: ");
        pw.println(this.mNumUidsRemoved);
        pw.println("Currently mapped isolated uids:");
        int numIsolatedUids = this.mIsolatedUids.size();
        for (int i = 0; i < numIsolatedUids; i++) {
            int isolatedUid = this.mIsolatedUids.keyAt(i);
            int ownerUid = this.mIsolatedUids.valueAt(i);
            int refCount = this.mIsolatedUidRefCounts.get(isolatedUid);
            pw.println("  " + isolatedUid + Session.SUBSESSION_SEPARATION_CHAR + ownerUid + " (ref count = " + refCount + NavigationBarInflaterView.KEY_CODE_END);
        }
        pw.println();
        dumpConstantsLocked(pw);
        pw.println();
        dumpMeasuredEnergyStatsLocked(pw);
    }
}
