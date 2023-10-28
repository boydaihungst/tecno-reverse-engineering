package com.android.server;

import android.app.ActivityManager;
import android.app.job.JobInfo;
import android.app.job.JobParameters;
import android.app.job.JobScheduler;
import android.app.job.JobService;
import android.content.ComponentName;
import android.content.Context;
import android.os.RemoteException;
import android.util.Slog;
import com.android.server.pm.PackageManagerService;
import java.util.Calendar;
import java.util.concurrent.TimeUnit;
/* loaded from: classes.dex */
public class MountServiceIdler extends JobService {
    private static final String TAG = "MountServiceIdler";
    private Runnable mFinishCallback = new Runnable() { // from class: com.android.server.MountServiceIdler.1
        @Override // java.lang.Runnable
        public void run() {
            Slog.i(MountServiceIdler.TAG, "Got mount service completion callback");
            synchronized (MountServiceIdler.this.mFinishCallback) {
                if (MountServiceIdler.this.mStarted) {
                    MountServiceIdler mountServiceIdler = MountServiceIdler.this;
                    mountServiceIdler.jobFinished(mountServiceIdler.mJobParams, false);
                    MountServiceIdler.this.mStarted = false;
                }
            }
            MountServiceIdler.scheduleIdlePass(MountServiceIdler.this);
        }
    };
    private JobParameters mJobParams;
    private boolean mStarted;
    private static ComponentName sIdleService = new ComponentName(PackageManagerService.PLATFORM_PACKAGE_NAME, MountServiceIdler.class.getName());
    private static int MOUNT_JOB_ID = 808;

    @Override // android.app.job.JobService
    public boolean onStartJob(JobParameters params) {
        try {
            ActivityManager.getService().performIdleMaintenance();
        } catch (RemoteException e) {
        }
        this.mJobParams = params;
        StorageManagerService ms = StorageManagerService.sSelf;
        if (ms != null) {
            synchronized (this.mFinishCallback) {
                this.mStarted = true;
            }
            ms.runIdleMaint(this.mFinishCallback);
        }
        return ms != null;
    }

    @Override // android.app.job.JobService
    public boolean onStopJob(JobParameters params) {
        StorageManagerService ms = StorageManagerService.sSelf;
        if (ms != null) {
            ms.abortIdleMaint(this.mFinishCallback);
            synchronized (this.mFinishCallback) {
                this.mStarted = false;
            }
        }
        return false;
    }

    public static void scheduleIdlePass(Context context) {
        long nextScheduleTime;
        JobScheduler tm = (JobScheduler) context.getSystemService("jobscheduler");
        long today3AM = offsetFromTodayMidnight(0, 3).getTimeInMillis();
        long today4AM = offsetFromTodayMidnight(0, 4).getTimeInMillis();
        long tomorrow3AM = offsetFromTodayMidnight(1, 3).getTimeInMillis();
        if (System.currentTimeMillis() > today3AM && System.currentTimeMillis() < today4AM) {
            nextScheduleTime = TimeUnit.SECONDS.toMillis(10L);
        } else {
            long nextScheduleTime2 = System.currentTimeMillis();
            nextScheduleTime = tomorrow3AM - nextScheduleTime2;
        }
        JobInfo.Builder builder = new JobInfo.Builder(MOUNT_JOB_ID, sIdleService);
        builder.setRequiresDeviceIdle(true);
        builder.setRequiresBatteryNotLow(true);
        builder.setRequiresCharging(true);
        builder.setMinimumLatency(nextScheduleTime);
        tm.schedule(builder.build());
    }

    private static Calendar offsetFromTodayMidnight(int nDays, int nHours) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(System.currentTimeMillis());
        calendar.set(11, nHours);
        calendar.set(12, 0);
        calendar.set(13, 0);
        calendar.set(14, 0);
        calendar.add(5, nDays);
        return calendar;
    }
}
