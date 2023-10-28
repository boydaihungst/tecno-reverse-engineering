package com.android.server.job.controllers;

import android.app.AppGlobals;
import android.app.job.JobInfo;
import android.app.job.JobWorkItem;
import android.content.ClipData;
import android.content.ComponentName;
import android.content.pm.ServiceInfo;
import android.net.Network;
import android.net.NetworkRequest;
import android.net.Uri;
import android.os.RemoteException;
import android.os.UserHandle;
import android.provider.MediaStore;
import android.text.format.DateFormat;
import android.util.ArraySet;
import android.util.IndentingPrintWriter;
import android.util.Pair;
import android.util.Range;
import android.util.Slog;
import android.util.TimeUtils;
import android.util.proto.ProtoOutputStream;
import com.android.internal.util.jobs.ArrayUtils;
import com.android.server.LocalServices;
import com.android.server.job.GrantedUriPermissions;
import com.android.server.job.JobSchedulerInternal;
import com.android.server.job.JobSchedulerService;
import com.android.server.job.controllers.ContentObserverController;
import com.android.server.slice.SliceClientPermissions;
import dalvik.annotation.optimization.NeverCompile;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.function.Predicate;
/* loaded from: classes.dex */
public final class JobStatus {
    static final int CONSTRAINTS_OF_INTEREST = -1803550705;
    static final int CONSTRAINT_BACKGROUND_NOT_RESTRICTED = 4194304;
    static final int CONSTRAINT_BATTERY_NOT_LOW = 2;
    static final int CONSTRAINT_CHARGING = 1;
    static final int CONSTRAINT_CONNECTIVITY = 268435456;
    static final int CONSTRAINT_CONTENT_TRIGGER = 67108864;
    static final int CONSTRAINT_DEADLINE = 1073741824;
    static final int CONSTRAINT_DEVICE_NOT_DOZING = 33554432;
    static final int CONSTRAINT_IDLE = 4;
    static final int CONSTRAINT_PREFETCH = 8388608;
    static final int CONSTRAINT_STORAGE_NOT_LOW = 8;
    static final int CONSTRAINT_TARE_WEALTH = 134217728;
    static final int CONSTRAINT_TIMING_DELAY = Integer.MIN_VALUE;
    static final int CONSTRAINT_WITHIN_QUOTA = 16777216;
    static final boolean DEBUG_PREPARE = true;
    public static final long DEFAULT_TRIGGER_MAX_DELAY = 120000;
    public static final long DEFAULT_TRIGGER_UPDATE_DELAY = 10000;
    private static final int DYNAMIC_EXPEDITED_DEFERRAL_CONSTRAINTS = 37748736;
    private static final int DYNAMIC_RESTRICTED_CONSTRAINTS = 268435463;
    public static final int INTERNAL_FLAG_HAS_FOREGROUND_EXEMPTION = 1;
    public static final long MIN_TRIGGER_MAX_DELAY = 1000;
    public static final long MIN_TRIGGER_UPDATE_DELAY = 500;
    public static final long NO_EARLIEST_RUNTIME = 0;
    public static final long NO_LATEST_RUNTIME = Long.MAX_VALUE;
    private static final int NUM_CONSTRAINT_CHANGE_HISTORY = 10;
    public static final int OVERRIDE_FULL = 3;
    public static final int OVERRIDE_NONE = 0;
    public static final int OVERRIDE_SOFT = 2;
    public static final int OVERRIDE_SORTING = 1;
    static final int SOFT_OVERRIDE_CONSTRAINTS = -2139095025;
    private static final int STATSD_CONSTRAINTS_TO_LOG = -847249404;
    private static final boolean STATS_LOG_ENABLED = false;
    private static final String TAG = "JobScheduler.JobStatus";
    public static final int TRACKING_BATTERY = 1;
    public static final int TRACKING_CONNECTIVITY = 2;
    public static final int TRACKING_CONTENT = 4;
    public static final int TRACKING_IDLE = 8;
    public static final int TRACKING_QUOTA = 64;
    public static final int TRACKING_STORAGE = 16;
    public static final int TRACKING_TIME = 32;
    boolean appHasDozeExemption;
    final String batteryName;
    final int callingUid;
    public ArraySet<String> changedAuthorities;
    public ArraySet<Uri> changedUris;
    ContentObserverController.JobInstance contentObserverJobInstance;
    private final long earliestRunTimeElapsedMillis;
    public long enqueueTime;
    public ArrayList<JobWorkItem> executingWork;
    final JobInfo job;
    public int lastEvaluatedBias;
    private final long latestRunTimeElapsedMillis;
    private int mConstraintChangeHistoryIndex;
    private final int[] mConstraintStatusHistory;
    private final long[] mConstraintUpdatedTimesElapsed;
    private int mDynamicConstraints;
    private boolean mExpeditedQuotaApproved;
    private boolean mExpeditedTareApproved;
    private long mFirstForceBatchedTimeElapsed;
    private final boolean mHasExemptedMediaUrisOnly;
    private boolean mHasMediaBackupExemption;
    private int mInternalFlags;
    private boolean mIsUserBgRestricted;
    private long mLastFailedRunTime;
    private long mLastSuccessfulRunTime;
    private boolean mLoggedBucketMismatch;
    private long mMinimumNetworkChunkBytes;
    private long mOriginalLatestRunTimeElapsedMillis;
    private Pair<Long, Long> mPersistedUtcTimes;
    private boolean mReadyDeadlineSatisfied;
    private boolean mReadyDynamicSatisfied;
    private boolean mReadyNotDozing;
    private boolean mReadyNotRestrictedInBg;
    private boolean mReadyTareWealth;
    private boolean mReadyWithinQuota;
    private int mReasonReadyToUnready;
    private final int mRequiredConstraintsOfInterest;
    private int mSatisfiedConstraintsOfInterest;
    private long mTotalNetworkDownloadBytes;
    private long mTotalNetworkUploadBytes;
    public long madeActive;
    public long madePending;
    public Network network;
    public int nextPendingWorkId;
    private final int numFailures;
    public int overrideState;
    public ArrayList<JobWorkItem> pendingWork;
    private boolean prepared;
    final int requiredConstraints;
    int satisfiedConstraints;
    public ServiceInfo serviceInfo;
    final String sourcePackageName;
    final String sourceTag;
    final int sourceUid;
    final int sourceUserId;
    private int standbyBucket;
    public boolean startedAsExpeditedJob;
    final String tag;
    private int trackingControllers;
    public boolean uidActive;
    private Throwable unpreparedPoint;
    private GrantedUriPermissions uriPerms;
    private long whenStandbyDeferred;
    static final boolean DEBUG = JobSchedulerService.DEBUG;
    private static final Uri[] MEDIA_URIS_FOR_STANDBY_EXEMPTION = {MediaStore.Images.Media.EXTERNAL_CONTENT_URI, MediaStore.Video.Media.EXTERNAL_CONTENT_URI};

