package com.android.server.am;

import android.app.ActivityManager;
import android.app.ActivityThread;
import android.database.ContentObserver;
import android.net.Uri;
import android.os.Debug;
import android.os.Handler;
import android.os.Message;
import android.os.Process;
import android.os.SystemClock;
import android.os.SystemProperties;
import android.os.Trace;
import android.provider.DeviceConfig;
import android.provider.Settings;
import android.text.TextUtils;
import android.util.EventLog;
import android.util.Slog;
import android.util.SparseArray;
import com.android.internal.os.ProcLocksReader;
import com.android.internal.util.FrameworkStatsLog;
import com.android.server.ServiceThread;
import com.android.server.am.CachedAppOptimizer;
import com.android.server.job.controllers.JobStatus;
import com.android.server.slice.SliceClientPermissions;
import com.android.server.timezonedetector.ServiceConfigAccessor;
import com.transsion.hubcore.server.am.ITranCachedAppOptimizer;
import com.transsion.hubcore.server.am.ITranOomAdjuster;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.function.Consumer;
/* loaded from: classes.dex */
public final class CachedAppOptimizer {
    static final int ASYNC_RECEIVED_WHILE_FROZEN = 2;
    private static final String ATRACE_COMPACTION_TRACK = "Compaction";
    private static final int COMPACT_ACTION_ANON = 2;
    private static final int COMPACT_ACTION_ANON_FLAG = 2;
    private static final int COMPACT_ACTION_FILE = 1;
    private static final int COMPACT_ACTION_FILE_FLAG = 1;
    private static final int COMPACT_ACTION_FULL = 3;
    private static final int COMPACT_ACTION_MEMFUSION_FLAG = 11;
    private static final int COMPACT_ACTION_MEMFUSION_FULL = 11;
    private static final int COMPACT_ACTION_NONE = 0;
    static final double COMPACT_DOWNGRADE_FREE_SWAP_THRESHOLD = 0.2d;
    static final int COMPACT_PROCESS_BFGS = 4;
    static final int COMPACT_PROCESS_FORCE_MSG = 11;
    static final int COMPACT_PROCESS_FULL = 2;
    static final int COMPACT_PROCESS_MSG = 1;
    static final int COMPACT_PROCESS_PERSISTENT = 3;
    static final int COMPACT_PROCESS_SOME = 1;
    static final int COMPACT_SYSTEM_MSG = 2;
    static final int DEFAULT_COMPACT_ACTION_1 = 1;
    static final int DEFAULT_COMPACT_ACTION_11 = 11;
    static final int DEFAULT_COMPACT_ACTION_2 = 3;
    static final long DEFAULT_COMPACT_FULL_DELTA_RSS_THROTTLE_KB = 8000;
    static final long DEFAULT_COMPACT_FULL_RSS_THROTTLE_KB = 12000;
    static final long DEFAULT_COMPACT_THROTTLE_1 = 5000;
    static final long DEFAULT_COMPACT_THROTTLE_2 = 10000;
    static final long DEFAULT_COMPACT_THROTTLE_3 = 500;
    static final long DEFAULT_COMPACT_THROTTLE_4 = 10000;
    static final long DEFAULT_COMPACT_THROTTLE_5 = 600000;
    static final long DEFAULT_COMPACT_THROTTLE_6 = 600000;
    static final long DEFAULT_COMPACT_THROTTLE_MAX_OOM_ADJ = 999;
    static final long DEFAULT_COMPACT_THROTTLE_MIN_OOM_ADJ = 900;
    static final long DEFAULT_FREEZER_DEBOUNCE_TIMEOUT = 600000;
    static final float DEFAULT_STATSD_SAMPLE_RATE = 0.1f;
    static final int DO_FREEZE = 1;
    static final String KEY_COMPACT_ACTION_1 = "compact_action_1";
    static final String KEY_COMPACT_ACTION_11 = "compact_action_11";
    static final String KEY_COMPACT_ACTION_2 = "compact_action_2";
    static final String KEY_COMPACT_FULL_DELTA_RSS_THROTTLE_KB = "compact_full_delta_rss_throttle_kb";
    static final String KEY_COMPACT_FULL_RSS_THROTTLE_KB = "compact_full_rss_throttle_kb";
    static final String KEY_COMPACT_PROC_STATE_THROTTLE = "compact_proc_state_throttle";
    static final String KEY_COMPACT_STATSD_SAMPLE_RATE = "compact_statsd_sample_rate";
    static final String KEY_COMPACT_THROTTLE_1 = "compact_throttle_1";
    static final String KEY_COMPACT_THROTTLE_2 = "compact_throttle_2";
    static final String KEY_COMPACT_THROTTLE_3 = "compact_throttle_3";
    static final String KEY_COMPACT_THROTTLE_4 = "compact_throttle_4";
    static final String KEY_COMPACT_THROTTLE_5 = "compact_throttle_5";
    static final String KEY_COMPACT_THROTTLE_6 = "compact_throttle_6";
    static final String KEY_COMPACT_THROTTLE_MAX_OOM_ADJ = "compact_throttle_max_oom_adj";
    static final String KEY_COMPACT_THROTTLE_MIN_OOM_ADJ = "compact_throttle_min_oom_adj";
    static final String KEY_FREEZER_DEBOUNCE_TIMEOUT = "freeze_debounce_timeout";
    static final String KEY_FREEZER_STATSD_SAMPLE_RATE = "freeze_statsd_sample_rate";
    static final String KEY_USE_COMPACTION = "use_compaction";
    static final String KEY_USE_FREEZER = "use_freezer";
    static final int REPORT_UNFREEZE = 2;
    static final int REPORT_UNFREEZE_MSG = 4;
    private static final int RSS_ANON_INDEX = 2;
    private static final int RSS_FILE_INDEX = 1;
    private static final int RSS_SWAP_INDEX = 3;
    private static final int RSS_TOTAL_INDEX = 0;
    static final int SET_FROZEN_PROCESS_MSG = 3;
    static final int SYNC_RECEIVED_WHILE_FROZEN = 1;
    static final int TXNS_PENDING_WHILE_FROZEN = 4;
    private final ActivityManagerService mAm;
    private long mBfgsCompactRequest;
    private int mBfgsCompactionCount;
    final ServiceThread mCachedAppOptimizerThread;
    volatile String mCompactActionFull;
    volatile String mCompactActionMemFusionFull;
    volatile String mCompactActionSome;
    volatile float mCompactStatsdSampleRate;
    volatile long mCompactThrottleBFGS;
    volatile long mCompactThrottleFullFull;
    volatile long mCompactThrottleFullSome;
    volatile long mCompactThrottleMaxOomAdj;
    volatile long mCompactThrottleMinOomAdj;
    volatile long mCompactThrottlePersistent;
    volatile long mCompactThrottleSomeFull;
    volatile long mCompactThrottleSomeSome;
    Handler mCompactionHandler;
    private Handler mFreezeHandler;
    volatile long mFreezerDebounceTimeout;
    private int mFreezerDisableCount;
    private final Object mFreezerLock;
    private boolean mFreezerOverride;
    volatile float mFreezerStatsdSampleRate;
    private final SparseArray<ProcessRecord> mFrozenProcesses;
    volatile long mFullAnonRssThrottleKb;
    private long mFullCompactRequest;
    private int mFullCompactionCount;
    volatile long mFullDeltaRssThrottleKb;
    LinkedHashMap<Integer, LastCompactionStats> mLastCompactionStats;
    private final DeviceConfig.OnPropertiesChangedListener mOnFlagsChangedListener;
    private final DeviceConfig.OnPropertiesChangedListener mOnNativeBootFlagsChangedListener;
    private final ArrayList<ProcessRecord> mPendingCompactionProcesses;
    private long mPersistentCompactRequest;
    private int mPersistentCompactionCount;
    private final Object mPhenotypeFlagLock;
    private long mProcCompactionsMiscThrottled;
    private long mProcCompactionsNoPidThrottled;
    private long mProcCompactionsOomAdjThrottled;
    private long mProcCompactionsPerformed;
    private long mProcCompactionsRSSThrottled;
    private long mProcCompactionsRequested;
    private long mProcCompactionsTimeThrottled;
    private final ActivityManagerGlobalLock mProcLock;
    private final ProcLocksReader mProcLocksReader;
    final Set<Integer> mProcStateThrottle;
    private final ProcessDependencies mProcessDependencies;
    private final Random mRandom;
    private final SettingsContentObserver mSettingsObserver;
    boolean mSkipCompactFullCheck;
    private long mSomeCompactRequest;
    private int mSomeCompactionCount;
    private long mSystemCompactionsPerformed;
    private PropertyChangedCallbackForTest mTestCallback;
    private final TranAmHooker mTranAmHooker;
    private volatile boolean mUseCompaction;
    private volatile boolean mUseFreezer;
    private static final String[] COMPACT_ACTION_STRING = {"", "file", "anon", "all"};
    static final Boolean DEFAULT_USE_COMPACTION = false;
    static final Boolean DEFAULT_USE_FREEZER = true;
    static final String DEFAULT_COMPACT_PROC_STATE_THROTTLE = String.valueOf(11);
    static final Uri CACHED_APP_FREEZER_ENABLED_URI = Settings.Global.getUriFor("cached_apps_freezer");
    public static String TAG_FREEZER = "Freezer/CloudSwitch";
    private static boolean mFreezerCloudSwitch = true;
    private static boolean mSkipRepeat = false;

    /* loaded from: classes.dex */
    interface ProcessDependencies {
        long[] getRss(int i);

        void performCompaction(String str, int i) throws IOException;
    }

    /* loaded from: classes.dex */
    interface PropertyChangedCallbackForTest {
        void onPropertyChanged();
    }

    private static native void cancelCompaction();

    /* JADX INFO: Access modifiers changed from: private */
    public static native void compactProcess(int i, int i2);

    /* JADX INFO: Access modifiers changed from: private */
    public native void compactSystem();

    /* JADX INFO: Access modifiers changed from: private */
    public static native int freezeBinder(int i, boolean z);

    /* JADX INFO: Access modifiers changed from: private */
    public static native int getBinderFreezeInfo(int i);

    private static native double getFreeSwapPercent();

    private static native String getFreezerCheckPath();

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes.dex */
    public final class SettingsContentObserver extends ContentObserver {
        SettingsContentObserver() {
            super(CachedAppOptimizer.this.mAm.mHandler);
        }

        @Override // android.database.ContentObserver
        public void onChange(boolean selfChange, Uri uri) {
            if (CachedAppOptimizer.CACHED_APP_FREEZER_ENABLED_URI.equals(uri)) {
                synchronized (CachedAppOptimizer.this.mPhenotypeFlagLock) {
                    CachedAppOptimizer.this.updateUseFreezer();
                }
            }
        }
    }

    public CachedAppOptimizer(ActivityManagerService am) {
        this(am, null, new DefaultProcessDependencies());
    }

