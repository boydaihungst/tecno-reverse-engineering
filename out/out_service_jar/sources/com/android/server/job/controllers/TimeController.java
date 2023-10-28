package com.android.server.job.controllers;

import android.app.AlarmManager;
import android.os.UserHandle;
import android.os.WorkSource;
import android.util.ArraySet;
import android.util.IndentingPrintWriter;
import android.util.Log;
import android.util.Slog;
import android.util.TimeUtils;
import android.util.proto.ProtoOutputStream;
import com.android.server.job.JobSchedulerService;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;
import java.util.function.Predicate;
/* loaded from: classes.dex */
public final class TimeController extends StateController {
    private static final boolean DEBUG;
    static final long DELAY_COALESCE_TIME_MS = 30000;
    private static final String TAG = "JobScheduler.Time";
    private final String DEADLINE_TAG;
    private final String DELAY_TAG;
    private AlarmManager mAlarmService;
    private final AlarmManager.OnAlarmListener mDeadlineExpiredListener;
    private volatile long mLastFiredDelayExpiredElapsedMillis;
    private long mNextDelayExpiredElapsedMillis;
    private final AlarmManager.OnAlarmListener mNextDelayExpiredListener;
    private long mNextJobExpiredElapsedMillis;
    private final List<JobStatus> mTrackedJobs;

    static {
        DEBUG = JobSchedulerService.DEBUG || Log.isLoggable(TAG, 3);
    }

    public TimeController(JobSchedulerService service) {
        super(service);
        this.DEADLINE_TAG = "*job.deadline*";
        this.DELAY_TAG = "*job.delay*";
        this.mAlarmService = null;
        this.mTrackedJobs = new LinkedList();
        this.mDeadlineExpiredListener = new AlarmManager.OnAlarmListener() { // from class: com.android.server.job.controllers.TimeController.1
            @Override // android.app.AlarmManager.OnAlarmListener
            public void onAlarm() {
                if (TimeController.DEBUG) {
                    Slog.d(TimeController.TAG, "Deadline-expired alarm fired");
                }
                TimeController.this.checkExpiredDeadlinesAndResetAlarm();
            }
        };
        this.mNextDelayExpiredListener = new AlarmManager.OnAlarmListener() { // from class: com.android.server.job.controllers.TimeController.2
            @Override // android.app.AlarmManager.OnAlarmListener
            public void onAlarm() {
                if (TimeController.DEBUG) {
                    Slog.d(TimeController.TAG, "Delay-expired alarm fired");
                }
                TimeController.this.mLastFiredDelayExpiredElapsedMillis = JobSchedulerService.sElapsedRealtimeClock.millis();
                TimeController.this.checkExpiredDelaysAndResetAlarm();
            }
        };
        this.mNextJobExpiredElapsedMillis = JobStatus.NO_LATEST_RUNTIME;
        this.mNextDelayExpiredElapsedMillis = JobStatus.NO_LATEST_RUNTIME;
    }

    @Override // com.android.server.job.controllers.StateController
    public void maybeStartTrackingJobLocked(JobStatus job, JobStatus lastJob) {
        if (job.hasTimingDelayConstraint() || job.hasDeadlineConstraint()) {
            maybeStopTrackingJobLocked(job, null, false);
            long nowElapsedMillis = JobSchedulerService.sElapsedRealtimeClock.millis();
            if (job.hasDeadlineConstraint() && evaluateDeadlineConstraint(job, nowElapsedMillis)) {
                return;
            }
            if (job.hasTimingDelayConstraint() && evaluateTimingDelayConstraint(job, nowElapsedMillis) && !job.hasDeadlineConstraint()) {
                return;
            }
            boolean isInsert = false;
            List<JobStatus> list = this.mTrackedJobs;
            ListIterator<JobStatus> it = list.listIterator(list.size());
            while (true) {
                if (!it.hasPrevious()) {
                    break;
                }
                JobStatus ts = it.previous();
                if (ts.getLatestRunTimeElapsed() < job.getLatestRunTimeElapsed()) {
                    isInsert = true;
                    break;
                }
            }
            if (isInsert) {
                it.next();
            }
            it.add(job);
            job.setTrackingController(32);
            WorkSource ws = this.mService.deriveWorkSource(job.getSourceUid(), job.getSourcePackageName());
            if (job.hasTimingDelayConstraint() && wouldBeReadyWithConstraintLocked(job, Integer.MIN_VALUE)) {
                maybeUpdateDelayAlarmLocked(job.getEarliestRunTime(), ws);
            }
            if (job.hasDeadlineConstraint() && wouldBeReadyWithConstraintLocked(job, 1073741824)) {
                maybeUpdateDeadlineAlarmLocked(job.getLatestRunTimeElapsed(), ws);
            }
        }
    }

