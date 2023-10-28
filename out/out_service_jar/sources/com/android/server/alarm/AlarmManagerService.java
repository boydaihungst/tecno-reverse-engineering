package com.android.server.alarm;

import android.app.ActivityManager;
import android.app.ActivityManagerInternal;
import android.app.ActivityOptions;
import android.app.AlarmManager;
import android.app.AppGlobals;
import android.app.AppOpsManager;
import android.app.BroadcastOptions;
import android.app.IAlarmCompleteListener;
import android.app.IAlarmListener;
import android.app.IAlarmManager;
import android.app.PendingIntent;
import android.app.compat.CompatChanges;
import android.app.role.RoleManager;
import android.app.usage.UsageStatsManagerInternal;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.PermissionChecker;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.IPackageManager;
import android.content.pm.PackageManagerInternal;
import android.content.pm.ParceledListSlice;
import android.content.pm.UserInfo;
import android.database.ContentObserver;
import android.hardware.audio.common.V2_0.AudioFormat;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.BatteryManager;
import android.os.Binder;
import android.os.Build;
import android.os.Bundle;
import android.os.Debug;
import android.os.Environment;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.os.ParcelableException;
import android.os.PowerManager;
import android.os.Process;
import android.os.RemoteException;
import android.os.ResultReceiver;
import android.os.ServiceManager;
import android.os.ShellCallback;
import android.os.ShellCommand;
import android.os.SystemClock;
import android.os.SystemProperties;
import android.os.ThreadLocalWorkSource;
import android.os.Trace;
import android.os.UserHandle;
import android.os.UserManager;
import android.os.WorkSource;
import android.provider.DeviceConfig;
import android.provider.Settings;
import android.system.Os;
import android.text.TextUtils;
import android.text.format.DateFormat;
import android.util.ArrayMap;
import android.util.ArraySet;
import android.util.EventLog;
import android.util.IndentingPrintWriter;
import android.util.Log;
import android.util.LongArrayQueue;
import android.util.NtpTrustedTime;
import android.util.Pair;
import android.util.Slog;
import android.util.SparseArray;
import android.util.SparseArrayMap;
import android.util.SparseBooleanArray;
import android.util.SparseIntArray;
import android.util.SparseLongArray;
import android.util.TimeUtils;
import android.util.proto.ProtoOutputStream;
import com.android.internal.app.IAppOpsCallback;
import com.android.internal.app.IAppOpsService;
import com.android.internal.util.FrameworkStatsLog;
import com.android.internal.util.LocalLog;
import com.android.internal.util.RingBuffer;
import com.android.internal.util.jobs.DumpUtils;
import com.android.internal.util.jobs.StatLogger;
import com.android.server.AlarmManagerInternal;
import com.android.server.AppStateTracker;
import com.android.server.AppStateTrackerImpl;
import com.android.server.DeviceIdleInternal;
import com.android.server.EventLogTags;
import com.android.server.JobSchedulerBackgroundThread;
import com.android.server.LocalServices;
import com.android.server.SystemService;
import com.android.server.SystemServiceManager;
import com.android.server.alarm.AlarmManagerService;
import com.android.server.alarm.AlarmStore;
import com.android.server.am.HostingRecord;
import com.android.server.backup.BackupAgentTimeoutParameters;
import com.android.server.job.controllers.JobStatus;
import com.android.server.pm.PackageManagerService;
import com.android.server.pm.parsing.pkg.AndroidPackage;
import com.android.server.pm.permission.PermissionManagerService;
import com.android.server.pm.permission.PermissionManagerServiceInternal;
import com.android.server.policy.PhoneWindowManager;
import com.android.server.tare.AlarmManagerEconomicPolicy;
import com.android.server.tare.EconomyManagerInternal;
import com.android.server.usage.AppStandbyInternal;
import com.transsion.hubcore.griffin.ITranGriffinFeature;
import com.transsion.hubcore.server.alarm.ITranAlarmManagerService;
import dalvik.annotation.optimization.NeverCompile;
import defpackage.CompanionAppsPermissions;
import java.io.File;
import java.io.FileDescriptor;
import java.io.IOException;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.time.DateTimeException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import java.util.TimeZone;
import java.util.TreeSet;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Predicate;
import java.util.function.Supplier;
import libcore.util.EmptyArray;
/* loaded from: classes.dex */
public class AlarmManagerService extends SystemService {
    static final int ACTIVE_INDEX = 0;
    private static final int ELAPSED_REALTIME_WAKEUP_MASK = 4;
    static final int FREQUENT_INDEX = 2;
    static final long INDEFINITE_DELAY = 31536000000L;
    static final int IS_WAKEUP_MASK = 5;
    static final long MIN_FUZZABLE_INTERVAL = 10000;
    static final int NEVER_INDEX = 4;
    static final int PRIO_NORMAL = 2;
    static final int PRIO_TICK = 0;
    static final int PRIO_WAKEUP = 1;
    static final int RARE_INDEX = 3;
    static final boolean RECORD_ALARMS_IN_HISTORY = true;
    static final boolean RECORD_DEVICE_IDLE_ALARMS = true;
    private static final int REMOVAL_HISTORY_SIZE_PER_UID = 10;
    private static final int RTC_WAKEUP_MASK = 1;
    protected static final String TAG = "AlarmManager";
    private static final long TEMPORARY_QUOTA_DURATION = 86400000;
    static final int TICK_HISTORY_DEPTH = 10;
    static final String TIMEZONE_PROPERTY = "persist.sys.timezone";
    static final int TIME_CHANGED_MASK = 65536;
    static final String TIME_TICK_TAG = "TIME_TICK";
    static final int WORKING_INDEX = 1;
    private static boolean sAlarmDebug;
    private final long DEEP_IDLE_TIME;
    private final long DUMP_ALARM_HPROF_INTERVAL;
    private ActivityManagerInternal mActivityManagerInternal;
    ActivityOptions mActivityOptsRestrictBal;
    private final SparseArrayMap<String, ArrayMap<EconomyManagerInternal.ActionBill, Boolean>> mAffordabilityCache;
    private final EconomyManagerInternal.AffordabilityChangeListener mAffordabilityChangeListener;
    private final Runnable mAlarmClockUpdater;
    final Comparator<Alarm> mAlarmDispatchComparator;
    AlarmStore mAlarmStore;
    SparseIntArray mAlarmsPerUid;
    AppWakeupHistory mAllowWhileIdleCompatHistory;
    final ArrayList<IdleDispatchEntry> mAllowWhileIdleDispatches;
    AppWakeupHistory mAllowWhileIdleHistory;
    AppOpsManager mAppOps;
    boolean mAppStandbyParole;
    private AppStateTrackerImpl mAppStateTracker;
    AppWakeupHistory mAppWakeupHistory;
    private final Intent mBackgroundIntent;
    BroadcastOptions mBroadcastOptsRestrictBal;
    int mBroadcastRefCount;
    final SparseArray<ArrayMap<String, BroadcastStats>> mBroadcastStats;
    ClockReceiver mClockReceiver;
    private ConnectivityManager mConnectManager;
    Constants mConstants;
    int mCurrentSeq;
    PendingIntent mDateChangeSender;
    final DeliveryTracker mDeliveryTracker;
    private final EconomyManagerInternal mEconomyManagerInternal;
    volatile Set<Integer> mExactAlarmCandidates;
    private List<String> mExceptList;
    private final AppStateTrackerImpl.Listener mForceAppStandbyListener;
    AlarmHandler mHandler;
    private final SparseArray<AlarmManager.AlarmClockInfo> mHandlerSparseAlarmClockArray;
    ArrayList<InFlight> mInFlight;
    private final ArrayList<AlarmManagerInternal.InFlightListener> mInFlightListeners;
    protected final Injector mInjector;
    boolean mInteractive;
    long mLastAlarmDeliveryTime;
    private long mLastDumpAlarmHprofTime;
    SparseIntArray mLastOpScheduleExactAlarm;
    private final SparseLongArray mLastPriorityAlarmDispatch;
    private long mLastTickReceived;
    private long mLastTickSet;
    long mLastTimeChangeClockTime;
    long mLastTimeChangeRealtime;
    private long mLastTrigger;
    private long mLastWakeup;
    private int mListenerCount;
    IBinder.DeathRecipient mListenerDeathRecipient;
    private int mListenerFinishCount;
    DeviceIdleInternal mLocalDeviceIdleController;
    private volatile PermissionManagerServiceInternal mLocalPermissionManager;
    final Object mLock;
    final LocalLog mLog;
    long mMaxDelayTime;
    MetricsHelper mMetricsHelper;
    private boolean mNetworkConnected;
    private NetworkInfo mNetworkInfo;
    private final SparseArray<AlarmManager.AlarmClockInfo> mNextAlarmClockForUser;
    private boolean mNextAlarmClockMayChange;
    private long mNextNonWakeUpSetAt;
    private long mNextNonWakeup;
    long mNextNonWakeupDeliveryTime;
    private int mNextTickHistory;
    Alarm mNextWakeFromIdle;
    private long mNextWakeUpSetAt;
    private long mNextWakeup;
    long mNonInteractiveStartTime;
    long mNonInteractiveTime;
    int mNumDelayedAlarms;
    int mNumTimeChanged;
    BroadcastOptions mOptsTimeBroadcast;
    BroadcastOptions mOptsWithFgs;
    BroadcastOptions mOptsWithFgsForAlarmClock;
    BroadcastOptions mOptsWithoutFgs;
    private PackageManagerInternal mPackageManagerInternal;
    SparseArray<ArrayList<Alarm>> mPendingBackgroundAlarms;
    Alarm mPendingIdleUntil;
    ArrayList<Alarm> mPendingNonWakeupAlarms;
    private final SparseBooleanArray mPendingSendNextAlarmClockChangedForUser;
    ArrayList<Alarm> mPendingWhileIdleAlarms;
    final HashMap<String, PriorityClass> mPriorities;
    private final SparseArray<RingBuffer<RemovedAlarm>> mRemovalHistory;
    private RoleManager mRoleManager;
    private boolean mScreenOff;
    private long mScreenOffTime;
    private int mSendCount;
    private int mSendFinishCount;
    private final IBinder mService;
    long mStartCurrentDelayTime;
    private final StatLogger mStatLogger;
    private ArrayList<String> mSystemApps;
    int mSystemUiUid;
    TemporaryQuotaReserve mTemporaryQuotaReserve;
    private final long[] mTickHistory;
    Intent mTimeTickIntent;
    IAlarmListener mTimeTickTrigger;
    private final SparseArray<AlarmManager.AlarmClockInfo> mTmpSparseAlarmClockArray;
    long mTotalDelayTime;
    private UsageStatsManagerInternal mUsageStatsManagerInternal;
    PowerManager.WakeLock mWakeLock;
    Context mWhiteListContext;
    SharedPreferences mWhiteListPrefs;
    long screenOffThreshold;
    private static boolean sTranSlmSupport = "1".equals(SystemProperties.get("ro.sleep_master"));
    protected static boolean localLOGV = true;
    protected static boolean DEBUG_BATCH = true;
    protected static boolean DEBUG_ALARM_CLOCK = true;
    protected static boolean DEBUG_LISTENER_CALLBACK = true;
    protected static boolean DEBUG_WAKELOCK = true;
    protected static boolean DEBUG_BG_LIMIT = true;
    protected static boolean DEBUG_STANDBY = true;
    protected static boolean DEBUG_TARE = true;
    private static final Intent NEXT_ALARM_CLOCK_CHANGED_INTENT = new Intent("android.app.action.NEXT_ALARM_CLOCK_CHANGED").addFlags(AudioFormat.APTX_HD);

    /* loaded from: classes.dex */
    interface Stats {
        public static final int HAS_SCHEDULE_EXACT_ALARM = 1;
        public static final int REORDER_ALARMS_FOR_STANDBY = 0;
        public static final int REORDER_ALARMS_FOR_TARE = 2;
    }

    /* renamed from: -$$Nest$sminit  reason: not valid java name */
    static /* bridge */ /* synthetic */ long m930$$Nest$sminit() {
        return init();
    }

    /* JADX INFO: Access modifiers changed from: private */
    public static native void close(long j);

    /* JADX INFO: Access modifiers changed from: private */
    public static native long getNextAlarm(long j, int i);

    private static native long init();

    protected static native int set(long j, int i, long j2, long j3);

    /* JADX INFO: Access modifiers changed from: private */
    public static native int setKernelTime(long j, long j2);

    /* JADX INFO: Access modifiers changed from: private */
    public static native int setKernelTimezone(long j, int i);

    /* JADX INFO: Access modifiers changed from: private */
    public static native int waitForAlarm(long j);

