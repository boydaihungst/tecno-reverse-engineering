package com.android.server.am;

import android.app.ActivityThread;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.Context;
import android.database.ContentObserver;
import android.net.Uri;
import android.os.Build;
import android.os.Handler;
import android.os.Message;
import android.os.SystemClock;
import android.os.SystemProperties;
import android.provider.DeviceConfig;
import android.provider.Settings;
import android.text.TextUtils;
import android.util.ArraySet;
import android.util.KeyValueListParser;
import android.util.Slog;
import com.android.internal.util.FrameworkStatsLog;
import com.android.server.am.ActivityManagerService;
import com.android.server.backup.BackupAgentTimeoutParameters;
import com.android.server.job.controllers.JobStatus;
import com.transsion.hubcore.server.am.ITranActivityManagerConstants;
import dalvik.annotation.optimization.NeverCompile;
import java.io.PrintWriter;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;
/* JADX INFO: Access modifiers changed from: package-private */
/* loaded from: classes.dex */
public final class ActivityManagerConstants extends ContentObserver {
    private static final Uri ACTIVITY_MANAGER_CONSTANTS_URI;
    private static final Uri ACTIVITY_STARTS_LOGGING_ENABLED_URI;
    public static int BINDER_HEAVY_HITTER_AUTO_SAMPLER_BATCHSIZE = 0;
    public static boolean BINDER_HEAVY_HITTER_AUTO_SAMPLER_ENABLED = false;
    public static float BINDER_HEAVY_HITTER_AUTO_SAMPLER_THRESHOLD = 0.0f;
    public static int BINDER_HEAVY_HITTER_WATCHER_BATCHSIZE = 0;
    public static boolean BINDER_HEAVY_HITTER_WATCHER_ENABLED = false;
    public static float BINDER_HEAVY_HITTER_WATCHER_THRESHOLD = 0.0f;
    static final long DEFAULT_BACKGROUND_SETTLE_TIME = 60000;
    private static final long DEFAULT_BG_START_TIMEOUT = 15000;
    private static final int DEFAULT_BOOT_TIME_TEMP_ALLOWLIST_DURATION = 20000;
    private static final int DEFAULT_BOUND_SERVICE_CRASH_MAX_RETRY = 16;
    private static final long DEFAULT_BOUND_SERVICE_CRASH_RESTART_DURATION = 1800000;
    private static final String DEFAULT_COMPONENT_ALIAS_OVERRIDES = "";
    private static final long DEFAULT_CONTENT_PROVIDER_RETAIN_TIME = 20000;
    private static final int DEFAULT_DEFER_BOOT_COMPLETED_BROADCAST = 6;
    private static final boolean DEFAULT_ENABLE_COMPONENT_ALIAS = false;
    private static final boolean DEFAULT_ENABLE_EXTRA_SERVICE_RESTART_DELAY_ON_MEM_PRESSURE = true;
    private static final long DEFAULT_EXTRA_SERVICE_RESTART_DELAY_ON_CRITICAL_MEM = 30000;
    private static final long DEFAULT_EXTRA_SERVICE_RESTART_DELAY_ON_LOW_MEM = 20000;
    private static final long[] DEFAULT_EXTRA_SERVICE_RESTART_DELAY_ON_MEM_PRESSURE;
    private static final long DEFAULT_EXTRA_SERVICE_RESTART_DELAY_ON_MODERATE_MEM = 10000;
    private static final long DEFAULT_EXTRA_SERVICE_RESTART_DELAY_ON_NORMAL_MEM = 0;
    private static final long DEFAULT_FGSERVICE_MIN_REPORT_TIME = 3000;
    private static final long DEFAULT_FGSERVICE_MIN_SHOWN_TIME = 2000;
    private static final long DEFAULT_FGSERVICE_SCREEN_ON_AFTER_TIME = 5000;
    private static final long DEFAULT_FGSERVICE_SCREEN_ON_BEFORE_TIME = 1000;
    private static final boolean DEFAULT_FGS_ALLOW_OPT_OUT = false;
    private static final float DEFAULT_FGS_ATOM_SAMPLE_RATE = 1.0f;
    private static final float DEFAULT_FGS_START_ALLOWED_LOG_SAMPLE_RATE = 0.25f;
    private static final float DEFAULT_FGS_START_DENIED_LOG_SAMPLE_RATE = 1.0f;
    private static final int DEFAULT_FGS_START_FOREGROUND_TIMEOUT_MS = 10000;
    private static final long DEFAULT_FG_TO_BG_FGS_GRACE_DURATION = 5000;
    private static final boolean DEFAULT_FORCE_BACKGROUND_CHECK_ON_RESTRICTED_APPS = true;
    private static final long DEFAULT_FULL_PSS_LOWERED_INTERVAL = 300000;
    private static final long DEFAULT_FULL_PSS_MIN_INTERVAL = 1200000;
    private static final long DEFAULT_GC_MIN_INTERVAL = 60000;
    private static final long DEFAULT_GC_TIMEOUT = 5000;
    static final boolean DEFAULT_KILL_BG_RESTRICTED_CACHED_IDLE = true;
    static final long DEFAULT_KILL_BG_RESTRICTED_CACHED_IDLE_SETTLE_TIME_MS = 60000;
    private static final int DEFAULT_MAX_CACHED_PROCESSES = 32;
    private static final long DEFAULT_MAX_EMPTY_TIME_MILLIS;
    private static final int DEFAULT_MAX_PHANTOM_PROCESSES = 32;
    private static final long DEFAULT_MAX_SERVICE_INACTIVITY = 1800000;
    private static final long DEFAULT_MEMORY_INFO_THROTTLE_TIME = 300000;
    private static final long DEFAULT_MIN_ASSOC_LOG_DURATION = 300000;
    private static final int DEFAULT_MIN_CRASH_INTERVAL = 120000;
    private static final long DEFAULT_NETWORK_ACCESS_TIMEOUT_MS = 200;
    private static final long DEFAULT_NO_KILL_CACHED_PROCESSES_POST_BOOT_COMPLETED_DURATION_MILLIS;
    private static final boolean DEFAULT_NO_KILL_CACHED_PROCESSES_UNTIL_BOOT_COMPLETED;
    private static final int DEFAULT_OOMADJ_UPDATE_POLICY = 1;
    private static final int DEFAULT_PENDINGINTENT_WARNING_THRESHOLD = 2000;
    private static final long DEFAULT_POWER_CHECK_INTERVAL;
    private static final int DEFAULT_POWER_CHECK_MAX_CPU_1 = 25;
    private static final int DEFAULT_POWER_CHECK_MAX_CPU_2 = 25;
    private static final int DEFAULT_POWER_CHECK_MAX_CPU_3 = 10;
    private static final int DEFAULT_POWER_CHECK_MAX_CPU_4 = 2;
    private static final int DEFAULT_PROCESS_CRASH_COUNT_LIMIT = 12;
    private static final int DEFAULT_PROCESS_CRASH_COUNT_RESET_INTERVAL = 43200000;
    private static final long DEFAULT_PROCESS_KILL_TIMEOUT_MS = 10000;
    private static final boolean DEFAULT_PROCESS_START_ASYNC = true;
    private static final int DEFAULT_PUSH_MESSAGING_OVER_QUOTA_BEHAVIOR = 1;
    private static final long DEFAULT_SERVICE_BG_ACTIVITY_START_TIMEOUT = 10000;
    private static final long DEFAULT_SERVICE_BIND_ALMOST_PERCEPTIBLE_TIMEOUT_MS = 15000;
    private static final long DEFAULT_SERVICE_MIN_RESTART_TIME_BETWEEN = 10000;
    private static final long DEFAULT_SERVICE_RESET_RUN_DURATION = 60000;
    private static final long DEFAULT_SERVICE_RESTART_DURATION = 1000;
    private static final int DEFAULT_SERVICE_RESTART_DURATION_FACTOR = 4;
    private static final int DEFAULT_SERVICE_START_FOREGROUND_ANR_DELAY_MS = 10000;
    private static final int DEFAULT_SERVICE_START_FOREGROUND_TIMEOUT_MS = 30000;
    private static final long DEFAULT_SERVICE_USAGE_INTERACTION_TIME_POST_S = 60000;
    private static final long DEFAULT_SERVICE_USAGE_INTERACTION_TIME_PRE_S = 1800000;
    private static final long DEFAULT_TOP_TO_ALMOST_PERCEPTIBLE_GRACE_DURATION = 15000;
    private static final long DEFAULT_TOP_TO_FGS_GRACE_DURATION = 15000;
    private static final long DEFAULT_USAGE_STATS_INTERACTION_INTERVAL_POST_S = 600000;
    private static final long DEFAULT_USAGE_STATS_INTERACTION_INTERVAL_PRE_S = 7200000;
    private static final Uri ENABLE_AUTOMATIC_SYSTEM_SERVER_HEAP_DUMPS_URI;
    private static final Uri FOREGROUND_SERVICE_STARTS_LOGGING_ENABLED_URI;
    static final String KEY_BACKGROUND_SETTLE_TIME = "background_settle_time";
    static final String KEY_BG_START_TIMEOUT = "service_bg_start_timeout";
    private static final String KEY_BINDER_HEAVY_HITTER_AUTO_SAMPLER_BATCHSIZE = "binder_heavy_hitter_auto_sampler_batchsize";
    private static final String KEY_BINDER_HEAVY_HITTER_AUTO_SAMPLER_ENABLED = "binder_heavy_hitter_auto_sampler_enabled";
    private static final String KEY_BINDER_HEAVY_HITTER_AUTO_SAMPLER_THRESHOLD = "binder_heavy_hitter_auto_sampler_threshold";
    private static final String KEY_BINDER_HEAVY_HITTER_WATCHER_BATCHSIZE = "binder_heavy_hitter_watcher_batchsize";
    private static final String KEY_BINDER_HEAVY_HITTER_WATCHER_ENABLED = "binder_heavy_hitter_watcher_enabled";
    private static final String KEY_BINDER_HEAVY_HITTER_WATCHER_THRESHOLD = "binder_heavy_hitter_watcher_threshold";
    static final String KEY_BOOT_TIME_TEMP_ALLOWLIST_DURATION = "boot_time_temp_allowlist_duration";
    static final String KEY_BOUND_SERVICE_CRASH_MAX_RETRY = "service_crash_max_retry";
    static final String KEY_BOUND_SERVICE_CRASH_RESTART_DURATION = "service_crash_restart_duration";
    static final String KEY_COMPONENT_ALIAS_OVERRIDES = "component_alias_overrides";
    private static final String KEY_CONTENT_PROVIDER_RETAIN_TIME = "content_provider_retain_time";
    private static final String KEY_DEFAULT_BACKGROUND_ACTIVITY_STARTS_ENABLED = "default_background_activity_starts_enabled";
    private static final String KEY_DEFAULT_BACKGROUND_FGS_STARTS_RESTRICTION_ENABLED = "default_background_fgs_starts_restriction_enabled";
    private static final String KEY_DEFAULT_FGS_STARTS_RESTRICTION_CHECK_CALLER_TARGET_SDK = "default_fgs_starts_restriction_check_caller_target_sdk";
    private static final String KEY_DEFAULT_FGS_STARTS_RESTRICTION_ENABLED = "default_fgs_starts_restriction_enabled";
    private static final String KEY_DEFAULT_FGS_STARTS_RESTRICTION_NOTIFICATION_ENABLED = "default_fgs_starts_restriction_notification_enabled";
    private static final String KEY_DEFERRED_FGS_NOTIFICATIONS_API_GATED = "deferred_fgs_notifications_api_gated";
    private static final String KEY_DEFERRED_FGS_NOTIFICATIONS_ENABLED = "deferred_fgs_notifications_enabled";
    private static final String KEY_DEFERRED_FGS_NOTIFICATION_EXCLUSION_TIME = "deferred_fgs_notification_exclusion_time";
    private static final String KEY_DEFERRED_FGS_NOTIFICATION_INTERVAL = "deferred_fgs_notification_interval";
    private static final String KEY_DEFER_BOOT_COMPLETED_BROADCAST = "defer_boot_completed_broadcast";
    static final String KEY_ENABLE_COMPONENT_ALIAS = "enable_experimental_component_alias";
    static final String KEY_ENABLE_EXTRA_SERVICE_RESTART_DELAY_ON_MEM_PRESSURE = "enable_extra_delay_svc_restart_mem_pressure";
    static final String KEY_EXTRA_SERVICE_RESTART_DELAY_ON_MEM_PRESSURE = "extra_delay_svc_restart_mem_pressure";
    private static final String KEY_FGSERVICE_MIN_REPORT_TIME = "fgservice_min_report_time";
    private static final String KEY_FGSERVICE_MIN_SHOWN_TIME = "fgservice_min_shown_time";
    private static final String KEY_FGSERVICE_SCREEN_ON_AFTER_TIME = "fgservice_screen_on_after_time";
    private static final String KEY_FGSERVICE_SCREEN_ON_BEFORE_TIME = "fgservice_screen_on_before_time";
    static final String KEY_FGS_ALLOW_OPT_OUT = "fgs_allow_opt_out";
    static final String KEY_FGS_ATOM_SAMPLE_RATE = "fgs_atom_sample_rate";
    static final String KEY_FGS_START_ALLOWED_LOG_SAMPLE_RATE = "fgs_start_allowed_log_sample_rate";
    static final String KEY_FGS_START_DENIED_LOG_SAMPLE_RATE = "fgs_start_denied_log_sample_rate";
    static final String KEY_FGS_START_FOREGROUND_TIMEOUT = "fgs_start_foreground_timeout";
    static final String KEY_FG_TO_BG_FGS_GRACE_DURATION = "fg_to_bg_fgs_grace_duration";
    private static final String KEY_FORCE_BACKGROUND_CHECK_ON_RESTRICTED_APPS = "force_bg_check_on_restricted";
    private static final String KEY_FULL_PSS_LOWERED_INTERVAL = "full_pss_lowered_interval";
    private static final String KEY_FULL_PSS_MIN_INTERVAL = "full_pss_min_interval";
    private static final String KEY_GC_MIN_INTERVAL = "gc_min_interval";
    private static final String KEY_GC_TIMEOUT = "gc_timeout";
    private static final String KEY_IMPERCEPTIBLE_KILL_EXEMPT_PACKAGES = "imperceptible_kill_exempt_packages";
    private static final String KEY_IMPERCEPTIBLE_KILL_EXEMPT_PROC_STATES = "imperceptible_kill_exempt_proc_states";
    static final String KEY_KILL_BG_RESTRICTED_CACHED_IDLE = "kill_bg_restricted_cached_idle";
    static final String KEY_KILL_BG_RESTRICTED_CACHED_IDLE_SETTLE_TIME = "kill_bg_restricted_cached_idle_settle_time";
    private static final String KEY_MAX_CACHED_PROCESSES = "max_cached_processes";
    private static final String KEY_MAX_EMPTY_TIME_MILLIS = "max_empty_time_millis";
    private static final String KEY_MAX_PHANTOM_PROCESSES = "max_phantom_processes";
    static final String KEY_MAX_SERVICE_INACTIVITY = "service_max_inactivity";
    static final String KEY_MEMORY_INFO_THROTTLE_TIME = "memory_info_throttle_time";
    private static final String KEY_MIN_ASSOC_LOG_DURATION = "min_assoc_log_duration";
    static final String KEY_MIN_CRASH_INTERVAL = "min_crash_interval";
    static final String KEY_NETWORK_ACCESS_TIMEOUT_MS = "network_access_timeout_ms";
    private static final String KEY_NO_KILL_CACHED_PROCESSES_POST_BOOT_COMPLETED_DURATION_MILLIS = "no_kill_cached_processes_post_boot_completed_duration_millis";
    private static final String KEY_NO_KILL_CACHED_PROCESSES_UNTIL_BOOT_COMPLETED = "no_kill_cached_processes_until_boot_completed";
    private static final String KEY_OOMADJ_UPDATE_POLICY = "oomadj_update_policy";
    static final String KEY_PENDINGINTENT_WARNING_THRESHOLD = "pendingintent_warning_threshold";
    private static final String KEY_POWER_CHECK_INTERVAL = "power_check_interval";
    private static final String KEY_POWER_CHECK_MAX_CPU_1 = "power_check_max_cpu_1";
    private static final String KEY_POWER_CHECK_MAX_CPU_2 = "power_check_max_cpu_2";
    private static final String KEY_POWER_CHECK_MAX_CPU_3 = "power_check_max_cpu_3";
    private static final String KEY_POWER_CHECK_MAX_CPU_4 = "power_check_max_cpu_4";
    static final String KEY_PROCESS_CRASH_COUNT_LIMIT = "process_crash_count_limit";
    static final String KEY_PROCESS_CRASH_COUNT_RESET_INTERVAL = "process_crash_count_reset_interval";
    private static final String KEY_PROCESS_KILL_TIMEOUT = "process_kill_timeout";
    static final String KEY_PROCESS_START_ASYNC = "process_start_async";
    private static final String KEY_PUSH_MESSAGING_OVER_QUOTA_BEHAVIOR = "push_messaging_over_quota_behavior";
    static final String KEY_SERVICE_BG_ACTIVITY_START_TIMEOUT = "service_bg_activity_start_timeout";
    private static final String KEY_SERVICE_BIND_ALMOST_PERCEPTIBLE_TIMEOUT_MS = "service_bind_almost_perceptible_timeout_ms";
    static final String KEY_SERVICE_MIN_RESTART_TIME_BETWEEN = "service_min_restart_time_between";
    static final String KEY_SERVICE_RESET_RUN_DURATION = "service_reset_run_duration";
    static final String KEY_SERVICE_RESTART_DURATION = "service_restart_duration";
    static final String KEY_SERVICE_RESTART_DURATION_FACTOR = "service_restart_duration_factor";
    private static final String KEY_SERVICE_START_FOREGROUND_ANR_DELAY_MS = "service_start_foreground_anr_delay_ms";
    private static final String KEY_SERVICE_START_FOREGROUND_TIMEOUT_MS = "service_start_foreground_timeout_ms";
    private static final String KEY_SERVICE_USAGE_INTERACTION_TIME_POST_S = "service_usage_interaction_time_post_s";
    private static final String KEY_SERVICE_USAGE_INTERACTION_TIME_PRE_S = "service_usage_interaction_time";
    static final String KEY_TOP_TO_ALMOST_PERCEPTIBLE_GRACE_DURATION = "top_to_almost_perceptible_grace_duration";
    static final String KEY_TOP_TO_FGS_GRACE_DURATION = "top_to_fgs_grace_duration";
    private static final String KEY_USAGE_STATS_INTERACTION_INTERVAL_POST_S = "usage_stats_interaction_interval_post_s";
    private static final String KEY_USAGE_STATS_INTERACTION_INTERVAL_PRE_S = "usage_stats_interaction_interval";
    public static long MIN_ASSOC_LOG_DURATION = 0;
    private static final long MIN_AUTOMATIC_HEAP_DUMP_PSS_THRESHOLD_BYTES = 102400;
    public static int MIN_CRASH_INTERVAL = 0;
    private static final int OOMADJ_UPDATE_POLICY_QUICK = 1;
    private static final int OOMADJ_UPDATE_POLICY_SLOW = 0;
    static int PROCESS_CRASH_COUNT_LIMIT = 0;
    static long PROCESS_CRASH_COUNT_RESET_INTERVAL = 0;
    private static final String TAG = "ActivityManagerConstants";
    public long BACKGROUND_SETTLE_TIME;
    public long BG_START_TIMEOUT;
    public long BOUND_SERVICE_CRASH_RESTART_DURATION;
    public long BOUND_SERVICE_MAX_CRASH_RETRY;
    long CONTENT_PROVIDER_RETAIN_TIME;
    public int CUR_MAX_CACHED_PROCESSES;
    public int CUR_MAX_EMPTY_PROCESSES;
    public int CUR_TRIM_CACHED_PROCESSES;
    public int CUR_TRIM_EMPTY_PROCESSES;
    public long FGSERVICE_MIN_REPORT_TIME;
    public long FGSERVICE_MIN_SHOWN_TIME;
    public long FGSERVICE_SCREEN_ON_AFTER_TIME;
    public long FGSERVICE_SCREEN_ON_BEFORE_TIME;
    public boolean FLAG_PROCESS_START_ASYNC;
    boolean FORCE_BACKGROUND_CHECK_ON_RESTRICTED_APPS;
    long FULL_PSS_LOWERED_INTERVAL;
    long FULL_PSS_MIN_INTERVAL;
    long GC_MIN_INTERVAL;
    long GC_TIMEOUT;
    public ArraySet<String> IMPERCEPTIBLE_KILL_EXEMPT_PACKAGES;
    public ArraySet<Integer> IMPERCEPTIBLE_KILL_EXEMPT_PROC_STATES;
    public final ArraySet<ComponentName> KEEP_WARMING_SERVICES;
    public int MAX_CACHED_PROCESSES;
    public int MAX_PHANTOM_PROCESSES;
    public long MAX_SERVICE_INACTIVITY;
    public long MEMORY_INFO_THROTTLE_TIME;
    public boolean OOMADJ_UPDATE_QUICK;
    public int PENDINGINTENT_WARNING_THRESHOLD;
    long POWER_CHECK_INTERVAL;
    int POWER_CHECK_MAX_CPU_1;
    int POWER_CHECK_MAX_CPU_2;
    int POWER_CHECK_MAX_CPU_3;
    int POWER_CHECK_MAX_CPU_4;
    public long SERVICE_BG_ACTIVITY_START_TIMEOUT;
    public long SERVICE_MIN_RESTART_TIME_BETWEEN;
    public long SERVICE_RESET_RUN_DURATION;
    public long SERVICE_RESTART_DURATION;
    public int SERVICE_RESTART_DURATION_FACTOR;
    long SERVICE_USAGE_INTERACTION_TIME_POST_S;
    long SERVICE_USAGE_INTERACTION_TIME_PRE_S;
    public long TOP_TO_ALMOST_PERCEPTIBLE_GRACE_DURATION;
    public long TOP_TO_FGS_GRACE_DURATION;
    long USAGE_STATS_INTERACTION_INTERVAL_POST_S;
    long USAGE_STATS_INTERACTION_INTERVAL_PRE_S;
    volatile long mBootTimeTempAllowlistDuration;
    volatile String mComponentAliasOverrides;
    private final int mCustomizedMaxCachedProcesses;
    private final int mDefaultBinderHeavyHitterAutoSamplerBatchSize;
    private final boolean mDefaultBinderHeavyHitterAutoSamplerEnabled;
    private final float mDefaultBinderHeavyHitterAutoSamplerThreshold;
    private final int mDefaultBinderHeavyHitterWatcherBatchSize;
    private final boolean mDefaultBinderHeavyHitterWatcherEnabled;
    private final float mDefaultBinderHeavyHitterWatcherThreshold;
    private List<String> mDefaultImperceptibleKillExemptPackages;
    private List<Integer> mDefaultImperceptibleKillExemptProcStates;
    volatile int mDeferBootCompletedBroadcast;
    volatile boolean mEnableComponentAlias;
    boolean mEnableExtraServiceRestartDelayOnMemPressure;
    long[] mExtraServiceRestartDelayOnMemPressure;
    volatile long mFgToBgFgsGraceDuration;
    volatile boolean mFgsAllowOptOut;
    volatile float mFgsAtomSampleRate;
    volatile long mFgsNotificationDeferralExclusionTime;
    volatile long mFgsNotificationDeferralInterval;
    volatile float mFgsStartAllowedLogSampleRate;
    volatile float mFgsStartDeniedLogSampleRate;
    volatile long mFgsStartForegroundTimeoutMs;
    volatile boolean mFgsStartRestrictionCheckCallerTargetSdk;
    volatile boolean mFgsStartRestrictionNotificationEnabled;
    volatile boolean mFlagActivityStartsLoggingEnabled;
    volatile boolean mFlagBackgroundActivityStartsEnabled;
    volatile boolean mFlagBackgroundFgsStartRestrictionEnabled;
    volatile boolean mFlagFgsNotificationDeferralApiGated;
    volatile boolean mFlagFgsNotificationDeferralEnabled;
    volatile boolean mFlagFgsStartRestrictionEnabled;
    volatile boolean mFlagForegroundServiceStartsLoggingEnabled;
    volatile boolean mKillBgRestrictedAndCachedIdle;
    volatile long mKillBgRestrictedAndCachedIdleSettleTimeMs;
    volatile long mMaxEmptyTimeMillis;
    volatile long mNetworkAccessTimeoutMs;
    volatile long mNoKillCachedProcessesPostBootCompletedDurationMillis;
    volatile boolean mNoKillCachedProcessesUntilBootCompleted;
    private final DeviceConfig.OnPropertiesChangedListener mOnDeviceConfigChangedForComponentAliasListener;
    private final DeviceConfig.OnPropertiesChangedListener mOnDeviceConfigChangedListener;
    private int mOverrideMaxCachedProcesses;
    private final KeyValueListParser mParser;
    volatile long mProcessKillTimeoutMs;
    volatile int mPushMessagingOverQuotaBehavior;
    private ContentResolver mResolver;
    private final ActivityManagerService mService;
    volatile long mServiceBindAlmostPerceptibleTimeoutMs;
    volatile int mServiceStartForegroundAnrDelayMs;
    volatile int mServiceStartForegroundTimeoutMs;
    private final boolean mSystemServerAutomaticHeapDumpEnabled;
    private final String mSystemServerAutomaticHeapDumpPackageName;
    private long mSystemServerAutomaticHeapDumpPssThresholdBytes;