    @Override // com.android.server.job.controllers.StateController
    public void maybeStopTrackingJobLocked(JobStatus job, JobStatus incomingJob, boolean forUpdate) {
        if (job.clearTrackingController(32) && this.mTrackedJobs.remove(job)) {
            checkExpiredDelaysAndResetAlarm();
            checkExpiredDeadlinesAndResetAlarm();
        }
    }

    @Override // com.android.server.job.controllers.StateController
    public void evaluateStateLocked(JobStatus job) {
        long nowElapsedMillis = JobSchedulerService.sElapsedRealtimeClock.millis();
        if (job.hasDeadlineConstraint() && !job.isConstraintSatisfied(1073741824) && job.getLatestRunTimeElapsed() <= this.mNextJobExpiredElapsedMillis) {
            if (evaluateDeadlineConstraint(job, nowElapsedMillis)) {
                if (job.isReady()) {
                    this.mStateChangedListener.onRunJobNow(job);
                }
            } else if (wouldBeReadyWithConstraintLocked(job, 1073741824)) {
                setDeadlineExpiredAlarmLocked(job.getLatestRunTimeElapsed(), this.mService.deriveWorkSource(job.getSourceUid(), job.getSourcePackageName()));
            }
        }
        if (job.hasTimingDelayConstraint() && !job.isConstraintSatisfied(Integer.MIN_VALUE) && job.getEarliestRunTime() <= this.mNextDelayExpiredElapsedMillis && !evaluateTimingDelayConstraint(job, nowElapsedMillis) && wouldBeReadyWithConstraintLocked(job, Integer.MIN_VALUE)) {
            setDelayExpiredAlarmLocked(job.getEarliestRunTime(), this.mService.deriveWorkSource(job.getSourceUid(), job.getSourcePackageName()));
        }
    }

    @Override // com.android.server.job.controllers.StateController
    public void reevaluateStateLocked(int uid) {
        checkExpiredDeadlinesAndResetAlarm();
        checkExpiredDelaysAndResetAlarm();
    }

    private boolean canStopTrackingJobLocked(JobStatus job) {
        return (!job.hasTimingDelayConstraint() || job.isConstraintSatisfied(Integer.MIN_VALUE)) && (!job.hasDeadlineConstraint() || job.isConstraintSatisfied(1073741824));
    }

    private void ensureAlarmServiceLocked() {
        if (this.mAlarmService == null) {
            this.mAlarmService = (AlarmManager) this.mContext.getSystemService("alarm");
        }
    }

    void checkExpiredDeadlinesAndResetAlarm() {
        synchronized (this.mLock) {
            long nextExpiryTime = JobStatus.NO_LATEST_RUNTIME;
            int nextExpiryUid = 0;
            String nextExpiryPackageName = null;
            long nowElapsedMillis = JobSchedulerService.sElapsedRealtimeClock.millis();
            ListIterator<JobStatus> it = this.mTrackedJobs.listIterator();
            while (true) {
                if (!it.hasNext()) {
                    break;
                }
                JobStatus job = it.next();
                if (job.hasDeadlineConstraint()) {
                    if (evaluateDeadlineConstraint(job, nowElapsedMillis)) {
                        if (job.isReady()) {
                            this.mStateChangedListener.onRunJobNow(job);
                        }
                        it.remove();
                    } else if (!wouldBeReadyWithConstraintLocked(job, 1073741824)) {
                        if (DEBUG) {
                            Slog.i(TAG, "Skipping " + job + " because deadline won't make it ready.");
                        }
                    } else {
                        nextExpiryTime = job.getLatestRunTimeElapsed();
                        nextExpiryUid = job.getSourceUid();
                        nextExpiryPackageName = job.getSourcePackageName();
                        break;
                    }
                }
            }
            setDeadlineExpiredAlarmLocked(nextExpiryTime, this.mService.deriveWorkSource(nextExpiryUid, nextExpiryPackageName));
        }
    }

