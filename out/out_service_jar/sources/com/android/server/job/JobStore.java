package com.android.server.job;

import android.app.job.JobInfo;
import android.content.ComponentName;
import android.content.Context;
import android.net.NetworkRequest;
import android.os.Environment;
import android.os.Handler;
import android.os.PersistableBundle;
import android.os.SystemClock;
import android.os.UserHandle;
import android.text.TextUtils;
import android.text.format.DateUtils;
import android.util.ArraySet;
import android.util.AtomicFile;
import android.util.Pair;
import android.util.Slog;
import android.util.SparseArray;
import android.util.SystemConfigFileCommitEventLogger;
import android.util.TypedXmlPullParser;
import android.util.TypedXmlSerializer;
import android.util.Xml;
import com.android.internal.util.jobs.ArrayUtils;
import com.android.internal.util.jobs.BitUtils;
import com.android.server.IoThread;
import com.android.server.am.HostingRecord;
import com.android.server.content.SyncJobService;
import com.android.server.job.JobSchedulerInternal;
import com.android.server.job.JobStore;
import com.android.server.job.controllers.JobStatus;
import com.android.server.net.watchlist.WatchlistLoggingHandler;
import com.android.server.pm.PackageManagerService;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.StringJoiner;
import java.util.function.Consumer;
import java.util.function.Predicate;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlSerializer;
/* loaded from: classes.dex */
public final class JobStore {
    private static final int JOBS_FILE_VERSION = 1;
    private static final long JOB_PERSIST_DELAY = 2000;
    private static final String TAG = "JobStore";
    private static final String XML_TAG_EXTRAS = "extras";
    private static final String XML_TAG_ONEOFF = "one-off";
    private static final String XML_TAG_PARAMS_CONSTRAINTS = "constraints";
    private static final String XML_TAG_PERIODIC = "periodic";
    private static JobStore sSingleton;
    final Context mContext;
    private final SystemConfigFileCommitEventLogger mEventLogger;
    final JobSet mJobSet;
    private final AtomicFile mJobsFile;
    final Object mLock;
    private boolean mRtcGood;
    private boolean mWriteInProgress;
    private boolean mWriteScheduled;
    private final long mXmlTimestamp;
    private static final boolean DEBUG = JobSchedulerService.DEBUG;
    private static final Object sSingletonLock = new Object();
    private final Handler mIoHandler = IoThread.getHandler();
    private JobSchedulerInternal.JobStorePersistStats mPersistInfo = new JobSchedulerInternal.JobStorePersistStats();
    private final Runnable mWriteRunnable = new AnonymousClass1();
    final Object mWriteScheduleLock = new Object();

    /* JADX INFO: Access modifiers changed from: package-private */
    public static JobStore initAndGet(JobSchedulerService jobManagerService) {
        JobStore jobStore;
        synchronized (sSingletonLock) {
            if (sSingleton == null) {
                sSingleton = new JobStore(jobManagerService.getContext(), jobManagerService.getLock(), Environment.getDataDirectory());
            }
            jobStore = sSingleton;
        }
        return jobStore;
    }

    public static JobStore initAndGetForTesting(Context context, File dataDir) {
        JobStore jobStoreUnderTest = new JobStore(context, new Object(), dataDir);
        jobStoreUnderTest.clearForTesting();
        return jobStoreUnderTest;
    }

    private JobStore(Context context, Object lock, File dataDir) {
        this.mLock = lock;
        this.mContext = context;
        File systemDir = new File(dataDir, HostingRecord.HOSTING_TYPE_SYSTEM);
        File jobDir = new File(systemDir, "job");
        jobDir.mkdirs();
        SystemConfigFileCommitEventLogger systemConfigFileCommitEventLogger = new SystemConfigFileCommitEventLogger("jobs");
        this.mEventLogger = systemConfigFileCommitEventLogger;
        AtomicFile atomicFile = new AtomicFile(new File(jobDir, "jobs.xml"), systemConfigFileCommitEventLogger);
        this.mJobsFile = atomicFile;
        JobSet jobSet = new JobSet();
        this.mJobSet = jobSet;
        long lastModifiedTime = atomicFile.getLastModifiedTime();
        this.mXmlTimestamp = lastModifiedTime;
        boolean z = JobSchedulerService.sSystemClock.millis() > lastModifiedTime;
        this.mRtcGood = z;
        readJobMapFromDisk(jobSet, z);
    }

    public boolean jobTimesInflatedValid() {
        return this.mRtcGood;
    }

    public boolean clockNowValidToInflate(long now) {
        return now >= this.mXmlTimestamp;
    }