    static {
        DEFAULT_POWER_CHECK_INTERVAL = (ActivityManagerDebugConfig.DEBUG_POWER_QUICK ? 1 : 5) * 60 * 1000;
        DEFAULT_EXTRA_SERVICE_RESTART_DELAY_ON_MEM_PRESSURE = new long[]{0, JobStatus.DEFAULT_TRIGGER_UPDATE_DELAY, 20000, 30000};
        MIN_CRASH_INTERVAL = DEFAULT_MIN_CRASH_INTERVAL;
        PROCESS_CRASH_COUNT_RESET_INTERVAL = 43200000L;
        PROCESS_CRASH_COUNT_LIMIT = 12;
        DEFAULT_NO_KILL_CACHED_PROCESSES_UNTIL_BOOT_COMPLETED = SystemProperties.getBoolean(KEY_NO_KILL_CACHED_PROCESSES_UNTIL_BOOT_COMPLETED, true);
        DEFAULT_NO_KILL_CACHED_PROCESSES_POST_BOOT_COMPLETED_DURATION_MILLIS = SystemProperties.getInt(KEY_NO_KILL_CACHED_PROCESSES_POST_BOOT_COMPLETED_DURATION_MILLIS, (int) FrameworkStatsLog.DEVICE_POLICY_EVENT__EVENT_ID__CREDENTIAL_MANAGEMENT_APP_REQUEST_ACCEPTED) * 1000;
        DEFAULT_MAX_EMPTY_TIME_MILLIS = SystemProperties.getInt(KEY_MAX_EMPTY_TIME_MILLIS, 1800) * 1000;
        ACTIVITY_MANAGER_CONSTANTS_URI = Settings.Global.getUriFor("activity_manager_constants");
        ACTIVITY_STARTS_LOGGING_ENABLED_URI = Settings.Global.getUriFor("activity_starts_logging_enabled");
        FOREGROUND_SERVICE_STARTS_LOGGING_ENABLED_URI = Settings.Global.getUriFor("foreground_service_starts_logging_enabled");
        ENABLE_AUTOMATIC_SYSTEM_SERVER_HEAP_DUMPS_URI = Settings.Global.getUriFor("enable_automatic_system_server_heap_dumps");
        MIN_ASSOC_LOG_DURATION = BackupAgentTimeoutParameters.DEFAULT_FULL_BACKUP_AGENT_TIMEOUT_MILLIS;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public ActivityManagerConstants(Context context, ActivityManagerService service, Handler handler) {
        super(handler);
        this.MAX_CACHED_PROCESSES = 32;
        this.BACKGROUND_SETTLE_TIME = 60000L;
        this.FGSERVICE_MIN_SHOWN_TIME = DEFAULT_FGSERVICE_MIN_SHOWN_TIME;
        this.FGSERVICE_MIN_REPORT_TIME = 3000L;
        this.FGSERVICE_SCREEN_ON_BEFORE_TIME = 1000L;
        this.FGSERVICE_SCREEN_ON_AFTER_TIME = 5000L;
        this.CONTENT_PROVIDER_RETAIN_TIME = 20000L;
        this.GC_TIMEOUT = 5000L;
        this.GC_MIN_INTERVAL = 60000L;
        boolean z = true;
        this.FORCE_BACKGROUND_CHECK_ON_RESTRICTED_APPS = true;
        this.FULL_PSS_MIN_INTERVAL = DEFAULT_FULL_PSS_MIN_INTERVAL;
        this.FULL_PSS_LOWERED_INTERVAL = BackupAgentTimeoutParameters.DEFAULT_FULL_BACKUP_AGENT_TIMEOUT_MILLIS;
        this.POWER_CHECK_INTERVAL = DEFAULT_POWER_CHECK_INTERVAL;
        this.POWER_CHECK_MAX_CPU_1 = 25;
        this.POWER_CHECK_MAX_CPU_2 = 25;
        this.POWER_CHECK_MAX_CPU_3 = 10;
        this.POWER_CHECK_MAX_CPU_4 = 2;
        this.SERVICE_USAGE_INTERACTION_TIME_PRE_S = 1800000L;
        this.SERVICE_USAGE_INTERACTION_TIME_POST_S = 60000L;
        this.USAGE_STATS_INTERACTION_INTERVAL_PRE_S = 7200000L;
        this.USAGE_STATS_INTERACTION_INTERVAL_POST_S = 600000L;
        this.SERVICE_RESTART_DURATION = 1000L;
        this.SERVICE_RESET_RUN_DURATION = 60000L;
        this.SERVICE_RESTART_DURATION_FACTOR = 4;
        this.SERVICE_MIN_RESTART_TIME_BETWEEN = JobStatus.DEFAULT_TRIGGER_UPDATE_DELAY;
        this.MAX_SERVICE_INACTIVITY = 1800000L;
        this.BG_START_TIMEOUT = 15000L;
        this.SERVICE_BG_ACTIVITY_START_TIMEOUT = JobStatus.DEFAULT_TRIGGER_UPDATE_DELAY;
        this.BOUND_SERVICE_CRASH_RESTART_DURATION = 1800000L;
        this.BOUND_SERVICE_MAX_CRASH_RETRY = 16L;
        this.FLAG_PROCESS_START_ASYNC = true;
        this.MEMORY_INFO_THROTTLE_TIME = BackupAgentTimeoutParameters.DEFAULT_FULL_BACKUP_AGENT_TIMEOUT_MILLIS;
        this.TOP_TO_FGS_GRACE_DURATION = 15000L;
        this.TOP_TO_ALMOST_PERCEPTIBLE_GRACE_DURATION = 15000L;
        this.mFlagBackgroundFgsStartRestrictionEnabled = true;
        this.mFlagFgsStartRestrictionEnabled = true;
        this.mFgsStartRestrictionNotificationEnabled = false;
        this.mFgsStartRestrictionCheckCallerTargetSdk = true;
        this.mFlagFgsNotificationDeferralEnabled = true;
        this.mFlagFgsNotificationDeferralApiGated = false;
        this.mFgsNotificationDeferralInterval = JobStatus.DEFAULT_TRIGGER_UPDATE_DELAY;
        this.mFgsNotificationDeferralExclusionTime = 120000L;
        this.mPushMessagingOverQuotaBehavior = 1;
        this.mBootTimeTempAllowlistDuration = 20000L;
        this.mFgToBgFgsGraceDuration = 5000L;
        this.mFgsStartForegroundTimeoutMs = JobStatus.DEFAULT_TRIGGER_UPDATE_DELAY;
        this.mFgsAtomSampleRate = 1.0f;
        this.mFgsStartAllowedLogSampleRate = DEFAULT_FGS_START_ALLOWED_LOG_SAMPLE_RATE;
        this.mFgsStartDeniedLogSampleRate = 1.0f;
        this.mKillBgRestrictedAndCachedIdle = true;
        this.mKillBgRestrictedAndCachedIdleSettleTimeMs = 60000L;
        this.mProcessKillTimeoutMs = JobStatus.DEFAULT_TRIGGER_UPDATE_DELAY;
        this.mFgsAllowOptOut = false;
        this.mExtraServiceRestartDelayOnMemPressure = DEFAULT_EXTRA_SERVICE_RESTART_DELAY_ON_MEM_PRESSURE;
        this.mEnableExtraServiceRestartDelayOnMemPressure = true;
        this.mEnableComponentAlias = false;
        this.mDeferBootCompletedBroadcast = 6;
        this.mServiceStartForegroundTimeoutMs = 30000;
        this.mServiceStartForegroundAnrDelayMs = 10000;
        this.mServiceBindAlmostPerceptibleTimeoutMs = 15000L;
        this.mComponentAliasOverrides = "";
        this.mParser = new KeyValueListParser(',');
        this.mOverrideMaxCachedProcesses = -1;
        this.mNoKillCachedProcessesUntilBootCompleted = DEFAULT_NO_KILL_CACHED_PROCESSES_UNTIL_BOOT_COMPLETED;
        this.mNoKillCachedProcessesPostBootCompletedDurationMillis = DEFAULT_NO_KILL_CACHED_PROCESSES_POST_BOOT_COMPLETED_DURATION_MILLIS;
        this.CUR_TRIM_EMPTY_PROCESSES = computeEmptyProcessLimit(this.MAX_CACHED_PROCESSES) / 2;
        int i = this.MAX_CACHED_PROCESSES;
        this.CUR_TRIM_CACHED_PROCESSES = (i - computeEmptyProcessLimit(i)) / 3;
        this.mMaxEmptyTimeMillis = DEFAULT_MAX_EMPTY_TIME_MILLIS;
        this.IMPERCEPTIBLE_KILL_EXEMPT_PACKAGES = new ArraySet<>();
        this.IMPERCEPTIBLE_KILL_EXEMPT_PROC_STATES = new ArraySet<>();
        this.PENDINGINTENT_WARNING_THRESHOLD = 2000;
        ArraySet<ComponentName> arraySet = new ArraySet<>();
        this.KEEP_WARMING_SERVICES = arraySet;
        this.MAX_PHANTOM_PROCESSES = 32;
        this.mNetworkAccessTimeoutMs = DEFAULT_NETWORK_ACCESS_TIMEOUT_MS;
        this.OOMADJ_UPDATE_QUICK = true;
        this.mOnDeviceConfigChangedListener = new DeviceConfig.OnPropertiesChangedListener() { // from class: com.android.server.am.ActivityManagerConstants.1
            public void onPropertiesChanged(DeviceConfig.Properties properties) {
                String name;
                Iterator it = properties.getKeyset().iterator();
                while (it.hasNext() && (name = (String) it.next()) != null) {
                    char c = 65535;
                    switch (name.hashCode()) {
                        case -2074391906:
                            if (name.equals(ActivityManagerConstants.KEY_IMPERCEPTIBLE_KILL_EXEMPT_PROC_STATES)) {
                                c = '\r';
                                break;
                            }
                            break;
                        case -1996097272:
                            if (name.equals(ActivityManagerConstants.KEY_KILL_BG_RESTRICTED_CACHED_IDLE_SETTLE_TIME)) {
                                c = 30;
                                break;
                            }
                            break;
                        case -1905817813:
                            if (name.equals(ActivityManagerConstants.KEY_DEFAULT_FGS_STARTS_RESTRICTION_ENABLED)) {
                                c = 3;
                                break;
                            }
                            break;
                        case -1903697007:
                            if (name.equals(ActivityManagerConstants.KEY_SERVICE_START_FOREGROUND_ANR_DELAY_MS)) {
                                c = '%';
                                break;
                            }
                            break;
                        case -1830853932:
                            if (name.equals(ActivityManagerConstants.KEY_BINDER_HEAVY_HITTER_WATCHER_BATCHSIZE)) {
                                c = 17;
                                break;
                            }
                            break;
                        case -1782036688:
                            if (name.equals(ActivityManagerConstants.KEY_DEFAULT_BACKGROUND_ACTIVITY_STARTS_ENABLED)) {
                                c = 1;
                                break;
                            }
                            break;
                        case -1660341473:
                            if (name.equals(ActivityManagerConstants.KEY_FGS_ALLOW_OPT_OUT)) {
                                c = 31;
                                break;
                            }
                            break;
                        case -1640024320:
                            if (name.equals(ActivityManagerConstants.KEY_FGS_START_DENIED_LOG_SAMPLE_RATE)) {
                                c = 28;
                                break;
                            }
                            break;
                        case -1406935837:
                            if (name.equals(ActivityManagerConstants.KEY_OOMADJ_UPDATE_POLICY)) {
                                c = 11;
                                break;
                            }
                            break;
                        case -1303617396:
                            if (name.equals(ActivityManagerConstants.KEY_DEFERRED_FGS_NOTIFICATION_INTERVAL)) {
                                c = '\b';
                                break;
                            }
                            break;
                        case -1220759920:
                            if (name.equals(ActivityManagerConstants.KEY_MAX_PHANTOM_PROCESSES)) {
                                c = 22;
                                break;
                            }
                            break;
                        case -1198352864:
                            if (name.equals(ActivityManagerConstants.KEY_DEFAULT_BACKGROUND_FGS_STARTS_RESTRICTION_ENABLED)) {
                                c = 2;
                                break;
                            }
                            break;
                        case -1191409506:
                            if (name.equals(ActivityManagerConstants.KEY_FORCE_BACKGROUND_CHECK_ON_RESTRICTED_APPS)) {
                                c = 14;
                                break;
                            }
                            break;
                        case -1092962821:
                            if (name.equals(ActivityManagerConstants.KEY_MAX_CACHED_PROCESSES)) {
                                c = 0;
                                break;
                            }
                            break;
                        case -1055864341:
                            if (name.equals(ActivityManagerConstants.KEY_MAX_EMPTY_TIME_MILLIS)) {
                                c = ')';
                                break;
                            }
                            break;
                        case -964261074:
                            if (name.equals(ActivityManagerConstants.KEY_NETWORK_ACCESS_TIMEOUT_MS)) {
                                c = '*';
                                break;
                            }
                            break;
                        case -815375578:
                            if (name.equals(ActivityManagerConstants.KEY_MIN_ASSOC_LOG_DURATION)) {
                                c = 15;
                                break;
                            }
                            break;
                        case -769365680:
                            if (name.equals(ActivityManagerConstants.KEY_PROCESS_KILL_TIMEOUT)) {
                                c = '\"';
                                break;
                            }
                            break;
                        case -682752716:
                            if (name.equals(ActivityManagerConstants.KEY_FGS_ATOM_SAMPLE_RATE)) {
                                c = 26;
                                break;
                            }
                            break;
                        case -577670375:
                            if (name.equals(ActivityManagerConstants.KEY_SERVICE_START_FOREGROUND_TIMEOUT_MS)) {
                                c = '$';
                                break;
                            }
                            break;
                        case -449032007:
                            if (name.equals(ActivityManagerConstants.KEY_FGS_START_ALLOWED_LOG_SAMPLE_RATE)) {
                                c = 27;
                                break;
                            }
                            break;
                        case -329920445:
                            if (name.equals(ActivityManagerConstants.KEY_DEFAULT_FGS_STARTS_RESTRICTION_NOTIFICATION_ENABLED)) {
                                c = 4;
                                break;
                            }
                            break;
                        case -292047334:
                            if (name.equals(ActivityManagerConstants.KEY_BINDER_HEAVY_HITTER_WATCHER_ENABLED)) {
                                c = 16;
                                break;
                            }
                            break;
                        case -253203740:
                            if (name.equals(ActivityManagerConstants.KEY_PUSH_MESSAGING_OVER_QUOTA_BEHAVIOR)) {
                                c = '\n';
                                break;
                            }
                            break;
                        case -224039213:
                            if (name.equals(ActivityManagerConstants.KEY_NO_KILL_CACHED_PROCESSES_POST_BOOT_COMPLETED_DURATION_MILLIS)) {
                                c = '(';
                                break;
                            }
                            break;
                        case -216971728:
                            if (name.equals(ActivityManagerConstants.KEY_DEFERRED_FGS_NOTIFICATIONS_API_GATED)) {
                                c = 7;
                                break;
                            }
                            break;
                        case -48740806:
                            if (name.equals(ActivityManagerConstants.KEY_IMPERCEPTIBLE_KILL_EXEMPT_PACKAGES)) {
                                c = '\f';
                                break;
                            }
                            break;
                        case 21817133:
                            if (name.equals(ActivityManagerConstants.KEY_DEFER_BOOT_COMPLETED_BROADCAST)) {
                                c = '#';
                                break;
                            }
                            break;
                        case 273690789:
                            if (name.equals(ActivityManagerConstants.KEY_ENABLE_EXTRA_SERVICE_RESTART_DELAY_ON_MEM_PRESSURE)) {
                                c = '!';
                                break;
                            }
                            break;
                        case 628754725:
                            if (name.equals(ActivityManagerConstants.KEY_DEFERRED_FGS_NOTIFICATION_EXCLUSION_TIME)) {
                                c = '\t';
                                break;
                            }
                            break;
                        case 886770227:
                            if (name.equals(ActivityManagerConstants.KEY_DEFAULT_FGS_STARTS_RESTRICTION_CHECK_CALLER_TARGET_SDK)) {
                                c = 5;
                                break;
                            }
                            break;
                        case 889934779:
                            if (name.equals(ActivityManagerConstants.KEY_NO_KILL_CACHED_PROCESSES_UNTIL_BOOT_COMPLETED)) {
                                c = '\'';
                                break;
                            }
                            break;
                        case 969545596:
                            if (name.equals(ActivityManagerConstants.KEY_FG_TO_BG_FGS_GRACE_DURATION)) {
                                c = 24;
                                break;
                            }
                            break;
                        case 1163990130:
                            if (name.equals(ActivityManagerConstants.KEY_BOOT_TIME_TEMP_ALLOWLIST_DURATION)) {
                                c = 23;
                                break;
                            }
                            break;
                        case 1199252102:
                            if (name.equals(ActivityManagerConstants.KEY_KILL_BG_RESTRICTED_CACHED_IDLE)) {
                                c = 29;
                                break;
                            }
                            break;
                        case 1351914345:
                            if (name.equals(ActivityManagerConstants.KEY_EXTRA_SERVICE_RESTART_DELAY_ON_MEM_PRESSURE)) {
                                c = ' ';
                                break;
                            }
                            break;
                        case 1380211165:
                            if (name.equals(ActivityManagerConstants.KEY_DEFERRED_FGS_NOTIFICATIONS_ENABLED)) {
                                c = 6;
                                break;
                            }
                            break;
                        case 1509836936:
                            if (name.equals(ActivityManagerConstants.KEY_BINDER_HEAVY_HITTER_AUTO_SAMPLER_THRESHOLD)) {
                                c = 21;
                                break;
                            }
                            break;
                        case 1598050974:
                            if (name.equals(ActivityManagerConstants.KEY_BINDER_HEAVY_HITTER_AUTO_SAMPLER_ENABLED)) {
                                c = 19;
                                break;
                            }
                            break;
                        case 1626346799:
                            if (name.equals(ActivityManagerConstants.KEY_FGS_START_FOREGROUND_TIMEOUT)) {
                                c = 25;
                                break;
                            }
                            break;
                        case 1896529156:
                            if (name.equals(ActivityManagerConstants.KEY_BINDER_HEAVY_HITTER_WATCHER_THRESHOLD)) {
                                c = 18;
                                break;
                            }
                            break;
                        case 2013655783:
                            if (name.equals(ActivityManagerConstants.KEY_SERVICE_BIND_ALMOST_PERCEPTIBLE_TIMEOUT_MS)) {
                                c = '&';
                                break;
                            }
                            break;
                        case 2077421144:
                            if (name.equals(ActivityManagerConstants.KEY_BINDER_HEAVY_HITTER_AUTO_SAMPLER_BATCHSIZE)) {
                                c = 20;
                                break;
                            }
                            break;
                    }
                    switch (c) {
                        case 0:
                            ActivityManagerConstants.this.updateMaxCachedProcesses();
                            break;
                        case 1:
                            ActivityManagerConstants.this.updateBackgroundActivityStarts();
                            break;
                        case 2:
                            ActivityManagerConstants.this.updateBackgroundFgsStartsRestriction();
                            break;
                        case 3:
                            ActivityManagerConstants.this.updateFgsStartsRestriction();
                            break;
                        case 4:
                            ActivityManagerConstants.this.updateFgsStartsRestrictionNotification();
                            break;
                        case 5:
                            ActivityManagerConstants.this.updateFgsStartsRestrictionCheckCallerTargetSdk();
                            break;
                        case 6:
                            ActivityManagerConstants.this.updateFgsNotificationDeferralEnable();
                            break;
                        case 7:
                            ActivityManagerConstants.this.updateFgsNotificationDeferralApiGated();
                            break;
                        case '\b':
                            ActivityManagerConstants.this.updateFgsNotificationDeferralInterval();
                            break;
                        case '\t':
                            ActivityManagerConstants.this.updateFgsNotificationDeferralExclusionTime();
                            break;
                        case '\n':
                            ActivityManagerConstants.this.updatePushMessagingOverQuotaBehavior();
                            break;
                        case 11:
                            ActivityManagerConstants.this.updateOomAdjUpdatePolicy();
                            break;
                        case '\f':
                        case '\r':
                            ActivityManagerConstants.this.updateImperceptibleKillExemptions();
                            break;
                        case 14:
                            ActivityManagerConstants.this.updateForceRestrictedBackgroundCheck();
                            break;
                        case 15:
                            ActivityManagerConstants.this.updateMinAssocLogDuration();
                            break;
                        case 16:
                        case 17:
                        case 18:
                        case 19:
                        case 20:
                        case 21:
                            ActivityManagerConstants.this.updateBinderHeavyHitterWatcher();
                            break;
                        case 22:
                            ActivityManagerConstants.this.updateMaxPhantomProcesses();
                            break;
                        case 23:
                            ActivityManagerConstants.this.updateBootTimeTempAllowListDuration();
                            break;
                        case 24:
                            ActivityManagerConstants.this.updateFgToBgFgsGraceDuration();
                            break;
                        case 25:
                            ActivityManagerConstants.this.updateFgsStartForegroundTimeout();
                            break;
                        case 26:
                            ActivityManagerConstants.this.updateFgsAtomSamplePercent();
                            break;
                        case 27:
                            ActivityManagerConstants.this.updateFgsStartAllowedLogSamplePercent();
                            break;
                        case 28:
                            ActivityManagerConstants.this.updateFgsStartDeniedLogSamplePercent();
                            break;
                        case 29:
                            ActivityManagerConstants.this.updateKillBgRestrictedCachedIdle();
                            break;
                        case 30:
                            ActivityManagerConstants.this.updateKillBgRestrictedCachedIdleSettleTime();
                            break;
                        case 31:
                            ActivityManagerConstants.this.updateFgsAllowOptOut();
                            break;
                        case ' ':
                            ActivityManagerConstants.this.updateExtraServiceRestartDelayOnMemPressure();
                            break;
                        case '!':
                            ActivityManagerConstants.this.updateEnableExtraServiceRestartDelayOnMemPressure();
                            break;
                        case '\"':
                            ActivityManagerConstants.this.updateProcessKillTimeout();
                            break;
                        case '#':
                            ActivityManagerConstants.this.updateDeferBootCompletedBroadcast();
                            break;
                        case '$':
                            ActivityManagerConstants.this.updateServiceStartForegroundTimeoutMs();
                            break;
                        case '%':
                            ActivityManagerConstants.this.updateServiceStartForegroundAnrDealyMs();
                            break;
                        case '&':
                            ActivityManagerConstants.this.updateServiceBindAlmostPerceptibleTimeoutMs();
                            break;
                        case '\'':
                            ActivityManagerConstants.this.updateNoKillCachedProcessesUntilBootCompleted();
                            break;
                        case '(':
                            ActivityManagerConstants.this.updateNoKillCachedProcessesPostBootCompletedDurationMillis();
                            break;
                        case ')':
                            ActivityManagerConstants.this.updateMaxEmptyTimeMillis();
                            break;
                        case '*':
                            ActivityManagerConstants.this.updateNetworkAccessTimeoutMs();
                            break;
                    }
                }
            }
        };
        this.mOnDeviceConfigChangedForComponentAliasListener = new DeviceConfig.OnPropertiesChangedListener() { // from class: com.android.server.am.ActivityManagerConstants.2
            public void onPropertiesChanged(DeviceConfig.Properties properties) {
                String name;
                Iterator it = properties.getKeyset().iterator();
                while (it.hasNext() && (name = (String) it.next()) != null) {
                    char c = 65535;
                    switch (name.hashCode()) {
                        case -1542414221:
                            if (name.equals(ActivityManagerConstants.KEY_ENABLE_COMPONENT_ALIAS)) {
                                c = 0;
                                break;
                            }
                            break;
                        case 551822262:
                            if (name.equals(ActivityManagerConstants.KEY_COMPONENT_ALIAS_OVERRIDES)) {
                                c = 1;
                                break;
                            }
                            break;
                    }
                    switch (c) {
                        case 0:
                        case 1:
                            ActivityManagerConstants.this.updateComponentAliases();
                            break;
                    }
                }
            }
        };
        if (SystemProperties.get("ro.os_go.support").equals("1")) {
            this.MAX_CACHED_PROCESSES = 8;
        }
        this.mService = service;
        this.mSystemServerAutomaticHeapDumpEnabled = (Build.IS_DEBUGGABLE && context.getResources().getBoolean(17891582)) ? z : false;
        this.mSystemServerAutomaticHeapDumpPackageName = context.getPackageName();
        this.mSystemServerAutomaticHeapDumpPssThresholdBytes = Math.max((long) MIN_AUTOMATIC_HEAP_DUMP_PSS_THRESHOLD_BYTES, context.getResources().getInteger(17694779));
        this.mDefaultImperceptibleKillExemptPackages = Arrays.asList(context.getResources().getStringArray(17236020));
        this.mDefaultImperceptibleKillExemptProcStates = (List) Arrays.stream(context.getResources().getIntArray(17236021)).boxed().collect(Collectors.toList());
        this.IMPERCEPTIBLE_KILL_EXEMPT_PACKAGES.addAll(this.mDefaultImperceptibleKillExemptPackages);
        this.IMPERCEPTIBLE_KILL_EXEMPT_PROC_STATES.addAll(this.mDefaultImperceptibleKillExemptProcStates);
        boolean z2 = context.getResources().getBoolean(17891585);
        this.mDefaultBinderHeavyHitterWatcherEnabled = z2;
        int integer = context.getResources().getInteger(17694783);
        this.mDefaultBinderHeavyHitterWatcherBatchSize = integer;
        float f = context.getResources().getFloat(17105071);
        this.mDefaultBinderHeavyHitterWatcherThreshold = f;
        boolean z3 = context.getResources().getBoolean(17891584);
        this.mDefaultBinderHeavyHitterAutoSamplerEnabled = z3;
        int integer2 = context.getResources().getInteger(17694782);
        this.mDefaultBinderHeavyHitterAutoSamplerBatchSize = integer2;
        float f2 = context.getResources().getFloat(17105070);
        this.mDefaultBinderHeavyHitterAutoSamplerThreshold = f2;
        BINDER_HEAVY_HITTER_WATCHER_ENABLED = z2;
        BINDER_HEAVY_HITTER_WATCHER_BATCHSIZE = integer;
        BINDER_HEAVY_HITTER_WATCHER_THRESHOLD = f;
        BINDER_HEAVY_HITTER_AUTO_SAMPLER_ENABLED = z3;
        BINDER_HEAVY_HITTER_AUTO_SAMPLER_BATCHSIZE = integer2;
        BINDER_HEAVY_HITTER_AUTO_SAMPLER_THRESHOLD = f2;
        service.scheduleUpdateBinderHeavyHitterWatcherConfig();
        arraySet.addAll((Collection) Arrays.stream(context.getResources().getStringArray(17236080)).map(new Function() { // from class: com.android.server.am.ActivityManagerConstants$$ExternalSyntheticLambda1
            @Override // java.util.function.Function
            public final Object apply(Object obj) {
                return ComponentName.unflattenFromString((String) obj);
            }
        }).collect(Collectors.toSet()));
        int integer3 = context.getResources().getInteger(17694774);
        this.mCustomizedMaxCachedProcesses = integer3;
        this.CUR_MAX_CACHED_PROCESSES = integer3;
        if (ITranActivityManagerConstants.Instance().isAgaresEnable()) {
            this.CUR_MAX_CACHED_PROCESSES = ITranActivityManagerConstants.Instance().getAgaresMaxCachedProcesses(this.CUR_MAX_CACHED_PROCESSES);
        }
        if (ITranActivityManagerConstants.Instance().isGriffinEnable()) {
            this.MAX_CACHED_PROCESSES = ITranActivityManagerConstants.Instance().updatePmMaxCachedNumber(this.MAX_CACHED_PROCESSES);
            this.CUR_MAX_CACHED_PROCESSES = ITranActivityManagerConstants.Instance().updatePmCurMaxCachedNumber(this.CUR_MAX_CACHED_PROCESSES);
        }
        this.CUR_MAX_EMPTY_PROCESSES = computeEmptyProcessLimit(this.CUR_MAX_CACHED_PROCESSES);
    }

    public void start(ContentResolver resolver) {
        this.mResolver = resolver;
        resolver.registerContentObserver(ACTIVITY_MANAGER_CONSTANTS_URI, false, this);
        this.mResolver.registerContentObserver(ACTIVITY_STARTS_LOGGING_ENABLED_URI, false, this);
        this.mResolver.registerContentObserver(FOREGROUND_SERVICE_STARTS_LOGGING_ENABLED_URI, false, this);
        if (this.mSystemServerAutomaticHeapDumpEnabled) {
            this.mResolver.registerContentObserver(ENABLE_AUTOMATIC_SYSTEM_SERVER_HEAP_DUMPS_URI, false, this);
        }
        updateConstants();
        if (this.mSystemServerAutomaticHeapDumpEnabled) {
            updateEnableAutomaticSystemServerHeapDumps();
        }
        DeviceConfig.addOnPropertiesChangedListener("activity_manager", ActivityThread.currentApplication().getMainExecutor(), this.mOnDeviceConfigChangedListener);
        DeviceConfig.addOnPropertiesChangedListener("activity_manager_ca", ActivityThread.currentApplication().getMainExecutor(), this.mOnDeviceConfigChangedForComponentAliasListener);
        loadDeviceConfigConstants();
        updateActivityStartsLoggingEnabled();
        updateForegroundServiceStartsLoggingEnabled();
    }

    private void loadDeviceConfigConstants() {
        this.mOnDeviceConfigChangedListener.onPropertiesChanged(DeviceConfig.getProperties("activity_manager", new String[0]));
        this.mOnDeviceConfigChangedForComponentAliasListener.onPropertiesChanged(DeviceConfig.getProperties("activity_manager_ca", new String[0]));
    }

    public void setOverrideMaxCachedProcesses(int value) {
        this.mOverrideMaxCachedProcesses = value;
        updateMaxCachedProcesses();
    }

    public int getOverrideMaxCachedProcesses() {
        return this.mOverrideMaxCachedProcesses;
    }

    public static int computeEmptyProcessLimit(int totalProcessLimit) {
        return totalProcessLimit / 2;
    }

    @Override // android.database.ContentObserver
    public void onChange(boolean selfChange, Uri uri) {
        if (uri == null) {
            return;
        }
        if (ACTIVITY_MANAGER_CONSTANTS_URI.equals(uri)) {
            updateConstants();
        } else if (ACTIVITY_STARTS_LOGGING_ENABLED_URI.equals(uri)) {
            updateActivityStartsLoggingEnabled();
        } else if (FOREGROUND_SERVICE_STARTS_LOGGING_ENABLED_URI.equals(uri)) {
            updateForegroundServiceStartsLoggingEnabled();
        } else if (ENABLE_AUTOMATIC_SYSTEM_SERVER_HEAP_DUMPS_URI.equals(uri)) {
            updateEnableAutomaticSystemServerHeapDumps();
        }
    }

    private void updateConstants() {
        String setting = Settings.Global.getString(this.mResolver, "activity_manager_constants");
        synchronized (this.mService) {
            try {
                try {
                    ActivityManagerService.boostPriorityForLockedSection();
                    this.mParser.setString(setting);
                } catch (IllegalArgumentException e) {
                    Slog.e(TAG, "Bad activity manager config settings", e);
                }
                long currentPowerCheckInterval = this.POWER_CHECK_INTERVAL;
                this.BACKGROUND_SETTLE_TIME = this.mParser.getLong(KEY_BACKGROUND_SETTLE_TIME, 60000L);
                this.FGSERVICE_MIN_SHOWN_TIME = this.mParser.getLong(KEY_FGSERVICE_MIN_SHOWN_TIME, (long) DEFAULT_FGSERVICE_MIN_SHOWN_TIME);
                this.FGSERVICE_MIN_REPORT_TIME = this.mParser.getLong(KEY_FGSERVICE_MIN_REPORT_TIME, 3000L);
                this.FGSERVICE_SCREEN_ON_BEFORE_TIME = this.mParser.getLong(KEY_FGSERVICE_SCREEN_ON_BEFORE_TIME, 1000L);
                this.FGSERVICE_SCREEN_ON_AFTER_TIME = this.mParser.getLong(KEY_FGSERVICE_SCREEN_ON_AFTER_TIME, 5000L);
                this.CONTENT_PROVIDER_RETAIN_TIME = this.mParser.getLong(KEY_CONTENT_PROVIDER_RETAIN_TIME, 20000L);
                this.GC_TIMEOUT = this.mParser.getLong(KEY_GC_TIMEOUT, 5000L);
                this.GC_MIN_INTERVAL = this.mParser.getLong(KEY_GC_MIN_INTERVAL, 60000L);
                this.FULL_PSS_MIN_INTERVAL = this.mParser.getLong(KEY_FULL_PSS_MIN_INTERVAL, (long) DEFAULT_FULL_PSS_MIN_INTERVAL);
                this.FULL_PSS_LOWERED_INTERVAL = this.mParser.getLong(KEY_FULL_PSS_LOWERED_INTERVAL, (long) BackupAgentTimeoutParameters.DEFAULT_FULL_BACKUP_AGENT_TIMEOUT_MILLIS);
                this.POWER_CHECK_INTERVAL = this.mParser.getLong(KEY_POWER_CHECK_INTERVAL, DEFAULT_POWER_CHECK_INTERVAL);
                this.POWER_CHECK_MAX_CPU_1 = this.mParser.getInt(KEY_POWER_CHECK_MAX_CPU_1, 25);
                this.POWER_CHECK_MAX_CPU_2 = this.mParser.getInt(KEY_POWER_CHECK_MAX_CPU_2, 25);
                this.POWER_CHECK_MAX_CPU_3 = this.mParser.getInt(KEY_POWER_CHECK_MAX_CPU_3, 10);
                this.POWER_CHECK_MAX_CPU_4 = this.mParser.getInt(KEY_POWER_CHECK_MAX_CPU_4, 2);
                this.SERVICE_USAGE_INTERACTION_TIME_PRE_S = this.mParser.getLong(KEY_SERVICE_USAGE_INTERACTION_TIME_PRE_S, 1800000L);
                this.SERVICE_USAGE_INTERACTION_TIME_POST_S = this.mParser.getLong(KEY_SERVICE_USAGE_INTERACTION_TIME_POST_S, 60000L);
                this.USAGE_STATS_INTERACTION_INTERVAL_PRE_S = this.mParser.getLong(KEY_USAGE_STATS_INTERACTION_INTERVAL_PRE_S, 7200000L);
                this.USAGE_STATS_INTERACTION_INTERVAL_POST_S = this.mParser.getLong(KEY_USAGE_STATS_INTERACTION_INTERVAL_POST_S, 600000L);
                this.SERVICE_RESTART_DURATION = this.mParser.getLong(KEY_SERVICE_RESTART_DURATION, 1000L);
                this.SERVICE_RESET_RUN_DURATION = this.mParser.getLong(KEY_SERVICE_RESET_RUN_DURATION, 60000L);
                this.SERVICE_RESTART_DURATION_FACTOR = this.mParser.getInt(KEY_SERVICE_RESTART_DURATION_FACTOR, 4);
                this.SERVICE_MIN_RESTART_TIME_BETWEEN = this.mParser.getLong(KEY_SERVICE_MIN_RESTART_TIME_BETWEEN, (long) JobStatus.DEFAULT_TRIGGER_UPDATE_DELAY);
                this.MAX_SERVICE_INACTIVITY = this.mParser.getLong(KEY_MAX_SERVICE_INACTIVITY, 1800000L);
                this.BG_START_TIMEOUT = this.mParser.getLong(KEY_BG_START_TIMEOUT, 15000L);
                this.SERVICE_BG_ACTIVITY_START_TIMEOUT = this.mParser.getLong(KEY_SERVICE_BG_ACTIVITY_START_TIMEOUT, (long) JobStatus.DEFAULT_TRIGGER_UPDATE_DELAY);
                this.BOUND_SERVICE_CRASH_RESTART_DURATION = this.mParser.getLong(KEY_BOUND_SERVICE_CRASH_RESTART_DURATION, 1800000L);
                this.BOUND_SERVICE_MAX_CRASH_RETRY = this.mParser.getInt(KEY_BOUND_SERVICE_CRASH_MAX_RETRY, 16);
                this.FLAG_PROCESS_START_ASYNC = this.mParser.getBoolean(KEY_PROCESS_START_ASYNC, true);
                this.MEMORY_INFO_THROTTLE_TIME = this.mParser.getLong(KEY_MEMORY_INFO_THROTTLE_TIME, (long) BackupAgentTimeoutParameters.DEFAULT_FULL_BACKUP_AGENT_TIMEOUT_MILLIS);
                this.TOP_TO_FGS_GRACE_DURATION = this.mParser.getDurationMillis(KEY_TOP_TO_FGS_GRACE_DURATION, 15000L);
                this.TOP_TO_ALMOST_PERCEPTIBLE_GRACE_DURATION = this.mParser.getDurationMillis(KEY_TOP_TO_ALMOST_PERCEPTIBLE_GRACE_DURATION, 15000L);
                MIN_CRASH_INTERVAL = this.mParser.getInt(KEY_MIN_CRASH_INTERVAL, (int) DEFAULT_MIN_CRASH_INTERVAL);
                this.PENDINGINTENT_WARNING_THRESHOLD = this.mParser.getInt(KEY_PENDINGINTENT_WARNING_THRESHOLD, 2000);
                PROCESS_CRASH_COUNT_RESET_INTERVAL = this.mParser.getInt(KEY_PROCESS_CRASH_COUNT_RESET_INTERVAL, (int) DEFAULT_PROCESS_CRASH_COUNT_RESET_INTERVAL);
                PROCESS_CRASH_COUNT_LIMIT = this.mParser.getInt(KEY_PROCESS_CRASH_COUNT_LIMIT, 12);
                if (this.POWER_CHECK_INTERVAL != currentPowerCheckInterval) {
                    this.mService.mHandler.removeMessages(27);
                    Message msg = this.mService.mHandler.obtainMessage(27);
                    this.mService.mHandler.sendMessageDelayed(msg, this.POWER_CHECK_INTERVAL);
                }
            } catch (Throwable th) {
                ActivityManagerService.resetPriorityAfterLockedSection();
                throw th;
            }
        }
        ActivityManagerService.resetPriorityAfterLockedSection();
    }

    private void updateActivityStartsLoggingEnabled() {
        this.mFlagActivityStartsLoggingEnabled = Settings.Global.getInt(this.mResolver, "activity_starts_logging_enabled", 1) == 1;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void updateBackgroundActivityStarts() {
        this.mFlagBackgroundActivityStartsEnabled = DeviceConfig.getBoolean("activity_manager", KEY_DEFAULT_BACKGROUND_ACTIVITY_STARTS_ENABLED, false);
    }

    private void updateForegroundServiceStartsLoggingEnabled() {
        this.mFlagForegroundServiceStartsLoggingEnabled = Settings.Global.getInt(this.mResolver, "foreground_service_starts_logging_enabled", 1) == 1;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void updateBackgroundFgsStartsRestriction() {
        this.mFlagBackgroundFgsStartRestrictionEnabled = DeviceConfig.getBoolean("activity_manager", KEY_DEFAULT_BACKGROUND_FGS_STARTS_RESTRICTION_ENABLED, true);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void updateFgsStartsRestriction() {
        this.mFlagFgsStartRestrictionEnabled = DeviceConfig.getBoolean("activity_manager", KEY_DEFAULT_FGS_STARTS_RESTRICTION_ENABLED, true);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void updateFgsStartsRestrictionNotification() {
        this.mFgsStartRestrictionNotificationEnabled = DeviceConfig.getBoolean("activity_manager", KEY_DEFAULT_FGS_STARTS_RESTRICTION_NOTIFICATION_ENABLED, false);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void updateFgsStartsRestrictionCheckCallerTargetSdk() {
        this.mFgsStartRestrictionCheckCallerTargetSdk = DeviceConfig.getBoolean("activity_manager", KEY_DEFAULT_FGS_STARTS_RESTRICTION_CHECK_CALLER_TARGET_SDK, true);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void updateFgsNotificationDeferralEnable() {
        this.mFlagFgsNotificationDeferralEnabled = DeviceConfig.getBoolean("activity_manager", KEY_DEFERRED_FGS_NOTIFICATIONS_ENABLED, true);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void updateFgsNotificationDeferralApiGated() {
        this.mFlagFgsNotificationDeferralApiGated = DeviceConfig.getBoolean("activity_manager", KEY_DEFERRED_FGS_NOTIFICATIONS_API_GATED, false);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void updateFgsNotificationDeferralInterval() {
        this.mFgsNotificationDeferralInterval = DeviceConfig.getLong("activity_manager", KEY_DEFERRED_FGS_NOTIFICATION_INTERVAL, (long) JobStatus.DEFAULT_TRIGGER_UPDATE_DELAY);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void updateFgsNotificationDeferralExclusionTime() {
        this.mFgsNotificationDeferralExclusionTime = DeviceConfig.getLong("activity_manager", KEY_DEFERRED_FGS_NOTIFICATION_EXCLUSION_TIME, 120000L);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void updatePushMessagingOverQuotaBehavior() {
        this.mPushMessagingOverQuotaBehavior = DeviceConfig.getInt("activity_manager", KEY_PUSH_MESSAGING_OVER_QUOTA_BEHAVIOR, 1);
        if (this.mPushMessagingOverQuotaBehavior < -1 || this.mPushMessagingOverQuotaBehavior > 1) {
            this.mPushMessagingOverQuotaBehavior = 1;
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void updateOomAdjUpdatePolicy() {
        this.OOMADJ_UPDATE_QUICK = DeviceConfig.getInt("activity_manager", KEY_OOMADJ_UPDATE_POLICY, 1) == 1;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void updateForceRestrictedBackgroundCheck() {
        this.FORCE_BACKGROUND_CHECK_ON_RESTRICTED_APPS = DeviceConfig.getBoolean("activity_manager", KEY_FORCE_BACKGROUND_CHECK_ON_RESTRICTED_APPS, true);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void updateBootTimeTempAllowListDuration() {
        this.mBootTimeTempAllowlistDuration = DeviceConfig.getLong("activity_manager", KEY_BOOT_TIME_TEMP_ALLOWLIST_DURATION, 20000L);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void updateFgToBgFgsGraceDuration() {
        this.mFgToBgFgsGraceDuration = DeviceConfig.getLong("activity_manager", KEY_FG_TO_BG_FGS_GRACE_DURATION, 5000L);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void updateFgsStartForegroundTimeout() {
        this.mFgsStartForegroundTimeoutMs = DeviceConfig.getLong("activity_manager", KEY_FGS_START_FOREGROUND_TIMEOUT, (long) JobStatus.DEFAULT_TRIGGER_UPDATE_DELAY);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void updateFgsAtomSamplePercent() {
        this.mFgsAtomSampleRate = DeviceConfig.getFloat("activity_manager", KEY_FGS_ATOM_SAMPLE_RATE, 1.0f);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void updateFgsStartAllowedLogSamplePercent() {
        this.mFgsStartAllowedLogSampleRate = DeviceConfig.getFloat("activity_manager", KEY_FGS_START_ALLOWED_LOG_SAMPLE_RATE, (float) DEFAULT_FGS_START_ALLOWED_LOG_SAMPLE_RATE);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void updateFgsStartDeniedLogSamplePercent() {
        this.mFgsStartDeniedLogSampleRate = DeviceConfig.getFloat("activity_manager", KEY_FGS_START_DENIED_LOG_SAMPLE_RATE, 1.0f);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void updateKillBgRestrictedCachedIdle() {
        this.mKillBgRestrictedAndCachedIdle = DeviceConfig.getBoolean("activity_manager", KEY_KILL_BG_RESTRICTED_CACHED_IDLE, true);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void updateKillBgRestrictedCachedIdleSettleTime() {
        long currentSettleTime = this.mKillBgRestrictedAndCachedIdleSettleTimeMs;
        this.mKillBgRestrictedAndCachedIdleSettleTimeMs = DeviceConfig.getLong("activity_manager", KEY_KILL_BG_RESTRICTED_CACHED_IDLE_SETTLE_TIME, 60000L);
        if (this.mKillBgRestrictedAndCachedIdleSettleTimeMs != currentSettleTime) {
            this.mService.mHandler.removeMessages(58);
            this.mService.mHandler.sendEmptyMessageDelayed(58, this.mKillBgRestrictedAndCachedIdleSettleTimeMs);
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void updateFgsAllowOptOut() {
        this.mFgsAllowOptOut = DeviceConfig.getBoolean("activity_manager", KEY_FGS_ALLOW_OPT_OUT, false);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void updateExtraServiceRestartDelayOnMemPressure() {
        synchronized (this.mService) {
            try {
                ActivityManagerService.boostPriorityForLockedSection();
                int memFactor = this.mService.mAppProfiler.getLastMemoryLevelLocked();
                long[] prevDelays = this.mExtraServiceRestartDelayOnMemPressure;
                this.mExtraServiceRestartDelayOnMemPressure = parseLongArray(KEY_EXTRA_SERVICE_RESTART_DELAY_ON_MEM_PRESSURE, DEFAULT_EXTRA_SERVICE_RESTART_DELAY_ON_MEM_PRESSURE);
                this.mService.mServices.performRescheduleServiceRestartOnMemoryPressureLocked(this.mExtraServiceRestartDelayOnMemPressure[memFactor], prevDelays[memFactor], "config", SystemClock.uptimeMillis());
            } catch (Throwable th) {
                ActivityManagerService.resetPriorityAfterLockedSection();
                throw th;
            }
        }
        ActivityManagerService.resetPriorityAfterLockedSection();
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void updateEnableExtraServiceRestartDelayOnMemPressure() {
        synchronized (this.mService) {
            try {
                ActivityManagerService.boostPriorityForLockedSection();
                boolean prevEnabled = this.mEnableExtraServiceRestartDelayOnMemPressure;
                this.mEnableExtraServiceRestartDelayOnMemPressure = DeviceConfig.getBoolean("activity_manager", KEY_ENABLE_EXTRA_SERVICE_RESTART_DELAY_ON_MEM_PRESSURE, true);
                this.mService.mServices.rescheduleServiceRestartOnMemoryPressureIfNeededLocked(prevEnabled, this.mEnableExtraServiceRestartDelayOnMemPressure, SystemClock.uptimeMillis());
            } catch (Throwable th) {
                ActivityManagerService.resetPriorityAfterLockedSection();
                throw th;
            }
        }
        ActivityManagerService.resetPriorityAfterLockedSection();
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void updateDeferBootCompletedBroadcast() {
        this.mDeferBootCompletedBroadcast = DeviceConfig.getInt("activity_manager", KEY_DEFER_BOOT_COMPLETED_BROADCAST, 6);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void updateNoKillCachedProcessesUntilBootCompleted() {
        this.mNoKillCachedProcessesUntilBootCompleted = DeviceConfig.getBoolean("activity_manager", KEY_NO_KILL_CACHED_PROCESSES_UNTIL_BOOT_COMPLETED, DEFAULT_NO_KILL_CACHED_PROCESSES_UNTIL_BOOT_COMPLETED);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void updateNoKillCachedProcessesPostBootCompletedDurationMillis() {
        this.mNoKillCachedProcessesPostBootCompletedDurationMillis = DeviceConfig.getLong("activity_manager", KEY_NO_KILL_CACHED_PROCESSES_POST_BOOT_COMPLETED_DURATION_MILLIS, DEFAULT_NO_KILL_CACHED_PROCESSES_POST_BOOT_COMPLETED_DURATION_MILLIS);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void updateMaxEmptyTimeMillis() {
        this.mMaxEmptyTimeMillis = DeviceConfig.getLong("activity_manager", KEY_MAX_EMPTY_TIME_MILLIS, DEFAULT_MAX_EMPTY_TIME_MILLIS);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void updateNetworkAccessTimeoutMs() {
        this.mNetworkAccessTimeoutMs = DeviceConfig.getLong("activity_manager", KEY_NETWORK_ACCESS_TIMEOUT_MS, (long) DEFAULT_NETWORK_ACCESS_TIMEOUT_MS);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void updateServiceStartForegroundTimeoutMs() {
        this.mServiceStartForegroundTimeoutMs = DeviceConfig.getInt("activity_manager", KEY_SERVICE_START_FOREGROUND_TIMEOUT_MS, 30000);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void updateServiceStartForegroundAnrDealyMs() {
        this.mServiceStartForegroundAnrDelayMs = DeviceConfig.getInt("activity_manager", KEY_SERVICE_START_FOREGROUND_ANR_DELAY_MS, 10000);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void updateServiceBindAlmostPerceptibleTimeoutMs() {
        this.mServiceBindAlmostPerceptibleTimeoutMs = DeviceConfig.getLong("activity_manager", KEY_SERVICE_BIND_ALMOST_PERCEPTIBLE_TIMEOUT_MS, 15000L);
    }

    private long[] parseLongArray(String key, long[] def) {
        String val = DeviceConfig.getString("activity_manager", key, (String) null);
        if (!TextUtils.isEmpty(val)) {
            String[] ss = val.split(",");
            if (ss.length == def.length) {
                long[] tmp = new long[ss.length];
                for (int i = 0; i < ss.length; i++) {
                    try {
                        tmp[i] = Long.parseLong(ss[i]);
                    } catch (NumberFormatException e) {
                    }
                }
                return tmp;
            }
        }
        return def;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void updateComponentAliases() {
        this.mEnableComponentAlias = DeviceConfig.getBoolean("activity_manager_ca", KEY_ENABLE_COMPONENT_ALIAS, false);
        this.mComponentAliasOverrides = DeviceConfig.getString("activity_manager_ca", KEY_COMPONENT_ALIAS_OVERRIDES, "");
        this.mService.mComponentAliasResolver.update(this.mEnableComponentAlias, this.mComponentAliasOverrides);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void updateProcessKillTimeout() {
        this.mProcessKillTimeoutMs = DeviceConfig.getLong("activity_manager", KEY_PROCESS_KILL_TIMEOUT, (long) JobStatus.DEFAULT_TRIGGER_UPDATE_DELAY);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void updateImperceptibleKillExemptions() {
        this.IMPERCEPTIBLE_KILL_EXEMPT_PACKAGES.clear();
        this.IMPERCEPTIBLE_KILL_EXEMPT_PACKAGES.addAll(this.mDefaultImperceptibleKillExemptPackages);
        String val = DeviceConfig.getString("activity_manager", KEY_IMPERCEPTIBLE_KILL_EXEMPT_PACKAGES, (String) null);
        if (!TextUtils.isEmpty(val)) {
            this.IMPERCEPTIBLE_KILL_EXEMPT_PACKAGES.addAll(Arrays.asList(val.split(",")));
        }
        this.IMPERCEPTIBLE_KILL_EXEMPT_PROC_STATES.clear();
        this.IMPERCEPTIBLE_KILL_EXEMPT_PROC_STATES.addAll(this.mDefaultImperceptibleKillExemptProcStates);
        String val2 = DeviceConfig.getString("activity_manager", KEY_IMPERCEPTIBLE_KILL_EXEMPT_PROC_STATES, (String) null);
        if (!TextUtils.isEmpty(val2)) {
            Arrays.asList(val2.split(",")).stream().forEach(new Consumer() { // from class: com.android.server.am.ActivityManagerConstants$$ExternalSyntheticLambda0
                @Override // java.util.function.Consumer
                public final void accept(Object obj) {
                    ActivityManagerConstants.this.m1013xbb18da17((String) obj);
                }
            });
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$updateImperceptibleKillExemptions$0$com-android-server-am-ActivityManagerConstants  reason: not valid java name */
    public /* synthetic */ void m1013xbb18da17(String v) {
        try {
            this.IMPERCEPTIBLE_KILL_EXEMPT_PROC_STATES.add(Integer.valueOf(Integer.parseInt(v)));
        } catch (NumberFormatException e) {
        }
    }

    private void updateEnableAutomaticSystemServerHeapDumps() {
        if (!this.mSystemServerAutomaticHeapDumpEnabled) {
            Slog.wtf(TAG, "updateEnableAutomaticSystemServerHeapDumps called when leak detection disabled");
            return;
        }
        boolean enabled = Settings.Global.getInt(this.mResolver, "enable_automatic_system_server_heap_dumps", 1) == 1;
        long threshold = enabled ? this.mSystemServerAutomaticHeapDumpPssThresholdBytes : 0L;
        this.mService.setDumpHeapDebugLimit(null, 0, threshold, this.mSystemServerAutomaticHeapDumpPackageName);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void updateMaxCachedProcesses() {
        String maxCachedProcessesFlag = DeviceConfig.getProperty("activity_manager", KEY_MAX_CACHED_PROCESSES);
        try {
            Slog.d(TAG, "updateMaxCachedProcesses--before, updated CUR_MAX_CACHED_PROCESSES: " + this.CUR_MAX_CACHED_PROCESSES);
            if (ITranActivityManagerConstants.Instance().isMemFusionEnabled()) {
                this.CUR_MAX_CACHED_PROCESSES = ITranActivityManagerConstants.Instance().getMemFusionMaxCachedProcesses();
                Slog.d(TAG, "updateMaxCachedProcesses, updated CUR_MAX_CACHED_PROCESSES: " + this.CUR_MAX_CACHED_PROCESSES);
            } else {
                int i = this.mOverrideMaxCachedProcesses;
                if (i < 0) {
                    i = TextUtils.isEmpty(maxCachedProcessesFlag) ? 32 : Integer.parseInt(maxCachedProcessesFlag);
                }
                this.CUR_MAX_CACHED_PROCESSES = i;
            }
            Slog.d(TAG, "updateMaxCachedProcesses--After, updated CUR_MAX_CACHED_PROCESSES: " + this.CUR_MAX_CACHED_PROCESSES);
        } catch (NumberFormatException e) {
            Slog.e(TAG, "Unable to parse flag for max_cached_processes: " + maxCachedProcessesFlag, e);
            this.CUR_MAX_CACHED_PROCESSES = this.mCustomizedMaxCachedProcesses;
        }
        if (ITranActivityManagerConstants.Instance().isAgaresEnable()) {
            this.CUR_MAX_CACHED_PROCESSES = ITranActivityManagerConstants.Instance().getAgaresMaxCachedProcesses(this.CUR_MAX_CACHED_PROCESSES);
        }
        if (ITranActivityManagerConstants.Instance().isGriffinEnable()) {
            this.MAX_CACHED_PROCESSES = ITranActivityManagerConstants.Instance().updatePmMaxCachedNumber(this.MAX_CACHED_PROCESSES);
            this.CUR_MAX_CACHED_PROCESSES = ITranActivityManagerConstants.Instance().updatePmCurMaxCachedNumber(this.CUR_MAX_CACHED_PROCESSES);
        }
        if (SystemProperties.get("ro.limit_cache.support").equals("1")) {
            this.CUR_MAX_CACHED_PROCESSES = SystemProperties.getInt("ro.cur_max_process.val", this.CUR_MAX_CACHED_PROCESSES);
        }
        this.CUR_MAX_EMPTY_PROCESSES = computeEmptyProcessLimit(this.CUR_MAX_CACHED_PROCESSES);
        int rawMaxEmptyProcesses = computeEmptyProcessLimit(this.MAX_CACHED_PROCESSES);
        this.CUR_TRIM_EMPTY_PROCESSES = rawMaxEmptyProcesses / 2;
        this.CUR_TRIM_CACHED_PROCESSES = (this.MAX_CACHED_PROCESSES - rawMaxEmptyProcesses) / 3;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void updateMinAssocLogDuration() {
        MIN_ASSOC_LOG_DURATION = DeviceConfig.getLong("activity_manager", KEY_MIN_ASSOC_LOG_DURATION, (long) BackupAgentTimeoutParameters.DEFAULT_FULL_BACKUP_AGENT_TIMEOUT_MILLIS);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void updateBinderHeavyHitterWatcher() {
        BINDER_HEAVY_HITTER_WATCHER_ENABLED = DeviceConfig.getBoolean("activity_manager", KEY_BINDER_HEAVY_HITTER_WATCHER_ENABLED, this.mDefaultBinderHeavyHitterWatcherEnabled);
        BINDER_HEAVY_HITTER_WATCHER_BATCHSIZE = DeviceConfig.getInt("activity_manager", KEY_BINDER_HEAVY_HITTER_WATCHER_BATCHSIZE, this.mDefaultBinderHeavyHitterWatcherBatchSize);
        BINDER_HEAVY_HITTER_WATCHER_THRESHOLD = DeviceConfig.getFloat("activity_manager", KEY_BINDER_HEAVY_HITTER_WATCHER_THRESHOLD, this.mDefaultBinderHeavyHitterWatcherThreshold);
        BINDER_HEAVY_HITTER_AUTO_SAMPLER_ENABLED = DeviceConfig.getBoolean("activity_manager", KEY_BINDER_HEAVY_HITTER_AUTO_SAMPLER_ENABLED, this.mDefaultBinderHeavyHitterAutoSamplerEnabled);
        BINDER_HEAVY_HITTER_AUTO_SAMPLER_BATCHSIZE = DeviceConfig.getInt("activity_manager", KEY_BINDER_HEAVY_HITTER_AUTO_SAMPLER_BATCHSIZE, this.mDefaultBinderHeavyHitterAutoSamplerBatchSize);
        BINDER_HEAVY_HITTER_WATCHER_THRESHOLD = DeviceConfig.getFloat("activity_manager", KEY_BINDER_HEAVY_HITTER_AUTO_SAMPLER_THRESHOLD, this.mDefaultBinderHeavyHitterAutoSamplerThreshold);
        this.mService.scheduleUpdateBinderHeavyHitterWatcherConfig();
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void updateMaxPhantomProcesses() {
        int oldVal = this.MAX_PHANTOM_PROCESSES;
        int i = DeviceConfig.getInt("activity_manager", KEY_MAX_PHANTOM_PROCESSES, 32);
        this.MAX_PHANTOM_PROCESSES = i;
        if (oldVal > i) {
            ActivityManagerService.MainHandler mainHandler = this.mService.mHandler;
            PhantomProcessList phantomProcessList = this.mService.mPhantomProcessList;
            Objects.requireNonNull(phantomProcessList);
            mainHandler.post(new ActivityManagerConstants$$ExternalSyntheticLambda2(phantomProcessList));
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    @NeverCompile
    public void dump(PrintWriter pw) {
        pw.println("ACTIVITY MANAGER SETTINGS (dumpsys activity settings) activity_manager_constants:");
        pw.print("  ");
        pw.print(KEY_MAX_CACHED_PROCESSES);
        pw.print("=");
        pw.println(this.MAX_CACHED_PROCESSES);
        pw.print("  ");
        pw.print(KEY_BACKGROUND_SETTLE_TIME);
        pw.print("=");
        pw.println(this.BACKGROUND_SETTLE_TIME);
        pw.print("  ");
        pw.print(KEY_FGSERVICE_MIN_SHOWN_TIME);
        pw.print("=");
        pw.println(this.FGSERVICE_MIN_SHOWN_TIME);
        pw.print("  ");
        pw.print(KEY_FGSERVICE_MIN_REPORT_TIME);
        pw.print("=");
        pw.println(this.FGSERVICE_MIN_REPORT_TIME);
        pw.print("  ");
        pw.print(KEY_FGSERVICE_SCREEN_ON_BEFORE_TIME);
        pw.print("=");
        pw.println(this.FGSERVICE_SCREEN_ON_BEFORE_TIME);
        pw.print("  ");
        pw.print(KEY_FGSERVICE_SCREEN_ON_AFTER_TIME);
        pw.print("=");
        pw.println(this.FGSERVICE_SCREEN_ON_AFTER_TIME);
        pw.print("  ");
        pw.print(KEY_CONTENT_PROVIDER_RETAIN_TIME);
        pw.print("=");
        pw.println(this.CONTENT_PROVIDER_RETAIN_TIME);
        pw.print("  ");
        pw.print(KEY_GC_TIMEOUT);
        pw.print("=");
        pw.println(this.GC_TIMEOUT);
        pw.print("  ");
        pw.print(KEY_GC_MIN_INTERVAL);
        pw.print("=");
        pw.println(this.GC_MIN_INTERVAL);
        pw.print("  ");
        pw.print(KEY_FORCE_BACKGROUND_CHECK_ON_RESTRICTED_APPS);
        pw.print("=");
        pw.println(this.FORCE_BACKGROUND_CHECK_ON_RESTRICTED_APPS);
        pw.print("  ");
        pw.print(KEY_FULL_PSS_MIN_INTERVAL);
        pw.print("=");
        pw.println(this.FULL_PSS_MIN_INTERVAL);
        pw.print("  ");
        pw.print(KEY_FULL_PSS_LOWERED_INTERVAL);
        pw.print("=");
        pw.println(this.FULL_PSS_LOWERED_INTERVAL);
        pw.print("  ");
        pw.print(KEY_POWER_CHECK_INTERVAL);
        pw.print("=");
        pw.println(this.POWER_CHECK_INTERVAL);
        pw.print("  ");
        pw.print(KEY_POWER_CHECK_MAX_CPU_1);
        pw.print("=");
        pw.println(this.POWER_CHECK_MAX_CPU_1);
        pw.print("  ");
        pw.print(KEY_POWER_CHECK_MAX_CPU_2);
        pw.print("=");
        pw.println(this.POWER_CHECK_MAX_CPU_2);
        pw.print("  ");
        pw.print(KEY_POWER_CHECK_MAX_CPU_3);
        pw.print("=");
        pw.println(this.POWER_CHECK_MAX_CPU_3);
        pw.print("  ");
        pw.print(KEY_POWER_CHECK_MAX_CPU_4);
        pw.print("=");
        pw.println(this.POWER_CHECK_MAX_CPU_4);
        pw.print("  ");
        pw.print(KEY_SERVICE_USAGE_INTERACTION_TIME_PRE_S);
        pw.print("=");
        pw.println(this.SERVICE_USAGE_INTERACTION_TIME_PRE_S);
        pw.print("  ");
        pw.print(KEY_SERVICE_USAGE_INTERACTION_TIME_POST_S);
        pw.print("=");
        pw.println(this.SERVICE_USAGE_INTERACTION_TIME_POST_S);
        pw.print("  ");
        pw.print(KEY_USAGE_STATS_INTERACTION_INTERVAL_PRE_S);
        pw.print("=");
        pw.println(this.USAGE_STATS_INTERACTION_INTERVAL_PRE_S);
        pw.print("  ");
        pw.print(KEY_USAGE_STATS_INTERACTION_INTERVAL_POST_S);
        pw.print("=");
        pw.println(this.USAGE_STATS_INTERACTION_INTERVAL_POST_S);
        pw.print("  ");
        pw.print(KEY_SERVICE_RESTART_DURATION);
        pw.print("=");
        pw.println(this.SERVICE_RESTART_DURATION);
        pw.print("  ");
        pw.print(KEY_SERVICE_RESET_RUN_DURATION);
        pw.print("=");
        pw.println(this.SERVICE_RESET_RUN_DURATION);
        pw.print("  ");
        pw.print(KEY_SERVICE_RESTART_DURATION_FACTOR);
        pw.print("=");
        pw.println(this.SERVICE_RESTART_DURATION_FACTOR);
        pw.print("  ");
        pw.print(KEY_SERVICE_MIN_RESTART_TIME_BETWEEN);
        pw.print("=");
        pw.println(this.SERVICE_MIN_RESTART_TIME_BETWEEN);
        pw.print("  ");
        pw.print(KEY_MAX_SERVICE_INACTIVITY);
        pw.print("=");
        pw.println(this.MAX_SERVICE_INACTIVITY);
        pw.print("  ");
        pw.print(KEY_BG_START_TIMEOUT);
        pw.print("=");
        pw.println(this.BG_START_TIMEOUT);
        pw.print("  ");
        pw.print(KEY_SERVICE_BG_ACTIVITY_START_TIMEOUT);
        pw.print("=");
        pw.println(this.SERVICE_BG_ACTIVITY_START_TIMEOUT);
        pw.print("  ");
        pw.print(KEY_BOUND_SERVICE_CRASH_RESTART_DURATION);
        pw.print("=");
        pw.println(this.BOUND_SERVICE_CRASH_RESTART_DURATION);
        pw.print("  ");
        pw.print(KEY_BOUND_SERVICE_CRASH_MAX_RETRY);
        pw.print("=");
        pw.println(this.BOUND_SERVICE_MAX_CRASH_RETRY);
        pw.print("  ");
        pw.print(KEY_PROCESS_START_ASYNC);
        pw.print("=");
        pw.println(this.FLAG_PROCESS_START_ASYNC);
        pw.print("  ");
        pw.print(KEY_MEMORY_INFO_THROTTLE_TIME);
        pw.print("=");
        pw.println(this.MEMORY_INFO_THROTTLE_TIME);
        pw.print("  ");
        pw.print(KEY_TOP_TO_FGS_GRACE_DURATION);
        pw.print("=");
        pw.println(this.TOP_TO_FGS_GRACE_DURATION);
        pw.print("  ");
        pw.print(KEY_TOP_TO_ALMOST_PERCEPTIBLE_GRACE_DURATION);
        pw.print("=");
        pw.println(this.TOP_TO_ALMOST_PERCEPTIBLE_GRACE_DURATION);
        pw.print("  ");
        pw.print(KEY_MIN_CRASH_INTERVAL);
        pw.print("=");
        pw.println(MIN_CRASH_INTERVAL);
        pw.print("  ");
        pw.print(KEY_PROCESS_CRASH_COUNT_RESET_INTERVAL);
        pw.print("=");
        pw.println(PROCESS_CRASH_COUNT_RESET_INTERVAL);
        pw.print("  ");
        pw.print(KEY_PROCESS_CRASH_COUNT_LIMIT);
        pw.print("=");
        pw.println(PROCESS_CRASH_COUNT_LIMIT);
        pw.print("  ");
        pw.print(KEY_IMPERCEPTIBLE_KILL_EXEMPT_PROC_STATES);
        pw.print("=");
        pw.println(Arrays.toString(this.IMPERCEPTIBLE_KILL_EXEMPT_PROC_STATES.toArray()));
        pw.print("  ");
        pw.print(KEY_IMPERCEPTIBLE_KILL_EXEMPT_PACKAGES);
        pw.print("=");
        pw.println(Arrays.toString(this.IMPERCEPTIBLE_KILL_EXEMPT_PACKAGES.toArray()));
        pw.print("  ");
        pw.print(KEY_MIN_ASSOC_LOG_DURATION);
        pw.print("=");
        pw.println(MIN_ASSOC_LOG_DURATION);
        pw.print("  ");
        pw.print(KEY_BINDER_HEAVY_HITTER_WATCHER_ENABLED);
        pw.print("=");
        pw.println(BINDER_HEAVY_HITTER_WATCHER_ENABLED);
        pw.print("  ");
        pw.print(KEY_BINDER_HEAVY_HITTER_WATCHER_BATCHSIZE);
        pw.print("=");
        pw.println(BINDER_HEAVY_HITTER_WATCHER_BATCHSIZE);
        pw.print("  ");
        pw.print(KEY_BINDER_HEAVY_HITTER_WATCHER_THRESHOLD);
        pw.print("=");
        pw.println(BINDER_HEAVY_HITTER_WATCHER_THRESHOLD);
        pw.print("  ");
        pw.print(KEY_BINDER_HEAVY_HITTER_AUTO_SAMPLER_ENABLED);
        pw.print("=");
        pw.println(BINDER_HEAVY_HITTER_AUTO_SAMPLER_ENABLED);
        pw.print("  ");
        pw.print(KEY_BINDER_HEAVY_HITTER_AUTO_SAMPLER_BATCHSIZE);
        pw.print("=");
        pw.println(BINDER_HEAVY_HITTER_AUTO_SAMPLER_BATCHSIZE);
        pw.print("  ");
        pw.print(KEY_BINDER_HEAVY_HITTER_AUTO_SAMPLER_THRESHOLD);
        pw.print("=");
        pw.println(BINDER_HEAVY_HITTER_AUTO_SAMPLER_THRESHOLD);
        pw.print("  ");
        pw.print(KEY_MAX_PHANTOM_PROCESSES);
        pw.print("=");
        pw.println(this.MAX_PHANTOM_PROCESSES);
        pw.print("  ");
        pw.print(KEY_BOOT_TIME_TEMP_ALLOWLIST_DURATION);
        pw.print("=");
        pw.println(this.mBootTimeTempAllowlistDuration);
        pw.print("  ");
        pw.print(KEY_FG_TO_BG_FGS_GRACE_DURATION);
        pw.print("=");
        pw.println(this.mFgToBgFgsGraceDuration);
        pw.print("  ");
        pw.print(KEY_FGS_START_FOREGROUND_TIMEOUT);
        pw.print("=");
        pw.println(this.mFgsStartForegroundTimeoutMs);
        pw.print("  ");
        pw.print(KEY_DEFAULT_BACKGROUND_ACTIVITY_STARTS_ENABLED);
        pw.print("=");
        pw.println(this.mFlagBackgroundActivityStartsEnabled);
        pw.print("  ");
        pw.print(KEY_DEFAULT_BACKGROUND_FGS_STARTS_RESTRICTION_ENABLED);
        pw.print("=");
        pw.println(this.mFlagBackgroundFgsStartRestrictionEnabled);
        pw.print("  ");
        pw.print(KEY_DEFAULT_FGS_STARTS_RESTRICTION_ENABLED);
        pw.print("=");
        pw.println(this.mFlagFgsStartRestrictionEnabled);
        pw.print("  ");
        pw.print(KEY_DEFAULT_FGS_STARTS_RESTRICTION_NOTIFICATION_ENABLED);
        pw.print("=");
        pw.println(this.mFgsStartRestrictionNotificationEnabled);
        pw.print("  ");
        pw.print(KEY_DEFAULT_FGS_STARTS_RESTRICTION_CHECK_CALLER_TARGET_SDK);
        pw.print("=");
        pw.println(this.mFgsStartRestrictionCheckCallerTargetSdk);
        pw.print("  ");
        pw.print(KEY_FGS_ATOM_SAMPLE_RATE);
        pw.print("=");
        pw.println(this.mFgsAtomSampleRate);
        pw.print("  ");
        pw.print(KEY_FGS_START_ALLOWED_LOG_SAMPLE_RATE);
        pw.print("=");
        pw.println(this.mFgsStartAllowedLogSampleRate);
        pw.print("  ");
        pw.print(KEY_FGS_START_DENIED_LOG_SAMPLE_RATE);
        pw.print("=");
        pw.println(this.mFgsStartDeniedLogSampleRate);
        pw.print("  ");
        pw.print(KEY_PUSH_MESSAGING_OVER_QUOTA_BEHAVIOR);
        pw.print("=");
        pw.println(this.mPushMessagingOverQuotaBehavior);
        pw.print("  ");
        pw.print(KEY_FGS_ALLOW_OPT_OUT);
        pw.print("=");
        pw.println(this.mFgsAllowOptOut);
        pw.print("  ");
        pw.print(KEY_ENABLE_COMPONENT_ALIAS);
        pw.print("=");
        pw.println(this.mEnableComponentAlias);
        pw.print("  ");
        pw.print(KEY_COMPONENT_ALIAS_OVERRIDES);
        pw.print("=");
        pw.println(this.mComponentAliasOverrides);
        pw.print("  ");
        pw.print(KEY_DEFER_BOOT_COMPLETED_BROADCAST);
        pw.print("=");
        pw.println(this.mDeferBootCompletedBroadcast);
        pw.print("  ");
        pw.print(KEY_NO_KILL_CACHED_PROCESSES_UNTIL_BOOT_COMPLETED);
        pw.print("=");
        pw.println(this.mNoKillCachedProcessesUntilBootCompleted);
        pw.print("  ");
        pw.print(KEY_NO_KILL_CACHED_PROCESSES_POST_BOOT_COMPLETED_DURATION_MILLIS);
        pw.print("=");
        pw.println(this.mNoKillCachedProcessesPostBootCompletedDurationMillis);
        pw.print("  ");
        pw.print(KEY_MAX_EMPTY_TIME_MILLIS);
        pw.print("=");
        pw.println(this.mMaxEmptyTimeMillis);
        pw.print("  ");
        pw.print(KEY_SERVICE_START_FOREGROUND_TIMEOUT_MS);
        pw.print("=");
        pw.println(this.mServiceStartForegroundTimeoutMs);
        pw.print("  ");
        pw.print(KEY_SERVICE_START_FOREGROUND_ANR_DELAY_MS);
        pw.print("=");
        pw.println(this.mServiceStartForegroundAnrDelayMs);
        pw.print("  ");
        pw.print(KEY_SERVICE_BIND_ALMOST_PERCEPTIBLE_TIMEOUT_MS);
        pw.print("=");
        pw.println(this.mServiceBindAlmostPerceptibleTimeoutMs);
        pw.print("  ");
        pw.print(KEY_NETWORK_ACCESS_TIMEOUT_MS);
        pw.print("=");
        pw.println(this.mNetworkAccessTimeoutMs);
        pw.println();
        if (this.mOverrideMaxCachedProcesses >= 0) {
            pw.print("  mOverrideMaxCachedProcesses=");
            pw.println(this.mOverrideMaxCachedProcesses);
        }
        pw.print("  mCustomizedMaxCachedProcesses=");
        pw.println(this.mCustomizedMaxCachedProcesses);
        pw.print("  CUR_MAX_CACHED_PROCESSES=");
        pw.println(this.CUR_MAX_CACHED_PROCESSES);
        pw.print("  CUR_MAX_EMPTY_PROCESSES=");
        pw.println(this.CUR_MAX_EMPTY_PROCESSES);
        pw.print("  CUR_TRIM_EMPTY_PROCESSES=");
        pw.println(this.CUR_TRIM_EMPTY_PROCESSES);
        pw.print("  CUR_TRIM_CACHED_PROCESSES=");
        pw.println(this.CUR_TRIM_CACHED_PROCESSES);
        pw.print("  OOMADJ_UPDATE_QUICK=");
        pw.println(this.OOMADJ_UPDATE_QUICK);
    }
}