    private boolean evaluateDeadlineConstraint(JobStatus job, long nowElapsedMillis) {
        long jobDeadline = job.getLatestRunTimeElapsed();
        if (jobDeadline <= nowElapsedMillis) {
            if (job.hasTimingDelayConstraint()) {
                job.setTimingDelayConstraintSatisfied(nowElapsedMillis, true);
            }
            job.setDeadlineConstraintSatisfied(nowElapsedMillis, true);
            return true;
        }
        return false;
    }

    void checkExpiredDelaysAndResetAlarm() {
        synchronized (this.mLock) {
            long nextDelayTime = JobStatus.NO_LATEST_RUNTIME;
            int nextDelayUid = 0;
            String nextDelayPackageName = null;
            ArraySet<JobStatus> changedJobs = new ArraySet<>();
            Iterator<JobStatus> it = this.mTrackedJobs.iterator();
            long nowElapsedMillis = JobSchedulerService.sElapsedRealtimeClock.millis();
            while (it.hasNext()) {
                JobStatus job = it.next();
                if (job.hasTimingDelayConstraint()) {
                    if (evaluateTimingDelayConstraint(job, nowElapsedMillis)) {
                        if (canStopTrackingJobLocked(job)) {
                            it.remove();
                        }
                        changedJobs.add(job);
                    } else if (!wouldBeReadyWithConstraintLocked(job, Integer.MIN_VALUE)) {
                        if (DEBUG) {
                            Slog.i(TAG, "Skipping " + job + " because delay won't make it ready.");
                        }
                    } else {
                        long jobDelayTime = job.getEarliestRunTime();
                        if (nextDelayTime > jobDelayTime) {
                            nextDelayTime = jobDelayTime;
                            nextDelayUid = job.getSourceUid();
                            nextDelayPackageName = job.getSourcePackageName();
                        }
                    }
                }
            }
            if (changedJobs.size() > 0) {
                this.mStateChangedListener.onControllerStateChanged(changedJobs);
            }
            setDelayExpiredAlarmLocked(nextDelayTime, this.mService.deriveWorkSource(nextDelayUid, nextDelayPackageName));
        }
    }

    private boolean evaluateTimingDelayConstraint(JobStatus job, long nowElapsedMillis) {
        long jobDelayTime = job.getEarliestRunTime();
        if (jobDelayTime <= nowElapsedMillis) {
            job.setTimingDelayConstraintSatisfied(nowElapsedMillis, true);
            return true;
        }
        return false;
    }

    private void maybeUpdateDelayAlarmLocked(long delayExpiredElapsed, WorkSource ws) {
        if (delayExpiredElapsed < this.mNextDelayExpiredElapsedMillis) {
            setDelayExpiredAlarmLocked(delayExpiredElapsed, ws);
        }
    }

    private void maybeUpdateDeadlineAlarmLocked(long deadlineExpiredElapsed, WorkSource ws) {
        if (deadlineExpiredElapsed < this.mNextJobExpiredElapsedMillis) {
            setDeadlineExpiredAlarmLocked(deadlineExpiredElapsed, ws);
        }
    }

    private void setDelayExpiredAlarmLocked(long alarmTimeElapsedMillis, WorkSource ws) {
        long alarmTimeElapsedMillis2 = maybeAdjustAlarmTime(Math.max(alarmTimeElapsedMillis, this.mLastFiredDelayExpiredElapsedMillis + 30000));
        if (this.mNextDelayExpiredElapsedMillis == alarmTimeElapsedMillis2) {
            return;
        }
        this.mNextDelayExpiredElapsedMillis = alarmTimeElapsedMillis2;
        updateAlarmWithListenerLocked("*job.delay*", 3, this.mNextDelayExpiredListener, alarmTimeElapsedMillis2, ws);
    }