    public void getRtcCorrectedJobsLocked(final ArrayList<JobStatus> toAdd, final ArrayList<JobStatus> toRemove) {
        final long elapsedNow = JobSchedulerService.sElapsedRealtimeClock.millis();
        forEachJob(new Consumer() { // from class: com.android.server.job.JobStore$$ExternalSyntheticLambda0
            @Override // java.util.function.Consumer
            public final void accept(Object obj) {
                JobStore.lambda$getRtcCorrectedJobsLocked$0(elapsedNow, toAdd, toRemove, (JobStatus) obj);
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static /* synthetic */ void lambda$getRtcCorrectedJobsLocked$0(long elapsedNow, ArrayList toAdd, ArrayList toRemove, JobStatus job) {
        Pair<Long, Long> utcTimes = job.getPersistedUtcTimes();
        if (utcTimes != null) {
            Pair<Long, Long> elapsedRuntimes = convertRtcBoundsToElapsed(utcTimes, elapsedNow);
            JobStatus newJob = new JobStatus(job, ((Long) elapsedRuntimes.first).longValue(), ((Long) elapsedRuntimes.second).longValue(), 0, job.getLastSuccessfulRunTime(), job.getLastFailedRunTime());
            newJob.prepareLocked();
            toAdd.add(newJob);
            toRemove.add(job);
        }
    }

    public boolean add(JobStatus jobStatus) {
        boolean replaced = this.mJobSet.remove(jobStatus);
        this.mJobSet.add(jobStatus);
        if (jobStatus.isPersisted()) {
            maybeWriteStatusToDiskAsync();
        }
        if (DEBUG) {
            Slog.d(TAG, "Added job status to store: " + jobStatus);
        }
        return replaced;
    }

    public void addForTesting(JobStatus jobStatus) {
        this.mJobSet.add(jobStatus);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean containsJob(JobStatus jobStatus) {
        return this.mJobSet.contains(jobStatus);
    }

    public int size() {
        return this.mJobSet.size();
    }

    public JobSchedulerInternal.JobStorePersistStats getPersistStats() {
        return this.mPersistInfo;
    }

    public int countJobsForUid(int uid) {
        return this.mJobSet.countJobsForUid(uid);
    }

    public boolean remove(JobStatus jobStatus, boolean removeFromPersisted) {
        boolean removed = this.mJobSet.remove(jobStatus);
        if (!removed) {
            if (DEBUG) {
                Slog.d(TAG, "Couldn't remove job: didn't exist: " + jobStatus);
                return false;
            }
            return false;
        }
        if (removeFromPersisted && jobStatus.isPersisted()) {
            maybeWriteStatusToDiskAsync();
        }
        return removed;
    }

    public void removeJobsOfUnlistedUsers(int[] keepUserIds) {
        this.mJobSet.removeJobsOfUnlistedUsers(keepUserIds);
    }

    public void clear() {
        this.mJobSet.clear();
        maybeWriteStatusToDiskAsync();
    }

    public void clearForTesting() {
        this.mJobSet.clear();
    }

    public List<JobStatus> getJobsByUser(int userHandle) {
        return this.mJobSet.getJobsByUser(userHandle);
    }

    public List<JobStatus> getJobsByUid(int uid) {
        return this.mJobSet.getJobsByUid(uid);
    }

    public JobStatus getJobByUidAndJobId(int uid, int jobId) {
        return this.mJobSet.get(uid, jobId);
    }

    public void forEachJob(Consumer<JobStatus> functor) {
        this.mJobSet.forEachJob((Predicate<JobStatus>) null, functor);
    }

    public void forEachJob(Predicate<JobStatus> filterPredicate, Consumer<JobStatus> functor) {
        this.mJobSet.forEachJob(filterPredicate, functor);
    }

    public void forEachJob(int uid, Consumer<JobStatus> functor) {
        this.mJobSet.forEachJob(uid, functor);
    }

    public void forEachJobForSourceUid(int sourceUid, Consumer<JobStatus> functor) {
        this.mJobSet.forEachJobForSourceUid(sourceUid, functor);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void maybeWriteStatusToDiskAsync() {
        synchronized (this.mWriteScheduleLock) {
            if (!this.mWriteScheduled) {
                if (DEBUG) {
                    Slog.v(TAG, "Scheduling persist of jobs to disk.");
                }
                this.mIoHandler.postDelayed(this.mWriteRunnable, JOB_PERSIST_DELAY);
                this.mWriteScheduled = true;
            }
        }
    }

    public void readJobMapFromDisk(JobSet jobSet, boolean rtcGood) {
        new ReadJobMapFromDiskRunnable(jobSet, rtcGood).run();
    }

    public void writeStatusToDiskForTesting() {
        synchronized (this.mWriteScheduleLock) {
            if (this.mWriteScheduled) {
                throw new IllegalStateException("An asynchronous write is already scheduled.");
            }
            this.mWriteScheduled = true;
            this.mWriteRunnable.run();
        }
    }

    public boolean waitForWriteToCompleteForTesting(long maxWaitMillis) {
        long start = SystemClock.uptimeMillis();
        long end = start + maxWaitMillis;
        synchronized (this.mWriteScheduleLock) {
            while (true) {
                if (!this.mWriteScheduled && !this.mWriteInProgress) {
                    break;
                }
                long now = SystemClock.uptimeMillis();
                if (now >= end) {
                    return false;
                }
                try {
                    this.mWriteScheduleLock.wait((now - start) + maxWaitMillis);
                } catch (InterruptedException e) {
                }
            }
            return true;
        }
    }

    static String intArrayToString(int[] values) {
        StringJoiner sj = new StringJoiner(",");
        for (int value : values) {
            sj.add(String.valueOf(value));
        }
        return sj.toString();
    }

    static int[] stringToIntArray(String str) {
        if (TextUtils.isEmpty(str)) {
            return new int[0];
        }
        String[] arr = str.split(",");
        int[] values = new int[arr.length];
        for (int i = 0; i < arr.length; i++) {
            values[i] = Integer.parseInt(arr[i]);
        }
        return values;
    }

    /* renamed from: com.android.server.job.JobStore$1  reason: invalid class name */
    /* loaded from: classes.dex */
    class AnonymousClass1 implements Runnable {
        AnonymousClass1() {
        }

        @Override // java.lang.Runnable
        public void run() {
            long startElapsed = JobSchedulerService.sElapsedRealtimeClock.millis();
            final List<JobStatus> storeCopy = new ArrayList<>();
            synchronized (JobStore.this.mWriteScheduleLock) {
                JobStore.this.mWriteScheduled = false;
                if (JobStore.this.mWriteInProgress) {
                    JobStore.this.maybeWriteStatusToDiskAsync();
                    return;
                }
                JobStore.this.mWriteInProgress = true;
                synchronized (JobStore.this.mLock) {
                    JobStore.this.mJobSet.forEachJob((Predicate<JobStatus>) null, new Consumer() { // from class: com.android.server.job.JobStore$1$$ExternalSyntheticLambda0
                        @Override // java.util.function.Consumer
                        public final void accept(Object obj) {
                            JobStore.AnonymousClass1.lambda$run$0(storeCopy, (JobStatus) obj);
                        }
                    });
                }
                writeJobsMapImpl(storeCopy);
                if (JobStore.DEBUG) {
                    Slog.v(JobStore.TAG, "Finished writing, took " + (JobSchedulerService.sElapsedRealtimeClock.millis() - startElapsed) + "ms");
                }
                synchronized (JobStore.this.mWriteScheduleLock) {
                    JobStore.this.mWriteInProgress = false;
                    JobStore.this.mWriteScheduleLock.notifyAll();
                }
            }
        }

        /* JADX INFO: Access modifiers changed from: package-private */
        public static /* synthetic */ void lambda$run$0(List storeCopy, JobStatus job) {
            if (job.isPersisted()) {
                storeCopy.add(new JobStatus(job));
            }
        }

        private void writeJobsMapImpl(List<JobStatus> jobList) {
            int numJobs = 0;
            int numSystemJobs = 0;
            int numSyncJobs = 0;
            JobStore.this.mEventLogger.setStartTime(SystemClock.uptimeMillis());
            try {
                try {
                    try {
                        FileOutputStream fos = JobStore.this.mJobsFile.startWrite();
                        try {
                            TypedXmlSerializer out = Xml.resolveSerializer(fos);
                            out.startDocument((String) null, true);
                            out.setFeature("http://xmlpull.org/v1/doc/features.html#indent-output", true);
                            out.startTag((String) null, "job-info");
                            out.attribute((String) null, "version", Integer.toString(1));
                            for (int i = 0; i < jobList.size(); i++) {
                                JobStatus jobStatus = jobList.get(i);
                                if (JobStore.DEBUG) {
                                    Slog.d(JobStore.TAG, "Saving job " + jobStatus.getJobId());
                                }
                                out.startTag((String) null, "job");
                                addAttributesToJobTag(out, jobStatus);
                                writeConstraintsToXml(out, jobStatus);
                                writeExecutionCriteriaToXml(out, jobStatus);
                                writeBundleToXml(jobStatus.getJob().getExtras(), out);
                                out.endTag((String) null, "job");
                                numJobs++;
                                if (jobStatus.getUid() == 1000) {
                                    numSystemJobs++;
                                    if (JobStore.isSyncJob(jobStatus)) {
                                        numSyncJobs++;
                                    }
                                }
                            }
                            out.endTag((String) null, "job-info");
                            out.endDocument();
                            JobStore.this.mJobsFile.finishWrite(fos);
                            if (fos != null) {
                                fos.close();
                            }
                        } catch (Throwable th) {
                            if (fos != null) {
                                try {
                                    fos.close();
                                } catch (Throwable th2) {
                                    th.addSuppressed(th2);
                                }
                            }
                            throw th;
                        }
                    } finally {
                        JobStore.this.mPersistInfo.countAllJobsSaved = 0;
                        JobStore.this.mPersistInfo.countSystemServerJobsSaved = 0;
                        JobStore.this.mPersistInfo.countSystemSyncManagerJobsSaved = 0;
                    }
                } catch (IOException e) {
                    if (JobStore.DEBUG) {
                        Slog.v(JobStore.TAG, "Error writing out job data.", e);
                    }
                }
            } catch (XmlPullParserException e2) {
                if (JobStore.DEBUG) {
                    Slog.d(JobStore.TAG, "Error persisting bundle.", e2);
                }
            }
        }

        private void addAttributesToJobTag(XmlSerializer out, JobStatus jobStatus) throws IOException {
            out.attribute(null, "jobid", Integer.toString(jobStatus.getJobId()));
            out.attribute(null, "package", jobStatus.getServiceComponent().getPackageName());
            out.attribute(null, "class", jobStatus.getServiceComponent().getClassName());
            if (jobStatus.getSourcePackageName() != null) {
                out.attribute(null, "sourcePackageName", jobStatus.getSourcePackageName());
            }
            if (jobStatus.getSourceTag() != null) {
                out.attribute(null, "sourceTag", jobStatus.getSourceTag());
            }
            out.attribute(null, "sourceUserId", String.valueOf(jobStatus.getSourceUserId()));
            out.attribute(null, WatchlistLoggingHandler.WatchlistEventKeys.UID, Integer.toString(jobStatus.getUid()));
            out.attribute(null, "bias", String.valueOf(jobStatus.getBias()));
            out.attribute(null, "priority", String.valueOf(jobStatus.getJob().getPriority()));
            out.attribute(null, "flags", String.valueOf(jobStatus.getFlags()));
            if (jobStatus.getInternalFlags() != 0) {
                out.attribute(null, "internalFlags", String.valueOf(jobStatus.getInternalFlags()));
            }
            out.attribute(null, "lastSuccessfulRunTime", String.valueOf(jobStatus.getLastSuccessfulRunTime()));
            out.attribute(null, "lastFailedRunTime", String.valueOf(jobStatus.getLastFailedRunTime()));
        }

        private void writeBundleToXml(PersistableBundle extras, XmlSerializer out) throws IOException, XmlPullParserException {
            out.startTag(null, JobStore.XML_TAG_EXTRAS);
            PersistableBundle extrasCopy = deepCopyBundle(extras, 10);
            extrasCopy.saveToXml(out);
            out.endTag(null, JobStore.XML_TAG_EXTRAS);
        }

        private PersistableBundle deepCopyBundle(PersistableBundle bundle, int maxDepth) {
            if (maxDepth <= 0) {
                return null;
            }
            PersistableBundle copy = (PersistableBundle) bundle.clone();
            Set<String> keySet = bundle.keySet();
            for (String key : keySet) {
                Object o = copy.get(key);
                if (o instanceof PersistableBundle) {
                    PersistableBundle bCopy = deepCopyBundle((PersistableBundle) o, maxDepth - 1);
                    copy.putPersistableBundle(key, bCopy);
                }
            }
            return copy;
        }

        private void writeConstraintsToXml(XmlSerializer out, JobStatus jobStatus) throws IOException {
            out.startTag(null, JobStore.XML_TAG_PARAMS_CONSTRAINTS);
            if (jobStatus.hasConnectivityConstraint()) {
                NetworkRequest network = jobStatus.getJob().getRequiredNetwork();
                out.attribute(null, "net-capabilities-csv", JobStore.intArrayToString(network.getCapabilities()));
                out.attribute(null, "net-forbidden-capabilities-csv", JobStore.intArrayToString(network.getForbiddenCapabilities()));
                out.attribute(null, "net-transport-types-csv", JobStore.intArrayToString(network.getTransportTypes()));
            }
            if (jobStatus.hasIdleConstraint()) {
                out.attribute(null, "idle", Boolean.toString(true));
            }
            if (jobStatus.hasChargingConstraint()) {
                out.attribute(null, "charging", Boolean.toString(true));
            }
            if (jobStatus.hasBatteryNotLowConstraint()) {
                out.attribute(null, "battery-not-low", Boolean.toString(true));
            }
            if (jobStatus.hasStorageNotLowConstraint()) {
                out.attribute(null, "storage-not-low", Boolean.toString(true));
            }
            out.endTag(null, JobStore.XML_TAG_PARAMS_CONSTRAINTS);
        }

        private void writeExecutionCriteriaToXml(XmlSerializer out, JobStatus jobStatus) throws IOException {
            long delayWallclock;
            long deadlineWallclock;
            JobInfo job = jobStatus.getJob();
            if (jobStatus.getJob().isPeriodic()) {
                out.startTag(null, JobStore.XML_TAG_PERIODIC);
                out.attribute(null, "period", Long.toString(job.getIntervalMillis()));
                out.attribute(null, "flex", Long.toString(job.getFlexMillis()));
            } else {
                out.startTag(null, JobStore.XML_TAG_ONEOFF);
            }
            Pair<Long, Long> utcJobTimes = jobStatus.getPersistedUtcTimes();
            if (JobStore.DEBUG && utcJobTimes != null) {
                Slog.i(JobStore.TAG, "storing original UTC timestamps for " + jobStatus);
            }
            long nowRTC = JobSchedulerService.sSystemClock.millis();
            long nowElapsed = JobSchedulerService.sElapsedRealtimeClock.millis();
            if (jobStatus.hasDeadlineConstraint()) {
                if (utcJobTimes == null) {
                    deadlineWallclock = (jobStatus.getLatestRunTimeElapsed() - nowElapsed) + nowRTC;
                } else {
                    deadlineWallclock = ((Long) utcJobTimes.second).longValue();
                }
                out.attribute(null, "deadline", Long.toString(deadlineWallclock));
            }
            if (jobStatus.hasTimingDelayConstraint()) {
                if (utcJobTimes == null) {
                    delayWallclock = (jobStatus.getEarliestRunTime() - nowElapsed) + nowRTC;
                } else {
                    delayWallclock = ((Long) utcJobTimes.first).longValue();
                }
                out.attribute(null, "delay", Long.toString(delayWallclock));
            }
            if (jobStatus.getJob().getInitialBackoffMillis() != 30000 || jobStatus.getJob().getBackoffPolicy() != 1) {
                out.attribute(null, "backoff-policy", Integer.toString(job.getBackoffPolicy()));
                out.attribute(null, "initial-backoff", Long.toString(job.getInitialBackoffMillis()));
            }
            if (job.isPeriodic()) {
                out.endTag(null, JobStore.XML_TAG_PERIODIC);
            } else {
                out.endTag(null, JobStore.XML_TAG_ONEOFF);
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public static Pair<Long, Long> convertRtcBoundsToElapsed(Pair<Long, Long> rtcTimes, long nowElapsed) {
        long earliest;
        long nowWallclock = JobSchedulerService.sSystemClock.millis();
        if (((Long) rtcTimes.first).longValue() > 0) {
            earliest = Math.max(((Long) rtcTimes.first).longValue() - nowWallclock, 0L) + nowElapsed;
        } else {
            earliest = 0;
        }
        long longValue = ((Long) rtcTimes.second).longValue();
        long j = JobStatus.NO_LATEST_RUNTIME;
        if (longValue < JobStatus.NO_LATEST_RUNTIME) {
            j = nowElapsed + Math.max(((Long) rtcTimes.second).longValue() - nowWallclock, 0L);
        }
        long latest = j;
        return Pair.create(Long.valueOf(earliest), Long.valueOf(latest));
    }

    /* JADX INFO: Access modifiers changed from: private */
    public static boolean isSyncJob(JobStatus status) {
        return SyncJobService.class.getName().equals(status.getServiceComponent().getClassName());
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes.dex */
    public final class ReadJobMapFromDiskRunnable implements Runnable {
        private final JobSet jobSet;
        private final boolean rtcGood;

        ReadJobMapFromDiskRunnable(JobSet jobSet, boolean rtcIsGood) {
            this.jobSet = jobSet;
            this.rtcGood = rtcIsGood;
        }

        /* JADX DEBUG: Another duplicated slice has different insns count: {[IGET, INVOKE, IGET]}, finally: {[IGET, INVOKE, IGET, IGET, INVOKE, IPUT, IGET, INVOKE, IPUT, IGET, INVOKE, IPUT, IF] complete} */
        /* JADX DEBUG: Don't trust debug lines info. Repeating lines: [750=4, 751=5] */
        /* JADX WARN: Code restructure failed: missing block: B:38:0x009a, code lost:
            if (r12.this$0.mPersistInfo.countAllJobsLoaded >= 0) goto L29;
         */
        /* JADX WARN: Code restructure failed: missing block: B:39:0x009c, code lost:
            r12.this$0.mPersistInfo.countAllJobsLoaded = 0;
            r12.this$0.mPersistInfo.countSystemServerJobsLoaded = 0;
            r12.this$0.mPersistInfo.countSystemSyncManagerJobsLoaded = 0;
         */
        /* JADX WARN: Code restructure failed: missing block: B:43:0x00c6, code lost:
            if (r12.this$0.mPersistInfo.countAllJobsLoaded >= 0) goto L29;
         */
        /* JADX WARN: Code restructure failed: missing block: B:50:0x00df, code lost:
            if (r12.this$0.mPersistInfo.countAllJobsLoaded >= 0) goto L29;
         */
        @Override // java.lang.Runnable
        /*
            Code decompiled incorrectly, please refer to instructions dump.
        */
        public void run() {
            FileInputStream fis;
            int numJobs = 0;
            int numSystemJobs = 0;
            int numSyncJobs = 0;
            try {
                try {
                    try {
                        try {
                            fis = JobStore.this.mJobsFile.openRead();
                        } catch (IOException | XmlPullParserException e) {
                            Slog.wtf(JobStore.TAG, "Error jobstore xml.", e);
                        }
                    } catch (Exception e2) {
                        Slog.wtf(JobStore.TAG, "Unexpected exception", e2);
                    }
                } catch (FileNotFoundException e3) {
                    if (JobStore.DEBUG) {
                        Slog.d(JobStore.TAG, "Could not find jobs file, probably there was nothing to load.");
                    }
                }
                try {
                    synchronized (JobStore.this.mLock) {
                        List<JobStatus> jobs = readJobMapImpl(fis, this.rtcGood);
                        if (jobs != null) {
                            long now = JobSchedulerService.sElapsedRealtimeClock.millis();
                            for (int i = 0; i < jobs.size(); i++) {
                                JobStatus js = jobs.get(i);
                                js.prepareLocked();
                                js.enqueueTime = now;
                                this.jobSet.add(js);
                                numJobs++;
                                if (js.getUid() == 1000) {
                                    numSystemJobs++;
                                    if (JobStore.isSyncJob(js)) {
                                        numSyncJobs++;
                                    }
                                }
                            }
                        }
                    }
                    if (fis != null) {
                        fis.close();
                    }
                    if (JobStore.this.mPersistInfo.countAllJobsLoaded < 0) {
                        JobStore.this.mPersistInfo.countAllJobsLoaded = numJobs;
                        JobStore.this.mPersistInfo.countSystemServerJobsLoaded = numSystemJobs;
                        JobStore.this.mPersistInfo.countSystemSyncManagerJobsLoaded = numSyncJobs;
                    }
                    Slog.i(JobStore.TAG, "Read " + numJobs + " jobs");
                } catch (Throwable th) {
                    if (fis != null) {
                        try {
                            fis.close();
                        } catch (Throwable th2) {
                            th.addSuppressed(th2);
                        }
                    }
                    throw th;
                }
            } catch (Throwable th3) {
                if (JobStore.this.mPersistInfo.countAllJobsLoaded < 0) {
                    JobStore.this.mPersistInfo.countAllJobsLoaded = 0;
                    JobStore.this.mPersistInfo.countSystemServerJobsLoaded = 0;
                    JobStore.this.mPersistInfo.countSystemSyncManagerJobsLoaded = 0;
                }
                throw th3;
            }
        }

        private List<JobStatus> readJobMapImpl(InputStream fis, boolean rtcIsGood) throws XmlPullParserException, IOException {
            TypedXmlPullParser resolvePullParser = Xml.resolvePullParser(fis);
            int eventType = resolvePullParser.getEventType();
            while (eventType != 2 && eventType != 1) {
                eventType = resolvePullParser.next();
                Slog.d(JobStore.TAG, "Start tag: " + resolvePullParser.getName());
            }
            if (eventType == 1) {
                if (JobStore.DEBUG) {
                    Slog.d(JobStore.TAG, "No persisted jobs.");
                }
                return null;
            }
            String tagName = resolvePullParser.getName();
            if (!"job-info".equals(tagName)) {
                return null;
            }
            List<JobStatus> jobs = new ArrayList<>();
            try {
                int version = Integer.parseInt(resolvePullParser.getAttributeValue(null, "version"));
                if (version > 1 || version < 0) {
                    Slog.d(JobStore.TAG, "Invalid version number, aborting jobs file read.");
                    return null;
                }
                int eventType2 = resolvePullParser.next();
                do {
                    if (eventType2 == 2) {
                        String tagName2 = resolvePullParser.getName();
                        if ("job".equals(tagName2)) {
                            JobStatus persistedJob = restoreJobFromXml(rtcIsGood, resolvePullParser, version);
                            if (persistedJob != null) {
                                if (JobStore.DEBUG) {
                                    Slog.d(JobStore.TAG, "Read out " + persistedJob);
                                }
                                jobs.add(persistedJob);
                            } else {
                                Slog.d(JobStore.TAG, "Error reading job from file.");
                            }
                        }
                    }
                    eventType2 = resolvePullParser.next();
                } while (eventType2 != 1);
                return jobs;
            } catch (NumberFormatException e) {
                Slog.e(JobStore.TAG, "Invalid version number, aborting jobs file read.");
                return null;
            }
        }

        private JobStatus restoreJobFromXml(boolean rtcIsGood, XmlPullParser parser, int schemaVersion) throws XmlPullParserException, IOException {
            JobInfo.Builder jobBuilder;
            boolean z;
            int uid;
            int internalFlags;
            int eventType;
            int eventType2;
            long periodMillis;
            long longValue;
            long flexMillis;
            int internalFlags2;
            String sourcePackageName;
            Pair<Long, Long> rtcRuntimes;
            Pair<Long, Long> elapsedRuntimes;
            int eventType3;
            String sourcePackageName2;
            JobStatus jobStatus = null;
            try {
                jobBuilder = buildBuilderFromXml(parser);
                z = true;
                jobBuilder.setPersisted(true);
                uid = Integer.parseInt(parser.getAttributeValue(null, WatchlistLoggingHandler.WatchlistEventKeys.UID));
                if (schemaVersion == 0) {
                    String val = parser.getAttributeValue(null, "priority");
                    if (val != null) {
                        jobBuilder.setBias(Integer.parseInt(val));
                    }
                } else if (schemaVersion >= 1) {
                    String val2 = parser.getAttributeValue(null, "bias");
                    if (val2 != null) {
                        jobBuilder.setBias(Integer.parseInt(val2));
                    }
                    String val3 = parser.getAttributeValue(null, "priority");
                    if (val3 != null) {
                        jobBuilder.setPriority(Integer.parseInt(val3));
                    }
                }
                String val4 = parser.getAttributeValue(null, "flags");
                if (val4 != null) {
                    jobBuilder.setFlags(Integer.parseInt(val4));
                }
                String val5 = parser.getAttributeValue(null, "internalFlags");
                internalFlags = val5 != null ? Integer.parseInt(val5) : 0;
            } catch (NumberFormatException e) {
            }
            try {
                String val6 = parser.getAttributeValue(null, "sourceUserId");
                int sourceUserId = val6 == null ? -1 : Integer.parseInt(val6);
                String val7 = parser.getAttributeValue(null, "lastSuccessfulRunTime");
                long lastSuccessfulRunTime = val7 == null ? 0L : Long.parseLong(val7);
                String val8 = parser.getAttributeValue(null, "lastFailedRunTime");
                long lastFailedRunTime = val8 == null ? 0L : Long.parseLong(val8);
                String sourcePackageName3 = parser.getAttributeValue(null, "sourcePackageName");
                String sourceTag = parser.getAttributeValue(null, "sourceTag");
                while (true) {
                    eventType = parser.next();
                    if (eventType != 4) {
                        break;
                    }
                    sourcePackageName3 = sourcePackageName3;
                    jobStatus = null;
                }
                if (eventType == 2 && JobStore.XML_TAG_PARAMS_CONSTRAINTS.equals(parser.getName())) {
                    try {
                        buildConstraintsFromXml(jobBuilder, parser);
                        parser.next();
                        while (true) {
                            eventType2 = parser.next();
                            if (eventType2 != 4) {
                                break;
                            }
                            sourcePackageName3 = sourcePackageName3;
                            z = z;
                            jobStatus = jobStatus;
                            internalFlags = internalFlags;
                        }
                        if (eventType2 != 2) {
                            return jobStatus;
                        }
                        try {
                            Pair<Long, Long> rtcRuntimes2 = buildRtcExecutionTimesFromXml(parser);
                            long elapsedNow = JobSchedulerService.sElapsedRealtimeClock.millis();
                            Pair<Long, Long> elapsedRuntimes2 = JobStore.convertRtcBoundsToElapsed(rtcRuntimes2, elapsedNow);
                            if (JobStore.XML_TAG_PERIODIC.equals(parser.getName())) {
                                try {
                                    periodMillis = Long.parseLong(parser.getAttributeValue(null, "period"));
                                    String val9 = parser.getAttributeValue(null, "flex");
                                    if (val9 == null) {
                                        longValue = periodMillis;
                                    } else {
                                        try {
                                            longValue = Long.valueOf(val9).longValue();
                                        } catch (NumberFormatException e2) {
                                            Slog.d(JobStore.TAG, "Error reading periodic execution criteria, skipping.");
                                            return null;
                                        }
                                    }
                                    flexMillis = longValue;
                                    internalFlags2 = internalFlags;
                                    sourcePackageName = sourcePackageName3;
                                    rtcRuntimes = rtcRuntimes2;
                                } catch (NumberFormatException e3) {
                                }
                                try {
                                    jobBuilder.setPeriodic(periodMillis, flexMillis);
                                    if (((Long) elapsedRuntimes2.second).longValue() > elapsedNow + periodMillis + flexMillis) {
                                        long clampedLateRuntimeElapsed = elapsedNow + flexMillis + periodMillis;
                                        long clampedEarlyRuntimeElapsed = clampedLateRuntimeElapsed - flexMillis;
                                        Slog.w(JobStore.TAG, String.format("Periodic job for uid='%d' persisted run-time is too big [%s, %s]. Clamping to [%s,%s]", Integer.valueOf(uid), DateUtils.formatElapsedTime(((Long) elapsedRuntimes2.first).longValue() / 1000), DateUtils.formatElapsedTime(((Long) elapsedRuntimes2.second).longValue() / 1000), DateUtils.formatElapsedTime(clampedEarlyRuntimeElapsed / 1000), DateUtils.formatElapsedTime(clampedLateRuntimeElapsed / 1000)));
                                        elapsedRuntimes2 = Pair.create(Long.valueOf(clampedEarlyRuntimeElapsed), Long.valueOf(clampedLateRuntimeElapsed));
                                    }
                                    elapsedRuntimes = elapsedRuntimes2;
                                } catch (NumberFormatException e4) {
                                    Slog.d(JobStore.TAG, "Error reading periodic execution criteria, skipping.");
                                    return null;
                                }
                            } else {
                                internalFlags2 = internalFlags;
                                sourcePackageName = sourcePackageName3;
                                rtcRuntimes = rtcRuntimes2;
                                if (!JobStore.XML_TAG_ONEOFF.equals(parser.getName())) {
                                    if (JobStore.DEBUG) {
                                        Slog.d(JobStore.TAG, "Invalid parameter tag, skipping - " + parser.getName());
                                        return null;
                                    }
                                    return null;
                                }
                                try {
                                    if (((Long) elapsedRuntimes2.first).longValue() != 0) {
                                        try {
                                            jobBuilder.setMinimumLatency(((Long) elapsedRuntimes2.first).longValue() - elapsedNow);
                                        } catch (NumberFormatException e5) {
                                            Slog.d(JobStore.TAG, "Error reading job execution criteria, skipping.");
                                            return null;
                                        }
                                    }
                                    if (((Long) elapsedRuntimes2.second).longValue() != JobStatus.NO_LATEST_RUNTIME) {
                                        jobBuilder.setOverrideDeadline(((Long) elapsedRuntimes2.second).longValue() - elapsedNow);
                                    }
                                    elapsedRuntimes = elapsedRuntimes2;
                                } catch (NumberFormatException e6) {
                                }
                            }
                            maybeBuildBackoffPolicyFromXml(jobBuilder, parser);
                            parser.nextTag();
                            while (true) {
                                eventType3 = parser.next();
                                if (eventType3 != 4) {
                                    break;
                                }
                            }
                            if (eventType3 == 2 && JobStore.XML_TAG_EXTRAS.equals(parser.getName())) {
                                try {
                                    PersistableBundle extras = PersistableBundle.restoreFromXml(parser);
                                    jobBuilder.setExtras(extras);
                                    parser.nextTag();
                                    try {
                                        JobInfo builtJob = jobBuilder.build(false);
                                        String sourcePackageName4 = sourcePackageName;
                                        if (PackageManagerService.PLATFORM_PACKAGE_NAME.equals(sourcePackageName4) && extras != null && extras.getBoolean("SyncManagerJob", false)) {
                                            sourcePackageName2 = extras.getString("owningPackage", sourcePackageName4);
                                            if (JobStore.DEBUG) {
                                                Slog.i(JobStore.TAG, "Fixing up sync job source package name from 'android' to '" + sourcePackageName2 + "'");
                                            }
                                        } else {
                                            sourcePackageName2 = sourcePackageName4;
                                        }
                                        int appBucket = JobSchedulerService.standbyBucketForPackage(sourcePackageName2, sourceUserId, elapsedNow);
                                        JobStatus js = new JobStatus(builtJob, uid, sourcePackageName2, sourceUserId, appBucket, sourceTag, ((Long) elapsedRuntimes.first).longValue(), ((Long) elapsedRuntimes.second).longValue(), lastSuccessfulRunTime, lastFailedRunTime, rtcIsGood ? null : rtcRuntimes, internalFlags2, 0);
                                        return js;
                                    } catch (Exception e7) {
                                        Slog.w(JobStore.TAG, "Unable to build job from XML, ignoring: " + jobBuilder.summarize(), e7);
                                        return null;
                                    }
                                } catch (IllegalArgumentException e8) {
                                    Slog.e(JobStore.TAG, "Persisted extras contained invalid data", e8);
                                    return null;
                                }
                            }
                            if (JobStore.DEBUG) {
                                Slog.d(JobStore.TAG, "Error reading extras, skipping.");
                                return null;
                            }
                            return null;
                        } catch (NumberFormatException e9) {
                            if (JobStore.DEBUG) {
                                Slog.d(JobStore.TAG, "Error parsing execution time parameters, skipping.");
                                return null;
                            }
                            return null;
                        }
                    } catch (IOException e10) {
                        JobStatus jobStatus2 = jobStatus;
                        Slog.d(JobStore.TAG, "Error I/O Exception.", e10);
                        return jobStatus2;
                    } catch (NumberFormatException e11) {
                        JobStatus jobStatus3 = jobStatus;
                        Slog.d(JobStore.TAG, "Error reading constraints, skipping.");
                        return jobStatus3;
                    } catch (IllegalArgumentException e12) {
                        JobStatus jobStatus4 = jobStatus;
                        Slog.e(JobStore.TAG, "Constraints contained invalid data", e12);
                        return jobStatus4;
                    } catch (XmlPullParserException e13) {
                        JobStatus jobStatus5 = jobStatus;
                        Slog.d(JobStore.TAG, "Error Parser Exception.", e13);
                        return jobStatus5;
                    }
                }
                return jobStatus;
            } catch (NumberFormatException e14) {
                Slog.e(JobStore.TAG, "Error parsing job's required fields, skipping");
                return null;
            }
        }

        private JobInfo.Builder buildBuilderFromXml(XmlPullParser parser) throws NumberFormatException {
            int jobId = Integer.parseInt(parser.getAttributeValue(null, "jobid"));
            String packageName = parser.getAttributeValue(null, "package");
            String className = parser.getAttributeValue(null, "class");
            ComponentName cname = new ComponentName(packageName, className);
            return new JobInfo.Builder(jobId, cname);
        }

        private void buildConstraintsFromXml(JobInfo.Builder jobBuilder, XmlPullParser parser) throws XmlPullParserException, IOException {
            String str;
            int[] unpackBits;
            int[] unpackBits2;
            int[] unpackBits3;
            boolean z;
            String netCapabilitiesLong = null;
            String netForbiddenCapabilitiesLong = null;
            String netTransportTypesLong = null;
            String netCapabilitiesIntArray = parser.getAttributeValue(null, "net-capabilities-csv");
            String netForbiddenCapabilitiesIntArray = parser.getAttributeValue(null, "net-forbidden-capabilities-csv");
            String netTransportTypesIntArray = parser.getAttributeValue(null, "net-transport-types-csv");
            if (netCapabilitiesIntArray == null || netTransportTypesIntArray == null) {
                netCapabilitiesLong = parser.getAttributeValue(null, "net-capabilities");
                netForbiddenCapabilitiesLong = parser.getAttributeValue(null, "net-unwanted-capabilities");
                netTransportTypesLong = parser.getAttributeValue(null, "net-transport-types");
            }
            if (netCapabilitiesIntArray != null && netTransportTypesIntArray != null) {
                NetworkRequest.Builder builder = new NetworkRequest.Builder().clearCapabilities();
                for (int capability : JobStore.stringToIntArray(netCapabilitiesIntArray)) {
                    builder.addCapability(capability);
                }
                for (int forbiddenCapability : JobStore.stringToIntArray(netForbiddenCapabilitiesIntArray)) {
                    builder.addForbiddenCapability(forbiddenCapability);
                }
                for (int transport : JobStore.stringToIntArray(netTransportTypesIntArray)) {
                    builder.addTransportType(transport);
                }
                jobBuilder.setRequiredNetwork(builder.build());
                str = null;
            } else if (netCapabilitiesLong != null && netTransportTypesLong != null) {
                NetworkRequest.Builder builder2 = new NetworkRequest.Builder().clearCapabilities();
                for (int capability2 : BitUtils.unpackBits(Long.parseLong(netCapabilitiesLong))) {
                    if (capability2 <= 25) {
                        builder2.addCapability(capability2);
                    }
                }
                for (int forbiddenCapability2 : BitUtils.unpackBits(Long.parseLong(netForbiddenCapabilitiesLong))) {
                    if (forbiddenCapability2 <= 25) {
                        builder2.addForbiddenCapability(forbiddenCapability2);
                    }
                }
                for (int transport2 : BitUtils.unpackBits(Long.parseLong(netTransportTypesLong))) {
                    if (transport2 <= 7) {
                        builder2.addTransportType(transport2);
                    }
                }
                jobBuilder.setRequiredNetwork(builder2.build());
                str = null;
            } else {
                str = null;
                String val = parser.getAttributeValue(null, "connectivity");
                if (val != null) {
                    jobBuilder.setRequiredNetworkType(1);
                }
                String val2 = parser.getAttributeValue(null, "metered");
                if (val2 != null) {
                    jobBuilder.setRequiredNetworkType(4);
                }
                String val3 = parser.getAttributeValue(null, "unmetered");
                if (val3 != null) {
                    jobBuilder.setRequiredNetworkType(2);
                }
                String val4 = parser.getAttributeValue(null, "not-roaming");
                if (val4 != null) {
                    jobBuilder.setRequiredNetworkType(3);
                }
            }
            String val5 = parser.getAttributeValue(str, "idle");
            if (val5 == null) {
                z = true;
            } else {
                z = true;
                jobBuilder.setRequiresDeviceIdle(true);
            }
            String val6 = parser.getAttributeValue(str, "charging");
            if (val6 != null) {
                jobBuilder.setRequiresCharging(z);
            }
            String val7 = parser.getAttributeValue(str, "battery-not-low");
            if (val7 != null) {
                jobBuilder.setRequiresBatteryNotLow(z);
            }
            String val8 = parser.getAttributeValue(str, "storage-not-low");
            if (val8 != null) {
                jobBuilder.setRequiresStorageNotLow(z);
            }
        }

        private void maybeBuildBackoffPolicyFromXml(JobInfo.Builder jobBuilder, XmlPullParser parser) {
            String val = parser.getAttributeValue(null, "initial-backoff");
            if (val != null) {
                long initialBackoff = Long.parseLong(val);
                int backoffPolicy = Integer.parseInt(parser.getAttributeValue(null, "backoff-policy"));
                jobBuilder.setBackoffCriteria(initialBackoff, backoffPolicy);
            }
        }

        private Pair<Long, Long> buildRtcExecutionTimesFromXml(XmlPullParser parser) throws NumberFormatException {
            long earliestRunTimeRtc;
            long latestRunTimeRtc;
            String val = parser.getAttributeValue(null, "delay");
            if (val != null) {
                earliestRunTimeRtc = Long.parseLong(val);
            } else {
                earliestRunTimeRtc = 0;
            }
            String val2 = parser.getAttributeValue(null, "deadline");
            if (val2 != null) {
                latestRunTimeRtc = Long.parseLong(val2);
            } else {
                latestRunTimeRtc = JobStatus.NO_LATEST_RUNTIME;
            }
            return Pair.create(Long.valueOf(earliestRunTimeRtc), Long.valueOf(latestRunTimeRtc));
        }
    }

    /* loaded from: classes.dex */
    public static final class JobSet {
        final SparseArray<ArraySet<JobStatus>> mJobs = new SparseArray<>();
        final SparseArray<ArraySet<JobStatus>> mJobsPerSourceUid = new SparseArray<>();

        public List<JobStatus> getJobsByUid(int uid) {
            ArrayList<JobStatus> matchingJobs = new ArrayList<>();
            ArraySet<JobStatus> jobs = this.mJobs.get(uid);
            if (jobs != null) {
                matchingJobs.addAll(jobs);
            }
            return matchingJobs;
        }

        public List<JobStatus> getJobsByUser(int userId) {
            ArraySet<JobStatus> jobs;
            ArrayList<JobStatus> result = new ArrayList<>();
            for (int i = this.mJobsPerSourceUid.size() - 1; i >= 0; i--) {
                if (UserHandle.getUserId(this.mJobsPerSourceUid.keyAt(i)) == userId && (jobs = this.mJobsPerSourceUid.valueAt(i)) != null) {
                    result.addAll(jobs);
                }
            }
            return result;
        }

        public boolean add(JobStatus job) {
            int uid = job.getUid();
            int sourceUid = job.getSourceUid();
            ArraySet<JobStatus> jobs = this.mJobs.get(uid);
            if (jobs == null) {
                jobs = new ArraySet<>();
                this.mJobs.put(uid, jobs);
            }
            ArraySet<JobStatus> jobsForSourceUid = this.mJobsPerSourceUid.get(sourceUid);
            if (jobsForSourceUid == null) {
                jobsForSourceUid = new ArraySet<>();
                this.mJobsPerSourceUid.put(sourceUid, jobsForSourceUid);
            }
            boolean added = jobs.add(job);
            boolean addedInSource = jobsForSourceUid.add(job);
            if (added != addedInSource) {
                Slog.wtf(JobStore.TAG, "mJobs and mJobsPerSourceUid mismatch; caller= " + added + " source= " + addedInSource);
            }
            return added || addedInSource;
        }

        public boolean remove(JobStatus job) {
            int uid = job.getUid();
            ArraySet<JobStatus> jobs = this.mJobs.get(uid);
            int sourceUid = job.getSourceUid();
            ArraySet<JobStatus> jobsForSourceUid = this.mJobsPerSourceUid.get(sourceUid);
            boolean didRemove = jobs != null && jobs.remove(job);
            boolean sourceRemove = jobsForSourceUid != null && jobsForSourceUid.remove(job);
            if (didRemove != sourceRemove) {
                Slog.wtf(JobStore.TAG, "Job presence mismatch; caller=" + didRemove + " source=" + sourceRemove);
            }
            if (didRemove || sourceRemove) {
                if (jobs != null && jobs.size() == 0) {
                    this.mJobs.remove(uid);
                }
                if (jobsForSourceUid != null && jobsForSourceUid.size() == 0) {
                    this.mJobsPerSourceUid.remove(sourceUid);
                }
                return true;
            }
            return false;
        }

        public void removeJobsOfUnlistedUsers(final int[] keepUserIds) {
            Predicate<JobStatus> noSourceUser = new Predicate() { // from class: com.android.server.job.JobStore$JobSet$$ExternalSyntheticLambda0
                @Override // java.util.function.Predicate
                public final boolean test(Object obj) {
                    return JobStore.JobSet.lambda$removeJobsOfUnlistedUsers$0(keepUserIds, (JobStatus) obj);
                }
            };
            Predicate<JobStatus> noCallingUser = new Predicate() { // from class: com.android.server.job.JobStore$JobSet$$ExternalSyntheticLambda1
                @Override // java.util.function.Predicate
                public final boolean test(Object obj) {
                    return JobStore.JobSet.lambda$removeJobsOfUnlistedUsers$1(keepUserIds, (JobStatus) obj);
                }
            };
            removeAll(noSourceUser.or(noCallingUser));
        }

        /* JADX INFO: Access modifiers changed from: package-private */
        public static /* synthetic */ boolean lambda$removeJobsOfUnlistedUsers$0(int[] keepUserIds, JobStatus job) {
            return !ArrayUtils.contains(keepUserIds, job.getSourceUserId());
        }

        /* JADX INFO: Access modifiers changed from: package-private */
        public static /* synthetic */ boolean lambda$removeJobsOfUnlistedUsers$1(int[] keepUserIds, JobStatus job) {
            return !ArrayUtils.contains(keepUserIds, job.getUserId());
        }

        private void removeAll(Predicate<JobStatus> predicate) {
            for (int jobSetIndex = this.mJobs.size() - 1; jobSetIndex >= 0; jobSetIndex--) {
                ArraySet<JobStatus> jobs = this.mJobs.valueAt(jobSetIndex);
                jobs.removeIf(predicate);
                if (jobs.size() == 0) {
                    this.mJobs.removeAt(jobSetIndex);
                }
            }
            for (int jobSetIndex2 = this.mJobsPerSourceUid.size() - 1; jobSetIndex2 >= 0; jobSetIndex2--) {
                ArraySet<JobStatus> jobs2 = this.mJobsPerSourceUid.valueAt(jobSetIndex2);
                jobs2.removeIf(predicate);
                if (jobs2.size() == 0) {
                    this.mJobsPerSourceUid.removeAt(jobSetIndex2);
                }
            }
        }

        public boolean contains(JobStatus job) {
            int uid = job.getUid();
            ArraySet<JobStatus> jobs = this.mJobs.get(uid);
            return jobs != null && jobs.contains(job);
        }

        public JobStatus get(int uid, int jobId) {
            ArraySet<JobStatus> jobs = this.mJobs.get(uid);
            if (jobs != null) {
                for (int i = jobs.size() - 1; i >= 0; i--) {
                    JobStatus job = jobs.valueAt(i);
                    if (job.getJobId() == jobId) {
                        return job;
                    }
                }
                return null;
            }
            return null;
        }

        public List<JobStatus> getAllJobs() {
            ArrayList<JobStatus> allJobs = new ArrayList<>(size());
            for (int i = this.mJobs.size() - 1; i >= 0; i--) {
                ArraySet<JobStatus> jobs = this.mJobs.valueAt(i);
                if (jobs != null) {
                    for (int j = jobs.size() - 1; j >= 0; j--) {
                        allJobs.add(jobs.valueAt(j));
                    }
                }
            }
            return allJobs;
        }

        public void clear() {
            this.mJobs.clear();
            this.mJobsPerSourceUid.clear();
        }

        public int size() {
            int total = 0;
            for (int i = this.mJobs.size() - 1; i >= 0; i--) {
                total += this.mJobs.valueAt(i).size();
            }
            return total;
        }

        public int countJobsForUid(int uid) {
            int total = 0;
            ArraySet<JobStatus> jobs = this.mJobs.get(uid);
            if (jobs != null) {
                for (int i = jobs.size() - 1; i >= 0; i--) {
                    JobStatus job = jobs.valueAt(i);
                    if (job.getUid() == job.getSourceUid()) {
                        total++;
                    }
                }
            }
            return total;
        }

        public void forEachJob(Predicate<JobStatus> filterPredicate, Consumer<JobStatus> functor) {
            for (int uidIndex = this.mJobs.size() - 1; uidIndex >= 0; uidIndex--) {
                ArraySet<JobStatus> jobs = this.mJobs.valueAt(uidIndex);
                if (jobs != null) {
                    for (int i = jobs.size() - 1; i >= 0; i--) {
                        JobStatus jobStatus = jobs.valueAt(i);
                        if (filterPredicate == null || filterPredicate.test(jobStatus)) {
                            functor.accept(jobStatus);
                        }
                    }
                }
            }
        }

        public void forEachJob(int callingUid, Consumer<JobStatus> functor) {
            ArraySet<JobStatus> jobs = this.mJobs.get(callingUid);
            if (jobs != null) {
                for (int i = jobs.size() - 1; i >= 0; i--) {
                    functor.accept(jobs.valueAt(i));
                }
            }
        }

        public void forEachJobForSourceUid(int sourceUid, Consumer<JobStatus> functor) {
            ArraySet<JobStatus> jobs = this.mJobsPerSourceUid.get(sourceUid);
            if (jobs != null) {
                for (int i = jobs.size() - 1; i >= 0; i--) {
                    functor.accept(jobs.valueAt(i));
                }
            }
        }
    }
}
