package com.android.server.backup;

import android.app.job.JobInfo;
import android.app.job.JobParameters;
import android.app.job.JobScheduler;
import android.app.job.JobService;
import android.content.ComponentName;
import android.content.Context;
import android.os.Bundle;
import android.util.SparseArray;
import com.android.server.pm.PackageManagerService;
/* loaded from: classes.dex */
public class FullBackupJob extends JobService {
    public static final int MAX_JOB_ID = 52419896;
    public static final int MIN_JOB_ID = 52418896;
    private static final String USER_ID_EXTRA_KEY = "userId";
    private static ComponentName sIdleService = new ComponentName(PackageManagerService.PLATFORM_PACKAGE_NAME, FullBackupJob.class.getName());
    private final SparseArray<JobParameters> mParamsForUser = new SparseArray<>();

    public static void schedule(int userId, Context ctx, long minDelay, BackupManagerConstants constants) {
        JobScheduler js = (JobScheduler) ctx.getSystemService("jobscheduler");
        JobInfo.Builder builder = new JobInfo.Builder(getJobIdForUserId(userId), sIdleService);
        synchronized (constants) {
            builder.setRequiresDeviceIdle(true).setRequiredNetworkType(constants.getFullBackupRequiredNetworkType()).setRequiresCharging(constants.getFullBackupRequireCharging());
        }
        if (minDelay > 0) {
            builder.setMinimumLatency(minDelay);
        }
        Bundle extraInfo = new Bundle();
        extraInfo.putInt("userId", userId);
        builder.setTransientExtras(extraInfo);
        js.schedule(builder.build());
    }

    public static void cancel(int userId, Context ctx) {
        JobScheduler js = (JobScheduler) ctx.getSystemService("jobscheduler");
        js.cancel(getJobIdForUserId(userId));
    }

    public void finishBackupPass(int userId) {
        synchronized (this.mParamsForUser) {
            JobParameters jobParameters = this.mParamsForUser.get(userId);
            if (jobParameters != null) {
                jobFinished(jobParameters, false);
                this.mParamsForUser.remove(userId);
            }
        }
    }

    @Override // android.app.job.JobService
    public boolean onStartJob(JobParameters params) {
        int userId = params.getTransientExtras().getInt("userId");
        synchronized (this.mParamsForUser) {
            this.mParamsForUser.put(userId, params);
        }
        BackupManagerService service = BackupManagerService.getInstance();
        return service.beginFullBackup(userId, this);
    }

    @Override // android.app.job.JobService
    public boolean onStopJob(JobParameters params) {
        int userId = params.getTransientExtras().getInt("userId");
        synchronized (this.mParamsForUser) {
            if (this.mParamsForUser.removeReturnOld(userId) == null) {
                return false;
            }
            BackupManagerService service = BackupManagerService.getInstance();
            service.endFullBackup(userId);
            return false;
        }
    }

    private static int getJobIdForUserId(int userId) {
        return JobIdManager.getJobIdForUserId(52418896, MAX_JOB_ID, userId);
    }
}