    private void setDeadlineExpiredAlarmLocked(long alarmTimeElapsedMillis, WorkSource ws) {
        long alarmTimeElapsedMillis2 = maybeAdjustAlarmTime(alarmTimeElapsedMillis);
        if (this.mNextJobExpiredElapsedMillis == alarmTimeElapsedMillis2) {
            return;
        }
        this.mNextJobExpiredElapsedMillis = alarmTimeElapsedMillis2;
        updateAlarmWithListenerLocked("*job.deadline*", 2, this.mDeadlineExpiredListener, alarmTimeElapsedMillis2, ws);
    }

    private long maybeAdjustAlarmTime(long proposedAlarmTimeElapsedMillis) {
        return Math.max(proposedAlarmTimeElapsedMillis, JobSchedulerService.sElapsedRealtimeClock.millis());
    }

    private void updateAlarmWithListenerLocked(String tag, int alarmType, AlarmManager.OnAlarmListener listener, long alarmTimeElapsed, WorkSource ws) {
        ensureAlarmServiceLocked();
        if (alarmTimeElapsed == JobStatus.NO_LATEST_RUNTIME) {
            this.mAlarmService.cancel(listener);
            return;
        }
        if (DEBUG) {
            Slog.d(TAG, "Setting " + tag + " for: " + alarmTimeElapsed);
        }
        this.mAlarmService.set(alarmType, alarmTimeElapsed, -1L, 0L, tag, listener, null, ws);
    }

    @Override // com.android.server.job.controllers.StateController
    public void dumpControllerStateLocked(IndentingPrintWriter pw, Predicate<JobStatus> predicate) {
        long nowElapsed = JobSchedulerService.sElapsedRealtimeClock.millis();
        pw.println("Elapsed clock: " + nowElapsed);
        pw.print("Next delay alarm in ");
        TimeUtils.formatDuration(this.mNextDelayExpiredElapsedMillis, nowElapsed, pw);
        pw.println();
        pw.print("Last delay alarm fired @ ");
        TimeUtils.formatDuration(nowElapsed, this.mLastFiredDelayExpiredElapsedMillis, pw);
        pw.println();
        pw.print("Next deadline alarm in ");
        TimeUtils.formatDuration(this.mNextJobExpiredElapsedMillis, nowElapsed, pw);
        pw.println();
        pw.println();
        for (JobStatus ts : this.mTrackedJobs) {
            if (predicate.test(ts)) {
                pw.print("#");
                ts.printUniqueId(pw);
                pw.print(" from ");
                UserHandle.formatUid(pw, ts.getSourceUid());
                pw.print(": Delay=");
                if (ts.hasTimingDelayConstraint()) {
                    TimeUtils.formatDuration(ts.getEarliestRunTime(), nowElapsed, pw);
                } else {
                    pw.print("N/A");
                }
                pw.print(", Deadline=");
                if (ts.hasDeadlineConstraint()) {
                    TimeUtils.formatDuration(ts.getLatestRunTimeElapsed(), nowElapsed, pw);
                } else {
                    pw.print("N/A");
                }
                pw.println();
            }
        }
    }

    @Override // com.android.server.job.controllers.StateController
    public void dumpControllerStateLocked(ProtoOutputStream proto, long fieldId, Predicate<JobStatus> predicate) {
        long token = proto.start(fieldId);
        long mToken = proto.start(1146756268040L);
        long nowElapsed = JobSchedulerService.sElapsedRealtimeClock.millis();
        proto.write(1112396529665L, nowElapsed);
        proto.write(1112396529666L, this.mNextDelayExpiredElapsedMillis - nowElapsed);
        proto.write(1112396529667L, this.mNextJobExpiredElapsedMillis - nowElapsed);
        for (JobStatus ts : this.mTrackedJobs) {
            if (predicate.test(ts)) {
                long tsToken = proto.start(2246267895812L);
                ts.writeToShortProto(proto, 1146756268033L);
                proto.write(1133871366147L, ts.hasTimingDelayConstraint());
                long token2 = token;
                long token3 = ts.getEarliestRunTime() - nowElapsed;
                proto.write(1112396529668L, token3);
                proto.write(1133871366149L, ts.hasDeadlineConstraint());
                proto.write(1112396529670L, ts.getLatestRunTimeElapsed() - nowElapsed);
                proto.end(tsToken);
                token = token2;
            }
        }
        proto.end(mToken);
        proto.end(token);
    }
}
