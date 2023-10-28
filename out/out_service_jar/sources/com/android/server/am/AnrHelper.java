package com.android.server.am;

import android.content.pm.ApplicationInfo;
import android.os.SystemClock;
import android.util.Slog;
import com.android.server.wm.WindowProcessController;
import java.util.ArrayList;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
/* JADX INFO: Access modifiers changed from: package-private */
/* loaded from: classes.dex */
public class AnrHelper {
    private static final String TAG = "ActivityManager";
    private final ActivityManagerService mService;
    private static final long EXPIRED_REPORT_TIME_MS = TimeUnit.MINUTES.toMillis(1);
    private static final long CONSECUTIVE_ANR_TIME_MS = TimeUnit.MINUTES.toMillis(2);
    private final ArrayList<AnrRecord> mAnrRecords = new ArrayList<>();
    private final AtomicBoolean mRunning = new AtomicBoolean(false);
    private long mLastAnrTimeMs = 0;
    private int mProcessingPid = -1;

    /* JADX INFO: Access modifiers changed from: package-private */
    public AnrHelper(ActivityManagerService service) {
        this.mService = service;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void appNotResponding(ProcessRecord anrProcess, String annotation) {
        appNotResponding(anrProcess, null, null, null, null, false, annotation);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void appNotResponding(ProcessRecord anrProcess, String activityShortComponentName, ApplicationInfo aInfo, String parentShortComponentName, WindowProcessController parentProcess, boolean aboveSystem, String annotation) {
        int incomingPid = anrProcess.mPid;
        synchronized (this.mAnrRecords) {
            if (incomingPid == 0) {
                Slog.i(TAG, "Skip zero pid ANR, process=" + anrProcess.processName);
            } else if (this.mProcessingPid == incomingPid) {
                Slog.i(TAG, "Skip duplicated ANR, pid=" + incomingPid + " " + annotation);
            } else {
                for (int i = this.mAnrRecords.size() - 1; i >= 0; i--) {
                    if (this.mAnrRecords.get(i).mPid == incomingPid) {
                        Slog.i(TAG, "Skip queued ANR, pid=" + incomingPid + " " + annotation);
                        return;
                    }
                }
                this.mAnrRecords.add(new AnrRecord(anrProcess, activityShortComponentName, aInfo, parentShortComponentName, parentProcess, aboveSystem, annotation));
                startAnrConsumerIfNeeded();
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void startAnrConsumerIfNeeded() {
        if (this.mRunning.compareAndSet(false, true)) {
            new AnrConsumerThread().start();
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes.dex */
    public class AnrConsumerThread extends Thread {
        AnrConsumerThread() {
            super("AnrConsumer");
        }

        private AnrRecord next() {
            synchronized (AnrHelper.this.mAnrRecords) {
                if (AnrHelper.this.mAnrRecords.isEmpty()) {
                    return null;
                }
                AnrRecord record = (AnrRecord) AnrHelper.this.mAnrRecords.remove(0);
                AnrHelper.this.mProcessingPid = record.mPid;
                return record;
            }
        }

        @Override // java.lang.Thread, java.lang.Runnable
        public void run() {
            while (true) {
                AnrRecord r = next();
                if (r == null) {
                    break;
                }
                AnrHelper.this.scheduleBinderHeavyHitterAutoSamplerIfNecessary();
                int currentPid = r.mApp.mPid;
                if (currentPid != r.mPid) {
                    Slog.i(AnrHelper.TAG, "Skip ANR with mismatched pid=" + r.mPid + ", current pid=" + currentPid);
                } else {
                    long startTime = SystemClock.uptimeMillis();
                    long reportLatency = startTime - r.mTimestamp;
                    boolean onlyDumpSelf = reportLatency > AnrHelper.EXPIRED_REPORT_TIME_MS;
                    r.appNotResponding(onlyDumpSelf);
                    long endTime = SystemClock.uptimeMillis();
                    Slog.d(AnrHelper.TAG, "Completed ANR of " + r.mApp.processName + " in " + (endTime - startTime) + "ms, latency " + reportLatency + (onlyDumpSelf ? "ms (expired, only dump ANR app)" : "ms"));
                }
            }
            AnrHelper.this.mRunning.set(false);
            synchronized (AnrHelper.this.mAnrRecords) {
                AnrHelper.this.mProcessingPid = -1;
                if (!AnrHelper.this.mAnrRecords.isEmpty()) {
                    AnrHelper.this.startAnrConsumerIfNeeded();
                }
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void scheduleBinderHeavyHitterAutoSamplerIfNecessary() {
        long now = SystemClock.uptimeMillis();
        if (this.mLastAnrTimeMs + CONSECUTIVE_ANR_TIME_MS > now) {
            this.mService.scheduleBinderHeavyHitterAutoSampler();
        }
        this.mLastAnrTimeMs = now;
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes.dex */
    public static class AnrRecord {
        final boolean mAboveSystem;
        final String mActivityShortComponentName;
        final String mAnnotation;
        final ProcessRecord mApp;
        final ApplicationInfo mAppInfo;
        final WindowProcessController mParentProcess;
        final String mParentShortComponentName;
        final int mPid;
        final long mTimestamp = SystemClock.uptimeMillis();

        AnrRecord(ProcessRecord anrProcess, String activityShortComponentName, ApplicationInfo aInfo, String parentShortComponentName, WindowProcessController parentProcess, boolean aboveSystem, String annotation) {
            this.mApp = anrProcess;
            this.mPid = anrProcess.mPid;
            this.mActivityShortComponentName = activityShortComponentName;
            this.mParentShortComponentName = parentShortComponentName;
            this.mAnnotation = annotation;
            this.mAppInfo = aInfo;
            this.mParentProcess = parentProcess;
            this.mAboveSystem = aboveSystem;
        }

        void appNotResponding(boolean onlyDumpSelf) {
            this.mApp.mErrorState.appNotResponding(this.mActivityShortComponentName, this.mAppInfo, this.mParentShortComponentName, this.mParentProcess, this.mAboveSystem, this.mAnnotation, onlyDumpSelf);
        }
    }
}