    CachedAppOptimizer(ActivityManagerService am, PropertyChangedCallbackForTest callback, ProcessDependencies processDependencies) {
        this.mPendingCompactionProcesses = new ArrayList<>();
        this.mFrozenProcesses = new SparseArray<>();
        this.mFreezerLock = new Object();
        this.mOnFlagsChangedListener = new DeviceConfig.OnPropertiesChangedListener() { // from class: com.android.server.am.CachedAppOptimizer.1
            public void onPropertiesChanged(DeviceConfig.Properties properties) {
                synchronized (CachedAppOptimizer.this.mPhenotypeFlagLock) {
                    for (String name : properties.getKeyset()) {
                        if (CachedAppOptimizer.KEY_USE_COMPACTION.equals(name)) {
                            CachedAppOptimizer.this.updateUseCompaction();
                        } else {
                            if (!CachedAppOptimizer.KEY_COMPACT_ACTION_1.equals(name) && !CachedAppOptimizer.KEY_COMPACT_ACTION_2.equals(name)) {
                                if (!CachedAppOptimizer.KEY_COMPACT_THROTTLE_1.equals(name) && !CachedAppOptimizer.KEY_COMPACT_THROTTLE_2.equals(name) && !CachedAppOptimizer.KEY_COMPACT_THROTTLE_3.equals(name) && !CachedAppOptimizer.KEY_COMPACT_THROTTLE_4.equals(name) && !CachedAppOptimizer.KEY_COMPACT_THROTTLE_5.equals(name) && !CachedAppOptimizer.KEY_COMPACT_THROTTLE_6.equals(name)) {
                                    if (CachedAppOptimizer.KEY_COMPACT_STATSD_SAMPLE_RATE.equals(name)) {
                                        CachedAppOptimizer.this.updateCompactStatsdSampleRate();
                                    } else if (CachedAppOptimizer.KEY_FREEZER_STATSD_SAMPLE_RATE.equals(name)) {
                                        CachedAppOptimizer.this.updateFreezerStatsdSampleRate();
                                    } else if (CachedAppOptimizer.KEY_COMPACT_FULL_RSS_THROTTLE_KB.equals(name)) {
                                        CachedAppOptimizer.this.updateFullRssThrottle();
                                    } else if (CachedAppOptimizer.KEY_COMPACT_FULL_DELTA_RSS_THROTTLE_KB.equals(name)) {
                                        CachedAppOptimizer.this.updateFullDeltaRssThrottle();
                                    } else if (CachedAppOptimizer.KEY_COMPACT_PROC_STATE_THROTTLE.equals(name)) {
                                        CachedAppOptimizer.this.updateProcStateThrottle();
                                    } else if (CachedAppOptimizer.KEY_COMPACT_THROTTLE_MIN_OOM_ADJ.equals(name)) {
                                        CachedAppOptimizer.this.updateMinOomAdjThrottle();
                                    } else if (CachedAppOptimizer.KEY_COMPACT_THROTTLE_MAX_OOM_ADJ.equals(name)) {
                                        CachedAppOptimizer.this.updateMaxOomAdjThrottle();
                                    }
                                }
                                CachedAppOptimizer.this.updateCompactionThrottles();
                            }
                            CachedAppOptimizer.this.updateCompactionActions();
                        }
                    }
                }
                if (CachedAppOptimizer.this.mTestCallback != null) {
                    CachedAppOptimizer.this.mTestCallback.onPropertyChanged();
                }
            }
        };
        this.mOnNativeBootFlagsChangedListener = new DeviceConfig.OnPropertiesChangedListener() { // from class: com.android.server.am.CachedAppOptimizer.2
            public void onPropertiesChanged(DeviceConfig.Properties properties) {
                synchronized (CachedAppOptimizer.this.mPhenotypeFlagLock) {
                    for (String name : properties.getKeyset()) {
                        if (CachedAppOptimizer.KEY_FREEZER_DEBOUNCE_TIMEOUT.equals(name)) {
                            CachedAppOptimizer.this.updateFreezerDebounceTimeout();
                        }
                    }
                }
                if (CachedAppOptimizer.this.mTestCallback != null) {
                    CachedAppOptimizer.this.mTestCallback.onPropertyChanged();
                }
            }
        };
        this.mPhenotypeFlagLock = new Object();
        this.mCompactActionSome = compactActionIntToString(1);
        this.mCompactActionFull = compactActionIntToString(3);
        this.mCompactActionMemFusionFull = compactActionIntToString(11);
        this.mCompactThrottleSomeSome = DEFAULT_COMPACT_THROTTLE_1;
        this.mCompactThrottleSomeFull = JobStatus.DEFAULT_TRIGGER_UPDATE_DELAY;
        this.mCompactThrottleFullSome = 500L;
        this.mCompactThrottleFullFull = JobStatus.DEFAULT_TRIGGER_UPDATE_DELAY;
        this.mCompactThrottleBFGS = 600000L;
        this.mCompactThrottlePersistent = 600000L;
        this.mCompactThrottleMinOomAdj = DEFAULT_COMPACT_THROTTLE_MIN_OOM_ADJ;
        this.mCompactThrottleMaxOomAdj = DEFAULT_COMPACT_THROTTLE_MAX_OOM_ADJ;
        this.mUseCompaction = DEFAULT_USE_COMPACTION.booleanValue();
        this.mUseFreezer = false;
        this.mFreezerDisableCount = 1;
        this.mRandom = new Random();
        this.mCompactStatsdSampleRate = DEFAULT_STATSD_SAMPLE_RATE;
        this.mFreezerStatsdSampleRate = DEFAULT_STATSD_SAMPLE_RATE;
        this.mFullAnonRssThrottleKb = DEFAULT_COMPACT_FULL_RSS_THROTTLE_KB;
        this.mFullDeltaRssThrottleKb = DEFAULT_COMPACT_FULL_DELTA_RSS_THROTTLE_KB;
        this.mFreezerOverride = false;
        this.mFreezerDebounceTimeout = 600000L;
        this.mLastCompactionStats = new LinkedHashMap<Integer, LastCompactionStats>() { // from class: com.android.server.am.CachedAppOptimizer.3
            /* JADX DEBUG: Method arguments types fixed to match base method, original types: [java.util.Map$Entry] */
            @Override // java.util.LinkedHashMap
            protected boolean removeEldestEntry(Map.Entry<Integer, LastCompactionStats> entry) {
                return size() > 100;
            }
        };
        this.mSkipCompactFullCheck = false;
        this.mAm = am;
        this.mProcLock = am.mProcLock;
        this.mCachedAppOptimizerThread = new ServiceThread("CachedAppOptimizerThread", 2, true);
        this.mProcStateThrottle = new HashSet();
        this.mProcessDependencies = processDependencies;
        this.mTestCallback = callback;
        this.mSettingsObserver = new SettingsContentObserver();
        this.mProcLocksReader = new ProcLocksReader();
        this.mTranAmHooker = new TranAmHooker(this);
    }

    public void init() {
        DeviceConfig.addOnPropertiesChangedListener("activity_manager", ActivityThread.currentApplication().getMainExecutor(), this.mOnFlagsChangedListener);
        DeviceConfig.addOnPropertiesChangedListener("activity_manager_native_boot", ActivityThread.currentApplication().getMainExecutor(), this.mOnNativeBootFlagsChangedListener);
        this.mAm.mContext.getContentResolver().registerContentObserver(CACHED_APP_FREEZER_ENABLED_URI, false, this.mSettingsObserver);
        synchronized (this.mPhenotypeFlagLock) {
            updateUseCompaction();
            updateCompactionActions();
            updateCompactionThrottles();
            updateCompactStatsdSampleRate();
            updateFreezerStatsdSampleRate();
            updateFullRssThrottle();
            updateFullDeltaRssThrottle();
            updateProcStateThrottle();
            updateUseFreezer();
            updateMinOomAdjThrottle();
            updateMaxOomAdjThrottle();
        }
    }

    public boolean useCompaction() {
        boolean z;
        synchronized (this.mPhenotypeFlagLock) {
            z = this.mUseCompaction;
        }
        return z;
    }

