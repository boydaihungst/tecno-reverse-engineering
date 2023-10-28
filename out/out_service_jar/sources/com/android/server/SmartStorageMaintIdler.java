package com.android.server;

import android.app.job.JobInfo;
import android.app.job.JobParameters;
import android.app.job.JobScheduler;
import android.app.job.JobService;
import android.content.ComponentName;
import android.content.Context;
import android.util.Slog;
import com.android.server.pm.PackageManagerService;
import java.util.concurrent.TimeUnit;
/* loaded from: classes.dex */
public class SmartStorageMaintIdler extends JobService {
    private static final int SMART_MAINT_JOB_ID = 2808;
    private static final ComponentName SMART_STORAGE_MAINT_SERVICE = new ComponentName(PackageManagerService.PLATFORM_PACKAGE_NAME, SmartStorageMaintIdler.class.getName());
    private static final String TAG = "SmartStorageMaintIdler";
    private final Runnable mFinishCallback = new Runnable() { // from class: com.android.server.SmartStorageMaintIdler.1
        @Override // java.lang.Runnable
        public void run() {
            Slog.i(SmartStorageMaintIdler.TAG, "Got smart storage maintenance service completion callback");
            if (SmartStorageMaintIdler.this.mStarted) {
                SmartStorageMaintIdler smartStorageMaintIdler = SmartStorageMaintIdler.this;
                smartStorageMaintIdler.jobFinished(smartStorageMaintIdler.mJobParams, false);
                SmartStorageMaintIdler.this.mStarted = false;
            }
            SmartStorageMaintIdler.scheduleSmartIdlePass(SmartStorageMaintIdler.this, StorageManagerService.sSmartIdleMaintPeriod);
        }
    };
    private JobParameters mJobParams;
    private boolean mStarted;

    @Override // android.app.job.JobService
    public boolean onStartJob(JobParameters params) {
        this.mJobParams = params;
        StorageManagerService ms = StorageManagerService.sSelf;
        if (ms != null) {
            this.mStarted = true;
            ms.runSmartIdleMaint(this.mFinishCallback);
        }
        return ms != null;
    }

    @Override // android.app.job.JobService
    public boolean onStopJob(JobParameters params) {
        this.mStarted = false;
        return false;
    }

    public static void scheduleSmartIdlePass(Context context, int nMinutes) {
        StorageManagerService ms = StorageManagerService.sSelf;
        if (ms == null || ms.isPassedLifetimeThresh()) {
            return;
        }
        JobScheduler tm = (JobScheduler) context.getSystemService(JobScheduler.class);
        long nextScheduleTime = TimeUnit.MINUTES.toMillis(nMinutes);
        JobInfo.Builder builder = new JobInfo.Builder(2808, SMART_STORAGE_MAINT_SERVICE);
        builder.setMinimumLatency(nextScheduleTime);
        tm.schedule(builder.build());
    }
}
