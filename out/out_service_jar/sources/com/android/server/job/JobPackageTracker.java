package com.android.server.job;

import android.app.job.JobParameters;
import android.os.UserHandle;
import android.text.format.DateFormat;
import android.util.ArrayMap;
import android.util.IndentingPrintWriter;
import android.util.SparseArray;
import android.util.SparseIntArray;
import android.util.TimeUtils;
import android.util.proto.ProtoOutputStream;
import com.android.internal.util.jobs.RingBufferIndices;
import com.android.server.job.controllers.JobStatus;
import com.android.server.pm.verify.domain.DomainVerificationPersistence;
import com.android.server.slice.SliceClientPermissions;
/* loaded from: classes.dex */
public final class JobPackageTracker {
    static final long BATCHING_TIME = 1800000;
    private static final int EVENT_BUFFER_SIZE = 100;
    public static final int EVENT_CMD_MASK = 255;
    public static final int EVENT_NULL = 0;
    public static final int EVENT_START_JOB = 1;
    public static final int EVENT_START_PERIODIC_JOB = 3;
    public static final int EVENT_STOP_JOB = 2;
    public static final int EVENT_STOP_PERIODIC_JOB = 4;
    public static final int EVENT_STOP_REASON_MASK = 65280;
    public static final int EVENT_STOP_REASON_SHIFT = 8;
    static final int NUM_HISTORY = 5;
    private final RingBufferIndices mEventIndices = new RingBufferIndices(100);
    private final int[] mEventCmds = new int[100];
    private final long[] mEventTimes = new long[100];
    private final int[] mEventUids = new int[100];
    private final String[] mEventTags = new String[100];
    private final int[] mEventJobIds = new int[100];
    private final String[] mEventReasons = new String[100];
    DataSet mCurDataSet = new DataSet();
    DataSet[] mLastDataSets = new DataSet[5];