    private JobStatus(JobInfo job, int callingUid, String sourcePackageName, int sourceUserId, int standbyBucket, String tag, int numFailures, long earliestRunTimeElapsedMillis, long latestRunTimeElapsedMillis, long lastSuccessfulRunTime, long lastFailedRunTime, int internalFlags, int dynamicConstraints) {
        String flattenToShortString;
        this.unpreparedPoint = null;
        this.satisfiedConstraints = 0;
        this.mSatisfiedConstraintsOfInterest = 0;
        this.mDynamicConstraints = 0;
        this.startedAsExpeditedJob = false;
        this.nextPendingWorkId = 1;
        this.overrideState = 0;
        this.mConstraintChangeHistoryIndex = 0;
        this.mConstraintUpdatedTimesElapsed = new long[10];
        this.mConstraintStatusHistory = new int[10];
        this.mTotalNetworkDownloadBytes = -1L;
        this.mTotalNetworkUploadBytes = -1L;
        this.mMinimumNetworkChunkBytes = -1L;
        this.mReasonReadyToUnready = 0;
        this.job = job;
        this.callingUid = callingUid;
        this.standbyBucket = standbyBucket;
        int tempSourceUid = -1;
        if (sourceUserId != -1 && sourcePackageName != null) {
            try {
                tempSourceUid = AppGlobals.getPackageManager().getPackageUid(sourcePackageName, 0L, sourceUserId);
            } catch (RemoteException e) {
            }
        }
        if (tempSourceUid == -1) {
            this.sourceUid = callingUid;
            this.sourceUserId = UserHandle.getUserId(callingUid);
            this.sourcePackageName = job.getService().getPackageName();
            this.sourceTag = null;
        } else {
            this.sourceUid = tempSourceUid;
            this.sourceUserId = sourceUserId;
            this.sourcePackageName = sourcePackageName;
            this.sourceTag = tag;
        }
        if (this.sourceTag != null) {
            flattenToShortString = this.sourceTag + ":" + job.getService().getPackageName();
        } else {
            flattenToShortString = job.getService().flattenToShortString();
        }
        this.batteryName = flattenToShortString;
        this.tag = "*job*/" + flattenToShortString;
        this.earliestRunTimeElapsedMillis = earliestRunTimeElapsedMillis;
        this.latestRunTimeElapsedMillis = latestRunTimeElapsedMillis;
        this.mOriginalLatestRunTimeElapsedMillis = latestRunTimeElapsedMillis;
        this.numFailures = numFailures;
        int requiredConstraints = job.getConstraintFlags();
        requiredConstraints = job.getRequiredNetwork() != null ? requiredConstraints | 268435456 : requiredConstraints;
        requiredConstraints = earliestRunTimeElapsedMillis != 0 ? requiredConstraints | Integer.MIN_VALUE : requiredConstraints;
        requiredConstraints = latestRunTimeElapsedMillis != NO_LATEST_RUNTIME ? requiredConstraints | 1073741824 : requiredConstraints;
        boolean exemptedMediaUrisOnly = false;
        if (job.getTriggerContentUris() != null) {
            int requiredConstraints2 = requiredConstraints | 67108864;
            exemptedMediaUrisOnly = true;
            JobInfo.TriggerContentUri[] triggerContentUris = job.getTriggerContentUris();
            int requiredConstraints3 = triggerContentUris.length;
            int i = 0;
            while (true) {
                if (i >= requiredConstraints3) {
                    requiredConstraints = requiredConstraints2;
                    break;
                }
                JobInfo.TriggerContentUri uri = triggerContentUris[i];
                int i2 = requiredConstraints3;
                if (ArrayUtils.contains(MEDIA_URIS_FOR_STANDBY_EXEMPTION, uri.getUri())) {
                    i++;
                    requiredConstraints3 = i2;
                } else {
                    exemptedMediaUrisOnly = false;
                    requiredConstraints = requiredConstraints2;
                    break;
                }
            }
        }
        this.mHasExemptedMediaUrisOnly = exemptedMediaUrisOnly;
        this.requiredConstraints = requiredConstraints;
        this.mRequiredConstraintsOfInterest = CONSTRAINTS_OF_INTEREST & requiredConstraints;
        addDynamicConstraints(dynamicConstraints);
        this.mReadyNotDozing = canRunInDoze();
        if (standbyBucket == 5) {
            addDynamicConstraints(DYNAMIC_RESTRICTED_CONSTRAINTS);
        } else {
            this.mReadyDynamicSatisfied = false;
        }
        this.mLastSuccessfulRunTime = lastSuccessfulRunTime;
        this.mLastFailedRunTime = lastFailedRunTime;
        this.mInternalFlags = internalFlags;
        updateNetworkBytesLocked();
        if (job.getRequiredNetwork() != null) {
            JobInfo.Builder builder = new JobInfo.Builder(job);
            NetworkRequest.Builder requestBuilder = new NetworkRequest.Builder(job.getRequiredNetwork());
            requestBuilder.setUids(Collections.singleton(new Range(Integer.valueOf(this.sourceUid), Integer.valueOf(this.sourceUid))));
            builder.setRequiredNetwork(requestBuilder.build());
            builder.build(false);
        }
        updateMediaBackupExemptionStatus();
    }

    public JobStatus(JobStatus jobStatus) {
        this(jobStatus.getJob(), jobStatus.getUid(), jobStatus.getSourcePackageName(), jobStatus.getSourceUserId(), jobStatus.getStandbyBucket(), jobStatus.getSourceTag(), jobStatus.getNumFailures(), jobStatus.getEarliestRunTime(), jobStatus.getLatestRunTimeElapsed(), jobStatus.getLastSuccessfulRunTime(), jobStatus.getLastFailedRunTime(), jobStatus.getInternalFlags(), jobStatus.mDynamicConstraints);
        this.mPersistedUtcTimes = jobStatus.mPersistedUtcTimes;
        if (jobStatus.mPersistedUtcTimes != null && DEBUG) {
            Slog.i(TAG, "Cloning job with persisted run times", new RuntimeException("here"));
        }
    }

    public JobStatus(JobInfo job, int callingUid, String sourcePkgName, int sourceUserId, int standbyBucket, String sourceTag, long earliestRunTimeElapsedMillis, long latestRunTimeElapsedMillis, long lastSuccessfulRunTime, long lastFailedRunTime, Pair<Long, Long> persistedExecutionTimesUTC, int innerFlags, int dynamicConstraints) {
        this(job, callingUid, sourcePkgName, sourceUserId, standbyBucket, sourceTag, 0, earliestRunTimeElapsedMillis, latestRunTimeElapsedMillis, lastSuccessfulRunTime, lastFailedRunTime, innerFlags, dynamicConstraints);
        this.mPersistedUtcTimes = persistedExecutionTimesUTC;
        if (persistedExecutionTimesUTC != null && DEBUG) {
            Slog.i(TAG, "+ restored job with RTC times because of bad boot clock");
        }
    }

    public JobStatus(JobStatus rescheduling, long newEarliestRuntimeElapsedMillis, long newLatestRuntimeElapsedMillis, int backoffAttempt, long lastSuccessfulRunTime, long lastFailedRunTime) {
        this(rescheduling.job, rescheduling.getUid(), rescheduling.getSourcePackageName(), rescheduling.getSourceUserId(), rescheduling.getStandbyBucket(), rescheduling.getSourceTag(), backoffAttempt, newEarliestRuntimeElapsedMillis, newLatestRuntimeElapsedMillis, lastSuccessfulRunTime, lastFailedRunTime, rescheduling.getInternalFlags(), rescheduling.mDynamicConstraints);
    }

    public static JobStatus createFromJobInfo(JobInfo job, int callingUid, String sourcePkg, int sourceUserId, String tag) {
        long period;
        long latestRunTimeElapsedMillis;
        long elapsedNow = JobSchedulerService.sElapsedRealtimeClock.millis();
        if (job.isPeriodic()) {
            long period2 = Math.max(JobInfo.getMinPeriodMillis(), Math.min(31536000000L, job.getIntervalMillis()));
            latestRunTimeElapsedMillis = elapsedNow + period2;
            period = latestRunTimeElapsedMillis - Math.max(JobInfo.getMinFlexMillis(), Math.min(period2, job.getFlexMillis()));
        } else {
            period = job.hasEarlyConstraint() ? job.getMinLatencyMillis() + elapsedNow : 0L;
            latestRunTimeElapsedMillis = job.hasLateConstraint() ? job.getMaxExecutionDelayMillis() + elapsedNow : NO_LATEST_RUNTIME;
        }
        String jobPackage = sourcePkg != null ? sourcePkg : job.getService().getPackageName();
        int standbyBucket = JobSchedulerService.standbyBucketForPackage(jobPackage, sourceUserId, elapsedNow);
        return new JobStatus(job, callingUid, sourcePkg, sourceUserId, standbyBucket, tag, 0, period, latestRunTimeElapsedMillis, 0L, 0L, 0, 0);
    }

    public void enqueueWorkLocked(JobWorkItem work) {
        if (this.pendingWork == null) {
            this.pendingWork = new ArrayList<>();
        }
        work.setWorkId(this.nextPendingWorkId);
        this.nextPendingWorkId++;
        if (work.getIntent() != null && GrantedUriPermissions.checkGrantFlags(work.getIntent().getFlags())) {
            work.setGrants(GrantedUriPermissions.createFromIntent(work.getIntent(), this.sourceUid, this.sourcePackageName, this.sourceUserId, toShortString()));
        }
        this.pendingWork.add(work);
        updateNetworkBytesLocked();
    }

    public JobWorkItem dequeueWorkLocked() {
        ArrayList<JobWorkItem> arrayList = this.pendingWork;
        if (arrayList != null && arrayList.size() > 0) {
            JobWorkItem work = this.pendingWork.remove(0);
            if (work != null) {
                if (this.executingWork == null) {
                    this.executingWork = new ArrayList<>();
                }
                this.executingWork.add(work);
                work.bumpDeliveryCount();
            }
            updateNetworkBytesLocked();
            return work;
        }
        return null;
    }

    public boolean hasWorkLocked() {
        ArrayList<JobWorkItem> arrayList = this.pendingWork;
        return (arrayList != null && arrayList.size() > 0) || hasExecutingWorkLocked();
    }

    public boolean hasExecutingWorkLocked() {
        ArrayList<JobWorkItem> arrayList = this.executingWork;
        return arrayList != null && arrayList.size() > 0;
    }

    private static void ungrantWorkItem(JobWorkItem work) {
        if (work.getGrants() != null) {
            ((GrantedUriPermissions) work.getGrants()).revoke();
        }
    }