    public boolean useFreezer() {
        boolean z;
        synchronized (this.mPhenotypeFlagLock) {
            z = this.mUseFreezer;
        }
        return z;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void dump(PrintWriter pw) {
        pw.println("CachedAppOptimizer settings");
        synchronized (this.mPhenotypeFlagLock) {
            pw.println("  use_compaction=" + this.mUseCompaction);
            pw.println("  compact_action_1=" + this.mCompactActionSome);
            pw.println("  compact_action_2=" + this.mCompactActionFull);
            if (ITranCachedAppOptimizer.Instance().isMemFusionEnabled() && this.mCompactActionMemFusionFull != null) {
                pw.println("  compact_action_11=" + this.mCompactActionMemFusionFull);
            }
            pw.println("  compact_throttle_1=" + this.mCompactThrottleSomeSome);
            pw.println("  compact_throttle_2=" + this.mCompactThrottleSomeFull);
            pw.println("  compact_throttle_3=" + this.mCompactThrottleFullSome);
            pw.println("  compact_throttle_4=" + this.mCompactThrottleFullFull);
            pw.println("  compact_throttle_5=" + this.mCompactThrottleBFGS);
            pw.println("  compact_throttle_6=" + this.mCompactThrottlePersistent);
            pw.println("  compact_throttle_min_oom_adj=" + this.mCompactThrottleMinOomAdj);
            pw.println("  compact_throttle_max_oom_adj=" + this.mCompactThrottleMaxOomAdj);
            pw.println("  compact_statsd_sample_rate=" + this.mCompactStatsdSampleRate);
            pw.println("  compact_full_rss_throttle_kb=" + this.mFullAnonRssThrottleKb);
            pw.println("  compact_full_delta_rss_throttle_kb=" + this.mFullDeltaRssThrottleKb);
            pw.println("  compact_proc_state_throttle=" + Arrays.toString(this.mProcStateThrottle.toArray(new Integer[0])));
            pw.println(" Requested:  " + this.mSomeCompactRequest + " some, " + this.mFullCompactRequest + " full, " + this.mPersistentCompactRequest + " persistent, " + this.mBfgsCompactRequest + " BFGS compactions.");
            pw.println(" Performed: " + this.mSomeCompactionCount + " some, " + this.mFullCompactionCount + " full, " + this.mPersistentCompactionCount + " persistent, " + this.mBfgsCompactionCount + " BFGS compactions.");
            pw.println(" Process Compactions Requested: " + this.mProcCompactionsRequested);
            pw.println(" Process Compactions Performed: " + this.mProcCompactionsPerformed);
            long compactionsThrottled = this.mProcCompactionsRequested - this.mProcCompactionsPerformed;
            pw.println(" Process Compactions Throttled: " + compactionsThrottled);
            double compactThrottlePercentage = (compactionsThrottled / this.mProcCompactionsRequested) * 100.0d;
            pw.println(" Process Compactions Throttle Percentage: " + compactThrottlePercentage);
            pw.println("        NoPid Throttled: " + this.mProcCompactionsNoPidThrottled);
            pw.println("        OomAdj Throttled: " + this.mProcCompactionsOomAdjThrottled);
            pw.println("        Time Throttled: " + this.mProcCompactionsTimeThrottled);
            pw.println("        RSS Throttled: " + this.mProcCompactionsRSSThrottled);
            pw.println("        Misc Throttled: " + this.mProcCompactionsMiscThrottled);
            long unaccountedThrottled = ((((compactionsThrottled - this.mProcCompactionsNoPidThrottled) - this.mProcCompactionsOomAdjThrottled) - this.mProcCompactionsTimeThrottled) - this.mProcCompactionsRSSThrottled) - this.mProcCompactionsMiscThrottled;
            pw.println("        Unaccounted Throttled: " + unaccountedThrottled);
            pw.println(" System Compactions Performed: " + this.mSystemCompactionsPerformed);
            pw.println("  Tracking last compaction stats for " + this.mLastCompactionStats.size() + " processes.");
            pw.println("  use_freezer=" + this.mUseFreezer);
            pw.println("  freeze_statsd_sample_rate=" + this.mFreezerStatsdSampleRate);
            pw.println("  freeze_debounce_timeout=" + this.mFreezerDebounceTimeout);
            synchronized (this.mProcLock) {
                ActivityManagerService.boostPriorityForProcLockedSection();
                int size = this.mFrozenProcesses.size();
                pw.println("  Apps frozen: " + size);
                for (int i = 0; i < size; i++) {
                    ProcessRecord app = this.mFrozenProcesses.valueAt(i);
                    pw.println("    " + app.mOptRecord.getFreezeUnfreezeTime() + ": " + app.getPid() + " " + app.processName);
                }
                if (!this.mPendingCompactionProcesses.isEmpty()) {
                    pw.println("  Pending compactions:");
                    int size2 = this.mPendingCompactionProcesses.size();
                    for (int i2 = 0; i2 < size2; i2++) {
                        ProcessRecord app2 = this.mPendingCompactionProcesses.get(i2);
                        pw.println("    pid: " + app2.getPid() + ". name: " + app2.processName + ". hasPendingCompact: " + app2.mOptRecord.hasPendingCompact());
                    }
                }
            }
            ActivityManagerService.resetPriorityAfterProcLockedSection();
            if (ActivityManagerDebugConfig.DEBUG_COMPACTION) {
                for (Map.Entry<Integer, LastCompactionStats> entry : this.mLastCompactionStats.entrySet()) {
                    int pid = entry.getKey().intValue();
                    LastCompactionStats stats = entry.getValue();
                    pw.println("    " + pid + ": " + Arrays.toString(stats.getRssAfterCompaction()));
                }
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void compactAppSome(ProcessRecord app, boolean force) {
        app.mOptRecord.setReqCompactAction(1);
        this.mSomeCompactRequest++;
        compactApp(app, force, "some");
    }

    boolean meetsCompactionRequirements(ProcessRecord proc) {
        if (this.mAm.mInternal.isPendingTopUid(proc.uid)) {
            if (ActivityManagerDebugConfig.DEBUG_COMPACTION) {
                Slog.d("ActivityManager", "Skip compaction since UID is active for  " + proc.processName);
            }
            return false;
        } else if (proc.mState.hasForegroundActivities()) {
            if (ActivityManagerDebugConfig.DEBUG_COMPACTION) {
                Slog.e("ActivityManager", "Skip compaction as process " + proc.processName + " has foreground activities");
            }
            return false;
        } else {
            return true;
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void compactAppFull(ProcessRecord app, boolean force) {
        boolean oomAdjEnteredCached = (((long) app.mState.getSetAdj()) < this.mCompactThrottleMinOomAdj || ((long) app.mState.getSetAdj()) > this.mCompactThrottleMaxOomAdj) && ((long) app.mState.getCurAdj()) >= this.mCompactThrottleMinOomAdj && ((long) app.mState.getCurAdj()) <= this.mCompactThrottleMaxOomAdj;
        if (ActivityManagerDebugConfig.DEBUG_COMPACTION) {
            Slog.d("ActivityManager", " compactAppFull requested for " + app.processName + " force: " + force + " oomAdjEnteredCached: " + oomAdjEnteredCached);
        }
        this.mFullCompactRequest++;
        if (force || oomAdjEnteredCached) {
            app.mOptRecord.setReqCompactAction(2);
            compactApp(app, force, "Full");
        } else if (ActivityManagerDebugConfig.DEBUG_COMPACTION) {
            Slog.d("ActivityManager", "Skipping full compaction for " + app.processName + " oom adj score changed from " + app.mState.getSetAdj() + " to " + app.mState.getCurAdj());
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void compactAppPersistent(ProcessRecord app) {
        app.mOptRecord.setReqCompactAction(3);
        this.mPersistentCompactRequest++;
        compactApp(app, false, "Persistent");
    }

    boolean compactApp(ProcessRecord app, boolean force, String compactRequestType) {
        if (!app.mOptRecord.hasPendingCompact() && meetsCompactionRequirements(app)) {
            String processName = app.processName != null ? app.processName : "";
            if (ActivityManagerDebugConfig.DEBUG_COMPACTION) {
                Slog.d("ActivityManager", "compactApp " + compactRequestType + " " + processName);
            }
            Trace.instantForTrack(64L, ATRACE_COMPACTION_TRACK, "compactApp " + compactRequestType + " " + processName);
            app.mOptRecord.setHasPendingCompact(true);
            app.mOptRecord.setForceCompact(force);
            this.mPendingCompactionProcesses.add(app);
            Handler handler = this.mCompactionHandler;
            handler.sendMessage(handler.obtainMessage(1, app.mState.getCurAdj(), app.mState.getSetProcState()));
            return true;
        } else if (ActivityManagerDebugConfig.DEBUG_COMPACTION) {
            Slog.d("ActivityManager", " compactApp Skipped for " + app.processName + " pendingCompact= " + app.mOptRecord.hasPendingCompact() + " meetsCompactionRequirements=" + meetsCompactionRequirements(app) + ". Requested compact: " + app.mOptRecord.getReqCompactAction());
            return false;
        } else {
            return false;
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void compactAppFullForced(ProcessRecord app) {
        if (ITranCachedAppOptimizer.Instance().isMemFusionEnabled() && !app.mOptRecord.hasPendingCompact()) {
            app.mOptRecord.setHasPendingCompact(true);
            app.mOptRecord.setReqCompactAction(2);
            this.mPendingCompactionProcesses.add(app);
            Handler handler = this.mCompactionHandler;
            handler.sendMessage(handler.obtainMessage(11, app.mState.getSetAdj(), app.mState.getSetProcState()));
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean shouldCompactPersistent(ProcessRecord app, long now) {
        return app.mOptRecord.getLastCompactTime() == 0 || now - app.mOptRecord.getLastCompactTime() > this.mCompactThrottlePersistent;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void compactAppBfgs(ProcessRecord app) {
        this.mBfgsCompactRequest++;
        app.mOptRecord.setReqCompactAction(4);
        compactApp(app, false, " Bfgs");
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean shouldCompactBFGS(ProcessRecord app, long now) {
        return app.mOptRecord.getLastCompactTime() == 0 || now - app.mOptRecord.getLastCompactTime() > this.mCompactThrottleBFGS;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void compactAllSystem() {
        if (useCompaction()) {
            if (ActivityManagerDebugConfig.DEBUG_COMPACTION) {
                Slog.d("ActivityManager", "compactAllSystem");
            }
            Trace.instantForTrack(64L, ATRACE_COMPACTION_TRACK, "compactAllSystem");
            Handler handler = this.mCompactionHandler;
            handler.sendMessage(handler.obtainMessage(2));
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void updateUseCompaction() {
        this.mUseCompaction = DeviceConfig.getBoolean("activity_manager", KEY_USE_COMPACTION, DEFAULT_USE_COMPACTION.booleanValue());
        boolean MEMORY_FUSION2_SUPPORT = SystemProperties.getBoolean("persist.vendor.swapfile_enable", false);
        boolean useMemFusionCompaction = false;
        if (MEMORY_FUSION2_SUPPORT && MEMORY_FUSION2_SUPPORT != this.mUseCompaction) {
            useMemFusionCompaction = true;
            Slog.d("ActivityManager", "force enable useCompaction since memfusion enabled" + MEMORY_FUSION2_SUPPORT);
        }
        if (this.mUseCompaction || useMemFusionCompaction) {
            if (!this.mCachedAppOptimizerThread.isAlive()) {
                this.mCachedAppOptimizerThread.start();
            }
            this.mCompactionHandler = new MemCompactionHandler();
            Process.setThreadGroupAndCpuset(this.mCachedAppOptimizerThread.getThreadId(), 2);
        }
    }

    public synchronized boolean enableFreezer(final boolean enable) {
        if (!this.mUseFreezer) {
            if (!mFreezerCloudSwitch) {
                Slog.d(TAG_FREEZER, "EnableFreezer and CloudFreezer are false, run updateUseCloudFreezer");
                updateUseCloudFreezer();
            }
            return false;
        }
        if (enable) {
            int i = this.mFreezerDisableCount - 1;
            this.mFreezerDisableCount = i;
            if (i > 0) {
                return true;
            }
            if (i < 0) {
                Slog.e("ActivityManager", "unbalanced call to enableFreezer, ignoring");
                this.mFreezerDisableCount = 0;
                return false;
            }
        } else {
            int i2 = this.mFreezerDisableCount + 1;
            this.mFreezerDisableCount = i2;
            if (i2 > 1) {
                return true;
            }
        }
        try {
            synchronized (this.mAm) {
                try {
                    ActivityManagerService.boostPriorityForLockedSection();
                    synchronized (this.mProcLock) {
                        ActivityManagerService.boostPriorityForProcLockedSection();
                        this.mFreezerOverride = enable ? false : true;
                        Slog.d("ActivityManager", "freezer override set to " + this.mFreezerOverride);
                        this.mAm.mProcessList.forEachLruProcessesLOSP(true, new Consumer() { // from class: com.android.server.am.CachedAppOptimizer$$ExternalSyntheticLambda1
                            @Override // java.util.function.Consumer
                            public final void accept(Object obj) {
                                CachedAppOptimizer.this.m1408lambda$enableFreezer$0$comandroidserveramCachedAppOptimizer(enable, (ProcessRecord) obj);
                            }
                        });
                    }
                    ActivityManagerService.resetPriorityAfterProcLockedSection();
                    ActivityManagerService.resetPriorityAfterLockedSection();
                    return true;
                } catch (Throwable th) {
                    th = th;
                    ActivityManagerService.resetPriorityAfterLockedSection();
                    throw th;
                }
            }
        } catch (Throwable th2) {
            th = th2;
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$enableFreezer$0$com-android-server-am-CachedAppOptimizer  reason: not valid java name */
    public /* synthetic */ void m1408lambda$enableFreezer$0$comandroidserveramCachedAppOptimizer(boolean enable, ProcessRecord process) {
        if (process == null) {
            return;
        }
        ProcessCachedOptimizerRecord opt = process.mOptRecord;
        if (enable && opt.hasFreezerOverride()) {
            freezeAppAsyncLSP(process);
            opt.setFreezerOverride(false);
        }
        if (!enable && opt.isFrozen()) {
            unfreezeAppLSP(process);
            opt.setFreezerOverride(true);
        }
    }

    /* JADX WARN: Removed duplicated region for block: B:28:0x0066 A[EXC_TOP_SPLITTER, SYNTHETIC] */
    /*
        Code decompiled incorrectly, please refer to instructions dump.
    */
    public static boolean isFreezerSupported() {
        char state;
        boolean supported = false;
        if (!mFreezerCloudSwitch) {
            Slog.d(TAG_FREEZER, "CloudFreezer is false, disabled the freezer by this system");
            return false;
        }
        FileReader fr = null;
        try {
            fr = new FileReader(getFreezerCheckPath());
            state = (char) fr.read();
        } catch (FileNotFoundException e) {
            Slog.w("ActivityManager", "cgroup.freeze not present");
        } catch (RuntimeException e2) {
            Slog.w("ActivityManager", "unable to read freezer info");
        } catch (Exception e3) {
            Slog.w("ActivityManager", "unable to read cgroup.freeze: " + e3.toString());
        }
        if (state != '1' && state != '0') {
            Slog.e("ActivityManager", "unexpected value in cgroup.freeze");
            if (fr != null) {
                try {
                    fr.close();
                } catch (IOException e4) {
                    Slog.e("ActivityManager", "Exception closing cgroup.freeze: " + e4.toString());
                }
            }
            return supported;
        }
        getBinderFreezeInfo(Process.myPid());
        supported = true;
        if (fr != null) {
        }
        return supported;
    }

    public void updateUseCloudFreezerHook(final boolean cloudFreezerSwitch) {
        this.mAm.mHandler.post(new Runnable() { // from class: com.android.server.am.CachedAppOptimizer$$ExternalSyntheticLambda3
            @Override // java.lang.Runnable
            public final void run() {
                CachedAppOptimizer.this.m1410xd0643d84(cloudFreezerSwitch);
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$updateUseCloudFreezerHook$1$com-android-server-am-CachedAppOptimizer  reason: not valid java name */
    public /* synthetic */ void m1410xd0643d84(boolean cloudFreezerSwitch) {
        synchronized (this.mPhenotypeFlagLock) {
            mFreezerCloudSwitch = cloudFreezerSwitch;
            updateUseFreezer();
            mSkipRepeat = true;
            Slog.d(TAG_FREEZER, "updateUseCloudFreezerHook and mSkipRepeat");
            if (cloudFreezerSwitch) {
                Settings.Global.putString(this.mAm.mContext.getContentResolver(), "cached_apps_freezer", ServiceConfigAccessor.PROVIDER_MODE_ENABLED);
                Slog.d(TAG_FREEZER, "cached_apps_freezer: enabled");
            } else {
                Settings.Global.putString(this.mAm.mContext.getContentResolver(), "cached_apps_freezer", ServiceConfigAccessor.PROVIDER_MODE_DISABLED);
                Slog.d(TAG_FREEZER, "cached_apps_freezer: disabled");
            }
        }
    }

    private boolean updateUseCloudFreezer() {
        if (!this.mUseFreezer && !mFreezerCloudSwitch) {
            synchronized (this.mAm) {
                try {
                    ActivityManagerService.boostPriorityForLockedSection();
                    synchronized (this.mProcLock) {
                        ActivityManagerService.boostPriorityForProcLockedSection();
                        this.mAm.mProcessList.forEachLruProcessesLOSP(true, new Consumer() { // from class: com.android.server.am.CachedAppOptimizer$$ExternalSyntheticLambda2
                            @Override // java.util.function.Consumer
                            public final void accept(Object obj) {
                                CachedAppOptimizer.this.m1409x98e0fc46((ProcessRecord) obj);
                            }
                        });
                    }
                    ActivityManagerService.resetPriorityAfterProcLockedSection();
                } catch (Throwable th) {
                    ActivityManagerService.resetPriorityAfterLockedSection();
                    throw th;
                }
            }
            ActivityManagerService.resetPriorityAfterLockedSection();
            return true;
        }
        return false;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$updateUseCloudFreezer$2$com-android-server-am-CachedAppOptimizer  reason: not valid java name */
    public /* synthetic */ void m1409x98e0fc46(ProcessRecord process) {
        if (process == null) {
            return;
        }
        ProcessCachedOptimizerRecord opt = process.mOptRecord;
        if (!mFreezerCloudSwitch && opt.isFrozen()) {
            Slog.d(TAG_FREEZER, "unfreezeAppLSP process = " + process);
            unfreezeAppLSP(process);
        }
    }

    public void regFreezerCloudEngine(Runnable runnable, long time) {
        Slog.d(TAG_FREEZER, "on regFreezerCloudEngine");
        this.mAm.mHandler.postDelayed(runnable, time);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void updateUseFreezer() {
        boolean z = false;
        if (mSkipRepeat) {
            Slog.d(TAG_FREEZER, "skip repeat");
            mSkipRepeat = false;
            return;
        }
        String configOverride = Settings.Global.getString(this.mAm.mContext.getContentResolver(), "cached_apps_freezer");
        if (ServiceConfigAccessor.PROVIDER_MODE_DISABLED.equals(configOverride)) {
            this.mUseFreezer = false;
        } else if (ServiceConfigAccessor.PROVIDER_MODE_ENABLED.equals(configOverride) || DeviceConfig.getBoolean("activity_manager_native_boot", KEY_USE_FREEZER, DEFAULT_USE_FREEZER.booleanValue())) {
            this.mUseFreezer = isFreezerSupported();
            updateFreezerDebounceTimeout();
        } else {
            this.mUseFreezer = false;
        }
        Slog.d(TAG_FREEZER, "mUseFreezer= " + this.mUseFreezer + " mFreezerCloudSwitch= " + mFreezerCloudSwitch + " mSkipRepeat= " + mSkipRepeat);
        if (this.mUseFreezer && mFreezerCloudSwitch) {
            z = true;
        }
        this.mUseFreezer = z;
        final boolean useFreezer = this.mUseFreezer;
        this.mAm.mHandler.post(new Runnable() { // from class: com.android.server.am.CachedAppOptimizer$$ExternalSyntheticLambda0
            @Override // java.lang.Runnable
            public final void run() {
                CachedAppOptimizer.this.m1411x26229832(useFreezer);
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$updateUseFreezer$3$com-android-server-am-CachedAppOptimizer  reason: not valid java name */
    public /* synthetic */ void m1411x26229832(boolean useFreezer) {
        if (useFreezer) {
            Slog.d("ActivityManager", "Freezer enabled");
            enableFreezer(true);
            if (!this.mCachedAppOptimizerThread.isAlive()) {
                this.mCachedAppOptimizerThread.start();
            }
            if (this.mFreezeHandler == null) {
                this.mFreezeHandler = new FreezeHandler();
            }
            Process.setThreadGroupAndCpuset(this.mCachedAppOptimizerThread.getThreadId(), 2);
            return;
        }
        Slog.d("ActivityManager", "Freezer disabled");
        enableFreezer(false);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void updateCompactionActions() {
        int compactAction1 = DeviceConfig.getInt("activity_manager", KEY_COMPACT_ACTION_1, 1);
        int compactAction2 = DeviceConfig.getInt("activity_manager", KEY_COMPACT_ACTION_2, 3);
        int compactAction11 = DeviceConfig.getInt("activity_manager", KEY_COMPACT_ACTION_11, 11);
        this.mCompactActionSome = compactActionIntToString(compactAction1);
        this.mCompactActionFull = compactActionIntToString(compactAction2);
        if (ITranCachedAppOptimizer.Instance().isMemFusionEnabled()) {
            this.mCompactActionMemFusionFull = compactActionIntToString(compactAction11);
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void updateCompactionThrottles() {
        boolean useThrottleDefaults = false;
        String throttleSomeSomeFlag = DeviceConfig.getProperty("activity_manager", KEY_COMPACT_THROTTLE_1);
        String throttleSomeFullFlag = DeviceConfig.getProperty("activity_manager", KEY_COMPACT_THROTTLE_2);
        String throttleFullSomeFlag = DeviceConfig.getProperty("activity_manager", KEY_COMPACT_THROTTLE_3);
        String throttleFullFullFlag = DeviceConfig.getProperty("activity_manager", KEY_COMPACT_THROTTLE_4);
        String throttleBFGSFlag = DeviceConfig.getProperty("activity_manager", KEY_COMPACT_THROTTLE_5);
        String throttlePersistentFlag = DeviceConfig.getProperty("activity_manager", KEY_COMPACT_THROTTLE_6);
        String throttleMinOomAdjFlag = DeviceConfig.getProperty("activity_manager", KEY_COMPACT_THROTTLE_MIN_OOM_ADJ);
        String throttleMaxOomAdjFlag = DeviceConfig.getProperty("activity_manager", KEY_COMPACT_THROTTLE_MAX_OOM_ADJ);
        if (TextUtils.isEmpty(throttleSomeSomeFlag) || TextUtils.isEmpty(throttleSomeFullFlag) || TextUtils.isEmpty(throttleFullSomeFlag) || TextUtils.isEmpty(throttleFullFullFlag) || TextUtils.isEmpty(throttleBFGSFlag) || TextUtils.isEmpty(throttlePersistentFlag) || TextUtils.isEmpty(throttleMinOomAdjFlag) || TextUtils.isEmpty(throttleMaxOomAdjFlag)) {
            useThrottleDefaults = true;
        } else {
            try {
                this.mCompactThrottleSomeSome = Integer.parseInt(throttleSomeSomeFlag);
                this.mCompactThrottleSomeFull = Integer.parseInt(throttleSomeFullFlag);
                this.mCompactThrottleFullSome = Integer.parseInt(throttleFullSomeFlag);
                this.mCompactThrottleFullFull = Integer.parseInt(throttleFullFullFlag);
                this.mCompactThrottleBFGS = Integer.parseInt(throttleBFGSFlag);
                this.mCompactThrottlePersistent = Integer.parseInt(throttlePersistentFlag);
                this.mCompactThrottleMinOomAdj = Long.parseLong(throttleMinOomAdjFlag);
                this.mCompactThrottleMaxOomAdj = Long.parseLong(throttleMaxOomAdjFlag);
            } catch (NumberFormatException e) {
                useThrottleDefaults = true;
            }
        }
        if (useThrottleDefaults) {
            this.mCompactThrottleSomeSome = DEFAULT_COMPACT_THROTTLE_1;
            this.mCompactThrottleSomeFull = JobStatus.DEFAULT_TRIGGER_UPDATE_DELAY;
            this.mCompactThrottleFullSome = 500L;
            this.mCompactThrottleFullFull = JobStatus.DEFAULT_TRIGGER_UPDATE_DELAY;
            this.mCompactThrottleBFGS = 600000L;
            this.mCompactThrottlePersistent = 600000L;
            this.mCompactThrottleMinOomAdj = DEFAULT_COMPACT_THROTTLE_MIN_OOM_ADJ;
            this.mCompactThrottleMaxOomAdj = DEFAULT_COMPACT_THROTTLE_MAX_OOM_ADJ;
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void updateCompactStatsdSampleRate() {
        this.mCompactStatsdSampleRate = DeviceConfig.getFloat("activity_manager", KEY_COMPACT_STATSD_SAMPLE_RATE, (float) DEFAULT_STATSD_SAMPLE_RATE);
        this.mCompactStatsdSampleRate = Math.min(1.0f, Math.max(0.0f, this.mCompactStatsdSampleRate));
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void updateFreezerStatsdSampleRate() {
        this.mFreezerStatsdSampleRate = DeviceConfig.getFloat("activity_manager", KEY_FREEZER_STATSD_SAMPLE_RATE, (float) DEFAULT_STATSD_SAMPLE_RATE);
        this.mFreezerStatsdSampleRate = Math.min(1.0f, Math.max(0.0f, this.mFreezerStatsdSampleRate));
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void updateFullRssThrottle() {
        this.mFullAnonRssThrottleKb = DeviceConfig.getLong("activity_manager", KEY_COMPACT_FULL_RSS_THROTTLE_KB, (long) DEFAULT_COMPACT_FULL_RSS_THROTTLE_KB);
        if (this.mFullAnonRssThrottleKb < 0) {
            this.mFullAnonRssThrottleKb = DEFAULT_COMPACT_FULL_RSS_THROTTLE_KB;
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void updateFullDeltaRssThrottle() {
        this.mFullDeltaRssThrottleKb = DeviceConfig.getLong("activity_manager", KEY_COMPACT_FULL_DELTA_RSS_THROTTLE_KB, (long) DEFAULT_COMPACT_FULL_DELTA_RSS_THROTTLE_KB);
        if (this.mFullDeltaRssThrottleKb < 0) {
            this.mFullDeltaRssThrottleKb = DEFAULT_COMPACT_FULL_DELTA_RSS_THROTTLE_KB;
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void updateProcStateThrottle() {
        String str = DEFAULT_COMPACT_PROC_STATE_THROTTLE;
        String procStateThrottleString = DeviceConfig.getString("activity_manager", KEY_COMPACT_PROC_STATE_THROTTLE, str);
        if (!parseProcStateThrottle(procStateThrottleString)) {
            Slog.w("ActivityManager", "Unable to parse app compact proc state throttle \"" + procStateThrottleString + "\" falling back to default.");
            if (!parseProcStateThrottle(str)) {
                Slog.wtf("ActivityManager", "Unable to parse default app compact proc state throttle " + str);
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void updateMinOomAdjThrottle() {
        this.mCompactThrottleMinOomAdj = DeviceConfig.getLong("activity_manager", KEY_COMPACT_THROTTLE_MIN_OOM_ADJ, (long) DEFAULT_COMPACT_THROTTLE_MIN_OOM_ADJ);
        if (this.mCompactThrottleMinOomAdj < DEFAULT_COMPACT_THROTTLE_MIN_OOM_ADJ) {
            this.mCompactThrottleMinOomAdj = DEFAULT_COMPACT_THROTTLE_MIN_OOM_ADJ;
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void updateMaxOomAdjThrottle() {
        this.mCompactThrottleMaxOomAdj = DeviceConfig.getLong("activity_manager", KEY_COMPACT_THROTTLE_MAX_OOM_ADJ, (long) DEFAULT_COMPACT_THROTTLE_MAX_OOM_ADJ);
        if (this.mCompactThrottleMaxOomAdj > DEFAULT_COMPACT_THROTTLE_MAX_OOM_ADJ) {
            this.mCompactThrottleMaxOomAdj = DEFAULT_COMPACT_THROTTLE_MAX_OOM_ADJ;
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void updateFreezerDebounceTimeout() {
        this.mFreezerDebounceTimeout = DeviceConfig.getLong("activity_manager_native_boot", KEY_FREEZER_DEBOUNCE_TIMEOUT, 600000L);
        if (this.mFreezerDebounceTimeout < 0) {
            this.mFreezerDebounceTimeout = 600000L;
        }
    }

    private boolean parseProcStateThrottle(String procStateThrottleString) {
        String[] procStates = TextUtils.split(procStateThrottleString, ",");
        this.mProcStateThrottle.clear();
        for (String procState : procStates) {
            try {
                this.mProcStateThrottle.add(Integer.valueOf(Integer.parseInt(procState)));
            } catch (NumberFormatException e) {
                Slog.e("ActivityManager", "Failed to parse default app compaction proc state: " + procState);
                return false;
            }
        }
        return true;
    }

    static String compactActionIntToString(int action) {
        if (action == 11) {
            return "memfusion";
        }
        if (action >= 0) {
            String[] strArr = COMPACT_ACTION_STRING;
            if (action >= strArr.length) {
                return "";
            }
            return strArr[action];
        }
        return "";
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void unfreezeTemporarily(ProcessRecord app) {
        if (this.mUseFreezer) {
            synchronized (this.mProcLock) {
                try {
                    ActivityManagerService.boostPriorityForProcLockedSection();
                    if (app.mOptRecord.isFrozen() || app.mOptRecord.isPendingFreeze()) {
                        unfreezeAppLSP(app);
                        freezeAppAsyncLSP(app);
                    }
                } catch (Throwable th) {
                    ActivityManagerService.resetPriorityAfterProcLockedSection();
                    throw th;
                }
            }
            ActivityManagerService.resetPriorityAfterProcLockedSection();
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void freezeAppAsyncLSP(ProcessRecord app) {
        ProcessCachedOptimizerRecord opt = app.mOptRecord;
        if (opt.isPendingFreeze()) {
            return;
        }
        Handler handler = this.mFreezeHandler;
        handler.sendMessageDelayed(handler.obtainMessage(3, 1, 0, app), this.mFreezerDebounceTimeout);
        opt.setPendingFreeze(true);
        if (ActivityManagerDebugConfig.DEBUG_FREEZER) {
            Slog.d("ActivityManager", "Async freezing " + app.getPid() + " " + app.processName);
        }
    }

    void unfreezeAppInternalLSP(ProcessRecord app) {
        int pid = app.getPid();
        ProcessCachedOptimizerRecord opt = app.mOptRecord;
        if (opt.isPendingFreeze()) {
            this.mFreezeHandler.removeMessages(3, app);
            opt.setPendingFreeze(false);
            if (ActivityManagerDebugConfig.DEBUG_FREEZER) {
                Slog.d("ActivityManager", "Cancel freezing " + pid + " " + app.processName);
            }
        }
        opt.setFreezerOverride(false);
        if (pid == 0 || !opt.isFrozen()) {
            return;
        }
        boolean processKilled = false;
        try {
            int freezeInfo = getBinderFreezeInfo(pid);
            if ((freezeInfo & 1) != 0) {
                Slog.d("ActivityManager", "pid " + pid + " " + app.processName + " received sync transactions while frozen, killing");
                app.killLocked("Sync transaction while in frozen state", 14, 20, true);
                processKilled = true;
            }
            if ((freezeInfo & 2) != 0 && ActivityManagerDebugConfig.DEBUG_FREEZER) {
                Slog.d("ActivityManager", "pid " + pid + " " + app.processName + " received async transactions while frozen");
            }
        } catch (Exception e) {
            Slog.d("ActivityManager", "Unable to query binder frozen info for pid " + pid + " " + app.processName + ". Killing it. Exception: " + e);
            app.killLocked("Unable to query binder frozen stats", 14, 19, true);
            processKilled = true;
        }
        if (processKilled) {
            return;
        }
        long freezeTime = opt.getFreezeUnfreezeTime();
        try {
            freezeBinder(pid, false);
            try {
                Process.setProcessFrozen(pid, app.uid, false);
                opt.setFreezeUnfreezeTime(SystemClock.uptimeMillis());
                opt.setFrozen(false);
                this.mFrozenProcesses.delete(pid);
            } catch (Exception e2) {
                Slog.e("ActivityManager", "Unable to unfreeze " + pid + " " + app.processName + ". This might cause inconsistency or UI hangs.");
            }
            if (!opt.isFrozen()) {
                Slog.d("ActivityManager", "sync unfroze " + pid + " " + app.processName);
                Handler handler = this.mFreezeHandler;
                handler.sendMessage(handler.obtainMessage(4, pid, (int) Math.min(opt.getFreezeUnfreezeTime() - freezeTime, 2147483647L), app.processName));
            }
        } catch (RuntimeException e3) {
            Slog.e("ActivityManager", "Unable to unfreeze binder for " + pid + " " + app.processName + ". Killing it");
            app.killLocked("Unable to unfreeze", 14, 19, true);
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void unfreezeAppLSP(ProcessRecord app) {
        synchronized (this.mFreezerLock) {
            unfreezeAppInternalLSP(app);
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void unfreezeProcess(int pid) {
        synchronized (this.mFreezerLock) {
            ProcessRecord app = this.mFrozenProcesses.get(pid);
            if (app == null) {
                return;
            }
            Slog.d("ActivityManager", "quick sync unfreeze " + pid);
            try {
                freezeBinder(pid, false);
                try {
                    Process.setProcessFrozen(pid, app.uid, false);
                } catch (Exception e) {
                    Slog.e("ActivityManager", "Unable to quick unfreeze " + pid);
                }
            } catch (RuntimeException e2) {
                Slog.e("ActivityManager", "Unable to quick unfreeze binder for " + pid);
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void onCleanupApplicationRecordLocked(ProcessRecord app) {
        if (this.mUseFreezer) {
            ProcessCachedOptimizerRecord opt = app.mOptRecord;
            if (opt.isPendingFreeze()) {
                this.mFreezeHandler.removeMessages(3, app);
                opt.setPendingFreeze(false);
            }
            this.mFrozenProcesses.delete(app.getPid());
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void onWakefulnessChanged(int wakefulness) {
        if (wakefulness == 1) {
            Slog.e("ActivityManager", "Cancel pending or running compactions as system is awake");
            cancelAllCompactions();
        }
    }

    void cancelAllCompactions() {
        synchronized (this.mProcLock) {
            try {
                ActivityManagerService.boostPriorityForProcLockedSection();
                int size = this.mPendingCompactionProcesses.size();
                for (int i = 0; i < size; i++) {
                    ProcessRecord record = this.mPendingCompactionProcesses.get(i);
                    record.mOptRecord.setHasPendingCompact(false);
                }
                this.mPendingCompactionProcesses.clear();
            } catch (Throwable th) {
                ActivityManagerService.resetPriorityAfterProcLockedSection();
                throw th;
            }
        }
        ActivityManagerService.resetPriorityAfterProcLockedSection();
        cancelCompaction();
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void onOomAdjustChanged(int oldAdj, int newAdj, ProcessRecord app) {
        if (DefaultProcessDependencies.mPidCompacting == app.mPid && newAdj < oldAdj && newAdj < 900) {
            cancelCompaction();
        }
        if (oldAdj <= 200 && (newAdj == 700 || newAdj == 600)) {
            compactAppSome(app, false);
        } else if (oldAdj < 900 && newAdj >= 900 && newAdj <= 999) {
            compactAppFull(app, false);
        }
    }

    int resolveCompactionAction(int pendingAction) {
        int resolvedAction;
        switch (pendingAction) {
            case 1:
                resolvedAction = 1;
                break;
            case 2:
                if (ITranCachedAppOptimizer.Instance().isMemFusionEnabled() && this.mSkipCompactFullCheck) {
                    resolvedAction = 11;
                    break;
                }
                break;
            case 3:
            case 4:
                resolvedAction = 3;
                break;
            default:
                resolvedAction = 0;
                break;
        }
        if (resolvedAction == 3) {
            double swapFreePercent = getFreeSwapPercent();
            if (swapFreePercent < COMPACT_DOWNGRADE_FREE_SWAP_THRESHOLD) {
                resolvedAction = 1;
                if (ActivityManagerDebugConfig.DEBUG_COMPACTION) {
                    Slog.d("ActivityManager", "Downgraded compaction to file only due to low swap. Swap Free% " + swapFreePercent);
                }
            }
        }
        return resolvedAction;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* loaded from: classes.dex */
    public static final class LastCompactionStats {
        private final long[] mRssAfterCompaction;

        LastCompactionStats(long[] rss) {
            this.mRssAfterCompaction = rss;
        }

        long[] getRssAfterCompaction() {
            return this.mRssAfterCompaction;
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes.dex */
    public final class MemCompactionHandler extends Handler {
        private MemCompactionHandler() {
            super(CachedAppOptimizer.this.mCachedAppOptimizerThread.getLooper());
        }

        private boolean shouldOomAdjThrottleCompaction(ProcessRecord proc, int action) {
            String name = proc.processName;
            if ((action == 1 || action == 3) && proc.mState.getSetAdj() <= 200) {
                if (ActivityManagerDebugConfig.DEBUG_COMPACTION) {
                    Slog.d("ActivityManager", "Skipping compaction as process " + name + " is now perceptible.");
                }
                return true;
            }
            return false;
        }

        /* JADX WARN: Code restructure failed: missing block: B:33:0x00db, code lost:
            if ((r20 - r6) >= r18.this$0.mCompactThrottleFullSome) goto L45;
         */
        /* JADX WARN: Code restructure failed: missing block: B:39:0x00eb, code lost:
            if ((r20 - r6) < r18.this$0.mCompactThrottleFullFull) goto L40;
         */
        /* JADX WARN: Code restructure failed: missing block: B:41:0x00ef, code lost:
            if (com.android.server.am.ActivityManagerDebugConfig.DEBUG_COMPACTION == false) goto L44;
         */
        /* JADX WARN: Code restructure failed: missing block: B:42:0x00f1, code lost:
            android.util.Slog.d(r16, "Skipping full compaction for " + r4 + ": too soon. throttle=" + r18.this$0.mCompactThrottleFullSome + com.android.server.slice.SliceClientPermissions.SliceAuthority.DELIMITER + r18.this$0.mCompactThrottleFullFull + " last=" + (r20 - r6) + "ms ago");
         */
        /* JADX WARN: Code restructure failed: missing block: B:43:0x012f, code lost:
            return true;
         */
        /* JADX WARN: Code restructure failed: missing block: B:73:?, code lost:
            return true;
         */
        /* JADX WARN: Code restructure failed: missing block: B:8:0x0032, code lost:
            if ((r20 - r6) >= r18.this$0.mCompactThrottleSomeSome) goto L14;
         */
        /*
            Code decompiled incorrectly, please refer to instructions dump.
        */
        private boolean shouldTimeThrottleCompaction(ProcessRecord proc, long start, int pendingAction) {
            String str;
            String str2;
            ProcessCachedOptimizerRecord opt = proc.mOptRecord;
            String name = proc.processName;
            int lastCompactAction = opt.getLastCompactAction();
            long lastCompactTime = opt.getLastCompactTime();
            if (lastCompactTime != 0) {
                if (pendingAction == 1) {
                    if (lastCompactAction == 1) {
                        str2 = " last=";
                    } else {
                        str2 = " last=";
                    }
                    if (lastCompactAction != 2 || start - lastCompactTime >= CachedAppOptimizer.this.mCompactThrottleSomeFull) {
                    }
                    if (ActivityManagerDebugConfig.DEBUG_COMPACTION) {
                        Slog.d("ActivityManager", "Skipping some compaction for " + name + ": too soon. throttle=" + CachedAppOptimizer.this.mCompactThrottleSomeSome + SliceClientPermissions.SliceAuthority.DELIMITER + CachedAppOptimizer.this.mCompactThrottleSomeFull + str2 + (start - lastCompactTime) + "ms ago");
                        return true;
                    }
                    return true;
                } else if (pendingAction == 2) {
                    if (ITranCachedAppOptimizer.Instance().isMemFusionEnabled() && CachedAppOptimizer.this.mSkipCompactFullCheck) {
                        if (ActivityManagerDebugConfig.DEBUG_COMPACTION) {
                            Slog.d("ActivityManager", "Skipping full compaction check for " + name + ": if too soon. last=" + (start - lastCompactTime) + "ms ago");
                        }
                    } else {
                        if (lastCompactAction == 1) {
                            str = "ActivityManager";
                        } else {
                            str = "ActivityManager";
                        }
                        if (lastCompactAction == 2) {
                        }
                    }
                } else if (pendingAction == 3) {
                    if (start - lastCompactTime < CachedAppOptimizer.this.mCompactThrottlePersistent) {
                        if (ActivityManagerDebugConfig.DEBUG_COMPACTION) {
                            Slog.d("ActivityManager", "Skipping persistent compaction for " + name + ": too soon. throttle=" + CachedAppOptimizer.this.mCompactThrottlePersistent + " last=" + (start - lastCompactTime) + "ms ago");
                            return true;
                        }
                        return true;
                    }
                } else if (pendingAction == 4 && start - lastCompactTime < CachedAppOptimizer.this.mCompactThrottleBFGS) {
                    if (ActivityManagerDebugConfig.DEBUG_COMPACTION) {
                        Slog.d("ActivityManager", "Skipping bfgs compaction for " + name + ": too soon. throttle=" + CachedAppOptimizer.this.mCompactThrottleBFGS + " last=" + (start - lastCompactTime) + "ms ago");
                        return true;
                    }
                    return true;
                }
            }
            if (ITranCachedAppOptimizer.Instance().isMemFusionEnabled() && !CachedAppOptimizer.this.mSkipCompactFullCheck) {
                return true;
            }
            return false;
        }

        private boolean shouldThrottleMiscCompaction(ProcessRecord proc, int procState, int action) {
            String name = proc.processName;
            if (CachedAppOptimizer.this.mProcStateThrottle.contains(Integer.valueOf(procState))) {
                if (ActivityManagerDebugConfig.DEBUG_COMPACTION) {
                    Slog.d("ActivityManager", "Skipping full compaction for process " + name + "; proc state is " + procState);
                }
                return true;
            } else if (action == 0) {
                if (ActivityManagerDebugConfig.DEBUG_COMPACTION) {
                    Slog.d("ActivityManager", "Skipping compaction for process " + name + "since action is None");
                }
                return true;
            } else {
                return false;
            }
        }

        private boolean shouldRssThrottleCompaction(int action, int pid, String name, long[] rssBefore) {
            long anonRssBefore = rssBefore[2];
            LastCompactionStats lastCompactionStats = CachedAppOptimizer.this.mLastCompactionStats.get(Integer.valueOf(pid));
            if (rssBefore[0] == 0 && rssBefore[1] == 0 && rssBefore[2] == 0 && rssBefore[3] == 0) {
                if (ActivityManagerDebugConfig.DEBUG_COMPACTION) {
                    Slog.d("ActivityManager", "Skipping compaction forprocess " + pid + " with no memory usage. Dead?");
                }
                return true;
            } else if (action == 3 || action == 2) {
                if (CachedAppOptimizer.this.mFullAnonRssThrottleKb > 0 && anonRssBefore < CachedAppOptimizer.this.mFullAnonRssThrottleKb) {
                    if (ActivityManagerDebugConfig.DEBUG_COMPACTION) {
                        Slog.d("ActivityManager", "Skipping full compaction for process " + name + "; anon RSS is too small: " + anonRssBefore + "KB.");
                    }
                    return true;
                } else if (lastCompactionStats != null && CachedAppOptimizer.this.mFullDeltaRssThrottleKb > 0) {
                    long[] lastRss = lastCompactionStats.getRssAfterCompaction();
                    long absDelta = Math.abs(rssBefore[1] - lastRss[1]) + Math.abs(rssBefore[2] - lastRss[2]) + Math.abs(rssBefore[3] - lastRss[3]);
                    if (absDelta <= CachedAppOptimizer.this.mFullDeltaRssThrottleKb) {
                        if (ActivityManagerDebugConfig.DEBUG_COMPACTION) {
                            Slog.d("ActivityManager", "Skipping full compaction for process " + name + "; abs delta is too small: " + absDelta + "KB.");
                            return true;
                        }
                        return true;
                    }
                    return false;
                } else {
                    return false;
                }
            } else {
                return false;
            }
        }

        /* JADX DEBUG: Don't trust debug lines info. Repeating lines: [1812=9, 1821=12] */
        /* JADX WARN: Can't fix incorrect switch cases order, some code will duplicate */
        @Override // android.os.Handler
        public void handleMessage(Message msg) {
            String name;
            int requestedAction;
            long[] rssBefore;
            String name2;
            int i;
            long[] rssAfter;
            ProcessCachedOptimizerRecord opt;
            long j;
            long j2;
            long j3;
            long j4;
            long j5;
            long j6;
            long j7;
            long j8;
            CachedAppOptimizer.this.mSkipCompactFullCheck = false;
            switch (msg.what) {
                case 1:
                    break;
                case 2:
                    if (ITranCachedAppOptimizer.Instance().isMemFusionEnabled() && !CachedAppOptimizer.this.mSkipCompactFullCheck) {
                        Slog.d("ActivityManager", "Skipping compaction sys since not force type");
                        return;
                    }
                    CachedAppOptimizer.this.mSystemCompactionsPerformed++;
                    Trace.traceBegin(64L, "compactSystem");
                    CachedAppOptimizer.this.compactSystem();
                    Trace.traceEnd(64L);
                    return;
                case 11:
                    CachedAppOptimizer.this.mSkipCompactFullCheck = true;
                    break;
                default:
                    return;
            }
            long start = SystemClock.uptimeMillis();
            int lastOomAdj = msg.arg1;
            int procState = msg.arg2;
            synchronized (CachedAppOptimizer.this.mProcLock) {
                try {
                    ActivityManagerService.boostPriorityForProcLockedSection();
                    if (CachedAppOptimizer.this.mPendingCompactionProcesses.isEmpty()) {
                        try {
                            if (ActivityManagerDebugConfig.DEBUG_COMPACTION) {
                                Slog.d("ActivityManager", "No processes pending compaction, bail out");
                            }
                            ActivityManagerService.resetPriorityAfterProcLockedSection();
                        } catch (Throwable th) {
                            th = th;
                            while (true) {
                                try {
                                    ActivityManagerService.resetPriorityAfterProcLockedSection();
                                    throw th;
                                } catch (Throwable th2) {
                                    th = th2;
                                }
                            }
                        }
                    } else {
                        ProcessRecord proc = (ProcessRecord) CachedAppOptimizer.this.mPendingCompactionProcesses.remove(0);
                        ProcessCachedOptimizerRecord opt2 = proc.mOptRecord;
                        boolean forceCompaction = opt2.isForceCompact();
                        opt2.setForceCompact(false);
                        int requestedAction2 = opt2.getReqCompactAction();
                        int pid = proc.getPid();
                        String name3 = proc.processName;
                        opt2.setHasPendingCompact(false);
                        int lastCompactAction = opt2.getLastCompactAction();
                        long lastCompactTime = opt2.getLastCompactTime();
                        ActivityManagerService.resetPriorityAfterProcLockedSection();
                        CachedAppOptimizer.this.mProcCompactionsRequested++;
                        if (pid == 0) {
                            if (ActivityManagerDebugConfig.DEBUG_COMPACTION) {
                                Slog.d("ActivityManager", "Compaction failed, pid is 0");
                            }
                            CachedAppOptimizer.this.mProcCompactionsNoPidThrottled++;
                            return;
                        }
                        if (CachedAppOptimizer.this.mSkipCompactFullCheck) {
                            name = name3;
                            Slog.d("ActivityManager", "Entering compaction as process " + name + " forced");
                        } else {
                            name = name3;
                        }
                        if (ITranCachedAppOptimizer.Instance().isMemFusionEnabled() && !CachedAppOptimizer.this.mSkipCompactFullCheck) {
                            return;
                        }
                        if (forceCompaction) {
                            requestedAction = requestedAction2;
                            long[] rssBefore2 = CachedAppOptimizer.this.mProcessDependencies.getRss(pid);
                            if (ActivityManagerDebugConfig.DEBUG_COMPACTION) {
                                Slog.d("ActivityManager", "Forcing compaction for " + name);
                            }
                            rssBefore = rssBefore2;
                        } else {
                            requestedAction = requestedAction2;
                            if (shouldOomAdjThrottleCompaction(proc, requestedAction)) {
                                CachedAppOptimizer.this.mProcCompactionsOomAdjThrottled++;
                                return;
                            } else if (shouldTimeThrottleCompaction(proc, start, requestedAction)) {
                                CachedAppOptimizer.this.mProcCompactionsTimeThrottled++;
                                return;
                            } else if (shouldThrottleMiscCompaction(proc, procState, requestedAction)) {
                                CachedAppOptimizer.this.mProcCompactionsMiscThrottled++;
                                return;
                            } else {
                                long[] rssBefore3 = CachedAppOptimizer.this.mProcessDependencies.getRss(pid);
                                if (shouldRssThrottleCompaction(requestedAction, pid, name, rssBefore3)) {
                                    CachedAppOptimizer.this.mProcCompactionsRSSThrottled++;
                                    return;
                                }
                                rssBefore = rssBefore3;
                            }
                        }
                        switch (requestedAction) {
                            case 1:
                                CachedAppOptimizer.this.mSomeCompactionCount++;
                                break;
                            case 2:
                                CachedAppOptimizer.this.mFullCompactionCount++;
                                break;
                            case 3:
                                CachedAppOptimizer.this.mPersistentCompactionCount++;
                                break;
                            case 4:
                                CachedAppOptimizer.this.mBfgsCompactionCount++;
                                break;
                        }
                        int resolvedAction = CachedAppOptimizer.this.resolveCompactionAction(requestedAction);
                        String action = CachedAppOptimizer.compactActionIntToString(resolvedAction);
                        try {
                            try {
                                Trace.traceBegin(64L, "Compact " + action + ": " + name);
                                CachedAppOptimizer.this.mProcCompactionsPerformed++;
                                long zramFreeKbBefore = Debug.getZramFreeKb();
                                CachedAppOptimizer.this.mProcessDependencies.performCompaction(action, pid);
                                long[] rssAfter2 = CachedAppOptimizer.this.mProcessDependencies.getRss(pid);
                                long end = SystemClock.uptimeMillis();
                                long end2 = end - start;
                                try {
                                    long zramFreeKbAfter = Debug.getZramFreeKb();
                                    long deltaTotalRss = rssAfter2[0] - rssBefore[0];
                                    try {
                                        long deltaFileRss = rssAfter2[1] - rssBefore[1];
                                        long deltaAnonRss = rssAfter2[2] - rssBefore[2];
                                        long deltaSwapRss = rssAfter2[3] - rssBefore[3];
                                        Object[] objArr = new Object[18];
                                        objArr[0] = Integer.valueOf(pid);
                                        objArr[1] = name;
                                        objArr[2] = action;
                                        objArr[3] = Long.valueOf(rssBefore[0]);
                                        objArr[4] = Long.valueOf(rssBefore[1]);
                                        objArr[5] = Long.valueOf(rssBefore[2]);
                                        objArr[6] = Long.valueOf(rssBefore[3]);
                                        objArr[7] = Long.valueOf(deltaTotalRss);
                                        objArr[8] = Long.valueOf(deltaFileRss);
                                        objArr[9] = Long.valueOf(deltaAnonRss);
                                        objArr[10] = Long.valueOf(deltaSwapRss);
                                        String name4 = name;
                                        try {
                                            objArr[11] = Long.valueOf(end2);
                                            objArr[12] = Integer.valueOf(lastCompactAction);
                                            objArr[13] = Long.valueOf(lastCompactTime);
                                            objArr[14] = Integer.valueOf(lastOomAdj);
                                            objArr[15] = Integer.valueOf(procState);
                                            objArr[16] = Long.valueOf(zramFreeKbBefore);
                                            objArr[17] = Long.valueOf(zramFreeKbAfter - zramFreeKbBefore);
                                            EventLog.writeEvent((int) EventLogTags.AM_COMPACT, objArr);
                                            if (CachedAppOptimizer.this.mRandom.nextFloat() < CachedAppOptimizer.this.mCompactStatsdSampleRate) {
                                                try {
                                                    j = rssBefore[0];
                                                    j2 = rssBefore[1];
                                                    j3 = rssBefore[2];
                                                    j4 = rssBefore[3];
                                                    j5 = rssAfter2[0];
                                                    j6 = rssAfter2[1];
                                                    j7 = rssAfter2[2];
                                                    j8 = rssAfter2[3];
                                                    name2 = name4;
                                                    i = 3;
                                                    opt = opt2;
                                                    rssAfter = rssAfter2;
                                                } catch (Exception e) {
                                                    e = e;
                                                    name2 = name4;
                                                } catch (Throwable th3) {
                                                    th = th3;
                                                }
                                                try {
                                                    FrameworkStatsLog.write(115, pid, name2, requestedAction, j, j2, j3, j4, j5, j6, j7, j8, end2, lastCompactAction, lastCompactTime, lastOomAdj, ActivityManager.processStateAmToProto(procState), zramFreeKbBefore, zramFreeKbAfter);
                                                } catch (Exception e2) {
                                                    e = e2;
                                                    try {
                                                        Slog.d("ActivityManager", "compaction sys error:" + e);
                                                        try {
                                                            Slog.d("ActivityManager", "Exception occurred while compacting pid: " + name2 + ". Exception:" + e.getMessage());
                                                            Trace.traceEnd(64L);
                                                        } catch (Throwable th4) {
                                                            th = th4;
                                                            Trace.traceEnd(64L);
                                                            throw th;
                                                        }
                                                    } catch (Throwable th5) {
                                                        th = th5;
                                                    }
                                                } catch (Throwable th6) {
                                                    th = th6;
                                                    Trace.traceEnd(64L);
                                                    throw th;
                                                }
                                            } else {
                                                i = 3;
                                                rssAfter = rssAfter2;
                                                opt = opt2;
                                                name2 = name4;
                                            }
                                            try {
                                                try {
                                                    synchronized (CachedAppOptimizer.this.mProcLock) {
                                                        try {
                                                            ActivityManagerService.boostPriorityForProcLockedSection();
                                                            ProcessCachedOptimizerRecord opt3 = opt;
                                                            try {
                                                                opt3.setLastCompactTime(end);
                                                                try {
                                                                    opt3.setLastCompactAction(resolvedAction);
                                                                    ActivityManagerService.resetPriorityAfterProcLockedSection();
                                                                    if (ITranCachedAppOptimizer.Instance().isMemFusionEnabled() && !CachedAppOptimizer.this.mSkipCompactFullCheck && ITranCachedAppOptimizer.Instance().updateCompactionState()) {
                                                                        Slog.d("ActivityManager", "Skipping compaction next time!");
                                                                    }
                                                                    if (resolvedAction == i || resolvedAction == 2 || resolvedAction == 11) {
                                                                        CachedAppOptimizer.this.mLastCompactionStats.remove(Integer.valueOf(pid));
                                                                        CachedAppOptimizer.this.mLastCompactionStats.put(Integer.valueOf(pid), new LastCompactionStats(rssAfter));
                                                                    }
                                                                    Trace.traceEnd(64L);
                                                                } catch (Throwable th7) {
                                                                    th = th7;
                                                                    while (true) {
                                                                        try {
                                                                            ActivityManagerService.resetPriorityAfterProcLockedSection();
                                                                            throw th;
                                                                        } catch (Throwable th8) {
                                                                            th = th8;
                                                                        }
                                                                    }
                                                                }
                                                            } catch (Throwable th9) {
                                                                th = th9;
                                                            }
                                                        } catch (Throwable th10) {
                                                            th = th10;
                                                        }
                                                    }
                                                } catch (Exception e3) {
                                                    e = e3;
                                                    Slog.d("ActivityManager", "compaction sys error:" + e);
                                                    Slog.d("ActivityManager", "Exception occurred while compacting pid: " + name2 + ". Exception:" + e.getMessage());
                                                    Trace.traceEnd(64L);
                                                } catch (Throwable th11) {
                                                    th = th11;
                                                    Trace.traceEnd(64L);
                                                    throw th;
                                                }
                                            } catch (Exception e4) {
                                                e = e4;
                                            } catch (Throwable th12) {
                                                th = th12;
                                            }
                                        } catch (Exception e5) {
                                            e = e5;
                                            name2 = name4;
                                        } catch (Throwable th13) {
                                            th = th13;
                                        }
                                    } catch (Exception e6) {
                                        e = e6;
                                        name2 = name;
                                    } catch (Throwable th14) {
                                        th = th14;
                                    }
                                } catch (Exception e7) {
                                    e = e7;
                                    name2 = name;
                                } catch (Throwable th15) {
                                    th = th15;
                                }
                            } catch (Exception e8) {
                                e = e8;
                                name2 = name;
                            } catch (Throwable th16) {
                                th = th16;
                            }
                        } catch (Exception e9) {
                            e = e9;
                            name2 = name;
                        } catch (Throwable th17) {
                            th = th17;
                        }
                    }
                } catch (Throwable th18) {
                    th = th18;
                }
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes.dex */
    public final class FreezeHandler extends Handler implements ProcLocksReader.ProcLocksReaderCallback {
        private FreezeHandler() {
            super(CachedAppOptimizer.this.mCachedAppOptimizerThread.getLooper());
        }

        @Override // android.os.Handler
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case 3:
                    synchronized (CachedAppOptimizer.this.mAm) {
                        try {
                            ActivityManagerService.boostPriorityForLockedSection();
                            freezeProcess((ProcessRecord) msg.obj);
                        } catch (Throwable th) {
                            ActivityManagerService.resetPriorityAfterLockedSection();
                            throw th;
                        }
                    }
                    ActivityManagerService.resetPriorityAfterLockedSection();
                    return;
                case 4:
                    int pid = msg.arg1;
                    int frozenDuration = msg.arg2;
                    String processName = (String) msg.obj;
                    reportUnfreeze(pid, frozenDuration, processName);
                    return;
                default:
                    return;
            }
        }

        private void rescheduleFreeze(ProcessRecord proc, String reason) {
            Slog.d("ActivityManager", "Reschedule freeze for process " + proc.getPid() + " " + proc.processName + " (" + reason + ")");
            CachedAppOptimizer.this.unfreezeAppLSP(proc);
            CachedAppOptimizer.this.freezeAppAsyncLSP(proc);
        }

        /* JADX DEBUG: Another duplicated slice has different insns count: {[]}, finally: {[INVOKE] complete} */
        private void freezeProcess(final ProcessRecord proc) {
            proc.getPid();
            String name = proc.processName;
            ProcessCachedOptimizerRecord opt = proc.mOptRecord;
            opt.setPendingFreeze(false);
            synchronized (CachedAppOptimizer.this.mProcLock) {
                try {
                    ActivityManagerService.boostPriorityForProcLockedSection();
                    int pid = proc.getPid();
                    if (proc.mState.getCurAdj() >= 900 && !opt.shouldNotFreeze() && !ITranOomAdjuster.Instance().hookSetShouldNotFreeze(proc.info.packageName)) {
                        if (CachedAppOptimizer.this.mFreezerOverride) {
                            opt.setFreezerOverride(true);
                            Slog.d("ActivityManager", "Skipping freeze for process " + pid + " " + name + " curAdj = " + proc.mState.getCurAdj() + "(override)");
                            return;
                        }
                        if (pid != 0 && !opt.isFrozen()) {
                            Slog.d("ActivityManager", "freezing " + pid + " " + name);
                            try {
                                if (CachedAppOptimizer.freezeBinder(pid, true) != 0) {
                                    rescheduleFreeze(proc, "outstanding txns");
                                    ActivityManagerService.resetPriorityAfterProcLockedSection();
                                    return;
                                }
                            } catch (RuntimeException e) {
                                Slog.e("ActivityManager", "Unable to freeze binder for " + pid + " " + name);
                                CachedAppOptimizer.this.mFreezeHandler.post(new Runnable() { // from class: com.android.server.am.CachedAppOptimizer$FreezeHandler$$ExternalSyntheticLambda0
                                    @Override // java.lang.Runnable
                                    public final void run() {
                                        CachedAppOptimizer.FreezeHandler.this.m1412xd7ba7683(proc);
                                    }
                                });
                            }
                            long unfreezeTime = opt.getFreezeUnfreezeTime();
                            try {
                                Process.setProcessFrozen(pid, proc.uid, true);
                                opt.setFreezeUnfreezeTime(SystemClock.uptimeMillis());
                                opt.setFrozen(true);
                                CachedAppOptimizer.this.mFrozenProcesses.put(pid, proc);
                            } catch (Exception e2) {
                                Slog.w("ActivityManager", "Unable to freeze " + pid + " " + name);
                            }
                            long unfrozenDuration = opt.getFreezeUnfreezeTime() - unfreezeTime;
                            boolean frozen = opt.isFrozen();
                            ActivityManagerService.resetPriorityAfterProcLockedSection();
                            if (!frozen) {
                                return;
                            }
                            EventLog.writeEvent((int) EventLogTags.AM_FREEZE, Integer.valueOf(pid), name);
                            if (CachedAppOptimizer.this.mRandom.nextFloat() < CachedAppOptimizer.this.mFreezerStatsdSampleRate) {
                                FrameworkStatsLog.write(254, 1, pid, name, unfrozenDuration);
                            }
                            try {
                                int freezeInfo = CachedAppOptimizer.getBinderFreezeInfo(pid);
                                if ((freezeInfo & 4) != 0) {
                                    synchronized (CachedAppOptimizer.this.mProcLock) {
                                        ActivityManagerService.boostPriorityForProcLockedSection();
                                        rescheduleFreeze(proc, "new pending txns");
                                    }
                                    return;
                                }
                            } catch (RuntimeException e3) {
                                Slog.e("ActivityManager", "Unable to freeze binder for " + pid + " " + name);
                                CachedAppOptimizer.this.mFreezeHandler.post(new Runnable() { // from class: com.android.server.am.CachedAppOptimizer$FreezeHandler$$ExternalSyntheticLambda1
                                    @Override // java.lang.Runnable
                                    public final void run() {
                                        CachedAppOptimizer.FreezeHandler.this.m1413xd8f0c962(proc);
                                    }
                                });
                            }
                            try {
                                CachedAppOptimizer.this.mProcLocksReader.handleBlockingFileLocks(this);
                                return;
                            } catch (Exception e4) {
                                Slog.e("ActivityManager", "Unable to check file locks for " + name + "(" + pid + "): " + e4);
                                synchronized (CachedAppOptimizer.this.mProcLock) {
                                    try {
                                        ActivityManagerService.boostPriorityForProcLockedSection();
                                        CachedAppOptimizer.this.unfreezeAppLSP(proc);
                                        ActivityManagerService.resetPriorityAfterProcLockedSection();
                                        return;
                                    } finally {
                                        ActivityManagerService.resetPriorityAfterProcLockedSection();
                                    }
                                }
                            }
                        }
                        ActivityManagerService.resetPriorityAfterProcLockedSection();
                        return;
                    }
                    if (ActivityManagerDebugConfig.DEBUG_FREEZER) {
                        Slog.d("ActivityManager", "Skipping freeze for process " + pid + " " + name + " curAdj = " + proc.mState.getCurAdj() + ", shouldNotFreeze = " + opt.shouldNotFreeze());
                    }
                    ActivityManagerService.resetPriorityAfterProcLockedSection();
                } finally {
                    ActivityManagerService.resetPriorityAfterProcLockedSection();
                }
            }
        }

        /* JADX INFO: Access modifiers changed from: package-private */
        /* renamed from: lambda$freezeProcess$0$com-android-server-am-CachedAppOptimizer$FreezeHandler  reason: not valid java name */
        public /* synthetic */ void m1412xd7ba7683(ProcessRecord proc) {
            synchronized (CachedAppOptimizer.this.mAm) {
                try {
                    ActivityManagerService.boostPriorityForLockedSection();
                    proc.killLocked("Unable to freeze binder interface", 14, 19, true);
                } catch (Throwable th) {
                    ActivityManagerService.resetPriorityAfterLockedSection();
                    throw th;
                }
            }
            ActivityManagerService.resetPriorityAfterLockedSection();
        }

        /* JADX INFO: Access modifiers changed from: package-private */
        /* renamed from: lambda$freezeProcess$1$com-android-server-am-CachedAppOptimizer$FreezeHandler  reason: not valid java name */
        public /* synthetic */ void m1413xd8f0c962(ProcessRecord proc) {
            synchronized (CachedAppOptimizer.this.mAm) {
                try {
                    ActivityManagerService.boostPriorityForLockedSection();
                    proc.killLocked("Unable to freeze binder interface", 14, 19, true);
                } catch (Throwable th) {
                    ActivityManagerService.resetPriorityAfterLockedSection();
                    throw th;
                }
            }
            ActivityManagerService.resetPriorityAfterLockedSection();
        }

        private void reportUnfreeze(int pid, int frozenDuration, String processName) {
            EventLog.writeEvent((int) EventLogTags.AM_UNFREEZE, Integer.valueOf(pid), processName);
            if (CachedAppOptimizer.this.mRandom.nextFloat() < CachedAppOptimizer.this.mFreezerStatsdSampleRate) {
                FrameworkStatsLog.write(254, 2, pid, processName, frozenDuration);
            }
        }

        public void onBlockingFileLock(int pid) {
            if (ActivityManagerDebugConfig.DEBUG_FREEZER) {
                Slog.d("ActivityManager", "Process (pid=" + pid + ") holds blocking file lock");
            }
            synchronized (CachedAppOptimizer.this.mProcLock) {
                try {
                    ActivityManagerService.boostPriorityForProcLockedSection();
                    ProcessRecord app = (ProcessRecord) CachedAppOptimizer.this.mFrozenProcesses.get(pid);
                    if (app != null) {
                        Slog.i("ActivityManager", app.processName + " (" + pid + ") holds blocking file lock");
                        CachedAppOptimizer.this.unfreezeAppLSP(app);
                    }
                } catch (Throwable th) {
                    ActivityManagerService.resetPriorityAfterProcLockedSection();
                    throw th;
                }
            }
            ActivityManagerService.resetPriorityAfterProcLockedSection();
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes.dex */
    public static final class DefaultProcessDependencies implements ProcessDependencies {
        public static volatile int mPidCompacting = -1;

        private DefaultProcessDependencies() {
        }

        @Override // com.android.server.am.CachedAppOptimizer.ProcessDependencies
        public long[] getRss(int pid) {
            return Process.getRss(pid);
        }

        @Override // com.android.server.am.CachedAppOptimizer.ProcessDependencies
        public void performCompaction(String action, int pid) throws IOException {
            mPidCompacting = pid;
            if (action.equals(CachedAppOptimizer.COMPACT_ACTION_STRING[3])) {
                CachedAppOptimizer.compactProcess(pid, 3);
            } else if (action.equals(CachedAppOptimizer.COMPACT_ACTION_STRING[1])) {
                CachedAppOptimizer.compactProcess(pid, 1);
            } else if (action.equals(CachedAppOptimizer.COMPACT_ACTION_STRING[2])) {
                CachedAppOptimizer.compactProcess(pid, 2);
            } else if (action.equals(CachedAppOptimizer.compactActionIntToString(11))) {
                CachedAppOptimizer.compactProcess(pid, 11);
            }
            mPidCompacting = -1;
        }
    }
}