    public void addEvent(int cmd, int uid, String tag, int jobId, int stopReason, String debugReason) {
        int index = this.mEventIndices.add();
        this.mEventCmds[index] = ((stopReason << 8) & EVENT_STOP_REASON_MASK) | cmd;
        this.mEventTimes[index] = JobSchedulerService.sElapsedRealtimeClock.millis();
        this.mEventUids[index] = uid;
        this.mEventTags[index] = tag;
        this.mEventJobIds[index] = jobId;
        this.mEventReasons[index] = debugReason;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* loaded from: classes.dex */
    public static final class PackageEntry {
        int activeCount;
        int activeNesting;
        long activeStartTime;
        int activeTopCount;
        int activeTopNesting;
        long activeTopStartTime;
        boolean hadActive;
        boolean hadActiveTop;
        boolean hadPending;
        long pastActiveTime;
        long pastActiveTopTime;
        long pastPendingTime;
        int pendingCount;
        int pendingNesting;
        long pendingStartTime;
        final SparseIntArray stopReasons = new SparseIntArray();

        PackageEntry() {
        }

        public long getActiveTime(long now) {
            long time = this.pastActiveTime;
            if (this.activeNesting > 0) {
                return time + (now - this.activeStartTime);
            }
            return time;
        }

        public long getActiveTopTime(long now) {
            long time = this.pastActiveTopTime;
            if (this.activeTopNesting > 0) {
                return time + (now - this.activeTopStartTime);
            }
            return time;
        }

        public long getPendingTime(long now) {
            long time = this.pastPendingTime;
            if (this.pendingNesting > 0) {
                return time + (now - this.pendingStartTime);
            }
            return time;
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* loaded from: classes.dex */
    public static final class DataSet {
        final SparseArray<ArrayMap<String, PackageEntry>> mEntries;
        int mMaxFgActive;
        int mMaxTotalActive;
        final long mStartClockTime;
        final long mStartElapsedTime;
        final long mStartUptimeTime;
        long mSummedTime;

        public DataSet(DataSet otherTimes) {
            this.mEntries = new SparseArray<>();
            this.mStartUptimeTime = otherTimes.mStartUptimeTime;
            this.mStartElapsedTime = otherTimes.mStartElapsedTime;
            this.mStartClockTime = otherTimes.mStartClockTime;
        }

        public DataSet() {
            this.mEntries = new SparseArray<>();
            this.mStartUptimeTime = JobSchedulerService.sUptimeMillisClock.millis();
            this.mStartElapsedTime = JobSchedulerService.sElapsedRealtimeClock.millis();
            this.mStartClockTime = JobSchedulerService.sSystemClock.millis();
        }

        private PackageEntry getOrCreateEntry(int uid, String pkg) {
            ArrayMap<String, PackageEntry> uidMap = this.mEntries.get(uid);
            if (uidMap == null) {
                uidMap = new ArrayMap<>();
                this.mEntries.put(uid, uidMap);
            }
            PackageEntry entry = uidMap.get(pkg);
            if (entry == null) {
                PackageEntry entry2 = new PackageEntry();
                uidMap.put(pkg, entry2);
                return entry2;
            }
            return entry;
        }

        public PackageEntry getEntry(int uid, String pkg) {
            ArrayMap<String, PackageEntry> uidMap = this.mEntries.get(uid);
            if (uidMap == null) {
                return null;
            }
            return uidMap.get(pkg);
        }

        long getTotalTime(long now) {
            long j = this.mSummedTime;
            if (j > 0) {
                return j;
            }
            return now - this.mStartUptimeTime;
        }

        void incPending(int uid, String pkg, long now) {
            PackageEntry pe = getOrCreateEntry(uid, pkg);
            if (pe.pendingNesting == 0) {
                pe.pendingStartTime = now;
                pe.pendingCount++;
            }
            pe.pendingNesting++;
        }

        void decPending(int uid, String pkg, long now) {
            PackageEntry pe = getOrCreateEntry(uid, pkg);
            if (pe.pendingNesting == 1) {
                pe.pastPendingTime += now - pe.pendingStartTime;
            }
            pe.pendingNesting--;
        }

        void incActive(int uid, String pkg, long now) {
            PackageEntry pe = getOrCreateEntry(uid, pkg);
            if (pe.activeNesting == 0) {
                pe.activeStartTime = now;
                pe.activeCount++;
            }
            pe.activeNesting++;
        }

        void decActive(int uid, String pkg, long now, int stopReason) {
            PackageEntry pe = getOrCreateEntry(uid, pkg);
            if (pe.activeNesting == 1) {
                pe.pastActiveTime += now - pe.activeStartTime;
            }
            pe.activeNesting--;
            int count = pe.stopReasons.get(stopReason, 0);
            pe.stopReasons.put(stopReason, count + 1);
        }

        void incActiveTop(int uid, String pkg, long now) {
            PackageEntry pe = getOrCreateEntry(uid, pkg);
            if (pe.activeTopNesting == 0) {
                pe.activeTopStartTime = now;
                pe.activeTopCount++;
            }
            pe.activeTopNesting++;
        }

        void decActiveTop(int uid, String pkg, long now, int stopReason) {
            PackageEntry pe = getOrCreateEntry(uid, pkg);
            if (pe.activeTopNesting == 1) {
                pe.pastActiveTopTime += now - pe.activeTopStartTime;
            }
            pe.activeTopNesting--;
            int count = pe.stopReasons.get(stopReason, 0);
            pe.stopReasons.put(stopReason, count + 1);
        }

        void finish(DataSet next, long now) {
            for (int i = this.mEntries.size() - 1; i >= 0; i--) {
                ArrayMap<String, PackageEntry> uidMap = this.mEntries.valueAt(i);
                for (int j = uidMap.size() - 1; j >= 0; j--) {
                    PackageEntry pe = uidMap.valueAt(j);
                    if (pe.activeNesting > 0 || pe.activeTopNesting > 0 || pe.pendingNesting > 0) {
                        PackageEntry nextPe = next.getOrCreateEntry(this.mEntries.keyAt(i), uidMap.keyAt(j));
                        nextPe.activeStartTime = now;
                        nextPe.activeNesting = pe.activeNesting;
                        nextPe.activeTopStartTime = now;
                        nextPe.activeTopNesting = pe.activeTopNesting;
                        nextPe.pendingStartTime = now;
                        nextPe.pendingNesting = pe.pendingNesting;
                        if (pe.activeNesting > 0) {
                            pe.pastActiveTime += now - pe.activeStartTime;
                            pe.activeNesting = 0;
                        }
                        if (pe.activeTopNesting > 0) {
                            pe.pastActiveTopTime += now - pe.activeTopStartTime;
                            pe.activeTopNesting = 0;
                        }
                        if (pe.pendingNesting > 0) {
                            pe.pastPendingTime += now - pe.pendingStartTime;
                            pe.pendingNesting = 0;
                        }
                    }
                }
            }
        }

        void addTo(DataSet out, long now) {
            out.mSummedTime += getTotalTime(now);
            for (int i = this.mEntries.size() - 1; i >= 0; i--) {
                ArrayMap<String, PackageEntry> uidMap = this.mEntries.valueAt(i);
                for (int j = uidMap.size() - 1; j >= 0; j--) {
                    PackageEntry pe = uidMap.valueAt(j);
                    PackageEntry outPe = out.getOrCreateEntry(this.mEntries.keyAt(i), uidMap.keyAt(j));
                    outPe.pastActiveTime += pe.pastActiveTime;
                    outPe.activeCount += pe.activeCount;
                    outPe.pastActiveTopTime += pe.pastActiveTopTime;
                    outPe.activeTopCount += pe.activeTopCount;
                    outPe.pastPendingTime += pe.pastPendingTime;
                    outPe.pendingCount += pe.pendingCount;
                    if (pe.activeNesting > 0) {
                        outPe.pastActiveTime += now - pe.activeStartTime;
                        outPe.hadActive = true;
                    }
                    if (pe.activeTopNesting > 0) {
                        outPe.pastActiveTopTime += now - pe.activeTopStartTime;
                        outPe.hadActiveTop = true;
                    }
                    if (pe.pendingNesting > 0) {
                        outPe.pastPendingTime += now - pe.pendingStartTime;
                        outPe.hadPending = true;
                    }
                    for (int k = pe.stopReasons.size() - 1; k >= 0; k--) {
                        int type = pe.stopReasons.keyAt(k);
                        outPe.stopReasons.put(type, outPe.stopReasons.get(type, 0) + pe.stopReasons.valueAt(k));
                    }
                }
            }
            int i2 = this.mMaxTotalActive;
            if (i2 > out.mMaxTotalActive) {
                out.mMaxTotalActive = i2;
            }
            int i3 = this.mMaxFgActive;
            if (i3 > out.mMaxFgActive) {
                out.mMaxFgActive = i3;
            }
        }

        boolean printDuration(IndentingPrintWriter pw, long period, long duration, int count, String suffix) {
            float fraction = ((float) duration) / ((float) period);
            int percent = (int) ((100.0f * fraction) + 0.5f);
            if (percent > 0) {
                pw.print(percent);
                pw.print("% ");
                pw.print(count);
                pw.print("x ");
                pw.print(suffix);
                return true;
            } else if (count > 0) {
                pw.print(count);
                pw.print("x ");
                pw.print(suffix);
                return true;
            } else {
                return false;
            }
        }

        void dump(IndentingPrintWriter pw, String header, long now, long nowElapsed, int filterAppId) {
            DataSet dataSet = this;
            int i = filterAppId;
            long period = dataSet.getTotalTime(now);
            pw.print(header);
            pw.print(" at ");
            pw.print(DateFormat.format("yyyy-MM-dd-HH-mm-ss", dataSet.mStartClockTime).toString());
            pw.print(" (");
            TimeUtils.formatDuration(dataSet.mStartElapsedTime, nowElapsed, pw);
            pw.print(") over ");
            TimeUtils.formatDuration(period, pw);
            pw.println(":");
            pw.increaseIndent();
            pw.print("Max concurrency: ");
            pw.print(dataSet.mMaxTotalActive);
            pw.print(" total, ");
            pw.print(dataSet.mMaxFgActive);
            pw.println(" foreground");
            pw.println();
            int NE = dataSet.mEntries.size();
            int i2 = 0;
            while (i2 < NE) {
                int uid = dataSet.mEntries.keyAt(i2);
                if (i == -1 || i == UserHandle.getAppId(uid)) {
                    ArrayMap<String, PackageEntry> uidMap = dataSet.mEntries.valueAt(i2);
                    int NP = uidMap.size();
                    int j = 0;
                    while (j < NP) {
                        PackageEntry pe = uidMap.valueAt(j);
                        UserHandle.formatUid(pw, uid);
                        int NP2 = NP;
                        pw.print(" / ");
                        pw.print(uidMap.keyAt(j));
                        pw.println(":");
                        pw.increaseIndent();
                        int j2 = j;
                        int uid2 = uid;
                        ArrayMap<String, PackageEntry> uidMap2 = uidMap;
                        int NE2 = NE;
                        int i3 = i2;
                        if (printDuration(pw, period, pe.getPendingTime(now), pe.pendingCount, "pending")) {
                            pw.print(" ");
                        }
                        if (printDuration(pw, period, pe.getActiveTime(now), pe.activeCount, DomainVerificationPersistence.TAG_ACTIVE)) {
                            pw.print(" ");
                        }
                        printDuration(pw, period, pe.getActiveTopTime(now), pe.activeTopCount, "active-top");
                        if (pe.pendingNesting > 0 || pe.hadPending) {
                            pw.print(" (pending)");
                        }
                        if (pe.activeNesting > 0 || pe.hadActive) {
                            pw.print(" (active)");
                        }
                        if (pe.activeTopNesting > 0 || pe.hadActiveTop) {
                            pw.print(" (active-top)");
                        }
                        pw.println();
                        if (pe.stopReasons.size() > 0) {
                            for (int k = 0; k < pe.stopReasons.size(); k++) {
                                if (k > 0) {
                                    pw.print(", ");
                                }
                                pw.print(pe.stopReasons.valueAt(k));
                                pw.print("x ");
                                pw.print(JobParameters.getInternalReasonCodeDescription(pe.stopReasons.keyAt(k)));
                            }
                            pw.println();
                        }
                        pw.decreaseIndent();
                        j = j2 + 1;
                        NP = NP2;
                        uid = uid2;
                        uidMap = uidMap2;
                        NE = NE2;
                        i2 = i3;
                    }
                }
                i2++;
                dataSet = this;
                i = filterAppId;
                NE = NE;
            }
            pw.decreaseIndent();
        }

        private void printPackageEntryState(ProtoOutputStream proto, long fieldId, long duration, int count) {
            long token = proto.start(fieldId);
            proto.write(1112396529665L, duration);
            proto.write(1120986464258L, count);
            proto.end(token);
        }

        void dump(ProtoOutputStream proto, long fieldId, long now, long nowElapsed, int filterUid) {
            int i;
            int i2 = filterUid;
            long token = proto.start(fieldId);
            long period = getTotalTime(now);
            proto.write(1112396529665L, this.mStartClockTime);
            proto.write(1112396529666L, nowElapsed - this.mStartElapsedTime);
            proto.write(1112396529667L, period);
            int NE = this.mEntries.size();
            int i3 = 0;
            while (i3 < NE) {
                int uid = this.mEntries.keyAt(i3);
                if (i2 != -1 && i2 != UserHandle.getAppId(uid)) {
                    i = i3;
                } else {
                    ArrayMap<String, PackageEntry> uidMap = this.mEntries.valueAt(i3);
                    int NP = uidMap.size();
                    int j = 0;
                    while (j < NP) {
                        int NP2 = NP;
                        int i4 = i3;
                        long peToken = proto.start(2246267895812L);
                        PackageEntry pe = uidMap.valueAt(j);
                        proto.write(CompanionMessage.MESSAGE_ID, uid);
                        int j2 = j;
                        proto.write(1138166333442L, uidMap.keyAt(j));
                        long period2 = period;
                        ArrayMap<String, PackageEntry> uidMap2 = uidMap;
                        int uid2 = uid;
                        int NE2 = NE;
                        printPackageEntryState(proto, 1146756268035L, pe.getPendingTime(now), pe.pendingCount);
                        printPackageEntryState(proto, 1146756268036L, pe.getActiveTime(now), pe.activeCount);
                        printPackageEntryState(proto, 1146756268037L, pe.getActiveTopTime(now), pe.activeTopCount);
                        boolean z = false;
                        proto.write(1133871366150L, pe.pendingNesting > 0 || pe.hadPending);
                        proto.write(1133871366151L, pe.activeNesting > 0 || pe.hadActive);
                        proto.write(1133871366152L, (pe.activeTopNesting > 0 || pe.hadActiveTop) ? true : true);
                        for (int k = 0; k < pe.stopReasons.size(); k++) {
                            long srcToken = proto.start(2246267895817L);
                            proto.write(1159641169921L, pe.stopReasons.keyAt(k));
                            proto.write(1120986464258L, pe.stopReasons.valueAt(k));
                            proto.end(srcToken);
                        }
                        proto.end(peToken);
                        j = j2 + 1;
                        i3 = i4;
                        uidMap = uidMap2;
                        NP = NP2;
                        uid = uid2;
                        NE = NE2;
                        period = period2;
                    }
                    i = i3;
                }
                i3 = i + 1;
                i2 = filterUid;
                NE = NE;
                period = period;
            }
            proto.write(1120986464261L, this.mMaxTotalActive);
            proto.write(1120986464262L, this.mMaxFgActive);
            proto.end(token);
        }
    }

    void rebatchIfNeeded(long now) {
        long totalTime = this.mCurDataSet.getTotalTime(now);
        if (totalTime > 1800000) {
            DataSet last = this.mCurDataSet;
            last.mSummedTime = totalTime;
            DataSet dataSet = new DataSet();
            this.mCurDataSet = dataSet;
            last.finish(dataSet, now);
            DataSet[] dataSetArr = this.mLastDataSets;
            System.arraycopy(dataSetArr, 0, dataSetArr, 1, dataSetArr.length - 1);
            this.mLastDataSets[0] = last;
        }
    }

    public void notePending(JobStatus job) {
        long now = JobSchedulerService.sUptimeMillisClock.millis();
        job.madePending = now;
        rebatchIfNeeded(now);
        this.mCurDataSet.incPending(job.getSourceUid(), job.getSourcePackageName(), now);
    }

    public void noteNonpending(JobStatus job) {
        long now = JobSchedulerService.sUptimeMillisClock.millis();
        this.mCurDataSet.decPending(job.getSourceUid(), job.getSourcePackageName(), now);
        rebatchIfNeeded(now);
    }

    public void noteActive(JobStatus job) {
        long now = JobSchedulerService.sUptimeMillisClock.millis();
        job.madeActive = now;
        rebatchIfNeeded(now);
        if (job.lastEvaluatedBias >= 40) {
            this.mCurDataSet.incActiveTop(job.getSourceUid(), job.getSourcePackageName(), now);
        } else {
            this.mCurDataSet.incActive(job.getSourceUid(), job.getSourcePackageName(), now);
        }
        addEvent(job.getJob().isPeriodic() ? 3 : 1, job.getSourceUid(), job.getBatteryName(), job.getJobId(), 0, null);
    }

    public void noteInactive(JobStatus job, int stopReason, String debugReason) {
        long now = JobSchedulerService.sUptimeMillisClock.millis();
        if (job.lastEvaluatedBias >= 40) {
            this.mCurDataSet.decActiveTop(job.getSourceUid(), job.getSourcePackageName(), now, stopReason);
        } else {
            this.mCurDataSet.decActive(job.getSourceUid(), job.getSourcePackageName(), now, stopReason);
        }
        rebatchIfNeeded(now);
        addEvent(job.getJob().isPeriodic() ? 4 : 2, job.getSourceUid(), job.getBatteryName(), job.getJobId(), stopReason, debugReason);
    }

    public void noteConcurrency(int totalActive, int fgActive) {
        if (totalActive > this.mCurDataSet.mMaxTotalActive) {
            this.mCurDataSet.mMaxTotalActive = totalActive;
        }
        if (fgActive > this.mCurDataSet.mMaxFgActive) {
            this.mCurDataSet.mMaxFgActive = fgActive;
        }
    }

    public float getLoadFactor(JobStatus job) {
        int uid = job.getSourceUid();
        String pkg = job.getSourcePackageName();
        PackageEntry cur = this.mCurDataSet.getEntry(uid, pkg);
        DataSet dataSet = this.mLastDataSets[0];
        PackageEntry last = dataSet != null ? dataSet.getEntry(uid, pkg) : null;
        if (cur == null && last == null) {
            return 0.0f;
        }
        long now = JobSchedulerService.sUptimeMillisClock.millis();
        long time = cur != null ? 0 + cur.getActiveTime(now) + cur.getPendingTime(now) : 0L;
        long period = this.mCurDataSet.getTotalTime(now);
        if (last != null) {
            time += last.getActiveTime(now) + last.getPendingTime(now);
            period += this.mLastDataSets[0].getTotalTime(now);
        }
        return ((float) time) / ((float) period);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void dump(IndentingPrintWriter pw, int filterAppId) {
        DataSet total;
        long now = JobSchedulerService.sUptimeMillisClock.millis();
        long nowElapsed = JobSchedulerService.sElapsedRealtimeClock.millis();
        DataSet dataSet = this.mLastDataSets[0];
        if (dataSet != null) {
            total = new DataSet(dataSet);
            this.mLastDataSets[0].addTo(total, now);
        } else {
            total = new DataSet(this.mCurDataSet);
        }
        this.mCurDataSet.addTo(total, now);
        int i = 1;
        while (true) {
            DataSet[] dataSetArr = this.mLastDataSets;
            if (i < dataSetArr.length) {
                DataSet dataSet2 = dataSetArr[i];
                if (dataSet2 != null) {
                    dataSet2.dump(pw, "Historical stats", now, nowElapsed, filterAppId);
                    pw.println();
                }
                i++;
            } else {
                total.dump(pw, "Current stats", now, nowElapsed, filterAppId);
                return;
            }
        }
    }

    public void dump(ProtoOutputStream proto, long fieldId, int filterUid) {
        DataSet total;
        int i;
        long token = proto.start(fieldId);
        long now = JobSchedulerService.sUptimeMillisClock.millis();
        long nowElapsed = JobSchedulerService.sElapsedRealtimeClock.millis();
        DataSet dataSet = this.mLastDataSets[0];
        if (dataSet != null) {
            total = new DataSet(dataSet);
            this.mLastDataSets[0].addTo(total, now);
        } else {
            total = new DataSet(this.mCurDataSet);
        }
        this.mCurDataSet.addTo(total, now);
        int i2 = 1;
        while (true) {
            DataSet[] dataSetArr = this.mLastDataSets;
            if (i2 < dataSetArr.length) {
                DataSet dataSet2 = dataSetArr[i2];
                if (dataSet2 == null) {
                    i = i2;
                } else {
                    i = i2;
                    dataSet2.dump(proto, CompanionAppsPermissions.APP_PERMISSIONS, now, nowElapsed, filterUid);
                }
                i2 = i + 1;
            } else {
                total.dump(proto, 1146756268034L, now, nowElapsed, filterUid);
                proto.end(token);
                return;
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean dumpHistory(IndentingPrintWriter pw, int filterAppId) {
        int cmd;
        String label;
        int size = this.mEventIndices.size();
        if (size <= 0) {
            return false;
        }
        pw.increaseIndent();
        pw.println("Job history:");
        pw.decreaseIndent();
        long now = JobSchedulerService.sElapsedRealtimeClock.millis();
        for (int i = 0; i < size; i++) {
            int index = this.mEventIndices.indexOf(i);
            int uid = this.mEventUids[index];
            if ((filterAppId == -1 || filterAppId == UserHandle.getAppId(uid)) && (cmd = this.mEventCmds[index] & 255) != 0) {
                switch (cmd) {
                    case 1:
                        label = "  START";
                        break;
                    case 2:
                        label = "   STOP";
                        break;
                    case 3:
                        label = "START-P";
                        break;
                    case 4:
                        label = " STOP-P";
                        break;
                    default:
                        label = "     ??";
                        break;
                }
                TimeUtils.formatDuration(this.mEventTimes[index] - now, pw, 19);
                pw.print(" ");
                pw.print(label);
                pw.print(": #");
                UserHandle.formatUid(pw, uid);
                pw.print(SliceClientPermissions.SliceAuthority.DELIMITER);
                pw.print(this.mEventJobIds[index]);
                pw.print(" ");
                pw.print(this.mEventTags[index]);
                if (cmd == 2 || cmd == 4) {
                    pw.print(" ");
                    String[] strArr = this.mEventReasons;
                    String reason = strArr[index];
                    if (reason != null) {
                        pw.print(strArr[index]);
                    } else {
                        pw.print(JobParameters.getInternalReasonCodeDescription((this.mEventCmds[index] & EVENT_STOP_REASON_MASK) >> 8));
                    }
                }
                pw.println();
            }
        }
        return true;
    }

    public void dumpHistory(ProtoOutputStream proto, long fieldId, int filterUid) {
        int size;
        int i = filterUid;
        int size2 = this.mEventIndices.size();
        if (size2 == 0) {
            return;
        }
        long token = proto.start(fieldId);
        long now = JobSchedulerService.sElapsedRealtimeClock.millis();
        int i2 = 0;
        while (i2 < size2) {
            int index = this.mEventIndices.indexOf(i2);
            int uid = this.mEventUids[index];
            if (i != -1 && i != UserHandle.getAppId(uid)) {
                size = size2;
            } else {
                int cmd = this.mEventCmds[index] & 255;
                if (cmd == 0) {
                    size = size2;
                } else {
                    long heToken = proto.start(CompanionAppsPermissions.APP_PERMISSIONS);
                    proto.write(1159641169921L, cmd);
                    size = size2;
                    proto.write(1112396529666L, now - this.mEventTimes[index]);
                    proto.write(1120986464259L, uid);
                    proto.write(1120986464260L, this.mEventJobIds[index]);
                    proto.write(1138166333445L, this.mEventTags[index]);
                    if (cmd == 2 || cmd == 4) {
                        proto.write(1159641169926L, (this.mEventCmds[index] & EVENT_STOP_REASON_MASK) >> 8);
                    }
                    proto.end(heToken);
                }
            }
            i2++;
            i = filterUid;
            size2 = size;
        }
        proto.end(token);
    }
}