    public boolean completeWorkLocked(int workId) {
        ArrayList<JobWorkItem> arrayList = this.executingWork;
        if (arrayList != null) {
            int N = arrayList.size();
            for (int i = 0; i < N; i++) {
                JobWorkItem work = this.executingWork.get(i);
                if (work.getWorkId() == workId) {
                    this.executingWork.remove(i);
                    ungrantWorkItem(work);
                    return true;
                }
            }
            return false;
        }
        return false;
    }

    private static void ungrantWorkList(ArrayList<JobWorkItem> list) {
        if (list != null) {
            int N = list.size();
            for (int i = 0; i < N; i++) {
                ungrantWorkItem(list.get(i));
            }
        }
    }

    public void stopTrackingJobLocked(JobStatus incomingJob) {
        if (incomingJob != null) {
            ArrayList<JobWorkItem> arrayList = this.executingWork;
            if (arrayList != null && arrayList.size() > 0) {
                incomingJob.pendingWork = this.executingWork;
            }
            if (incomingJob.pendingWork == null) {
                incomingJob.pendingWork = this.pendingWork;
            } else {
                ArrayList<JobWorkItem> arrayList2 = this.pendingWork;
                if (arrayList2 != null && arrayList2.size() > 0) {
                    incomingJob.pendingWork.addAll(this.pendingWork);
                }
            }
            this.pendingWork = null;
            this.executingWork = null;
            incomingJob.nextPendingWorkId = this.nextPendingWorkId;
            incomingJob.updateNetworkBytesLocked();
        } else {
            ungrantWorkList(this.pendingWork);
            this.pendingWork = null;
            ungrantWorkList(this.executingWork);
            this.executingWork = null;
        }
        updateNetworkBytesLocked();
    }

    public void prepareLocked() {
        if (this.prepared) {
            Slog.wtf(TAG, "Already prepared: " + this);
            return;
        }
        this.prepared = true;
        this.unpreparedPoint = null;
        ClipData clip = this.job.getClipData();
        if (clip != null) {
            this.uriPerms = GrantedUriPermissions.createFromClip(clip, this.sourceUid, this.sourcePackageName, this.sourceUserId, this.job.getClipGrantFlags(), toShortString());
        }
    }

    public void unprepareLocked() {
        if (!this.prepared) {
            Slog.wtf(TAG, "Hasn't been prepared: " + this);
            Throwable th = this.unpreparedPoint;
            if (th != null) {
                Slog.e(TAG, "Was already unprepared at ", th);
                return;
            }
            return;
        }
        this.prepared = false;
        this.unpreparedPoint = new Throwable().fillInStackTrace();
        GrantedUriPermissions grantedUriPermissions = this.uriPerms;
        if (grantedUriPermissions != null) {
            grantedUriPermissions.revoke();
            this.uriPerms = null;
        }
    }

    public boolean isPreparedLocked() {
        return this.prepared;
    }

    public JobInfo getJob() {
        return this.job;
    }

    public int getJobId() {
        return this.job.getId();
    }

    public void printUniqueId(PrintWriter pw) {
        UserHandle.formatUid(pw, this.callingUid);
        pw.print(SliceClientPermissions.SliceAuthority.DELIMITER);
        pw.print(this.job.getId());
    }

    public int getNumFailures() {
        return this.numFailures;
    }

    public ComponentName getServiceComponent() {
        return this.job.getService();
    }

    public String getSourcePackageName() {
        return this.sourcePackageName;
    }

    public int getSourceUid() {
        return this.sourceUid;
    }

    public int getSourceUserId() {
        return this.sourceUserId;
    }

    public int getUserId() {
        return UserHandle.getUserId(this.callingUid);
    }

    public int getEffectiveStandbyBucket() {
        int actualBucket = getStandbyBucket();
        if (actualBucket == 6) {
            return actualBucket;
        }
        if (this.uidActive || getJob().isExemptedFromAppStandby()) {
            return 0;
        }
        if (actualBucket != 5 && actualBucket != 4 && this.mHasMediaBackupExemption) {
            return Math.min(1, actualBucket);
        }
        return actualBucket;
    }

    public int getStandbyBucket() {
        return this.standbyBucket;
    }

    public void setStandbyBucket(int newBucket) {
        if (newBucket == 5) {
            addDynamicConstraints(DYNAMIC_RESTRICTED_CONSTRAINTS);
        } else if (this.standbyBucket == 5) {
            removeDynamicConstraints(DYNAMIC_RESTRICTED_CONSTRAINTS);
        }
        this.standbyBucket = newBucket;
        this.mLoggedBucketMismatch = false;
    }

    public void maybeLogBucketMismatch() {
        if (!this.mLoggedBucketMismatch) {
            Slog.wtf(TAG, "App " + getSourcePackageName() + " became active but still in NEVER bucket");
            this.mLoggedBucketMismatch = true;
        }
    }

    public long getWhenStandbyDeferred() {
        return this.whenStandbyDeferred;
    }

    public void setWhenStandbyDeferred(long now) {
        this.whenStandbyDeferred = now;
    }

    public long getFirstForceBatchedTimeElapsed() {
        return this.mFirstForceBatchedTimeElapsed;
    }

    public void setFirstForceBatchedTimeElapsed(long now) {
        this.mFirstForceBatchedTimeElapsed = now;
    }

    public boolean updateMediaBackupExemptionStatus() {
        JobSchedulerInternal jsi = (JobSchedulerInternal) LocalServices.getService(JobSchedulerInternal.class);
        boolean hasMediaExemption = this.mHasExemptedMediaUrisOnly && !this.job.hasLateConstraint() && this.job.getRequiredNetwork() != null && getEffectivePriority() >= 300 && this.sourcePackageName.equals(jsi.getCloudMediaProviderPackage(this.sourceUserId));
        if (this.mHasMediaBackupExemption == hasMediaExemption) {
            return false;
        }
        this.mHasMediaBackupExemption = hasMediaExemption;
        return true;
    }

    public String getSourceTag() {
        return this.sourceTag;
    }

    public int getUid() {
        return this.callingUid;
    }

    public String getBatteryName() {
        return this.batteryName;
    }

    public String getTag() {
        return this.tag;
    }

    public int getBias() {
        return this.job.getBias();
    }

    public int getEffectivePriority() {
        int rawPriority = this.job.getPriority();
        if (this.numFailures < 2) {
            return rawPriority;
        }
        if (isRequestedExpeditedJob()) {
            return 400;
        }
        int dropPower = this.numFailures / 2;
        switch (dropPower) {
            case 1:
                return Math.min(300, rawPriority);
            case 2:
                return Math.min(200, rawPriority);
            default:
                return 100;
        }
    }

    public int getFlags() {
        return this.job.getFlags();
    }

    public int getInternalFlags() {
        return this.mInternalFlags;
    }

    public void addInternalFlags(int flags) {
        this.mInternalFlags |= flags;
    }

    public int getSatisfiedConstraintFlags() {
        return this.satisfiedConstraints;
    }

    public void maybeAddForegroundExemption(Predicate<Integer> uidForegroundChecker) {
        if (!this.job.hasEarlyConstraint() && !this.job.hasLateConstraint() && (this.mInternalFlags & 1) == 0 && uidForegroundChecker.test(Integer.valueOf(getSourceUid()))) {
            addInternalFlags(1);
        }
    }

    private void updateNetworkBytesLocked() {
        this.mTotalNetworkDownloadBytes = this.job.getEstimatedNetworkDownloadBytes();
        this.mTotalNetworkUploadBytes = this.job.getEstimatedNetworkUploadBytes();
        this.mMinimumNetworkChunkBytes = this.job.getMinimumNetworkChunkBytes();
        if (this.pendingWork != null) {
            for (int i = 0; i < this.pendingWork.size(); i++) {
                if (this.mTotalNetworkDownloadBytes != -1) {
                    long downloadBytes = this.pendingWork.get(i).getEstimatedNetworkDownloadBytes();
                    if (downloadBytes != -1) {
                        this.mTotalNetworkDownloadBytes += downloadBytes;
                    }
                }
                if (this.mTotalNetworkUploadBytes != -1) {
                    long uploadBytes = this.pendingWork.get(i).getEstimatedNetworkUploadBytes();
                    if (uploadBytes != -1) {
                        this.mTotalNetworkUploadBytes += uploadBytes;
                    }
                }
                long chunkBytes = this.pendingWork.get(i).getMinimumNetworkChunkBytes();
                long j = this.mMinimumNetworkChunkBytes;
                if (j == -1) {
                    this.mMinimumNetworkChunkBytes = chunkBytes;
                } else if (chunkBytes != -1) {
                    this.mMinimumNetworkChunkBytes = Math.min(j, chunkBytes);
                }
            }
        }
    }

    public long getEstimatedNetworkDownloadBytes() {
        return this.mTotalNetworkDownloadBytes;
    }

