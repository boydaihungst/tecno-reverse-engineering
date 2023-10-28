package com.android.server.job;

import android.app.ActivityManager;
import android.app.ActivityManagerInternal;
import android.app.AppGlobals;
import android.app.IUidObserver;
import android.app.compat.CompatChanges;
import android.app.job.IJobScheduler;
import android.app.job.JobInfo;
import android.app.job.JobParameters;
import android.app.job.JobSnapshot;
import android.app.job.JobWorkItem;
import android.app.usage.UsageStatsManagerInternal;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ApplicationInfo;
import android.content.pm.IPackageManager;
import android.content.pm.PackageManager;
import android.content.pm.PackageManagerInternal;
import android.content.pm.ParceledListSlice;
import android.content.pm.ProviderInfo;
import android.content.pm.ServiceInfo;
import android.net.Uri;
import android.os.BatteryManagerInternal;
import android.os.BatteryStatsInternal;
import android.os.Binder;
import android.os.Handler;
import android.os.LimitExceededException;
import android.os.Looper;
import android.os.Message;
import android.os.ParcelFileDescriptor;
import android.os.Process;
import android.os.RemoteException;
import android.os.SystemClock;
import android.os.UserHandle;
import android.os.WorkSource;
import android.os.storage.StorageManagerInternal;
import android.provider.DeviceConfig;
import android.util.ArrayMap;
import android.util.ArraySet;
import android.util.IndentingPrintWriter;
import android.util.Log;
import android.util.Slog;
import android.util.SparseArray;
import android.util.SparseBooleanArray;
import android.util.SparseIntArray;
import android.util.SparseSetArray;
import android.util.TimeUtils;
import android.util.proto.ProtoOutputStream;
import com.android.internal.os.SomeArgs;
import com.android.internal.util.FrameworkStatsLog;
import com.android.internal.util.jobs.ArrayUtils;
import com.android.internal.util.jobs.DumpUtils;
import com.android.server.AppStateTracker;
import com.android.server.AppStateTrackerImpl;
import com.android.server.DeviceIdleInternal;
import com.android.server.JobSchedulerBackgroundThread;
import com.android.server.LocalServices;
import com.android.server.SystemService;
import com.android.server.backup.BackupAgentTimeoutParameters;
import com.android.server.job.JobSchedulerInternal;
import com.android.server.job.JobSchedulerService;
import com.android.server.job.controllers.BackgroundJobsController;
import com.android.server.job.controllers.BatteryController;
import com.android.server.job.controllers.ComponentController;
import com.android.server.job.controllers.ConnectivityController;
import com.android.server.job.controllers.ContentObserverController;
import com.android.server.job.controllers.DeviceIdleJobsController;
import com.android.server.job.controllers.IdleController;
import com.android.server.job.controllers.JobStatus;
import com.android.server.job.controllers.PrefetchController;
import com.android.server.job.controllers.QuotaController;
import com.android.server.job.controllers.RestrictingController;
import com.android.server.job.controllers.StateController;
import com.android.server.job.controllers.StorageController;
import com.android.server.job.controllers.TareController;
import com.android.server.job.controllers.TimeController;
import com.android.server.job.restrictions.JobRestriction;
import com.android.server.job.restrictions.ThermalStatusRestriction;
import com.android.server.pm.PackageManagerService;
import com.android.server.pm.UserManagerInternal;
import com.android.server.pm.verify.domain.DomainVerificationPersistence;
import com.android.server.slice.SliceClientPermissions;
import com.android.server.storage.DeviceStorageMonitorService;
import com.android.server.tare.EconomyManagerInternal;
import com.android.server.usage.AppStandbyInternal;
import com.android.server.utils.quota.Categorizer;
import com.android.server.utils.quota.Category;
import com.android.server.utils.quota.CountQuotaTracker;
import dalvik.annotation.optimization.NeverCompile;
import java.io.FileDescriptor;
import java.io.PrintWriter;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Predicate;
import libcore.util.EmptyArray;
/* loaded from: classes.dex */
public class JobSchedulerService extends SystemService implements StateChangedListener, JobCompletedListener {
    public static final int ACTIVE_INDEX = 0;
    public static final boolean DEBUG;
    public static final boolean DEBUG_STANDBY;
    public static final int EXEMPTED_INDEX = 6;
    public static final int FREQUENT_INDEX = 2;
    public static final long MAX_ALLOWED_PERIOD_MS = 31536000000L;
    private static final int MAX_JOBS_PER_APP = 150;
    static final int MSG_CHECK_CHANGED_JOB_LIST = 8;
    static final int MSG_CHECK_INDIVIDUAL_JOB = 0;
    static final int MSG_CHECK_JOB = 1;
    static final int MSG_CHECK_JOB_GREEDY = 3;
    static final int MSG_CHECK_MEDIA_EXEMPTION = 9;
    static final int MSG_STOP_JOB = 2;
    static final int MSG_UID_ACTIVE = 6;
    static final int MSG_UID_GONE = 5;
    static final int MSG_UID_IDLE = 7;
    static final int MSG_UID_STATE_CHANGED = 4;
    public static final int NEVER_INDEX = 4;
    private static final int NUM_COMPLETED_JOB_HISTORY = 20;
    private static final long PERIODIC_JOB_WINDOW_BUFFER = 1800000;
    private static final Categorizer QUOTA_CATEGORIZER;
    private static final Category QUOTA_TRACKER_CATEGORY_SCHEDULE_LOGGED;
    private static final Category QUOTA_TRACKER_CATEGORY_SCHEDULE_PERSISTED;
    private static final String QUOTA_TRACKER_SCHEDULE_LOGGED = ".schedulePersisted out-of-quota logged";
    private static final String QUOTA_TRACKER_SCHEDULE_PERSISTED_TAG = ".schedulePersisted()";
    public static final int RARE_INDEX = 3;
    public static final int RESTRICTED_INDEX = 5;
    public static final String TAG = "JobScheduler";
    public static final int WORKING_INDEX = 1;
    public static Clock sElapsedRealtimeClock;
    public static Clock sSystemClock;
    public static Clock sUptimeMillisClock;
    ActivityManagerInternal mActivityManagerInternal;
    private final AppStandbyInternal mAppStandbyInternal;
    AppStateTrackerImpl mAppStateTracker;
    private final SparseBooleanArray mBackingUpUids;
    private final BatteryStateTracker mBatteryStateTracker;
    private final BroadcastReceiver mBroadcastReceiver;
    private final ArraySet<JobStatus> mChangedJobList;
    private final SparseArray<String> mCloudMediaProviderPackages;
    final JobConcurrencyManager mConcurrencyManager;
    final Constants mConstants;
    final ConstantsObserver mConstantsObserver;
    final List<StateController> mControllers;
    final ArrayMap<String, Boolean> mDebuggableApps;
    private final DeviceIdleJobsController mDeviceIdleJobsController;
    final JobHandler mHandler;
    private final Predicate<Integer> mIsUidActivePredicate;
    final JobPackageTracker mJobPackageTracker;
    private final List<JobRestriction> mJobRestrictions;
    final JobSchedulerStub mJobSchedulerStub;
    private final Runnable mJobTimeUpdater;
    final JobStore mJobs;
    private int mLastCompletedJobIndex;
    private final long[] mLastCompletedJobTimeElapsed;
    private final JobStatus[] mLastCompletedJobs;
    DeviceIdleInternal mLocalDeviceIdleController;
    PackageManagerInternal mLocalPM;
    final Object mLock;
    private final MaybeReadyJobQueueFunctor mMaybeQueueFunctor;
    private final PendingJobQueue mPendingJobQueue;
    private final PrefetchController mPrefetchController;
    private final QuotaController mQuotaController;
    private final CountQuotaTracker mQuotaTracker;
    private final ReadyJobQueueFunctor mReadyQueueFunctor;
    boolean mReadyToRock;
    boolean mReportedActive;
    private final List<RestrictingController> mRestrictiveControllers;
    final StandbyTracker mStandbyTracker;
    int[] mStartedUsers;
    private final StorageController mStorageController;
    private final TareController mTareController;
    private final BroadcastReceiver mTimeSetReceiver;
    final SparseIntArray mUidBiasOverride;
    private final IUidObserver mUidObserver;
    private final SparseSetArray<String> mUidToPackageCache;
    final UsageStatsManagerInternal mUsageStats;

    static {
        boolean isLoggable = Log.isLoggable(TAG, 3);
        DEBUG = isLoggable;
        DEBUG_STANDBY = isLoggable;
        sSystemClock = Clock.systemUTC();
        sUptimeMillisClock = new MySimpleClock(ZoneOffset.UTC) { // from class: com.android.server.job.JobSchedulerService.1
            @Override // com.android.server.job.JobSchedulerService.MySimpleClock, java.time.Clock
            public long millis() {
                return SystemClock.uptimeMillis();
            }
        };
        sElapsedRealtimeClock = new MySimpleClock(ZoneOffset.UTC) { // from class: com.android.server.job.JobSchedulerService.2
            @Override // com.android.server.job.JobSchedulerService.MySimpleClock, java.time.Clock
            public long millis() {
                return SystemClock.elapsedRealtime();
            }
        };
        QUOTA_TRACKER_CATEGORY_SCHEDULE_PERSISTED = new Category(QUOTA_TRACKER_SCHEDULE_PERSISTED_TAG);
        QUOTA_TRACKER_CATEGORY_SCHEDULE_LOGGED = new Category(QUOTA_TRACKER_SCHEDULE_LOGGED);
        QUOTA_CATEGORIZER = new Categorizer() { // from class: com.android.server.job.JobSchedulerService$$ExternalSyntheticLambda7
            @Override // com.android.server.utils.quota.Categorizer
            public final Category getCategory(int i, String str, String str2) {
                return JobSchedulerService.lambda$static$0(i, str, str2);
            }
        };
    }

    /* loaded from: classes.dex */
    private static abstract class MySimpleClock extends Clock {
        private final ZoneId mZoneId;

        @Override // java.time.Clock
        public abstract long millis();

        MySimpleClock(ZoneId zoneId) {
            this.mZoneId = zoneId;
        }

        @Override // java.time.Clock
        public ZoneId getZone() {
            return this.mZoneId;
        }

        @Override // java.time.Clock
        public Clock withZone(ZoneId zone) {
            return new MySimpleClock(zone) { // from class: com.android.server.job.JobSchedulerService.MySimpleClock.1
                @Override // com.android.server.job.JobSchedulerService.MySimpleClock, java.time.Clock
                public long millis() {
                    return MySimpleClock.this.millis();
                }
            };
        }

