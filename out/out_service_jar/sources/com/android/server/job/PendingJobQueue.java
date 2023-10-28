package com.android.server.job;

import android.util.Pools;
import android.util.SparseArray;
import com.android.server.job.PendingJobQueue;
import com.android.server.job.controllers.JobStatus;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.PriorityQueue;
/* JADX INFO: Access modifiers changed from: package-private */
/* loaded from: classes.dex */
public class PendingJobQueue {
    private final Pools.Pool<AppJobQueue> mAppJobQueuePool = new Pools.SimplePool(8);
    private final SparseArray<AppJobQueue> mCurrentQueues = new SparseArray<>();
    private final PriorityQueue<AppJobQueue> mOrderedQueues = new PriorityQueue<>(new Comparator() { // from class: com.android.server.job.PendingJobQueue$$ExternalSyntheticLambda0
        @Override // java.util.Comparator
        public final int compare(Object obj, Object obj2) {
            return PendingJobQueue.lambda$new$0((PendingJobQueue.AppJobQueue) obj, (PendingJobQueue.AppJobQueue) obj2);
        }
    });
    private int mSize = 0;
    private boolean mOptimizeIteration = true;
    private int mPullCount = 0;
    private boolean mNeedToResetIterators = false;

    /* JADX INFO: Access modifiers changed from: package-private */
    public static /* synthetic */ int lambda$new$0(AppJobQueue ajq1, AppJobQueue ajq2) {
        long t1 = ajq1.peekNextTimestamp();
        long t2 = ajq2.peekNextTimestamp();
        if (t1 == -1) {
            if (t2 == -1) {
                return 0;
            }
            return 1;
        } else if (t2 == -1) {
            return -1;
        } else {
            int o1 = ajq1.peekNextOverrideState();
            int o2 = ajq2.peekNextOverrideState();
            if (o1 != o2) {
                return Integer.compare(o2, o1);
            }
            return Long.compare(t1, t2);
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void add(JobStatus job) {
        AppJobQueue ajq = getAppJobQueue(job.getSourceUid(), true);
        long prevTimestamp = ajq.peekNextTimestamp();
        ajq.add(job);
        this.mSize++;
        if (prevTimestamp != ajq.peekNextTimestamp()) {
            this.mOrderedQueues.remove(ajq);
            this.mOrderedQueues.offer(ajq);
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void addAll(List<JobStatus> jobs) {
        SparseArray<List<JobStatus>> jobsByUid = new SparseArray<>();
        for (int i = jobs.size() - 1; i >= 0; i--) {
            JobStatus job = jobs.get(i);
            List<JobStatus> appJobs = jobsByUid.get(job.getSourceUid());
            if (appJobs == null) {
                appJobs = new ArrayList<>();
                jobsByUid.put(job.getSourceUid(), appJobs);
            }
            appJobs.add(job);
        }
        int i2 = jobsByUid.size();
        for (int i3 = i2 - 1; i3 >= 0; i3--) {
            AppJobQueue ajq = getAppJobQueue(jobsByUid.keyAt(i3), true);
            ajq.addAll(jobsByUid.valueAt(i3));
        }
        int i4 = this.mSize;
        this.mSize = i4 + jobs.size();
        this.mOrderedQueues.clear();
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void clear() {
        this.mSize = 0;
        for (int i = this.mCurrentQueues.size() - 1; i >= 0; i--) {
            AppJobQueue ajq = this.mCurrentQueues.valueAt(i);
            ajq.clear();
            this.mAppJobQueuePool.release(ajq);
        }
        this.mCurrentQueues.clear();
        this.mOrderedQueues.clear();
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean contains(JobStatus job) {
        AppJobQueue ajq = this.mCurrentQueues.get(job.getSourceUid());
        if (ajq == null) {
            return false;
        }
        return ajq.contains(job);
    }

    private AppJobQueue getAppJobQueue(int uid, boolean create) {
        AppJobQueue ajq = this.mCurrentQueues.get(uid);
        if (ajq == null && create) {
            ajq = (AppJobQueue) this.mAppJobQueuePool.acquire();
            if (ajq == null) {
                ajq = new AppJobQueue();
            }
            this.mCurrentQueues.put(uid, ajq);
        }
        return ajq;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public JobStatus next() {
        if (this.mNeedToResetIterators) {
            this.mOrderedQueues.clear();
            for (int i = this.mCurrentQueues.size() - 1; i >= 0; i--) {
                AppJobQueue ajq = this.mCurrentQueues.valueAt(i);
                ajq.resetIterator(0L);
                this.mOrderedQueues.offer(ajq);
            }
            this.mNeedToResetIterators = false;
            this.mPullCount = 0;
        } else if (this.mOrderedQueues.size() == 0) {
            for (int i2 = this.mCurrentQueues.size() - 1; i2 >= 0; i2--) {
                this.mOrderedQueues.offer(this.mCurrentQueues.valueAt(i2));
            }
            this.mPullCount = 0;
        }
        int numQueues = this.mOrderedQueues.size();
        if (numQueues == 0) {
            return null;
        }
        int pullLimit = this.mOptimizeIteration ? Math.min(3, ((numQueues - 1) >>> 2) + 1) : 1;
        AppJobQueue earliestQueue = this.mOrderedQueues.peek();
        if (earliestQueue == null) {
            return null;
        }
        JobStatus job = earliestQueue.next();
        int i3 = this.mPullCount + 1;
        this.mPullCount = i3;
        if (i3 >= pullLimit || ((job != null && earliestQueue.peekNextOverrideState() != job.overrideState) || earliestQueue.peekNextTimestamp() == -1)) {
            this.mOrderedQueues.poll();
            if (earliestQueue.peekNextTimestamp() != -1) {
                this.mOrderedQueues.offer(earliestQueue);
            }
            this.mPullCount = 0;
        }
        return job;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean remove(JobStatus job) {
        AppJobQueue ajq = getAppJobQueue(job.getSourceUid(), false);
        if (ajq == null) {
            return false;
        }
        long prevTimestamp = ajq.peekNextTimestamp();
        if (ajq.remove(job)) {
            this.mSize--;
            if (ajq.size() == 0) {
                this.mCurrentQueues.remove(job.getSourceUid());
                this.mOrderedQueues.remove(ajq);
                ajq.clear();
                this.mAppJobQueuePool.release(ajq);
            } else if (prevTimestamp != ajq.peekNextTimestamp()) {
                this.mOrderedQueues.remove(ajq);
                this.mOrderedQueues.offer(ajq);
            }
            return true;
        }
        return false;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void resetIterator() {
        this.mNeedToResetIterators = true;
    }

    void setOptimizeIteration(boolean optimize) {
        this.mOptimizeIteration = optimize;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public int size() {
        return this.mSize;
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes.dex */
    public static final class AppJobQueue {
        static final int NO_NEXT_OVERRIDE_STATE = -1;
        static final long NO_NEXT_TIMESTAMP = -1;
        private int mCurIndex;
        private final List<AdjustedJobStatus> mJobs;
        private static final Comparator<AdjustedJobStatus> sJobComparator = new Comparator() { // from class: com.android.server.job.PendingJobQueue$AppJobQueue$$ExternalSyntheticLambda0
            @Override // java.util.Comparator
            public final int compare(Object obj, Object obj2) {
                return PendingJobQueue.AppJobQueue.lambda$static$0((PendingJobQueue.AppJobQueue.AdjustedJobStatus) obj, (PendingJobQueue.AppJobQueue.AdjustedJobStatus) obj2);
            }
        };
        private static final Pools.Pool<AdjustedJobStatus> mAdjustedJobStatusPool = new Pools.SimplePool(16);

        private AppJobQueue() {
            this.mJobs = new ArrayList();
            this.mCurIndex = 0;
        }

        /* JADX INFO: Access modifiers changed from: private */
        /* loaded from: classes.dex */
        public static class AdjustedJobStatus {
            public long adjustedEnqueueTime;
            public JobStatus job;

            private AdjustedJobStatus() {
            }

            void clear() {
                this.adjustedEnqueueTime = 0L;
                this.job = null;
            }
        }

        /* JADX INFO: Access modifiers changed from: package-private */
        public static /* synthetic */ int lambda$static$0(AdjustedJobStatus aj1, AdjustedJobStatus aj2) {
            JobStatus job1;
            JobStatus job2;
            if (aj1 == aj2 || (job1 = aj1.job) == (job2 = aj2.job)) {
                return 0;
            }
            if (job1.overrideState != job2.overrideState) {
                return Integer.compare(job2.overrideState, job1.overrideState);
            }
            boolean job1EJ = job1.isRequestedExpeditedJob();
            boolean job2EJ = job2.isRequestedExpeditedJob();
            if (job1EJ != job2EJ) {
                return job1EJ ? -1 : 1;
            }
            int job1Priority = job1.getEffectivePriority();
            int job2Priority = job2.getEffectivePriority();
            if (job1Priority != job2Priority) {
                return Integer.compare(job2Priority, job1Priority);
            }
            if (job1.lastEvaluatedBias != job2.lastEvaluatedBias) {
                return Integer.compare(job2.lastEvaluatedBias, job1.lastEvaluatedBias);
            }
            return Long.compare(job1.enqueueTime, job2.enqueueTime);
        }

        void add(JobStatus jobStatus) {
            AdjustedJobStatus adjustedJobStatus = (AdjustedJobStatus) mAdjustedJobStatusPool.acquire();
            if (adjustedJobStatus == null) {
                adjustedJobStatus = new AdjustedJobStatus();
            }
            adjustedJobStatus.adjustedEnqueueTime = jobStatus.enqueueTime;
            adjustedJobStatus.job = jobStatus;
            int where = Collections.binarySearch(this.mJobs, adjustedJobStatus, sJobComparator);
            if (where < 0) {
                where = ~where;
            }
            this.mJobs.add(where, adjustedJobStatus);
            if (where < this.mCurIndex) {
                this.mCurIndex = where;
            }
            if (where > 0) {
                long prevTimestamp = this.mJobs.get(where - 1).adjustedEnqueueTime;
                adjustedJobStatus.adjustedEnqueueTime = Math.max(prevTimestamp, adjustedJobStatus.adjustedEnqueueTime);
            }
            int numJobs = this.mJobs.size();
            if (where < numJobs - 1) {
                for (int i = where; i < numJobs; i++) {
                    AdjustedJobStatus ajs = this.mJobs.get(i);
                    if (adjustedJobStatus.adjustedEnqueueTime >= ajs.adjustedEnqueueTime) {
                        ajs.adjustedEnqueueTime = adjustedJobStatus.adjustedEnqueueTime;
                    } else {
                        return;
                    }
                }
            }
        }

        void addAll(List<JobStatus> jobs) {
            int earliestIndex = Integer.MAX_VALUE;
            for (int i = jobs.size() - 1; i >= 0; i--) {
                JobStatus job = jobs.get(i);
                AdjustedJobStatus adjustedJobStatus = (AdjustedJobStatus) mAdjustedJobStatusPool.acquire();
                if (adjustedJobStatus == null) {
                    adjustedJobStatus = new AdjustedJobStatus();
                }
                adjustedJobStatus.adjustedEnqueueTime = job.enqueueTime;
                adjustedJobStatus.job = job;
                int where = Collections.binarySearch(this.mJobs, adjustedJobStatus, sJobComparator);
                if (where < 0) {
                    where = ~where;
                }
                this.mJobs.add(where, adjustedJobStatus);
                if (where < this.mCurIndex) {
                    this.mCurIndex = where;
                }
                earliestIndex = Math.min(earliestIndex, where);
            }
            int numJobs = this.mJobs.size();
            for (int i2 = Math.max(earliestIndex, 1); i2 < numJobs; i2++) {
                AdjustedJobStatus ajs = this.mJobs.get(i2);
                AdjustedJobStatus prev = this.mJobs.get(i2 - 1);
                ajs.adjustedEnqueueTime = Math.max(ajs.adjustedEnqueueTime, prev.adjustedEnqueueTime);
            }
        }

        void clear() {
            this.mJobs.clear();
            this.mCurIndex = 0;
        }

        boolean contains(JobStatus job) {
            return indexOf(job) >= 0;
        }

        private int indexOf(JobStatus jobStatus) {
            int size = this.mJobs.size();
            for (int i = 0; i < size; i++) {
                AdjustedJobStatus adjustedJobStatus = this.mJobs.get(i);
                if (adjustedJobStatus.job == jobStatus) {
                    return i;
                }
            }
            return -1;
        }

        JobStatus next() {
            if (this.mCurIndex >= this.mJobs.size()) {
                return null;
            }
            List<AdjustedJobStatus> list = this.mJobs;
            int i = this.mCurIndex;
            this.mCurIndex = i + 1;
            return list.get(i).job;
        }

        int peekNextOverrideState() {
            if (this.mCurIndex >= this.mJobs.size()) {
                return -1;
            }
            return this.mJobs.get(this.mCurIndex).job.overrideState;
        }

        long peekNextTimestamp() {
            if (this.mCurIndex >= this.mJobs.size()) {
                return -1L;
            }
            return this.mJobs.get(this.mCurIndex).adjustedEnqueueTime;
        }

        boolean remove(JobStatus jobStatus) {
            int idx = indexOf(jobStatus);
            if (idx < 0) {
                return false;
            }
            AdjustedJobStatus adjustedJobStatus = this.mJobs.remove(idx);
            adjustedJobStatus.clear();
            mAdjustedJobStatusPool.release(adjustedJobStatus);
            int i = this.mCurIndex;
            if (idx < i) {
                this.mCurIndex = i - 1;
            }
            return true;
        }

        void resetIterator(long earliestEnqueueTime) {
            if (earliestEnqueueTime == 0 || this.mJobs.size() == 0) {
                this.mCurIndex = 0;
                return;
            }
            int low = 0;
            int high = this.mJobs.size() - 1;
            while (low < high) {
                int mid = (low + high) >>> 1;
                AdjustedJobStatus midVal = this.mJobs.get(mid);
                if (midVal.adjustedEnqueueTime < earliestEnqueueTime) {
                    low = mid + 1;
                } else if (midVal.adjustedEnqueueTime > earliestEnqueueTime) {
                    high = mid - 1;
                } else {
                    high = mid;
                }
            }
            this.mCurIndex = high;
        }

        int size() {
            return this.mJobs.size();
        }
    }
}