    public long getEstimatedNetworkUploadBytes() {
        return this.mTotalNetworkUploadBytes;
    }

    public long getMinimumNetworkChunkBytes() {
        return this.mMinimumNetworkChunkBytes;
    }

    public boolean hasConnectivityConstraint() {
        return (this.requiredConstraints & 268435456) != 0;
    }

    public boolean hasChargingConstraint() {
        return hasConstraint(1);
    }

    public boolean hasBatteryNotLowConstraint() {
        return hasConstraint(2);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean hasPowerConstraint() {
        return hasConstraint(3);
    }

    public boolean hasStorageNotLowConstraint() {
        return hasConstraint(8);
    }

    public boolean hasTimingDelayConstraint() {
        return hasConstraint(Integer.MIN_VALUE);
    }

    public boolean hasDeadlineConstraint() {
        return hasConstraint(1073741824);
    }

    public boolean hasIdleConstraint() {
        return hasConstraint(4);
    }

    public boolean hasContentTriggerConstraint() {
        return (this.requiredConstraints & 67108864) != 0;
    }

    private boolean hasConstraint(int constraint) {
        return ((this.requiredConstraints & constraint) == 0 && (this.mDynamicConstraints & constraint) == 0) ? false : true;
    }

    public long getTriggerContentUpdateDelay() {
        long time = this.job.getTriggerContentUpdateDelay();
        if (time < 0) {
            return DEFAULT_TRIGGER_UPDATE_DELAY;
        }
        return Math.max(time, 500L);
    }

    public long getTriggerContentMaxDelay() {
        long time = this.job.getTriggerContentMaxDelay();
        if (time < 0) {
            return 120000L;
        }
        return Math.max(time, 1000L);
    }

    public boolean isPersisted() {
        return this.job.isPersisted();
    }

    public long getEarliestRunTime() {
        return this.earliestRunTimeElapsedMillis;
    }

    public long getLatestRunTimeElapsed() {
        return this.latestRunTimeElapsedMillis;
    }

    public long getOriginalLatestRunTimeElapsed() {
        return this.mOriginalLatestRunTimeElapsedMillis;
    }

    public void setOriginalLatestRunTimeElapsed(long latestRunTimeElapsed) {
        this.mOriginalLatestRunTimeElapsedMillis = latestRunTimeElapsed;
    }

    public int getStopReason() {
        return this.mReasonReadyToUnready;
    }

    public float getFractionRunTime() {
        long now = JobSchedulerService.sElapsedRealtimeClock.millis();
        long j = this.earliestRunTimeElapsedMillis;
        if (j == 0 && this.latestRunTimeElapsedMillis == NO_LATEST_RUNTIME) {
            return 1.0f;
        }
        if (j == 0) {
            return now >= this.latestRunTimeElapsedMillis ? 1.0f : 0.0f;
        }
        long j2 = this.latestRunTimeElapsedMillis;
        if (j2 == NO_LATEST_RUNTIME) {
            return now >= j ? 1.0f : 0.0f;
        } else if (now <= j) {
            return 0.0f;
        } else {
            if (now >= j2) {
                return 1.0f;
            }
            return ((float) (now - j)) / ((float) (j2 - j));
        }
    }

    public Pair<Long, Long> getPersistedUtcTimes() {
        return this.mPersistedUtcTimes;
    }

    public void clearPersistedUtcTimes() {
        this.mPersistedUtcTimes = null;
    }

    public boolean isRequestedExpeditedJob() {
        return (getFlags() & 16) != 0;
    }

    public boolean shouldTreatAsExpeditedJob() {
        return this.mExpeditedQuotaApproved && this.mExpeditedTareApproved && isRequestedExpeditedJob();
    }

    public boolean canRunInDoze() {
        if (this.appHasDozeExemption || (getFlags() & 1) != 0) {
            return true;
        }
        return (shouldTreatAsExpeditedJob() || this.startedAsExpeditedJob) && (this.mDynamicConstraints & 33554432) == 0;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean canRunInBatterySaver() {
        if ((getInternalFlags() & 1) == 0) {
            return (shouldTreatAsExpeditedJob() || this.startedAsExpeditedJob) && (this.mDynamicConstraints & 4194304) == 0;
        }
        return true;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean setChargingConstraintSatisfied(long nowElapsed, boolean state) {
        return setConstraintSatisfied(1, nowElapsed, state);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean setBatteryNotLowConstraintSatisfied(long nowElapsed, boolean state) {
        return setConstraintSatisfied(2, nowElapsed, state);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean setStorageNotLowConstraintSatisfied(long nowElapsed, boolean state) {
        return setConstraintSatisfied(8, nowElapsed, state);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean setPrefetchConstraintSatisfied(long nowElapsed, boolean state) {
        return setConstraintSatisfied(8388608, nowElapsed, state);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean setTimingDelayConstraintSatisfied(long nowElapsed, boolean state) {
        return setConstraintSatisfied(Integer.MIN_VALUE, nowElapsed, state);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean setDeadlineConstraintSatisfied(long nowElapsed, boolean state) {
        boolean z = false;
        if (setConstraintSatisfied(1073741824, nowElapsed, state)) {
            if (!this.job.isPeriodic() && hasDeadlineConstraint() && state) {
                z = true;
            }
            this.mReadyDeadlineSatisfied = z;
            return true;
        }
        return false;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean setIdleConstraintSatisfied(long nowElapsed, boolean state) {
        return setConstraintSatisfied(4, nowElapsed, state);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean setConnectivityConstraintSatisfied(long nowElapsed, boolean state) {
        return setConstraintSatisfied(268435456, nowElapsed, state);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean setContentTriggerConstraintSatisfied(long nowElapsed, boolean state) {
        return setConstraintSatisfied(67108864, nowElapsed, state);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean setDeviceNotDozingConstraintSatisfied(long nowElapsed, boolean state, boolean whitelisted) {
        this.appHasDozeExemption = whitelisted;
        boolean z = false;
        if (setConstraintSatisfied(33554432, nowElapsed, state)) {
            if (state || canRunInDoze()) {
                z = true;
            }
            this.mReadyNotDozing = z;
            return true;
        }
        return false;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean setBackgroundNotRestrictedConstraintSatisfied(long nowElapsed, boolean state, boolean isUserBgRestricted) {
        this.mIsUserBgRestricted = isUserBgRestricted;
        if (setConstraintSatisfied(4194304, nowElapsed, state)) {
            this.mReadyNotRestrictedInBg = state;
            return true;
        }
        return false;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean setQuotaConstraintSatisfied(long nowElapsed, boolean state) {
        if (setConstraintSatisfied(16777216, nowElapsed, state)) {
            this.mReadyWithinQuota = state;
            return true;
        }
        return false;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean setTareWealthConstraintSatisfied(long nowElapsed, boolean state) {
        if (setConstraintSatisfied(134217728, nowElapsed, state)) {
            this.mReadyTareWealth = state;
            return true;
        }
        return false;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean setExpeditedJobQuotaApproved(long nowElapsed, boolean state) {
        if (this.mExpeditedQuotaApproved == state) {
            return false;
        }
        boolean wasReady = !state && isReady();
        this.mExpeditedQuotaApproved = state;
        updateExpeditedDependencies();
        boolean isReady = isReady();
        if (wasReady && !isReady) {
            this.mReasonReadyToUnready = 10;
        } else if (!wasReady && isReady) {
            this.mReasonReadyToUnready = 0;
        }
        return true;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean setExpeditedJobTareApproved(long nowElapsed, boolean state) {
        if (this.mExpeditedTareApproved == state) {
            return false;
        }
        boolean wasReady = !state && isReady();
        this.mExpeditedTareApproved = state;
        updateExpeditedDependencies();
        boolean isReady = isReady();
        if (wasReady && !isReady) {
            this.mReasonReadyToUnready = 10;
        } else if (!wasReady && isReady) {
            this.mReasonReadyToUnready = 0;
        }
        return true;
    }

    private void updateExpeditedDependencies() {
        this.mReadyNotDozing = isConstraintSatisfied(33554432) || canRunInDoze();
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean setUidActive(boolean newActiveState) {
        if (newActiveState != this.uidActive) {
            this.uidActive = newActiveState;
            return true;
        }
        return false;
    }

    boolean setConstraintSatisfied(int constraint, long nowElapsed, boolean state) {
        boolean old = (this.satisfiedConstraints & constraint) != 0;
        if (old == state) {
            return false;
        }
        if (DEBUG) {
            Slog.v(TAG, "Constraint " + constraint + " is " + (!state ? "NOT " : "") + "satisfied for " + toShortString());
        }
        boolean wasReady = !state && isReady();
        int i = (this.satisfiedConstraints & (~constraint)) | (state ? constraint : 0);
        this.satisfiedConstraints = i;
        this.mSatisfiedConstraintsOfInterest = CONSTRAINTS_OF_INTEREST & i;
        int i2 = this.mDynamicConstraints;
        this.mReadyDynamicSatisfied = i2 != 0 && i2 == (i & i2);
        long[] jArr = this.mConstraintUpdatedTimesElapsed;
        int i3 = this.mConstraintChangeHistoryIndex;
        jArr[i3] = nowElapsed;
        this.mConstraintStatusHistory[i3] = i;
        this.mConstraintChangeHistoryIndex = (i3 + 1) % 10;
        boolean isReady = readinessStatusWithConstraint(constraint, state);
        if (wasReady && !isReady) {
            this.mReasonReadyToUnready = constraintToStopReason(constraint);
        } else if (!wasReady && isReady) {
            this.mReasonReadyToUnready = 0;
        }
        return true;
    }

    private int constraintToStopReason(int constraint) {
        switch (constraint) {
            case 1:
                return (this.requiredConstraints & constraint) != 0 ? 6 : 12;
            case 2:
                return (this.requiredConstraints & constraint) != 0 ? 5 : 12;
            case 4:
                return (this.requiredConstraints & constraint) != 0 ? 8 : 12;
            case 8:
                return 9;
            case 4194304:
                if (!this.mIsUserBgRestricted) {
                    return 4;
                }
                return 11;
            case 8388608:
                return 15;
            case 16777216:
            case 134217728:
                return 10;
            case 33554432:
                return 4;
            case 268435456:
                return 7;
            default:
                Slog.wtf(TAG, "Unsupported constraint (" + constraint + ") --stop reason mapping");
                return 0;
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean isConstraintSatisfied(int constraint) {
        return (this.satisfiedConstraints & constraint) != 0;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean isExpeditedQuotaApproved() {
        return this.mExpeditedQuotaApproved;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean clearTrackingController(int which) {
        int i = this.trackingControllers;
        if ((i & which) != 0) {
            this.trackingControllers = i & (~which);
            return true;
        }
        return false;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void setTrackingController(int which) {
        this.trackingControllers |= which;
    }

    public void disallowRunInBatterySaverAndDoze() {
        addDynamicConstraints(DYNAMIC_EXPEDITED_DEFERRAL_CONSTRAINTS);
    }

    private void addDynamicConstraints(int constraints) {
        if ((16777216 & constraints) != 0) {
            Slog.wtf(TAG, "Tried to set quota as a dynamic constraint");
            constraints &= -16777217;
        }
        if ((134217728 & constraints) != 0) {
            Slog.wtf(TAG, "Tried to set TARE as a dynamic constraint");
            constraints &= -134217729;
        }
        if (!hasConnectivityConstraint()) {
            constraints &= -268435457;
        }
        if (!hasContentTriggerConstraint()) {
            constraints &= -67108865;
        }
        int i = this.mDynamicConstraints | constraints;
        this.mDynamicConstraints = i;
        this.mReadyDynamicSatisfied = i != 0 && i == (this.satisfiedConstraints & i);
    }

    private void removeDynamicConstraints(int constraints) {
        int i = this.mDynamicConstraints & (~constraints);
        this.mDynamicConstraints = i;
        this.mReadyDynamicSatisfied = i != 0 && i == (this.satisfiedConstraints & i);
    }

    public long getLastSuccessfulRunTime() {
        return this.mLastSuccessfulRunTime;
    }

    public long getLastFailedRunTime() {
        return this.mLastFailedRunTime;
    }

    public boolean isReady() {
        return isReady(this.mSatisfiedConstraintsOfInterest);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean wouldBeReadyWithConstraint(int constraint) {
        return readinessStatusWithConstraint(constraint, true);
    }

    private boolean readinessStatusWithConstraint(int constraint, boolean value) {
        boolean oldValue = false;
        int satisfied = this.mSatisfiedConstraintsOfInterest;
        boolean z = true;
        switch (constraint) {
            case 4194304:
                oldValue = this.mReadyNotRestrictedInBg;
                this.mReadyNotRestrictedInBg = value;
                break;
            case 16777216:
                oldValue = this.mReadyWithinQuota;
                this.mReadyWithinQuota = value;
                break;
            case 33554432:
                oldValue = this.mReadyNotDozing;
                this.mReadyNotDozing = value;
                break;
            case 134217728:
                oldValue = this.mReadyTareWealth;
                this.mReadyTareWealth = value;
                break;
            case 1073741824:
                oldValue = this.mReadyDeadlineSatisfied;
                this.mReadyDeadlineSatisfied = value;
                break;
            default:
                if (value) {
                    satisfied |= constraint;
                } else {
                    satisfied &= ~constraint;
                }
                int i = this.mDynamicConstraints;
                this.mReadyDynamicSatisfied = i != 0 && i == (satisfied & i);
                break;
        }
        boolean toReturn = isReady(satisfied);
        switch (constraint) {
            case 4194304:
                this.mReadyNotRestrictedInBg = oldValue;
                break;
            case 16777216:
                this.mReadyWithinQuota = oldValue;
                break;
            case 33554432:
                this.mReadyNotDozing = oldValue;
                break;
            case 134217728:
                this.mReadyTareWealth = oldValue;
                break;
            case 1073741824:
                this.mReadyDeadlineSatisfied = oldValue;
                break;
            default:
                int i2 = this.mDynamicConstraints;
                if (i2 == 0 || i2 != (this.satisfiedConstraints & i2)) {
                    z = false;
                }
                this.mReadyDynamicSatisfied = z;
                break;
        }
        return toReturn;
    }

    private boolean isReady(int satisfiedConstraints) {
        if (((this.mReadyWithinQuota && this.mReadyTareWealth) || this.mReadyDynamicSatisfied || shouldTreatAsExpeditedJob()) && getEffectiveStandbyBucket() != 4 && this.mReadyNotDozing && this.mReadyNotRestrictedInBg && this.serviceInfo != null) {
            return this.mReadyDeadlineSatisfied || isConstraintsSatisfied(satisfiedConstraints);
        }
        return false;
    }

    public boolean areDynamicConstraintsSatisfied() {
        return this.mReadyDynamicSatisfied;
    }

    public boolean isConstraintsSatisfied() {
        return isConstraintsSatisfied(this.mSatisfiedConstraintsOfInterest);
    }

    private boolean isConstraintsSatisfied(int satisfiedConstraints) {
        int i = this.overrideState;
        if (i == 3) {
            return true;
        }
        int sat = satisfiedConstraints;
        if (i == 2) {
            sat |= this.requiredConstraints & SOFT_OVERRIDE_CONSTRAINTS;
        }
        int i2 = this.mRequiredConstraintsOfInterest;
        return (sat & i2) == i2;
    }

    public boolean matches(int uid, int jobId) {
        return this.job.getId() == jobId && this.callingUid == uid;
    }

    public String toString() {
        StringBuilder sb = new StringBuilder(128);
        sb.append("JobStatus{");
        sb.append(Integer.toHexString(System.identityHashCode(this)));
        sb.append(" #");
        UserHandle.formatUid(sb, this.callingUid);
        sb.append(SliceClientPermissions.SliceAuthority.DELIMITER);
        sb.append(this.job.getId());
        sb.append(' ');
        sb.append(this.batteryName);
        sb.append(" u=");
        sb.append(getUserId());
        sb.append(" s=");
        sb.append(getSourceUid());
        if (this.earliestRunTimeElapsedMillis != 0 || this.latestRunTimeElapsedMillis != NO_LATEST_RUNTIME) {
            long now = JobSchedulerService.sElapsedRealtimeClock.millis();
            sb.append(" TIME=");
            formatRunTime(sb, this.earliestRunTimeElapsedMillis, 0L, now);
            sb.append(":");
            formatRunTime(sb, this.latestRunTimeElapsedMillis, NO_LATEST_RUNTIME, now);
        }
        if (this.job.getRequiredNetwork() != null) {
            sb.append(" NET");
        }
        if (this.job.isRequireCharging()) {
            sb.append(" CHARGING");
        }
        if (this.job.isRequireBatteryNotLow()) {
            sb.append(" BATNOTLOW");
        }
        if (this.job.isRequireStorageNotLow()) {
            sb.append(" STORENOTLOW");
        }
        if (this.job.isRequireDeviceIdle()) {
            sb.append(" IDLE");
        }
        if (this.job.isPeriodic()) {
            sb.append(" PERIODIC");
        }
        if (this.job.isPersisted()) {
            sb.append(" PERSISTED");
        }
        if ((this.satisfiedConstraints & 33554432) == 0) {
            sb.append(" WAIT:DEV_NOT_DOZING");
        }
        if (this.job.getTriggerContentUris() != null) {
            sb.append(" URIS=");
            sb.append(Arrays.toString(this.job.getTriggerContentUris()));
        }
        if (this.numFailures != 0) {
            sb.append(" failures=");
            sb.append(this.numFailures);
        }
        if (isReady()) {
            sb.append(" READY");
        } else {
            sb.append(" satisfied:0x").append(Integer.toHexString(this.satisfiedConstraints));
            StringBuilder append = sb.append(" unsatisfied:0x");
            int i = this.satisfiedConstraints;
            int i2 = this.mRequiredConstraintsOfInterest;
            append.append(Integer.toHexString((i & i2) ^ i2));
        }
        sb.append("}");
        return sb.toString();
    }

    private void formatRunTime(PrintWriter pw, long runtime, long defaultValue, long now) {
        if (runtime == defaultValue) {
            pw.print("none");
        } else {
            TimeUtils.formatDuration(runtime - now, pw);
        }
    }

    private void formatRunTime(StringBuilder sb, long runtime, long defaultValue, long now) {
        if (runtime == defaultValue) {
            sb.append("none");
        } else {
            TimeUtils.formatDuration(runtime - now, sb);
        }
    }

    public String toShortString() {
        StringBuilder sb = new StringBuilder();
        sb.append(Integer.toHexString(System.identityHashCode(this)));
        sb.append(" #");
        UserHandle.formatUid(sb, this.callingUid);
        sb.append(SliceClientPermissions.SliceAuthority.DELIMITER);
        sb.append(this.job.getId());
        sb.append(' ');
        sb.append(this.batteryName);
        return sb.toString();
    }

    public String toShortStringExceptUniqueId() {
        return Integer.toHexString(System.identityHashCode(this)) + ' ' + this.batteryName;
    }

    public void writeToShortProto(ProtoOutputStream proto, long fieldId) {
        long token = proto.start(fieldId);
        proto.write(CompanionMessage.MESSAGE_ID, this.callingUid);
        proto.write(1120986464258L, this.job.getId());
        proto.write(1138166333443L, this.batteryName);
        proto.end(token);
    }

    void dumpConstraints(PrintWriter pw, int constraints) {
        if ((constraints & 1) != 0) {
            pw.print(" CHARGING");
        }
        if ((constraints & 2) != 0) {
            pw.print(" BATTERY_NOT_LOW");
        }
        if ((constraints & 8) != 0) {
            pw.print(" STORAGE_NOT_LOW");
        }
        if ((Integer.MIN_VALUE & constraints) != 0) {
            pw.print(" TIMING_DELAY");
        }
        if ((1073741824 & constraints) != 0) {
            pw.print(" DEADLINE");
        }
        if ((constraints & 4) != 0) {
            pw.print(" IDLE");
        }
        if ((268435456 & constraints) != 0) {
            pw.print(" CONNECTIVITY");
        }
        if ((67108864 & constraints) != 0) {
            pw.print(" CONTENT_TRIGGER");
        }
        if ((33554432 & constraints) != 0) {
            pw.print(" DEVICE_NOT_DOZING");
        }
        if ((4194304 & constraints) != 0) {
            pw.print(" BACKGROUND_NOT_RESTRICTED");
        }
        if ((8388608 & constraints) != 0) {
            pw.print(" PREFETCH");
        }
        if ((134217728 & constraints) != 0) {
            pw.print(" TARE_WEALTH");
        }
        if ((16777216 & constraints) != 0) {
            pw.print(" WITHIN_QUOTA");
        }
        if (constraints != 0) {
            pw.print(" [0x");
            pw.print(Integer.toHexString(constraints));
            pw.print("]");
        }
    }

    private int getProtoConstraint(int constraint) {
        switch (constraint) {
            case Integer.MIN_VALUE:
                return 4;
            case 1:
                return 1;
            case 2:
                return 2;
            case 4:
                return 6;
            case 8:
                return 3;
            case 4194304:
                return 11;
            case 16777216:
                return 10;
            case 33554432:
                return 9;
            case 67108864:
                return 8;
            case 268435456:
                return 7;
            case 1073741824:
                return 5;
            default:
                return 0;
        }
    }

    void dumpConstraints(ProtoOutputStream proto, long fieldId, int constraints) {
        if ((constraints & 1) != 0) {
            proto.write(fieldId, 1);
        }
        if ((constraints & 2) != 0) {
            proto.write(fieldId, 2);
        }
        if ((constraints & 8) != 0) {
            proto.write(fieldId, 3);
        }
        if ((Integer.MIN_VALUE & constraints) != 0) {
            proto.write(fieldId, 4);
        }
        if ((1073741824 & constraints) != 0) {
            proto.write(fieldId, 5);
        }
        if ((constraints & 4) != 0) {
            proto.write(fieldId, 6);
        }
        if ((268435456 & constraints) != 0) {
            proto.write(fieldId, 7);
        }
        if ((67108864 & constraints) != 0) {
            proto.write(fieldId, 8);
        }
        if ((33554432 & constraints) != 0) {
            proto.write(fieldId, 9);
        }
        if ((16777216 & constraints) != 0) {
            proto.write(fieldId, 10);
        }
        if ((4194304 & constraints) != 0) {
            proto.write(fieldId, 11);
        }
    }

    private void dumpJobWorkItem(IndentingPrintWriter pw, JobWorkItem work, int index) {
        pw.increaseIndent();
        pw.print("#");
        pw.print(index);
        pw.print(": #");
        pw.print(work.getWorkId());
        pw.print(" ");
        pw.print(work.getDeliveryCount());
        pw.print("x ");
        pw.println(work.getIntent());
        if (work.getGrants() != null) {
            pw.println("URI grants:");
            pw.increaseIndent();
            ((GrantedUriPermissions) work.getGrants()).dump(pw);
            pw.decreaseIndent();
        }
        pw.decreaseIndent();
    }

    private void dumpJobWorkItem(ProtoOutputStream proto, long fieldId, JobWorkItem work) {
        long token = proto.start(fieldId);
        proto.write(CompanionMessage.MESSAGE_ID, work.getWorkId());
        proto.write(1120986464258L, work.getDeliveryCount());
        if (work.getIntent() != null) {
            work.getIntent().dumpDebug(proto, 1146756268035L);
        }
        Object grants = work.getGrants();
        if (grants != null) {
            ((GrantedUriPermissions) grants).dump(proto, 1146756268036L);
        }
        proto.end(token);
    }

    String getBucketName() {
        return bucketName(this.standbyBucket);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static String bucketName(int standbyBucket) {
        switch (standbyBucket) {
            case 0:
                return "ACTIVE";
            case 1:
                return "WORKING_SET";
            case 2:
                return "FREQUENT";
            case 3:
                return "RARE";
            case 4:
                return "NEVER";
            case 5:
                return "RESTRICTED";
            case 6:
                return "EXEMPTED";
            default:
                return "Unknown: " + standbyBucket;
        }
    }

    @NeverCompile
    public void dump(IndentingPrintWriter pw, boolean full, long nowElapsed) {
        UserHandle.formatUid(pw, this.callingUid);
        pw.print(" tag=");
        pw.println(this.tag);
        pw.print("Source: uid=");
        UserHandle.formatUid(pw, getSourceUid());
        pw.print(" user=");
        pw.print(getSourceUserId());
        pw.print(" pkg=");
        pw.println(getSourcePackageName());
        if (full) {
            pw.println("JobInfo:");
            pw.increaseIndent();
            pw.print("Service: ");
            pw.println(this.job.getService().flattenToShortString());
            if (this.job.isPeriodic()) {
                pw.print("PERIODIC: interval=");
                TimeUtils.formatDuration(this.job.getIntervalMillis(), pw);
                pw.print(" flex=");
                TimeUtils.formatDuration(this.job.getFlexMillis(), pw);
                pw.println();
            }
            if (this.job.isPersisted()) {
                pw.println("PERSISTED");
            }
            if (this.job.getBias() != 0) {
                pw.print("Bias: ");
                pw.println(JobInfo.getBiasString(this.job.getBias()));
            }
            pw.print("Priority: ");
            pw.print(JobInfo.getPriorityString(this.job.getPriority()));
            int effectivePriority = getEffectivePriority();
            if (effectivePriority != this.job.getPriority()) {
                pw.print(" effective=");
                pw.print(JobInfo.getPriorityString(effectivePriority));
            }
            pw.println();
            if (this.job.getFlags() != 0) {
                pw.print("Flags: ");
                pw.println(Integer.toHexString(this.job.getFlags()));
            }
            if (getInternalFlags() != 0) {
                pw.print("Internal flags: ");
                pw.print(Integer.toHexString(getInternalFlags()));
                if ((getInternalFlags() & 1) != 0) {
                    pw.print(" HAS_FOREGROUND_EXEMPTION");
                }
                pw.println();
            }
            pw.print("Requires: charging=");
            pw.print(this.job.isRequireCharging());
            pw.print(" batteryNotLow=");
            pw.print(this.job.isRequireBatteryNotLow());
            pw.print(" deviceIdle=");
            pw.println(this.job.isRequireDeviceIdle());
            if (this.job.getTriggerContentUris() != null) {
                pw.println("Trigger content URIs:");
                pw.increaseIndent();
                for (int i = 0; i < this.job.getTriggerContentUris().length; i++) {
                    JobInfo.TriggerContentUri trig = this.job.getTriggerContentUris()[i];
                    pw.print(Integer.toHexString(trig.getFlags()));
                    pw.print(' ');
                    pw.println(trig.getUri());
                }
                pw.decreaseIndent();
                if (this.job.getTriggerContentUpdateDelay() >= 0) {
                    pw.print("Trigger update delay: ");
                    TimeUtils.formatDuration(this.job.getTriggerContentUpdateDelay(), pw);
                    pw.println();
                }
                if (this.job.getTriggerContentMaxDelay() >= 0) {
                    pw.print("Trigger max delay: ");
                    TimeUtils.formatDuration(this.job.getTriggerContentMaxDelay(), pw);
                    pw.println();
                }
                pw.print("Has media backup exemption", Boolean.valueOf(this.mHasMediaBackupExemption)).println();
            }
            if (this.job.getExtras() != null && !this.job.getExtras().isDefinitelyEmpty()) {
                pw.print("Extras: ");
                pw.println(this.job.getExtras().toShortString());
            }
            if (this.job.getTransientExtras() != null && !this.job.getTransientExtras().isDefinitelyEmpty()) {
                pw.print("Transient extras: ");
                pw.println(this.job.getTransientExtras().toShortString());
            }
            if (this.job.getClipData() != null) {
                pw.print("Clip data: ");
                StringBuilder b = new StringBuilder(128);
                b.append(this.job.getClipData());
                pw.println(b);
            }
            if (this.uriPerms != null) {
                pw.println("Granted URI permissions:");
                this.uriPerms.dump(pw);
            }
            if (this.job.getRequiredNetwork() != null) {
                pw.print("Network type: ");
                pw.println(this.job.getRequiredNetwork());
            }
            if (this.mTotalNetworkDownloadBytes != -1) {
                pw.print("Network download bytes: ");
                pw.println(this.mTotalNetworkDownloadBytes);
            }
            if (this.mTotalNetworkUploadBytes != -1) {
                pw.print("Network upload bytes: ");
                pw.println(this.mTotalNetworkUploadBytes);
            }
            if (this.mMinimumNetworkChunkBytes != -1) {
                pw.print("Minimum network chunk bytes: ");
                pw.println(this.mMinimumNetworkChunkBytes);
            }
            if (this.job.getMinLatencyMillis() != 0) {
                pw.print("Minimum latency: ");
                TimeUtils.formatDuration(this.job.getMinLatencyMillis(), pw);
                pw.println();
            }
            if (this.job.getMaxExecutionDelayMillis() != 0) {
                pw.print("Max execution delay: ");
                TimeUtils.formatDuration(this.job.getMaxExecutionDelayMillis(), pw);
                pw.println();
            }
            pw.print("Backoff: policy=");
            pw.print(this.job.getBackoffPolicy());
            pw.print(" initial=");
            TimeUtils.formatDuration(this.job.getInitialBackoffMillis(), pw);
            pw.println();
            if (this.job.hasEarlyConstraint()) {
                pw.println("Has early constraint");
            }
            if (this.job.hasLateConstraint()) {
                pw.println("Has late constraint");
            }
            pw.decreaseIndent();
        }
        pw.print("Required constraints:");
        dumpConstraints(pw, this.requiredConstraints);
        pw.println();
        pw.print("Dynamic constraints:");
        dumpConstraints(pw, this.mDynamicConstraints);
        pw.println();
        if (full) {
            pw.print("Satisfied constraints:");
            dumpConstraints(pw, this.satisfiedConstraints);
            pw.println();
            pw.print("Unsatisfied constraints:");
            dumpConstraints(pw, (this.requiredConstraints | 16777216 | 134217728) & (~this.satisfiedConstraints));
            pw.println();
            pw.println("Constraint history:");
            pw.increaseIndent();
            for (int h = 0; h < 10; h++) {
                int idx = (this.mConstraintChangeHistoryIndex + h) % 10;
                long j = this.mConstraintUpdatedTimesElapsed[idx];
                if (j != 0) {
                    TimeUtils.formatDuration(j, nowElapsed, pw);
                    pw.print(" =");
                    dumpConstraints(pw, this.mConstraintStatusHistory[idx]);
                    pw.println();
                }
            }
            pw.decreaseIndent();
            if (this.appHasDozeExemption) {
                pw.println("Doze whitelisted: true");
            }
            if (this.uidActive) {
                pw.println("Uid: active");
            }
            if (this.job.isExemptedFromAppStandby()) {
                pw.println("Is exempted from app standby");
            }
        }
        if (this.trackingControllers != 0) {
            pw.print("Tracking:");
            if ((this.trackingControllers & 1) != 0) {
                pw.print(" BATTERY");
            }
            if ((this.trackingControllers & 2) != 0) {
                pw.print(" CONNECTIVITY");
            }
            if ((this.trackingControllers & 4) != 0) {
                pw.print(" CONTENT");
            }
            if ((this.trackingControllers & 8) != 0) {
                pw.print(" IDLE");
            }
            if ((this.trackingControllers & 16) != 0) {
                pw.print(" STORAGE");
            }
            if ((32 & this.trackingControllers) != 0) {
                pw.print(" TIME");
            }
            if ((this.trackingControllers & 64) != 0) {
                pw.print(" QUOTA");
            }
            pw.println();
        }
        pw.println("Implicit constraints:");
        pw.increaseIndent();
        pw.print("readyNotDozing: ");
        pw.println(this.mReadyNotDozing);
        pw.print("readyNotRestrictedInBg: ");
        pw.println(this.mReadyNotRestrictedInBg);
        if (!this.job.isPeriodic() && hasDeadlineConstraint()) {
            pw.print("readyDeadlineSatisfied: ");
            pw.println(this.mReadyDeadlineSatisfied);
        }
        if (this.mDynamicConstraints != 0) {
            pw.print("readyDynamicSatisfied: ");
            pw.println(this.mReadyDynamicSatisfied);
        }
        pw.print("readyComponentEnabled: ");
        pw.println(this.serviceInfo != null);
        if ((getFlags() & 16) != 0) {
            pw.print("expeditedQuotaApproved: ");
            pw.print(this.mExpeditedQuotaApproved);
            pw.print(" expeditedTareApproved: ");
            pw.print(this.mExpeditedTareApproved);
            pw.print(" (started as EJ: ");
            pw.print(this.startedAsExpeditedJob);
            pw.println(")");
        }
        pw.decreaseIndent();
        if (this.changedAuthorities != null) {
            pw.println("Changed authorities:");
            pw.increaseIndent();
            for (int i2 = 0; i2 < this.changedAuthorities.size(); i2++) {
                pw.println(this.changedAuthorities.valueAt(i2));
            }
            pw.decreaseIndent();
        }
        if (this.changedUris != null) {
            pw.println("Changed URIs:");
            pw.increaseIndent();
            for (int i3 = 0; i3 < this.changedUris.size(); i3++) {
                pw.println(this.changedUris.valueAt(i3));
            }
            pw.decreaseIndent();
        }
        if (this.network != null) {
            pw.print("Network: ");
            pw.println(this.network);
        }
        ArrayList<JobWorkItem> arrayList = this.pendingWork;
        if (arrayList != null && arrayList.size() > 0) {
            pw.println("Pending work:");
            for (int i4 = 0; i4 < this.pendingWork.size(); i4++) {
                dumpJobWorkItem(pw, this.pendingWork.get(i4), i4);
            }
        }
        ArrayList<JobWorkItem> arrayList2 = this.executingWork;
        if (arrayList2 != null && arrayList2.size() > 0) {
            pw.println("Executing work:");
            for (int i5 = 0; i5 < this.executingWork.size(); i5++) {
                dumpJobWorkItem(pw, this.executingWork.get(i5), i5);
            }
        }
        pw.print("Standby bucket: ");
        pw.println(getBucketName());
        pw.increaseIndent();
        if (this.whenStandbyDeferred != 0) {
            pw.print("Deferred since: ");
            TimeUtils.formatDuration(this.whenStandbyDeferred, nowElapsed, pw);
            pw.println();
        }
        if (this.mFirstForceBatchedTimeElapsed != 0) {
            pw.print("Time since first force batch attempt: ");
            TimeUtils.formatDuration(this.mFirstForceBatchedTimeElapsed, nowElapsed, pw);
            pw.println();
        }
        pw.decreaseIndent();
        pw.print("Enqueue time: ");
        TimeUtils.formatDuration(this.enqueueTime, nowElapsed, pw);
        pw.println();
        pw.print("Run time: earliest=");
        formatRunTime((PrintWriter) pw, this.earliestRunTimeElapsedMillis, 0L, nowElapsed);
        pw.print(", latest=");
        formatRunTime((PrintWriter) pw, this.latestRunTimeElapsedMillis, NO_LATEST_RUNTIME, nowElapsed);
        pw.print(", original latest=");
        formatRunTime((PrintWriter) pw, this.mOriginalLatestRunTimeElapsedMillis, NO_LATEST_RUNTIME, nowElapsed);
        pw.println();
        if (this.numFailures != 0) {
            pw.print("Num failures: ");
            pw.println(this.numFailures);
        }
        if (this.mLastSuccessfulRunTime != 0) {
            pw.print("Last successful run: ");
            pw.println(formatTime(this.mLastSuccessfulRunTime));
        }
        if (this.mLastFailedRunTime != 0) {
            pw.print("Last failed run: ");
            pw.println(formatTime(this.mLastFailedRunTime));
        }
    }

    private static CharSequence formatTime(long time) {
        return DateFormat.format("yyyy-MM-dd HH:mm:ss", time);
    }

    public void dump(ProtoOutputStream proto, long fieldId, boolean full, long elapsedRealtimeMillis) {
        long token = proto.start(fieldId);
        int i = this.callingUid;
        long j = CompanionMessage.MESSAGE_ID;
        proto.write(CompanionMessage.MESSAGE_ID, i);
        proto.write(1138166333442L, this.tag);
        proto.write(1120986464259L, getSourceUid());
        proto.write(1120986464260L, getSourceUserId());
        proto.write(1138166333445L, getSourcePackageName());
        if (full) {
            long jiToken = proto.start(1146756268038L);
            this.job.getService().dumpDebug(proto, 1146756268033L);
            proto.write(1133871366146L, this.job.isPeriodic());
            proto.write(1112396529667L, this.job.getIntervalMillis());
            proto.write(1112396529668L, this.job.getFlexMillis());
            proto.write(1133871366149L, this.job.isPersisted());
            proto.write(1172526071814L, this.job.getBias());
            proto.write(1120986464263L, this.job.getFlags());
            proto.write(1112396529688L, getInternalFlags());
            proto.write(1133871366152L, this.job.isRequireCharging());
            proto.write(1133871366153L, this.job.isRequireBatteryNotLow());
            proto.write(1133871366154L, this.job.isRequireDeviceIdle());
            if (this.job.getTriggerContentUris() != null) {
                int i2 = 0;
                while (i2 < this.job.getTriggerContentUris().length) {
                    long tcuToken = proto.start(2246267895819L);
                    JobInfo.TriggerContentUri trig = this.job.getTriggerContentUris()[i2];
                    proto.write(j, trig.getFlags());
                    Uri u = trig.getUri();
                    if (u != null) {
                        proto.write(1138166333442L, u.toString());
                    }
                    proto.end(tcuToken);
                    i2++;
                    j = CompanionMessage.MESSAGE_ID;
                }
                if (this.job.getTriggerContentUpdateDelay() >= 0) {
                    proto.write(1112396529676L, this.job.getTriggerContentUpdateDelay());
                }
                if (this.job.getTriggerContentMaxDelay() >= 0) {
                    proto.write(1112396529677L, this.job.getTriggerContentMaxDelay());
                }
            }
            if (this.job.getExtras() != null && !this.job.getExtras().isDefinitelyEmpty()) {
                this.job.getExtras().dumpDebug(proto, 1146756268046L);
            }
            if (this.job.getTransientExtras() != null && !this.job.getTransientExtras().isDefinitelyEmpty()) {
                this.job.getTransientExtras().dumpDebug(proto, 1146756268047L);
            }
            if (this.job.getClipData() != null) {
                this.job.getClipData().dumpDebug(proto, 1146756268048L);
            }
            GrantedUriPermissions grantedUriPermissions = this.uriPerms;
            if (grantedUriPermissions != null) {
                grantedUriPermissions.dump(proto, 1146756268049L);
            }
            long j2 = this.mTotalNetworkDownloadBytes;
            if (j2 != -1) {
                proto.write(1112396529689L, j2);
            }
            long j3 = this.mTotalNetworkUploadBytes;
            if (j3 != -1) {
                proto.write(1112396529690L, j3);
            }
            proto.write(1112396529684L, this.job.getMinLatencyMillis());
            proto.write(1112396529685L, this.job.getMaxExecutionDelayMillis());
            long bpToken = proto.start(1146756268054L);
            proto.write(1159641169921L, this.job.getBackoffPolicy());
            proto.write(1112396529666L, this.job.getInitialBackoffMillis());
            proto.end(bpToken);
            proto.write(1133871366167L, this.job.hasEarlyConstraint());
            proto.write(1133871366168L, this.job.hasLateConstraint());
            proto.end(jiToken);
        }
        dumpConstraints(proto, 2259152797703L, this.requiredConstraints);
        dumpConstraints(proto, 2259152797727L, this.mDynamicConstraints);
        if (full) {
            dumpConstraints(proto, 2259152797704L, this.satisfiedConstraints);
            dumpConstraints(proto, 2259152797705L, (this.requiredConstraints | 16777216) & (~this.satisfiedConstraints));
            proto.write(1133871366154L, this.appHasDozeExemption);
            proto.write(1133871366170L, this.uidActive);
            proto.write(1133871366171L, this.job.isExemptedFromAppStandby());
        }
        if ((this.trackingControllers & 1) != 0) {
            proto.write(2259152797707L, 0);
        }
        if ((this.trackingControllers & 2) != 0) {
            proto.write(2259152797707L, 1);
        }
        if ((this.trackingControllers & 4) != 0) {
            proto.write(2259152797707L, 2);
        }
        if ((this.trackingControllers & 8) != 0) {
            proto.write(2259152797707L, 3);
        }
        if ((this.trackingControllers & 16) != 0) {
            proto.write(2259152797707L, 4);
        }
        if ((this.trackingControllers & 32) != 0) {
            proto.write(2259152797707L, 5);
        }
        if ((this.trackingControllers & 64) != 0) {
            proto.write(2259152797707L, 6);
        }
        long icToken = proto.start(1146756268057L);
        proto.write(1133871366145L, this.mReadyNotDozing);
        proto.write(1133871366146L, this.mReadyNotRestrictedInBg);
        proto.write(1133871366147L, this.mReadyDynamicSatisfied);
        proto.end(icToken);
        if (this.changedAuthorities != null) {
            for (int k = 0; k < this.changedAuthorities.size(); k++) {
                proto.write(2237677961228L, this.changedAuthorities.valueAt(k));
            }
        }
        if (this.changedUris != null) {
            for (int i3 = 0; i3 < this.changedUris.size(); i3++) {
                proto.write(2237677961229L, this.changedUris.valueAt(i3).toString());
            }
        }
        if (this.pendingWork != null) {
            for (int i4 = 0; i4 < this.pendingWork.size(); i4++) {
                dumpJobWorkItem(proto, 2246267895823L, this.pendingWork.get(i4));
            }
        }
        if (this.executingWork != null) {
            for (int i5 = 0; i5 < this.executingWork.size(); i5++) {
                dumpJobWorkItem(proto, 2246267895824L, this.executingWork.get(i5));
            }
        }
        proto.write(1159641169937L, this.standbyBucket);
        proto.write(1112396529682L, elapsedRealtimeMillis - this.enqueueTime);
        long j4 = this.whenStandbyDeferred;
        proto.write(1112396529692L, j4 == 0 ? 0L : elapsedRealtimeMillis - j4);
        long j5 = this.mFirstForceBatchedTimeElapsed;
        proto.write(1112396529693L, j5 == 0 ? 0L : elapsedRealtimeMillis - j5);
        long j6 = this.earliestRunTimeElapsedMillis;
        if (j6 == 0) {
            proto.write(1176821039123L, 0);
        } else {
            proto.write(1176821039123L, j6 - elapsedRealtimeMillis);
        }
        long j7 = this.latestRunTimeElapsedMillis;
        if (j7 == NO_LATEST_RUNTIME) {
            proto.write(1176821039124L, 0);
        } else {
            proto.write(1176821039124L, j7 - elapsedRealtimeMillis);
        }
        proto.write(1116691496990L, this.mOriginalLatestRunTimeElapsedMillis);
        proto.write(1120986464277L, this.numFailures);
        proto.write(1112396529686L, this.mLastSuccessfulRunTime);
        proto.write(1112396529687L, this.mLastFailedRunTime);
        proto.end(token);
    }
}