    /* JADX INFO: Access modifiers changed from: package-private */
    public static boolean isTimeTickAlarm(Alarm a) {
        return a.uid == 1000 && TIME_TICK_TAG.equals(a.listenerTag);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* loaded from: classes.dex */
    public static final class IdleDispatchEntry {
        long argRealtime;
        long elapsedRealtime;
        String op;
        String pkg;
        String tag;
        int uid;

        IdleDispatchEntry() {
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$new$0$com-android-server-alarm-AlarmManagerService  reason: not valid java name */
    public /* synthetic */ void m937lambda$new$0$comandroidserveralarmAlarmManagerService() {
        this.mNextAlarmClockMayChange = true;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* loaded from: classes.dex */
    public static class TemporaryQuotaReserve {
        private long mMaxDuration;
        private final ArrayMap<Pair<String, Integer>, QuotaInfo> mQuotaBuffer = new ArrayMap<>();

        /* JADX INFO: Access modifiers changed from: private */
        /* loaded from: classes.dex */
        public static class QuotaInfo {
            public long expirationTime;
            public long lastUsage;
            public int remainingQuota;

            private QuotaInfo() {
            }
        }

        TemporaryQuotaReserve(long maxDuration) {
            this.mMaxDuration = maxDuration;
        }

        void replenishQuota(String packageName, int userId, int quota, long nowElapsed) {
            if (quota <= 0) {
                return;
            }
            Pair<String, Integer> packageUser = Pair.create(packageName, Integer.valueOf(userId));
            QuotaInfo currentQuotaInfo = this.mQuotaBuffer.get(packageUser);
            if (currentQuotaInfo == null) {
                currentQuotaInfo = new QuotaInfo();
                this.mQuotaBuffer.put(packageUser, currentQuotaInfo);
            }
            currentQuotaInfo.remainingQuota = quota;
            currentQuotaInfo.expirationTime = this.mMaxDuration + nowElapsed;
        }

        boolean hasQuota(String packageName, int userId, long triggerElapsed) {
            Pair<String, Integer> packageUser = Pair.create(packageName, Integer.valueOf(userId));
            QuotaInfo quotaInfo = this.mQuotaBuffer.get(packageUser);
            return quotaInfo != null && quotaInfo.remainingQuota > 0 && triggerElapsed <= quotaInfo.expirationTime;
        }

        void recordUsage(String packageName, int userId, long nowElapsed) {
            Pair<String, Integer> packageUser = Pair.create(packageName, Integer.valueOf(userId));
            QuotaInfo quotaInfo = this.mQuotaBuffer.get(packageUser);
            if (quotaInfo == null) {
                Slog.wtf(AlarmManagerService.TAG, "Temporary quota being consumed at " + nowElapsed + " but not found for package: " + packageName + ", user: " + userId);
            } else if (nowElapsed > quotaInfo.lastUsage) {
                if (quotaInfo.remainingQuota <= 0) {
                    Slog.wtf(AlarmManagerService.TAG, "Temporary quota being consumed at " + nowElapsed + " but remaining only " + quotaInfo.remainingQuota + " for package: " + packageName + ", user: " + userId);
                } else if (quotaInfo.expirationTime < nowElapsed) {
                    Slog.wtf(AlarmManagerService.TAG, "Temporary quota being consumed at " + nowElapsed + " but expired at " + quotaInfo.expirationTime + " for package: " + packageName + ", user: " + userId);
                } else {
                    quotaInfo.remainingQuota--;
                }
                quotaInfo.lastUsage = nowElapsed;
            }
        }

        void cleanUpExpiredQuotas(long nowElapsed) {
            for (int i = this.mQuotaBuffer.size() - 1; i >= 0; i--) {
                QuotaInfo quotaInfo = this.mQuotaBuffer.valueAt(i);
                if (quotaInfo.expirationTime < nowElapsed) {
                    this.mQuotaBuffer.removeAt(i);
                }
            }
        }

        void removeForUser(int userId) {
            for (int i = this.mQuotaBuffer.size() - 1; i >= 0; i--) {
                Pair<String, Integer> packageUserKey = this.mQuotaBuffer.keyAt(i);
                if (((Integer) packageUserKey.second).intValue() == userId) {
                    this.mQuotaBuffer.removeAt(i);
                }
            }
        }

        void removeForPackage(String packageName, int userId) {
            Pair<String, Integer> packageUser = Pair.create(packageName, Integer.valueOf(userId));
            this.mQuotaBuffer.remove(packageUser);
        }

        void dump(IndentingPrintWriter pw, long nowElapsed) {
            pw.increaseIndent();
            for (int i = 0; i < this.mQuotaBuffer.size(); i++) {
                Pair<String, Integer> packageUser = this.mQuotaBuffer.keyAt(i);
                QuotaInfo quotaInfo = this.mQuotaBuffer.valueAt(i);
                pw.print((String) packageUser.first);
                pw.print(", u");
                pw.print(packageUser.second);
                pw.print(": ");
                if (quotaInfo == null) {
                    pw.print("--");
                } else {
                    pw.print("quota: ");
                    pw.print(quotaInfo.remainingQuota);
                    pw.print(", expiration: ");
                    TimeUtils.formatDuration(quotaInfo.expirationTime, nowElapsed, pw);
                    pw.print(" last used: ");
                    TimeUtils.formatDuration(quotaInfo.lastUsage, nowElapsed, pw);
                }
                pw.println();
            }
            pw.decreaseIndent();
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* loaded from: classes.dex */
    public static class AppWakeupHistory {
        private ArrayMap<Pair<String, Integer>, LongArrayQueue> mPackageHistory = new ArrayMap<>();
        private long mWindowSize;

        AppWakeupHistory(long windowSize) {
            this.mWindowSize = windowSize;
        }

        void recordAlarmForPackage(String packageName, int userId, long nowElapsed) {
            Pair<String, Integer> packageUser = Pair.create(packageName, Integer.valueOf(userId));
            LongArrayQueue history = this.mPackageHistory.get(packageUser);
            if (history == null) {
                history = new LongArrayQueue();
                this.mPackageHistory.put(packageUser, history);
            }
            if (history.size() == 0 || history.peekLast() < nowElapsed) {
                history.addLast(nowElapsed);
            }
            snapToWindow(history);
        }

        void removeForUser(int userId) {
            for (int i = this.mPackageHistory.size() - 1; i >= 0; i--) {
                Pair<String, Integer> packageUserKey = this.mPackageHistory.keyAt(i);
                if (((Integer) packageUserKey.second).intValue() == userId) {
                    this.mPackageHistory.removeAt(i);
                }
            }
        }

        void removeForPackage(String packageName, int userId) {
            Pair<String, Integer> packageUser = Pair.create(packageName, Integer.valueOf(userId));
            this.mPackageHistory.remove(packageUser);
        }

        private void snapToWindow(LongArrayQueue history) {
            while (history.peekFirst() + this.mWindowSize < history.peekLast()) {
                history.removeFirst();
            }
        }

        int getTotalWakeupsInWindow(String packageName, int userId) {
            LongArrayQueue history = this.mPackageHistory.get(Pair.create(packageName, Integer.valueOf(userId)));
            if (history == null) {
                return 0;
            }
            return history.size();
        }

        long getNthLastWakeupForPackage(String packageName, int userId, int n) {
            int i;
            LongArrayQueue history = this.mPackageHistory.get(Pair.create(packageName, Integer.valueOf(userId)));
            if (history != null && (i = history.size() - n) >= 0) {
                return history.get(i);
            }
            return 0L;
        }

        void dump(IndentingPrintWriter pw, long nowElapsed) {
            pw.increaseIndent();
            for (int i = 0; i < this.mPackageHistory.size(); i++) {
                Pair<String, Integer> packageUser = this.mPackageHistory.keyAt(i);
                LongArrayQueue timestamps = this.mPackageHistory.valueAt(i);
                pw.print((String) packageUser.first);
                pw.print(", u");
                pw.print(packageUser.second);
                pw.print(": ");
                int lastIdx = Math.max(0, timestamps.size() - 100);
                for (int j = timestamps.size() - 1; j >= lastIdx; j--) {
                    TimeUtils.formatDuration(timestamps.get(j), nowElapsed, pw);
                    pw.print(", ");
                }
                pw.println();
            }
            pw.decreaseIndent();
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* loaded from: classes.dex */
    public static class RemovedAlarm {
        static final int REMOVE_REASON_ALARM_CANCELLED = 1;
        static final int REMOVE_REASON_DATA_CLEARED = 3;
        static final int REMOVE_REASON_EXACT_PERMISSION_REVOKED = 2;
        static final int REMOVE_REASON_PI_CANCELLED = 4;
        static final int REMOVE_REASON_UNDEFINED = 0;
        final int mRemoveReason;
        final String mTag;
        final long mWhenRemovedElapsed;
        final long mWhenRemovedRtc;

        RemovedAlarm(Alarm a, int removeReason, long nowRtc, long nowElapsed) {
            this.mTag = a.statsTag;
            this.mRemoveReason = removeReason;
            this.mWhenRemovedRtc = nowRtc;
            this.mWhenRemovedElapsed = nowElapsed;
        }

        static final boolean isLoggable(int reason) {
            return reason != 0;
        }

        static final String removeReasonToString(int reason) {
            switch (reason) {
                case 1:
                    return "alarm_cancelled";
                case 2:
                    return "exact_alarm_permission_revoked";
                case 3:
                    return "data_cleared";
                case 4:
                    return "pi_cancelled";
                default:
                    return "unknown:" + reason;
            }
        }

        void dump(IndentingPrintWriter pw, long nowElapsed, SimpleDateFormat sdf) {
            pw.print("[tag", this.mTag);
            pw.print(PhoneWindowManager.SYSTEM_DIALOG_REASON_KEY, removeReasonToString(this.mRemoveReason));
            pw.print("elapsed=");
            TimeUtils.formatDuration(this.mWhenRemovedElapsed, nowElapsed, pw);
            pw.print(" rtc=");
            pw.print(sdf.format(new Date(this.mWhenRemovedRtc)));
            pw.println("]");
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* loaded from: classes.dex */
    public final class Constants implements DeviceConfig.OnPropertiesChangedListener, EconomyManagerInternal.TareStateChangeListener {
        private static final int DEFAULT_ALLOW_WHILE_IDLE_COMPAT_QUOTA = 1;
        private static final long DEFAULT_ALLOW_WHILE_IDLE_COMPAT_WINDOW = 540000;
        private static final int DEFAULT_ALLOW_WHILE_IDLE_QUOTA = 72;
        private static final long DEFAULT_ALLOW_WHILE_IDLE_WHITELIST_DURATION = 10000;
        private static final long DEFAULT_ALLOW_WHILE_IDLE_WINDOW = 3600000;
        private static final int DEFAULT_APP_STANDBY_RESTRICTED_QUOTA = 1;
        private static final long DEFAULT_APP_STANDBY_RESTRICTED_WINDOW = 86400000;
        private static final long DEFAULT_APP_STANDBY_WINDOW = 3600000;
        private static final boolean DEFAULT_CRASH_NON_CLOCK_APPS = true;
        private static final boolean DEFAULT_KILL_ON_SCHEDULE_EXACT_ALARM_REVOKED = true;
        private static final boolean DEFAULT_LAZY_BATCHING = true;
        private static final long DEFAULT_LISTENER_TIMEOUT = 5000;
        private static final int DEFAULT_MAX_ALARMS_PER_UID = 500;
        private static final long DEFAULT_MAX_DEVICE_IDLE_FUZZ = 900000;
        private static final long DEFAULT_MAX_INTERVAL = 31536000000L;
        private static final long DEFAULT_MIN_DEVICE_IDLE_FUZZ = 120000;
        private static final long DEFAULT_MIN_FUTURITY = 5000;
        private static final long DEFAULT_MIN_INTERVAL = 60000;
        private static final long DEFAULT_MIN_WINDOW = 600000;
        private static final long DEFAULT_PRIORITY_ALARM_DELAY = 540000;
        private static final int DEFAULT_TEMPORARY_QUOTA_BUMP = 0;
        private static final boolean DEFAULT_TIME_TICK_ALLOWED_WHILE_IDLE = true;
        static final String KEY_ALLOW_WHILE_IDLE_COMPAT_QUOTA = "allow_while_idle_compat_quota";
        static final String KEY_ALLOW_WHILE_IDLE_COMPAT_WINDOW = "allow_while_idle_compat_window";
        static final String KEY_ALLOW_WHILE_IDLE_QUOTA = "allow_while_idle_quota";
        static final String KEY_ALLOW_WHILE_IDLE_WHITELIST_DURATION = "allow_while_idle_whitelist_duration";
        static final String KEY_ALLOW_WHILE_IDLE_WINDOW = "allow_while_idle_window";
        private static final String KEY_APP_STANDBY_RESTRICTED_QUOTA = "standby_quota_restricted";
        private static final String KEY_APP_STANDBY_RESTRICTED_WINDOW = "app_standby_restricted_window";
        private static final String KEY_APP_STANDBY_WINDOW = "app_standby_window";
        static final String KEY_CRASH_NON_CLOCK_APPS = "crash_non_clock_apps";
        static final String KEY_EXACT_ALARM_DENY_LIST = "exact_alarm_deny_list";
        static final String KEY_KILL_ON_SCHEDULE_EXACT_ALARM_REVOKED = "kill_on_schedule_exact_alarm_revoked";
        static final String KEY_LAZY_BATCHING = "lazy_batching";
        static final String KEY_LISTENER_TIMEOUT = "listener_timeout";
        static final String KEY_MAX_ALARMS_PER_UID = "max_alarms_per_uid";
        static final String KEY_MAX_DEVICE_IDLE_FUZZ = "max_device_idle_fuzz";
        static final String KEY_MAX_INTERVAL = "max_interval";
        static final String KEY_MIN_DEVICE_IDLE_FUZZ = "min_device_idle_fuzz";
        static final String KEY_MIN_FUTURITY = "min_futurity";
        static final String KEY_MIN_INTERVAL = "min_interval";
        static final String KEY_MIN_WINDOW = "min_window";
        private static final String KEY_PREFIX_STANDBY_QUOTA = "standby_quota_";
        static final String KEY_PRIORITY_ALARM_DELAY = "priority_alarm_delay";
        static final String KEY_TEMPORARY_QUOTA_BUMP = "temporary_quota_bump";
        private static final String KEY_TIME_TICK_ALLOWED_WHILE_IDLE = "time_tick_allowed_while_idle";
        static final int MAX_EXACT_ALARM_DENY_LIST_SIZE = 250;
        public int[] APP_STANDBY_QUOTAS;
        private final int[] DEFAULT_APP_STANDBY_QUOTAS;
        final String[] KEYS_APP_STANDBY_QUOTAS = {"standby_quota_active", "standby_quota_working", "standby_quota_frequent", "standby_quota_rare", "standby_quota_never"};
        public long MIN_FUTURITY = 5000;
        public long MIN_INTERVAL = 60000;
        public long MAX_INTERVAL = 31536000000L;
        public long MIN_WINDOW = 600000;
        public long ALLOW_WHILE_IDLE_WHITELIST_DURATION = 10000;
        public long LISTENER_TIMEOUT = 5000;
        public int MAX_ALARMS_PER_UID = 500;
        public long APP_STANDBY_WINDOW = 3600000;
        public int APP_STANDBY_RESTRICTED_QUOTA = 1;
        public long APP_STANDBY_RESTRICTED_WINDOW = 86400000;
        public boolean LAZY_BATCHING = true;
        public boolean TIME_TICK_ALLOWED_WHILE_IDLE = true;
        public int ALLOW_WHILE_IDLE_QUOTA = 72;
        public int ALLOW_WHILE_IDLE_COMPAT_QUOTA = 1;
        public long ALLOW_WHILE_IDLE_COMPAT_WINDOW = 540000;
        public long ALLOW_WHILE_IDLE_WINDOW = 3600000;
        public boolean CRASH_NON_CLOCK_APPS = true;
        public long PRIORITY_ALARM_DELAY = 540000;
        public volatile Set<String> EXACT_ALARM_DENY_LIST = Collections.emptySet();
        public long MIN_DEVICE_IDLE_FUZZ = 120000;
        public long MAX_DEVICE_IDLE_FUZZ = DEFAULT_MAX_DEVICE_IDLE_FUZZ;
        public boolean KILL_ON_SCHEDULE_EXACT_ALARM_REVOKED = true;
        public boolean USE_TARE_POLICY = false;
        public int TEMPORARY_QUOTA_BUMP = 0;
        private long mLastAllowWhileIdleWhitelistDuration = -1;
        private int mVersion = 0;

        Constants(Handler handler) {
            int[] iArr = {720, 10, 2, 1, 0};
            this.DEFAULT_APP_STANDBY_QUOTAS = iArr;
            this.APP_STANDBY_QUOTAS = new int[iArr.length];
            updateAllowWhileIdleWhitelistDurationLocked();
            int i = 0;
            while (true) {
                int[] iArr2 = this.APP_STANDBY_QUOTAS;
                if (i < iArr2.length) {
                    iArr2[i] = this.DEFAULT_APP_STANDBY_QUOTAS[i];
                    i++;
                } else {
                    return;
                }
            }
        }

        public int getVersion() {
            int i;
            synchronized (AlarmManagerService.this.mLock) {
                i = this.mVersion;
            }
            return i;
        }

        public void start() {
            AlarmManagerService.this.mInjector.registerDeviceConfigListener(this);
            EconomyManagerInternal economyManagerInternal = (EconomyManagerInternal) LocalServices.getService(EconomyManagerInternal.class);
            economyManagerInternal.registerTareStateChangeListener(this);
            onPropertiesChanged(DeviceConfig.getProperties("alarm_manager", new String[0]));
            updateTareSettings(economyManagerInternal.isEnabled());
        }

        public void updateAllowWhileIdleWhitelistDurationLocked() {
            long j = this.mLastAllowWhileIdleWhitelistDuration;
            long j2 = this.ALLOW_WHILE_IDLE_WHITELIST_DURATION;
            if (j != j2) {
                this.mLastAllowWhileIdleWhitelistDuration = j2;
                AlarmManagerService.this.mOptsWithFgs.setTemporaryAppAllowlist(this.ALLOW_WHILE_IDLE_WHITELIST_DURATION, 0, (int) FrameworkStatsLog.APP_BACKGROUND_RESTRICTIONS_INFO__EXEMPTION_REASON__REASON_ALARM_MANAGER_WHILE_IDLE, "");
                AlarmManagerService.this.mOptsWithFgsForAlarmClock.setTemporaryAppAllowlist(this.ALLOW_WHILE_IDLE_WHITELIST_DURATION, 0, (int) FrameworkStatsLog.APP_BACKGROUND_RESTRICTIONS_INFO__EXEMPTION_REASON__REASON_ALARM_MANAGER_ALARM_CLOCK, "");
                AlarmManagerService.this.mOptsWithoutFgs.setTemporaryAppAllowlist(this.ALLOW_WHILE_IDLE_WHITELIST_DURATION, 1, -1, "");
            }
        }

        /* JADX WARN: Can't fix incorrect switch cases order, some code will duplicate */
        public void onPropertiesChanged(DeviceConfig.Properties properties) {
            String[] values;
            boolean standbyQuotaUpdated = false;
            boolean deviceIdleFuzzBoundariesUpdated = false;
            synchronized (AlarmManagerService.this.mLock) {
                this.mVersion++;
                for (String name : properties.getKeyset()) {
                    if (name != null) {
                        char c = 65535;
                        switch (name.hashCode()) {
                            case -2073052857:
                                if (name.equals(KEY_KILL_ON_SCHEDULE_EXACT_ALARM_REVOKED)) {
                                    c = 20;
                                    break;
                                }
                                break;
                            case -1783300397:
                                if (name.equals(KEY_LAZY_BATCHING)) {
                                    c = '\r';
                                    break;
                                }
                                break;
                            case -1577286106:
                                if (name.equals(KEY_ALLOW_WHILE_IDLE_COMPAT_WINDOW)) {
                                    c = 7;
                                    break;
                                }
                                break;
                            case -1490947714:
                                if (name.equals(KEY_MIN_DEVICE_IDLE_FUZZ)) {
                                    c = 18;
                                    break;
                                }
                                break;
                            case -1293038119:
                                if (name.equals(KEY_MIN_FUTURITY)) {
                                    c = 0;
                                    break;
                                }
                                break;
                            case -975718548:
                                if (name.equals(KEY_MAX_ALARMS_PER_UID)) {
                                    c = '\n';
                                    break;
                                }
                                break;
                            case -880907612:
                                if (name.equals(KEY_APP_STANDBY_RESTRICTED_WINDOW)) {
                                    c = '\f';
                                    break;
                                }
                                break;
                            case -618440710:
                                if (name.equals(KEY_PRIORITY_ALARM_DELAY)) {
                                    c = 16;
                                    break;
                                }
                                break;
                            case -591494837:
                                if (name.equals(KEY_TEMPORARY_QUOTA_BUMP)) {
                                    c = 21;
                                    break;
                                }
                                break;
                            case -577593775:
                                if (name.equals(KEY_ALLOW_WHILE_IDLE_QUOTA)) {
                                    c = 3;
                                    break;
                                }
                                break;
                            case -564889801:
                                if (name.equals(KEY_ALLOW_WHILE_IDLE_WINDOW)) {
                                    c = 6;
                                    break;
                                }
                                break;
                            case -410928980:
                                if (name.equals(KEY_MAX_DEVICE_IDLE_FUZZ)) {
                                    c = 19;
                                    break;
                                }
                                break;
                            case -392965507:
                                if (name.equals(KEY_MIN_WINDOW)) {
                                    c = 4;
                                    break;
                                }
                                break;
                            case -147388512:
                                if (name.equals(KEY_APP_STANDBY_WINDOW)) {
                                    c = 11;
                                    break;
                                }
                                break;
                            case 620314701:
                                if (name.equals(KEY_CRASH_NON_CLOCK_APPS)) {
                                    c = 15;
                                    break;
                                }
                                break;
                            case 932562134:
                                if (name.equals(KEY_LISTENER_TIMEOUT)) {
                                    c = '\t';
                                    break;
                                }
                                break;
                            case 1139967827:
                                if (name.equals(KEY_ALLOW_WHILE_IDLE_WHITELIST_DURATION)) {
                                    c = '\b';
                                    break;
                                }
                                break;
                            case 1213697417:
                                if (name.equals(KEY_TIME_TICK_ALLOWED_WHILE_IDLE)) {
                                    c = 14;
                                    break;
                                }
                                break;
                            case 1528643904:
                                if (name.equals(KEY_MAX_INTERVAL)) {
                                    c = 2;
                                    break;
                                }
                                break;
                            case 1690736963:
                                if (name.equals(KEY_EXACT_ALARM_DENY_LIST)) {
                                    c = 17;
                                    break;
                                }
                                break;
                            case 1883600258:
                                if (name.equals(KEY_ALLOW_WHILE_IDLE_COMPAT_QUOTA)) {
                                    c = 5;
                                    break;
                                }
                                break;
                            case 2003069970:
                                if (name.equals(KEY_MIN_INTERVAL)) {
                                    c = 1;
                                    break;
                                }
                                break;
                        }
                        switch (c) {
                            case 0:
                                this.MIN_FUTURITY = properties.getLong(KEY_MIN_FUTURITY, 5000L);
                                break;
                            case 1:
                                this.MIN_INTERVAL = properties.getLong(KEY_MIN_INTERVAL, 60000L);
                                break;
                            case 2:
                                this.MAX_INTERVAL = properties.getLong(KEY_MAX_INTERVAL, 31536000000L);
                                break;
                            case 3:
                                int i = properties.getInt(KEY_ALLOW_WHILE_IDLE_QUOTA, 72);
                                this.ALLOW_WHILE_IDLE_QUOTA = i;
                                if (i <= 0) {
                                    Slog.w(AlarmManagerService.TAG, "Must have positive allow_while_idle quota");
                                    this.ALLOW_WHILE_IDLE_QUOTA = 1;
                                    break;
                                }
                                break;
                            case 4:
                                this.MIN_WINDOW = properties.getLong(KEY_MIN_WINDOW, 600000L);
                                break;
                            case 5:
                                int i2 = properties.getInt(KEY_ALLOW_WHILE_IDLE_COMPAT_QUOTA, 1);
                                this.ALLOW_WHILE_IDLE_COMPAT_QUOTA = i2;
                                if (i2 <= 0) {
                                    Slog.w(AlarmManagerService.TAG, "Must have positive allow_while_idle_compat quota");
                                    this.ALLOW_WHILE_IDLE_COMPAT_QUOTA = 1;
                                    break;
                                }
                                break;
                            case 6:
                                long j = properties.getLong(KEY_ALLOW_WHILE_IDLE_WINDOW, 3600000L);
                                this.ALLOW_WHILE_IDLE_WINDOW = j;
                                if (j > 3600000) {
                                    Slog.w(AlarmManagerService.TAG, "Cannot have allow_while_idle_window > 3600000");
                                    this.ALLOW_WHILE_IDLE_WINDOW = 3600000L;
                                    break;
                                } else if (j != 3600000) {
                                    Slog.w(AlarmManagerService.TAG, "Using a non-default allow_while_idle_window = " + this.ALLOW_WHILE_IDLE_WINDOW);
                                    break;
                                }
                                break;
                            case 7:
                                long j2 = properties.getLong(KEY_ALLOW_WHILE_IDLE_COMPAT_WINDOW, 540000L);
                                this.ALLOW_WHILE_IDLE_COMPAT_WINDOW = j2;
                                if (j2 > 3600000) {
                                    Slog.w(AlarmManagerService.TAG, "Cannot have allow_while_idle_compat_window > 3600000");
                                    this.ALLOW_WHILE_IDLE_COMPAT_WINDOW = 3600000L;
                                    break;
                                } else if (j2 != 540000) {
                                    Slog.w(AlarmManagerService.TAG, "Using a non-default allow_while_idle_compat_window = " + this.ALLOW_WHILE_IDLE_COMPAT_WINDOW);
                                    break;
                                }
                                break;
                            case '\b':
                                this.ALLOW_WHILE_IDLE_WHITELIST_DURATION = properties.getLong(KEY_ALLOW_WHILE_IDLE_WHITELIST_DURATION, 10000L);
                                updateAllowWhileIdleWhitelistDurationLocked();
                                break;
                            case '\t':
                                this.LISTENER_TIMEOUT = properties.getLong(KEY_LISTENER_TIMEOUT, 5000L);
                                break;
                            case '\n':
                                int i3 = properties.getInt(KEY_MAX_ALARMS_PER_UID, 500);
                                this.MAX_ALARMS_PER_UID = i3;
                                if (i3 < 500) {
                                    Slog.w(AlarmManagerService.TAG, "Cannot set max_alarms_per_uid lower than 500");
                                    this.MAX_ALARMS_PER_UID = 500;
                                    break;
                                }
                                break;
                            case 11:
                            case '\f':
                                updateStandbyWindowsLocked();
                                break;
                            case '\r':
                                boolean oldLazyBatching = this.LAZY_BATCHING;
                                boolean z = properties.getBoolean(KEY_LAZY_BATCHING, true);
                                this.LAZY_BATCHING = z;
                                if (oldLazyBatching != z) {
                                    migrateAlarmsToNewStoreLocked();
                                    break;
                                }
                                break;
                            case 14:
                                this.TIME_TICK_ALLOWED_WHILE_IDLE = properties.getBoolean(KEY_TIME_TICK_ALLOWED_WHILE_IDLE, true);
                                break;
                            case 15:
                                this.CRASH_NON_CLOCK_APPS = properties.getBoolean(KEY_CRASH_NON_CLOCK_APPS, true);
                                break;
                            case 16:
                                this.PRIORITY_ALARM_DELAY = properties.getLong(KEY_PRIORITY_ALARM_DELAY, 540000L);
                                break;
                            case 17:
                                String rawValue = properties.getString(KEY_EXACT_ALARM_DENY_LIST, "");
                                if (rawValue.isEmpty()) {
                                    values = EmptyArray.STRING;
                                } else {
                                    values = rawValue.split(",", 251);
                                }
                                if (values.length > 250) {
                                    Slog.w(AlarmManagerService.TAG, "Deny list too long, truncating to 250 elements.");
                                    updateExactAlarmDenyList((String[]) Arrays.copyOf(values, 250));
                                    break;
                                } else {
                                    updateExactAlarmDenyList(values);
                                    break;
                                }
                            case 18:
                            case 19:
                                if (!deviceIdleFuzzBoundariesUpdated) {
                                    updateDeviceIdleFuzzBoundaries();
                                    deviceIdleFuzzBoundariesUpdated = true;
                                    break;
                                }
                                break;
                            case 20:
                                this.KILL_ON_SCHEDULE_EXACT_ALARM_REVOKED = properties.getBoolean(KEY_KILL_ON_SCHEDULE_EXACT_ALARM_REVOKED, true);
                                break;
                            case 21:
                                this.TEMPORARY_QUOTA_BUMP = properties.getInt(KEY_TEMPORARY_QUOTA_BUMP, 0);
                                break;
                            default:
                                if (name.startsWith(KEY_PREFIX_STANDBY_QUOTA) && !standbyQuotaUpdated) {
                                    updateStandbyQuotasLocked();
                                    standbyQuotaUpdated = true;
                                    break;
                                }
                                break;
                        }
                    }
                }
            }
        }

        @Override // com.android.server.tare.EconomyManagerInternal.TareStateChangeListener
        public void onTareEnabledStateChanged(boolean isTareEnabled) {
            updateTareSettings(isTareEnabled);
        }

        private void updateTareSettings(boolean isTareEnabled) {
            synchronized (AlarmManagerService.this.mLock) {
                if (this.USE_TARE_POLICY != isTareEnabled) {
                    this.USE_TARE_POLICY = isTareEnabled;
                    boolean changed = AlarmManagerService.this.mAlarmStore.updateAlarmDeliveries(new AlarmStore.AlarmDeliveryCalculator() { // from class: com.android.server.alarm.AlarmManagerService$Constants$$ExternalSyntheticLambda0
                        @Override // com.android.server.alarm.AlarmStore.AlarmDeliveryCalculator
                        public final boolean updateAlarmDelivery(Alarm alarm) {
                            return AlarmManagerService.Constants.this.m956x48a08537(alarm);
                        }
                    });
                    if (!this.USE_TARE_POLICY) {
                        AlarmManagerService.this.mAffordabilityCache.clear();
                    }
                    if (changed) {
                        AlarmManagerService.this.rescheduleKernelAlarmsLocked();
                        AlarmManagerService.this.updateNextAlarmClockLocked();
                    }
                }
            }
        }

        /* JADX INFO: Access modifiers changed from: package-private */
        /* renamed from: lambda$updateTareSettings$0$com-android-server-alarm-AlarmManagerService$Constants  reason: not valid java name */
        public /* synthetic */ boolean m956x48a08537(Alarm alarm) {
            boolean standbyChanged = AlarmManagerService.this.adjustDeliveryTimeBasedOnBucketLocked(alarm);
            boolean tareChanged = AlarmManagerService.this.adjustDeliveryTimeBasedOnTareLocked(alarm);
            if (this.USE_TARE_POLICY) {
                AlarmManagerService.this.registerTareListener(alarm);
            } else {
                AlarmManagerService.this.mEconomyManagerInternal.unregisterAffordabilityChangeListener(UserHandle.getUserId(alarm.uid), alarm.sourcePackage, AlarmManagerService.this.mAffordabilityChangeListener, TareBill.getAppropriateBill(alarm));
            }
            return standbyChanged || tareChanged;
        }

        private void updateExactAlarmDenyList(String[] newDenyList) {
            Set<String> newSet = Collections.unmodifiableSet(new ArraySet(newDenyList));
            Set<String> removed = new ArraySet<>(this.EXACT_ALARM_DENY_LIST);
            Set<String> added = new ArraySet<>(newDenyList);
            added.removeAll(this.EXACT_ALARM_DENY_LIST);
            removed.removeAll(newSet);
            if (added.size() > 0) {
                AlarmManagerService.this.mHandler.obtainMessage(9, added).sendToTarget();
            }
            if (removed.size() > 0) {
                AlarmManagerService.this.mHandler.obtainMessage(10, removed).sendToTarget();
            }
            if (newDenyList.length == 0) {
                this.EXACT_ALARM_DENY_LIST = Collections.emptySet();
            } else {
                this.EXACT_ALARM_DENY_LIST = newSet;
            }
        }

        private void migrateAlarmsToNewStoreLocked() {
            AlarmStore newStore = this.LAZY_BATCHING ? new LazyAlarmStore() : new BatchingAlarmStore();
            ArrayList<Alarm> allAlarms = AlarmManagerService.this.mAlarmStore.remove(new Predicate() { // from class: com.android.server.alarm.AlarmManagerService$Constants$$ExternalSyntheticLambda1
                @Override // java.util.function.Predicate
                public final boolean test(Object obj) {
                    return AlarmManagerService.Constants.lambda$migrateAlarmsToNewStoreLocked$1((Alarm) obj);
                }
            });
            newStore.addAll(allAlarms);
            AlarmManagerService.this.mAlarmStore = newStore;
            AlarmManagerService.this.mAlarmStore.setAlarmClockRemovalListener(AlarmManagerService.this.mAlarmClockUpdater);
        }

        /* JADX INFO: Access modifiers changed from: package-private */
        public static /* synthetic */ boolean lambda$migrateAlarmsToNewStoreLocked$1(Alarm unused) {
            return true;
        }

        private void updateDeviceIdleFuzzBoundaries() {
            DeviceConfig.Properties properties = DeviceConfig.getProperties("alarm_manager", new String[]{KEY_MIN_DEVICE_IDLE_FUZZ, KEY_MAX_DEVICE_IDLE_FUZZ});
            this.MIN_DEVICE_IDLE_FUZZ = properties.getLong(KEY_MIN_DEVICE_IDLE_FUZZ, 120000L);
            long j = properties.getLong(KEY_MAX_DEVICE_IDLE_FUZZ, (long) DEFAULT_MAX_DEVICE_IDLE_FUZZ);
            this.MAX_DEVICE_IDLE_FUZZ = j;
            if (j < this.MIN_DEVICE_IDLE_FUZZ) {
                Slog.w(AlarmManagerService.TAG, "max_device_idle_fuzz cannot be smaller than min_device_idle_fuzz! Increasing to " + this.MIN_DEVICE_IDLE_FUZZ);
                this.MAX_DEVICE_IDLE_FUZZ = this.MIN_DEVICE_IDLE_FUZZ;
            }
        }

        private void updateStandbyQuotasLocked() {
            DeviceConfig.Properties properties = DeviceConfig.getProperties("alarm_manager", this.KEYS_APP_STANDBY_QUOTAS);
            this.APP_STANDBY_QUOTAS[0] = properties.getInt(this.KEYS_APP_STANDBY_QUOTAS[0], this.DEFAULT_APP_STANDBY_QUOTAS[0]);
            int i = 1;
            while (true) {
                String[] strArr = this.KEYS_APP_STANDBY_QUOTAS;
                if (i < strArr.length) {
                    int[] iArr = this.APP_STANDBY_QUOTAS;
                    iArr[i] = properties.getInt(strArr[i], Math.min(iArr[i - 1], this.DEFAULT_APP_STANDBY_QUOTAS[i]));
                    i++;
                } else {
                    this.APP_STANDBY_RESTRICTED_QUOTA = Math.max(1, DeviceConfig.getInt("alarm_manager", KEY_APP_STANDBY_RESTRICTED_QUOTA, 1));
                    return;
                }
            }
        }

        private void updateStandbyWindowsLocked() {
            DeviceConfig.Properties properties = DeviceConfig.getProperties("alarm_manager", new String[]{KEY_APP_STANDBY_WINDOW, KEY_APP_STANDBY_RESTRICTED_WINDOW});
            long j = properties.getLong(KEY_APP_STANDBY_WINDOW, 3600000L);
            this.APP_STANDBY_WINDOW = j;
            if (j > 3600000) {
                Slog.w(AlarmManagerService.TAG, "Cannot exceed the app_standby_window size of 3600000");
                this.APP_STANDBY_WINDOW = 3600000L;
            } else if (j < 3600000) {
                Slog.w(AlarmManagerService.TAG, "Using a non-default app_standby_window of " + this.APP_STANDBY_WINDOW);
            }
            this.APP_STANDBY_RESTRICTED_WINDOW = Math.max(this.APP_STANDBY_WINDOW, properties.getLong(KEY_APP_STANDBY_RESTRICTED_WINDOW, 86400000L));
        }

        void dump(IndentingPrintWriter pw) {
            pw.println("Settings:");
            pw.increaseIndent();
            pw.print("version", Integer.valueOf(this.mVersion));
            pw.println();
            pw.print(KEY_MIN_FUTURITY);
            pw.print("=");
            TimeUtils.formatDuration(this.MIN_FUTURITY, pw);
            pw.println();
            pw.print(KEY_MIN_INTERVAL);
            pw.print("=");
            TimeUtils.formatDuration(this.MIN_INTERVAL, pw);
            pw.println();
            pw.print(KEY_MAX_INTERVAL);
            pw.print("=");
            TimeUtils.formatDuration(this.MAX_INTERVAL, pw);
            pw.println();
            pw.print(KEY_MIN_WINDOW);
            pw.print("=");
            TimeUtils.formatDuration(this.MIN_WINDOW, pw);
            pw.println();
            pw.print(KEY_LISTENER_TIMEOUT);
            pw.print("=");
            TimeUtils.formatDuration(this.LISTENER_TIMEOUT, pw);
            pw.println();
            pw.print(KEY_ALLOW_WHILE_IDLE_QUOTA, Integer.valueOf(this.ALLOW_WHILE_IDLE_QUOTA));
            pw.println();
            pw.print(KEY_ALLOW_WHILE_IDLE_WINDOW);
            pw.print("=");
            TimeUtils.formatDuration(this.ALLOW_WHILE_IDLE_WINDOW, pw);
            pw.println();
            pw.print(KEY_ALLOW_WHILE_IDLE_COMPAT_QUOTA, Integer.valueOf(this.ALLOW_WHILE_IDLE_COMPAT_QUOTA));
            pw.println();
            pw.print(KEY_ALLOW_WHILE_IDLE_COMPAT_WINDOW);
            pw.print("=");
            TimeUtils.formatDuration(this.ALLOW_WHILE_IDLE_COMPAT_WINDOW, pw);
            pw.println();
            pw.print(KEY_ALLOW_WHILE_IDLE_WHITELIST_DURATION);
            pw.print("=");
            TimeUtils.formatDuration(this.ALLOW_WHILE_IDLE_WHITELIST_DURATION, pw);
            pw.println();
            pw.print(KEY_MAX_ALARMS_PER_UID, Integer.valueOf(this.MAX_ALARMS_PER_UID));
            pw.println();
            pw.print(KEY_APP_STANDBY_WINDOW);
            pw.print("=");
            TimeUtils.formatDuration(this.APP_STANDBY_WINDOW, pw);
            pw.println();
            int i = 0;
            while (true) {
                String[] strArr = this.KEYS_APP_STANDBY_QUOTAS;
                if (i < strArr.length) {
                    pw.print(strArr[i], Integer.valueOf(this.APP_STANDBY_QUOTAS[i]));
                    pw.println();
                    i++;
                } else {
                    int i2 = this.APP_STANDBY_RESTRICTED_QUOTA;
                    pw.print(KEY_APP_STANDBY_RESTRICTED_QUOTA, Integer.valueOf(i2));
                    pw.println();
                    pw.print(KEY_APP_STANDBY_RESTRICTED_WINDOW);
                    pw.print("=");
                    TimeUtils.formatDuration(this.APP_STANDBY_RESTRICTED_WINDOW, pw);
                    pw.println();
                    pw.print(KEY_LAZY_BATCHING, Boolean.valueOf(this.LAZY_BATCHING));
                    pw.println();
                    pw.print(KEY_TIME_TICK_ALLOWED_WHILE_IDLE, Boolean.valueOf(this.TIME_TICK_ALLOWED_WHILE_IDLE));
                    pw.println();
                    pw.print(KEY_CRASH_NON_CLOCK_APPS, Boolean.valueOf(this.CRASH_NON_CLOCK_APPS));
                    pw.println();
                    pw.print(KEY_PRIORITY_ALARM_DELAY);
                    pw.print("=");
                    TimeUtils.formatDuration(this.PRIORITY_ALARM_DELAY, pw);
                    pw.println();
                    pw.print(KEY_EXACT_ALARM_DENY_LIST, this.EXACT_ALARM_DENY_LIST);
                    pw.println();
                    pw.print(KEY_MIN_DEVICE_IDLE_FUZZ);
                    pw.print("=");
                    TimeUtils.formatDuration(this.MIN_DEVICE_IDLE_FUZZ, pw);
                    pw.println();
                    pw.print(KEY_MAX_DEVICE_IDLE_FUZZ);
                    pw.print("=");
                    TimeUtils.formatDuration(this.MAX_DEVICE_IDLE_FUZZ, pw);
                    pw.println();
                    pw.print(KEY_KILL_ON_SCHEDULE_EXACT_ALARM_REVOKED, Boolean.valueOf(this.KILL_ON_SCHEDULE_EXACT_ALARM_REVOKED));
                    pw.println();
                    pw.print("enable_tare", Boolean.valueOf(this.USE_TARE_POLICY));
                    pw.println();
                    pw.print(KEY_TEMPORARY_QUOTA_BUMP, Integer.valueOf(this.TEMPORARY_QUOTA_BUMP));
                    pw.println();
                    pw.decreaseIndent();
                    return;
                }
            }
        }

        void dumpProto(ProtoOutputStream proto, long fieldId) {
            long token = proto.start(fieldId);
            proto.write(1112396529665L, this.MIN_FUTURITY);
            proto.write(1112396529666L, this.MIN_INTERVAL);
            proto.write(1112396529671L, this.MAX_INTERVAL);
            proto.write(1112396529667L, this.LISTENER_TIMEOUT);
            proto.write(1112396529670L, this.ALLOW_WHILE_IDLE_WHITELIST_DURATION);
            proto.end(token);
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* loaded from: classes.dex */
    public final class PriorityClass {
        int priority = 2;
        int seq;

        PriorityClass() {
            this.seq = AlarmManagerService.this.mCurrentSeq - 1;
        }
    }

    void calculateDeliveryPriorities(ArrayList<Alarm> alarms) {
        int alarmPrio;
        int N = alarms.size();
        for (int i = 0; i < N; i++) {
            Alarm a = alarms.get(i);
            if (a.listener == this.mTimeTickTrigger) {
                alarmPrio = 0;
            } else if (a.wakeup) {
                alarmPrio = 1;
            } else {
                alarmPrio = 2;
            }
            PriorityClass packagePrio = a.priorityClass;
            String alarmPackage = a.sourcePackage;
            if (packagePrio == null) {
                packagePrio = this.mPriorities.get(alarmPackage);
            }
            if (packagePrio == null) {
                PriorityClass priorityClass = new PriorityClass();
                a.priorityClass = priorityClass;
                packagePrio = priorityClass;
                this.mPriorities.put(alarmPackage, packagePrio);
            }
            a.priorityClass = packagePrio;
            if (packagePrio.seq != this.mCurrentSeq) {
                packagePrio.priority = alarmPrio;
                packagePrio.seq = this.mCurrentSeq;
            } else if (alarmPrio < packagePrio.priority) {
                packagePrio.priority = alarmPrio;
            }
        }
    }

    protected boolean isPowerOffAlarmType(int type) {
        return false;
    }

    protected boolean schedulePoweroffAlarm(int type, long triggerAtTime, long interval, PendingIntent operation, IAlarmListener directReceiver, String listenerTag, WorkSource workSource, AlarmManager.AlarmClockInfo alarmClock, String callingPackage) {
        return true;
    }

    protected void updatePoweroffAlarmtoNowRtc() {
    }

    public void cancelPoweroffAlarmImpl(String name) {
    }

    AlarmManagerService(Context context, Injector injector) {
        super(context);
        this.DUMP_ALARM_HPROF_INTERVAL = 120000L;
        this.mSystemApps = new ArrayList<>();
        this.mWhiteListPrefs = null;
        this.mWhiteListContext = null;
        this.DEEP_IDLE_TIME = 14400000L;
        this.mBackgroundIntent = new Intent().addFlags(4);
        this.mLog = new LocalLog(TAG);
        this.mLock = new Object();
        this.mExactAlarmCandidates = Collections.emptySet();
        this.mLastOpScheduleExactAlarm = new SparseIntArray();
        this.mAffordabilityCache = new SparseArrayMap<>();
        this.mPendingBackgroundAlarms = new SparseArray<>();
        this.mTickHistory = new long[10];
        this.mBroadcastRefCount = 0;
        this.mAlarmsPerUid = new SparseIntArray();
        this.mPendingNonWakeupAlarms = new ArrayList<>();
        this.mInFlight = new ArrayList<>();
        this.mInFlightListeners = new ArrayList<>();
        this.mLastPriorityAlarmDispatch = new SparseLongArray();
        this.mRemovalHistory = new SparseArray<>();
        this.mDeliveryTracker = new DeliveryTracker();
        this.mInteractive = true;
        this.mAllowWhileIdleDispatches = new ArrayList<>();
        this.mStatLogger = new StatLogger("Alarm manager stats", new String[]{"REORDER_ALARMS_FOR_STANDBY", "HAS_SCHEDULE_EXACT_ALARM", "REORDER_ALARMS_FOR_TARE"});
        this.mOptsWithFgs = BroadcastOptions.makeBasic();
        this.mOptsWithFgsForAlarmClock = BroadcastOptions.makeBasic();
        this.mOptsWithoutFgs = BroadcastOptions.makeBasic();
        this.mOptsTimeBroadcast = BroadcastOptions.makeBasic();
        this.mActivityOptsRestrictBal = ActivityOptions.makeBasic();
        this.mBroadcastOptsRestrictBal = BroadcastOptions.makeBasic();
        this.mNextAlarmClockForUser = new SparseArray<>();
        this.mTmpSparseAlarmClockArray = new SparseArray<>();
        this.mPendingSendNextAlarmClockChangedForUser = new SparseBooleanArray();
        this.mAlarmClockUpdater = new Runnable() { // from class: com.android.server.alarm.AlarmManagerService$$ExternalSyntheticLambda23
            @Override // java.lang.Runnable
            public final void run() {
                AlarmManagerService.this.m937lambda$new$0$comandroidserveralarmAlarmManagerService();
            }
        };
        this.mHandlerSparseAlarmClockArray = new SparseArray<>();
        this.mPriorities = new HashMap<>();
        this.mCurrentSeq = 0;
        this.mAlarmDispatchComparator = new Comparator<Alarm>() { // from class: com.android.server.alarm.AlarmManagerService.1
            /* JADX DEBUG: Method merged with bridge method */
            @Override // java.util.Comparator
            public int compare(Alarm lhs, Alarm rhs) {
                boolean lhsIdleUntil = (lhs.flags & 16) != 0;
                boolean rhsIdleUntil = (rhs.flags & 16) != 0;
                if (lhsIdleUntil != rhsIdleUntil) {
                    return lhsIdleUntil ? -1 : 1;
                } else if (lhs.priorityClass.priority < rhs.priorityClass.priority) {
                    return -1;
                } else {
                    if (lhs.priorityClass.priority > rhs.priorityClass.priority) {
                        return 1;
                    }
                    if (lhs.getRequestedElapsed() < rhs.getRequestedElapsed()) {
                        return -1;
                    }
                    return lhs.getRequestedElapsed() > rhs.getRequestedElapsed() ? 1 : 0;
                }
            }
        };
        this.mPendingWhileIdleAlarms = new ArrayList<>();
        this.mPendingIdleUntil = null;
        this.mNextWakeFromIdle = null;
        this.mBroadcastStats = new SparseArray<>();
        this.mNumDelayedAlarms = 0;
        this.mTotalDelayTime = 0L;
        this.mMaxDelayTime = 0L;
        this.mExceptList = Arrays.asList("com.transsion.alarmmanager");
        this.mService = new IAlarmManager.Stub() { // from class: com.android.server.alarm.AlarmManagerService.5
            public void set(String callingPackage, int type, long triggerAtTime, long windowLength, long interval, int flags, PendingIntent operation, IAlarmListener directReceiver, String listenerTag, WorkSource workSource, AlarmManager.AlarmClockInfo alarmClock) {
                long windowLength2;
                int flags2;
                long windowLength3;
                boolean needsPermission;
                boolean lowerQuota;
                Bundle idleOptions;
                int exactAllowReason;
                Bundle idleOptions2;
                Bundle bundle;
                int flags3;
                int callingUid = AlarmManagerService.this.mInjector.getCallingUid();
                int callingUserId = UserHandle.getUserId(callingUid);
                if (callingUid != AlarmManagerService.this.mPackageManagerInternal.getPackageUid(callingPackage, 0L, callingUserId)) {
                    throw new SecurityException("Package " + callingPackage + " does not belong to the calling uid " + callingUid);
                }
                if (interval != 0 && directReceiver != null) {
                    throw new IllegalArgumentException("Repeating alarms cannot use AlarmReceivers");
                }
                if (workSource != null) {
                    AlarmManagerService.this.getContext().enforcePermission("android.permission.UPDATE_DEVICE_STATS", Binder.getCallingPid(), callingUid, "AlarmManager.set");
                }
                if ((flags & 16) == 0) {
                    windowLength2 = windowLength;
                    flags2 = flags;
                } else if (callingUid != 1000) {
                    flags2 = flags & (-17);
                    windowLength2 = windowLength;
                } else {
                    windowLength2 = 0;
                    flags2 = flags;
                }
                int flags4 = flags2 & (-43);
                if (alarmClock != null) {
                    flags4 |= 2;
                    windowLength3 = 0;
                } else if (workSource == null && (UserHandle.isCore(callingUid) || UserHandle.isSameApp(callingUid, AlarmManagerService.this.mSystemUiUid) || (AlarmManagerService.this.mAppStateTracker != null && AlarmManagerService.this.mAppStateTracker.isUidPowerSaveUserExempt(callingUid)))) {
                    flags4 = (flags4 | 8) & (-69);
                    windowLength3 = windowLength2;
                } else {
                    windowLength3 = windowLength2;
                }
                boolean allowWhileIdle = (flags4 & 4) != 0;
                boolean exact = windowLength3 == 0;
                int exactAllowReason2 = -1;
                if ((flags4 & 64) != 0) {
                    AlarmManagerService.this.getContext().enforcePermission("android.permission.SCHEDULE_PRIORITIZED_ALARM", Binder.getCallingPid(), callingUid, "AlarmManager.setPrioritized");
                    flags4 &= -5;
                    exactAllowReason = -1;
                    idleOptions2 = null;
                } else if (!exact && !allowWhileIdle) {
                    exactAllowReason = -1;
                    idleOptions2 = null;
                } else {
                    if (AlarmManagerService.isExactAlarmChangeEnabled(callingPackage, callingUserId)) {
                        needsPermission = exact;
                        lowerQuota = exact ? false : true;
                        if (exact) {
                            if (alarmClock != null) {
                                bundle = AlarmManagerService.this.mOptsWithFgsForAlarmClock.toBundle();
                            } else {
                                bundle = AlarmManagerService.this.mOptsWithFgs.toBundle();
                            }
                            idleOptions = bundle;
                        } else {
                            idleOptions = AlarmManagerService.this.mOptsWithoutFgs.toBundle();
                        }
                    } else {
                        needsPermission = false;
                        lowerQuota = allowWhileIdle;
                        idleOptions = allowWhileIdle ? AlarmManagerService.this.mOptsWithFgs.toBundle() : null;
                        if (exact) {
                            exactAllowReason2 = 2;
                        }
                    }
                    if (needsPermission) {
                        if (AlarmManagerService.this.hasUseExactAlarmInternal(callingPackage, callingUid)) {
                            exactAllowReason2 = 3;
                        } else if (AlarmManagerService.this.hasScheduleExactAlarmInternal(callingPackage, callingUid)) {
                            exactAllowReason2 = 0;
                        } else {
                            if (AlarmManagerService.this.isExemptFromExactAlarmPermissionNoLock(callingUid)) {
                                exactAllowReason2 = 1;
                            } else {
                                String errorMessage = "Caller " + callingPackage + " needs to hold android.permission.SCHEDULE_EXACT_ALARM or android.permission.USE_EXACT_ALARM to set exact alarms.";
                                if (AlarmManagerService.this.mConstants.CRASH_NON_CLOCK_APPS) {
                                    throw new SecurityException(errorMessage);
                                }
                                Slog.wtf(AlarmManagerService.TAG, errorMessage);
                            }
                            idleOptions = allowWhileIdle ? AlarmManagerService.this.mOptsWithoutFgs.toBundle() : null;
                            lowerQuota = allowWhileIdle;
                        }
                    }
                    if (!lowerQuota) {
                        exactAllowReason = exactAllowReason2;
                        idleOptions2 = idleOptions;
                    } else {
                        flags4 = (flags4 & (-5)) | 32;
                        exactAllowReason = exactAllowReason2;
                        idleOptions2 = idleOptions;
                    }
                }
                if (!exact) {
                    flags3 = flags4;
                } else {
                    flags3 = flags4 | 1;
                }
                AlarmManagerService.this.setImpl(type, triggerAtTime, windowLength3, interval, operation, directReceiver, listenerTag, flags3, workSource, alarmClock, callingUid, callingPackage, idleOptions2, exactAllowReason);
            }

            public boolean canScheduleExactAlarms(String packageName) {
                int callingUid = AlarmManagerService.this.mInjector.getCallingUid();
                int userId = UserHandle.getUserId(callingUid);
                int packageUid = AlarmManagerService.this.mPackageManagerInternal.getPackageUid(packageName, 0L, userId);
                if (callingUid == packageUid) {
                    return !AlarmManagerService.isExactAlarmChangeEnabled(packageName, userId) || AlarmManagerService.this.isExemptFromExactAlarmPermissionNoLock(packageUid) || AlarmManagerService.this.hasScheduleExactAlarmInternal(packageName, packageUid) || AlarmManagerService.this.hasUseExactAlarmInternal(packageName, packageUid);
                }
                throw new SecurityException("Uid " + callingUid + " cannot query canScheduleExactAlarms for package " + packageName);
            }

            public boolean hasScheduleExactAlarm(String packageName, int userId) {
                int callingUid = AlarmManagerService.this.mInjector.getCallingUid();
                if (UserHandle.getUserId(callingUid) != userId) {
                    AlarmManagerService.this.getContext().enforceCallingOrSelfPermission("android.permission.INTERACT_ACROSS_USERS_FULL", "hasScheduleExactAlarm");
                }
                int uid = AlarmManagerService.this.mPackageManagerInternal.getPackageUid(packageName, 0L, userId);
                if (callingUid != uid && !UserHandle.isCore(callingUid)) {
                    throw new SecurityException("Uid " + callingUid + " cannot query hasScheduleExactAlarm for package " + packageName);
                }
                if (uid > 0) {
                    return AlarmManagerService.this.hasScheduleExactAlarmInternal(packageName, uid);
                }
                return false;
            }

            public boolean setTime(long millis) {
                AlarmManagerService.this.getContext().enforceCallingOrSelfPermission("android.permission.SET_TIME", "setTime");
                return AlarmManagerService.this.setTimeImpl(millis);
            }

            public void setTimeZone(String tz) {
                AlarmManagerService.this.getContext().enforceCallingOrSelfPermission("android.permission.SET_TIME_ZONE", "setTimeZone");
                long oldId = Binder.clearCallingIdentity();
                try {
                    AlarmManagerService.this.setTimeZoneImpl(tz);
                } finally {
                    Binder.restoreCallingIdentity(oldId);
                }
            }

            public void remove(PendingIntent operation, IAlarmListener listener) {
                if (operation == null && listener == null) {
                    Slog.w(AlarmManagerService.TAG, "remove() with no intent or listener");
                    return;
                }
                synchronized (AlarmManagerService.this.mLock) {
                    AlarmManagerService.this.removeLocked(operation, listener, 1);
                }
            }

            public long getNextWakeFromIdleTime() {
                return AlarmManagerService.this.getNextWakeFromIdleTimeImpl();
            }

            public void cancelPoweroffAlarm(String name) {
                AlarmManagerService.this.cancelPoweroffAlarmImpl(name);
            }

            public AlarmManager.AlarmClockInfo getNextAlarmClock(int userId) {
                return AlarmManagerService.this.getNextAlarmClockImpl(AlarmManagerService.this.mActivityManagerInternal.handleIncomingUser(Binder.getCallingPid(), Binder.getCallingUid(), userId, false, 0, "getNextAlarmClock", (String) null));
            }

            public long currentNetworkTimeMillis() {
                NtpTrustedTime time = NtpTrustedTime.getInstance(AlarmManagerService.this.getContext());
                NtpTrustedTime.TimeResult ntpResult = time.getCachedTimeResult();
                if (ntpResult != null) {
                    return ntpResult.currentTimeMillis();
                }
                throw new ParcelableException(new DateTimeException("Missing NTP fix"));
            }

            public int getConfigVersion() {
                AlarmManagerService.this.getContext().enforceCallingOrSelfPermission("android.permission.DUMP", "getConfigVersion");
                return AlarmManagerService.this.mConstants.getVersion();
            }

            protected void dump(FileDescriptor fd, PrintWriter pw, String[] args) {
                if (DumpUtils.checkDumpAndUsageStatsPermission(AlarmManagerService.this.getContext(), AlarmManagerService.TAG, pw)) {
                    if (args.length > 0 && "--proto".equals(args[0])) {
                        AlarmManagerService.this.dumpProto(fd);
                    } else if (args.length > 0 && "mSystemApps".equals(args[0])) {
                        AlarmManagerService.this.dumpSystemFlagApps(pw);
                    } else if (args.length > 0 && "alarmmanagerOn".equals(args[0])) {
                        AlarmManagerService.this.dumpAlarmManagerOnInfo(pw);
                    } else if (args.length > 0 && "alarmmanagerOff".equals(args[0])) {
                        AlarmManagerService.this.dumpAlarmManagerOffInfo(pw);
                    } else if (args.length > 0 && "alarmmanagerdebugOn".equals(args[0])) {
                        AlarmManagerService.this.dumpAlarmManagerDebug(pw);
                    } else {
                        AlarmManagerService.this.dumpImpl(new IndentingPrintWriter(pw, "  "));
                    }
                }
            }

            /* JADX DEBUG: Multi-variable search result rejected for r8v0, resolved type: com.android.server.alarm.AlarmManagerService$5 */
            /* JADX WARN: Multi-variable type inference failed */
            public void onShellCommand(FileDescriptor in, FileDescriptor out, FileDescriptor err, String[] args, ShellCallback callback, ResultReceiver resultReceiver) {
                new ShellCmd().exec(this, in, out, err, args, callback, resultReceiver);
            }
        };
        this.mScreenOffTime = 0L;
        this.mScreenOff = false;
        this.screenOffThreshold = 120000L;
        this.mAffordabilityChangeListener = new EconomyManagerInternal.AffordabilityChangeListener() { // from class: com.android.server.alarm.AlarmManagerService.8
            @Override // com.android.server.tare.EconomyManagerInternal.AffordabilityChangeListener
            public void onAffordabilityChanged(int userId, String packageName, EconomyManagerInternal.ActionBill bill, boolean canAfford) {
                if (AlarmManagerService.DEBUG_TARE) {
                    Slog.d(AlarmManagerService.TAG, userId + ":" + packageName + " affordability for " + TareBill.getName(bill) + " changed to " + canAfford);
                }
                ArrayMap<EconomyManagerInternal.ActionBill, Boolean> actionAffordability = (ArrayMap) AlarmManagerService.this.mAffordabilityCache.get(userId, packageName);
                if (actionAffordability == null) {
                    actionAffordability = new ArrayMap<>();
                    AlarmManagerService.this.mAffordabilityCache.add(userId, packageName, actionAffordability);
                }
                actionAffordability.put(bill, Boolean.valueOf(canAfford));
                AlarmManagerService.this.mHandler.obtainMessage(12, userId, canAfford ? 1 : 0, packageName).sendToTarget();
            }
        };
        this.mForceAppStandbyListener = new AnonymousClass9();
        this.mSendCount = 0;
        this.mSendFinishCount = 0;
        this.mListenerCount = 0;
        this.mListenerFinishCount = 0;
        this.mInjector = injector;
        this.mEconomyManagerInternal = (EconomyManagerInternal) LocalServices.getService(EconomyManagerInternal.class);
    }

    public AlarmManagerService(Context context) {
        this(context, new Injector(context));
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static boolean isRtc(int type) {
        return type == 1 || type == 0;
    }

    private long convertToElapsed(long when, int type) {
        if (isRtc(type)) {
            return when - (this.mInjector.getCurrentTimeMillis() - this.mInjector.getElapsedRealtime());
        }
        return when;
    }

    long getMinimumAllowedWindow(long nowElapsed, long triggerElapsed) {
        long futurity = triggerElapsed - nowElapsed;
        return Math.min((long) (futurity * 0.75d), this.mConstants.MIN_WINDOW);
    }

    static long maxTriggerTime(long now, long triggerAtTime, long interval) {
        long futurity;
        if (interval == 0) {
            futurity = triggerAtTime - now;
        } else {
            futurity = interval;
        }
        if (futurity < 10000) {
            futurity = 0;
        }
        long maxElapsed = ((long) (futurity * 0.75d)) + triggerAtTime;
        if (interval == 0) {
            maxElapsed = Math.min(maxElapsed, 3600000 + triggerAtTime);
        }
        return clampPositive(maxElapsed);
    }

    void reevaluateRtcAlarms() {
        Alarm alarm;
        synchronized (this.mLock) {
            boolean changed = this.mAlarmStore.updateAlarmDeliveries(new AlarmStore.AlarmDeliveryCalculator() { // from class: com.android.server.alarm.AlarmManagerService$$ExternalSyntheticLambda20
                @Override // com.android.server.alarm.AlarmStore.AlarmDeliveryCalculator
                public final boolean updateAlarmDelivery(Alarm alarm2) {
                    return AlarmManagerService.this.m940xf80ae1fa(alarm2);
                }
            });
            if (changed && this.mPendingIdleUntil != null && (alarm = this.mNextWakeFromIdle) != null && isRtc(alarm.type)) {
                boolean idleUntilUpdated = this.mAlarmStore.updateAlarmDeliveries(new AlarmStore.AlarmDeliveryCalculator() { // from class: com.android.server.alarm.AlarmManagerService$$ExternalSyntheticLambda21
                    @Override // com.android.server.alarm.AlarmStore.AlarmDeliveryCalculator
                    public final boolean updateAlarmDelivery(Alarm alarm2) {
                        return AlarmManagerService.this.m941xb1826f99(alarm2);
                    }
                });
                if (idleUntilUpdated) {
                    this.mAlarmStore.updateAlarmDeliveries(new AlarmStore.AlarmDeliveryCalculator() { // from class: com.android.server.alarm.AlarmManagerService$$ExternalSyntheticLambda22
                        @Override // com.android.server.alarm.AlarmStore.AlarmDeliveryCalculator
                        public final boolean updateAlarmDelivery(Alarm alarm2) {
                            return AlarmManagerService.this.m942x6af9fd38(alarm2);
                        }
                    });
                }
            }
            if (changed) {
                rescheduleKernelAlarmsLocked();
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$reevaluateRtcAlarms$1$com-android-server-alarm-AlarmManagerService  reason: not valid java name */
    public /* synthetic */ boolean m940xf80ae1fa(Alarm a) {
        if (!isRtc(a.type)) {
            return false;
        }
        return restoreRequestedTime(a);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$reevaluateRtcAlarms$2$com-android-server-alarm-AlarmManagerService  reason: not valid java name */
    public /* synthetic */ boolean m941xb1826f99(Alarm a) {
        return a == this.mPendingIdleUntil && adjustIdleUntilTime(a);
    }

    boolean reorderAlarmsBasedOnStandbyBuckets(final ArraySet<Pair<String, Integer>> targetPackages) {
        long start = this.mStatLogger.getTime();
        boolean changed = this.mAlarmStore.updateAlarmDeliveries(new AlarmStore.AlarmDeliveryCalculator() { // from class: com.android.server.alarm.AlarmManagerService$$ExternalSyntheticLambda5
            @Override // com.android.server.alarm.AlarmStore.AlarmDeliveryCalculator
            public final boolean updateAlarmDelivery(Alarm alarm) {
                return AlarmManagerService.this.m946xa831ecc3(targetPackages, alarm);
            }
        });
        this.mStatLogger.logDurationStat(0, start);
        return changed;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$reorderAlarmsBasedOnStandbyBuckets$4$com-android-server-alarm-AlarmManagerService  reason: not valid java name */
    public /* synthetic */ boolean m946xa831ecc3(ArraySet targetPackages, Alarm a) {
        Pair<String, Integer> packageUser = Pair.create(a.sourcePackage, Integer.valueOf(UserHandle.getUserId(a.creatorUid)));
        if (targetPackages != null && !targetPackages.contains(packageUser)) {
            return false;
        }
        return adjustDeliveryTimeBasedOnBucketLocked(a);
    }

    boolean reorderAlarmsBasedOnTare(final ArraySet<Pair<String, Integer>> targetPackages) {
        long start = this.mStatLogger.getTime();
        boolean changed = this.mAlarmStore.updateAlarmDeliveries(new AlarmStore.AlarmDeliveryCalculator() { // from class: com.android.server.alarm.AlarmManagerService$$ExternalSyntheticLambda10
            @Override // com.android.server.alarm.AlarmStore.AlarmDeliveryCalculator
            public final boolean updateAlarmDelivery(Alarm alarm) {
                return AlarmManagerService.this.m947xf5f0425e(targetPackages, alarm);
            }
        });
        this.mStatLogger.logDurationStat(2, start);
        return changed;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$reorderAlarmsBasedOnTare$5$com-android-server-alarm-AlarmManagerService  reason: not valid java name */
    public /* synthetic */ boolean m947xf5f0425e(ArraySet targetPackages, Alarm a) {
        Pair<String, Integer> packageUser = Pair.create(a.sourcePackage, Integer.valueOf(UserHandle.getUserId(a.creatorUid)));
        if (targetPackages != null && !targetPackages.contains(packageUser)) {
            return false;
        }
        return adjustDeliveryTimeBasedOnTareLocked(a);
    }

    private boolean restoreRequestedTime(Alarm a) {
        return a.setPolicyElapsed(0, convertToElapsed(a.origWhen, a.type));
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static long clampPositive(long val) {
        return val >= 0 ? val : JobStatus.NO_LATEST_RUNTIME;
    }

    void sendPendingBackgroundAlarmsLocked(int uid, String packageName) {
        ArrayList<Alarm> alarmsToDeliver;
        ArrayList<Alarm> alarmsForUid = this.mPendingBackgroundAlarms.get(uid);
        if (alarmsForUid == null || alarmsForUid.size() == 0) {
            return;
        }
        if (packageName != null) {
            if (DEBUG_BG_LIMIT) {
                Slog.d(TAG, "Sending blocked alarms for uid " + uid + ", package " + packageName);
            }
            alarmsToDeliver = new ArrayList<>();
            for (int i = alarmsForUid.size() - 1; i >= 0; i--) {
                Alarm a = alarmsForUid.get(i);
                if (a.matches(packageName)) {
                    alarmsToDeliver.add(alarmsForUid.remove(i));
                }
            }
            int i2 = alarmsForUid.size();
            if (i2 == 0) {
                this.mPendingBackgroundAlarms.remove(uid);
            }
        } else {
            if (DEBUG_BG_LIMIT) {
                Slog.d(TAG, "Sending blocked alarms for uid " + uid);
            }
            alarmsToDeliver = alarmsForUid;
            this.mPendingBackgroundAlarms.remove(uid);
        }
        deliverPendingBackgroundAlarmsLocked(alarmsToDeliver, this.mInjector.getElapsedRealtime());
    }

    void sendAllUnrestrictedPendingBackgroundAlarmsLocked() {
        ArrayList<Alarm> alarmsToDeliver = new ArrayList<>();
        findAllUnrestrictedPendingBackgroundAlarmsLockedInner(this.mPendingBackgroundAlarms, alarmsToDeliver, new Predicate() { // from class: com.android.server.alarm.AlarmManagerService$$ExternalSyntheticLambda0
            @Override // java.util.function.Predicate
            public final boolean test(Object obj) {
                boolean isBackgroundRestricted;
                isBackgroundRestricted = AlarmManagerService.this.isBackgroundRestricted((Alarm) obj);
                return isBackgroundRestricted;
            }
        });
        if (alarmsToDeliver.size() > 0) {
            deliverPendingBackgroundAlarmsLocked(alarmsToDeliver, this.mInjector.getElapsedRealtime());
        }
    }

    static void findAllUnrestrictedPendingBackgroundAlarmsLockedInner(SparseArray<ArrayList<Alarm>> pendingAlarms, ArrayList<Alarm> unrestrictedAlarms, Predicate<Alarm> isBackgroundRestricted) {
        for (int uidIndex = pendingAlarms.size() - 1; uidIndex >= 0; uidIndex--) {
            ArrayList<Alarm> alarmsForUid = pendingAlarms.valueAt(uidIndex);
            for (int alarmIndex = alarmsForUid.size() - 1; alarmIndex >= 0; alarmIndex--) {
                Alarm alarm = alarmsForUid.get(alarmIndex);
                if (!isBackgroundRestricted.test(alarm)) {
                    unrestrictedAlarms.add(alarm);
                    alarmsForUid.remove(alarmIndex);
                }
            }
            int alarmIndex2 = alarmsForUid.size();
            if (alarmIndex2 == 0) {
                pendingAlarms.removeAt(uidIndex);
            }
        }
    }

    private void deliverPendingBackgroundAlarmsLocked(ArrayList<Alarm> alarms, long nowELAPSED) {
        AlarmManagerService alarmManagerService;
        ArrayList<Alarm> arrayList;
        long j;
        boolean hasWakeup;
        int i;
        int N;
        AlarmManagerService alarmManagerService2 = this;
        ArrayList<Alarm> arrayList2 = alarms;
        long j2 = nowELAPSED;
        int N2 = alarms.size();
        boolean hasWakeup2 = false;
        int i2 = 0;
        while (i2 < N2) {
            Alarm alarm = arrayList2.get(i2);
            if (!alarm.wakeup) {
                hasWakeup = hasWakeup2;
            } else {
                hasWakeup = true;
            }
            alarm.count = 1;
            if (alarm.repeatInterval <= 0) {
                i = i2;
                N = N2;
            } else {
                alarm.count = (int) (alarm.count + ((j2 - alarm.getRequestedElapsed()) / alarm.repeatInterval));
                long delta = alarm.count * alarm.repeatInterval;
                long nextElapsed = alarm.getRequestedElapsed() + delta;
                long nextMaxElapsed = maxTriggerTime(nowELAPSED, nextElapsed, alarm.repeatInterval);
                i = i2;
                N = N2;
                setImplLocked(alarm.type, alarm.origWhen + delta, nextElapsed, nextMaxElapsed - nextElapsed, alarm.repeatInterval, alarm.operation, null, null, alarm.flags, alarm.workSource, alarm.alarmClock, alarm.uid, alarm.packageName, null, -1);
            }
            i2 = i + 1;
            alarmManagerService2 = this;
            arrayList2 = alarms;
            j2 = nowELAPSED;
            hasWakeup2 = hasWakeup;
            N2 = N;
        }
        if (hasWakeup2) {
            alarmManagerService = this;
            arrayList = alarms;
            j = nowELAPSED;
        } else {
            alarmManagerService = this;
            j = nowELAPSED;
            if (!alarmManagerService.checkAllowNonWakeupDelayLocked(j)) {
                arrayList = alarms;
            } else {
                if (alarmManagerService.mPendingNonWakeupAlarms.size() == 0) {
                    alarmManagerService.mStartCurrentDelayTime = j;
                    alarmManagerService.mNextNonWakeupDeliveryTime = ((alarmManagerService.currentNonWakeupFuzzLocked(j) * 3) / 2) + j;
                }
                alarmManagerService.mPendingNonWakeupAlarms.addAll(alarms);
                alarmManagerService.mNumDelayedAlarms += alarms.size();
                return;
            }
        }
        if (DEBUG_BG_LIMIT) {
            Slog.d(TAG, "Waking up to deliver pending blocked alarms");
        }
        if (alarmManagerService.mPendingNonWakeupAlarms.size() > 0) {
            arrayList.addAll(alarmManagerService.mPendingNonWakeupAlarms);
            long thisDelayTime = j - alarmManagerService.mStartCurrentDelayTime;
            alarmManagerService.mTotalDelayTime += thisDelayTime;
            if (alarmManagerService.mMaxDelayTime < thisDelayTime) {
                alarmManagerService.mMaxDelayTime = thisDelayTime;
            }
            alarmManagerService.mPendingNonWakeupAlarms.clear();
        }
        calculateDeliveryPriorities(alarms);
        Collections.sort(arrayList, alarmManagerService.mAlarmDispatchComparator);
        deliverAlarmsLocked(alarms, nowELAPSED);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* loaded from: classes.dex */
    public static final class InFlight {
        final int mAlarmType;
        final BroadcastStats mBroadcastStats;
        final int mCreatorUid;
        final FilterStats mFilterStats;
        final IBinder mListener;
        final PendingIntent mPendingIntent;
        final String mTag;
        final int mUid;
        final long mWhenElapsed;
        final WorkSource mWorkSource;

        InFlight(AlarmManagerService service, Alarm alarm, long nowELAPSED) {
            BroadcastStats statsLocked;
            this.mPendingIntent = alarm.operation;
            this.mWhenElapsed = nowELAPSED;
            this.mListener = alarm.listener != null ? alarm.listener.asBinder() : null;
            this.mWorkSource = alarm.workSource;
            this.mUid = alarm.uid;
            this.mCreatorUid = alarm.creatorUid;
            String str = alarm.statsTag;
            this.mTag = str;
            if (alarm.operation != null) {
                statsLocked = service.getStatsLocked(alarm.operation);
            } else {
                statsLocked = service.getStatsLocked(alarm.uid, alarm.packageName);
            }
            this.mBroadcastStats = statsLocked;
            FilterStats fs = statsLocked.filterStats.get(str);
            if (fs == null) {
                fs = new FilterStats(statsLocked, str);
                statsLocked.filterStats.put(str, fs);
            }
            fs.lastTime = nowELAPSED;
            this.mFilterStats = fs;
            this.mAlarmType = alarm.type;
        }

        boolean isBroadcast() {
            PendingIntent pendingIntent = this.mPendingIntent;
            return pendingIntent != null && pendingIntent.isBroadcast();
        }

        public String toString() {
            return "InFlight{pendingIntent=" + this.mPendingIntent + ", when=" + this.mWhenElapsed + ", workSource=" + this.mWorkSource + ", uid=" + this.mUid + ", creatorUid=" + this.mCreatorUid + ", tag=" + this.mTag + ", broadcastStats=" + this.mBroadcastStats + ", filterStats=" + this.mFilterStats + ", alarmType=" + this.mAlarmType + "}";
        }

        public void dumpDebug(ProtoOutputStream proto, long fieldId) {
            long token = proto.start(fieldId);
            proto.write(CompanionMessage.MESSAGE_ID, this.mUid);
            proto.write(1138166333442L, this.mTag);
            proto.write(1112396529667L, this.mWhenElapsed);
            proto.write(1159641169924L, this.mAlarmType);
            PendingIntent pendingIntent = this.mPendingIntent;
            if (pendingIntent != null) {
                pendingIntent.dumpDebug(proto, 1146756268037L);
            }
            BroadcastStats broadcastStats = this.mBroadcastStats;
            if (broadcastStats != null) {
                broadcastStats.dumpDebug(proto, 1146756268038L);
            }
            FilterStats filterStats = this.mFilterStats;
            if (filterStats != null) {
                filterStats.dumpDebug(proto, 1146756268039L);
            }
            WorkSource workSource = this.mWorkSource;
            if (workSource != null) {
                workSource.dumpDebug(proto, 1146756268040L);
            }
            proto.end(token);
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void notifyBroadcastAlarmPendingLocked(int uid) {
        int numListeners = this.mInFlightListeners.size();
        for (int i = 0; i < numListeners; i++) {
            this.mInFlightListeners.get(i).broadcastAlarmPending(uid);
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void notifyBroadcastAlarmCompleteLocked(int uid) {
        int numListeners = this.mInFlightListeners.size();
        for (int i = 0; i < numListeners; i++) {
            this.mInFlightListeners.get(i).broadcastAlarmComplete(uid);
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* loaded from: classes.dex */
    public static final class FilterStats {
        long aggregateTime;
        int count;
        long lastTime;
        final BroadcastStats mBroadcastStats;
        final String mTag;
        int nesting;
        int numWakeup;
        long startTime;

        FilterStats(BroadcastStats broadcastStats, String tag) {
            this.mBroadcastStats = broadcastStats;
            this.mTag = tag;
        }

        public String toString() {
            return "FilterStats{tag=" + this.mTag + ", lastTime=" + this.lastTime + ", aggregateTime=" + this.aggregateTime + ", count=" + this.count + ", numWakeup=" + this.numWakeup + ", startTime=" + this.startTime + ", nesting=" + this.nesting + "}";
        }

        public void dumpDebug(ProtoOutputStream proto, long fieldId) {
            long token = proto.start(fieldId);
            proto.write(CompanionAppsPermissions.AppPermissions.PACKAGE_NAME, this.mTag);
            proto.write(1112396529666L, this.lastTime);
            proto.write(1112396529667L, this.aggregateTime);
            proto.write(1120986464260L, this.count);
            proto.write(1120986464261L, this.numWakeup);
            proto.write(1112396529670L, this.startTime);
            proto.write(1120986464263L, this.nesting);
            proto.end(token);
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* loaded from: classes.dex */
    public static final class BroadcastStats {
        long aggregateTime;
        int count;
        final ArrayMap<String, FilterStats> filterStats = new ArrayMap<>();
        final String mPackageName;
        final int mUid;
        int nesting;
        int numWakeup;
        long startTime;

        BroadcastStats(int uid, String packageName) {
            this.mUid = uid;
            this.mPackageName = packageName;
        }

        public String toString() {
            return "BroadcastStats{uid=" + this.mUid + ", packageName=" + this.mPackageName + ", aggregateTime=" + this.aggregateTime + ", count=" + this.count + ", numWakeup=" + this.numWakeup + ", startTime=" + this.startTime + ", nesting=" + this.nesting + "}";
        }

        public void dumpDebug(ProtoOutputStream proto, long fieldId) {
            long token = proto.start(fieldId);
            proto.write(CompanionMessage.MESSAGE_ID, this.mUid);
            proto.write(1138166333442L, this.mPackageName);
            proto.write(1112396529667L, this.aggregateTime);
            proto.write(1120986464260L, this.count);
            proto.write(1120986464261L, this.numWakeup);
            proto.write(1112396529670L, this.startTime);
            proto.write(1120986464263L, this.nesting);
            proto.end(token);
        }
    }

    @Override // com.android.server.SystemService
    public void onStart() {
        this.mInjector.init();
        this.mOptsWithFgs.setPendingIntentBackgroundActivityLaunchAllowed(false);
        this.mOptsWithFgsForAlarmClock.setPendingIntentBackgroundActivityLaunchAllowed(false);
        this.mOptsWithoutFgs.setPendingIntentBackgroundActivityLaunchAllowed(false);
        this.mOptsTimeBroadcast.setPendingIntentBackgroundActivityLaunchAllowed(false);
        this.mActivityOptsRestrictBal.setPendingIntentBackgroundActivityLaunchAllowed(false);
        this.mBroadcastOptsRestrictBal.setPendingIntentBackgroundActivityLaunchAllowed(false);
        this.mMetricsHelper = new MetricsHelper(getContext(), this.mLock);
        this.mActivityManagerInternal = (ActivityManagerInternal) LocalServices.getService(ActivityManagerInternal.class);
        this.mListenerDeathRecipient = new IBinder.DeathRecipient() { // from class: com.android.server.alarm.AlarmManagerService.2
            @Override // android.os.IBinder.DeathRecipient
            public void binderDied() {
            }

            public void binderDied(IBinder who) {
                IAlarmListener listener = IAlarmListener.Stub.asInterface(who);
                AlarmManagerService.this.removeImpl(null, listener);
            }
        };
        synchronized (this.mLock) {
            AlarmHandler alarmHandler = new AlarmHandler();
            this.mHandler = alarmHandler;
            Constants constants = new Constants(alarmHandler);
            this.mConstants = constants;
            AlarmStore lazyAlarmStore = constants.LAZY_BATCHING ? new LazyAlarmStore() : new BatchingAlarmStore();
            this.mAlarmStore = lazyAlarmStore;
            lazyAlarmStore.setAlarmClockRemovalListener(this.mAlarmClockUpdater);
            this.mAppWakeupHistory = new AppWakeupHistory(3600000L);
            this.mAllowWhileIdleHistory = new AppWakeupHistory(3600000L);
            this.mAllowWhileIdleCompatHistory = new AppWakeupHistory(3600000L);
            this.mTemporaryQuotaReserve = new TemporaryQuotaReserve(86400000L);
            this.mNextNonWakeup = 0L;
            this.mNextWakeup = 0L;
            setTimeZoneImpl(SystemProperties.get(TIMEZONE_PROPERTY));
            long systemBuildTime = Long.max(SystemProperties.getLong("ro.build.date.utc", -1L) * 1000, Long.max(Environment.getRootDirectory().lastModified(), Build.TIME));
            if (this.mInjector.getCurrentTimeMillis() < systemBuildTime) {
                Slog.i(TAG, "Current time only " + this.mInjector.getCurrentTimeMillis() + ", advancing to build time " + systemBuildTime);
                this.mInjector.setKernelTime(systemBuildTime);
            }
            PackageManagerInternal packageManagerInternal = (PackageManagerInternal) LocalServices.getService(PackageManagerInternal.class);
            this.mPackageManagerInternal = packageManagerInternal;
            int systemUiUid = this.mInjector.getSystemUiUid(packageManagerInternal);
            this.mSystemUiUid = systemUiUid;
            if (systemUiUid <= 0) {
                Slog.wtf(TAG, "SysUI package not found!");
            }
            this.mWakeLock = this.mInjector.getAlarmWakeLock();
            this.mTimeTickIntent = new Intent("android.intent.action.TIME_TICK").addFlags(1344274432);
            this.mTimeTickTrigger = new AnonymousClass3();
            Intent intent = new Intent("android.intent.action.DATE_CHANGED");
            intent.addFlags(538968064);
            this.mDateChangeSender = PendingIntent.getBroadcastAsUser(getContext(), 0, intent, 67108864, UserHandle.ALL);
            this.mClockReceiver = this.mInjector.getClockReceiver(this);
            new ChargingReceiver();
            new InteractiveStateReceiver();
            new UninstallReceiver();
            if (sTranSlmSupport) {
                new NetworkReceiver();
            }
            if (this.mInjector.isAlarmDriverPresent()) {
                AlarmThread waitThread = new AlarmThread();
                waitThread.start();
            } else {
                Slog.w(TAG, "Failed to open alarm driver. Falling back to a handler.");
            }
        }
        this.mLastDumpAlarmHprofTime = System.currentTimeMillis();
        publishLocalService(AlarmManagerInternal.class, new LocalService());
        publishBinderService("alarm", this.mService);
        if (sTranSlmSupport) {
            initSystemFlagApps();
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: com.android.server.alarm.AlarmManagerService$3  reason: invalid class name */
    /* loaded from: classes.dex */
    public class AnonymousClass3 extends IAlarmListener.Stub {
        AnonymousClass3() {
        }

        public void doAlarm(final IAlarmCompleteListener callback) throws RemoteException {
            if (AlarmManagerService.DEBUG_BATCH) {
                Slog.v(AlarmManagerService.TAG, "Received TIME_TICK alarm; rescheduling");
            }
            AlarmManagerService.this.mHandler.post(new Runnable() { // from class: com.android.server.alarm.AlarmManagerService$3$$ExternalSyntheticLambda0
                @Override // java.lang.Runnable
                public final void run() {
                    AlarmManagerService.AnonymousClass3.this.m953lambda$doAlarm$0$comandroidserveralarmAlarmManagerService$3(callback);
                }
            });
            synchronized (AlarmManagerService.this.mLock) {
                AlarmManagerService alarmManagerService = AlarmManagerService.this;
                alarmManagerService.mLastTickReceived = alarmManagerService.mInjector.getCurrentTimeMillis();
            }
            AlarmManagerService.this.mClockReceiver.scheduleTimeTickEvent();
        }

        /* JADX DEBUG: Multi-variable search result rejected for r3v0, resolved type: com.android.server.alarm.AlarmManagerService$3 */
        /* JADX INFO: Access modifiers changed from: package-private */
        /* JADX WARN: Multi-variable type inference failed */
        /* renamed from: lambda$doAlarm$0$com-android-server-alarm-AlarmManagerService$3  reason: not valid java name */
        public /* synthetic */ void m953lambda$doAlarm$0$comandroidserveralarmAlarmManagerService$3(IAlarmCompleteListener callback) {
            AlarmManagerService.this.getContext().sendBroadcastAsUser(AlarmManagerService.this.mTimeTickIntent, UserHandle.ALL);
            try {
                callback.alarmComplete(this);
            } catch (RemoteException e) {
            }
        }
    }

    void refreshExactAlarmCandidates() {
        String[] candidates = this.mLocalPermissionManager.getAppOpPermissionPackages("android.permission.SCHEDULE_EXACT_ALARM");
        Set<Integer> newAppIds = new ArraySet<>(candidates.length);
        for (String candidate : candidates) {
            int uid = this.mPackageManagerInternal.getPackageUid(candidate, 4194304L, 0);
            if (uid > 0) {
                newAppIds.add(Integer.valueOf(UserHandle.getAppId(uid)));
            }
        }
        this.mExactAlarmCandidates = Collections.unmodifiableSet(newAppIds);
    }

    @Override // com.android.server.SystemService
    public void onUserStarting(SystemService.TargetUser user) {
        super.onUserStarting(user);
        final int userId = user.getUserIdentifier();
        this.mHandler.post(new Runnable() { // from class: com.android.server.alarm.AlarmManagerService$$ExternalSyntheticLambda24
            @Override // java.lang.Runnable
            public final void run() {
                AlarmManagerService.this.m939x91b3b288(userId);
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$onUserStarting$6$com-android-server-alarm-AlarmManagerService  reason: not valid java name */
    public /* synthetic */ void m939x91b3b288(int userId) {
        for (Integer num : this.mExactAlarmCandidates) {
            int appId = num.intValue();
            int uid = UserHandle.getUid(userId, appId);
            AndroidPackage androidPackage = this.mPackageManagerInternal.getPackage(uid);
            if (androidPackage != null) {
                int mode = this.mAppOps.checkOpNoThrow(107, uid, androidPackage.getPackageName());
                synchronized (this.mLock) {
                    this.mLastOpScheduleExactAlarm.put(uid, mode);
                }
            }
        }
    }

    @Override // com.android.server.SystemService
    public void onBootPhase(int phase) {
        if (phase == 500) {
            synchronized (this.mLock) {
                this.mConstants.start();
                this.mAppOps = (AppOpsManager) getContext().getSystemService("appops");
                this.mLocalDeviceIdleController = (DeviceIdleInternal) LocalServices.getService(DeviceIdleInternal.class);
                this.mUsageStatsManagerInternal = (UsageStatsManagerInternal) LocalServices.getService(UsageStatsManagerInternal.class);
                AppStateTrackerImpl appStateTrackerImpl = (AppStateTrackerImpl) LocalServices.getService(AppStateTracker.class);
                this.mAppStateTracker = appStateTrackerImpl;
                appStateTrackerImpl.addListener(this.mForceAppStandbyListener);
                BatteryManager bm = (BatteryManager) getContext().getSystemService(BatteryManager.class);
                this.mAppStandbyParole = bm.isCharging();
                this.mClockReceiver.scheduleTimeTickEvent();
                this.mClockReceiver.scheduleDateChangedEvent();
            }
            IAppOpsService iAppOpsService = this.mInjector.getAppOpsService();
            try {
                iAppOpsService.startWatchingMode(107, (String) null, new IAppOpsCallback.Stub() { // from class: com.android.server.alarm.AlarmManagerService.4
                    public void opChanged(int op, int uid, String packageName) throws RemoteException {
                        int oldMode;
                        boolean hadPermission;
                        int userId = UserHandle.getUserId(uid);
                        if (op == 107 && AlarmManagerService.isExactAlarmChangeEnabled(packageName, userId) && !AlarmManagerService.this.hasUseExactAlarmInternal(packageName, uid) && AlarmManagerService.this.mExactAlarmCandidates.contains(Integer.valueOf(UserHandle.getAppId(uid)))) {
                            int newMode = AlarmManagerService.this.mAppOps.checkOpNoThrow(107, uid, packageName);
                            synchronized (AlarmManagerService.this.mLock) {
                                int index = AlarmManagerService.this.mLastOpScheduleExactAlarm.indexOfKey(uid);
                                if (index < 0) {
                                    oldMode = AppOpsManager.opToDefaultMode(107);
                                    AlarmManagerService.this.mLastOpScheduleExactAlarm.put(uid, newMode);
                                } else {
                                    oldMode = AlarmManagerService.this.mLastOpScheduleExactAlarm.valueAt(index);
                                    AlarmManagerService.this.mLastOpScheduleExactAlarm.setValueAt(index, newMode);
                                }
                            }
                            if (oldMode == newMode) {
                                return;
                            }
                            boolean allowedByDefault = AlarmManagerService.this.isScheduleExactAlarmAllowedByDefault(packageName, uid);
                            boolean hasPermission = true;
                            if (oldMode != 3) {
                                hadPermission = oldMode == 0;
                            } else {
                                hadPermission = allowedByDefault;
                            }
                            if (newMode != 3) {
                                if (newMode != 0) {
                                    hasPermission = false;
                                }
                            } else {
                                hasPermission = allowedByDefault;
                            }
                            if (hadPermission && !hasPermission) {
                                AlarmManagerService.this.mHandler.obtainMessage(8, uid, 0, packageName).sendToTarget();
                            } else if (!hadPermission && hasPermission) {
                                AlarmManagerService.this.sendScheduleExactAlarmPermissionStateChangedBroadcast(packageName, userId);
                            }
                        }
                    }
                });
            } catch (RemoteException e) {
            }
            this.mLocalPermissionManager = (PermissionManagerServiceInternal) LocalServices.getService(PermissionManagerServiceInternal.class);
            refreshExactAlarmCandidates();
            AppStandbyInternal appStandbyInternal = (AppStandbyInternal) LocalServices.getService(AppStandbyInternal.class);
            appStandbyInternal.addListener(new AppStandbyTracker());
            this.mRoleManager = (RoleManager) getContext().getSystemService(RoleManager.class);
            this.mMetricsHelper.registerPuller(new Supplier() { // from class: com.android.server.alarm.AlarmManagerService$$ExternalSyntheticLambda25
                @Override // java.util.function.Supplier
                public final Object get() {
                    return AlarmManagerService.this.m938x4c3c7d61();
                }
            });
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$onBootPhase$7$com-android-server-alarm-AlarmManagerService  reason: not valid java name */
    public /* synthetic */ AlarmStore m938x4c3c7d61() {
        return this.mAlarmStore;
    }

    protected void finalize() throws Throwable {
        try {
            this.mInjector.close();
        } finally {
            super.finalize();
        }
    }

    boolean setTimeImpl(long millis) {
        if (!this.mInjector.isAlarmDriverPresent()) {
            Slog.w(TAG, "Not setting time since no alarm driver is available.");
            return false;
        }
        synchronized (this.mLock) {
            long currentTimeMillis = this.mInjector.getCurrentTimeMillis();
            this.mInjector.setKernelTime(millis);
            TimeZone timeZone = TimeZone.getDefault();
            int currentTzOffset = timeZone.getOffset(currentTimeMillis);
            int newTzOffset = timeZone.getOffset(millis);
            if (currentTzOffset != newTzOffset) {
                Slog.i(TAG, "Timezone offset has changed, updating kernel timezone");
                this.mInjector.setKernelTimezone(-(newTzOffset / 60000));
            }
        }
        return true;
    }

    void setTimeZoneImpl(String tz) {
        if (TextUtils.isEmpty(tz)) {
            return;
        }
        TimeZone zone = TimeZone.getTimeZone(tz);
        boolean timeZoneWasChanged = false;
        synchronized (this) {
            String current = SystemProperties.get(TIMEZONE_PROPERTY);
            if (current == null || !current.equals(zone.getID())) {
                if (localLOGV) {
                    Slog.v(TAG, "timezone changed: " + current + ", new=" + zone.getID());
                }
                timeZoneWasChanged = true;
                SystemProperties.set(TIMEZONE_PROPERTY, zone.getID());
            }
            int gmtOffset = zone.getOffset(this.mInjector.getCurrentTimeMillis());
            this.mInjector.setKernelTimezone(-(gmtOffset / 60000));
        }
        TimeZone.setDefault(null);
        if (timeZoneWasChanged) {
            this.mClockReceiver.scheduleDateChangedEvent();
            Intent intent = new Intent("android.intent.action.TIMEZONE_CHANGED");
            intent.addFlags(622854144);
            intent.putExtra("time-zone", zone.getID());
            this.mOptsTimeBroadcast.setTemporaryAppAllowlist(this.mActivityManagerInternal.getBootTimeTempAllowListDuration(), 0, 204, "");
            getContext().sendBroadcastAsUser(intent, UserHandle.ALL, null, this.mOptsTimeBroadcast.toBundle());
        }
    }

    void removeImpl(PendingIntent operation, IAlarmListener listener) {
        synchronized (this.mLock) {
            removeLocked(operation, listener, 0);
        }
    }

    private void initSystemFlagApps() {
        try {
            IPackageManager pm = AppGlobals.getPackageManager();
            ParceledListSlice<ApplicationInfo> slice = null;
            if (pm != null) {
                slice = AppGlobals.getPackageManager().getInstalledApplications(0L, getContext().getUserId());
            }
            if (slice != null) {
                List<ApplicationInfo> allAppInfoList = slice.getList();
                for (ApplicationInfo appInfo : allAppInfoList) {
                    if ((appInfo.flags & 1) == 1 || (appInfo.flags & 8) == 8) {
                        if (appInfo.packageName != null) {
                            this.mSystemApps.add(appInfo.packageName);
                        }
                    }
                }
            }
        } catch (RemoteException e) {
            Slog.e(TAG, "getInstalledApplications err");
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void dumpSystemFlagApps(PrintWriter pw) {
        Iterator<String> it = this.mSystemApps.iterator();
        while (it.hasNext()) {
            String app = it.next();
            pw.println(" " + app);
        }
    }

    private boolean isThirdPackageByName(String name) {
        ArrayList<String> arrayList;
        if (name != null && (arrayList = this.mSystemApps) != null && !arrayList.contains(name)) {
            if (sAlarmDebug) {
                Slog.d(TAG, "isThirdPackageByName name=" + name + ", return true");
                return true;
            }
            return true;
        }
        return false;
    }

    private boolean isExcept(String packageName) {
        if (packageName == null || !this.mExceptList.contains(packageName)) {
            return false;
        }
        return true;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void dumpAlarmManagerOnInfo(PrintWriter pw) {
        sTranSlmSupport = true;
        pw.println("sTranSlmSupport :" + sTranSlmSupport);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void dumpAlarmManagerOffInfo(PrintWriter pw) {
        sTranSlmSupport = false;
        pw.println("sTranSlmSupport :" + sTranSlmSupport);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void dumpAlarmManagerDebug(PrintWriter pw) {
        sAlarmDebug = true;
        pw.println("sAlarmDebug :" + sAlarmDebug);
    }

    private boolean getSyncHeartBeatState() {
        return Settings.Global.getInt(getContext().getContentResolver(), "sync_heartbeat_enable", 1) != 0;
    }

    private boolean isAlarmNeedReset(String name, int type, long windowLength) {
        return getSyncHeartBeatState() && isThirdPackageByName(name) && !hasPackageNameInWhiteList(name) && !isExcept(name) && windowLength == 0 && (type == 0 || type == 2);
    }

    private boolean hasPackageNameInWhiteList(String name) {
        boolean isExit = false;
        boolean isCustomExit = false;
        Set<String> customWhiteListitems = null;
        try {
            if (this.mWhiteListContext == null) {
                this.mWhiteListContext = getContext().createPackageContext("com.android.settings", 2);
            }
            Context context = this.mWhiteListContext;
            if (context != null && this.mWhiteListPrefs == null) {
                this.mWhiteListPrefs = context.getSharedPreferences("sync_heartbeat", 0);
            }
            SharedPreferences sharedPreferences = this.mWhiteListPrefs;
            if (sharedPreferences != null) {
                customWhiteListitems = sharedPreferences.getStringSet("white_list", null);
            }
            if (customWhiteListitems != null) {
                isCustomExit = customWhiteListitems.contains(name);
            }
            isExit = isCustomExit;
            if (sAlarmDebug) {
                Slog.d(TAG, "pkgname =" + name + "is in whitelist");
            }
        } catch (Exception e) {
            Log.d(TAG, "hasPackageNameInWhiteList except");
        }
        return isExit;
    }

    /* JADX DEBUG: Don't trust debug lines info. Repeating lines: [2568=4] */
    /* JADX WARN: Not initialized variable reg: 20, insn: 0x0388: MOVE  (r21 I:??[long, double]) = (r20 I:??[long, double] A[D('triggerAtTime' long)]), block:B:118:0x0376 */
    /* JADX WARN: Removed duplicated region for block: B:154:0x025b A[EXC_TOP_SPLITTER, SYNTHETIC] */
    /* JADX WARN: Removed duplicated region for block: B:68:0x019a  */
    /* JADX WARN: Removed duplicated region for block: B:69:0x01a9  */
    /*
        Code decompiled incorrectly, please refer to instructions dump.
    */
    void setImpl(int type, long triggerAtTime, long windowLength, long interval, PendingIntent operation, IAlarmListener directReceiver, String listenerTag, int flags, WorkSource workSource, AlarmManager.AlarmClockInfo alarmClock, int callingUid, String callingPackage, Bundle idleOptions, int exactAllowReason) {
        long interval2;
        long what;
        long minTrigger;
        long triggerAtTime2;
        long windowLength2;
        PendingIntent pendingIntent;
        int flags2;
        long nominalTrigger;
        int i;
        long nowElapsed;
        long nominalTrigger2;
        long nowElapsed2;
        Object obj;
        Object obj2;
        long maxElapsed;
        long maxElapsed2;
        long triggerAtTime3;
        long interval3 = interval;
        if ((operation == null && directReceiver == null) || (operation != null && directReceiver != null)) {
            Slog.w(TAG, "Alarms must either supply a PendingIntent or an AlarmReceiver");
            return;
        }
        if (directReceiver != null) {
            try {
                directReceiver.asBinder().linkToDeath(this.mListenerDeathRecipient, 0);
            } catch (RemoteException e) {
                Slog.w(TAG, "Dropping unreachable alarm listener " + listenerTag);
                return;
            }
        }
        long minInterval = this.mConstants.MIN_INTERVAL;
        if (interval3 > 0 && interval3 < minInterval) {
            Slog.w(TAG, "Suspiciously short interval " + interval3 + " millis; expanding to " + (minInterval / 1000) + " seconds");
            interval3 = minInterval;
        } else if (interval3 > this.mConstants.MAX_INTERVAL) {
            Slog.w(TAG, "Suspiciously long interval " + interval3 + " millis; clamping");
            interval3 = this.mConstants.MAX_INTERVAL;
        }
        if ((type < 0 || type > 3) && !isPowerOffAlarmType(type)) {
            throw new IllegalArgumentException("Invalid alarm type " + type);
        }
        if (triggerAtTime < 0) {
            long what2 = Binder.getCallingPid();
            interval2 = interval3;
            Slog.w(TAG, "Invalid alarm trigger time! " + triggerAtTime + " from uid=" + callingUid + " pid=" + what2);
            what = 0;
        } else {
            interval2 = interval3;
            what = triggerAtTime;
        }
        long interval4 = interval2;
        long triggerAtTime4 = what;
        if (!schedulePoweroffAlarm(type, what, interval4, operation, directReceiver, listenerTag, workSource, alarmClock, callingPackage)) {
            return;
        }
        int type2 = isPowerOffAlarmType(type) ? 0 : type;
        long nowElapsed3 = this.mInjector.getElapsedRealtime();
        long nominalTrigger3 = convertToElapsed(triggerAtTime4, type2);
        long minTrigger2 = nowElapsed3 + (UserHandle.isCore(callingUid) ? 0L : this.mConstants.MIN_FUTURITY);
        long triggerElapsed = Math.max(minTrigger2, nominalTrigger3);
        if (!sTranSlmSupport) {
            minTrigger = minTrigger2;
            triggerAtTime2 = triggerAtTime4;
            windowLength2 = windowLength;
            pendingIntent = operation;
        } else if (this.mScreenOff) {
            triggerAtTime2 = triggerAtTime4;
            pendingIntent = operation;
            if (pendingIntent != null) {
                String packageName = operation.getTargetPackage();
                if (packageName == null) {
                    minTrigger = minTrigger2;
                    windowLength2 = windowLength;
                } else if (!getSyncHeartBeatState()) {
                    minTrigger = minTrigger2;
                    windowLength2 = windowLength;
                } else if (packageName != null) {
                    minTrigger = minTrigger2;
                    windowLength2 = windowLength;
                    if (isAlarmNeedReset(packageName, type2, windowLength2)) {
                        windowLength2 = -1;
                        flags2 = 0;
                        if (windowLength2 != 0) {
                            i = callingUid;
                            nominalTrigger = nominalTrigger3;
                            nominalTrigger2 = triggerElapsed;
                            nowElapsed = nowElapsed3;
                            nowElapsed2 = windowLength2;
                        } else if (windowLength2 < 0) {
                            long maxElapsed3 = maxTriggerTime(nowElapsed3, triggerElapsed, interval4);
                            long windowLength3 = maxElapsed3 - triggerElapsed;
                            i = callingUid;
                            nominalTrigger = nominalTrigger3;
                            nominalTrigger2 = maxElapsed3;
                            nowElapsed = nowElapsed3;
                            nowElapsed2 = windowLength3;
                        } else {
                            nominalTrigger = nominalTrigger3;
                            long minAllowedWindow = getMinimumAllowedWindow(nowElapsed3, triggerElapsed);
                            if (windowLength2 > 86400000) {
                                Slog.w(TAG, "Window length " + windowLength2 + "ms too long; limiting to 1 day");
                                windowLength2 = 86400000;
                                i = callingUid;
                                nowElapsed = nowElapsed3;
                            } else if ((flags2 & 64) != 0 || windowLength2 >= minAllowedWindow) {
                                i = callingUid;
                                nowElapsed = nowElapsed3;
                            } else {
                                i = callingUid;
                                if (isExemptFromMinWindowRestrictions(i)) {
                                    nowElapsed = nowElapsed3;
                                } else {
                                    nowElapsed = nowElapsed3;
                                    if (CompatChanges.isChangeEnabled(185199076L, callingPackage, UserHandle.getUserHandleForUid(callingUid))) {
                                        Slog.w(TAG, "Window length " + windowLength2 + "ms too short; expanding to " + minAllowedWindow + "ms.");
                                        windowLength2 = minAllowedWindow;
                                    }
                                }
                            }
                            nominalTrigger2 = triggerElapsed + windowLength2;
                            nowElapsed2 = windowLength2;
                        }
                        obj = this.mLock;
                        synchronized (obj) {
                            try {
                                try {
                                    if (DEBUG_BATCH) {
                                        try {
                                            StringBuilder append = new StringBuilder().append("set(").append(pendingIntent).append(") : type=").append(type2).append(" triggerAtTime=").append(triggerAtTime2).append(" win=").append(nowElapsed2).append(" tElapsed=").append(triggerElapsed).append(" maxElapsed=").append(nominalTrigger2).append(" interval=");
                                            maxElapsed = nominalTrigger2;
                                            maxElapsed2 = interval4;
                                            try {
                                                Slog.v(TAG, append.append(maxElapsed2).append(" flags=0x").append(Integer.toHexString(flags2)).toString());
                                            } catch (Throwable th) {
                                                th = th;
                                                obj2 = obj;
                                                throw th;
                                            }
                                        } catch (Throwable th2) {
                                            th = th2;
                                            obj2 = obj;
                                            throw th;
                                        }
                                    } else {
                                        maxElapsed = nominalTrigger2;
                                        maxElapsed2 = interval4;
                                    }
                                    try {
                                        if (this.mAlarmsPerUid.get(i, 0) >= this.mConstants.MAX_ALARMS_PER_UID) {
                                            try {
                                                try {
                                                    String errorMsg = "Maximum limit of concurrent alarms " + this.mConstants.MAX_ALARMS_PER_UID + " reached for uid: " + UserHandle.formatUid(callingUid) + ", callingPackage: " + callingPackage;
                                                    Slog.w(TAG, errorMsg);
                                                    if (UserHandle.isCore(callingUid) && Build.IS_DEBUG_ENABLE) {
                                                        dumpHprof(i);
                                                    }
                                                    try {
                                                        if (i != 1000) {
                                                            throw new IllegalStateException(errorMsg);
                                                        }
                                                        triggerAtTime3 = triggerAtTime2;
                                                        EventLog.writeEvent(1397638484, "234441463", -1, errorMsg);
                                                    } catch (Throwable th3) {
                                                        th = th3;
                                                        obj2 = obj;
                                                        throw th;
                                                    }
                                                } catch (Throwable th4) {
                                                    th = th4;
                                                    obj2 = obj;
                                                    throw th;
                                                }
                                            } catch (Throwable th5) {
                                                th = th5;
                                            }
                                        } else {
                                            triggerAtTime3 = triggerAtTime2;
                                        }
                                        long interval5 = maxElapsed2;
                                        long interval6 = nowElapsed2;
                                        setImplLocked(type2, triggerAtTime3, triggerElapsed, interval6, interval5, operation, directReceiver, listenerTag, flags2, workSource, alarmClock, callingUid, callingPackage, idleOptions, exactAllowReason);
                                    } catch (Throwable th6) {
                                        th = th6;
                                        obj2 = obj;
                                    }
                                } catch (Throwable th7) {
                                    th = th7;
                                    obj2 = obj;
                                }
                            } catch (Throwable th8) {
                                th = th8;
                            }
                        }
                        return;
                    }
                } else {
                    minTrigger = minTrigger2;
                    windowLength2 = windowLength;
                }
            } else {
                minTrigger = minTrigger2;
                windowLength2 = windowLength;
            }
        } else {
            minTrigger = minTrigger2;
            triggerAtTime2 = triggerAtTime4;
            windowLength2 = windowLength;
            pendingIntent = operation;
        }
        flags2 = flags;
        if (windowLength2 != 0) {
        }
        obj = this.mLock;
        synchronized (obj) {
        }
    }

    public void dumpHprof(int callingUid) {
        String path = String.format("/data/hprof/SYSTEM_ALARM_%d.hprof", Integer.valueOf(callingUid));
        File alarmHprofFile = new File(path);
        long currentTime = System.currentTimeMillis();
        if (alarmHprofFile.exists() && currentTime - this.mLastDumpAlarmHprofTime < 120000) {
            return;
        }
        Slog.d(TAG, "dump hprof when system_server alarm begin in dumpHprof.");
        try {
            Debug.dumpHprofData(path);
        } catch (IOException e) {
            Slog.e(TAG, "dump hprof when system_server alarm fail in dumpHprof.", e);
        } catch (RuntimeException e2) {
            Slog.e(TAG, "Failed to dump hprof:", e2);
        }
        Slog.d(TAG, "dump hprof when system_server alarm end in dumpHprof");
        this.mLastDumpAlarmHprofTime = currentTime;
    }

    private boolean ifRemoveGmsAlarm(String packageName) {
        if (packageName == null || !this.mScreenOff || this.mNetworkConnected) {
            return false;
        }
        if (packageName.contains("com.google")) {
            if (sAlarmDebug) {
                Slog.d(TAG, "Skip gms alarm, packagename =" + packageName);
            }
            return true;
        }
        if (packageName.contains("facebook") || packageName.contains("com.android.vending")) {
            if (sAlarmDebug) {
                Slog.d(TAG, "Skip facebook/vending alarm, packagename =" + packageName);
            }
            return true;
        }
        return false;
    }

    private boolean isGmsApplication(Alarm a) {
        if (a == null || a.packageName == null || a.statsTag == null) {
            return false;
        }
        if (!a.packageName.contains("com.google") && !a.statsTag.contains("com.google") && !isSpecialGmsApplication(a.packageName, a.statsTag)) {
            return false;
        }
        return true;
    }

    private boolean isSpecialGmsApplication(String pkgname, String tag) {
        if (pkgname.contains("com.facebook") || tag.contains("com.facebook") || pkgname.contains("com.android.vending") || tag.contains("com.android.vending")) {
            return true;
        }
        return false;
    }

    private boolean isIdleFlag(Alarm a) {
        if (a == null || (a.flags & 2) != 0 || (a.flags & 4) != 0 || (a.flags & 8) != 0) {
            return true;
        }
        return false;
    }

    private boolean isClockAlarm(Alarm a) {
        return a.packageName == null || a.packageName.contains("clock") || a.packageName.contains("Clock") || a.statsTag == null || a.statsTag.contains("clock") || a.statsTag.contains("Clock");
    }

    private boolean isAndroidAlarm(Alarm a) {
        return a.packageName == null || a.packageName.contains(PackageManagerService.PLATFORM_PACKAGE_NAME) || a.statsTag == null || a.statsTag.contains(PackageManagerService.PLATFORM_PACKAGE_NAME);
    }

    private void setImplLocked(int type, long when, long whenElapsed, long windowLength, long interval, PendingIntent operation, IAlarmListener directReceiver, String listenerTag, int flags, WorkSource workSource, AlarmManager.AlarmClockInfo alarmClock, int callingUid, String callingPackage, Bundle idleOptions, int exactAllowReason) {
        Alarm a = new Alarm(type, when, whenElapsed, windowLength, interval, operation, directReceiver, listenerTag, workSource, flags, alarmClock, callingUid, callingPackage, idleOptions, exactAllowReason);
        if (this.mActivityManagerInternal.isAppStartModeDisabled(callingUid, callingPackage)) {
            Slog.w(TAG, "Not setting alarm from " + callingUid + ":" + a + " -- package not allowed to start");
            return;
        }
        if (sAlarmDebug) {
            Slog.d(TAG, "packageName :" + a.packageName + "tag :" + a.flags);
        }
        int callerProcState = this.mActivityManagerInternal.getUidProcessState(callingUid);
        removeLocked(operation, directReceiver, 0);
        if (sTranSlmSupport && operation != null) {
            String packageName = operation.getTargetPackage();
            if (ifRemoveGmsAlarm(packageName)) {
                return;
            }
        }
        incrementAlarmCount(a.uid);
        setImplLocked(a);
        MetricsHelper.pushAlarmScheduled(a, callerProcState);
    }

    int getQuotaForBucketLocked(int bucket) {
        int index;
        if (bucket <= 10) {
            index = 0;
        } else if (bucket <= 20) {
            index = 1;
        } else if (bucket <= 30) {
            index = 2;
        } else if (bucket < 50) {
            index = 3;
        } else {
            index = 4;
        }
        return this.mConstants.APP_STANDBY_QUOTAS[index];
    }

    private boolean adjustIdleUntilTime(Alarm alarm) {
        if ((alarm.flags & 16) == 0) {
            return false;
        }
        boolean changedBeforeFuzz = restoreRequestedTime(alarm);
        Alarm alarm2 = this.mNextWakeFromIdle;
        if (alarm2 == null) {
            return changedBeforeFuzz;
        }
        long upcomingWakeFromIdle = alarm2.getWhenElapsed();
        if (alarm.getWhenElapsed() < upcomingWakeFromIdle - this.mConstants.MIN_DEVICE_IDLE_FUZZ) {
            return changedBeforeFuzz;
        }
        long nowElapsed = this.mInjector.getElapsedRealtime();
        long futurity = upcomingWakeFromIdle - nowElapsed;
        if (futurity <= this.mConstants.MIN_DEVICE_IDLE_FUZZ) {
            alarm.setPolicyElapsed(0, nowElapsed);
            return true;
        }
        ThreadLocalRandom random = ThreadLocalRandom.current();
        long upperBoundExcl = Math.min(this.mConstants.MAX_DEVICE_IDLE_FUZZ, futurity) + 1;
        long fuzz = random.nextLong(this.mConstants.MIN_DEVICE_IDLE_FUZZ, upperBoundExcl);
        alarm.setPolicyElapsed(0, upcomingWakeFromIdle - fuzz);
        return true;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public boolean adjustDeliveryTimeBasedOnBatterySaver(Alarm alarm) {
        long batterySaverPolicyElapsed;
        int quota;
        long window;
        AppWakeupHistory history;
        long nowElapsed = this.mInjector.getElapsedRealtime();
        if (isExemptFromBatterySaver(alarm)) {
            return false;
        }
        AppStateTrackerImpl appStateTrackerImpl = this.mAppStateTracker;
        if (appStateTrackerImpl == null || !appStateTrackerImpl.areAlarmsRestrictedByBatterySaver(alarm.creatorUid, alarm.sourcePackage)) {
            return alarm.setPolicyElapsed(3, nowElapsed);
        }
        if ((alarm.flags & 8) != 0) {
            batterySaverPolicyElapsed = nowElapsed;
        } else if (isAllowedWhileIdleRestricted(alarm)) {
            int userId = UserHandle.getUserId(alarm.creatorUid);
            if ((alarm.flags & 4) != 0) {
                quota = this.mConstants.ALLOW_WHILE_IDLE_QUOTA;
                window = this.mConstants.ALLOW_WHILE_IDLE_WINDOW;
                history = this.mAllowWhileIdleHistory;
            } else {
                quota = this.mConstants.ALLOW_WHILE_IDLE_COMPAT_QUOTA;
                window = this.mConstants.ALLOW_WHILE_IDLE_COMPAT_WINDOW;
                history = this.mAllowWhileIdleCompatHistory;
            }
            int dispatchesInHistory = history.getTotalWakeupsInWindow(alarm.sourcePackage, userId);
            if (dispatchesInHistory < quota) {
                batterySaverPolicyElapsed = nowElapsed;
            } else {
                batterySaverPolicyElapsed = history.getNthLastWakeupForPackage(alarm.sourcePackage, userId, quota) + window;
            }
        } else if ((alarm.flags & 64) != 0) {
            long lastDispatch = this.mLastPriorityAlarmDispatch.get(alarm.creatorUid, 0L);
            if (lastDispatch == 0) {
                batterySaverPolicyElapsed = nowElapsed;
            } else {
                batterySaverPolicyElapsed = this.mConstants.PRIORITY_ALARM_DELAY + lastDispatch;
            }
        } else {
            batterySaverPolicyElapsed = 31536000000L + nowElapsed;
        }
        return alarm.setPolicyElapsed(3, batterySaverPolicyElapsed);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public static boolean isAllowedWhileIdleRestricted(Alarm a) {
        return (a.flags & 36) != 0;
    }

    /* JADX DEBUG: Method merged with bridge method */
    /* JADX INFO: Access modifiers changed from: private */
    /* renamed from: adjustDeliveryTimeBasedOnDeviceIdle */
    public boolean m951x4dd46403(Alarm alarm) {
        long deviceIdlePolicyTime;
        long whenAllowed;
        int quota;
        long window;
        AppWakeupHistory history;
        long nowElapsed = this.mInjector.getElapsedRealtime();
        Alarm alarm2 = this.mPendingIdleUntil;
        if (alarm2 == null || alarm2 == alarm) {
            return alarm.setPolicyElapsed(2, nowElapsed);
        }
        if ((alarm.flags & 10) != 0) {
            deviceIdlePolicyTime = nowElapsed;
        } else if (isAllowedWhileIdleRestricted(alarm)) {
            int userId = UserHandle.getUserId(alarm.creatorUid);
            if ((alarm.flags & 4) != 0) {
                quota = this.mConstants.ALLOW_WHILE_IDLE_QUOTA;
                window = this.mConstants.ALLOW_WHILE_IDLE_WINDOW;
                history = this.mAllowWhileIdleHistory;
            } else {
                quota = this.mConstants.ALLOW_WHILE_IDLE_COMPAT_QUOTA;
                window = this.mConstants.ALLOW_WHILE_IDLE_COMPAT_WINDOW;
                history = this.mAllowWhileIdleCompatHistory;
            }
            int dispatchesInHistory = history.getTotalWakeupsInWindow(alarm.sourcePackage, userId);
            if (dispatchesInHistory < quota) {
                deviceIdlePolicyTime = nowElapsed;
            } else {
                long whenInQuota = history.getNthLastWakeupForPackage(alarm.sourcePackage, userId, quota) + window;
                deviceIdlePolicyTime = Math.min(whenInQuota, this.mPendingIdleUntil.getWhenElapsed());
            }
        } else if ((alarm.flags & 64) != 0) {
            long lastDispatch = this.mLastPriorityAlarmDispatch.get(alarm.creatorUid, 0L);
            if (lastDispatch == 0) {
                whenAllowed = nowElapsed;
            } else {
                whenAllowed = this.mConstants.PRIORITY_ALARM_DELAY + lastDispatch;
            }
            deviceIdlePolicyTime = Math.min(whenAllowed, this.mPendingIdleUntil.getWhenElapsed());
        } else {
            deviceIdlePolicyTime = this.mPendingIdleUntil.getWhenElapsed();
        }
        return alarm.setPolicyElapsed(2, deviceIdlePolicyTime);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public boolean adjustDeliveryTimeBasedOnBucketLocked(Alarm alarm) {
        long t;
        long nowElapsed = this.mInjector.getElapsedRealtime();
        if (this.mConstants.USE_TARE_POLICY || isExemptFromAppStandby(alarm) || this.mAppStandbyParole) {
            return alarm.setPolicyElapsed(1, nowElapsed);
        }
        String sourcePackage = alarm.sourcePackage;
        int sourceUserId = UserHandle.getUserId(alarm.creatorUid);
        int standbyBucket = this.mUsageStatsManagerInternal.getAppStandbyBucket(sourcePackage, sourceUserId, nowElapsed);
        int wakeupsInWindow = this.mAppWakeupHistory.getTotalWakeupsInWindow(sourcePackage, sourceUserId);
        if (standbyBucket == 45) {
            if (wakeupsInWindow > 0) {
                long lastWakeupTime = this.mAppWakeupHistory.getNthLastWakeupForPackage(sourcePackage, sourceUserId, this.mConstants.APP_STANDBY_RESTRICTED_QUOTA);
                if (nowElapsed - lastWakeupTime < this.mConstants.APP_STANDBY_RESTRICTED_WINDOW) {
                    return alarm.setPolicyElapsed(1, this.mConstants.APP_STANDBY_RESTRICTED_WINDOW + lastWakeupTime);
                }
            }
        } else {
            int quotaForBucket = getQuotaForBucketLocked(standbyBucket);
            if (wakeupsInWindow >= quotaForBucket) {
                if (this.mTemporaryQuotaReserve.hasQuota(sourcePackage, sourceUserId, nowElapsed)) {
                    alarm.mUsingReserveQuota = true;
                    return alarm.setPolicyElapsed(1, nowElapsed);
                }
                if (quotaForBucket <= 0) {
                    t = 31536000000L + nowElapsed;
                } else {
                    long t2 = this.mAppWakeupHistory.getNthLastWakeupForPackage(sourcePackage, sourceUserId, quotaForBucket);
                    t = this.mConstants.APP_STANDBY_WINDOW + t2;
                }
                return alarm.setPolicyElapsed(1, t);
            }
        }
        alarm.mUsingReserveQuota = false;
        return alarm.setPolicyElapsed(1, nowElapsed);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public boolean adjustDeliveryTimeBasedOnTareLocked(Alarm alarm) {
        long nowElapsed = this.mInjector.getElapsedRealtime();
        if (!this.mConstants.USE_TARE_POLICY || isExemptFromTare(alarm) || hasEnoughWealthLocked(alarm)) {
            return alarm.setPolicyElapsed(4, nowElapsed);
        }
        return alarm.setPolicyElapsed(4, 31536000000L + nowElapsed);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void registerTareListener(Alarm alarm) {
        if (!this.mConstants.USE_TARE_POLICY) {
            return;
        }
        this.mEconomyManagerInternal.registerAffordabilityChangeListener(UserHandle.getUserId(alarm.creatorUid), alarm.sourcePackage, this.mAffordabilityChangeListener, TareBill.getAppropriateBill(alarm));
    }

    private void maybeUnregisterTareListenerLocked(final Alarm alarm) {
        if (!this.mConstants.USE_TARE_POLICY) {
            return;
        }
        final EconomyManagerInternal.ActionBill bill = TareBill.getAppropriateBill(alarm);
        Predicate<Alarm> isSameAlarmTypeForSameApp = new Predicate() { // from class: com.android.server.alarm.AlarmManagerService$$ExternalSyntheticLambda12
            @Override // java.util.function.Predicate
            public final boolean test(Object obj) {
                return AlarmManagerService.lambda$maybeUnregisterTareListenerLocked$8(Alarm.this, bill, (Alarm) obj);
            }
        };
        if (this.mAlarmStore.getCount(isSameAlarmTypeForSameApp) == 0) {
            int userId = UserHandle.getUserId(alarm.creatorUid);
            this.mEconomyManagerInternal.unregisterAffordabilityChangeListener(userId, alarm.sourcePackage, this.mAffordabilityChangeListener, bill);
            ArrayMap<EconomyManagerInternal.ActionBill, Boolean> actionAffordability = (ArrayMap) this.mAffordabilityCache.get(userId, alarm.sourcePackage);
            if (actionAffordability != null) {
                actionAffordability.remove(bill);
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static /* synthetic */ boolean lambda$maybeUnregisterTareListenerLocked$8(Alarm alarm, EconomyManagerInternal.ActionBill bill, Alarm a) {
        return alarm.creatorUid == a.creatorUid && alarm.sourcePackage.equals(a.sourcePackage) && bill.equals(TareBill.getAppropriateBill(a));
    }

    private void setImplLocked(Alarm a) {
        String packageName;
        String packageName2;
        if ((a.flags & 16) != 0) {
            adjustIdleUntilTime(a);
            IdleDispatchEntry ent = new IdleDispatchEntry();
            ent.uid = a.uid;
            ent.pkg = a.sourcePackage;
            ent.tag = a.statsTag;
            ent.op = "START IDLE";
            ent.elapsedRealtime = this.mInjector.getElapsedRealtime();
            ent.argRealtime = a.getWhenElapsed();
            this.mAllowWhileIdleDispatches.add(ent);
            Alarm alarm = this.mPendingIdleUntil;
            if (alarm != a && alarm != null) {
                Slog.wtfStack(TAG, "setImplLocked: idle until changed from " + this.mPendingIdleUntil + " to " + a);
                AlarmStore alarmStore = this.mAlarmStore;
                final Alarm alarm2 = this.mPendingIdleUntil;
                Objects.requireNonNull(alarm2);
                alarmStore.remove(new Predicate() { // from class: com.android.server.alarm.AlarmManagerService$$ExternalSyntheticLambda13
                    @Override // java.util.function.Predicate
                    public final boolean test(Object obj) {
                        return Alarm.this.equals((Alarm) obj);
                    }
                });
            }
            this.mPendingIdleUntil = a;
            this.mAlarmStore.updateAlarmDeliveries(new AlarmStore.AlarmDeliveryCalculator() { // from class: com.android.server.alarm.AlarmManagerService$$ExternalSyntheticLambda14
                @Override // com.android.server.alarm.AlarmStore.AlarmDeliveryCalculator
                public final boolean updateAlarmDelivery(Alarm alarm3) {
                    return AlarmManagerService.this.m950x2eaeac5d(alarm3);
                }
            });
        } else if (this.mPendingIdleUntil != null) {
            m951x4dd46403(a);
        }
        if ((a.flags & 2) != 0) {
            Alarm alarm3 = this.mNextWakeFromIdle;
            if (alarm3 == null || alarm3.getWhenElapsed() > a.getWhenElapsed()) {
                this.mNextWakeFromIdle = a;
                if (this.mPendingIdleUntil != null) {
                    boolean updated = this.mAlarmStore.updateAlarmDeliveries(new AlarmStore.AlarmDeliveryCalculator() { // from class: com.android.server.alarm.AlarmManagerService$$ExternalSyntheticLambda15
                        @Override // com.android.server.alarm.AlarmStore.AlarmDeliveryCalculator
                        public final boolean updateAlarmDelivery(Alarm alarm4) {
                            return AlarmManagerService.this.m948xb1a27ead(alarm4);
                        }
                    });
                    if (updated) {
                        this.mAlarmStore.updateAlarmDeliveries(new AlarmStore.AlarmDeliveryCalculator() { // from class: com.android.server.alarm.AlarmManagerService$$ExternalSyntheticLambda16
                            @Override // com.android.server.alarm.AlarmStore.AlarmDeliveryCalculator
                            public final boolean updateAlarmDelivery(Alarm alarm4) {
                                return AlarmManagerService.this.m949x6b1a0c4c(alarm4);
                            }
                        });
                    }
                }
            }
        } else if (sTranSlmSupport && this.mScreenOff && (a.flags & 2) == 0) {
            if (System.currentTimeMillis() - this.mScreenOffTime > this.screenOffThreshold && a.operation != null && (packageName2 = a.operation.getTargetPackage()) != null && !isExcept(packageName2) && isThirdPackageByName(packageName2) && sAlarmDebug) {
                Slog.d(TAG, "mPendingWhileIdleAlarms.add(a) pending while screenoff after 2min,packageName :" + a.packageName);
            }
        } else if (sTranSlmSupport && this.mScreenOff && !isIdleFlag(a) && System.currentTimeMillis() - this.mScreenOffTime > 14400000 && a != null && a.operation != null && (packageName = a.operation.getTargetPackage()) != null && this.mSystemApps.contains(packageName) && !isClockAlarm(a) && !isAndroidAlarm(a) && sAlarmDebug) {
            Slog.d(TAG, "packageName :" + a.packageName + ",alarm was pending by slm in deep idle");
        }
        if (a.alarmClock != null) {
            this.mNextAlarmClockMayChange = true;
        }
        adjustDeliveryTimeBasedOnBatterySaver(a);
        adjustDeliveryTimeBasedOnBucketLocked(a);
        adjustDeliveryTimeBasedOnTareLocked(a);
        registerTareListener(a);
        this.mAlarmStore.add(a);
        rescheduleKernelAlarmsLocked();
        updateNextAlarmClockLocked();
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$setImplLocked$10$com-android-server-alarm-AlarmManagerService  reason: not valid java name */
    public /* synthetic */ boolean m948xb1a27ead(Alarm alarm) {
        return alarm == this.mPendingIdleUntil && adjustIdleUntilTime(alarm);
    }

    /* loaded from: classes.dex */
    private final class LocalService implements AlarmManagerInternal {
        private LocalService() {
        }

        @Override // com.android.server.AlarmManagerInternal
        public boolean isIdling() {
            return AlarmManagerService.this.isIdlingImpl();
        }

        @Override // com.android.server.AlarmManagerInternal
        public void removeAlarmsForUid(int uid) {
            synchronized (AlarmManagerService.this.mLock) {
                AlarmManagerService.this.removeLocked(uid, 3);
            }
        }

        @Override // com.android.server.AlarmManagerInternal
        public void remove(PendingIntent pi) {
            AlarmManagerService.this.mHandler.obtainMessage(7, pi).sendToTarget();
        }

        @Override // com.android.server.AlarmManagerInternal
        public boolean hasExactAlarmPermission(String packageName, int uid) {
            return AlarmManagerService.this.hasScheduleExactAlarmInternal(packageName, uid) || AlarmManagerService.this.hasUseExactAlarmInternal(packageName, uid);
        }

        @Override // com.android.server.AlarmManagerInternal
        public void registerInFlightListener(AlarmManagerInternal.InFlightListener callback) {
            synchronized (AlarmManagerService.this.mLock) {
                AlarmManagerService.this.mInFlightListeners.add(callback);
            }
        }
    }

    boolean hasUseExactAlarmInternal(String packageName, int uid) {
        return isUseExactAlarmEnabled(packageName, UserHandle.getUserId(uid)) && PermissionChecker.checkPermissionForPreflight(getContext(), "android.permission.USE_EXACT_ALARM", -1, uid, packageName) == 0;
    }

    boolean isScheduleExactAlarmAllowedByDefault(String packageName, int uid) {
        List<String> wellbeingHolders;
        if (isScheduleExactAlarmDeniedByDefault(packageName, UserHandle.getUserId(uid))) {
            if (this.mPackageManagerInternal.isPlatformSigned(packageName) || this.mPackageManagerInternal.isUidPrivileged(uid)) {
                return true;
            }
            long token = Binder.clearCallingIdentity();
            try {
                RoleManager roleManager = this.mRoleManager;
                if (roleManager != null) {
                    wellbeingHolders = roleManager.getRoleHolders("android.app.role.SYSTEM_WELLBEING");
                } else {
                    wellbeingHolders = Collections.emptyList();
                }
                return wellbeingHolders.contains(packageName);
            } finally {
                Binder.restoreCallingIdentity(token);
            }
        }
        return !this.mConstants.EXACT_ALARM_DENY_LIST.contains(packageName);
    }

    boolean hasScheduleExactAlarmInternal(String packageName, int uid) {
        boolean hasPermission;
        long start = this.mStatLogger.getTime();
        if (!this.mExactAlarmCandidates.contains(Integer.valueOf(UserHandle.getAppId(uid)))) {
            hasPermission = false;
        } else if (!isExactAlarmChangeEnabled(packageName, UserHandle.getUserId(uid))) {
            hasPermission = false;
        } else {
            int mode = this.mAppOps.checkOpNoThrow(107, uid, packageName);
            if (mode == 3) {
                hasPermission = isScheduleExactAlarmAllowedByDefault(packageName, uid);
            } else {
                hasPermission = mode == 0;
            }
        }
        this.mStatLogger.logDurationStat(1, start);
        return hasPermission;
    }

    boolean isExemptFromMinWindowRestrictions(int uid) {
        return isExemptFromExactAlarmPermissionNoLock(uid);
    }

    boolean isExemptFromExactAlarmPermissionNoLock(int uid) {
        DeviceIdleInternal deviceIdleInternal;
        if (Build.IS_DEBUGGABLE && Thread.holdsLock(this.mLock)) {
            Slog.wtfStack(TAG, "Alarm lock held while calling into DeviceIdleController");
        }
        return UserHandle.isSameApp(this.mSystemUiUid, uid) || UserHandle.isCore(uid) || (deviceIdleInternal = this.mLocalDeviceIdleController) == null || deviceIdleInternal.isAppOnWhitelist(UserHandle.getAppId(uid));
    }

    /* JADX INFO: Access modifiers changed from: private */
    public static boolean isExactAlarmChangeEnabled(String packageName, int userId) {
        return CompatChanges.isChangeEnabled(171306433L, packageName, UserHandle.of(userId));
    }

    private static boolean isUseExactAlarmEnabled(String packageName, int userId) {
        return CompatChanges.isChangeEnabled(218533173L, packageName, UserHandle.of(userId));
    }

    private boolean isScheduleExactAlarmDeniedByDefault(String packageName, int userId) {
        return CompatChanges.isChangeEnabled(226439802L, packageName, UserHandle.of(userId));
    }

    @NeverCompile
    void dumpImpl(final IndentingPrintWriter pw) {
        boolean blocked;
        BroadcastStats bs;
        int pos;
        long nextNonWakeupRTC;
        synchronized (this.mLock) {
            pw.println("Current Alarm Manager state:");
            pw.increaseIndent();
            this.mConstants.dump(pw);
            pw.println();
            if (this.mConstants.USE_TARE_POLICY) {
                pw.println("TARE details:");
                pw.increaseIndent();
                pw.println("Affordability cache:");
                pw.increaseIndent();
                this.mAffordabilityCache.forEach(new SparseArrayMap.TriConsumer() { // from class: com.android.server.alarm.AlarmManagerService$$ExternalSyntheticLambda9
                    public final void accept(int i, Object obj, Object obj2) {
                        AlarmManagerService.lambda$dumpImpl$12(pw, i, (String) obj, (ArrayMap) obj2);
                    }
                });
                pw.decreaseIndent();
                pw.decreaseIndent();
                pw.println();
            } else {
                AppStateTrackerImpl appStateTrackerImpl = this.mAppStateTracker;
                if (appStateTrackerImpl != null) {
                    appStateTrackerImpl.dump(pw);
                    pw.println();
                }
                pw.println("App Standby Parole: " + this.mAppStandbyParole);
                pw.println();
            }
            long nowELAPSED = this.mInjector.getElapsedRealtime();
            long nowUPTIME = SystemClock.uptimeMillis();
            long nowRTC = this.mInjector.getCurrentTimeMillis();
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");
            pw.print("nowRTC=");
            pw.print(nowRTC);
            pw.print("=");
            pw.print(sdf.format(new Date(nowRTC)));
            pw.print(" nowELAPSED=");
            pw.print(nowELAPSED);
            pw.println();
            pw.print("mLastTimeChangeClockTime=");
            pw.print(this.mLastTimeChangeClockTime);
            pw.print("=");
            pw.println(sdf.format(new Date(this.mLastTimeChangeClockTime)));
            pw.print("mLastTimeChangeRealtime=");
            pw.println(this.mLastTimeChangeRealtime);
            pw.print("mLastTickReceived=");
            pw.println(sdf.format(new Date(this.mLastTickReceived)));
            pw.print("mLastTickSet=");
            pw.println(sdf.format(new Date(this.mLastTickSet)));
            pw.println();
            pw.println("Recent TIME_TICK history:");
            pw.increaseIndent();
            int i = this.mNextTickHistory;
            while (true) {
                i--;
                if (i < 0) {
                    i = 9;
                }
                long time = this.mTickHistory[i];
                pw.println(time > 0 ? sdf.format(new Date(nowRTC - (nowELAPSED - time))) : "-");
                if (i == this.mNextTickHistory) {
                    break;
                }
            }
            pw.decreaseIndent();
            SystemServiceManager ssm = (SystemServiceManager) LocalServices.getService(SystemServiceManager.class);
            if (ssm != null) {
                pw.println();
                pw.print("RuntimeStarted=");
                pw.print(sdf.format(new Date((nowRTC - nowELAPSED) + ssm.getRuntimeStartElapsedTime())));
                if (ssm.isRuntimeRestarted()) {
                    pw.print("  (Runtime restarted)");
                }
                pw.println();
                pw.print("Runtime uptime (elapsed): ");
                TimeUtils.formatDuration(nowELAPSED, ssm.getRuntimeStartElapsedTime(), pw);
                pw.println();
                pw.print("Runtime uptime (uptime): ");
                TimeUtils.formatDuration(nowUPTIME, ssm.getRuntimeStartUptime(), pw);
                pw.println();
            }
            pw.println();
            if (!this.mInteractive) {
                pw.print("Time since non-interactive: ");
                TimeUtils.formatDuration(nowELAPSED - this.mNonInteractiveStartTime, pw);
                pw.println();
            }
            pw.print("Max wakeup delay: ");
            TimeUtils.formatDuration(currentNonWakeupFuzzLocked(nowELAPSED), pw);
            pw.println();
            pw.print("Time since last dispatch: ");
            TimeUtils.formatDuration(nowELAPSED - this.mLastAlarmDeliveryTime, pw);
            pw.println();
            pw.print("Next non-wakeup delivery time: ");
            TimeUtils.formatDuration(this.mNextNonWakeupDeliveryTime, nowELAPSED, pw);
            pw.println();
            long nextWakeupRTC = this.mNextWakeup + (nowRTC - nowELAPSED);
            long nextNonWakeupRTC2 = this.mNextNonWakeup + (nowRTC - nowELAPSED);
            pw.print("Next non-wakeup alarm: ");
            TimeUtils.formatDuration(this.mNextNonWakeup, nowELAPSED, pw);
            pw.print(" = ");
            pw.print(this.mNextNonWakeup);
            pw.print(" = ");
            pw.println(sdf.format(new Date(nextNonWakeupRTC2)));
            pw.increaseIndent();
            pw.print("set at ");
            TimeUtils.formatDuration(this.mNextNonWakeUpSetAt, nowELAPSED, pw);
            pw.decreaseIndent();
            pw.println();
            pw.print("Next wakeup alarm: ");
            TimeUtils.formatDuration(this.mNextWakeup, nowELAPSED, pw);
            pw.print(" = ");
            pw.print(this.mNextWakeup);
            pw.print(" = ");
            pw.println(sdf.format(new Date(nextWakeupRTC)));
            pw.increaseIndent();
            pw.print("set at ");
            TimeUtils.formatDuration(this.mNextWakeUpSetAt, nowELAPSED, pw);
            pw.decreaseIndent();
            pw.println();
            pw.print("Next kernel non-wakeup alarm: ");
            TimeUtils.formatDuration(this.mInjector.getNextAlarm(3), pw);
            pw.println();
            pw.print("Next kernel wakeup alarm: ");
            TimeUtils.formatDuration(this.mInjector.getNextAlarm(2), pw);
            pw.println();
            pw.print("Last wakeup: ");
            TimeUtils.formatDuration(this.mLastWakeup, nowELAPSED, pw);
            pw.print(" = ");
            pw.println(this.mLastWakeup);
            pw.print("Last trigger: ");
            TimeUtils.formatDuration(this.mLastTrigger, nowELAPSED, pw);
            pw.print(" = ");
            pw.println(this.mLastTrigger);
            pw.print("Num time change events: ");
            pw.println(this.mNumTimeChanged);
            pw.println();
            pw.println("App ids requesting SCHEDULE_EXACT_ALARM: " + this.mExactAlarmCandidates);
            pw.println();
            pw.print("Last OP_SCHEDULE_EXACT_ALARM: [");
            int i2 = 0;
            while (i2 < this.mLastOpScheduleExactAlarm.size()) {
                if (i2 > 0) {
                    pw.print(", ");
                }
                UserHandle.formatUid(pw, this.mLastOpScheduleExactAlarm.keyAt(i2));
                pw.print(":" + AppOpsManager.modeToName(this.mLastOpScheduleExactAlarm.valueAt(i2)));
                i2++;
                ssm = ssm;
            }
            pw.println("]");
            pw.println();
            pw.println("Next alarm clock information: ");
            pw.increaseIndent();
            TreeSet<Integer> users = new TreeSet<>();
            for (int i3 = 0; i3 < this.mNextAlarmClockForUser.size(); i3++) {
                users.add(Integer.valueOf(this.mNextAlarmClockForUser.keyAt(i3)));
            }
            for (int i4 = 0; i4 < this.mPendingSendNextAlarmClockChangedForUser.size(); i4++) {
                users.add(Integer.valueOf(this.mPendingSendNextAlarmClockChangedForUser.keyAt(i4)));
            }
            Iterator<Integer> it = users.iterator();
            while (it.hasNext()) {
                int user = it.next().intValue();
                TreeSet<Integer> users2 = users;
                AlarmManager.AlarmClockInfo next = this.mNextAlarmClockForUser.get(user);
                long time2 = next != null ? next.getTriggerTime() : 0L;
                boolean pendingSend = this.mPendingSendNextAlarmClockChangedForUser.get(user);
                Iterator<Integer> it2 = it;
                pw.print("user:");
                pw.print(user);
                pw.print(" pendingSend:");
                pw.print(pendingSend);
                pw.print(" time:");
                pw.print(time2);
                if (time2 > 0) {
                    pw.print(" = ");
                    pw.print(sdf.format(new Date(time2)));
                    pw.print(" = ");
                    TimeUtils.formatDuration(time2, nowRTC, pw);
                }
                pw.println();
                users = users2;
                it = it2;
            }
            pw.decreaseIndent();
            if (this.mAlarmStore.size() > 0) {
                pw.println();
                this.mAlarmStore.dump(pw, nowELAPSED, sdf);
            }
            pw.println();
            pw.println("Pending user blocked background alarms: ");
            pw.increaseIndent();
            boolean blocked2 = false;
            for (int i5 = 0; i5 < this.mPendingBackgroundAlarms.size(); i5++) {
                ArrayList<Alarm> blockedAlarms = this.mPendingBackgroundAlarms.valueAt(i5);
                if (blockedAlarms != null && blockedAlarms.size() > 0) {
                    blocked2 = true;
                    dumpAlarmList(pw, blockedAlarms, nowELAPSED, sdf);
                }
            }
            if (!blocked2) {
                pw.println("none");
            }
            pw.decreaseIndent();
            pw.println();
            pw.print("Pending alarms per uid: [");
            for (int i6 = 0; i6 < this.mAlarmsPerUid.size(); i6++) {
                if (i6 > 0) {
                    pw.print(", ");
                }
                UserHandle.formatUid(pw, this.mAlarmsPerUid.keyAt(i6));
                pw.print(":");
                pw.print(this.mAlarmsPerUid.valueAt(i6));
            }
            pw.println("]");
            pw.println();
            pw.println("App Alarm history:");
            this.mAppWakeupHistory.dump(pw, nowELAPSED);
            pw.println();
            pw.println("Temporary Quota Reserves:");
            this.mTemporaryQuotaReserve.dump(pw, nowELAPSED);
            if (this.mPendingIdleUntil != null) {
                pw.println();
                pw.println("Idle mode state:");
                pw.increaseIndent();
                pw.print("Idling until: ");
                Alarm alarm = this.mPendingIdleUntil;
                if (alarm != null) {
                    pw.println(alarm);
                    this.mPendingIdleUntil.dump(pw, nowELAPSED, sdf);
                } else {
                    pw.println("null");
                }
                pw.decreaseIndent();
            }
            if (this.mNextWakeFromIdle != null) {
                pw.println();
                pw.print("Next wake from idle: ");
                pw.println(this.mNextWakeFromIdle);
                pw.increaseIndent();
                this.mNextWakeFromIdle.dump(pw, nowELAPSED, sdf);
                pw.decreaseIndent();
            }
            pw.println();
            pw.print("Past-due non-wakeup alarms: ");
            if (this.mPendingNonWakeupAlarms.size() > 0) {
                pw.println(this.mPendingNonWakeupAlarms.size());
                pw.increaseIndent();
                dumpAlarmList(pw, this.mPendingNonWakeupAlarms, nowELAPSED, sdf);
                pw.decreaseIndent();
            } else {
                pw.println("(none)");
            }
            pw.increaseIndent();
            pw.print("Number of delayed alarms: ");
            pw.print(this.mNumDelayedAlarms);
            pw.print(", total delay time: ");
            boolean blocked3 = blocked2;
            TimeUtils.formatDuration(this.mTotalDelayTime, pw);
            pw.println();
            pw.print("Max delay time: ");
            TimeUtils.formatDuration(this.mMaxDelayTime, pw);
            pw.print(", max non-interactive time: ");
            TimeUtils.formatDuration(this.mNonInteractiveTime, pw);
            pw.println();
            pw.decreaseIndent();
            pw.println();
            pw.print("Broadcast ref count: ");
            pw.println(this.mBroadcastRefCount);
            pw.print("PendingIntent send count: ");
            pw.println(this.mSendCount);
            pw.print("PendingIntent finish count: ");
            pw.println(this.mSendFinishCount);
            pw.print("Listener send count: ");
            pw.println(this.mListenerCount);
            pw.print("Listener finish count: ");
            pw.println(this.mListenerFinishCount);
            pw.println();
            if (this.mInFlight.size() > 0) {
                pw.println("Outstanding deliveries:");
                pw.increaseIndent();
                for (int i7 = 0; i7 < this.mInFlight.size(); i7++) {
                    pw.print("#");
                    pw.print(i7);
                    pw.print(": ");
                    pw.println(this.mInFlight.get(i7));
                }
                pw.decreaseIndent();
                pw.println();
            }
            pw.println("Allow while idle history:");
            this.mAllowWhileIdleHistory.dump(pw, nowELAPSED);
            pw.println();
            pw.println("Allow while idle compat history:");
            this.mAllowWhileIdleCompatHistory.dump(pw, nowELAPSED);
            pw.println();
            if (this.mLastPriorityAlarmDispatch.size() > 0) {
                pw.println("Last priority alarm dispatches:");
                pw.increaseIndent();
                int i8 = 0;
                while (i8 < this.mLastPriorityAlarmDispatch.size()) {
                    pw.print("UID: ");
                    UserHandle.formatUid(pw, this.mLastPriorityAlarmDispatch.keyAt(i8));
                    pw.print(": ");
                    TimeUtils.formatDuration(this.mLastPriorityAlarmDispatch.valueAt(i8), nowELAPSED, pw);
                    pw.println();
                    i8++;
                    nowRTC = nowRTC;
                }
                pw.decreaseIndent();
            }
            if (this.mRemovalHistory.size() > 0) {
                pw.println("Removal history: ");
                pw.increaseIndent();
                for (int i9 = 0; i9 < this.mRemovalHistory.size(); i9++) {
                    UserHandle.formatUid(pw, this.mRemovalHistory.keyAt(i9));
                    pw.println(":");
                    pw.increaseIndent();
                    RemovedAlarm[] historyForUid = (RemovedAlarm[]) this.mRemovalHistory.valueAt(i9).toArray();
                    int length = historyForUid.length;
                    int i10 = 0;
                    while (i10 < length) {
                        RemovedAlarm removedAlarm = historyForUid[i10];
                        removedAlarm.dump(pw, nowELAPSED, sdf);
                        i10++;
                        historyForUid = historyForUid;
                    }
                    pw.decreaseIndent();
                }
                pw.decreaseIndent();
                pw.println();
            }
            if (this.mLog.dump(pw, "Recent problems:")) {
                pw.println();
            }
            FilterStats[] topFilters = new FilterStats[10];
            Comparator<FilterStats> comparator = new Comparator<FilterStats>() { // from class: com.android.server.alarm.AlarmManagerService.6
                /* JADX DEBUG: Method merged with bridge method */
                @Override // java.util.Comparator
                public int compare(FilterStats lhs, FilterStats rhs) {
                    if (lhs.aggregateTime < rhs.aggregateTime) {
                        return 1;
                    }
                    if (lhs.aggregateTime > rhs.aggregateTime) {
                        return -1;
                    }
                    return 0;
                }
            };
            int len = 0;
            int iu = 0;
            while (true) {
                SimpleDateFormat sdf2 = sdf;
                if (iu >= this.mBroadcastStats.size()) {
                    break;
                }
                ArrayMap<String, BroadcastStats> uidStats = this.mBroadcastStats.valueAt(iu);
                int len2 = len;
                int len3 = 0;
                while (true) {
                    blocked = blocked3;
                    if (len3 < uidStats.size()) {
                        BroadcastStats bs2 = uidStats.valueAt(len3);
                        ArrayMap<String, BroadcastStats> uidStats2 = uidStats;
                        long nextWakeupRTC2 = nextWakeupRTC;
                        int len4 = len2;
                        int is = 0;
                        while (is < bs2.filterStats.size()) {
                            FilterStats fs = bs2.filterStats.valueAt(is);
                            if (len4 > 0) {
                                bs = bs2;
                                pos = Arrays.binarySearch(topFilters, 0, len4, fs, comparator);
                            } else {
                                bs = bs2;
                                pos = 0;
                            }
                            int pos2 = pos;
                            if (pos2 >= 0) {
                                nextNonWakeupRTC = nextNonWakeupRTC2;
                            } else {
                                nextNonWakeupRTC = nextNonWakeupRTC2;
                                pos2 = (-pos2) - 1;
                            }
                            if (pos2 < topFilters.length) {
                                int copylen = (topFilters.length - pos2) - 1;
                                if (copylen > 0) {
                                    System.arraycopy(topFilters, pos2, topFilters, pos2 + 1, copylen);
                                }
                                topFilters[pos2] = fs;
                                if (len4 < topFilters.length) {
                                    len4++;
                                }
                            }
                            is++;
                            bs2 = bs;
                            nextNonWakeupRTC2 = nextNonWakeupRTC;
                        }
                        len3++;
                        len2 = len4;
                        blocked3 = blocked;
                        uidStats = uidStats2;
                        nextWakeupRTC = nextWakeupRTC2;
                    }
                }
                iu++;
                sdf = sdf2;
                len = len2;
                blocked3 = blocked;
            }
            if (len > 0) {
                pw.println("Top Alarms:");
                pw.increaseIndent();
                for (int i11 = 0; i11 < len; i11++) {
                    FilterStats fs2 = topFilters[i11];
                    if (fs2.nesting > 0) {
                        pw.print("*ACTIVE* ");
                    }
                    TimeUtils.formatDuration(fs2.aggregateTime, pw);
                    pw.print(" running, ");
                    pw.print(fs2.numWakeup);
                    pw.print(" wakeups, ");
                    pw.print(fs2.count);
                    pw.print(" alarms: ");
                    UserHandle.formatUid(pw, fs2.mBroadcastStats.mUid);
                    pw.print(":");
                    pw.print(fs2.mBroadcastStats.mPackageName);
                    pw.println();
                    pw.increaseIndent();
                    pw.print(fs2.mTag);
                    pw.println();
                    pw.decreaseIndent();
                }
                pw.decreaseIndent();
            }
            pw.println();
            pw.println("Alarm Stats:");
            ArrayList<FilterStats> tmpFilters = new ArrayList<>();
            for (int iu2 = 0; iu2 < this.mBroadcastStats.size(); iu2++) {
                ArrayMap<String, BroadcastStats> uidStats3 = this.mBroadcastStats.valueAt(iu2);
                int ip = 0;
                while (ip < uidStats3.size()) {
                    BroadcastStats bs3 = uidStats3.valueAt(ip);
                    if (bs3.nesting > 0) {
                        pw.print("*ACTIVE* ");
                    }
                    UserHandle.formatUid(pw, bs3.mUid);
                    pw.print(":");
                    pw.print(bs3.mPackageName);
                    pw.print(" ");
                    TimeUtils.formatDuration(bs3.aggregateTime, pw);
                    pw.print(" running, ");
                    pw.print(bs3.numWakeup);
                    pw.println(" wakeups:");
                    tmpFilters.clear();
                    for (int is2 = 0; is2 < bs3.filterStats.size(); is2++) {
                        tmpFilters.add(bs3.filterStats.valueAt(is2));
                    }
                    Collections.sort(tmpFilters, comparator);
                    pw.increaseIndent();
                    int i12 = 0;
                    while (i12 < tmpFilters.size()) {
                        FilterStats fs3 = tmpFilters.get(i12);
                        FilterStats[] topFilters2 = topFilters;
                        if (fs3.nesting > 0) {
                            pw.print("*ACTIVE* ");
                        }
                        TimeUtils.formatDuration(fs3.aggregateTime, pw);
                        pw.print(" ");
                        pw.print(fs3.numWakeup);
                        pw.print(" wakes ");
                        pw.print(fs3.count);
                        pw.print(" alarms, last ");
                        TimeUtils.formatDuration(fs3.lastTime, nowELAPSED, pw);
                        pw.println(":");
                        pw.increaseIndent();
                        pw.print(fs3.mTag);
                        pw.println();
                        pw.decreaseIndent();
                        i12++;
                        topFilters = topFilters2;
                        comparator = comparator;
                    }
                    pw.decreaseIndent();
                    ip++;
                    topFilters = topFilters;
                    comparator = comparator;
                }
            }
            pw.println();
            this.mStatLogger.dump(pw);
            pw.println();
            pw.println("Allow while idle dispatches:");
            pw.increaseIndent();
            for (int i13 = 0; i13 < this.mAllowWhileIdleDispatches.size(); i13++) {
                IdleDispatchEntry ent = this.mAllowWhileIdleDispatches.get(i13);
                TimeUtils.formatDuration(ent.elapsedRealtime, nowELAPSED, pw);
                pw.print(": ");
                UserHandle.formatUid(pw, ent.uid);
                pw.print(":");
                pw.println(ent.pkg);
                pw.increaseIndent();
                if (ent.op != null) {
                    pw.print(ent.op);
                    pw.print(" / ");
                    pw.print(ent.tag);
                    if (ent.argRealtime != 0) {
                        pw.print(" (");
                        TimeUtils.formatDuration(ent.argRealtime, nowELAPSED, pw);
                        pw.print(")");
                    }
                    pw.println();
                }
                pw.decreaseIndent();
            }
            pw.decreaseIndent();
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static /* synthetic */ void lambda$dumpImpl$12(IndentingPrintWriter pw, int userId, String pkgName, ArrayMap billMap) {
        int numBills = billMap.size();
        if (numBills > 0) {
            pw.print(userId);
            pw.print(":");
            pw.print(pkgName);
            pw.println(":");
            pw.increaseIndent();
            for (int i = 0; i < numBills; i++) {
                pw.print(TareBill.getName((EconomyManagerInternal.ActionBill) billMap.keyAt(i)));
                pw.print(": ");
                pw.println(billMap.valueAt(i));
            }
            pw.decreaseIndent();
        }
    }

    void dumpProto(FileDescriptor fd) {
        ArrayMap<String, BroadcastStats> uidStats;
        BroadcastStats bs;
        long nowRTC;
        int pendingSendNextAlarmClockChangedForUserSize;
        long nowRTC2;
        ProtoOutputStream proto = new ProtoOutputStream(fd);
        synchronized (this.mLock) {
            long nowRTC3 = this.mInjector.getCurrentTimeMillis();
            long nowElapsed = this.mInjector.getElapsedRealtime();
            proto.write(1112396529665L, nowRTC3);
            proto.write(1112396529666L, nowElapsed);
            proto.write(1112396529667L, this.mLastTimeChangeClockTime);
            proto.write(1112396529668L, this.mLastTimeChangeRealtime);
            this.mConstants.dumpProto(proto, 1146756268037L);
            AppStateTrackerImpl appStateTrackerImpl = this.mAppStateTracker;
            if (appStateTrackerImpl != null) {
                appStateTrackerImpl.dumpProto(proto, 1146756268038L);
            }
            proto.write(1133871366151L, this.mInteractive);
            if (!this.mInteractive) {
                proto.write(1112396529672L, nowElapsed - this.mNonInteractiveStartTime);
                proto.write(1112396529673L, currentNonWakeupFuzzLocked(nowElapsed));
                proto.write(1112396529674L, nowElapsed - this.mLastAlarmDeliveryTime);
                proto.write(1112396529675L, nowElapsed - this.mNextNonWakeupDeliveryTime);
            }
            proto.write(1112396529676L, this.mNextNonWakeup - nowElapsed);
            proto.write(1112396529677L, this.mNextWakeup - nowElapsed);
            proto.write(1112396529678L, nowElapsed - this.mLastWakeup);
            proto.write(1112396529679L, nowElapsed - this.mNextWakeUpSetAt);
            proto.write(1112396529680L, this.mNumTimeChanged);
            TreeSet<Integer> users = new TreeSet<>();
            int nextAlarmClockForUserSize = this.mNextAlarmClockForUser.size();
            for (int i = 0; i < nextAlarmClockForUserSize; i++) {
                users.add(Integer.valueOf(this.mNextAlarmClockForUser.keyAt(i)));
            }
            int pendingSendNextAlarmClockChangedForUserSize2 = this.mPendingSendNextAlarmClockChangedForUser.size();
            for (int i2 = 0; i2 < pendingSendNextAlarmClockChangedForUserSize2; i2++) {
                users.add(Integer.valueOf(this.mPendingSendNextAlarmClockChangedForUser.keyAt(i2)));
            }
            for (Iterator<Integer> it = users.iterator(); it.hasNext(); it = it) {
                int user = it.next().intValue();
                AlarmManager.AlarmClockInfo next = this.mNextAlarmClockForUser.get(user);
                long time = next != null ? next.getTriggerTime() : 0L;
                boolean pendingSend = this.mPendingSendNextAlarmClockChangedForUser.get(user);
                long aToken = proto.start(2246267895826L);
                proto.write(CompanionMessage.MESSAGE_ID, user);
                proto.write(1133871366146L, pendingSend);
                proto.write(1112396529667L, time);
                proto.end(aToken);
                pendingSendNextAlarmClockChangedForUserSize2 = pendingSendNextAlarmClockChangedForUserSize2;
            }
            int pendingSendNextAlarmClockChangedForUserSize3 = pendingSendNextAlarmClockChangedForUserSize2;
            long j = CompanionMessage.MESSAGE_ID;
            this.mAlarmStore.dumpProto(proto, nowElapsed);
            int i3 = 0;
            while (i3 < this.mPendingBackgroundAlarms.size()) {
                ArrayList<Alarm> blockedAlarms = this.mPendingBackgroundAlarms.valueAt(i3);
                if (blockedAlarms == null) {
                    nowRTC = nowRTC3;
                    pendingSendNextAlarmClockChangedForUserSize = pendingSendNextAlarmClockChangedForUserSize3;
                    nowRTC2 = j;
                } else {
                    Iterator<Alarm> it2 = blockedAlarms.iterator();
                    while (it2.hasNext()) {
                        Alarm a = it2.next();
                        a.dumpDebug(proto, 2246267895828L, nowElapsed);
                        j = j;
                        pendingSendNextAlarmClockChangedForUserSize3 = pendingSendNextAlarmClockChangedForUserSize3;
                        nowRTC3 = nowRTC3;
                    }
                    nowRTC = nowRTC3;
                    pendingSendNextAlarmClockChangedForUserSize = pendingSendNextAlarmClockChangedForUserSize3;
                    nowRTC2 = j;
                }
                i3++;
                j = nowRTC2;
                pendingSendNextAlarmClockChangedForUserSize3 = pendingSendNextAlarmClockChangedForUserSize;
                nowRTC3 = nowRTC;
            }
            Alarm alarm = this.mPendingIdleUntil;
            if (alarm != null) {
                alarm.dumpDebug(proto, 1146756268053L, nowElapsed);
            }
            Alarm alarm2 = this.mNextWakeFromIdle;
            if (alarm2 != null) {
                alarm2.dumpDebug(proto, 1146756268055L, nowElapsed);
            }
            Iterator<Alarm> it3 = this.mPendingNonWakeupAlarms.iterator();
            while (it3.hasNext()) {
                Alarm a2 = it3.next();
                a2.dumpDebug(proto, 2246267895832L, nowElapsed);
            }
            proto.write(1120986464281L, this.mNumDelayedAlarms);
            proto.write(1112396529690L, this.mTotalDelayTime);
            proto.write(1112396529691L, this.mMaxDelayTime);
            proto.write(1112396529692L, this.mNonInteractiveTime);
            proto.write(1120986464285L, this.mBroadcastRefCount);
            proto.write(1120986464286L, this.mSendCount);
            proto.write(1120986464287L, this.mSendFinishCount);
            proto.write(1120986464288L, this.mListenerCount);
            proto.write(1120986464289L, this.mListenerFinishCount);
            Iterator<InFlight> it4 = this.mInFlight.iterator();
            while (it4.hasNext()) {
                InFlight f = it4.next();
                f.dumpDebug(proto, 2246267895842L);
            }
            this.mLog.dumpDebug(proto, 1146756268069L);
            FilterStats[] topFilters = new FilterStats[10];
            Comparator<FilterStats> comparator = new Comparator<FilterStats>() { // from class: com.android.server.alarm.AlarmManagerService.7
                /* JADX DEBUG: Method merged with bridge method */
                @Override // java.util.Comparator
                public int compare(FilterStats lhs, FilterStats rhs) {
                    if (lhs.aggregateTime < rhs.aggregateTime) {
                        return 1;
                    }
                    if (lhs.aggregateTime > rhs.aggregateTime) {
                        return -1;
                    }
                    return 0;
                }
            };
            int len = 0;
            for (int iu = 0; iu < this.mBroadcastStats.size(); iu++) {
                ArrayMap<String, BroadcastStats> uidStats2 = this.mBroadcastStats.valueAt(iu);
                for (int ip = 0; ip < uidStats2.size(); ip++) {
                    BroadcastStats bs2 = uidStats2.valueAt(ip);
                    int is = 0;
                    while (is < bs2.filterStats.size()) {
                        FilterStats fs = bs2.filterStats.valueAt(is);
                        TreeSet<Integer> users2 = users;
                        int pos = len > 0 ? Arrays.binarySearch(topFilters, 0, len, fs, comparator) : 0;
                        if (pos >= 0) {
                            uidStats = uidStats2;
                        } else {
                            uidStats = uidStats2;
                            pos = (-pos) - 1;
                        }
                        if (pos >= topFilters.length) {
                            bs = bs2;
                        } else {
                            int copylen = (topFilters.length - pos) - 1;
                            if (copylen <= 0) {
                                bs = bs2;
                            } else {
                                bs = bs2;
                                System.arraycopy(topFilters, pos, topFilters, pos + 1, copylen);
                            }
                            topFilters[pos] = fs;
                            if (len < topFilters.length) {
                                len++;
                            }
                        }
                        is++;
                        users = users2;
                        uidStats2 = uidStats;
                        bs2 = bs;
                    }
                }
            }
            int i4 = 0;
            while (i4 < len) {
                long token = proto.start(2246267895846L);
                FilterStats fs2 = topFilters[i4];
                proto.write(CompanionMessage.MESSAGE_ID, fs2.mBroadcastStats.mUid);
                proto.write(1138166333442L, fs2.mBroadcastStats.mPackageName);
                fs2.dumpDebug(proto, 1146756268035L);
                proto.end(token);
                i4++;
                nowElapsed = nowElapsed;
            }
            ArrayList<FilterStats> tmpFilters = new ArrayList<>();
            for (int iu2 = 0; iu2 < this.mBroadcastStats.size(); iu2++) {
                ArrayMap<String, BroadcastStats> uidStats3 = this.mBroadcastStats.valueAt(iu2);
                int ip2 = 0;
                while (ip2 < uidStats3.size()) {
                    long token2 = proto.start(2246267895847L);
                    BroadcastStats bs3 = uidStats3.valueAt(ip2);
                    bs3.dumpDebug(proto, 1146756268033L);
                    tmpFilters.clear();
                    for (int is2 = 0; is2 < bs3.filterStats.size(); is2++) {
                        tmpFilters.add(bs3.filterStats.valueAt(is2));
                    }
                    Collections.sort(tmpFilters, comparator);
                    Iterator<FilterStats> it5 = tmpFilters.iterator();
                    while (it5.hasNext()) {
                        it5.next().dumpDebug(proto, 2246267895810L);
                        topFilters = topFilters;
                        comparator = comparator;
                    }
                    proto.end(token2);
                    ip2++;
                    topFilters = topFilters;
                    comparator = comparator;
                }
            }
            for (int i5 = 0; i5 < this.mAllowWhileIdleDispatches.size(); i5++) {
                IdleDispatchEntry ent = this.mAllowWhileIdleDispatches.get(i5);
                long token3 = proto.start(2246267895848L);
                proto.write(CompanionMessage.MESSAGE_ID, ent.uid);
                proto.write(1138166333442L, ent.pkg);
                proto.write(1138166333443L, ent.tag);
                proto.write(1138166333444L, ent.op);
                proto.write(1112396529669L, ent.elapsedRealtime);
                proto.write(1112396529670L, ent.argRealtime);
                proto.end(token3);
            }
        }
        proto.flush();
    }

    long getNextWakeFromIdleTimeImpl() {
        long whenElapsed;
        synchronized (this.mLock) {
            Alarm alarm = this.mNextWakeFromIdle;
            whenElapsed = alarm != null ? alarm.getWhenElapsed() : JobStatus.NO_LATEST_RUNTIME;
        }
        return whenElapsed;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public boolean isIdlingImpl() {
        boolean z;
        synchronized (this.mLock) {
            z = this.mPendingIdleUntil != null;
        }
        return z;
    }

    AlarmManager.AlarmClockInfo getNextAlarmClockImpl(int userId) {
        AlarmManager.AlarmClockInfo alarmClockInfo;
        synchronized (this.mLock) {
            alarmClockInfo = this.mNextAlarmClockForUser.get(userId);
        }
        return alarmClockInfo;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void updateNextAlarmClockLocked() {
        if (!this.mNextAlarmClockMayChange) {
            return;
        }
        this.mNextAlarmClockMayChange = false;
        SparseArray<AlarmManager.AlarmClockInfo> nextForUser = this.mTmpSparseAlarmClockArray;
        nextForUser.clear();
        ITranAlarmManagerService.Instance().updateAlarms(new ITranAlarmManagerService.IAlarmManagerServiceTroy() { // from class: com.android.server.alarm.AlarmManagerService$$ExternalSyntheticLambda1
            @Override // com.transsion.hubcore.server.alarm.ITranAlarmManagerService.IAlarmManagerServiceTroy
            public final List getAlarmListWhenUpdateNextAlarmClockLocked() {
                return AlarmManagerService.this.m952x457dc07b();
            }
        });
        ArrayList<Alarm> allAlarms = this.mAlarmStore.asList();
        Iterator<Alarm> it = allAlarms.iterator();
        while (it.hasNext()) {
            Alarm a = it.next();
            if (a.alarmClock != null) {
                int userId = UserHandle.getUserId(a.uid);
                AlarmManager.AlarmClockInfo current = this.mNextAlarmClockForUser.get(userId);
                if (DEBUG_ALARM_CLOCK) {
                    Log.v(TAG, "Found AlarmClockInfo " + a.alarmClock + " at " + formatNextAlarm(getContext(), a.alarmClock, userId) + " for user " + userId);
                }
                if (nextForUser.get(userId) == null) {
                    nextForUser.put(userId, a.alarmClock);
                } else if (a.alarmClock.equals(current) && current.getTriggerTime() <= nextForUser.get(userId).getTriggerTime()) {
                    nextForUser.put(userId, current);
                }
            }
        }
        int newUserCount = nextForUser.size();
        for (int i = 0; i < newUserCount; i++) {
            AlarmManager.AlarmClockInfo newAlarm = nextForUser.valueAt(i);
            int userId2 = nextForUser.keyAt(i);
            AlarmManager.AlarmClockInfo currentAlarm = this.mNextAlarmClockForUser.get(userId2);
            if (!newAlarm.equals(currentAlarm)) {
                updateNextAlarmInfoForUserLocked(userId2, newAlarm);
            }
        }
        int oldUserCount = this.mNextAlarmClockForUser.size();
        for (int i2 = oldUserCount - 1; i2 >= 0; i2--) {
            int userId3 = this.mNextAlarmClockForUser.keyAt(i2);
            if (nextForUser.get(userId3) == null) {
                updateNextAlarmInfoForUserLocked(userId3, null);
            }
        }
    }

    private void updateNextAlarmInfoForUserLocked(int userId, AlarmManager.AlarmClockInfo alarmClock) {
        if (alarmClock != null) {
            if (DEBUG_ALARM_CLOCK) {
                Log.v(TAG, "Next AlarmClockInfoForUser(" + userId + "): " + formatNextAlarm(getContext(), alarmClock, userId));
            }
            this.mNextAlarmClockForUser.put(userId, alarmClock);
        } else {
            if (DEBUG_ALARM_CLOCK) {
                Log.v(TAG, "Next AlarmClockInfoForUser(" + userId + "): None");
            }
            this.mNextAlarmClockForUser.remove(userId);
        }
        this.mPendingSendNextAlarmClockChangedForUser.put(userId, true);
        this.mHandler.removeMessages(2);
        this.mHandler.sendEmptyMessage(2);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void sendNextAlarmClockChanged() {
        SparseArray<AlarmManager.AlarmClockInfo> pendingUsers = this.mHandlerSparseAlarmClockArray;
        pendingUsers.clear();
        synchronized (this.mLock) {
            int n = this.mPendingSendNextAlarmClockChangedForUser.size();
            for (int i = 0; i < n; i++) {
                int userId = this.mPendingSendNextAlarmClockChangedForUser.keyAt(i);
                pendingUsers.append(userId, this.mNextAlarmClockForUser.get(userId));
            }
            this.mPendingSendNextAlarmClockChangedForUser.clear();
        }
        int n2 = pendingUsers.size();
        for (int i2 = 0; i2 < n2; i2++) {
            int userId2 = pendingUsers.keyAt(i2);
            AlarmManager.AlarmClockInfo alarmClock = pendingUsers.valueAt(i2);
            Settings.System.putStringForUser(getContext().getContentResolver(), "next_alarm_formatted", formatNextAlarm(getContext(), alarmClock, userId2), userId2);
            getContext().sendBroadcastAsUser(NEXT_ALARM_CLOCK_CHANGED_INTENT, new UserHandle(userId2));
        }
    }

    private static String formatNextAlarm(Context context, AlarmManager.AlarmClockInfo info, int userId) {
        String skeleton = DateFormat.is24HourFormat(context, userId) ? "EHm" : "Ehma";
        String pattern = DateFormat.getBestDateTimePattern(Locale.getDefault(), skeleton);
        return info == null ? "" : DateFormat.format(pattern, info.getTriggerTime()).toString();
    }

    void rescheduleKernelAlarmsLocked() {
        long nowElapsed = this.mInjector.getElapsedRealtime();
        long nextNonWakeup = 0;
        if (this.mAlarmStore.size() > 0) {
            long firstWakeup = this.mAlarmStore.getNextWakeupDeliveryTime();
            long first = this.mAlarmStore.getNextDeliveryTime();
            if (firstWakeup != 0) {
                this.mNextWakeup = firstWakeup;
                this.mNextWakeUpSetAt = nowElapsed;
                setLocked(2, firstWakeup);
            }
            if (first != firstWakeup) {
                nextNonWakeup = first;
            }
        }
        if (this.mPendingNonWakeupAlarms.size() > 0 && (nextNonWakeup == 0 || this.mNextNonWakeupDeliveryTime < nextNonWakeup)) {
            nextNonWakeup = this.mNextNonWakeupDeliveryTime;
        }
        if (nextNonWakeup != 0) {
            this.mNextNonWakeup = nextNonWakeup;
            this.mNextNonWakeUpSetAt = nowElapsed;
            setLocked(3, nextNonWakeup);
        }
    }

    void handleChangesToExactAlarmDenyList(ArraySet<String> changedPackages, boolean added) {
        int appOpMode;
        Slog.w(TAG, "Packages " + changedPackages + (added ? " added to" : " removed from") + " the exact alarm deny list.");
        int[] startedUserIds = this.mActivityManagerInternal.getStartedUserIds();
        for (int i = 0; i < changedPackages.size(); i++) {
            String changedPackage = changedPackages.valueAt(i);
            for (int userId : startedUserIds) {
                int uid = this.mPackageManagerInternal.getPackageUid(changedPackage, 0L, userId);
                if (uid > 0 && isExactAlarmChangeEnabled(changedPackage, userId) && !isScheduleExactAlarmDeniedByDefault(changedPackage, userId) && !hasUseExactAlarmInternal(changedPackage, uid) && this.mExactAlarmCandidates.contains(Integer.valueOf(UserHandle.getAppId(uid)))) {
                    synchronized (this.mLock) {
                        appOpMode = this.mLastOpScheduleExactAlarm.get(uid, AppOpsManager.opToDefaultMode(107));
                    }
                    if (appOpMode == 3) {
                        if (added) {
                            removeExactAlarmsOnPermissionRevoked(uid, changedPackage, true);
                        } else {
                            sendScheduleExactAlarmPermissionStateChangedBroadcast(changedPackage, userId);
                        }
                    }
                }
            }
        }
    }

    void removeExactAlarmsOnPermissionRevoked(final int uid, final String packageName, boolean killUid) {
        if (isExemptFromExactAlarmPermissionNoLock(uid) || !isExactAlarmChangeEnabled(packageName, UserHandle.getUserId(uid))) {
            return;
        }
        Slog.w(TAG, "Package " + packageName + ", uid " + uid + " lost permission to set exact alarms!");
        Predicate<Alarm> whichAlarms = new Predicate() { // from class: com.android.server.alarm.AlarmManagerService$$ExternalSyntheticLambda8
            @Override // java.util.function.Predicate
            public final boolean test(Object obj) {
                return AlarmManagerService.lambda$removeExactAlarmsOnPermissionRevoked$14(uid, packageName, (Alarm) obj);
            }
        };
        synchronized (this.mLock) {
            removeAlarmsInternalLocked(whichAlarms, 2);
        }
        if (killUid && this.mConstants.KILL_ON_SCHEDULE_EXACT_ALARM_REVOKED) {
            PermissionManagerService.killUid(UserHandle.getAppId(uid), UserHandle.getUserId(uid), "schedule_exact_alarm revoked");
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static /* synthetic */ boolean lambda$removeExactAlarmsOnPermissionRevoked$14(int uid, String packageName, Alarm a) {
        return a.uid == uid && a.packageName.equals(packageName) && a.windowLength == 0;
    }

    private void removeAlarmsInternalLocked(Predicate<Alarm> whichAlarms, int reason) {
        long nowRtc = this.mInjector.getCurrentTimeMillis();
        long nowElapsed = this.mInjector.getElapsedRealtime();
        ArrayList<Alarm> removedAlarms = this.mAlarmStore.remove(whichAlarms);
        int i = 1;
        boolean removedFromStore = !removedAlarms.isEmpty();
        for (int i2 = this.mPendingBackgroundAlarms.size() - 1; i2 >= 0; i2--) {
            ArrayList<Alarm> alarmsForUid = this.mPendingBackgroundAlarms.valueAt(i2);
            for (int j = alarmsForUid.size() - 1; j >= 0; j--) {
                Alarm alarm = alarmsForUid.get(j);
                if (whichAlarms.test(alarm)) {
                    removedAlarms.add(alarmsForUid.remove(j));
                }
            }
            int j2 = alarmsForUid.size();
            if (j2 == 0) {
                this.mPendingBackgroundAlarms.removeAt(i2);
            }
        }
        for (int i3 = this.mPendingNonWakeupAlarms.size() - 1; i3 >= 0; i3--) {
            Alarm a = this.mPendingNonWakeupAlarms.get(i3);
            if (whichAlarms.test(a)) {
                removedAlarms.add(this.mPendingNonWakeupAlarms.remove(i3));
            }
        }
        Iterator<Alarm> it = removedAlarms.iterator();
        while (it.hasNext()) {
            Alarm removed = it.next();
            decrementAlarmCount(removed.uid, i);
            if (removed.listener != null) {
                removed.listener.asBinder().unlinkToDeath(this.mListenerDeathRecipient, 0);
            }
            if (RemovedAlarm.isLoggable(reason)) {
                RingBuffer<RemovedAlarm> bufferForUid = this.mRemovalHistory.get(removed.uid);
                if (bufferForUid == null) {
                    bufferForUid = new RingBuffer<>(RemovedAlarm.class, 10);
                    this.mRemovalHistory.put(removed.uid, bufferForUid);
                }
                bufferForUid.append(new RemovedAlarm(removed, reason, nowRtc, nowElapsed));
                maybeUnregisterTareListenerLocked(removed);
                removedAlarms = removedAlarms;
                nowRtc = nowRtc;
                i = 1;
            }
        }
        if (removedFromStore) {
            boolean idleUntilUpdated = false;
            Alarm alarm2 = this.mPendingIdleUntil;
            if (alarm2 != null && whichAlarms.test(alarm2)) {
                this.mPendingIdleUntil = null;
                idleUntilUpdated = true;
            }
            Alarm alarm3 = this.mNextWakeFromIdle;
            if (alarm3 != null && whichAlarms.test(alarm3)) {
                this.mNextWakeFromIdle = this.mAlarmStore.getNextWakeFromIdleAlarm();
                if (this.mPendingIdleUntil != null) {
                    idleUntilUpdated |= this.mAlarmStore.updateAlarmDeliveries(new AlarmStore.AlarmDeliveryCalculator() { // from class: com.android.server.alarm.AlarmManagerService$$ExternalSyntheticLambda6
                        @Override // com.android.server.alarm.AlarmStore.AlarmDeliveryCalculator
                        public final boolean updateAlarmDelivery(Alarm alarm4) {
                            return AlarmManagerService.this.m943x7c569643(alarm4);
                        }
                    });
                }
            }
            if (idleUntilUpdated) {
                this.mAlarmStore.updateAlarmDeliveries(new AlarmStore.AlarmDeliveryCalculator() { // from class: com.android.server.alarm.AlarmManagerService$$ExternalSyntheticLambda7
                    @Override // com.android.server.alarm.AlarmStore.AlarmDeliveryCalculator
                    public final boolean updateAlarmDelivery(Alarm alarm4) {
                        return AlarmManagerService.this.m944x35ce23e2(alarm4);
                    }
                });
            }
            rescheduleKernelAlarmsLocked();
            updateNextAlarmClockLocked();
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$removeAlarmsInternalLocked$15$com-android-server-alarm-AlarmManagerService  reason: not valid java name */
    public /* synthetic */ boolean m943x7c569643(Alarm alarm) {
        return alarm == this.mPendingIdleUntil && adjustIdleUntilTime(alarm);
    }

    void removeLocked(final PendingIntent operation, final IAlarmListener directReceiver, int reason) {
        if (operation == null && directReceiver == null) {
            if (localLOGV) {
                Slog.w(TAG, "requested remove() of null operation", new RuntimeException("here"));
                return;
            }
            return;
        }
        removeAlarmsInternalLocked(new Predicate() { // from class: com.android.server.alarm.AlarmManagerService$$ExternalSyntheticLambda2
            @Override // java.util.function.Predicate
            public final boolean test(Object obj) {
                boolean matches;
                matches = ((Alarm) obj).matches(operation, directReceiver);
                return matches;
            }
        }, reason);
    }

    void removeLocked(final int uid, int reason) {
        if (uid == 1000) {
            return;
        }
        removeAlarmsInternalLocked(new Predicate() { // from class: com.android.server.alarm.AlarmManagerService$$ExternalSyntheticLambda17
            @Override // java.util.function.Predicate
            public final boolean test(Object obj) {
                return AlarmManagerService.lambda$removeLocked$18(uid, (Alarm) obj);
            }
        }, reason);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static /* synthetic */ boolean lambda$removeLocked$18(int uid, Alarm a) {
        return a.uid == uid;
    }

    void removeLocked(final String packageName) {
        if (packageName == null) {
            if (localLOGV) {
                Slog.w(TAG, "requested remove() of null packageName", new RuntimeException("here"));
                return;
            }
            return;
        }
        removeAlarmsInternalLocked(new Predicate() { // from class: com.android.server.alarm.AlarmManagerService$$ExternalSyntheticLambda11
            @Override // java.util.function.Predicate
            public final boolean test(Object obj) {
                boolean matches;
                matches = ((Alarm) obj).matches(packageName);
                return matches;
            }
        }, 0);
    }

    void removeForStoppedLocked(final int uid) {
        if (uid == 1000) {
            return;
        }
        Predicate<Alarm> whichAlarms = new Predicate() { // from class: com.android.server.alarm.AlarmManagerService$$ExternalSyntheticLambda3
            @Override // java.util.function.Predicate
            public final boolean test(Object obj) {
                return AlarmManagerService.this.m945x7fe462ce(uid, (Alarm) obj);
            }
        };
        removeAlarmsInternalLocked(whichAlarms, 0);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$removeForStoppedLocked$20$com-android-server-alarm-AlarmManagerService  reason: not valid java name */
    public /* synthetic */ boolean m945x7fe462ce(int uid, Alarm a) {
        return a.uid == uid && this.mActivityManagerInternal.isAppStartModeDisabled(uid, a.packageName);
    }

    void removeUserLocked(final int userHandle) {
        if (userHandle == 0) {
            Slog.w(TAG, "Ignoring attempt to remove system-user state!");
            return;
        }
        Predicate<Alarm> whichAlarms = new Predicate() { // from class: com.android.server.alarm.AlarmManagerService$$ExternalSyntheticLambda4
            @Override // java.util.function.Predicate
            public final boolean test(Object obj) {
                return AlarmManagerService.lambda$removeUserLocked$21(userHandle, (Alarm) obj);
            }
        };
        removeAlarmsInternalLocked(whichAlarms, 0);
        for (int i = this.mLastPriorityAlarmDispatch.size() - 1; i >= 0; i--) {
            if (UserHandle.getUserId(this.mLastPriorityAlarmDispatch.keyAt(i)) == userHandle) {
                this.mLastPriorityAlarmDispatch.removeAt(i);
            }
        }
        for (int i2 = this.mRemovalHistory.size() - 1; i2 >= 0; i2--) {
            if (UserHandle.getUserId(this.mRemovalHistory.keyAt(i2)) == userHandle) {
                this.mRemovalHistory.removeAt(i2);
            }
        }
        for (int i3 = this.mLastOpScheduleExactAlarm.size() - 1; i3 >= 0; i3--) {
            if (UserHandle.getUserId(this.mLastOpScheduleExactAlarm.keyAt(i3)) == userHandle) {
                this.mLastOpScheduleExactAlarm.removeAt(i3);
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static /* synthetic */ boolean lambda$removeUserLocked$21(int userHandle, Alarm a) {
        return UserHandle.getUserId(a.uid) == userHandle;
    }

    void interactiveStateChangedLocked(boolean interactive) {
        if (this.mInteractive != interactive) {
            this.mInteractive = interactive;
            long nowELAPSED = this.mInjector.getElapsedRealtime();
            if (interactive) {
                if (this.mPendingNonWakeupAlarms.size() > 0) {
                    long thisDelayTime = nowELAPSED - this.mStartCurrentDelayTime;
                    this.mTotalDelayTime += thisDelayTime;
                    if (this.mMaxDelayTime < thisDelayTime) {
                        this.mMaxDelayTime = thisDelayTime;
                    }
                    ArrayList<Alarm> triggerList = new ArrayList<>(this.mPendingNonWakeupAlarms);
                    deliverAlarmsLocked(triggerList, nowELAPSED);
                    this.mPendingNonWakeupAlarms.clear();
                }
                long thisDelayTime2 = this.mNonInteractiveStartTime;
                if (thisDelayTime2 > 0) {
                    long dur = nowELAPSED - thisDelayTime2;
                    if (dur > this.mNonInteractiveTime) {
                        this.mNonInteractiveTime = dur;
                    }
                }
                this.mHandler.post(new Runnable() { // from class: com.android.server.alarm.AlarmManagerService$$ExternalSyntheticLambda19
                    @Override // java.lang.Runnable
                    public final void run() {
                        AlarmManagerService.this.m936xd6df192f();
                    }
                });
                return;
            }
            this.mNonInteractiveStartTime = nowELAPSED;
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$interactiveStateChangedLocked$22$com-android-server-alarm-AlarmManagerService  reason: not valid java name */
    public /* synthetic */ void m936xd6df192f() {
        getContext().sendBroadcastAsUser(this.mTimeTickIntent, UserHandle.ALL);
    }

    boolean lookForPackageLocked(String packageName) {
        ArrayList<Alarm> allAlarms = this.mAlarmStore.asList();
        Iterator<Alarm> it = allAlarms.iterator();
        while (it.hasNext()) {
            Alarm alarm = it.next();
            if (alarm.matches(packageName)) {
                return true;
            }
        }
        return false;
    }

    private void setLocked(int type, long when) {
        if (this.mInjector.isAlarmDriverPresent()) {
            this.mInjector.setAlarm(type, when);
            return;
        }
        Message msg = Message.obtain();
        msg.what = 1;
        this.mHandler.removeMessages(msg.what);
        this.mHandler.sendMessageAtTime(msg, when);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static final void dumpAlarmList(IndentingPrintWriter ipw, ArrayList<Alarm> list, long nowELAPSED, SimpleDateFormat sdf) {
        int n = list.size();
        for (int i = n - 1; i >= 0; i--) {
            Alarm a = list.get(i);
            String label = Alarm.typeToString(a.type);
            ipw.print(label);
            ipw.print(" #");
            ipw.print(n - i);
            ipw.print(": ");
            ipw.println(a);
            ipw.increaseIndent();
            a.dump(ipw, nowELAPSED, sdf);
            ipw.decreaseIndent();
        }
    }

    private static boolean isExemptFromBatterySaver(Alarm alarm) {
        if (alarm.alarmClock != null) {
            return true;
        }
        return (alarm.operation != null && (alarm.operation.isActivity() || alarm.operation.isForegroundService())) || UserHandle.isCore(alarm.creatorUid);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public boolean isBackgroundRestricted(Alarm alarm) {
        AppStateTrackerImpl appStateTrackerImpl;
        if (alarm.alarmClock != null) {
            return false;
        }
        if (alarm.operation == null || !alarm.operation.isActivity()) {
            String sourcePackage = alarm.sourcePackage;
            int sourceUid = alarm.creatorUid;
            return (UserHandle.isCore(sourceUid) || (appStateTrackerImpl = this.mAppStateTracker) == null || !appStateTrackerImpl.areAlarmsRestricted(sourceUid, sourcePackage)) ? false : true;
        }
        return false;
    }

    int triggerAlarmsLocked(ArrayList<Alarm> triggerList, long nowELAPSED) {
        Alarm alarm;
        AlarmManagerService alarmManagerService;
        final AlarmManagerService alarmManagerService2 = this;
        ArrayList<Alarm> arrayList = triggerList;
        long j = nowELAPSED;
        ArrayList<Alarm> pendingAlarms = alarmManagerService2.mAlarmStore.removePendingAlarms(j);
        Iterator<Alarm> it = pendingAlarms.iterator();
        int wakeUps = 0;
        while (it.hasNext()) {
            Alarm alarm2 = it.next();
            if (alarmManagerService2.isBackgroundRestricted(alarm2)) {
                if (DEBUG_BG_LIMIT) {
                    Slog.d(TAG, "Deferring alarm " + alarm2 + " due to user forced app standby");
                }
                ArrayList<Alarm> alarmsForUid = alarmManagerService2.mPendingBackgroundAlarms.get(alarm2.creatorUid);
                if (alarmsForUid == null) {
                    alarmsForUid = new ArrayList<>();
                    alarmManagerService2.mPendingBackgroundAlarms.put(alarm2.creatorUid, alarmsForUid);
                }
                alarmsForUid.add(alarm2);
            } else {
                alarm2.count = 1;
                arrayList.add(alarm2);
                if ((alarm2.flags & 2) != 0) {
                    EventLogTags.writeDeviceIdleWakeFromIdle(alarmManagerService2.mPendingIdleUntil != null ? 1 : 0, alarm2.statsTag);
                }
                if (alarmManagerService2.mPendingIdleUntil == alarm2) {
                    alarmManagerService2.mPendingIdleUntil = null;
                    alarmManagerService2.mAlarmStore.updateAlarmDeliveries(new AlarmStore.AlarmDeliveryCalculator() { // from class: com.android.server.alarm.AlarmManagerService$$ExternalSyntheticLambda18
                        @Override // com.android.server.alarm.AlarmStore.AlarmDeliveryCalculator
                        public final boolean updateAlarmDelivery(Alarm alarm3) {
                            return AlarmManagerService.this.m951x4dd46403(alarm3);
                        }
                    });
                    IdleDispatchEntry ent = new IdleDispatchEntry();
                    ent.uid = alarm2.uid;
                    ent.pkg = alarm2.sourcePackage;
                    ent.tag = alarm2.statsTag;
                    ent.op = "END IDLE";
                    ent.elapsedRealtime = alarmManagerService2.mInjector.getElapsedRealtime();
                    ent.argRealtime = alarm2.getWhenElapsed();
                    alarmManagerService2.mAllowWhileIdleDispatches.add(ent);
                }
                if (alarmManagerService2.mNextWakeFromIdle == alarm2) {
                    alarmManagerService2.mNextWakeFromIdle = alarmManagerService2.mAlarmStore.getNextWakeFromIdleAlarm();
                }
                if (alarm2.repeatInterval <= 0) {
                    alarm = alarm2;
                } else {
                    alarm2.count = (int) (alarm2.count + ((j - alarm2.getRequestedElapsed()) / alarm2.repeatInterval));
                    long delta = alarm2.count * alarm2.repeatInterval;
                    long nextElapsed = alarm2.getRequestedElapsed() + delta;
                    long nextMaxElapsed = maxTriggerTime(nowELAPSED, nextElapsed, alarm2.repeatInterval);
                    alarm = alarm2;
                    setImplLocked(alarm2.type, alarm2.origWhen + delta, nextElapsed, nextMaxElapsed - nextElapsed, alarm2.repeatInterval, alarm2.operation, null, null, alarm2.flags, alarm2.workSource, alarm2.alarmClock, alarm2.uid, alarm2.packageName, null, -1);
                }
                Alarm alarm3 = alarm;
                if (alarm3.wakeup) {
                    wakeUps++;
                }
                if (alarm3.alarmClock == null) {
                    alarmManagerService = this;
                } else {
                    alarmManagerService = this;
                    alarmManagerService.mNextAlarmClockMayChange = true;
                }
                arrayList = triggerList;
                j = nowELAPSED;
                alarmManagerService2 = alarmManagerService;
            }
        }
        AlarmManagerService alarmManagerService3 = alarmManagerService2;
        alarmManagerService3.mCurrentSeq++;
        calculateDeliveryPriorities(triggerList);
        Collections.sort(triggerList, alarmManagerService3.mAlarmDispatchComparator);
        if (localLOGV) {
            for (int i = 0; i < triggerList.size(); i++) {
                Slog.v(TAG, "Triggering alarm #" + i + ": " + triggerList.get(i));
            }
        }
        return wakeUps;
    }

    long currentNonWakeupFuzzLocked(long nowELAPSED) {
        long timeSinceOn = nowELAPSED - this.mNonInteractiveStartTime;
        if (timeSinceOn < BackupAgentTimeoutParameters.DEFAULT_FULL_BACKUP_AGENT_TIMEOUT_MILLIS) {
            return 120000L;
        }
        if (timeSinceOn < 1800000) {
            return 900000L;
        }
        return 3600000L;
    }

    boolean checkAllowNonWakeupDelayLocked(long nowELAPSED) {
        if (!this.mInteractive && this.mLastAlarmDeliveryTime > 0) {
            if (this.mPendingNonWakeupAlarms.size() <= 0 || this.mNextNonWakeupDeliveryTime >= nowELAPSED) {
                long timeSinceLast = nowELAPSED - this.mLastAlarmDeliveryTime;
                return timeSinceLast <= currentNonWakeupFuzzLocked(nowELAPSED);
            }
            return false;
        }
        return false;
    }

    /* JADX WARN: Removed duplicated region for block: B:39:0x0111 A[Catch: RuntimeException -> 0x0149, TryCatch #2 {RuntimeException -> 0x0149, blocks: (B:37:0x010d, B:39:0x0111, B:40:0x0128, B:42:0x0145), top: B:53:0x010d }] */
    /* JADX WARN: Removed duplicated region for block: B:42:0x0145 A[Catch: RuntimeException -> 0x0149, TRY_LEAVE, TryCatch #2 {RuntimeException -> 0x0149, blocks: (B:37:0x010d, B:39:0x0111, B:40:0x0128, B:42:0x0145), top: B:53:0x010d }] */
    /*
        Code decompiled incorrectly, please refer to instructions dump.
    */
    void deliverAlarmsLocked(ArrayList<Alarm> triggerList, long nowELAPSED) {
        int i;
        this.mLastAlarmDeliveryTime = nowELAPSED;
        int i2 = 0;
        while (i2 < triggerList.size()) {
            Alarm alarm = triggerList.get(i2);
            if (alarm.wakeup) {
                Trace.traceBegin(131072L, "Dispatch wakeup alarm to " + alarm.packageName);
            } else {
                Trace.traceBegin(131072L, "Dispatch non-wakeup alarm to " + alarm.packageName);
            }
            if (sTranSlmSupport && this.mScreenOff && !this.mNetworkConnected && isGmsApplication(alarm) && !isIdleFlag(alarm)) {
                if (sAlarmDebug) {
                    Slog.d(TAG, "packageName :" + alarm.packageName + ",alarm was removed by slm");
                }
                i = i2 - 1;
                triggerList.remove(i2);
                decrementAlarmCount(alarm.uid, 1);
            } else {
                try {
                    UserManager userManager = (UserManager) getContext().getSystemService("user");
                    int userId = UserHandle.getUserId(alarm.creatorUid);
                    ActivityManager activityManager = (ActivityManager) getContext().getSystemService(HostingRecord.HOSTING_TYPE_ACTIVITY);
                    int currentUserId = ActivityManager.getCurrentUser();
                    UserInfo userInfo = userManager.getUserInfo(userId);
                    if ("com.transsion.deskclock".equals(alarm.packageName) && userInfo.isGuest() && currentUserId != userId) {
                        Slog.d(TAG, "remove alarm:" + alarm.packageName + " " + alarm.uid);
                        int i3 = i2 - 1;
                        try {
                            triggerList.remove(i2);
                            decrementAlarmCount(alarm.uid, 1);
                            i = i3;
                        } catch (RuntimeException e) {
                            e = e;
                            i2 = i3;
                            Slog.w(TAG, "failed", e);
                            if (localLOGV) {
                            }
                            this.mActivityManagerInternal.noteAlarmStart(alarm.operation, alarm.workSource, alarm.uid, alarm.statsTag);
                            this.mDeliveryTracker.deliverLocked(alarm, nowELAPSED);
                            reportAlarmEventToTare(alarm);
                            if (alarm.repeatInterval <= 0) {
                            }
                            Trace.traceEnd(131072L);
                            decrementAlarmCount(alarm.uid, 1);
                            i = i2;
                            i2 = i + 1;
                        }
                    }
                } catch (RuntimeException e2) {
                    e = e2;
                }
                try {
                    if (localLOGV) {
                        Slog.v(TAG, "sending alarm " + alarm);
                    }
                    this.mActivityManagerInternal.noteAlarmStart(alarm.operation, alarm.workSource, alarm.uid, alarm.statsTag);
                    this.mDeliveryTracker.deliverLocked(alarm, nowELAPSED);
                    reportAlarmEventToTare(alarm);
                    if (alarm.repeatInterval <= 0) {
                        maybeUnregisterTareListenerLocked(alarm);
                    }
                } catch (RuntimeException e3) {
                    Slog.w(TAG, "Failure sending alarm.", e3);
                }
                Trace.traceEnd(131072L);
                decrementAlarmCount(alarm.uid, 1);
                i = i2;
            }
            i2 = i + 1;
        }
    }

    private void reportAlarmEventToTare(Alarm alarm) {
        int action;
        if (!this.mConstants.USE_TARE_POLICY) {
            return;
        }
        boolean allowWhileIdle = (alarm.flags & 12) != 0;
        if (alarm.alarmClock != null) {
            action = AlarmManagerEconomicPolicy.ACTION_ALARM_CLOCK;
        } else if (alarm.wakeup) {
            if (alarm.windowLength == 0) {
                if (allowWhileIdle) {
                    action = 1073741824;
                } else {
                    action = 1073741825;
                }
            } else if (allowWhileIdle) {
                action = AlarmManagerEconomicPolicy.ACTION_ALARM_WAKEUP_INEXACT_ALLOW_WHILE_IDLE;
            } else {
                action = AlarmManagerEconomicPolicy.ACTION_ALARM_WAKEUP_INEXACT;
            }
        } else if (alarm.windowLength == 0) {
            if (allowWhileIdle) {
                action = AlarmManagerEconomicPolicy.ACTION_ALARM_NONWAKEUP_EXACT_ALLOW_WHILE_IDLE;
            } else {
                action = AlarmManagerEconomicPolicy.ACTION_ALARM_NONWAKEUP_EXACT;
            }
        } else if (allowWhileIdle) {
            action = AlarmManagerEconomicPolicy.ACTION_ALARM_NONWAKEUP_INEXACT_ALLOW_WHILE_IDLE;
        } else {
            action = AlarmManagerEconomicPolicy.ACTION_ALARM_NONWAKEUP_INEXACT;
        }
        this.mEconomyManagerInternal.noteInstantaneousEvent(UserHandle.getUserId(alarm.creatorUid), alarm.sourcePackage, action, null);
    }

    static boolean isExemptFromAppStandby(Alarm a) {
        return (a.alarmClock == null && !UserHandle.isCore(a.creatorUid) && (a.flags & 12) == 0) ? false : true;
    }

    static boolean isExemptFromTare(Alarm a) {
        return (a.alarmClock == null && !UserHandle.isCore(a.creatorUid) && (a.flags & 8) == 0) ? false : true;
    }

    /* JADX INFO: Access modifiers changed from: protected */
    /* loaded from: classes.dex */
    public static class Injector {
        private Context mContext;
        private long mNativeData;

        Injector(Context context) {
            this.mContext = context;
        }

        void init() {
            System.loadLibrary("alarm_jni");
            this.mNativeData = AlarmManagerService.m930$$Nest$sminit();
        }

        int waitForAlarm() {
            return AlarmManagerService.waitForAlarm(this.mNativeData);
        }

        public long getNativeData() {
            return this.mNativeData;
        }

        boolean isAlarmDriverPresent() {
            return this.mNativeData != 0;
        }

        void setAlarm(int type, long millis) {
            long alarmSeconds;
            long alarmSeconds2;
            if (millis < 0) {
                alarmSeconds = 0;
                alarmSeconds2 = 0;
            } else {
                long alarmSeconds3 = millis / 1000;
                alarmSeconds = alarmSeconds3;
                alarmSeconds2 = 1000 * (millis % 1000) * 1000;
            }
            int result = AlarmManagerService.set(this.mNativeData, type, alarmSeconds, alarmSeconds2);
            if (result != 0) {
                long nowElapsed = SystemClock.elapsedRealtime();
                Slog.wtf(AlarmManagerService.TAG, "Unable to set kernel alarm, now=" + nowElapsed + " type=" + type + " @ (" + alarmSeconds + "," + alarmSeconds2 + "), ret = " + result + " = " + Os.strerror(result));
            }
        }

        int getCallingUid() {
            return Binder.getCallingUid();
        }

        long getNextAlarm(int type) {
            return AlarmManagerService.getNextAlarm(this.mNativeData, type);
        }

        void setKernelTimezone(int minutesWest) {
            AlarmManagerService.setKernelTimezone(this.mNativeData, minutesWest);
        }

        void setKernelTime(long millis) {
            long j = this.mNativeData;
            if (j != 0) {
                AlarmManagerService.setKernelTime(j, millis);
            }
        }

        void close() {
            AlarmManagerService.close(this.mNativeData);
        }

        long getElapsedRealtime() {
            return SystemClock.elapsedRealtime();
        }

        long getCurrentTimeMillis() {
            return System.currentTimeMillis();
        }

        PowerManager.WakeLock getAlarmWakeLock() {
            PowerManager pm = (PowerManager) this.mContext.getSystemService("power");
            return pm.newWakeLock(1, "*alarm*");
        }

        int getSystemUiUid(PackageManagerInternal pm) {
            return pm.getPackageUid(pm.getSystemUiServiceComponent().getPackageName(), 1048576L, 0);
        }

        IAppOpsService getAppOpsService() {
            return IAppOpsService.Stub.asInterface(ServiceManager.getService("appops"));
        }

        ClockReceiver getClockReceiver(AlarmManagerService service) {
            Objects.requireNonNull(service);
            return new ClockReceiver();
        }

        void registerContentObserver(ContentObserver contentObserver, Uri uri) {
            this.mContext.getContentResolver().registerContentObserver(uri, false, contentObserver);
        }

        void registerDeviceConfigListener(DeviceConfig.OnPropertiesChangedListener listener) {
            DeviceConfig.addOnPropertiesChangedListener("alarm_manager", JobSchedulerBackgroundThread.getExecutor(), listener);
        }
    }

    /* loaded from: classes.dex */
    private class AlarmThread extends Thread {
        private int mFalseWakeups;
        private int mWtfThreshold;

        AlarmThread() {
            super(AlarmManagerService.TAG);
            this.mFalseWakeups = 0;
            this.mWtfThreshold = 100;
        }

        /* JADX WARN: Removed duplicated region for block: B:38:0x010a  */
        /* JADX WARN: Removed duplicated region for block: B:89:0x0294  */
        @Override // java.lang.Thread, java.lang.Runnable
        /*
            Code decompiled incorrectly, please refer to instructions dump.
        */
        public void run() {
            int result;
            long lastTimeChangeClockTime;
            long expectedClockTime;
            ArrayList<Alarm> triggerList = new ArrayList<>();
            while (true) {
                int result2 = AlarmManagerService.this.mInjector.waitForAlarm();
                long nowRTC = AlarmManagerService.this.mInjector.getCurrentTimeMillis();
                long nowELAPSED = AlarmManagerService.this.mInjector.getElapsedRealtime();
                synchronized (AlarmManagerService.this.mLock) {
                    AlarmManagerService.this.mLastWakeup = nowELAPSED;
                }
                if (result2 == 0) {
                    Slog.wtf(AlarmManagerService.TAG, "waitForAlarm returned 0, nowRTC = " + nowRTC + ", nowElapsed = " + nowELAPSED);
                }
                triggerList.clear();
                if ((result2 & 65536) != 0) {
                    synchronized (AlarmManagerService.this.mLock) {
                        lastTimeChangeClockTime = AlarmManagerService.this.mLastTimeChangeClockTime;
                        expectedClockTime = (nowELAPSED - AlarmManagerService.this.mLastTimeChangeRealtime) + lastTimeChangeClockTime;
                    }
                    if (lastTimeChangeClockTime == 0 || nowRTC < expectedClockTime - 1000 || nowRTC > 1000 + expectedClockTime) {
                        if (AlarmManagerService.DEBUG_BATCH) {
                            Slog.v(AlarmManagerService.TAG, "Time changed notification from kernel; rebatching");
                        }
                        FrameworkStatsLog.write(45, nowRTC);
                        AlarmManagerService alarmManagerService = AlarmManagerService.this;
                        alarmManagerService.removeImpl(null, alarmManagerService.mTimeTickTrigger);
                        AlarmManagerService alarmManagerService2 = AlarmManagerService.this;
                        alarmManagerService2.removeImpl(alarmManagerService2.mDateChangeSender, null);
                        AlarmManagerService.this.reevaluateRtcAlarms();
                        AlarmManagerService.this.mClockReceiver.scheduleTimeTickEvent();
                        AlarmManagerService.this.mClockReceiver.scheduleDateChangedEvent();
                        synchronized (AlarmManagerService.this.mLock) {
                            AlarmManagerService.this.mNumTimeChanged++;
                            AlarmManagerService.this.mLastTimeChangeClockTime = nowRTC;
                            AlarmManagerService.this.mLastTimeChangeRealtime = nowELAPSED;
                        }
                        Intent intent = new Intent("android.intent.action.TIME_SET");
                        intent.addFlags(622854144);
                        AlarmManagerService.this.mOptsTimeBroadcast.setTemporaryAppAllowlist(AlarmManagerService.this.mActivityManagerInternal.getBootTimeTempAllowListDuration(), 0, 205, "");
                        AlarmManagerService.this.getContext().sendBroadcastAsUser(intent, UserHandle.ALL, null, AlarmManagerService.this.mOptsTimeBroadcast.toBundle());
                        result = result2 | 5;
                        if (result == 65536) {
                            synchronized (AlarmManagerService.this.mLock) {
                                if (AlarmManagerService.localLOGV) {
                                    Slog.v(AlarmManagerService.TAG, "Checking for alarms... rtc=" + nowRTC + ", elapsed=" + nowELAPSED);
                                }
                                AlarmManagerService.this.mLastTrigger = nowELAPSED;
                                int wakeUps = AlarmManagerService.this.triggerAlarmsLocked(triggerList, nowELAPSED);
                                if (wakeUps == 0 && AlarmManagerService.this.checkAllowNonWakeupDelayLocked(nowELAPSED)) {
                                    if (AlarmManagerService.this.mPendingNonWakeupAlarms.size() == 0) {
                                        AlarmManagerService.this.mStartCurrentDelayTime = nowELAPSED;
                                        AlarmManagerService alarmManagerService3 = AlarmManagerService.this;
                                        alarmManagerService3.mNextNonWakeupDeliveryTime = ((alarmManagerService3.currentNonWakeupFuzzLocked(nowELAPSED) * 3) / 2) + nowELAPSED;
                                    }
                                    AlarmManagerService.this.mPendingNonWakeupAlarms.addAll(triggerList);
                                    AlarmManagerService.this.mNumDelayedAlarms += triggerList.size();
                                    AlarmManagerService.this.rescheduleKernelAlarmsLocked();
                                    AlarmManagerService.this.updateNextAlarmClockLocked();
                                } else {
                                    if (AlarmManagerService.this.mPendingNonWakeupAlarms.size() > 0) {
                                        AlarmManagerService alarmManagerService4 = AlarmManagerService.this;
                                        alarmManagerService4.calculateDeliveryPriorities(alarmManagerService4.mPendingNonWakeupAlarms);
                                        triggerList.addAll(AlarmManagerService.this.mPendingNonWakeupAlarms);
                                        Collections.sort(triggerList, AlarmManagerService.this.mAlarmDispatchComparator);
                                        long thisDelayTime = nowELAPSED - AlarmManagerService.this.mStartCurrentDelayTime;
                                        AlarmManagerService.this.mTotalDelayTime += thisDelayTime;
                                        if (AlarmManagerService.this.mMaxDelayTime < thisDelayTime) {
                                            AlarmManagerService.this.mMaxDelayTime = thisDelayTime;
                                        }
                                        AlarmManagerService.this.mPendingNonWakeupAlarms.clear();
                                    }
                                    if (AlarmManagerService.this.mLastTimeChangeRealtime != nowELAPSED && triggerList.isEmpty()) {
                                        int i = this.mFalseWakeups + 1;
                                        this.mFalseWakeups = i;
                                        if (i >= this.mWtfThreshold) {
                                            Slog.wtf(AlarmManagerService.TAG, "Too many (" + this.mFalseWakeups + ") false wakeups, nowElapsed=" + nowELAPSED);
                                            int i2 = this.mWtfThreshold;
                                            if (i2 < 100000) {
                                                this.mWtfThreshold = i2 * 10;
                                            } else {
                                                this.mFalseWakeups = 0;
                                            }
                                        }
                                    }
                                    ArraySet<Pair<String, Integer>> triggerPackages = new ArraySet<>();
                                    for (int i3 = 0; i3 < triggerList.size(); i3++) {
                                        Alarm a = triggerList.get(i3);
                                        if (AlarmManagerService.this.mConstants.USE_TARE_POLICY) {
                                            if (!AlarmManagerService.isExemptFromTare(a)) {
                                                triggerPackages.add(Pair.create(a.sourcePackage, Integer.valueOf(UserHandle.getUserId(a.creatorUid))));
                                            }
                                        } else if (!AlarmManagerService.isExemptFromAppStandby(a)) {
                                            triggerPackages.add(Pair.create(a.sourcePackage, Integer.valueOf(UserHandle.getUserId(a.creatorUid))));
                                        }
                                    }
                                    AlarmManagerService.this.deliverAlarmsLocked(triggerList, nowELAPSED);
                                    AlarmManagerService.this.mTemporaryQuotaReserve.cleanUpExpiredQuotas(nowELAPSED);
                                    if (AlarmManagerService.this.mConstants.USE_TARE_POLICY) {
                                        AlarmManagerService.this.reorderAlarmsBasedOnTare(triggerPackages);
                                    } else {
                                        AlarmManagerService.this.reorderAlarmsBasedOnStandbyBuckets(triggerPackages);
                                    }
                                    AlarmManagerService.this.rescheduleKernelAlarmsLocked();
                                    AlarmManagerService.this.updateNextAlarmClockLocked();
                                    MetricsHelper.pushAlarmBatchDelivered(triggerList.size(), wakeUps);
                                }
                            }
                        } else {
                            synchronized (AlarmManagerService.this.mLock) {
                                AlarmManagerService.this.rescheduleKernelAlarmsLocked();
                            }
                        }
                    }
                }
                result = result2;
                if (result == 65536) {
                }
            }
        }
    }

    void setWakelockWorkSource(WorkSource ws, int knownUid, String tag, boolean first) {
        try {
            this.mWakeLock.setHistoryTag(first ? tag : null);
        } catch (Exception e) {
        }
        if (ws != null) {
            this.mWakeLock.setWorkSource(ws);
            return;
        }
        if (knownUid >= 0) {
            this.mWakeLock.setWorkSource(new WorkSource(knownUid));
            return;
        }
        this.mWakeLock.setWorkSource(null);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public static int getAlarmAttributionUid(Alarm alarm) {
        if (alarm.workSource != null && !alarm.workSource.isEmpty()) {
            return alarm.workSource.getAttributionUid();
        }
        return alarm.creatorUid;
    }

    private boolean canAffordBillLocked(Alarm alarm, EconomyManagerInternal.ActionBill bill) {
        int userId = UserHandle.getUserId(alarm.creatorUid);
        String pkgName = alarm.sourcePackage;
        ArrayMap<EconomyManagerInternal.ActionBill, Boolean> actionAffordability = (ArrayMap) this.mAffordabilityCache.get(userId, pkgName);
        if (actionAffordability == null) {
            actionAffordability = new ArrayMap<>();
            this.mAffordabilityCache.add(userId, pkgName, actionAffordability);
        }
        if (actionAffordability.containsKey(bill)) {
            return actionAffordability.get(bill).booleanValue();
        }
        boolean canAfford = this.mEconomyManagerInternal.canPayFor(userId, pkgName, bill);
        actionAffordability.put(bill, Boolean.valueOf(canAfford));
        return canAfford;
    }

    private boolean hasEnoughWealthLocked(Alarm alarm) {
        return canAffordBillLocked(alarm, TareBill.getAppropriateBill(alarm));
    }

    /* JADX INFO: Access modifiers changed from: private */
    public Bundle getAlarmOperationBundle(Alarm alarm) {
        if (alarm.mIdleOptions != null) {
            return alarm.mIdleOptions;
        }
        if (alarm.operation.isActivity()) {
            return this.mActivityOptsRestrictBal.toBundle();
        }
        return this.mBroadcastOptsRestrictBal.toBundle();
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* loaded from: classes.dex */
    public class AlarmHandler extends Handler {
        public static final int ALARM_EVENT = 1;
        public static final int APP_STANDBY_BUCKET_CHANGED = 5;
        public static final int CHARGING_STATUS_CHANGED = 6;
        public static final int CHECK_EXACT_ALARM_PERMISSION_ON_UPDATE = 13;
        public static final int EXACT_ALARM_DENY_LIST_PACKAGES_ADDED = 9;
        public static final int EXACT_ALARM_DENY_LIST_PACKAGES_REMOVED = 10;
        public static final int LISTENER_TIMEOUT = 3;
        public static final int REFRESH_EXACT_ALARM_CANDIDATES = 11;
        public static final int REMOVE_EXACT_ALARMS = 8;
        public static final int REMOVE_FOR_CANCELED = 7;
        public static final int REPORT_ALARMS_ACTIVE = 4;
        public static final int SEND_NEXT_ALARM_CLOCK_CHANGED = 2;
        public static final int TARE_AFFORDABILITY_CHANGED = 12;
        public static final int TEMPORARY_QUOTA_CHANGED = 14;

        AlarmHandler() {
            super(Looper.myLooper());
        }

        @Override // android.os.Handler
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case 1:
                    ArrayList<Alarm> triggerList = new ArrayList<>();
                    synchronized (AlarmManagerService.this.mLock) {
                        long nowELAPSED = AlarmManagerService.this.mInjector.getElapsedRealtime();
                        AlarmManagerService.this.triggerAlarmsLocked(triggerList, nowELAPSED);
                        AlarmManagerService.this.updateNextAlarmClockLocked();
                    }
                    for (int i = 0; i < triggerList.size(); i++) {
                        Alarm alarm = triggerList.get(i);
                        try {
                            Bundle bundle = AlarmManagerService.this.getAlarmOperationBundle(alarm);
                            alarm.operation.send(null, 0, null, null, null, null, bundle);
                        } catch (PendingIntent.CanceledException e) {
                            if (alarm.repeatInterval > 0) {
                                AlarmManagerService.this.removeImpl(alarm.operation, null);
                            }
                        }
                        AlarmManagerService.this.decrementAlarmCount(alarm.uid, 1);
                    }
                    return;
                case 2:
                    AlarmManagerService.this.sendNextAlarmClockChanged();
                    return;
                case 3:
                    AlarmManagerService.this.mDeliveryTracker.alarmTimedOut((IBinder) msg.obj);
                    return;
                case 4:
                    if (AlarmManagerService.this.mLocalDeviceIdleController != null) {
                        AlarmManagerService.this.mLocalDeviceIdleController.setAlarmsActive(msg.arg1 != 0);
                        return;
                    }
                    return;
                case 5:
                case 14:
                    synchronized (AlarmManagerService.this.mLock) {
                        ArraySet<Pair<String, Integer>> filterPackages = new ArraySet<>();
                        filterPackages.add(Pair.create((String) msg.obj, Integer.valueOf(msg.arg1)));
                        if (AlarmManagerService.this.reorderAlarmsBasedOnStandbyBuckets(filterPackages)) {
                            AlarmManagerService.this.rescheduleKernelAlarmsLocked();
                            AlarmManagerService.this.updateNextAlarmClockLocked();
                        }
                    }
                    return;
                case 6:
                    synchronized (AlarmManagerService.this.mLock) {
                        AlarmManagerService.this.mAppStandbyParole = ((Boolean) msg.obj).booleanValue();
                        if (AlarmManagerService.this.reorderAlarmsBasedOnStandbyBuckets(null)) {
                            AlarmManagerService.this.rescheduleKernelAlarmsLocked();
                            AlarmManagerService.this.updateNextAlarmClockLocked();
                        }
                    }
                    return;
                case 7:
                    PendingIntent operation = (PendingIntent) msg.obj;
                    synchronized (AlarmManagerService.this.mLock) {
                        AlarmManagerService.this.removeLocked(operation, null, 4);
                    }
                    return;
                case 8:
                    int uid = msg.arg1;
                    String packageName = (String) msg.obj;
                    AlarmManagerService.this.removeExactAlarmsOnPermissionRevoked(uid, packageName, true);
                    return;
                case 9:
                    AlarmManagerService.this.handleChangesToExactAlarmDenyList((ArraySet) msg.obj, true);
                    return;
                case 10:
                    AlarmManagerService.this.handleChangesToExactAlarmDenyList((ArraySet) msg.obj, false);
                    return;
                case 11:
                    AlarmManagerService.this.refreshExactAlarmCandidates();
                    return;
                case 12:
                    synchronized (AlarmManagerService.this.mLock) {
                        int userId = msg.arg1;
                        String packageName2 = (String) msg.obj;
                        ArraySet<Pair<String, Integer>> filterPackages2 = new ArraySet<>();
                        filterPackages2.add(Pair.create(packageName2, Integer.valueOf(userId)));
                        if (AlarmManagerService.this.reorderAlarmsBasedOnTare(filterPackages2)) {
                            AlarmManagerService.this.rescheduleKernelAlarmsLocked();
                            AlarmManagerService.this.updateNextAlarmClockLocked();
                        }
                    }
                    return;
                case 13:
                    String packageName3 = (String) msg.obj;
                    int uid2 = msg.arg1;
                    if (!AlarmManagerService.this.hasScheduleExactAlarmInternal(packageName3, uid2) && !AlarmManagerService.this.hasUseExactAlarmInternal(packageName3, uid2)) {
                        AlarmManagerService.this.removeExactAlarmsOnPermissionRevoked(uid2, packageName3, false);
                        return;
                    }
                    return;
                default:
                    return;
            }
        }
    }

    /* loaded from: classes.dex */
    class ChargingReceiver extends BroadcastReceiver {
        ChargingReceiver() {
            IntentFilter filter = new IntentFilter();
            filter.addAction("android.os.action.CHARGING");
            filter.addAction("android.os.action.DISCHARGING");
            AlarmManagerService.this.getContext().registerReceiver(this, filter);
        }

        @Override // android.content.BroadcastReceiver
        public void onReceive(Context context, Intent intent) {
            boolean charging;
            String action = intent.getAction();
            if ("android.os.action.CHARGING".equals(action)) {
                if (AlarmManagerService.DEBUG_STANDBY) {
                    Slog.d(AlarmManagerService.TAG, "Device is charging.");
                }
                charging = true;
            } else {
                boolean charging2 = AlarmManagerService.DEBUG_STANDBY;
                if (charging2) {
                    Slog.d(AlarmManagerService.TAG, "Disconnected from power.");
                }
                charging = false;
            }
            AlarmManagerService.this.mHandler.removeMessages(6);
            AlarmManagerService.this.mHandler.obtainMessage(6, Boolean.valueOf(charging)).sendToTarget();
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* loaded from: classes.dex */
    public class ClockReceiver extends BroadcastReceiver {
        public ClockReceiver() {
            IntentFilter filter = new IntentFilter();
            filter.addAction("android.intent.action.DATE_CHANGED");
            AlarmManagerService.this.getContext().registerReceiver(this, filter);
        }

        @Override // android.content.BroadcastReceiver
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals("android.intent.action.DATE_CHANGED")) {
                TimeZone zone = TimeZone.getTimeZone(SystemProperties.get(AlarmManagerService.TIMEZONE_PROPERTY));
                int gmtOffset = zone.getOffset(AlarmManagerService.this.mInjector.getCurrentTimeMillis());
                AlarmManagerService.this.mInjector.setKernelTimezone(-(gmtOffset / 60000));
                scheduleDateChangedEvent();
            }
        }

        public void scheduleTimeTickEvent() {
            long currentTime = AlarmManagerService.this.mInjector.getCurrentTimeMillis();
            long nextTime = ((currentTime / 60000) + 1) * 60000;
            long tickEventDelay = nextTime - currentTime;
            int flags = 1 | (AlarmManagerService.this.mConstants.TIME_TICK_ALLOWED_WHILE_IDLE ? 8 : 0);
            AlarmManagerService alarmManagerService = AlarmManagerService.this;
            alarmManagerService.setImpl(3, alarmManagerService.mInjector.getElapsedRealtime() + tickEventDelay, 0L, 0L, null, AlarmManagerService.this.mTimeTickTrigger, AlarmManagerService.TIME_TICK_TAG, flags, null, null, Process.myUid(), PackageManagerService.PLATFORM_PACKAGE_NAME, null, 1);
            synchronized (AlarmManagerService.this.mLock) {
                AlarmManagerService.this.mLastTickSet = currentTime;
            }
        }

        public void scheduleDateChangedEvent() {
            Calendar calendar = Calendar.getInstance();
            calendar.setTimeInMillis(AlarmManagerService.this.mInjector.getCurrentTimeMillis());
            calendar.set(11, 0);
            calendar.set(12, 0);
            calendar.set(13, 0);
            calendar.set(14, 0);
            calendar.add(5, 1);
            AlarmManagerService.this.setImpl(1, calendar.getTimeInMillis(), 0L, 0L, AlarmManagerService.this.mDateChangeSender, null, null, 1, null, null, Process.myUid(), PackageManagerService.PLATFORM_PACKAGE_NAME, null, 1);
        }
    }

    /* loaded from: classes.dex */
    class InteractiveStateReceiver extends BroadcastReceiver {
        public InteractiveStateReceiver() {
            IntentFilter filter = new IntentFilter();
            filter.addAction("android.intent.action.SCREEN_OFF");
            filter.addAction("android.intent.action.SCREEN_ON");
            filter.setPriority(1000);
            AlarmManagerService.this.getContext().registerReceiver(this, filter);
        }

        @Override // android.content.BroadcastReceiver
        public void onReceive(Context context, Intent intent) {
            synchronized (AlarmManagerService.this.mLock) {
                if ("android.intent.action.SCREEN_OFF".equals(intent.getAction())) {
                    AlarmManagerService.this.mScreenOff = true;
                    AlarmManagerService.this.mScreenOffTime = System.currentTimeMillis();
                    Slog.v(AlarmManagerService.TAG, "ACTION_SCREEN_OFF " + AlarmManagerService.this.mScreenOffTime);
                } else if ("android.intent.action.SCREEN_ON".equals(intent.getAction())) {
                    AlarmManagerService.this.mScreenOff = false;
                    AlarmManagerService.this.mScreenOffTime = 0L;
                    Slog.v(AlarmManagerService.TAG, "ACTION_SCREEN_ON");
                }
                AlarmManagerService.this.interactiveStateChangedLocked("android.intent.action.SCREEN_ON".equals(intent.getAction()));
            }
        }
    }

    /* loaded from: classes.dex */
    private class NetworkReceiver extends BroadcastReceiver {
        public NetworkReceiver() {
            IntentFilter filter = new IntentFilter("android.net.conn.CONNECTIVITY_CHANGE");
            AlarmManagerService.this.getContext().registerReceiver(this, filter);
        }

        @Override // android.content.BroadcastReceiver
        public void onReceive(Context context, Intent intent) {
            if (AlarmManagerService.this.mConnectManager == null) {
                AlarmManagerService.this.mConnectManager = (ConnectivityManager) context.getSystemService("connectivity");
            }
            if (AlarmManagerService.this.mConnectManager != null) {
                AlarmManagerService alarmManagerService = AlarmManagerService.this;
                alarmManagerService.mNetworkInfo = alarmManagerService.mConnectManager.getActiveNetworkInfo();
            }
            AlarmManagerService alarmManagerService2 = AlarmManagerService.this;
            alarmManagerService2.mNetworkConnected = alarmManagerService2.mNetworkInfo != null;
            Slog.d(AlarmManagerService.TAG, "mNetworkConnected = " + AlarmManagerService.this.mNetworkConnected);
        }
    }

    /* loaded from: classes.dex */
    class UninstallReceiver extends BroadcastReceiver {
        public UninstallReceiver() {
            IntentFilter filter = new IntentFilter();
            filter.addAction("android.intent.action.PACKAGE_REMOVED");
            filter.addAction("android.intent.action.PACKAGE_ADDED");
            filter.addAction("android.intent.action.PACKAGE_RESTARTED");
            filter.addAction("android.intent.action.QUERY_PACKAGE_RESTART");
            filter.addDataScheme("package");
            AlarmManagerService.this.getContext().registerReceiverForAllUsers(this, filter, null, null);
            IntentFilter sdFilter = new IntentFilter();
            sdFilter.addAction("android.intent.action.EXTERNAL_APPLICATIONS_UNAVAILABLE");
            sdFilter.addAction("android.intent.action.USER_STOPPED");
            sdFilter.addAction("android.intent.action.UID_REMOVED");
            AlarmManagerService.this.getContext().registerReceiverForAllUsers(this, sdFilter, null, null);
        }

        /* JADX WARN: Can't fix incorrect switch cases order, some code will duplicate */
        @Override // android.content.BroadcastReceiver
        public void onReceive(Context context, Intent intent) {
            char c;
            String[] pkgList;
            String pkg;
            int uid = intent.getIntExtra("android.intent.extra.UID", -1);
            synchronized (AlarmManagerService.this.mLock) {
                String[] pkgList2 = null;
                String action = intent.getAction();
                switch (action.hashCode()) {
                    case -1749672628:
                        if (action.equals("android.intent.action.UID_REMOVED")) {
                            c = 2;
                            break;
                        }
                        c = 65535;
                        break;
                    case -1403934493:
                        if (action.equals("android.intent.action.EXTERNAL_APPLICATIONS_UNAVAILABLE")) {
                            c = 4;
                            break;
                        }
                        c = 65535;
                        break;
                    case -1072806502:
                        if (action.equals("android.intent.action.QUERY_PACKAGE_RESTART")) {
                            c = 0;
                            break;
                        }
                        c = 65535;
                        break;
                    case -757780528:
                        if (action.equals("android.intent.action.PACKAGE_RESTARTED")) {
                            c = 6;
                            break;
                        }
                        c = 65535;
                        break;
                    case -742246786:
                        if (action.equals("android.intent.action.USER_STOPPED")) {
                            c = 1;
                            break;
                        }
                        c = 65535;
                        break;
                    case 525384130:
                        if (action.equals("android.intent.action.PACKAGE_REMOVED")) {
                            c = 5;
                            break;
                        }
                        c = 65535;
                        break;
                    case 1544582882:
                        if (action.equals("android.intent.action.PACKAGE_ADDED")) {
                            c = 3;
                            break;
                        }
                        c = 65535;
                        break;
                    default:
                        c = 65535;
                        break;
                }
                switch (c) {
                    case 0:
                        for (String packageName : intent.getStringArrayExtra("android.intent.extra.PACKAGES")) {
                            if (AlarmManagerService.this.lookForPackageLocked(packageName)) {
                                setResultCode(-1);
                                return;
                            }
                        }
                        return;
                    case 1:
                        int userHandle = intent.getIntExtra("android.intent.extra.user_handle", -1);
                        if (userHandle >= 0) {
                            AlarmManagerService.this.removeUserLocked(userHandle);
                            AlarmManagerService.this.mAppWakeupHistory.removeForUser(userHandle);
                            AlarmManagerService.this.mAllowWhileIdleHistory.removeForUser(userHandle);
                            AlarmManagerService.this.mAllowWhileIdleCompatHistory.removeForUser(userHandle);
                            AlarmManagerService.this.mTemporaryQuotaReserve.removeForUser(userHandle);
                        }
                        return;
                    case 2:
                        AlarmManagerService.this.mLastPriorityAlarmDispatch.delete(uid);
                        AlarmManagerService.this.mRemovalHistory.delete(uid);
                        AlarmManagerService.this.mLastOpScheduleExactAlarm.delete(uid);
                        return;
                    case 3:
                        AlarmManagerService.this.mHandler.sendEmptyMessage(11);
                        if (intent.getBooleanExtra("android.intent.extra.REPLACING", false)) {
                            String packageUpdated = intent.getData().getSchemeSpecificPart();
                            AlarmManagerService.this.mHandler.obtainMessage(13, uid, -1, packageUpdated).sendToTarget();
                        }
                        return;
                    case 4:
                        pkgList2 = intent.getStringArrayExtra("android.intent.extra.changed_package_list");
                        break;
                    case 5:
                        if (intent.getBooleanExtra("android.intent.extra.REPLACING", false)) {
                            return;
                        }
                        AlarmManagerService.this.mHandler.sendEmptyMessage(11);
                    case 6:
                        Uri data = intent.getData();
                        if (data != null && (pkg = data.getSchemeSpecificPart()) != null) {
                            pkgList2 = new String[]{pkg};
                            break;
                        }
                        break;
                }
                if (pkgList2 != null && pkgList2.length > 0) {
                    for (String pkg2 : pkgList2) {
                        if (uid >= 0) {
                            AlarmManagerService.this.mAppWakeupHistory.removeForPackage(pkg2, UserHandle.getUserId(uid));
                            AlarmManagerService.this.mAllowWhileIdleHistory.removeForPackage(pkg2, UserHandle.getUserId(uid));
                            AlarmManagerService.this.mAllowWhileIdleCompatHistory.removeForPackage(pkg2, UserHandle.getUserId(uid));
                            AlarmManagerService.this.mTemporaryQuotaReserve.removeForPackage(pkg2, UserHandle.getUserId(uid));
                            AlarmManagerService.this.removeLocked(uid, 0);
                        } else {
                            AlarmManagerService.this.removeLocked(pkg2);
                        }
                        AlarmManagerService.this.mPriorities.remove(pkg2);
                        for (int i = AlarmManagerService.this.mBroadcastStats.size() - 1; i >= 0; i--) {
                            ArrayMap<String, BroadcastStats> uidStats = AlarmManagerService.this.mBroadcastStats.valueAt(i);
                            if (uidStats.remove(pkg2) != null && uidStats.size() <= 0) {
                                AlarmManagerService.this.mBroadcastStats.removeAt(i);
                            }
                        }
                    }
                }
            }
        }
    }

    /* loaded from: classes.dex */
    private final class AppStandbyTracker extends AppStandbyInternal.AppIdleStateChangeListener {
        private AppStandbyTracker() {
        }

        public void onAppIdleStateChanged(String packageName, int userId, boolean idle, int bucket, int reason) {
            if (AlarmManagerService.DEBUG_STANDBY) {
                Slog.d(AlarmManagerService.TAG, "Package " + packageName + " for user " + userId + " now in bucket " + bucket);
            }
            AlarmManagerService.this.mHandler.obtainMessage(5, userId, -1, packageName).sendToTarget();
        }

        public void triggerTemporaryQuotaBump(String packageName, int userId) {
            int quotaBump;
            int uid;
            synchronized (AlarmManagerService.this.mLock) {
                quotaBump = AlarmManagerService.this.mConstants.TEMPORARY_QUOTA_BUMP;
            }
            if (quotaBump <= 0 || (uid = AlarmManagerService.this.mPackageManagerInternal.getPackageUid(packageName, 0L, userId)) < 0 || UserHandle.isCore(uid)) {
                return;
            }
            if (AlarmManagerService.DEBUG_STANDBY) {
                Slog.d(AlarmManagerService.TAG, "Bumping quota temporarily for " + packageName + " for user " + userId);
            }
            synchronized (AlarmManagerService.this.mLock) {
                AlarmManagerService.this.mTemporaryQuotaReserve.replenishQuota(packageName, userId, quotaBump, AlarmManagerService.this.mInjector.getElapsedRealtime());
            }
            AlarmManagerService.this.mHandler.obtainMessage(14, userId, -1, packageName).sendToTarget();
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: com.android.server.alarm.AlarmManagerService$9  reason: invalid class name */
    /* loaded from: classes.dex */
    public class AnonymousClass9 extends AppStateTrackerImpl.Listener {
        AnonymousClass9() {
        }

        @Override // com.android.server.AppStateTrackerImpl.Listener
        public void updateAllAlarms() {
            synchronized (AlarmManagerService.this.mLock) {
                if (AlarmManagerService.this.mAlarmStore.updateAlarmDeliveries(new AlarmStore.AlarmDeliveryCalculator() { // from class: com.android.server.alarm.AlarmManagerService$9$$ExternalSyntheticLambda0
                    @Override // com.android.server.alarm.AlarmStore.AlarmDeliveryCalculator
                    public final boolean updateAlarmDelivery(Alarm alarm) {
                        return AlarmManagerService.AnonymousClass9.this.m955x833f682d(alarm);
                    }
                })) {
                    AlarmManagerService.this.rescheduleKernelAlarmsLocked();
                }
            }
        }

        /* JADX INFO: Access modifiers changed from: package-private */
        /* renamed from: lambda$updateAllAlarms$0$com-android-server-alarm-AlarmManagerService$9  reason: not valid java name */
        public /* synthetic */ boolean m955x833f682d(Alarm a) {
            return AlarmManagerService.this.adjustDeliveryTimeBasedOnBatterySaver(a);
        }

        @Override // com.android.server.AppStateTrackerImpl.Listener
        public void updateAlarmsForUid(final int uid) {
            synchronized (AlarmManagerService.this.mLock) {
                if (AlarmManagerService.this.mAlarmStore.updateAlarmDeliveries(new AlarmStore.AlarmDeliveryCalculator() { // from class: com.android.server.alarm.AlarmManagerService$9$$ExternalSyntheticLambda1
                    @Override // com.android.server.alarm.AlarmStore.AlarmDeliveryCalculator
                    public final boolean updateAlarmDelivery(Alarm alarm) {
                        return AlarmManagerService.AnonymousClass9.this.m954xa0e2b6da(uid, alarm);
                    }
                })) {
                    AlarmManagerService.this.rescheduleKernelAlarmsLocked();
                }
            }
        }

        /* JADX INFO: Access modifiers changed from: package-private */
        /* renamed from: lambda$updateAlarmsForUid$1$com-android-server-alarm-AlarmManagerService$9  reason: not valid java name */
        public /* synthetic */ boolean m954xa0e2b6da(int uid, Alarm a) {
            if (a.creatorUid != uid) {
                return false;
            }
            return AlarmManagerService.this.adjustDeliveryTimeBasedOnBatterySaver(a);
        }

        @Override // com.android.server.AppStateTrackerImpl.Listener
        public void unblockAllUnrestrictedAlarms() {
            synchronized (AlarmManagerService.this.mLock) {
                AlarmManagerService.this.sendAllUnrestrictedPendingBackgroundAlarmsLocked();
            }
        }

        @Override // com.android.server.AppStateTrackerImpl.Listener
        public void unblockAlarmsForUid(int uid) {
            synchronized (AlarmManagerService.this.mLock) {
                AlarmManagerService.this.sendPendingBackgroundAlarmsLocked(uid, null);
            }
        }

        @Override // com.android.server.AppStateTrackerImpl.Listener
        public void unblockAlarmsForUidPackage(int uid, String packageName) {
            synchronized (AlarmManagerService.this.mLock) {
                AlarmManagerService.this.sendPendingBackgroundAlarmsLocked(uid, packageName);
            }
        }

        @Override // com.android.server.AppStateTrackerImpl.Listener
        public void removeAlarmsForUid(int uid) {
            synchronized (AlarmManagerService.this.mLock) {
                AlarmManagerService.this.removeForStoppedLocked(uid);
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public final BroadcastStats getStatsLocked(PendingIntent pi) {
        String pkg = pi.getCreatorPackage();
        int uid = pi.getCreatorUid();
        return getStatsLocked(uid, pkg);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public final BroadcastStats getStatsLocked(int uid, String pkgName) {
        ArrayMap<String, BroadcastStats> uidStats = this.mBroadcastStats.get(uid);
        if (uidStats == null) {
            uidStats = new ArrayMap<>();
            this.mBroadcastStats.put(uid, uidStats);
        }
        BroadcastStats bs = uidStats.get(pkgName);
        if (bs == null) {
            BroadcastStats bs2 = new BroadcastStats(uid, pkgName);
            uidStats.put(pkgName, bs2);
            return bs2;
        }
        return bs;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* loaded from: classes.dex */
    public class DeliveryTracker extends IAlarmCompleteListener.Stub implements PendingIntent.OnFinished {
        DeliveryTracker() {
        }

        private InFlight removeLocked(PendingIntent pi, Intent intent) {
            for (int i = 0; i < AlarmManagerService.this.mInFlight.size(); i++) {
                InFlight inflight = AlarmManagerService.this.mInFlight.get(i);
                if (inflight.mPendingIntent == pi) {
                    if (pi.isBroadcast()) {
                        AlarmManagerService.this.notifyBroadcastAlarmCompleteLocked(inflight.mUid);
                    }
                    return AlarmManagerService.this.mInFlight.remove(i);
                }
            }
            AlarmManagerService.this.mLog.w("No in-flight alarm for " + pi + " " + intent);
            return null;
        }

        private InFlight removeLocked(IBinder listener) {
            for (int i = 0; i < AlarmManagerService.this.mInFlight.size(); i++) {
                if (AlarmManagerService.this.mInFlight.get(i).mListener == listener) {
                    return AlarmManagerService.this.mInFlight.remove(i);
                }
            }
            AlarmManagerService.this.mLog.w("No in-flight alarm for listener " + listener);
            return null;
        }

        private void updateStatsLocked(InFlight inflight) {
            long nowELAPSED = AlarmManagerService.this.mInjector.getElapsedRealtime();
            BroadcastStats bs = inflight.mBroadcastStats;
            bs.nesting--;
            if (bs.nesting <= 0) {
                bs.nesting = 0;
                bs.aggregateTime += nowELAPSED - bs.startTime;
            }
            FilterStats fs = inflight.mFilterStats;
            fs.nesting--;
            if (fs.nesting <= 0) {
                fs.nesting = 0;
                fs.aggregateTime += nowELAPSED - fs.startTime;
            }
            AlarmManagerService.this.mActivityManagerInternal.noteAlarmFinish(inflight.mPendingIntent, inflight.mWorkSource, inflight.mUid, inflight.mTag);
        }

        private void updateTrackingLocked(InFlight inflight) {
            if (inflight != null) {
                updateStatsLocked(inflight);
            }
            AlarmManagerService alarmManagerService = AlarmManagerService.this;
            alarmManagerService.mBroadcastRefCount--;
            if (AlarmManagerService.DEBUG_WAKELOCK) {
                Slog.d(AlarmManagerService.TAG, "mBroadcastRefCount -> " + AlarmManagerService.this.mBroadcastRefCount);
            }
            if (AlarmManagerService.this.mBroadcastRefCount == 0) {
                AlarmManagerService.this.mHandler.obtainMessage(4, 0, 0).sendToTarget();
                AlarmManagerService.this.mWakeLock.release();
                if (AlarmManagerService.this.mInFlight.size() > 0) {
                    AlarmManagerService.this.mLog.w("Finished all dispatches with " + AlarmManagerService.this.mInFlight.size() + " remaining inflights");
                    for (int i = 0; i < AlarmManagerService.this.mInFlight.size(); i++) {
                        AlarmManagerService.this.mLog.w("  Remaining #" + i + ": " + AlarmManagerService.this.mInFlight.get(i));
                    }
                    AlarmManagerService.this.mInFlight.clear();
                }
            } else if (AlarmManagerService.this.mInFlight.size() > 0) {
                InFlight inFlight = AlarmManagerService.this.mInFlight.get(0);
                AlarmManagerService.this.setWakelockWorkSource(inFlight.mWorkSource, inFlight.mCreatorUid, inFlight.mTag, false);
            } else {
                AlarmManagerService.this.mLog.w("Alarm wakelock still held but sent queue empty");
                AlarmManagerService.this.mWakeLock.setWorkSource(null);
            }
        }

        public void alarmComplete(IBinder who) {
            if (who == null) {
                AlarmManagerService.this.mLog.w("Invalid alarmComplete: uid=" + Binder.getCallingUid() + " pid=" + Binder.getCallingPid());
                return;
            }
            long ident = Binder.clearCallingIdentity();
            try {
                synchronized (AlarmManagerService.this.mLock) {
                    AlarmManagerService.this.mHandler.removeMessages(3, who);
                    InFlight inflight = removeLocked(who);
                    if (inflight != null) {
                        if (AlarmManagerService.DEBUG_LISTENER_CALLBACK) {
                            Slog.i(AlarmManagerService.TAG, "alarmComplete() from " + who);
                        }
                        updateTrackingLocked(inflight);
                        AlarmManagerService.this.mListenerFinishCount++;
                    } else if (AlarmManagerService.DEBUG_LISTENER_CALLBACK) {
                        Slog.i(AlarmManagerService.TAG, "Late alarmComplete() from " + who);
                    }
                }
            } finally {
                Binder.restoreCallingIdentity(ident);
            }
        }

        @Override // android.app.PendingIntent.OnFinished
        public void onSendFinished(PendingIntent pi, Intent intent, int resultCode, String resultData, Bundle resultExtras) {
            synchronized (AlarmManagerService.this.mLock) {
                AlarmManagerService.this.mSendFinishCount++;
                updateTrackingLocked(removeLocked(pi, intent));
            }
        }

        public void alarmTimedOut(IBinder who) {
            synchronized (AlarmManagerService.this.mLock) {
                InFlight inflight = removeLocked(who);
                if (inflight != null) {
                    if (AlarmManagerService.DEBUG_LISTENER_CALLBACK) {
                        Slog.i(AlarmManagerService.TAG, "Alarm listener " + who + " timed out in delivery");
                    }
                    updateTrackingLocked(inflight);
                    AlarmManagerService.this.mListenerFinishCount++;
                } else {
                    if (AlarmManagerService.DEBUG_LISTENER_CALLBACK) {
                        Slog.i(AlarmManagerService.TAG, "Spurious timeout of listener " + who);
                    }
                    AlarmManagerService.this.mLog.w("Spurious timeout of listener " + who);
                }
            }
        }

        /* JADX DEBUG: Don't trust debug lines info. Repeating lines: [6130=4] */
        /* JADX WARN: Removed duplicated region for block: B:27:0x0102  */
        /* JADX WARN: Removed duplicated region for block: B:30:0x0125  */
        /* JADX WARN: Removed duplicated region for block: B:33:0x015d  */
        /* JADX WARN: Removed duplicated region for block: B:36:0x016a  */
        /* JADX WARN: Removed duplicated region for block: B:37:0x016c  */
        /* JADX WARN: Removed duplicated region for block: B:48:0x018e  */
        /* JADX WARN: Removed duplicated region for block: B:53:0x01b5  */
        /* JADX WARN: Removed duplicated region for block: B:60:0x01f8  */
        /* JADX WARN: Removed duplicated region for block: B:66:0x0220  */
        /* JADX WARN: Removed duplicated region for block: B:67:0x0225  */
        /* JADX WARN: Removed duplicated region for block: B:70:0x0235  */
        /* JADX WARN: Removed duplicated region for block: B:71:0x023a  */
        /*
            Code decompiled incorrectly, please refer to instructions dump.
        */
        public void deliverLocked(final Alarm alarm, long nowELAPSED) {
            InFlight inflight;
            final boolean doze;
            BroadcastStats bs;
            FilterStats fs;
            long workSourceToken = ThreadLocalWorkSource.setUid(AlarmManagerService.getAlarmAttributionUid(alarm));
            try {
                final boolean batterySaver = false;
                if (alarm.operation == null) {
                    AlarmManagerService.this.mListenerCount++;
                    alarm.listener.asBinder().unlinkToDeath(AlarmManagerService.this.mListenerDeathRecipient, 0);
                    if (alarm.listener == AlarmManagerService.this.mTimeTickTrigger) {
                        long[] jArr = AlarmManagerService.this.mTickHistory;
                        AlarmManagerService alarmManagerService = AlarmManagerService.this;
                        int i = alarmManagerService.mNextTickHistory;
                        alarmManagerService.mNextTickHistory = i + 1;
                        jArr[i] = nowELAPSED;
                        if (AlarmManagerService.this.mNextTickHistory >= 10) {
                            AlarmManagerService.this.mNextTickHistory = 0;
                        }
                    }
                    try {
                        if (AlarmManagerService.DEBUG_LISTENER_CALLBACK) {
                            Slog.v(AlarmManagerService.TAG, "Alarm to uid=" + alarm.uid + " listener=" + alarm.listener.asBinder());
                        }
                        alarm.listener.doAlarm(this);
                        AlarmManagerService.this.mHandler.sendMessageDelayed(AlarmManagerService.this.mHandler.obtainMessage(3, alarm.listener.asBinder()), AlarmManagerService.this.mConstants.LISTENER_TIMEOUT);
                        ThreadLocalWorkSource.restore(workSourceToken);
                        if (AlarmManagerService.DEBUG_WAKELOCK) {
                        }
                        if (AlarmManagerService.this.mBroadcastRefCount == 0) {
                        }
                        inflight = new InFlight(AlarmManagerService.this, alarm, nowELAPSED);
                        AlarmManagerService.this.mInFlight.add(inflight);
                        AlarmManagerService.this.mBroadcastRefCount++;
                        if (inflight.isBroadcast()) {
                        }
                        if (AlarmManagerService.this.mPendingIdleUntil == null) {
                        }
                        if (AlarmManagerService.this.mAppStateTracker != null) {
                            batterySaver = true;
                        }
                        if (!doze) {
                        }
                        if (!AlarmManagerService.isAllowedWhileIdleRestricted(alarm)) {
                        }
                        IdleDispatchEntry ent = new IdleDispatchEntry();
                        ent.uid = alarm.uid;
                        ent.pkg = alarm.packageName;
                        ent.tag = alarm.statsTag;
                        ent.op = "DELIVER";
                        ent.elapsedRealtime = nowELAPSED;
                        AlarmManagerService.this.mAllowWhileIdleDispatches.add(ent);
                        if (!AlarmManagerService.isExemptFromAppStandby(alarm)) {
                        }
                        bs = inflight.mBroadcastStats;
                        bs.count++;
                        if (bs.nesting != 0) {
                        }
                        fs = inflight.mFilterStats;
                        fs.count++;
                        if (fs.nesting != 0) {
                        }
                        if (alarm.type != 2) {
                        }
                        bs.numWakeup++;
                        fs.numWakeup++;
                        AlarmManagerService.this.mActivityManagerInternal.noteWakeupAlarm(alarm.operation, alarm.workSource, alarm.uid, alarm.packageName, alarm.statsTag);
                    } catch (Exception e) {
                        if (AlarmManagerService.DEBUG_LISTENER_CALLBACK) {
                            Slog.i(AlarmManagerService.TAG, "Alarm undeliverable to listener " + alarm.listener.asBinder(), e);
                        }
                        AlarmManagerService.this.mListenerFinishCount++;
                        ThreadLocalWorkSource.restore(workSourceToken);
                        return;
                    }
                }
                AlarmManagerService.this.mSendCount++;
                try {
                    Bundle bundle = AlarmManagerService.this.getAlarmOperationBundle(alarm);
                    alarm.operation.send(AlarmManagerService.this.getContext(), 0, AlarmManagerService.this.mBackgroundIntent.putExtra("android.intent.extra.ALARM_COUNT", alarm.count), AlarmManagerService.this.mDeliveryTracker, AlarmManagerService.this.mHandler, null, bundle);
                    ThreadLocalWorkSource.restore(workSourceToken);
                    if (AlarmManagerService.DEBUG_WAKELOCK) {
                        Slog.d(AlarmManagerService.TAG, "mBroadcastRefCount -> " + (AlarmManagerService.this.mBroadcastRefCount + 1));
                    }
                    if (AlarmManagerService.this.mBroadcastRefCount == 0) {
                        AlarmManagerService.this.setWakelockWorkSource(alarm.workSource, alarm.creatorUid, alarm.statsTag, true);
                        AlarmManagerService.this.mWakeLock.acquire();
                        AlarmManagerService.this.mHandler.obtainMessage(4, 1, 0).sendToTarget();
                    }
                    inflight = new InFlight(AlarmManagerService.this, alarm, nowELAPSED);
                    AlarmManagerService.this.mInFlight.add(inflight);
                    AlarmManagerService.this.mBroadcastRefCount++;
                    if (inflight.isBroadcast()) {
                        AlarmManagerService.this.notifyBroadcastAlarmPendingLocked(alarm.uid);
                    }
                    doze = AlarmManagerService.this.mPendingIdleUntil == null;
                    if (AlarmManagerService.this.mAppStateTracker != null && AlarmManagerService.this.mAppStateTracker.isForceAllAppsStandbyEnabled()) {
                        batterySaver = true;
                    }
                    if (!doze || batterySaver) {
                        if (!AlarmManagerService.isAllowedWhileIdleRestricted(alarm)) {
                            AppWakeupHistory history = (4 & alarm.flags) != 0 ? AlarmManagerService.this.mAllowWhileIdleHistory : AlarmManagerService.this.mAllowWhileIdleCompatHistory;
                            history.recordAlarmForPackage(alarm.sourcePackage, UserHandle.getUserId(alarm.creatorUid), nowELAPSED);
                            AlarmManagerService.this.mAlarmStore.updateAlarmDeliveries(new AlarmStore.AlarmDeliveryCalculator() { // from class: com.android.server.alarm.AlarmManagerService$DeliveryTracker$$ExternalSyntheticLambda0
                                @Override // com.android.server.alarm.AlarmStore.AlarmDeliveryCalculator
                                public final boolean updateAlarmDelivery(Alarm alarm2) {
                                    return AlarmManagerService.DeliveryTracker.this.m957x614a6403(alarm, doze, batterySaver, alarm2);
                                }
                            });
                        } else if ((alarm.flags & 64) != 0) {
                            AlarmManagerService.this.mLastPriorityAlarmDispatch.put(alarm.creatorUid, nowELAPSED);
                            AlarmManagerService.this.mAlarmStore.updateAlarmDeliveries(new AlarmStore.AlarmDeliveryCalculator() { // from class: com.android.server.alarm.AlarmManagerService$DeliveryTracker$$ExternalSyntheticLambda1
                                @Override // com.android.server.alarm.AlarmStore.AlarmDeliveryCalculator
                                public final boolean updateAlarmDelivery(Alarm alarm2) {
                                    return AlarmManagerService.DeliveryTracker.this.m958xf588d3a2(alarm, doze, batterySaver, alarm2);
                                }
                            });
                        }
                        IdleDispatchEntry ent2 = new IdleDispatchEntry();
                        ent2.uid = alarm.uid;
                        ent2.pkg = alarm.packageName;
                        ent2.tag = alarm.statsTag;
                        ent2.op = "DELIVER";
                        ent2.elapsedRealtime = nowELAPSED;
                        AlarmManagerService.this.mAllowWhileIdleDispatches.add(ent2);
                    }
                    if (!AlarmManagerService.isExemptFromAppStandby(alarm)) {
                        int userId = UserHandle.getUserId(alarm.creatorUid);
                        if (alarm.mUsingReserveQuota) {
                            AlarmManagerService.this.mTemporaryQuotaReserve.recordUsage(alarm.sourcePackage, userId, nowELAPSED);
                        } else {
                            AlarmManagerService.this.mAppWakeupHistory.recordAlarmForPackage(alarm.sourcePackage, userId, nowELAPSED);
                        }
                    }
                    bs = inflight.mBroadcastStats;
                    bs.count++;
                    if (bs.nesting != 0) {
                        bs.nesting = 1;
                        bs.startTime = nowELAPSED;
                    } else {
                        bs.nesting++;
                    }
                    fs = inflight.mFilterStats;
                    fs.count++;
                    if (fs.nesting != 0) {
                        fs.nesting = 1;
                        fs.startTime = nowELAPSED;
                    } else {
                        fs.nesting++;
                    }
                    if (alarm.type != 2 || alarm.type == 0) {
                        bs.numWakeup++;
                        fs.numWakeup++;
                        AlarmManagerService.this.mActivityManagerInternal.noteWakeupAlarm(alarm.operation, alarm.workSource, alarm.uid, alarm.packageName, alarm.statsTag);
                    }
                } catch (PendingIntent.CanceledException e2) {
                    if (alarm.repeatInterval > 0) {
                        AlarmManagerService.this.removeImpl(alarm.operation, null);
                    }
                    AlarmManagerService.this.mSendFinishCount++;
                    ThreadLocalWorkSource.restore(workSourceToken);
                }
            } catch (Throwable e3) {
                ThreadLocalWorkSource.restore(workSourceToken);
                throw e3;
            }
        }

        /* JADX INFO: Access modifiers changed from: package-private */
        /* renamed from: lambda$deliverLocked$0$com-android-server-alarm-AlarmManagerService$DeliveryTracker  reason: not valid java name */
        public /* synthetic */ boolean m957x614a6403(Alarm alarm, boolean doze, boolean batterySaver, Alarm a) {
            if (a.creatorUid == alarm.creatorUid && AlarmManagerService.isAllowedWhileIdleRestricted(a)) {
                boolean dozeAdjusted = doze && AlarmManagerService.this.m951x4dd46403(a);
                boolean batterySaverAdjusted = batterySaver && AlarmManagerService.this.adjustDeliveryTimeBasedOnBatterySaver(a);
                return dozeAdjusted || batterySaverAdjusted;
            }
            return false;
        }

        /* JADX INFO: Access modifiers changed from: package-private */
        /* renamed from: lambda$deliverLocked$1$com-android-server-alarm-AlarmManagerService$DeliveryTracker  reason: not valid java name */
        public /* synthetic */ boolean m958xf588d3a2(Alarm alarm, boolean doze, boolean batterySaver, Alarm a) {
            if (a.creatorUid != alarm.creatorUid || (alarm.flags & 64) == 0) {
                return false;
            }
            boolean dozeAdjusted = doze && AlarmManagerService.this.m951x4dd46403(a);
            boolean batterySaverAdjusted = batterySaver && AlarmManagerService.this.adjustDeliveryTimeBasedOnBatterySaver(a);
            return dozeAdjusted || batterySaverAdjusted;
        }
    }

    private void incrementAlarmCount(int uid) {
        int uidIndex = this.mAlarmsPerUid.indexOfKey(uid);
        if (uidIndex < 0) {
            this.mAlarmsPerUid.put(uid, 1);
            return;
        }
        SparseIntArray sparseIntArray = this.mAlarmsPerUid;
        sparseIntArray.setValueAt(uidIndex, sparseIntArray.valueAt(uidIndex) + 1);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void sendScheduleExactAlarmPermissionStateChangedBroadcast(String packageName, int userId) {
        Intent i = new Intent("android.app.action.SCHEDULE_EXACT_ALARM_PERMISSION_STATE_CHANGED");
        i.addFlags(872415232);
        i.setPackage(packageName);
        BroadcastOptions opts = BroadcastOptions.makeBasic();
        opts.setTemporaryAppAllowlist(this.mActivityManagerInternal.getBootTimeTempAllowListDuration(), 0, 207, "");
        getContext().sendBroadcastAsUser(i, UserHandle.of(userId), null, opts.toBundle());
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void decrementAlarmCount(int uid, int decrement) {
        int oldCount = 0;
        int uidIndex = this.mAlarmsPerUid.indexOfKey(uid);
        if (uidIndex >= 0) {
            oldCount = this.mAlarmsPerUid.valueAt(uidIndex);
            if (oldCount > decrement) {
                this.mAlarmsPerUid.setValueAt(uidIndex, oldCount - decrement);
            } else {
                this.mAlarmsPerUid.removeAt(uidIndex);
            }
        }
        if (oldCount < decrement) {
            Slog.wtf(TAG, "Attempt to decrement existing alarm count " + oldCount + " by " + decrement + " for uid " + uid);
        }
    }

    /* loaded from: classes.dex */
    private class ShellCmd extends ShellCommand {
        private ShellCmd() {
        }

        IAlarmManager getBinderService() {
            return IAlarmManager.Stub.asInterface(AlarmManagerService.this.mService);
        }

        /* JADX WARN: Can't fix incorrect switch cases order, some code will duplicate */
        public int onCommand(String cmd) {
            char c;
            if (cmd == null) {
                return handleDefaultCommands(cmd);
            }
            PrintWriter pw = getOutPrintWriter();
            try {
                switch (cmd.hashCode()) {
                    case -2120488796:
                        if (cmd.equals("get-config-version")) {
                            c = 2;
                            break;
                        }
                        c = 65535;
                        break;
                    case 1369384280:
                        if (cmd.equals("set-time")) {
                            c = 0;
                            break;
                        }
                        c = 65535;
                        break;
                    case 2023087364:
                        if (cmd.equals("set-timezone")) {
                            c = 1;
                            break;
                        }
                        c = 65535;
                        break;
                    default:
                        c = 65535;
                        break;
                }
                switch (c) {
                    case 0:
                        String tz = getNextArgRequired();
                        long millis = Long.parseLong(tz);
                        return getBinderService().setTime(millis) ? 0 : -1;
                    case 1:
                        String tz2 = getNextArgRequired();
                        getBinderService().setTimeZone(tz2);
                        return 0;
                    case 2:
                        int version = getBinderService().getConfigVersion();
                        pw.println(version);
                        return 0;
                    default:
                        return handleDefaultCommands(cmd);
                }
            } catch (Exception e) {
                pw.println(e);
                return -1;
            }
        }

        public void onHelp() {
            PrintWriter pw = getOutPrintWriter();
            pw.println("Alarm manager service (alarm) commands:");
            pw.println("  help");
            pw.println("    Print this help text.");
            pw.println("  set-time TIME");
            pw.println("    Set the system clock time to TIME where TIME is milliseconds");
            pw.println("    since the Epoch.");
            pw.println("  set-timezone TZ");
            pw.println("    Set the system timezone to TZ where TZ is an Olson id.");
            pw.println("  get-config-version");
            pw.println("    Returns an integer denoting the version of device_config keys the service is sync'ed to. As long as this returns the same version, the values of the config are guaranteed to remain the same.");
        }
    }

    /* JADX DEBUG: Method merged with bridge method */
    /* JADX INFO: Access modifiers changed from: private */
    /* renamed from: updateAlarms */
    public List<String> m952x457dc07b() {
        long startTime = SystemClock.elapsedRealtime();
        List<String> alarmList = new ArrayList<>();
        if (this.mAlarmStore.asList().size() > 0) {
            ArrayList<Alarm> alarms = this.mAlarmStore.asList();
            for (int k = 0; k < alarms.size(); k++) {
                Alarm alarm = alarms.get(k);
                String packageName = alarm.packageName;
                if (!PackageManagerService.PLATFORM_PACKAGE_NAME.equals(packageName) && alarm.alarmClock != null && alarm.flags != 0 && !alarmList.contains(packageName)) {
                    alarmList.add(packageName);
                    if (ITranGriffinFeature.Instance().isGriffinDebugOpen()) {
                        Slog.d(TAG, "alarm[" + k + "] = " + packageName);
                    }
                }
            }
        }
        if (ITranGriffinFeature.Instance().isGriffinDebugOpen()) {
            Slog.d(TAG, "updateAlarm coast " + (SystemClock.elapsedRealtime() - startTime) + "(ms)");
        }
        return alarmList;
    }
}