        @Override // java.time.Clock
        public Instant instant() {
            return Instant.ofEpochMilli(millis());
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static /* synthetic */ Category lambda$static$0(int userId, String packageName, String tag) {
        if (QUOTA_TRACKER_SCHEDULE_PERSISTED_TAG.equals(tag)) {
            return QUOTA_TRACKER_CATEGORY_SCHEDULE_PERSISTED;
        }
        return QUOTA_TRACKER_CATEGORY_SCHEDULE_LOGGED;
    }

    /* loaded from: classes.dex */
    private class ConstantsObserver implements DeviceConfig.OnPropertiesChangedListener, EconomyManagerInternal.TareStateChangeListener {
        private ConstantsObserver() {
        }

        public void start() {
            DeviceConfig.addOnPropertiesChangedListener("jobscheduler", JobSchedulerBackgroundThread.getExecutor(), this);
            EconomyManagerInternal economyManagerInternal = (EconomyManagerInternal) LocalServices.getService(EconomyManagerInternal.class);
            economyManagerInternal.registerTareStateChangeListener(this);
            synchronized (JobSchedulerService.this.mLock) {
                JobSchedulerService.this.mConstants.updateTareSettingsLocked(economyManagerInternal.isEnabled());
            }
            onPropertiesChanged(DeviceConfig.getProperties("jobscheduler", new String[0]));
        }

        /* JADX WARN: Can't fix incorrect switch cases order, some code will duplicate */
        public void onPropertiesChanged(DeviceConfig.Properties properties) {
            boolean apiQuotaScheduleUpdated = false;
            boolean concurrencyUpdated = false;
            boolean runtimeUpdated = false;
            for (int controller = 0; controller < JobSchedulerService.this.mControllers.size(); controller++) {
                StateController sc = JobSchedulerService.this.mControllers.get(controller);
                sc.prepareForUpdatedConstantsLocked();
            }
            synchronized (JobSchedulerService.this.mLock) {
                for (String name : properties.getKeyset()) {
                    if (name != null) {
                        char c = 65535;
                        switch (name.hashCode()) {
                            case -1787939498:
                                if (name.equals("aq_schedule_count")) {
                                    c = 1;
                                    break;
                                }
                                break;
                            case -1644162308:
                                if (name.equals("enable_api_quotas")) {
                                    c = 0;
                                    break;
                                }
                                break;
                            case -1470844605:
                                if (name.equals("runtime_min_ej_guarantee_ms")) {
                                    c = 19;
                                    break;
                                }
                                break;
                            case -1313417082:
                                if (name.equals("conn_use_cell_signal_strength")) {
                                    c = 14;
                                    break;
                                }
                                break;
                            case -1272362358:
                                if (name.equals("prefetch_force_batch_relax_threshold_ms")) {
                                    c = 16;
                                    break;
                                }
                                break;
                            case -1265463825:
                                if (name.equals("runtime_min_high_priority_guarantee_ms")) {
                                    c = 20;
                                    break;
                                }
                                break;
                            case -1215861621:
                                if (name.equals("conn_update_all_jobs_min_interval_ms")) {
                                    c = 15;
                                    break;
                                }
                                break;
                            case -1062323940:
                                if (name.equals("aq_schedule_window_ms")) {
                                    c = 2;
                                    break;
                                }
                                break;
                            case -941023983:
                                if (name.equals("runtime_min_guarantee_ms")) {
                                    c = 18;
                                    break;
                                }
                                break;
                            case -722508861:
                                if (name.equals("moderate_use_factor")) {
                                    c = '\b';
                                    break;
                                }
                                break;
                            case -492250078:
                                if (name.equals("conn_low_signal_strength_relax_frac")) {
                                    c = '\r';
                                    break;
                                }
                                break;
                            case -109453036:
                                if (name.equals("aq_schedule_return_failure")) {
                                    c = 3;
                                    break;
                                }
                                break;
                            case -57293457:
                                if (name.equals("conn_congestion_delay_frac")) {
                                    c = 11;
                                    break;
                                }
                                break;
                            case -45782187:
                                if (name.equals("max_non_active_job_batch_delay_ms")) {
                                    c = 6;
                                    break;
                                }
                                break;
                            case 263198386:
                                if (name.equals("min_exp_backoff_time_ms")) {
                                    c = '\n';
                                    break;
                                }
                                break;
                            case 289418623:
                                if (name.equals("heavy_use_factor")) {
                                    c = 7;
                                    break;
                                }
                                break;
                            case 709194164:
                                if (name.equals("min_linear_backoff_time_ms")) {
                                    c = '\t';
                                    break;
                                }
                                break;
                            case 1004645316:
                                if (name.equals("min_ready_non_active_jobs_count")) {
                                    c = 5;
                                    break;
                                }
                                break;
                            case 1185743293:
                                if (name.equals("aq_schedule_throw_exception")) {
                                    c = 4;
                                    break;
                                }
                                break;
                            case 1470808280:
                                if (name.equals("runtime_free_quota_max_limit_ms")) {
                                    c = 17;
                                    break;
                                }
                                break;
                            case 1692637170:
                                if (name.equals("conn_prefetch_relax_frac")) {
                                    c = '\f';
                                    break;
                                }
                                break;
                        }
                        switch (c) {
                            case 0:
                            case 1:
                            case 2:
                            case 3:
                            case 4:
                                if (!apiQuotaScheduleUpdated) {
                                    JobSchedulerService.this.mConstants.updateApiQuotaConstantsLocked();
                                    JobSchedulerService.this.updateQuotaTracker();
                                    apiQuotaScheduleUpdated = true;
                                    break;
                                }
                                break;
                            case 5:
                            case 6:
                                JobSchedulerService.this.mConstants.updateBatchingConstantsLocked();
                                break;
                            case 7:
                            case '\b':
                                JobSchedulerService.this.mConstants.updateUseFactorConstantsLocked();
                                break;
                            case '\t':
                            case '\n':
                                JobSchedulerService.this.mConstants.updateBackoffConstantsLocked();
                                break;
                            case 11:
                            case '\f':
                            case '\r':
                            case 14:
                            case 15:
                                JobSchedulerService.this.mConstants.updateConnectivityConstantsLocked();
                                break;
                            case 16:
                                JobSchedulerService.this.mConstants.updatePrefetchConstantsLocked();
                                break;
                            case 17:
                            case 18:
                            case 19:
                            case 20:
                                if (!runtimeUpdated) {
                                    JobSchedulerService.this.mConstants.updateRuntimeConstantsLocked();
                                    runtimeUpdated = true;
                                    break;
                                }
                                break;
                            default:
                                if (name.startsWith("concurrency_") && !concurrencyUpdated) {
                                    JobSchedulerService.this.mConcurrencyManager.updateConfigLocked();
                                    concurrencyUpdated = true;
                                    break;
                                } else {
                                    for (int ctrlr = 0; ctrlr < JobSchedulerService.this.mControllers.size(); ctrlr++) {
                                        StateController sc2 = JobSchedulerService.this.mControllers.get(ctrlr);
                                        sc2.processConstantLocked(properties, name);
                                    }
                                    break;
                                }
                        }
                    }
                }
                for (int controller2 = 0; controller2 < JobSchedulerService.this.mControllers.size(); controller2++) {
                    StateController sc3 = JobSchedulerService.this.mControllers.get(controller2);
                    sc3.onConstantsUpdatedLocked();
                }
            }
        }

        @Override // com.android.server.tare.EconomyManagerInternal.TareStateChangeListener
        public void onTareEnabledStateChanged(boolean isTareEnabled) {
            if (JobSchedulerService.this.mConstants.updateTareSettingsLocked(isTareEnabled)) {
                for (int controller = 0; controller < JobSchedulerService.this.mControllers.size(); controller++) {
                    StateController sc = JobSchedulerService.this.mControllers.get(controller);
                    sc.onConstantsUpdatedLocked();
                }
                JobSchedulerService.this.onControllerStateChanged(null);
            }
        }
    }

    void updateQuotaTracker() {
        this.mQuotaTracker.setEnabled(this.mConstants.ENABLE_API_QUOTAS);
        this.mQuotaTracker.setCountLimit(QUOTA_TRACKER_CATEGORY_SCHEDULE_PERSISTED, this.mConstants.API_QUOTA_SCHEDULE_COUNT, this.mConstants.API_QUOTA_SCHEDULE_WINDOW_MS);
    }

    /* loaded from: classes.dex */
    public static class Constants {
        private static final int DEFAULT_API_QUOTA_SCHEDULE_COUNT = 250;
        private static final boolean DEFAULT_API_QUOTA_SCHEDULE_RETURN_FAILURE_RESULT = false;
        private static final boolean DEFAULT_API_QUOTA_SCHEDULE_THROW_EXCEPTION = true;
        private static final long DEFAULT_API_QUOTA_SCHEDULE_WINDOW_MS = 60000;
        private static final float DEFAULT_CONN_CONGESTION_DELAY_FRAC = 0.5f;
        private static final float DEFAULT_CONN_LOW_SIGNAL_STRENGTH_RELAX_FRAC = 0.5f;
        private static final float DEFAULT_CONN_PREFETCH_RELAX_FRAC = 0.5f;
        private static final long DEFAULT_CONN_UPDATE_ALL_JOBS_MIN_INTERVAL_MS = 60000;
        private static final boolean DEFAULT_CONN_USE_CELL_SIGNAL_STRENGTH = true;
        private static final boolean DEFAULT_ENABLE_API_QUOTAS = true;
        private static final float DEFAULT_HEAVY_USE_FACTOR = 0.9f;
        private static final long DEFAULT_MAX_NON_ACTIVE_JOB_BATCH_DELAY_MS = 1860000;
        private static final long DEFAULT_MIN_EXP_BACKOFF_TIME_MS = 10000;
        private static final long DEFAULT_MIN_LINEAR_BACKOFF_TIME_MS = 10000;
        private static final int DEFAULT_MIN_READY_NON_ACTIVE_JOBS_COUNT = 5;
        private static final float DEFAULT_MODERATE_USE_FACTOR = 0.5f;
        private static final long DEFAULT_PREFETCH_FORCE_BATCH_RELAX_THRESHOLD_MS = 3600000;
        public static final long DEFAULT_RUNTIME_FREE_QUOTA_MAX_LIMIT_MS = 1800000;
        public static final long DEFAULT_RUNTIME_MIN_EJ_GUARANTEE_MS = 180000;
        public static final long DEFAULT_RUNTIME_MIN_GUARANTEE_MS = 600000;
        static final long DEFAULT_RUNTIME_MIN_HIGH_PRIORITY_GUARANTEE_MS = 300000;
        private static final boolean DEFAULT_USE_TARE_POLICY = false;
        private static final String KEY_API_QUOTA_SCHEDULE_COUNT = "aq_schedule_count";
        private static final String KEY_API_QUOTA_SCHEDULE_RETURN_FAILURE_RESULT = "aq_schedule_return_failure";
        private static final String KEY_API_QUOTA_SCHEDULE_THROW_EXCEPTION = "aq_schedule_throw_exception";
        private static final String KEY_API_QUOTA_SCHEDULE_WINDOW_MS = "aq_schedule_window_ms";
        private static final String KEY_CONN_CONGESTION_DELAY_FRAC = "conn_congestion_delay_frac";
        private static final String KEY_CONN_LOW_SIGNAL_STRENGTH_RELAX_FRAC = "conn_low_signal_strength_relax_frac";
        private static final String KEY_CONN_PREFETCH_RELAX_FRAC = "conn_prefetch_relax_frac";
        private static final String KEY_CONN_UPDATE_ALL_JOBS_MIN_INTERVAL_MS = "conn_update_all_jobs_min_interval_ms";
        private static final String KEY_CONN_USE_CELL_SIGNAL_STRENGTH = "conn_use_cell_signal_strength";
        private static final String KEY_ENABLE_API_QUOTAS = "enable_api_quotas";
        private static final String KEY_HEAVY_USE_FACTOR = "heavy_use_factor";
        private static final String KEY_MAX_NON_ACTIVE_JOB_BATCH_DELAY_MS = "max_non_active_job_batch_delay_ms";
        private static final String KEY_MIN_EXP_BACKOFF_TIME_MS = "min_exp_backoff_time_ms";
        private static final String KEY_MIN_LINEAR_BACKOFF_TIME_MS = "min_linear_backoff_time_ms";
        private static final String KEY_MIN_READY_NON_ACTIVE_JOBS_COUNT = "min_ready_non_active_jobs_count";
        private static final String KEY_MODERATE_USE_FACTOR = "moderate_use_factor";
        private static final String KEY_PREFETCH_FORCE_BATCH_RELAX_THRESHOLD_MS = "prefetch_force_batch_relax_threshold_ms";
        private static final String KEY_RUNTIME_FREE_QUOTA_MAX_LIMIT_MS = "runtime_free_quota_max_limit_ms";
        private static final String KEY_RUNTIME_MIN_EJ_GUARANTEE_MS = "runtime_min_ej_guarantee_ms";
        private static final String KEY_RUNTIME_MIN_GUARANTEE_MS = "runtime_min_guarantee_ms";
        private static final String KEY_RUNTIME_MIN_HIGH_PRIORITY_GUARANTEE_MS = "runtime_min_high_priority_guarantee_ms";
        int MIN_READY_NON_ACTIVE_JOBS_COUNT = 5;
        long MAX_NON_ACTIVE_JOB_BATCH_DELAY_MS = DEFAULT_MAX_NON_ACTIVE_JOB_BATCH_DELAY_MS;
        float HEAVY_USE_FACTOR = DEFAULT_HEAVY_USE_FACTOR;
        float MODERATE_USE_FACTOR = 0.5f;
        long MIN_LINEAR_BACKOFF_TIME_MS = JobStatus.DEFAULT_TRIGGER_UPDATE_DELAY;
        long MIN_EXP_BACKOFF_TIME_MS = JobStatus.DEFAULT_TRIGGER_UPDATE_DELAY;
        public float CONN_CONGESTION_DELAY_FRAC = 0.5f;
        public float CONN_PREFETCH_RELAX_FRAC = 0.5f;
        public boolean CONN_USE_CELL_SIGNAL_STRENGTH = true;
        public long CONN_UPDATE_ALL_JOBS_MIN_INTERVAL_MS = 60000;
        public float CONN_LOW_SIGNAL_STRENGTH_RELAX_FRAC = 0.5f;
        public long PREFETCH_FORCE_BATCH_RELAX_THRESHOLD_MS = 3600000;
        public boolean ENABLE_API_QUOTAS = true;
        public int API_QUOTA_SCHEDULE_COUNT = 250;
        public long API_QUOTA_SCHEDULE_WINDOW_MS = 60000;
        public boolean API_QUOTA_SCHEDULE_THROW_EXCEPTION = true;
        public boolean API_QUOTA_SCHEDULE_RETURN_FAILURE_RESULT = false;
        public long RUNTIME_FREE_QUOTA_MAX_LIMIT_MS = 1800000;
        public long RUNTIME_MIN_GUARANTEE_MS = 600000;
        public long RUNTIME_MIN_EJ_GUARANTEE_MS = 180000;
        public long RUNTIME_MIN_HIGH_PRIORITY_GUARANTEE_MS = 300000;
        public boolean USE_TARE_POLICY = false;

        /* JADX INFO: Access modifiers changed from: private */
        public void updateBatchingConstantsLocked() {
            this.MIN_READY_NON_ACTIVE_JOBS_COUNT = DeviceConfig.getInt("jobscheduler", KEY_MIN_READY_NON_ACTIVE_JOBS_COUNT, 5);
            this.MAX_NON_ACTIVE_JOB_BATCH_DELAY_MS = DeviceConfig.getLong("jobscheduler", KEY_MAX_NON_ACTIVE_JOB_BATCH_DELAY_MS, (long) DEFAULT_MAX_NON_ACTIVE_JOB_BATCH_DELAY_MS);
        }

        /* JADX INFO: Access modifiers changed from: private */
        public void updateUseFactorConstantsLocked() {
            this.HEAVY_USE_FACTOR = DeviceConfig.getFloat("jobscheduler", KEY_HEAVY_USE_FACTOR, (float) DEFAULT_HEAVY_USE_FACTOR);
            this.MODERATE_USE_FACTOR = DeviceConfig.getFloat("jobscheduler", KEY_MODERATE_USE_FACTOR, 0.5f);
        }

        /* JADX INFO: Access modifiers changed from: private */
        public void updateBackoffConstantsLocked() {
            this.MIN_LINEAR_BACKOFF_TIME_MS = DeviceConfig.getLong("jobscheduler", KEY_MIN_LINEAR_BACKOFF_TIME_MS, (long) JobStatus.DEFAULT_TRIGGER_UPDATE_DELAY);
            this.MIN_EXP_BACKOFF_TIME_MS = DeviceConfig.getLong("jobscheduler", KEY_MIN_EXP_BACKOFF_TIME_MS, (long) JobStatus.DEFAULT_TRIGGER_UPDATE_DELAY);
        }

        /* JADX INFO: Access modifiers changed from: private */
        public void updateConnectivityConstantsLocked() {
            this.CONN_CONGESTION_DELAY_FRAC = DeviceConfig.getFloat("jobscheduler", KEY_CONN_CONGESTION_DELAY_FRAC, 0.5f);
            this.CONN_PREFETCH_RELAX_FRAC = DeviceConfig.getFloat("jobscheduler", KEY_CONN_PREFETCH_RELAX_FRAC, 0.5f);
            this.CONN_USE_CELL_SIGNAL_STRENGTH = DeviceConfig.getBoolean("jobscheduler", KEY_CONN_USE_CELL_SIGNAL_STRENGTH, true);
            this.CONN_UPDATE_ALL_JOBS_MIN_INTERVAL_MS = DeviceConfig.getLong("jobscheduler", KEY_CONN_UPDATE_ALL_JOBS_MIN_INTERVAL_MS, 60000L);
            this.CONN_LOW_SIGNAL_STRENGTH_RELAX_FRAC = DeviceConfig.getFloat("jobscheduler", KEY_CONN_LOW_SIGNAL_STRENGTH_RELAX_FRAC, 0.5f);
        }

        /* JADX INFO: Access modifiers changed from: private */
        public void updatePrefetchConstantsLocked() {
            this.PREFETCH_FORCE_BATCH_RELAX_THRESHOLD_MS = DeviceConfig.getLong("jobscheduler", KEY_PREFETCH_FORCE_BATCH_RELAX_THRESHOLD_MS, 3600000L);
        }

        /* JADX INFO: Access modifiers changed from: private */
        public void updateApiQuotaConstantsLocked() {
            this.ENABLE_API_QUOTAS = DeviceConfig.getBoolean("jobscheduler", KEY_ENABLE_API_QUOTAS, true);
            this.API_QUOTA_SCHEDULE_COUNT = Math.max(250, DeviceConfig.getInt("jobscheduler", KEY_API_QUOTA_SCHEDULE_COUNT, 250));
            this.API_QUOTA_SCHEDULE_WINDOW_MS = DeviceConfig.getLong("jobscheduler", KEY_API_QUOTA_SCHEDULE_WINDOW_MS, 60000L);
            this.API_QUOTA_SCHEDULE_THROW_EXCEPTION = DeviceConfig.getBoolean("jobscheduler", KEY_API_QUOTA_SCHEDULE_THROW_EXCEPTION, true);
            this.API_QUOTA_SCHEDULE_RETURN_FAILURE_RESULT = DeviceConfig.getBoolean("jobscheduler", KEY_API_QUOTA_SCHEDULE_RETURN_FAILURE_RESULT, false);
        }

        /* JADX INFO: Access modifiers changed from: private */
        public void updateRuntimeConstantsLocked() {
            DeviceConfig.Properties properties = DeviceConfig.getProperties("jobscheduler", new String[]{KEY_RUNTIME_FREE_QUOTA_MAX_LIMIT_MS, KEY_RUNTIME_MIN_GUARANTEE_MS, KEY_RUNTIME_MIN_EJ_GUARANTEE_MS, KEY_RUNTIME_MIN_HIGH_PRIORITY_GUARANTEE_MS});
            this.RUNTIME_MIN_GUARANTEE_MS = Math.max(600000L, properties.getLong(KEY_RUNTIME_MIN_GUARANTEE_MS, 600000L));
            this.RUNTIME_MIN_HIGH_PRIORITY_GUARANTEE_MS = Math.max(240000L, properties.getLong(KEY_RUNTIME_MIN_HIGH_PRIORITY_GUARANTEE_MS, 300000L));
            this.RUNTIME_MIN_EJ_GUARANTEE_MS = Math.max(60000L, properties.getLong(KEY_RUNTIME_MIN_EJ_GUARANTEE_MS, 180000L));
            this.RUNTIME_FREE_QUOTA_MAX_LIMIT_MS = Math.max(this.RUNTIME_MIN_GUARANTEE_MS, properties.getLong(KEY_RUNTIME_FREE_QUOTA_MAX_LIMIT_MS, 1800000L));
        }

        /* JADX INFO: Access modifiers changed from: private */
        public boolean updateTareSettingsLocked(boolean isTareEnabled) {
            if (this.USE_TARE_POLICY == isTareEnabled) {
                return false;
            }
            this.USE_TARE_POLICY = isTareEnabled;
            return true;
        }

        void dump(IndentingPrintWriter pw) {
            pw.println("Settings:");
            pw.increaseIndent();
            pw.print(KEY_MIN_READY_NON_ACTIVE_JOBS_COUNT, Integer.valueOf(this.MIN_READY_NON_ACTIVE_JOBS_COUNT)).println();
            pw.print(KEY_MAX_NON_ACTIVE_JOB_BATCH_DELAY_MS, Long.valueOf(this.MAX_NON_ACTIVE_JOB_BATCH_DELAY_MS)).println();
            pw.print(KEY_HEAVY_USE_FACTOR, Float.valueOf(this.HEAVY_USE_FACTOR)).println();
            pw.print(KEY_MODERATE_USE_FACTOR, Float.valueOf(this.MODERATE_USE_FACTOR)).println();
            pw.print(KEY_MIN_LINEAR_BACKOFF_TIME_MS, Long.valueOf(this.MIN_LINEAR_BACKOFF_TIME_MS)).println();
            pw.print(KEY_MIN_EXP_BACKOFF_TIME_MS, Long.valueOf(this.MIN_EXP_BACKOFF_TIME_MS)).println();
            pw.print(KEY_CONN_CONGESTION_DELAY_FRAC, Float.valueOf(this.CONN_CONGESTION_DELAY_FRAC)).println();
            pw.print(KEY_CONN_PREFETCH_RELAX_FRAC, Float.valueOf(this.CONN_PREFETCH_RELAX_FRAC)).println();
            pw.print(KEY_CONN_USE_CELL_SIGNAL_STRENGTH, Boolean.valueOf(this.CONN_USE_CELL_SIGNAL_STRENGTH)).println();
            pw.print(KEY_CONN_UPDATE_ALL_JOBS_MIN_INTERVAL_MS, Long.valueOf(this.CONN_UPDATE_ALL_JOBS_MIN_INTERVAL_MS)).println();
            pw.print(KEY_CONN_LOW_SIGNAL_STRENGTH_RELAX_FRAC, Float.valueOf(this.CONN_LOW_SIGNAL_STRENGTH_RELAX_FRAC)).println();
            pw.print(KEY_PREFETCH_FORCE_BATCH_RELAX_THRESHOLD_MS, Long.valueOf(this.PREFETCH_FORCE_BATCH_RELAX_THRESHOLD_MS)).println();
            pw.print(KEY_ENABLE_API_QUOTAS, Boolean.valueOf(this.ENABLE_API_QUOTAS)).println();
            pw.print(KEY_API_QUOTA_SCHEDULE_COUNT, Integer.valueOf(this.API_QUOTA_SCHEDULE_COUNT)).println();
            pw.print(KEY_API_QUOTA_SCHEDULE_WINDOW_MS, Long.valueOf(this.API_QUOTA_SCHEDULE_WINDOW_MS)).println();
            pw.print(KEY_API_QUOTA_SCHEDULE_THROW_EXCEPTION, Boolean.valueOf(this.API_QUOTA_SCHEDULE_THROW_EXCEPTION)).println();
            pw.print(KEY_API_QUOTA_SCHEDULE_RETURN_FAILURE_RESULT, Boolean.valueOf(this.API_QUOTA_SCHEDULE_RETURN_FAILURE_RESULT)).println();
            pw.print(KEY_RUNTIME_MIN_GUARANTEE_MS, Long.valueOf(this.RUNTIME_MIN_GUARANTEE_MS)).println();
            pw.print(KEY_RUNTIME_MIN_EJ_GUARANTEE_MS, Long.valueOf(this.RUNTIME_MIN_EJ_GUARANTEE_MS)).println();
            pw.print(KEY_RUNTIME_MIN_HIGH_PRIORITY_GUARANTEE_MS, Long.valueOf(this.RUNTIME_MIN_HIGH_PRIORITY_GUARANTEE_MS)).println();
            pw.print(KEY_RUNTIME_FREE_QUOTA_MAX_LIMIT_MS, Long.valueOf(this.RUNTIME_FREE_QUOTA_MAX_LIMIT_MS)).println();
            pw.print("enable_tare", Boolean.valueOf(this.USE_TARE_POLICY)).println();
            pw.decreaseIndent();
        }

        void dump(ProtoOutputStream proto) {
            proto.write(1120986464285L, this.MIN_READY_NON_ACTIVE_JOBS_COUNT);
            proto.write(1112396529694L, this.MAX_NON_ACTIVE_JOB_BATCH_DELAY_MS);
            proto.write(1103806595080L, this.HEAVY_USE_FACTOR);
            proto.write(1103806595081L, this.MODERATE_USE_FACTOR);
            proto.write(1112396529681L, this.MIN_LINEAR_BACKOFF_TIME_MS);
            proto.write(1112396529682L, this.MIN_EXP_BACKOFF_TIME_MS);
            proto.write(1103806595093L, this.CONN_CONGESTION_DELAY_FRAC);
            proto.write(1103806595094L, this.CONN_PREFETCH_RELAX_FRAC);
            proto.write(1133871366175L, this.ENABLE_API_QUOTAS);
            proto.write(1120986464288L, this.API_QUOTA_SCHEDULE_COUNT);
            proto.write(1112396529697L, this.API_QUOTA_SCHEDULE_WINDOW_MS);
            proto.write(1133871366178L, this.API_QUOTA_SCHEDULE_THROW_EXCEPTION);
            proto.write(1133871366179L, this.API_QUOTA_SCHEDULE_RETURN_FAILURE_RESULT);
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public String getPackageName(Intent intent) {
        Uri uri = intent.getData();
        if (uri != null) {
            String pkg = uri.getSchemeSpecificPart();
            return pkg;
        }
        return null;
    }

    public Context getTestableContext() {
        return getContext();
    }

    public Object getLock() {
        return this.mLock;
    }

    public JobStore getJobStore() {
        return this.mJobs;
    }

    public Constants getConstants() {
        return this.mConstants;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public PendingJobQueue getPendingJobQueue() {
        return this.mPendingJobQueue;
    }

    public WorkSource deriveWorkSource(int sourceUid, String sourcePackageName) {
        if (!WorkSource.isChainedBatteryAttributionEnabled(getContext())) {
            return sourcePackageName == null ? new WorkSource(sourceUid) : new WorkSource(sourceUid, sourcePackageName);
        }
        WorkSource ws = new WorkSource();
        ws.createWorkChain().addNode(sourceUid, sourcePackageName).addNode(1000, TAG);
        return ws;
    }

    public ArraySet<String> getPackagesForUidLocked(int uid) {
        ArraySet<String> packages = this.mUidToPackageCache.get(uid);
        if (packages == null) {
            try {
                String[] pkgs = AppGlobals.getPackageManager().getPackagesForUid(uid);
                if (pkgs != null) {
                    for (String pkg : pkgs) {
                        this.mUidToPackageCache.add(uid, pkg);
                    }
                    return this.mUidToPackageCache.get(uid);
                }
                return packages;
            } catch (RemoteException e) {
                return packages;
            }
        }
        return packages;
    }

    @Override // com.android.server.SystemService
    public void onUserStarting(SystemService.TargetUser user) {
        synchronized (this.mLock) {
            this.mStartedUsers = ArrayUtils.appendInt(this.mStartedUsers, user.getUserIdentifier());
        }
    }

    @Override // com.android.server.SystemService
    public void onUserCompletedEvent(SystemService.TargetUser user, SystemService.UserCompletedEventType eventType) {
        if (eventType.includesOnUserStarting() || eventType.includesOnUserUnlocked()) {
            this.mHandler.obtainMessage(1).sendToTarget();
        }
    }

    @Override // com.android.server.SystemService
    public void onUserStopping(SystemService.TargetUser user) {
        synchronized (this.mLock) {
            this.mStartedUsers = ArrayUtils.removeInt(this.mStartedUsers, user.getUserIdentifier());
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public boolean isUidActive(int uid) {
        return this.mAppStateTracker.isUidActiveSynced(uid);
    }

    public int scheduleAsPackage(JobInfo job, JobWorkItem work, int uId, String packageName, int userId, String tag) {
        JobStatus jobStatus;
        String servicePkg = job.getService().getPackageName();
        Object obj = null;
        if (job.isPersisted() && (packageName == null || packageName.equals(servicePkg))) {
            String pkg = packageName == null ? servicePkg : packageName;
            if (!this.mQuotaTracker.isWithinQuota(userId, pkg, QUOTA_TRACKER_SCHEDULE_PERSISTED_TAG)) {
                if (this.mQuotaTracker.isWithinQuota(userId, pkg, QUOTA_TRACKER_SCHEDULE_LOGGED)) {
                    Slog.wtf(TAG, userId + "-" + pkg + " has called schedule() too many times");
                    this.mQuotaTracker.noteEvent(userId, pkg, QUOTA_TRACKER_SCHEDULE_LOGGED);
                }
                this.mAppStandbyInternal.restrictApp(pkg, userId, 4);
                if (this.mConstants.API_QUOTA_SCHEDULE_THROW_EXCEPTION) {
                    synchronized (this.mLock) {
                        if (!this.mDebuggableApps.containsKey(packageName)) {
                            try {
                                ApplicationInfo appInfo = AppGlobals.getPackageManager().getApplicationInfo(pkg, 0L, userId);
                                if (appInfo == null) {
                                    return 0;
                                }
                                this.mDebuggableApps.put(packageName, Boolean.valueOf((appInfo.flags & 2) != 0));
                            } catch (RemoteException e) {
                                throw new RuntimeException(e);
                            }
                        }
                        boolean isDebuggable = this.mDebuggableApps.get(packageName).booleanValue();
                        if (isDebuggable) {
                            StringBuilder append = new StringBuilder().append("schedule()/enqueue() called more than ");
                            CountQuotaTracker countQuotaTracker = this.mQuotaTracker;
                            Category category = QUOTA_TRACKER_CATEGORY_SCHEDULE_PERSISTED;
                            throw new LimitExceededException(append.append(countQuotaTracker.getLimit(category)).append(" times in the past ").append(this.mQuotaTracker.getWindowSizeMs(category)).append("ms. See the documentation for more information.").toString());
                        }
                    }
                }
                if (this.mConstants.API_QUOTA_SCHEDULE_RETURN_FAILURE_RESULT) {
                    return 0;
                }
            }
            this.mQuotaTracker.noteEvent(userId, pkg, QUOTA_TRACKER_SCHEDULE_PERSISTED_TAG);
        }
        if (this.mActivityManagerInternal.isAppStartModeDisabled(uId, servicePkg)) {
            Slog.w(TAG, "Not scheduling job " + uId + ":" + job.toString() + " -- package not allowed to start");
            return 0;
        }
        Object obj2 = this.mLock;
        synchronized (obj2) {
            try {
                try {
                    JobStatus toCancel = this.mJobs.getJobByUidAndJobId(uId, job.getId());
                    if (work != null && toCancel != null) {
                        try {
                            if (toCancel.getJob().equals(job)) {
                                toCancel.enqueueWorkLocked(work);
                                toCancel.maybeAddForegroundExemption(this.mIsUidActivePredicate);
                                return 1;
                            }
                        } catch (Throwable th) {
                            th = th;
                            obj = obj2;
                            throw th;
                        }
                    }
                    JobStatus jobStatus2 = JobStatus.createFromJobInfo(job, uId, packageName, userId, tag);
                    if (!jobStatus2.isRequestedExpeditedJob() || ((!this.mConstants.USE_TARE_POLICY || this.mTareController.canScheduleEJ(jobStatus2)) && (this.mConstants.USE_TARE_POLICY || this.mQuotaController.isWithinEJQuotaLocked(jobStatus2)))) {
                        jobStatus2.maybeAddForegroundExemption(this.mIsUidActivePredicate);
                        if (DEBUG) {
                            Slog.d(TAG, "SCHEDULE: " + jobStatus2.toShortString());
                        }
                        if (packageName == null && this.mJobs.countJobsForUid(uId) > 150) {
                            Slog.w(TAG, "Too many jobs for uid " + uId);
                            throw new IllegalStateException("Apps may not schedule more than 150 distinct jobs");
                        }
                        jobStatus2.prepareLocked();
                        if (toCancel != null) {
                            jobStatus = jobStatus2;
                            cancelJobImplLocked(toCancel, jobStatus2, 1, 0, "job rescheduled by app");
                        } else {
                            jobStatus = jobStatus2;
                            startTrackingJobLocked(jobStatus, null);
                        }
                        if (work != null) {
                            jobStatus.enqueueWorkLocked(work);
                        }
                        JobStatus jobStatus3 = jobStatus;
                        FrameworkStatsLog.write_non_chained(8, uId, null, jobStatus.getBatteryName(), 2, -1, jobStatus.getStandbyBucket(), jobStatus.getJobId(), jobStatus.hasChargingConstraint(), jobStatus.hasBatteryNotLowConstraint(), jobStatus.hasStorageNotLowConstraint(), jobStatus.hasTimingDelayConstraint(), jobStatus.hasDeadlineConstraint(), jobStatus.hasIdleConstraint(), jobStatus.hasConnectivityConstraint(), jobStatus.hasContentTriggerConstraint(), jobStatus.isRequestedExpeditedJob(), false, 0, jobStatus.getJob().isPrefetch(), jobStatus.getJob().getPriority(), jobStatus.getEffectivePriority(), jobStatus.getNumFailures());
                        if (isReadyToBeExecutedLocked(jobStatus3)) {
                            this.mJobPackageTracker.notePending(jobStatus3);
                            this.mPendingJobQueue.add(jobStatus3);
                            maybeRunPendingJobsLocked();
                        } else {
                            evaluateControllerStatesLocked(jobStatus3);
                        }
                        return 1;
                    }
                    return 0;
                } catch (Throwable th2) {
                    th = th2;
                    obj = obj2;
                }
            } catch (Throwable th3) {
                th = th3;
            }
        }
    }

    public List<JobInfo> getPendingJobs(int uid) {
        ArrayList<JobInfo> outList;
        synchronized (this.mLock) {
            List<JobStatus> jobs = this.mJobs.getJobsByUid(uid);
            outList = new ArrayList<>(jobs.size());
            for (int i = jobs.size() - 1; i >= 0; i--) {
                JobStatus job = jobs.get(i);
                outList.add(job.getJob());
            }
        }
        return outList;
    }

    public JobInfo getPendingJob(int uid, int jobId) {
        synchronized (this.mLock) {
            List<JobStatus> jobs = this.mJobs.getJobsByUid(uid);
            for (int i = jobs.size() - 1; i >= 0; i--) {
                JobStatus job = jobs.get(i);
                if (job.getJobId() == jobId) {
                    return job.getJob();
                }
            }
            return null;
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void cancelJobsForUserLocked(int userHandle) {
        List<JobStatus> jobsForUser = this.mJobs.getJobsByUser(userHandle);
        for (int i = 0; i < jobsForUser.size(); i++) {
            JobStatus toRemove = jobsForUser.get(i);
            cancelJobImplLocked(toRemove, null, 13, 7, "user removed");
        }
    }

    private void cancelJobsForNonExistentUsers() {
        UserManagerInternal umi = (UserManagerInternal) LocalServices.getService(UserManagerInternal.class);
        synchronized (this.mLock) {
            this.mJobs.removeJobsOfUnlistedUsers(umi.getUserIds());
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void cancelJobsForPackageAndUidLocked(String pkgName, int uid, int reason, int internalReasonCode, String debugReason) {
        if (PackageManagerService.PLATFORM_PACKAGE_NAME.equals(pkgName)) {
            Slog.wtfStack(TAG, "Can't cancel all jobs for system package");
            return;
        }
        List<JobStatus> jobsForUid = this.mJobs.getJobsByUid(uid);
        for (int i = jobsForUid.size() - 1; i >= 0; i--) {
            JobStatus job = jobsForUid.get(i);
            if (job.getSourcePackageName().equals(pkgName)) {
                cancelJobImplLocked(job, null, reason, internalReasonCode, debugReason);
            }
        }
    }

    public boolean cancelJobsForUid(int uid, int reason, int internalReasonCode, String debugReason) {
        if (uid == 1000) {
            Slog.wtfStack(TAG, "Can't cancel all jobs for system uid");
            return false;
        }
        boolean jobsCanceled = false;
        synchronized (this.mLock) {
            List<JobStatus> jobsForUid = this.mJobs.getJobsByUid(uid);
            for (int i = 0; i < jobsForUid.size(); i++) {
                JobStatus toRemove = jobsForUid.get(i);
                cancelJobImplLocked(toRemove, null, reason, internalReasonCode, debugReason);
                jobsCanceled = true;
            }
        }
        return jobsCanceled;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public boolean cancelJob(int uid, int jobId, int callingUid, int reason) {
        boolean z;
        synchronized (this.mLock) {
            JobStatus toCancel = this.mJobs.getJobByUidAndJobId(uid, jobId);
            if (toCancel != null) {
                cancelJobImplLocked(toCancel, null, reason, 0, "cancel() called by app, callingUid=" + callingUid + " uid=" + uid + " jobId=" + jobId);
            }
            z = toCancel != null;
        }
        return z;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void cancelJobImplLocked(JobStatus cancelled, JobStatus incomingJob, int reason, int internalReasonCode, String debugReason) {
        boolean z = DEBUG;
        if (z) {
            Slog.d(TAG, "CANCEL: " + cancelled.toShortString());
        }
        cancelled.unprepareLocked();
        stopTrackingJobLocked(cancelled, incomingJob, true);
        if (this.mPendingJobQueue.remove(cancelled)) {
            this.mJobPackageTracker.noteNonpending(cancelled);
        }
        this.mChangedJobList.remove(cancelled);
        this.mConcurrencyManager.stopJobOnServiceContextLocked(cancelled, reason, internalReasonCode, debugReason);
        if (incomingJob != null) {
            if (z) {
                Slog.i(TAG, "Tracking replacement job " + incomingJob.toShortString());
            }
            startTrackingJobLocked(incomingJob, cancelled);
        }
        reportActiveLocked();
    }

    void updateUidState(int uid, int procState) {
        synchronized (this.mLock) {
            int prevBias = this.mUidBiasOverride.get(uid, 0);
            if (procState == 2) {
                this.mUidBiasOverride.put(uid, 40);
            } else if (procState <= 4) {
                this.mUidBiasOverride.put(uid, 35);
            } else if (procState <= 5) {
                this.mUidBiasOverride.put(uid, 30);
            } else {
                this.mUidBiasOverride.delete(uid);
            }
            int newBias = this.mUidBiasOverride.get(uid, 0);
            if (prevBias != newBias) {
                if (DEBUG) {
                    Slog.d(TAG, "UID " + uid + " bias changed from " + prevBias + " to " + newBias);
                }
                for (int c = 0; c < this.mControllers.size(); c++) {
                    this.mControllers.get(c).onUidBiasChangedLocked(uid, prevBias, newBias);
                }
                this.mConcurrencyManager.onUidBiasChangedLocked(prevBias, newBias);
            }
        }
    }

    public int getUidBias(int uid) {
        int i;
        synchronized (this.mLock) {
            i = this.mUidBiasOverride.get(uid, 0);
        }
        return i;
    }

    @Override // com.android.server.job.StateChangedListener
    public void onDeviceIdleStateChanged(boolean deviceIdle) {
        synchronized (this.mLock) {
            if (DEBUG) {
                Slog.d(TAG, "Doze state changed: " + deviceIdle);
            }
            if (!deviceIdle && this.mReadyToRock) {
                DeviceIdleInternal deviceIdleInternal = this.mLocalDeviceIdleController;
                if (deviceIdleInternal != null && !this.mReportedActive) {
                    this.mReportedActive = true;
                    deviceIdleInternal.setJobsActive(true);
                }
                this.mHandler.obtainMessage(1).sendToTarget();
            }
        }
    }

    @Override // com.android.server.job.StateChangedListener
    public void onRestrictedBucketChanged(List<JobStatus> jobs) {
        int len = jobs.size();
        if (len == 0) {
            Slog.wtf(TAG, "onRestrictedBucketChanged called with no jobs");
            return;
        }
        synchronized (this.mLock) {
            for (int i = 0; i < len; i++) {
                JobStatus js = jobs.get(i);
                for (int j = this.mRestrictiveControllers.size() - 1; j >= 0; j--) {
                    if (js.getStandbyBucket() == 5) {
                        this.mRestrictiveControllers.get(j).startTrackingRestrictedJobLocked(js);
                    } else {
                        this.mRestrictiveControllers.get(j).stopTrackingRestrictedJobLocked(js);
                    }
                }
            }
        }
        this.mHandler.obtainMessage(1).sendToTarget();
    }

    void reportActiveLocked() {
        boolean active = this.mPendingJobQueue.size() > 0;
        if (!active) {
            ArraySet<JobStatus> runningJobs = this.mConcurrencyManager.getRunningJobsLocked();
            int i = runningJobs.size() - 1;
            while (true) {
                if (i < 0) {
                    break;
                }
                JobStatus job = runningJobs.valueAt(i);
                if (job.canRunInDoze()) {
                    i--;
                } else {
                    active = true;
                    break;
                }
            }
        }
        if (this.mReportedActive != active) {
            this.mReportedActive = active;
            DeviceIdleInternal deviceIdleInternal = this.mLocalDeviceIdleController;
            if (deviceIdleInternal != null) {
                deviceIdleInternal.setJobsActive(active);
            }
        }
    }

    void reportAppUsage(String packageName, int userId) {
    }

    public JobSchedulerService(Context context) {
        super(context);
        this.mLock = new Object();
        this.mJobPackageTracker = new JobPackageTracker();
        this.mCloudMediaProviderPackages = new SparseArray<>();
        this.mPendingJobQueue = new PendingJobQueue();
        this.mStartedUsers = EmptyArray.INT;
        this.mLastCompletedJobIndex = 0;
        this.mLastCompletedJobs = new JobStatus[20];
        this.mLastCompletedJobTimeElapsed = new long[20];
        this.mUidBiasOverride = new SparseIntArray();
        this.mBackingUpUids = new SparseBooleanArray();
        this.mDebuggableApps = new ArrayMap<>();
        this.mUidToPackageCache = new SparseSetArray<>();
        this.mChangedJobList = new ArraySet<>();
        this.mBroadcastReceiver = new BroadcastReceiver() { // from class: com.android.server.job.JobSchedulerService.3
            @Override // android.content.BroadcastReceiver
            public void onReceive(Context context2, Intent intent) {
                List<JobStatus> jobsForUid;
                String action = intent.getAction();
                if (JobSchedulerService.DEBUG) {
                    Slog.d(JobSchedulerService.TAG, "Receieved: " + action);
                }
                String pkgName = JobSchedulerService.this.getPackageName(intent);
                int pkgUid = intent.getIntExtra("android.intent.extra.UID", -1);
                int i = 0;
                if ("android.intent.action.PACKAGE_CHANGED".equals(action)) {
                    if (pkgName != null && pkgUid != -1) {
                        String[] changedComponents = intent.getStringArrayExtra("android.intent.extra.changed_component_name_list");
                        if (changedComponents != null) {
                            int length = changedComponents.length;
                            while (true) {
                                if (i >= length) {
                                    break;
                                }
                                String component = changedComponents[i];
                                if (!component.equals(pkgName)) {
                                    i++;
                                } else {
                                    if (JobSchedulerService.DEBUG) {
                                        Slog.d(JobSchedulerService.TAG, "Package state change: " + pkgName);
                                    }
                                    try {
                                        int userId = UserHandle.getUserId(pkgUid);
                                        IPackageManager pm = AppGlobals.getPackageManager();
                                        int state = pm.getApplicationEnabledSetting(pkgName, userId);
                                        if (state == 2 || state == 3) {
                                            if (JobSchedulerService.DEBUG) {
                                                Slog.d(JobSchedulerService.TAG, "Removing jobs for package " + pkgName + " in user " + userId);
                                            }
                                            try {
                                                synchronized (JobSchedulerService.this.mLock) {
                                                    try {
                                                        JobSchedulerService.this.cancelJobsForPackageAndUidLocked(pkgName, pkgUid, 13, 7, "app disabled");
                                                    } catch (Throwable th) {
                                                        th = th;
                                                    }
                                                }
                                            } catch (Throwable th2) {
                                                th = th2;
                                            }
                                            throw th;
                                        }
                                    } catch (RemoteException | IllegalArgumentException e) {
                                    }
                                }
                            }
                            if (JobSchedulerService.DEBUG) {
                                Slog.d(JobSchedulerService.TAG, "Something in " + pkgName + " changed. Reevaluating controller states.");
                            }
                            synchronized (JobSchedulerService.this.mLock) {
                                for (int c = JobSchedulerService.this.mControllers.size() - 1; c >= 0; c--) {
                                    JobSchedulerService.this.mControllers.get(c).reevaluateStateLocked(pkgUid);
                                }
                            }
                            return;
                        }
                        return;
                    }
                    Slog.w(JobSchedulerService.TAG, "PACKAGE_CHANGED for " + pkgName + " / uid " + pkgUid);
                } else if ("android.intent.action.PACKAGE_ADDED".equals(action)) {
                    if (!intent.getBooleanExtra("android.intent.extra.REPLACING", false)) {
                        int uid = intent.getIntExtra("android.intent.extra.UID", -1);
                        synchronized (JobSchedulerService.this.mLock) {
                            JobSchedulerService.this.mUidToPackageCache.remove(uid);
                        }
                    }
                } else if ("android.intent.action.PACKAGE_FULLY_REMOVED".equals(action)) {
                    if (JobSchedulerService.DEBUG) {
                        Slog.d(JobSchedulerService.TAG, "Removing jobs for " + pkgName + " (uid=" + pkgUid + ")");
                    }
                    synchronized (JobSchedulerService.this.mLock) {
                        JobSchedulerService.this.mUidToPackageCache.remove(pkgUid);
                        JobSchedulerService.this.cancelJobsForPackageAndUidLocked(pkgName, pkgUid, 13, 7, "app uninstalled");
                        for (int c2 = 0; c2 < JobSchedulerService.this.mControllers.size(); c2++) {
                            JobSchedulerService.this.mControllers.get(c2).onAppRemovedLocked(pkgName, pkgUid);
                        }
                        JobSchedulerService.this.mDebuggableApps.remove(pkgName);
                        JobSchedulerService.this.mConcurrencyManager.onAppRemovedLocked(pkgName, pkgUid);
                    }
                } else if ("android.intent.action.USER_ADDED".equals(action)) {
                    int userId2 = intent.getIntExtra("android.intent.extra.user_handle", 0);
                    synchronized (JobSchedulerService.this.mLock) {
                        for (int c3 = 0; c3 < JobSchedulerService.this.mControllers.size(); c3++) {
                            JobSchedulerService.this.mControllers.get(c3).onUserAddedLocked(userId2);
                        }
                    }
                } else if ("android.intent.action.USER_REMOVED".equals(action)) {
                    int userId3 = intent.getIntExtra("android.intent.extra.user_handle", 0);
                    if (JobSchedulerService.DEBUG) {
                        Slog.d(JobSchedulerService.TAG, "Removing jobs for user: " + userId3);
                    }
                    synchronized (JobSchedulerService.this.mLock) {
                        JobSchedulerService.this.mUidToPackageCache.clear();
                        JobSchedulerService.this.cancelJobsForUserLocked(userId3);
                        for (int c4 = 0; c4 < JobSchedulerService.this.mControllers.size(); c4++) {
                            JobSchedulerService.this.mControllers.get(c4).onUserRemovedLocked(userId3);
                        }
                    }
                    JobSchedulerService.this.mConcurrencyManager.onUserRemoved(userId3);
                } else if ("android.intent.action.QUERY_PACKAGE_RESTART".equals(action)) {
                    if (pkgUid != -1) {
                        synchronized (JobSchedulerService.this.mLock) {
                            jobsForUid = JobSchedulerService.this.mJobs.getJobsByUid(pkgUid);
                        }
                        for (int i2 = jobsForUid.size() - 1; i2 >= 0; i2--) {
                            if (jobsForUid.get(i2).getSourcePackageName().equals(pkgName)) {
                                if (JobSchedulerService.DEBUG) {
                                    Slog.d(JobSchedulerService.TAG, "Restart query: package " + pkgName + " at uid " + pkgUid + " has jobs");
                                }
                                setResultCode(-1);
                                return;
                            }
                        }
                    }
                } else if ("android.intent.action.PACKAGE_RESTARTED".equals(action) && pkgUid != -1) {
                    if (JobSchedulerService.DEBUG) {
                        Slog.d(JobSchedulerService.TAG, "Removing jobs for pkg " + pkgName + " at uid " + pkgUid);
                    }
                    synchronized (JobSchedulerService.this.mLock) {
                        JobSchedulerService.this.cancelJobsForPackageAndUidLocked(pkgName, pkgUid, 13, 0, "app force stopped");
                    }
                }
            }
        };
        this.mUidObserver = new IUidObserver.Stub() { // from class: com.android.server.job.JobSchedulerService.4
            public void onUidStateChanged(int uid, int procState, long procStateSeq, int capability) {
                JobSchedulerService.this.mHandler.obtainMessage(4, uid, procState).sendToTarget();
            }

            public void onUidGone(int uid, boolean disabled) {
                JobSchedulerService.this.mHandler.obtainMessage(5, uid, disabled ? 1 : 0).sendToTarget();
            }

            public void onUidActive(int uid) throws RemoteException {
                JobSchedulerService.this.mHandler.obtainMessage(6, uid, 0).sendToTarget();
            }

            public void onUidIdle(int uid, boolean disabled) {
                JobSchedulerService.this.mHandler.obtainMessage(7, uid, disabled ? 1 : 0).sendToTarget();
            }

            public void onUidCachedChanged(int uid, boolean cached) {
            }

            public void onUidProcAdjChanged(int uid) {
            }
        };
        this.mIsUidActivePredicate = new Predicate() { // from class: com.android.server.job.JobSchedulerService$$ExternalSyntheticLambda2
            @Override // java.util.function.Predicate
            public final boolean test(Object obj) {
                boolean isUidActive;
                isUidActive = JobSchedulerService.this.isUidActive(((Integer) obj).intValue());
                return isUidActive;
            }
        };
        BroadcastReceiver broadcastReceiver = new BroadcastReceiver() { // from class: com.android.server.job.JobSchedulerService.5
            @Override // android.content.BroadcastReceiver
            public void onReceive(Context context2, Intent intent) {
                if ("android.intent.action.TIME_SET".equals(intent.getAction()) && JobSchedulerService.this.mJobs.clockNowValidToInflate(JobSchedulerService.sSystemClock.millis())) {
                    Slog.i(JobSchedulerService.TAG, "RTC now valid; recalculating persisted job windows");
                    context2.unregisterReceiver(this);
                    new Thread(JobSchedulerService.this.mJobTimeUpdater, "JobSchedulerTimeSetReceiver").start();
                }
            }
        };
        this.mTimeSetReceiver = broadcastReceiver;
        this.mJobTimeUpdater = new Runnable() { // from class: com.android.server.job.JobSchedulerService$$ExternalSyntheticLambda3
            @Override // java.lang.Runnable
            public final void run() {
                JobSchedulerService.this.m4071lambda$new$1$comandroidserverjobJobSchedulerService();
            }
        };
        this.mReadyQueueFunctor = new ReadyJobQueueFunctor();
        this.mMaybeQueueFunctor = new MaybeReadyJobQueueFunctor();
        this.mLocalPM = (PackageManagerInternal) LocalServices.getService(PackageManagerInternal.class);
        this.mActivityManagerInternal = (ActivityManagerInternal) Objects.requireNonNull((ActivityManagerInternal) LocalServices.getService(ActivityManagerInternal.class));
        this.mHandler = new JobHandler(context.getMainLooper());
        Constants constants = new Constants();
        this.mConstants = constants;
        this.mConstantsObserver = new ConstantsObserver();
        this.mJobSchedulerStub = new JobSchedulerStub();
        this.mConcurrencyManager = new JobConcurrencyManager(this);
        StandbyTracker standbyTracker = new StandbyTracker();
        this.mStandbyTracker = standbyTracker;
        this.mUsageStats = (UsageStatsManagerInternal) LocalServices.getService(UsageStatsManagerInternal.class);
        CountQuotaTracker countQuotaTracker = new CountQuotaTracker(context, QUOTA_CATEGORIZER);
        this.mQuotaTracker = countQuotaTracker;
        countQuotaTracker.setCountLimit(QUOTA_TRACKER_CATEGORY_SCHEDULE_PERSISTED, constants.API_QUOTA_SCHEDULE_COUNT, constants.API_QUOTA_SCHEDULE_WINDOW_MS);
        countQuotaTracker.setCountLimit(QUOTA_TRACKER_CATEGORY_SCHEDULE_LOGGED, 1, 60000L);
        AppStandbyInternal appStandbyInternal = (AppStandbyInternal) LocalServices.getService(AppStandbyInternal.class);
        this.mAppStandbyInternal = appStandbyInternal;
        appStandbyInternal.addListener(standbyTracker);
        publishLocalService(JobSchedulerInternal.class, new LocalService());
        JobStore initAndGet = JobStore.initAndGet(this);
        this.mJobs = initAndGet;
        BatteryStateTracker batteryStateTracker = new BatteryStateTracker();
        this.mBatteryStateTracker = batteryStateTracker;
        batteryStateTracker.startTracking();
        ArrayList arrayList = new ArrayList();
        this.mControllers = arrayList;
        ConnectivityController connectivityController = new ConnectivityController(this);
        arrayList.add(connectivityController);
        arrayList.add(new TimeController(this));
        IdleController idleController = new IdleController(this);
        arrayList.add(idleController);
        BatteryController batteryController = new BatteryController(this);
        arrayList.add(batteryController);
        StorageController storageController = new StorageController(this);
        this.mStorageController = storageController;
        arrayList.add(storageController);
        BackgroundJobsController backgroundJobsController = new BackgroundJobsController(this);
        arrayList.add(backgroundJobsController);
        arrayList.add(new ContentObserverController(this));
        DeviceIdleJobsController deviceIdleJobsController = new DeviceIdleJobsController(this);
        this.mDeviceIdleJobsController = deviceIdleJobsController;
        arrayList.add(deviceIdleJobsController);
        PrefetchController prefetchController = new PrefetchController(this);
        this.mPrefetchController = prefetchController;
        arrayList.add(prefetchController);
        QuotaController quotaController = new QuotaController(this, backgroundJobsController, connectivityController);
        this.mQuotaController = quotaController;
        arrayList.add(quotaController);
        arrayList.add(new ComponentController(this));
        TareController tareController = new TareController(this, backgroundJobsController, connectivityController);
        this.mTareController = tareController;
        arrayList.add(tareController);
        ArrayList arrayList2 = new ArrayList();
        this.mRestrictiveControllers = arrayList2;
        arrayList2.add(batteryController);
        arrayList2.add(connectivityController);
        arrayList2.add(idleController);
        ArrayList arrayList3 = new ArrayList();
        this.mJobRestrictions = arrayList3;
        arrayList3.add(new ThermalStatusRestriction(this));
        if (!initAndGet.jobTimesInflatedValid()) {
            Slog.w(TAG, "!!! RTC not yet good; tracking time updates for job scheduling");
            context.registerReceiver(broadcastReceiver, new IntentFilter("android.intent.action.TIME_SET"));
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$new$1$com-android-server-job-JobSchedulerService  reason: not valid java name */
    public /* synthetic */ void m4071lambda$new$1$comandroidserverjobJobSchedulerService() {
        Process.setThreadPriority(-2);
        ArrayList<JobStatus> toRemove = new ArrayList<>();
        ArrayList<JobStatus> toAdd = new ArrayList<>();
        synchronized (this.mLock) {
            getJobStore().getRtcCorrectedJobsLocked(toAdd, toRemove);
            int N = toAdd.size();
            for (int i = 0; i < N; i++) {
                JobStatus oldJob = toRemove.get(i);
                JobStatus newJob = toAdd.get(i);
                if (DEBUG) {
                    Slog.v(TAG, "  replacing " + oldJob + " with " + newJob);
                }
                cancelJobImplLocked(oldJob, newJob, 14, 9, "deferred rtc calculation");
            }
        }
    }

    @Override // com.android.server.SystemService
    public void onStart() {
        publishBinderService("jobscheduler", this.mJobSchedulerStub);
    }

    @Override // com.android.server.SystemService
    public void onBootPhase(int phase) {
        if (500 == phase) {
            this.mConstantsObserver.start();
            for (StateController controller : this.mControllers) {
                controller.onSystemServicesReady();
            }
            this.mAppStateTracker = (AppStateTrackerImpl) Objects.requireNonNull((AppStateTracker) LocalServices.getService(AppStateTracker.class));
            ((StorageManagerInternal) LocalServices.getService(StorageManagerInternal.class)).registerCloudProviderChangeListener(new CloudProviderChangeListener());
            IntentFilter filter = new IntentFilter();
            filter.addAction("android.intent.action.PACKAGE_FULLY_REMOVED");
            filter.addAction("android.intent.action.PACKAGE_ADDED");
            filter.addAction("android.intent.action.PACKAGE_CHANGED");
            filter.addAction("android.intent.action.PACKAGE_RESTARTED");
            filter.addAction("android.intent.action.QUERY_PACKAGE_RESTART");
            filter.addDataScheme("package");
            getContext().registerReceiverAsUser(this.mBroadcastReceiver, UserHandle.ALL, filter, null, null);
            IntentFilter userFilter = new IntentFilter("android.intent.action.USER_REMOVED");
            userFilter.addAction("android.intent.action.USER_ADDED");
            getContext().registerReceiverAsUser(this.mBroadcastReceiver, UserHandle.ALL, userFilter, null, null);
            try {
                ActivityManager.getService().registerUidObserver(this.mUidObserver, 15, -1, (String) null);
            } catch (RemoteException e) {
            }
            this.mConcurrencyManager.onSystemReady();
            cancelJobsForNonExistentUsers();
            for (int i = this.mJobRestrictions.size() - 1; i >= 0; i--) {
                this.mJobRestrictions.get(i).onSystemServicesReady();
            }
        } else if (phase == 600) {
            synchronized (this.mLock) {
                this.mReadyToRock = true;
                this.mLocalDeviceIdleController = (DeviceIdleInternal) LocalServices.getService(DeviceIdleInternal.class);
                this.mConcurrencyManager.onThirdPartyAppsCanStart();
                this.mJobs.forEachJob(new Consumer() { // from class: com.android.server.job.JobSchedulerService$$ExternalSyntheticLambda4
                    @Override // java.util.function.Consumer
                    public final void accept(Object obj) {
                        JobSchedulerService.this.m4072lambda$onBootPhase$2$comandroidserverjobJobSchedulerService((JobStatus) obj);
                    }
                });
                this.mHandler.obtainMessage(1).sendToTarget();
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$onBootPhase$2$com-android-server-job-JobSchedulerService  reason: not valid java name */
    public /* synthetic */ void m4072lambda$onBootPhase$2$comandroidserverjobJobSchedulerService(JobStatus job) {
        for (int controller = 0; controller < this.mControllers.size(); controller++) {
            StateController sc = this.mControllers.get(controller);
            sc.maybeStartTrackingJobLocked(job, null);
        }
    }

    private void startTrackingJobLocked(JobStatus jobStatus, JobStatus lastJob) {
        if (!jobStatus.isPreparedLocked()) {
            Slog.wtf(TAG, "Not yet prepared when started tracking: " + jobStatus);
        }
        jobStatus.enqueueTime = sElapsedRealtimeClock.millis();
        boolean update = this.mJobs.add(jobStatus);
        if (this.mReadyToRock) {
            for (int i = 0; i < this.mControllers.size(); i++) {
                StateController controller = this.mControllers.get(i);
                if (update) {
                    controller.maybeStopTrackingJobLocked(jobStatus, null, true);
                }
                controller.maybeStartTrackingJobLocked(jobStatus, lastJob);
            }
        }
    }

    private boolean stopTrackingJobLocked(JobStatus jobStatus, JobStatus incomingJob, boolean removeFromPersisted) {
        jobStatus.stopTrackingJobLocked(incomingJob);
        boolean removed = this.mJobs.remove(jobStatus, removeFromPersisted);
        if (!removed) {
            Slog.w(TAG, "Job didn't exist in JobStore: " + jobStatus.toShortString());
        }
        if (this.mReadyToRock) {
            for (int i = 0; i < this.mControllers.size(); i++) {
                StateController controller = this.mControllers.get(i);
                controller.maybeStopTrackingJobLocked(jobStatus, incomingJob, false);
            }
        }
        return removed;
    }

    public boolean isCurrentlyRunningLocked(JobStatus job) {
        return this.mConcurrencyManager.isJobRunningLocked(job);
    }

    private void noteJobPending(JobStatus job) {
        this.mJobPackageTracker.notePending(job);
    }

    void noteJobsPending(List<JobStatus> jobs) {
        for (int i = jobs.size() - 1; i >= 0; i--) {
            noteJobPending(jobs.get(i));
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void noteJobNonPending(JobStatus job) {
        this.mJobPackageTracker.noteNonpending(job);
    }

    private void clearPendingJobQueue() {
        this.mPendingJobQueue.resetIterator();
        while (true) {
            JobStatus job = this.mPendingJobQueue.next();
            if (job != null) {
                noteJobNonPending(job);
            } else {
                this.mPendingJobQueue.clear();
                return;
            }
        }
    }

    JobStatus getRescheduleJobForFailureLocked(JobStatus failureToReschedule) {
        long backoff;
        long elapsedNowMillis = sElapsedRealtimeClock.millis();
        JobInfo job = failureToReschedule.getJob();
        long initialBackoffMillis = job.getInitialBackoffMillis();
        int backoffAttempts = failureToReschedule.getNumFailures() + 1;
        switch (job.getBackoffPolicy()) {
            case 0:
                long backoff2 = initialBackoffMillis;
                if (backoff2 < this.mConstants.MIN_LINEAR_BACKOFF_TIME_MS) {
                    backoff2 = this.mConstants.MIN_LINEAR_BACKOFF_TIME_MS;
                }
                backoff = backoff2 * backoffAttempts;
                break;
            default:
                if (DEBUG) {
                    Slog.v(TAG, "Unrecognised back-off policy, defaulting to exponential.");
                }
            case 1:
                long backoff3 = initialBackoffMillis;
                if (backoff3 < this.mConstants.MIN_EXP_BACKOFF_TIME_MS) {
                    backoff3 = this.mConstants.MIN_EXP_BACKOFF_TIME_MS;
                }
                backoff = Math.scalb((float) backoff3, backoffAttempts - 1);
                break;
        }
        long delayMillis = Math.min(backoff, 18000000L);
        JobStatus newJob = new JobStatus(failureToReschedule, elapsedNowMillis + delayMillis, JobStatus.NO_LATEST_RUNTIME, backoffAttempts, failureToReschedule.getLastSuccessfulRunTime(), sSystemClock.millis());
        if (job.isPeriodic()) {
            newJob.setOriginalLatestRunTimeElapsed(failureToReschedule.getOriginalLatestRunTimeElapsed());
        }
        for (int ic = 0; ic < this.mControllers.size(); ic++) {
            StateController controller = this.mControllers.get(ic);
            controller.rescheduleForFailureLocked(newJob, failureToReschedule);
        }
        return newJob;
    }

    JobStatus getRescheduleJobForPeriodic(JobStatus periodicToReschedule) {
        long rescheduleBuffer;
        long olrte;
        long rescheduleBuffer2;
        long elapsedNow = sElapsedRealtimeClock.millis();
        long period = Math.max(JobInfo.getMinPeriodMillis(), Math.min(31536000000L, periodicToReschedule.getJob().getIntervalMillis()));
        long flex = Math.max(JobInfo.getMinFlexMillis(), Math.min(period, periodicToReschedule.getJob().getFlexMillis()));
        long olrte2 = periodicToReschedule.getOriginalLatestRunTimeElapsed();
        if (olrte2 < 0 || olrte2 == JobStatus.NO_LATEST_RUNTIME) {
            Slog.wtf(TAG, "Invalid periodic job original latest run time: " + olrte2);
            olrte2 = elapsedNow;
        }
        long latestRunTimeElapsed = olrte2;
        long diffMs = Math.abs(elapsedNow - latestRunTimeElapsed);
        if (elapsedNow > latestRunTimeElapsed) {
            boolean z = DEBUG;
            if (!z) {
                rescheduleBuffer2 = 0;
            } else {
                rescheduleBuffer2 = 0;
                Slog.i(TAG, "Periodic job ran after its intended window by " + diffMs + " ms");
            }
            long numSkippedWindows = (diffMs / period) + 1;
            if (period != flex && (period - flex) - (diffMs % period) <= flex / 6) {
                if (z) {
                    Slog.d(TAG, "Custom flex job ran too close to next window.");
                }
                numSkippedWindows++;
            }
            long newLatestRuntimeElapsed = latestRunTimeElapsed + (period * numSkippedWindows);
            olrte = newLatestRuntimeElapsed;
            rescheduleBuffer = rescheduleBuffer2;
        } else {
            long rescheduleBuffer3 = latestRunTimeElapsed + period;
            if (diffMs < 1800000 && diffMs < period / 6) {
                rescheduleBuffer = Math.min(1800000L, (period / 6) - diffMs);
                olrte = rescheduleBuffer3;
            } else {
                rescheduleBuffer = 0;
                olrte = rescheduleBuffer3;
            }
        }
        if (olrte < elapsedNow) {
            Slog.wtf(TAG, "Rescheduling calculated latest runtime in the past: " + olrte);
            return new JobStatus(periodicToReschedule, (elapsedNow + period) - flex, elapsedNow + period, 0, sSystemClock.millis(), periodicToReschedule.getLastFailedRunTime());
        }
        long newEarliestRunTimeElapsed = olrte - Math.min(flex, period - rescheduleBuffer);
        if (DEBUG) {
            Slog.v(TAG, "Rescheduling executed periodic. New execution window [" + (newEarliestRunTimeElapsed / 1000) + ", " + (olrte / 1000) + "]s");
        }
        return new JobStatus(periodicToReschedule, newEarliestRunTimeElapsed, olrte, 0, sSystemClock.millis(), periodicToReschedule.getLastFailedRunTime());
    }

    @Override // com.android.server.job.JobCompletedListener
    public void onJobCompletedLocked(JobStatus jobStatus, int debugStopReason, boolean needsReschedule) {
        boolean z = DEBUG;
        if (z) {
            Slog.d(TAG, "Completed " + jobStatus + ", reason=" + debugStopReason + ", reschedule=" + needsReschedule);
        }
        JobStatus[] jobStatusArr = this.mLastCompletedJobs;
        int i = this.mLastCompletedJobIndex;
        jobStatusArr[i] = jobStatus;
        this.mLastCompletedJobTimeElapsed[i] = sElapsedRealtimeClock.millis();
        this.mLastCompletedJobIndex = (this.mLastCompletedJobIndex + 1) % 20;
        if (debugStopReason == 7 || debugStopReason == 8) {
            jobStatus.unprepareLocked();
            reportActiveLocked();
            return;
        }
        JobStatus rescheduledJob = needsReschedule ? getRescheduleJobForFailureLocked(jobStatus) : null;
        if (rescheduledJob != null && (debugStopReason == 3 || debugStopReason == 2)) {
            rescheduledJob.disallowRunInBatterySaverAndDoze();
        }
        if (!stopTrackingJobLocked(jobStatus, rescheduledJob, !jobStatus.getJob().isPeriodic())) {
            if (z) {
                Slog.d(TAG, "Could not find job to remove. Was job removed while executing?");
            }
            JobStatus newJs = this.mJobs.getJobByUidAndJobId(jobStatus.getUid(), jobStatus.getJobId());
            if (newJs != null) {
                this.mHandler.obtainMessage(0, newJs).sendToTarget();
                return;
            }
            return;
        }
        if (rescheduledJob != null) {
            try {
                rescheduledJob.prepareLocked();
            } catch (SecurityException e) {
                Slog.w(TAG, "Unable to regrant job permissions for " + rescheduledJob);
            }
            startTrackingJobLocked(rescheduledJob, jobStatus);
        } else if (jobStatus.getJob().isPeriodic()) {
            JobStatus rescheduledPeriodic = getRescheduleJobForPeriodic(jobStatus);
            try {
                rescheduledPeriodic.prepareLocked();
            } catch (SecurityException e2) {
                Slog.w(TAG, "Unable to regrant job permissions for " + rescheduledPeriodic);
            }
            startTrackingJobLocked(rescheduledPeriodic, jobStatus);
        }
        jobStatus.unprepareLocked();
        reportActiveLocked();
    }

    @Override // com.android.server.job.StateChangedListener
    public void onControllerStateChanged(ArraySet<JobStatus> changedJobs) {
        if (changedJobs == null) {
            this.mHandler.obtainMessage(1).sendToTarget();
        } else if (changedJobs.size() > 0) {
            synchronized (this.mLock) {
                this.mChangedJobList.addAll((ArraySet<? extends JobStatus>) changedJobs);
            }
            this.mHandler.obtainMessage(8).sendToTarget();
        }
    }

    @Override // com.android.server.job.StateChangedListener
    public void onRunJobNow(JobStatus jobStatus) {
        if (jobStatus == null) {
            this.mHandler.obtainMessage(3).sendToTarget();
        } else {
            this.mHandler.obtainMessage(0, jobStatus).sendToTarget();
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes.dex */
    public final class JobHandler extends Handler {
        public JobHandler(Looper looper) {
            super(looper);
        }

        @Override // android.os.Handler
        public void handleMessage(Message message) {
            synchronized (JobSchedulerService.this.mLock) {
                if (JobSchedulerService.this.mReadyToRock) {
                    switch (message.what) {
                        case 0:
                            JobStatus js = (JobStatus) message.obj;
                            if (js != null) {
                                if (JobSchedulerService.this.isReadyToBeExecutedLocked(js)) {
                                    JobSchedulerService.this.mJobPackageTracker.notePending(js);
                                    JobSchedulerService.this.mPendingJobQueue.add(js);
                                }
                                JobSchedulerService.this.mChangedJobList.remove(js);
                                break;
                            } else {
                                Slog.e(JobSchedulerService.TAG, "Given null job to check individually");
                                break;
                            }
                        case 1:
                            if (JobSchedulerService.DEBUG) {
                                Slog.d(JobSchedulerService.TAG, "MSG_CHECK_JOB");
                            }
                            if (JobSchedulerService.this.mReportedActive) {
                                JobSchedulerService.this.queueReadyJobsForExecutionLocked();
                                break;
                            } else {
                                JobSchedulerService.this.maybeQueueReadyJobsForExecutionLocked();
                                break;
                            }
                        case 2:
                            JobSchedulerService.this.cancelJobImplLocked((JobStatus) message.obj, null, message.arg1, 1, "app no longer allowed to run");
                            break;
                        case 3:
                            if (JobSchedulerService.DEBUG) {
                                Slog.d(JobSchedulerService.TAG, "MSG_CHECK_JOB_GREEDY");
                            }
                            JobSchedulerService.this.queueReadyJobsForExecutionLocked();
                            break;
                        case 4:
                            int uid = message.arg1;
                            int procState = message.arg2;
                            JobSchedulerService.this.updateUidState(uid, procState);
                            break;
                        case 5:
                            int uid2 = message.arg1;
                            boolean disabled = message.arg2 != 0;
                            JobSchedulerService.this.updateUidState(uid2, 19);
                            if (disabled) {
                                JobSchedulerService.this.cancelJobsForUid(uid2, 11, 1, "uid gone");
                            }
                            synchronized (JobSchedulerService.this.mLock) {
                                JobSchedulerService.this.mDeviceIdleJobsController.setUidActiveLocked(uid2, false);
                            }
                            break;
                        case 6:
                            int uid3 = message.arg1;
                            synchronized (JobSchedulerService.this.mLock) {
                                JobSchedulerService.this.mDeviceIdleJobsController.setUidActiveLocked(uid3, true);
                            }
                            break;
                        case 7:
                            int uid4 = message.arg1;
                            boolean disabled2 = message.arg2 != 0;
                            if (disabled2) {
                                JobSchedulerService.this.cancelJobsForUid(uid4, 11, 1, "app uid idle");
                            }
                            synchronized (JobSchedulerService.this.mLock) {
                                JobSchedulerService.this.mDeviceIdleJobsController.setUidActiveLocked(uid4, false);
                            }
                            break;
                        case 8:
                            if (JobSchedulerService.DEBUG) {
                                Slog.d(JobSchedulerService.TAG, "MSG_CHECK_CHANGED_JOB_LIST");
                            }
                            JobSchedulerService.this.checkChangedJobListLocked();
                            break;
                        case 9:
                            SomeArgs args = (SomeArgs) message.obj;
                            synchronized (JobSchedulerService.this.mLock) {
                                JobSchedulerService.this.updateMediaBackupExemptionLocked(args.argi1, (String) args.arg1, (String) args.arg2);
                            }
                            args.recycle();
                            break;
                    }
                    JobSchedulerService.this.maybeRunPendingJobsLocked();
                }
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public JobRestriction checkIfRestricted(JobStatus job) {
        if (evaluateJobBiasLocked(job) >= 35) {
            return null;
        }
        for (int i = this.mJobRestrictions.size() - 1; i >= 0; i--) {
            JobRestriction restriction = this.mJobRestrictions.get(i);
            if (restriction.isJobRestricted(job)) {
                return restriction;
            }
        }
        return null;
    }

    private void stopNonReadyActiveJobsLocked() {
        this.mConcurrencyManager.stopNonReadyActiveJobsLocked();
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void queueReadyJobsForExecutionLocked() {
        this.mHandler.removeMessages(3);
        this.mHandler.removeMessages(0);
        this.mHandler.removeMessages(1);
        this.mHandler.removeMessages(8);
        this.mChangedJobList.clear();
        boolean z = DEBUG;
        if (z) {
            Slog.d(TAG, "queuing all ready jobs for execution:");
        }
        clearPendingJobQueue();
        stopNonReadyActiveJobsLocked();
        this.mJobs.forEachJob(this.mReadyQueueFunctor);
        this.mReadyQueueFunctor.postProcessLocked();
        if (z) {
            int queuedJobs = this.mPendingJobQueue.size();
            if (queuedJobs == 0) {
                Slog.d(TAG, "No jobs pending.");
            } else {
                Slog.d(TAG, queuedJobs + " jobs queued.");
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* loaded from: classes.dex */
    public final class ReadyJobQueueFunctor implements Consumer<JobStatus> {
        final ArrayList<JobStatus> newReadyJobs = new ArrayList<>();

        ReadyJobQueueFunctor() {
        }

        /* JADX DEBUG: Method merged with bridge method */
        @Override // java.util.function.Consumer
        public void accept(JobStatus job) {
            if (JobSchedulerService.this.isReadyToBeExecutedLocked(job)) {
                if (JobSchedulerService.DEBUG) {
                    Slog.d(JobSchedulerService.TAG, "    queued " + job.toShortString());
                }
                this.newReadyJobs.add(job);
                return;
            }
            JobSchedulerService.this.evaluateControllerStatesLocked(job);
        }

        /* JADX INFO: Access modifiers changed from: private */
        public void postProcessLocked() {
            JobSchedulerService.this.noteJobsPending(this.newReadyJobs);
            JobSchedulerService.this.mPendingJobQueue.addAll(this.newReadyJobs);
            this.newReadyJobs.clear();
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* loaded from: classes.dex */
    public final class MaybeReadyJobQueueFunctor implements Consumer<JobStatus> {
        int forceBatchedCount;
        final List<JobStatus> runnableJobs = new ArrayList();
        int unbatchedCount;

        public MaybeReadyJobQueueFunctor() {
            reset();
        }

        /* JADX DEBUG: Method merged with bridge method */
        @Override // java.util.function.Consumer
        public void accept(JobStatus job) {
            int internalStopReason;
            String debugReason;
            boolean batchDelayExpired;
            boolean isRunning = JobSchedulerService.this.isCurrentlyRunningLocked(job);
            if (JobSchedulerService.this.isReadyToBeExecutedLocked(job, false)) {
                if (JobSchedulerService.this.mActivityManagerInternal.isAppStartModeDisabled(job.getUid(), job.getJob().getService().getPackageName())) {
                    Slog.w(JobSchedulerService.TAG, "Aborting job " + job.getUid() + ":" + job.getJob().toString() + " -- package not allowed to start");
                    if (isRunning) {
                        JobSchedulerService.this.mHandler.obtainMessage(2, 11, 0, job).sendToTarget();
                        return;
                    } else if (JobSchedulerService.this.mPendingJobQueue.remove(job)) {
                        JobSchedulerService.this.noteJobNonPending(job);
                        return;
                    } else {
                        return;
                    }
                }
                if (job.shouldTreatAsExpeditedJob()) {
                    batchDelayExpired = false;
                } else if (job.getEffectiveStandbyBucket() == 5) {
                    batchDelayExpired = true;
                } else if (job.getJob().isPrefetch()) {
                    long relativelySoonCutoffTime = JobSchedulerService.sSystemClock.millis() + JobSchedulerService.this.mConstants.PREFETCH_FORCE_BATCH_RELAX_THRESHOLD_MS;
                    batchDelayExpired = JobSchedulerService.this.mPrefetchController.getNextEstimatedLaunchTimeLocked(job) > relativelySoonCutoffTime;
                } else if (job.getNumFailures() > 0) {
                    batchDelayExpired = false;
                } else {
                    long nowElapsed = JobSchedulerService.sElapsedRealtimeClock.millis();
                    boolean batchDelayExpired2 = job.getFirstForceBatchedTimeElapsed() > 0 && nowElapsed - job.getFirstForceBatchedTimeElapsed() >= JobSchedulerService.this.mConstants.MAX_NON_ACTIVE_JOB_BATCH_DELAY_MS;
                    if (JobSchedulerService.this.mConstants.MIN_READY_NON_ACTIVE_JOBS_COUNT > 1 && job.getEffectiveStandbyBucket() != 0 && job.getEffectiveStandbyBucket() != 6 && !batchDelayExpired2) {
                        r2 = true;
                    }
                    batchDelayExpired = r2;
                }
                if (batchDelayExpired) {
                    this.forceBatchedCount++;
                    if (job.getFirstForceBatchedTimeElapsed() == 0) {
                        job.setFirstForceBatchedTimeElapsed(JobSchedulerService.sElapsedRealtimeClock.millis());
                    }
                } else {
                    this.unbatchedCount++;
                }
                if (!isRunning) {
                    this.runnableJobs.add(job);
                    return;
                }
                return;
            }
            if (isRunning) {
                if (!job.isReady()) {
                    if (job.getEffectiveStandbyBucket() == 5 && job.getStopReason() == 12) {
                        internalStopReason = 6;
                        debugReason = "cancelled due to restricted bucket";
                    } else {
                        internalStopReason = 1;
                        debugReason = "cancelled due to unsatisfied constraints";
                    }
                } else {
                    JobRestriction restriction = JobSchedulerService.this.checkIfRestricted(job);
                    if (restriction != null) {
                        int internalStopReason2 = restriction.getInternalReason();
                        internalStopReason = internalStopReason2;
                        debugReason = "restricted due to " + JobParameters.getInternalReasonCodeDescription(internalStopReason2);
                    } else {
                        internalStopReason = -1;
                        debugReason = "couldn't figure out why the job should stop running";
                    }
                }
                JobSchedulerService.this.mConcurrencyManager.stopJobOnServiceContextLocked(job, job.getStopReason(), internalStopReason, debugReason);
            } else if (JobSchedulerService.this.mPendingJobQueue.remove(job)) {
                JobSchedulerService.this.noteJobNonPending(job);
            }
            JobSchedulerService.this.evaluateControllerStatesLocked(job);
        }

        void postProcessLocked() {
            if (this.unbatchedCount > 0 || this.forceBatchedCount >= JobSchedulerService.this.mConstants.MIN_READY_NON_ACTIVE_JOBS_COUNT) {
                if (JobSchedulerService.DEBUG) {
                    Slog.d(JobSchedulerService.TAG, "maybeQueueReadyJobsForExecutionLocked: Running jobs.");
                }
                JobSchedulerService.this.noteJobsPending(this.runnableJobs);
                JobSchedulerService.this.mPendingJobQueue.addAll(this.runnableJobs);
            } else if (JobSchedulerService.DEBUG) {
                Slog.d(JobSchedulerService.TAG, "maybeQueueReadyJobsForExecutionLocked: Not running anything.");
            }
            reset();
        }

        void reset() {
            this.forceBatchedCount = 0;
            this.unbatchedCount = 0;
            this.runnableJobs.clear();
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void maybeQueueReadyJobsForExecutionLocked() {
        this.mHandler.removeMessages(1);
        this.mHandler.removeMessages(8);
        this.mChangedJobList.clear();
        if (DEBUG) {
            Slog.d(TAG, "Maybe queuing ready jobs...");
        }
        clearPendingJobQueue();
        stopNonReadyActiveJobsLocked();
        this.mJobs.forEachJob(this.mMaybeQueueFunctor);
        this.mMaybeQueueFunctor.postProcessLocked();
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void checkChangedJobListLocked() {
        this.mHandler.removeMessages(8);
        if (DEBUG) {
            Slog.d(TAG, "Check changed jobs...");
        }
        if (this.mChangedJobList.size() == 0) {
            return;
        }
        this.mChangedJobList.forEach(this.mMaybeQueueFunctor);
        this.mMaybeQueueFunctor.postProcessLocked();
        this.mChangedJobList.clear();
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void updateMediaBackupExemptionLocked(final int userId, final String oldPkg, final String newPkg) {
        Predicate<JobStatus> shouldProcessJob = new Predicate() { // from class: com.android.server.job.JobSchedulerService$$ExternalSyntheticLambda0
            @Override // java.util.function.Predicate
            public final boolean test(Object obj) {
                return JobSchedulerService.lambda$updateMediaBackupExemptionLocked$3(userId, oldPkg, newPkg, (JobStatus) obj);
            }
        };
        this.mJobs.forEachJob(shouldProcessJob, new Consumer() { // from class: com.android.server.job.JobSchedulerService$$ExternalSyntheticLambda1
            @Override // java.util.function.Consumer
            public final void accept(Object obj) {
                JobSchedulerService.this.m4073xa4d48f88((JobStatus) obj);
            }
        });
        this.mHandler.sendEmptyMessage(8);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static /* synthetic */ boolean lambda$updateMediaBackupExemptionLocked$3(int userId, String oldPkg, String newPkg, JobStatus job) {
        return job.getSourceUserId() == userId && (job.getSourcePackageName().equals(oldPkg) || job.getSourcePackageName().equals(newPkg));
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$updateMediaBackupExemptionLocked$4$com-android-server-job-JobSchedulerService  reason: not valid java name */
    public /* synthetic */ void m4073xa4d48f88(JobStatus job) {
        if (job.updateMediaBackupExemptionStatus()) {
            this.mChangedJobList.add(job);
        }
    }

    public boolean areUsersStartedLocked(JobStatus job) {
        boolean sourceStarted = ArrayUtils.contains(this.mStartedUsers, job.getSourceUserId());
        if (job.getUserId() == job.getSourceUserId()) {
            return sourceStarted;
        }
        return sourceStarted && ArrayUtils.contains(this.mStartedUsers, job.getUserId());
    }

    boolean isReadyToBeExecutedLocked(JobStatus job) {
        return isReadyToBeExecutedLocked(job, true);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean isReadyToBeExecutedLocked(JobStatus job, boolean rejectActive) {
        boolean jobReady = job.isReady();
        boolean z = DEBUG;
        if (z) {
            Slog.v(TAG, "isReadyToBeExecutedLocked: " + job.toShortString() + " ready=" + jobReady);
        }
        if (!jobReady) {
            if (job.getSourcePackageName().equals("android.jobscheduler.cts.jobtestapp")) {
                Slog.v(TAG, "    NOT READY: " + job);
            }
            return false;
        }
        boolean jobExists = this.mJobs.containsJob(job);
        boolean userStarted = areUsersStartedLocked(job);
        boolean backingUp = this.mBackingUpUids.get(job.getSourceUid());
        if (z) {
            Slog.v(TAG, "isReadyToBeExecutedLocked: " + job.toShortString() + " exists=" + jobExists + " userStarted=" + userStarted + " backingUp=" + backingUp);
        }
        if (!jobExists || !userStarted || backingUp || checkIfRestricted(job) != null) {
            return false;
        }
        boolean jobPending = this.mPendingJobQueue.contains(job);
        boolean jobActive = rejectActive && this.mConcurrencyManager.isJobRunningLocked(job);
        if (z) {
            Slog.v(TAG, "isReadyToBeExecutedLocked: " + job.toShortString() + " pending=" + jobPending + " active=" + jobActive);
        }
        if (jobPending || jobActive) {
            return false;
        }
        return isComponentUsable(job);
    }

    private boolean isComponentUsable(JobStatus job) {
        ServiceInfo service = job.serviceInfo;
        if (service == null) {
            if (DEBUG) {
                Slog.v(TAG, "isComponentUsable: " + job.toShortString() + " component not present");
                return false;
            }
            return false;
        }
        boolean appIsBad = this.mActivityManagerInternal.isAppBad(service.processName, service.applicationInfo.uid);
        if (DEBUG && appIsBad) {
            Slog.i(TAG, "App is bad for " + job.toShortString() + " so not runnable");
        }
        return !appIsBad;
    }

    void evaluateControllerStatesLocked(JobStatus job) {
        for (int c = this.mControllers.size() - 1; c >= 0; c--) {
            StateController sc = this.mControllers.get(c);
            sc.evaluateStateLocked(job);
        }
    }

    public boolean areComponentsInPlaceLocked(JobStatus job) {
        boolean jobExists = this.mJobs.containsJob(job);
        boolean userStarted = areUsersStartedLocked(job);
        boolean backingUp = this.mBackingUpUids.get(job.getSourceUid());
        boolean z = DEBUG;
        if (z) {
            Slog.v(TAG, "areComponentsInPlaceLocked: " + job.toShortString() + " exists=" + jobExists + " userStarted=" + userStarted + " backingUp=" + backingUp);
        }
        if (!jobExists || !userStarted || backingUp) {
            return false;
        }
        JobRestriction restriction = checkIfRestricted(job);
        if (restriction != null) {
            if (z) {
                Slog.v(TAG, "areComponentsInPlaceLocked: " + job.toShortString() + " restricted due to " + restriction.getInternalReason());
            }
            return false;
        }
        return isComponentUsable(job);
    }

    public long getMinJobExecutionGuaranteeMs(JobStatus job) {
        long min;
        synchronized (this.mLock) {
            if (job.shouldTreatAsExpeditedJob()) {
                if (job.getEffectiveStandbyBucket() != 5) {
                    min = this.mConstants.RUNTIME_MIN_EJ_GUARANTEE_MS;
                } else {
                    min = Math.min(this.mConstants.RUNTIME_MIN_EJ_GUARANTEE_MS, (long) BackupAgentTimeoutParameters.DEFAULT_FULL_BACKUP_AGENT_TIMEOUT_MILLIS);
                }
                return min;
            } else if (job.getEffectivePriority() >= 400) {
                return this.mConstants.RUNTIME_MIN_HIGH_PRIORITY_GUARANTEE_MS;
            } else {
                return this.mConstants.RUNTIME_MIN_GUARANTEE_MS;
            }
        }
    }

    public long getMaxJobExecutionTimeMs(JobStatus job) {
        long maxJobExecutionTimeMsLocked;
        long min;
        synchronized (this.mLock) {
            long j = this.mConstants.RUNTIME_FREE_QUOTA_MAX_LIMIT_MS;
            if (this.mConstants.USE_TARE_POLICY) {
                maxJobExecutionTimeMsLocked = this.mTareController.getMaxJobExecutionTimeMsLocked(job);
            } else {
                maxJobExecutionTimeMsLocked = this.mQuotaController.getMaxJobExecutionTimeMsLocked(job);
            }
            min = Math.min(j, maxJobExecutionTimeMsLocked);
        }
        return min;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void maybeRunPendingJobsLocked() {
        if (DEBUG) {
            Slog.d(TAG, "pending queue: " + this.mPendingJobQueue.size() + " jobs.");
        }
        this.mConcurrencyManager.assignJobsToContextsLocked();
        reportActiveLocked();
    }

    private int adjustJobBias(int curBias, JobStatus job) {
        if (curBias < 40) {
            float factor = this.mJobPackageTracker.getLoadFactor(job);
            if (factor >= this.mConstants.HEAVY_USE_FACTOR) {
                return curBias - 80;
            }
            if (factor >= this.mConstants.MODERATE_USE_FACTOR) {
                return curBias - 40;
            }
            return curBias;
        }
        return curBias;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public int evaluateJobBiasLocked(JobStatus job) {
        int bias = job.getBias();
        if (bias >= 30) {
            return adjustJobBias(bias, job);
        }
        int override = this.mUidBiasOverride.get(job.getSourceUid(), 0);
        if (override != 0) {
            return adjustJobBias(override, job);
        }
        return adjustJobBias(bias, job);
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes.dex */
    public final class BatteryStateTracker extends BroadcastReceiver {
        private boolean mBatteryNotLow;
        private boolean mCharging;
        private int mLastBatterySeq = -1;
        private BroadcastReceiver mMonitor;

        BatteryStateTracker() {
        }

        public void startTracking() {
            IntentFilter filter = new IntentFilter();
            filter.addAction("android.intent.action.BATTERY_LOW");
            filter.addAction("android.intent.action.BATTERY_OKAY");
            filter.addAction("android.os.action.CHARGING");
            filter.addAction("android.os.action.DISCHARGING");
            JobSchedulerService.this.getTestableContext().registerReceiver(this, filter);
            BatteryManagerInternal batteryManagerInternal = (BatteryManagerInternal) LocalServices.getService(BatteryManagerInternal.class);
            this.mBatteryNotLow = !batteryManagerInternal.getBatteryLevelLow();
            this.mCharging = batteryManagerInternal.isPowered(15);
        }

        public void setMonitorBatteryLocked(boolean enabled) {
            if (enabled) {
                if (this.mMonitor == null) {
                    this.mMonitor = new BroadcastReceiver() { // from class: com.android.server.job.JobSchedulerService.BatteryStateTracker.1
                        @Override // android.content.BroadcastReceiver
                        public void onReceive(Context context, Intent intent) {
                            BatteryStateTracker.this.onReceiveInternal(intent);
                        }
                    };
                    IntentFilter filter = new IntentFilter();
                    filter.addAction("android.intent.action.BATTERY_CHANGED");
                    JobSchedulerService.this.getTestableContext().registerReceiver(this.mMonitor, filter);
                }
            } else if (this.mMonitor != null) {
                JobSchedulerService.this.getTestableContext().unregisterReceiver(this.mMonitor);
                this.mMonitor = null;
            }
        }

        public boolean isCharging() {
            return this.mCharging;
        }

        public boolean isBatteryNotLow() {
            return this.mBatteryNotLow;
        }

        public boolean isMonitoring() {
            return this.mMonitor != null;
        }

        public int getSeq() {
            return this.mLastBatterySeq;
        }

        @Override // android.content.BroadcastReceiver
        public void onReceive(Context context, Intent intent) {
            onReceiveInternal(intent);
        }

        public void onReceiveInternal(Intent intent) {
            synchronized (JobSchedulerService.this.mLock) {
                String action = intent.getAction();
                boolean changed = false;
                if ("android.intent.action.BATTERY_LOW".equals(action)) {
                    if (JobSchedulerService.DEBUG) {
                        Slog.d(JobSchedulerService.TAG, "Battery life too low @ " + JobSchedulerService.sElapsedRealtimeClock.millis());
                    }
                    if (this.mBatteryNotLow) {
                        this.mBatteryNotLow = false;
                        changed = true;
                    }
                } else if ("android.intent.action.BATTERY_OKAY".equals(action)) {
                    if (JobSchedulerService.DEBUG) {
                        Slog.d(JobSchedulerService.TAG, "Battery high enough @ " + JobSchedulerService.sElapsedRealtimeClock.millis());
                    }
                    if (!this.mBatteryNotLow) {
                        this.mBatteryNotLow = true;
                        changed = true;
                    }
                } else if ("android.os.action.CHARGING".equals(action)) {
                    if (JobSchedulerService.DEBUG) {
                        Slog.d(JobSchedulerService.TAG, "Battery charging @ " + JobSchedulerService.sElapsedRealtimeClock.millis());
                    }
                    if (!this.mCharging) {
                        this.mCharging = true;
                        changed = true;
                    }
                } else if ("android.os.action.DISCHARGING".equals(action)) {
                    if (JobSchedulerService.DEBUG) {
                        Slog.d(JobSchedulerService.TAG, "Battery discharging @ " + JobSchedulerService.sElapsedRealtimeClock.millis());
                    }
                    if (this.mCharging) {
                        this.mCharging = false;
                        changed = true;
                    }
                }
                this.mLastBatterySeq = intent.getIntExtra(DeviceStorageMonitorService.EXTRA_SEQUENCE, this.mLastBatterySeq);
                if (changed) {
                    for (int c = JobSchedulerService.this.mControllers.size() - 1; c >= 0; c--) {
                        JobSchedulerService.this.mControllers.get(c).onBatteryStateChangedLocked();
                    }
                }
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* loaded from: classes.dex */
    public final class LocalService implements JobSchedulerInternal {
        LocalService() {
        }

        public List<JobInfo> getSystemScheduledPendingJobs() {
            final List<JobInfo> pendingJobs;
            synchronized (JobSchedulerService.this.mLock) {
                pendingJobs = new ArrayList<>();
                JobSchedulerService.this.mJobs.forEachJob(1000, new Consumer() { // from class: com.android.server.job.JobSchedulerService$LocalService$$ExternalSyntheticLambda0
                    @Override // java.util.function.Consumer
                    public final void accept(Object obj) {
                        JobSchedulerService.LocalService.this.m4083x3e42f17(pendingJobs, (JobStatus) obj);
                    }
                });
            }
            return pendingJobs;
        }

        /* JADX INFO: Access modifiers changed from: package-private */
        /* renamed from: lambda$getSystemScheduledPendingJobs$0$com-android-server-job-JobSchedulerService$LocalService  reason: not valid java name */
        public /* synthetic */ void m4083x3e42f17(List pendingJobs, JobStatus job) {
            if (job.getJob().isPeriodic() || !JobSchedulerService.this.mConcurrencyManager.isJobRunningLocked(job)) {
                pendingJobs.add(job.getJob());
            }
        }

        public void cancelJobsForUid(int uid, int reason, int internalReasonCode, String debugReason) {
            JobSchedulerService.this.cancelJobsForUid(uid, reason, internalReasonCode, debugReason);
        }

        public void addBackingUpUid(int uid) {
            synchronized (JobSchedulerService.this.mLock) {
                JobSchedulerService.this.mBackingUpUids.put(uid, true);
            }
        }

        public void removeBackingUpUid(int uid) {
            synchronized (JobSchedulerService.this.mLock) {
                JobSchedulerService.this.mBackingUpUids.delete(uid);
                if (JobSchedulerService.this.mJobs.countJobsForUid(uid) > 0) {
                    JobSchedulerService.this.mHandler.obtainMessage(1).sendToTarget();
                }
            }
        }

        public void clearAllBackingUpUids() {
            synchronized (JobSchedulerService.this.mLock) {
                if (JobSchedulerService.this.mBackingUpUids.size() > 0) {
                    JobSchedulerService.this.mBackingUpUids.clear();
                    JobSchedulerService.this.mHandler.obtainMessage(1).sendToTarget();
                }
            }
        }

        public String getCloudMediaProviderPackage(int userId) {
            return (String) JobSchedulerService.this.mCloudMediaProviderPackages.get(userId);
        }

        public void reportAppUsage(String packageName, int userId) {
            JobSchedulerService.this.reportAppUsage(packageName, userId);
        }

        public JobSchedulerInternal.JobStorePersistStats getPersistStats() {
            JobSchedulerInternal.JobStorePersistStats jobStorePersistStats;
            synchronized (JobSchedulerService.this.mLock) {
                jobStorePersistStats = new JobSchedulerInternal.JobStorePersistStats(JobSchedulerService.this.mJobs.getPersistStats());
            }
            return jobStorePersistStats;
        }
    }

    /* loaded from: classes.dex */
    final class StandbyTracker extends AppStandbyInternal.AppIdleStateChangeListener {
        StandbyTracker() {
        }

        public void onAppIdleStateChanged(String packageName, int userId, boolean idle, int bucket, int reason) {
        }

        public void onUserInteractionStarted(String packageName, int userId) {
            int uid = JobSchedulerService.this.mLocalPM.getPackageUid(packageName, 8192L, userId);
            if (uid < 0) {
                return;
            }
            long sinceLast = JobSchedulerService.this.mUsageStats.getTimeSinceLastJobRun(packageName, userId);
            if (sinceLast > 172800000) {
                sinceLast = 0;
            }
            DeferredJobCounter counter = new DeferredJobCounter();
            synchronized (JobSchedulerService.this.mLock) {
                JobSchedulerService.this.mJobs.forEachJobForSourceUid(uid, counter);
            }
            if (counter.numDeferred() > 0 || sinceLast > 0) {
                BatteryStatsInternal mBatteryStatsInternal = (BatteryStatsInternal) LocalServices.getService(BatteryStatsInternal.class);
                mBatteryStatsInternal.noteJobsDeferred(uid, counter.numDeferred(), sinceLast);
                FrameworkStatsLog.write_non_chained(85, uid, (String) null, counter.numDeferred(), sinceLast);
            }
        }
    }

    /* loaded from: classes.dex */
    static class DeferredJobCounter implements Consumer<JobStatus> {
        private int mDeferred = 0;

        DeferredJobCounter() {
        }

        public int numDeferred() {
            return this.mDeferred;
        }

        /* JADX DEBUG: Method merged with bridge method */
        @Override // java.util.function.Consumer
        public void accept(JobStatus job) {
            if (job.getWhenStandbyDeferred() > 0) {
                this.mDeferred++;
            }
        }
    }

    public static int standbyBucketToBucketIndex(int bucket) {
        if (bucket == 50) {
            return 4;
        }
        if (bucket > 40) {
            return 5;
        }
        if (bucket > 30) {
            return 3;
        }
        if (bucket > 20) {
            return 2;
        }
        if (bucket > 10) {
            return 1;
        }
        if (bucket > 5) {
            return 0;
        }
        return 6;
    }

    public static int standbyBucketForPackage(String packageName, int userId, long elapsedNow) {
        int bucket;
        UsageStatsManagerInternal usageStats = (UsageStatsManagerInternal) LocalServices.getService(UsageStatsManagerInternal.class);
        if (usageStats != null) {
            bucket = usageStats.getAppStandbyBucket(packageName, userId, elapsedNow);
        } else {
            bucket = 0;
        }
        int bucket2 = standbyBucketToBucketIndex(bucket);
        if (DEBUG_STANDBY) {
            Slog.v(TAG, packageName + SliceClientPermissions.SliceAuthority.DELIMITER + userId + " standby bucket index: " + bucket2);
        }
        return bucket2;
    }

    /* loaded from: classes.dex */
    private class CloudProviderChangeListener implements StorageManagerInternal.CloudProviderChangeListener {
        private CloudProviderChangeListener() {
        }

        public void onCloudProviderChanged(int userId, String authority) {
            PackageManager pm = JobSchedulerService.this.getContext().createContextAsUser(UserHandle.of(userId), 0).getPackageManager();
            ProviderInfo pi = pm.resolveContentProvider(authority, PackageManager.ComponentInfoFlags.of(0L));
            String newPkg = pi == null ? null : pi.packageName;
            synchronized (JobSchedulerService.this.mLock) {
                String oldPkg = (String) JobSchedulerService.this.mCloudMediaProviderPackages.get(userId);
                if (!Objects.equals(oldPkg, newPkg)) {
                    if (JobSchedulerService.DEBUG) {
                        Slog.d(JobSchedulerService.TAG, "Cloud provider of user " + userId + " changed from " + oldPkg + " to " + newPkg);
                    }
                    JobSchedulerService.this.mCloudMediaProviderPackages.put(userId, newPkg);
                    SomeArgs args = SomeArgs.obtain();
                    args.argi1 = userId;
                    args.arg1 = oldPkg;
                    args.arg2 = newPkg;
                    JobSchedulerService.this.mHandler.obtainMessage(9, args).sendToTarget();
                }
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* loaded from: classes.dex */
    public final class JobSchedulerStub extends IJobScheduler.Stub {
        private final SparseArray<Boolean> mPersistCache = new SparseArray<>();

        JobSchedulerStub() {
        }

        private void enforceValidJobRequest(int uid, JobInfo job) {
            PackageManager pm = JobSchedulerService.this.getContext().createContextAsUser(UserHandle.getUserHandleForUid(uid), 0).getPackageManager();
            ComponentName service = job.getService();
            try {
                ServiceInfo si = pm.getServiceInfo(service, 786432);
                if (si == null) {
                    throw new IllegalArgumentException("No such service " + service);
                }
                if (si.applicationInfo.uid != uid) {
                    throw new IllegalArgumentException("uid " + uid + " cannot schedule job in " + service.getPackageName());
                }
                if (!"android.permission.BIND_JOB_SERVICE".equals(si.permission)) {
                    throw new IllegalArgumentException("Scheduled service " + service + " does not require android.permission.BIND_JOB_SERVICE permission");
                }
            } catch (PackageManager.NameNotFoundException e) {
                throw new IllegalArgumentException("Tried to schedule job for non-existent component: " + service);
            }
        }

        private boolean canPersistJobs(int pid, int uid) {
            boolean canPersist;
            synchronized (this.mPersistCache) {
                Boolean cached = this.mPersistCache.get(uid);
                if (cached != null) {
                    canPersist = cached.booleanValue();
                } else {
                    int result = JobSchedulerService.this.getContext().checkPermission("android.permission.RECEIVE_BOOT_COMPLETED", pid, uid);
                    boolean canPersist2 = result == 0;
                    this.mPersistCache.put(uid, Boolean.valueOf(canPersist2));
                    canPersist = canPersist2;
                }
            }
            return canPersist;
        }

        private void validateJobFlags(JobInfo job, int callingUid) {
            job.enforceValidity(CompatChanges.isChangeEnabled(194532703L, callingUid));
            if ((job.getFlags() & 1) != 0) {
                JobSchedulerService.this.getContext().enforceCallingOrSelfPermission("android.permission.CONNECTIVITY_INTERNAL", JobSchedulerService.TAG);
            }
            if ((job.getFlags() & 8) != 0) {
                if (callingUid != 1000) {
                    throw new SecurityException("Job has invalid flags");
                }
                if (job.isPeriodic()) {
                    Slog.wtf(JobSchedulerService.TAG, "Periodic jobs mustn't have FLAG_EXEMPT_FROM_APP_STANDBY. Job=" + job);
                }
            }
        }

        public int schedule(JobInfo job) throws RemoteException {
            if (JobSchedulerService.DEBUG) {
                Slog.d(JobSchedulerService.TAG, "Scheduling job: " + job.toString());
            }
            int pid = Binder.getCallingPid();
            int uid = Binder.getCallingUid();
            int userId = UserHandle.getUserId(uid);
            enforceValidJobRequest(uid, job);
            if (job.isPersisted() && !canPersistJobs(pid, uid)) {
                throw new IllegalArgumentException("Error: requested job be persisted without holding RECEIVE_BOOT_COMPLETED permission.");
            }
            validateJobFlags(job, uid);
            long ident = Binder.clearCallingIdentity();
            try {
                return JobSchedulerService.this.scheduleAsPackage(job, null, uid, null, userId, null);
            } finally {
                Binder.restoreCallingIdentity(ident);
            }
        }

        public int enqueue(JobInfo job, JobWorkItem work) throws RemoteException {
            if (JobSchedulerService.DEBUG) {
                Slog.d(JobSchedulerService.TAG, "Enqueueing job: " + job.toString() + " work: " + work);
            }
            int uid = Binder.getCallingUid();
            int userId = UserHandle.getUserId(uid);
            enforceValidJobRequest(uid, job);
            if (job.isPersisted()) {
                throw new IllegalArgumentException("Can't enqueue work for persisted jobs");
            }
            if (work == null) {
                throw new NullPointerException("work is null");
            }
            work.enforceValidity();
            validateJobFlags(job, uid);
            long ident = Binder.clearCallingIdentity();
            try {
                return JobSchedulerService.this.scheduleAsPackage(job, work, uid, null, userId, null);
            } finally {
                Binder.restoreCallingIdentity(ident);
            }
        }

        public int scheduleAsPackage(JobInfo job, String packageName, int userId, String tag) throws RemoteException {
            int callerUid = Binder.getCallingUid();
            if (JobSchedulerService.DEBUG) {
                Slog.d(JobSchedulerService.TAG, "Caller uid " + callerUid + " scheduling job: " + job.toString() + " on behalf of " + packageName + SliceClientPermissions.SliceAuthority.DELIMITER);
            }
            if (packageName == null) {
                throw new NullPointerException("Must specify a package for scheduleAsPackage()");
            }
            int mayScheduleForOthers = JobSchedulerService.this.getContext().checkCallingOrSelfPermission("android.permission.UPDATE_DEVICE_STATS");
            if (mayScheduleForOthers != 0) {
                throw new SecurityException("Caller uid " + callerUid + " not permitted to schedule jobs for other apps");
            }
            validateJobFlags(job, callerUid);
            long ident = Binder.clearCallingIdentity();
            try {
                return JobSchedulerService.this.scheduleAsPackage(job, null, callerUid, packageName, userId, tag);
            } finally {
                Binder.restoreCallingIdentity(ident);
            }
        }

        public ParceledListSlice<JobInfo> getAllPendingJobs() throws RemoteException {
            int uid = Binder.getCallingUid();
            long ident = Binder.clearCallingIdentity();
            try {
                return new ParceledListSlice<>(JobSchedulerService.this.getPendingJobs(uid));
            } finally {
                Binder.restoreCallingIdentity(ident);
            }
        }

        public JobInfo getPendingJob(int jobId) throws RemoteException {
            int uid = Binder.getCallingUid();
            long ident = Binder.clearCallingIdentity();
            try {
                return JobSchedulerService.this.getPendingJob(uid, jobId);
            } finally {
                Binder.restoreCallingIdentity(ident);
            }
        }

        public void cancelAll() throws RemoteException {
            int uid = Binder.getCallingUid();
            long ident = Binder.clearCallingIdentity();
            try {
                JobSchedulerService.this.cancelJobsForUid(uid, 1, 0, "cancelAll() called by app, callingUid=" + uid);
            } finally {
                Binder.restoreCallingIdentity(ident);
            }
        }

        public void cancel(int jobId) throws RemoteException {
            int uid = Binder.getCallingUid();
            long ident = Binder.clearCallingIdentity();
            try {
                JobSchedulerService.this.cancelJob(uid, jobId, uid, 1);
            } finally {
                Binder.restoreCallingIdentity(ident);
            }
        }

        public void dump(FileDescriptor fd, PrintWriter pw, String[] args) {
            if (DumpUtils.checkDumpAndUsageStatsPermission(JobSchedulerService.this.getContext(), JobSchedulerService.TAG, pw)) {
                int filterUid = -1;
                boolean proto = false;
                if (!ArrayUtils.isEmpty(args)) {
                    int opti = 0;
                    while (true) {
                        if (opti >= args.length) {
                            break;
                        }
                        String arg = args[opti];
                        if ("-h".equals(arg)) {
                            JobSchedulerService.dumpHelp(pw);
                            return;
                        }
                        if (!"-a".equals(arg)) {
                            if ("--proto".equals(arg)) {
                                proto = true;
                            } else if (arg.length() > 0 && arg.charAt(0) == '-') {
                                pw.println("Unknown option: " + arg);
                                return;
                            }
                        }
                        opti++;
                    }
                    if (opti < args.length) {
                        String pkg = args[opti];
                        try {
                            filterUid = JobSchedulerService.this.getContext().getPackageManager().getPackageUid(pkg, 4194304);
                        } catch (PackageManager.NameNotFoundException e) {
                            pw.println("Invalid package: " + pkg);
                            return;
                        }
                    }
                }
                long identityToken = Binder.clearCallingIdentity();
                try {
                    if (proto) {
                        JobSchedulerService.this.dumpInternalProto(fd, filterUid);
                    } else {
                        JobSchedulerService.this.dumpInternal(new IndentingPrintWriter(pw, "  "), filterUid);
                    }
                } finally {
                    Binder.restoreCallingIdentity(identityToken);
                }
            }
        }

        /* JADX DEBUG: Multi-variable search result rejected for r6v0, resolved type: com.android.server.job.JobSchedulerService$JobSchedulerStub */
        /* JADX WARN: Multi-variable type inference failed */
        public int handleShellCommand(ParcelFileDescriptor in, ParcelFileDescriptor out, ParcelFileDescriptor err, String[] args) {
            return new JobSchedulerShellCommand(JobSchedulerService.this).exec(this, in.getFileDescriptor(), out.getFileDescriptor(), err.getFileDescriptor(), args);
        }

        public List<JobInfo> getStartedJobs() {
            ArrayList<JobInfo> runningJobs;
            int uid = Binder.getCallingUid();
            if (uid != 1000) {
                throw new SecurityException("getStartedJobs() is system internal use only.");
            }
            synchronized (JobSchedulerService.this.mLock) {
                ArraySet<JobStatus> runningJobStatuses = JobSchedulerService.this.mConcurrencyManager.getRunningJobsLocked();
                runningJobs = new ArrayList<>(runningJobStatuses.size());
                for (int i = runningJobStatuses.size() - 1; i >= 0; i--) {
                    JobStatus job = runningJobStatuses.valueAt(i);
                    if (job != null) {
                        runningJobs.add(job.getJob());
                    }
                }
            }
            return runningJobs;
        }

        public ParceledListSlice<JobSnapshot> getAllJobSnapshots() {
            ParceledListSlice<JobSnapshot> parceledListSlice;
            int uid = Binder.getCallingUid();
            if (uid != 1000) {
                throw new SecurityException("getAllJobSnapshots() is system internal use only.");
            }
            synchronized (JobSchedulerService.this.mLock) {
                final ArrayList<JobSnapshot> snapshots = new ArrayList<>(JobSchedulerService.this.mJobs.size());
                JobSchedulerService.this.mJobs.forEachJob(new Consumer() { // from class: com.android.server.job.JobSchedulerService$JobSchedulerStub$$ExternalSyntheticLambda0
                    @Override // java.util.function.Consumer
                    public final void accept(Object obj) {
                        JobSchedulerService.JobSchedulerStub.this.m4082x68d4f94b(snapshots, (JobStatus) obj);
                    }
                });
                parceledListSlice = new ParceledListSlice<>(snapshots);
            }
            return parceledListSlice;
        }

        /* JADX INFO: Access modifiers changed from: package-private */
        /* renamed from: lambda$getAllJobSnapshots$0$com-android-server-job-JobSchedulerService$JobSchedulerStub  reason: not valid java name */
        public /* synthetic */ void m4082x68d4f94b(ArrayList snapshots, JobStatus job) {
            snapshots.add(new JobSnapshot(job.getJob(), job.getSatisfiedConstraintFlags(), JobSchedulerService.this.isReadyToBeExecutedLocked(job)));
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public int executeRunCommand(String pkgName, int userId, int jobId, boolean satisfied, boolean force) {
        int uid;
        int i;
        Slog.d(TAG, "executeRunCommand(): " + pkgName + SliceClientPermissions.SliceAuthority.DELIMITER + userId + " " + jobId + " s=" + satisfied + " f=" + force);
        try {
            uid = AppGlobals.getPackageManager().getPackageUid(pkgName, 0L, userId != -1 ? userId : 0);
        } catch (RemoteException e) {
        }
        if (uid < 0) {
            return -1000;
        }
        synchronized (this.mLock) {
            JobStatus js = this.mJobs.getJobByUidAndJobId(uid, jobId);
            if (js == null) {
                return JobSchedulerShellCommand.CMD_ERR_NO_JOB;
            }
            if (force) {
                i = 3;
            } else {
                i = satisfied ? 1 : 2;
            }
            js.overrideState = i;
            for (int c = this.mControllers.size() - 1; c >= 0; c--) {
                this.mControllers.get(c).reevaluateStateLocked(uid);
            }
            if (!js.isConstraintsSatisfied()) {
                js.overrideState = 0;
                return JobSchedulerShellCommand.CMD_ERR_CONSTRAINTS;
            }
            queueReadyJobsForExecutionLocked();
            maybeRunPendingJobsLocked();
            return 0;
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public int executeTimeoutCommand(PrintWriter pw, String pkgName, int userId, boolean hasJobId, int jobId) {
        if (DEBUG) {
            Slog.v(TAG, "executeTimeoutCommand(): " + pkgName + SliceClientPermissions.SliceAuthority.DELIMITER + userId + " " + jobId);
        }
        synchronized (this.mLock) {
            boolean foundSome = this.mConcurrencyManager.executeTimeoutCommandLocked(pw, pkgName, userId, hasJobId, jobId);
            if (!foundSome) {
                pw.println("No matching executing jobs found.");
            }
        }
        return 0;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public int executeCancelCommand(PrintWriter pw, String pkgName, int userId, boolean hasJobId, int jobId) {
        if (DEBUG) {
            Slog.v(TAG, "executeCancelCommand(): " + pkgName + SliceClientPermissions.SliceAuthority.DELIMITER + userId + " " + jobId);
        }
        int pkgUid = -1;
        try {
            IPackageManager pm = AppGlobals.getPackageManager();
            pkgUid = pm.getPackageUid(pkgName, 0L, userId);
        } catch (RemoteException e) {
        }
        if (pkgUid < 0) {
            pw.println("Package " + pkgName + " not found.");
            return -1000;
        }
        if (!hasJobId) {
            pw.println("Canceling all jobs for " + pkgName + " in user " + userId);
            if (!cancelJobsForUid(pkgUid, 13, 0, "cancel shell command for package")) {
                pw.println("No matching jobs found.");
            }
        } else {
            pw.println("Canceling job " + pkgName + "/#" + jobId + " in user " + userId);
            if (!cancelJob(pkgUid, jobId, 2000, 13)) {
                pw.println("No matching job found.");
            }
        }
        return 0;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void setMonitorBattery(boolean enabled) {
        synchronized (this.mLock) {
            this.mBatteryStateTracker.setMonitorBatteryLocked(enabled);
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public int getBatterySeq() {
        int seq;
        synchronized (this.mLock) {
            seq = this.mBatteryStateTracker.getSeq();
        }
        return seq;
    }

    public boolean isBatteryCharging() {
        boolean isCharging;
        synchronized (this.mLock) {
            isCharging = this.mBatteryStateTracker.isCharging();
        }
        return isCharging;
    }

    public boolean isBatteryNotLow() {
        boolean isBatteryNotLow;
        synchronized (this.mLock) {
            isBatteryNotLow = this.mBatteryStateTracker.isBatteryNotLow();
        }
        return isBatteryNotLow;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public int getStorageSeq() {
        int seq;
        synchronized (this.mLock) {
            StorageController storageController = this.mStorageController;
            seq = storageController != null ? storageController.getTracker().getSeq() : -1;
        }
        return seq;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean getStorageNotLow() {
        boolean isStorageNotLow;
        synchronized (this.mLock) {
            StorageController storageController = this.mStorageController;
            isStorageNotLow = storageController != null ? storageController.getTracker().isStorageNotLow() : false;
        }
        return isStorageNotLow;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public int getJobState(PrintWriter pw, String pkgName, int userId, int jobId) {
        int uid;
        try {
            uid = AppGlobals.getPackageManager().getPackageUid(pkgName, 0L, userId != -1 ? userId : 0);
        } catch (RemoteException e) {
        }
        if (uid < 0) {
            pw.print("unknown(");
            pw.print(pkgName);
            pw.println(")");
            return -1000;
        }
        synchronized (this.mLock) {
            JobStatus js = this.mJobs.getJobByUidAndJobId(uid, jobId);
            if (DEBUG) {
                Slog.d(TAG, "get-job-state " + uid + SliceClientPermissions.SliceAuthority.DELIMITER + jobId + ": " + js);
            }
            if (js == null) {
                pw.print("unknown(");
                UserHandle.formatUid(pw, uid);
                pw.print("/jid");
                pw.print(jobId);
                pw.println(")");
                return JobSchedulerShellCommand.CMD_ERR_NO_JOB;
            }
            boolean printed = false;
            if (this.mPendingJobQueue.contains(js)) {
                pw.print("pending");
                printed = true;
            }
            if (this.mConcurrencyManager.isJobRunningLocked(js)) {
                if (printed) {
                    pw.print(" ");
                }
                printed = true;
                pw.println(DomainVerificationPersistence.TAG_ACTIVE);
            }
            if (!ArrayUtils.contains(this.mStartedUsers, js.getUserId())) {
                if (printed) {
                    pw.print(" ");
                }
                printed = true;
                pw.println("user-stopped");
            }
            if (!ArrayUtils.contains(this.mStartedUsers, js.getSourceUserId())) {
                if (printed) {
                    pw.print(" ");
                }
                printed = true;
                pw.println("source-user-stopped");
            }
            if (this.mBackingUpUids.get(js.getSourceUid())) {
                if (printed) {
                    pw.print(" ");
                }
                printed = true;
                pw.println("backing-up");
            }
            boolean componentPresent = false;
            try {
                componentPresent = AppGlobals.getPackageManager().getServiceInfo(js.getServiceComponent(), 268435456L, js.getUserId()) != null;
            } catch (RemoteException e2) {
            }
            if (!componentPresent) {
                if (printed) {
                    pw.print(" ");
                }
                printed = true;
                pw.println("no-component");
            }
            if (js.isReady()) {
                if (printed) {
                    pw.print(" ");
                }
                printed = true;
                pw.println("ready");
            }
            if (!printed) {
                pw.print("waiting");
            }
            pw.println();
            return 0;
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void resetExecutionQuota(String pkgName, int userId) {
        synchronized (this.mLock) {
            this.mQuotaController.clearAppStatsLocked(userId, pkgName);
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void resetScheduleQuota() {
        this.mQuotaTracker.clear();
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void triggerDockState(boolean idleState) {
        Intent dockIntent;
        if (idleState) {
            dockIntent = new Intent("android.intent.action.DOCK_IDLE");
        } else {
            dockIntent = new Intent("android.intent.action.DOCK_ACTIVE");
        }
        dockIntent.setPackage(PackageManagerService.PLATFORM_PACKAGE_NAME);
        dockIntent.addFlags(1342177280);
        getContext().sendBroadcastAsUser(dockIntent, UserHandle.ALL);
    }

    static void dumpHelp(PrintWriter pw) {
        pw.println("Job Scheduler (jobscheduler) dump options:");
        pw.println("  [-h] [package] ...");
        pw.println("    -h: print this help");
        pw.println("  [package] is an optional package name to limit the output to.");
    }

    private static void sortJobs(List<JobStatus> jobs) {
        Collections.sort(jobs, new Comparator<JobStatus>() { // from class: com.android.server.job.JobSchedulerService.6
            /* JADX DEBUG: Method merged with bridge method */
            @Override // java.util.Comparator
            public int compare(JobStatus o1, JobStatus o2) {
                int uid1 = o1.getUid();
                int uid2 = o2.getUid();
                int id1 = o1.getJobId();
                int id2 = o2.getJobId();
                if (uid1 != uid2) {
                    return uid1 < uid2 ? -1 : 1;
                } else if (id1 < id2) {
                    return -1;
                } else {
                    return id1 > id2 ? 1 : 0;
                }
            }
        });
    }

    @NeverCompile
    void dumpInternal(IndentingPrintWriter pw, int filterUid) {
        Object obj;
        boolean z;
        boolean jobPrinted;
        boolean z2;
        final int filterAppId = UserHandle.getAppId(filterUid);
        long now = sSystemClock.millis();
        long nowElapsed = sElapsedRealtimeClock.millis();
        long nowUptime = sUptimeMillisClock.millis();
        Predicate<JobStatus> predicate = new Predicate() { // from class: com.android.server.job.JobSchedulerService$$ExternalSyntheticLambda6
            @Override // java.util.function.Predicate
            public final boolean test(Object obj2) {
                return JobSchedulerService.lambda$dumpInternal$5(filterAppId, (JobStatus) obj2);
            }
        };
        Object obj2 = this.mLock;
        synchronized (obj2) {
            try {
                try {
                    this.mConstants.dump(pw);
                    for (StateController controller : this.mControllers) {
                        try {
                            pw.increaseIndent();
                            controller.dumpConstants(pw);
                            pw.decreaseIndent();
                        } catch (Throwable th) {
                            th = th;
                            obj = obj2;
                            throw th;
                        }
                    }
                    pw.println();
                    boolean z3 = true;
                    for (int i = this.mJobRestrictions.size() - 1; i >= 0; i--) {
                        this.mJobRestrictions.get(i).dumpConstants(pw);
                    }
                    pw.println();
                    this.mQuotaTracker.dump(pw);
                    pw.println();
                    pw.print("Battery charging: ");
                    pw.println(this.mBatteryStateTracker.isCharging());
                    pw.print("Battery not low: ");
                    pw.println(this.mBatteryStateTracker.isBatteryNotLow());
                    if (this.mBatteryStateTracker.isMonitoring()) {
                        pw.print("MONITORING: seq=");
                        pw.println(this.mBatteryStateTracker.getSeq());
                    }
                    pw.println();
                    pw.println("Started users: " + Arrays.toString(this.mStartedUsers));
                    pw.println();
                    pw.print("Media Cloud Providers: ");
                    pw.println(this.mCloudMediaProviderPackages);
                    pw.println();
                    pw.print("Registered ");
                    pw.print(this.mJobs.size());
                    pw.println(" jobs:");
                    pw.increaseIndent();
                    boolean jobPrinted2 = false;
                    if (this.mJobs.size() <= 0) {
                        z = true;
                    } else {
                        List<JobStatus> jobs = this.mJobs.mJobSet.getAllJobs();
                        sortJobs(jobs);
                        for (JobStatus job : jobs) {
                            if (predicate.test(job)) {
                                boolean jobPrinted3 = true;
                                pw.print("JOB #");
                                job.printUniqueId(pw);
                                pw.print(": ");
                                pw.println(job.toShortStringExceptUniqueId());
                                pw.increaseIndent();
                                job.dump(pw, z3, nowElapsed);
                                pw.print("Restricted due to:");
                                boolean isRestricted = checkIfRestricted(job) != null ? z3 : false;
                                if (isRestricted) {
                                    z2 = true;
                                    int i2 = this.mJobRestrictions.size() - 1;
                                    while (i2 >= 0) {
                                        boolean jobPrinted4 = jobPrinted3;
                                        JobRestriction restriction = this.mJobRestrictions.get(i2);
                                        if (restriction.isJobRestricted(job)) {
                                            int reason = restriction.getInternalReason();
                                            pw.print(" ");
                                            pw.print(JobParameters.getInternalReasonCodeDescription(reason));
                                        }
                                        i2--;
                                        jobPrinted3 = jobPrinted4;
                                    }
                                    jobPrinted = jobPrinted3;
                                } else {
                                    jobPrinted = true;
                                    z2 = z3;
                                    pw.print(" none");
                                }
                                pw.println(".");
                                pw.print("Ready: ");
                                pw.print(isReadyToBeExecutedLocked(job));
                                pw.print(" (job=");
                                pw.print(job.isReady());
                                pw.print(" user=");
                                pw.print(areUsersStartedLocked(job));
                                pw.print(" !restricted=");
                                pw.print(!isRestricted ? z2 : false);
                                pw.print(" !pending=");
                                pw.print(!this.mPendingJobQueue.contains(job) ? z2 : false);
                                pw.print(" !active=");
                                pw.print(!this.mConcurrencyManager.isJobRunningLocked(job) ? z2 : false);
                                pw.print(" !backingup=");
                                pw.print(!this.mBackingUpUids.get(job.getSourceUid()) ? z2 : false);
                                pw.print(" comp=");
                                pw.print(isComponentUsable(job));
                                pw.println(")");
                                pw.decreaseIndent();
                                z3 = z2;
                                jobPrinted2 = jobPrinted;
                            }
                        }
                        z = z3;
                    }
                    if (!jobPrinted2) {
                        pw.println("None.");
                    }
                    pw.decreaseIndent();
                    for (int i3 = 0; i3 < this.mControllers.size(); i3++) {
                        pw.println();
                        pw.println(this.mControllers.get(i3).getClass().getSimpleName() + ":");
                        pw.increaseIndent();
                        this.mControllers.get(i3).dumpControllerStateLocked(pw, predicate);
                        pw.decreaseIndent();
                    }
                    boolean overridePrinted = false;
                    for (int i4 = 0; i4 < this.mUidBiasOverride.size(); i4++) {
                        int uid = this.mUidBiasOverride.keyAt(i4);
                        if (filterAppId == -1 || filterAppId == UserHandle.getAppId(uid)) {
                            if (!overridePrinted) {
                                overridePrinted = true;
                                pw.println();
                                pw.println("Uid bias overrides:");
                                pw.increaseIndent();
                            }
                            pw.print(UserHandle.formatUid(uid));
                            pw.print(": ");
                            pw.println(this.mUidBiasOverride.valueAt(i4));
                        }
                    }
                    if (overridePrinted) {
                        pw.decreaseIndent();
                    }
                    boolean uidMapPrinted = false;
                    for (int i5 = 0; i5 < this.mUidToPackageCache.size(); i5++) {
                        int uid2 = this.mUidToPackageCache.keyAt(i5);
                        if (filterUid == -1 || filterUid == uid2) {
                            if (!uidMapPrinted) {
                                uidMapPrinted = true;
                                pw.println();
                                pw.println("Cached UID->package map:");
                                pw.increaseIndent();
                            }
                            pw.print(uid2);
                            pw.print(": ");
                            pw.println(this.mUidToPackageCache.get(uid2));
                        }
                    }
                    if (uidMapPrinted) {
                        pw.decreaseIndent();
                    }
                    boolean backingPrinted = false;
                    for (int i6 = 0; i6 < this.mBackingUpUids.size(); i6++) {
                        int uid3 = this.mBackingUpUids.keyAt(i6);
                        if (filterAppId == -1 || filterAppId == UserHandle.getAppId(uid3)) {
                            if (!backingPrinted) {
                                pw.println();
                                pw.println("Backing up uids:");
                                pw.increaseIndent();
                                backingPrinted = true;
                            } else {
                                pw.print(", ");
                            }
                            pw.print(UserHandle.formatUid(uid3));
                        }
                    }
                    if (backingPrinted) {
                        pw.decreaseIndent();
                        pw.println();
                    }
                    pw.println();
                    this.mJobPackageTracker.dump(pw, filterAppId);
                    pw.println();
                    if (this.mJobPackageTracker.dumpHistory(pw, filterAppId)) {
                        pw.println();
                    }
                    pw.println("Pending queue:");
                    pw.increaseIndent();
                    this.mPendingJobQueue.resetIterator();
                    boolean pendingPrinted = false;
                    int pendingIdx = 0;
                    while (true) {
                        JobStatus job2 = this.mPendingJobQueue.next();
                        if (job2 == null) {
                            break;
                        }
                        int pendingIdx2 = pendingIdx + 1;
                        if (!predicate.test(job2)) {
                            pendingIdx = pendingIdx2;
                        } else {
                            if (!pendingPrinted) {
                                pendingPrinted = true;
                            }
                            pw.print("Pending #");
                            pw.print(pendingIdx2);
                            pw.print(": ");
                            pw.println(job2.toShortString());
                            pw.increaseIndent();
                            job2.dump(pw, false, nowElapsed);
                            int bias = evaluateJobBiasLocked(job2);
                            pw.print("Evaluated bias: ");
                            pw.println(JobInfo.getBiasString(bias));
                            pw.print("Tag: ");
                            pw.println(job2.getTag());
                            pw.print("Enq: ");
                            pendingIdx = pendingIdx2;
                            TimeUtils.formatDuration(job2.madePending - nowUptime, pw);
                            pw.decreaseIndent();
                            pw.println();
                        }
                    }
                    if (!pendingPrinted) {
                        pw.println("None");
                    }
                    pw.decreaseIndent();
                    pw.println();
                    boolean z4 = z;
                    this.mConcurrencyManager.dumpContextInfoLocked(pw, predicate, nowElapsed, nowUptime);
                    pw.println();
                    pw.println("Recently completed jobs:");
                    pw.increaseIndent();
                    boolean recentPrinted = false;
                    for (int r = 1; r <= 20; r++) {
                        int idx = ((this.mLastCompletedJobIndex + 20) - r) % 20;
                        JobStatus job3 = this.mLastCompletedJobs[idx];
                        if (job3 != null && predicate.test(job3)) {
                            TimeUtils.formatDuration(this.mLastCompletedJobTimeElapsed[idx], nowElapsed, pw);
                            pw.println();
                            pw.increaseIndent();
                            pw.increaseIndent();
                            pw.println(job3.toShortString());
                            job3.dump(pw, z4, nowElapsed);
                            pw.decreaseIndent();
                            pw.decreaseIndent();
                            recentPrinted = true;
                        }
                    }
                    if (!recentPrinted) {
                        pw.println("None");
                    }
                    pw.decreaseIndent();
                    pw.println();
                    if (filterUid == -1) {
                        pw.println();
                        pw.print("mReadyToRock=");
                        pw.println(this.mReadyToRock);
                        pw.print("mReportedActive=");
                        pw.println(this.mReportedActive);
                    }
                    pw.println();
                    this.mConcurrencyManager.dumpLocked(pw, now, nowElapsed);
                    pw.println();
                    pw.print("PersistStats: ");
                    pw.println(this.mJobs.getPersistStats());
                } catch (Throwable th2) {
                    th = th2;
                }
            } catch (Throwable th3) {
                th = th3;
                obj = obj2;
            }
        }
        pw.println();
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static /* synthetic */ boolean lambda$dumpInternal$5(int filterAppId, JobStatus js) {
        return filterAppId == -1 || UserHandle.getAppId(js.getUid()) == filterAppId || UserHandle.getAppId(js.getSourceUid()) == filterAppId;
    }

    /* JADX DEBUG: Don't trust debug lines info. Repeating lines: [3966=8] */
    void dumpInternalProto(FileDescriptor fd, int filterUid) {
        Object obj;
        int filterAppId;
        long now;
        int filterAppId2;
        ProtoOutputStream proto = new ProtoOutputStream(fd);
        final int filterAppId3 = UserHandle.getAppId(filterUid);
        long now2 = sSystemClock.millis();
        long nowElapsed = sElapsedRealtimeClock.millis();
        long nowUptime = sUptimeMillisClock.millis();
        Predicate<JobStatus> predicate = new Predicate() { // from class: com.android.server.job.JobSchedulerService$$ExternalSyntheticLambda5
            @Override // java.util.function.Predicate
            public final boolean test(Object obj2) {
                return JobSchedulerService.lambda$dumpInternalProto$6(filterAppId3, (JobStatus) obj2);
            }
        };
        Object obj2 = this.mLock;
        synchronized (obj2) {
            try {
                try {
                    long settingsToken = proto.start(1146756268033L);
                    this.mConstants.dump(proto);
                    for (StateController controller : this.mControllers) {
                        try {
                            controller.dumpConstants(proto);
                        } catch (Throwable th) {
                            th = th;
                            obj = obj2;
                        }
                    }
                    proto.end(settingsToken);
                    for (int i = this.mJobRestrictions.size() - 1; i >= 0; i--) {
                        this.mJobRestrictions.get(i).dumpConstants(proto);
                    }
                    int[] iArr = this.mStartedUsers;
                    int length = iArr.length;
                    int i2 = 0;
                    while (i2 < length) {
                        int u = iArr[i2];
                        int[] iArr2 = iArr;
                        proto.write(2220498092034L, u);
                        i2++;
                        iArr = iArr2;
                    }
                    this.mQuotaTracker.dump(proto, 1146756268054L);
                    if (this.mJobs.size() > 0) {
                        try {
                            List<JobStatus> jobs = this.mJobs.mJobSet.getAllJobs();
                            sortJobs(jobs);
                            for (JobStatus job : jobs) {
                                long rjToken = proto.start(2246267895811L);
                                job.writeToShortProto(proto, 1146756268033L);
                                if (predicate.test(job)) {
                                    long settingsToken2 = settingsToken;
                                    int filterAppId4 = filterAppId3;
                                    long now3 = now2;
                                    obj = obj2;
                                    try {
                                        job.dump(proto, 1146756268034L, true, nowElapsed);
                                        proto.write(1133871366154L, isReadyToBeExecutedLocked(job));
                                        proto.write(1133871366147L, job.isReady());
                                        proto.write(1133871366148L, areUsersStartedLocked(job));
                                        proto.write(1133871366155L, checkIfRestricted(job) != null);
                                        proto.write(1133871366149L, this.mPendingJobQueue.contains(job));
                                        proto.write(1133871366150L, this.mConcurrencyManager.isJobRunningLocked(job));
                                        proto.write(1133871366151L, this.mBackingUpUids.get(job.getSourceUid()));
                                        proto.write(1133871366152L, isComponentUsable(job));
                                        for (JobRestriction restriction : this.mJobRestrictions) {
                                            long restrictionsToken = proto.start(2246267895820L);
                                            proto.write(1159641169921L, restriction.getInternalReason());
                                            proto.write(1133871366146L, restriction.isJobRestricted(job));
                                            proto.end(restrictionsToken);
                                        }
                                        proto.end(rjToken);
                                        obj2 = obj;
                                        filterAppId3 = filterAppId4;
                                        settingsToken = settingsToken2;
                                        now2 = now3;
                                    } catch (Throwable th2) {
                                        th = th2;
                                    }
                                }
                            }
                            filterAppId = filterAppId3;
                            now = now2;
                            obj = obj2;
                        } catch (Throwable th3) {
                            th = th3;
                            obj = obj2;
                        }
                    } else {
                        filterAppId = filterAppId3;
                        now = now2;
                        obj = obj2;
                    }
                    try {
                        for (StateController controller2 : this.mControllers) {
                            controller2.dumpControllerStateLocked(proto, 2246267895812L, predicate);
                        }
                        int i3 = 0;
                        while (i3 < this.mUidBiasOverride.size()) {
                            try {
                                int uid = this.mUidBiasOverride.keyAt(i3);
                                filterAppId2 = filterAppId;
                                if (filterAppId2 != -1) {
                                    try {
                                        if (filterAppId2 != UserHandle.getAppId(uid)) {
                                            i3++;
                                            filterAppId = filterAppId2;
                                        }
                                    } catch (Throwable th4) {
                                        th = th4;
                                    }
                                }
                                long pToken = proto.start(2246267895813L);
                                proto.write(CompanionMessage.MESSAGE_ID, uid);
                                proto.write(1172526071810L, this.mUidBiasOverride.valueAt(i3));
                                proto.end(pToken);
                                i3++;
                                filterAppId = filterAppId2;
                            } catch (Throwable th5) {
                                th = th5;
                            }
                        }
                        filterAppId2 = filterAppId;
                        for (int i4 = 0; i4 < this.mBackingUpUids.size(); i4++) {
                            try {
                                int uid2 = this.mBackingUpUids.keyAt(i4);
                                if (filterAppId2 == -1 || filterAppId2 == UserHandle.getAppId(uid2)) {
                                    proto.write(2220498092038L, uid2);
                                }
                            } catch (Throwable th6) {
                                th = th6;
                            }
                        }
                        this.mJobPackageTracker.dump(proto, 1146756268040L, filterAppId2);
                        this.mJobPackageTracker.dumpHistory(proto, 1146756268039L, filterAppId2);
                        this.mPendingJobQueue.resetIterator();
                    } catch (Throwable th7) {
                        th = th7;
                    }
                } catch (Throwable th8) {
                    th = th8;
                }
            } catch (Throwable th9) {
                th = th9;
                obj = obj2;
            }
            while (true) {
                JobStatus job2 = this.mPendingJobQueue.next();
                if (job2 == null) {
                    break;
                }
                try {
                    long pjToken = proto.start(2246267895817L);
                    job2.writeToShortProto(proto, 1146756268033L);
                    int filterAppId5 = filterAppId2;
                    job2.dump(proto, 1146756268034L, false, nowElapsed);
                    proto.write(1172526071811L, evaluateJobBiasLocked(job2));
                    proto.write(1112396529668L, nowUptime - job2.madePending);
                    proto.end(pjToken);
                    filterAppId2 = filterAppId5;
                } catch (Throwable th10) {
                    th = th10;
                }
                throw th;
            }
            if (filterUid == -1) {
                try {
                    proto.write(1133871366155L, this.mReadyToRock);
                    proto.write(1133871366156L, this.mReportedActive);
                } catch (Throwable th11) {
                    th = th11;
                }
            }
            try {
                this.mConcurrencyManager.dumpProtoLocked(proto, 1146756268052L, now, nowElapsed);
                this.mJobs.getPersistStats().dumpDebug(proto, 1146756268053L);
                proto.flush();
            } catch (Throwable th12) {
                th = th12;
                throw th;
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static /* synthetic */ boolean lambda$dumpInternalProto$6(int filterAppId, JobStatus js) {
        return filterAppId == -1 || UserHandle.getAppId(js.getUid()) == filterAppId || UserHandle.getAppId(js.getSourceUid()) == filterAppId;
    }
}
