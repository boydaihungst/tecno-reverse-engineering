package com.android.server.timezone;

import android.app.job.JobInfo;
import android.app.job.JobParameters;
import android.app.job.JobScheduler;
import android.app.job.JobService;
import android.content.ComponentName;
import android.content.Context;
import android.util.Slog;
import com.android.server.LocalServices;
/* loaded from: classes2.dex */
public final class TimeZoneUpdateIdler extends JobService {
    private static final String TAG = "timezone.TimeZoneUpdateIdler";
    private static final int TIME_ZONE_UPDATE_IDLE_JOB_ID = 27042305;

    @Override // android.app.job.JobService
    public boolean onStartJob(JobParameters params) {
        RulesManagerService rulesManagerService = (RulesManagerService) LocalServices.getService(RulesManagerService.class);
        Slog.d(TAG, "onStartJob() called");
        rulesManagerService.notifyIdle();
        return false;
    }

    @Override // android.app.job.JobService
    public boolean onStopJob(JobParameters params) {
        boolean reschedule = params.getStopReason() != 1;
        Slog.d(TAG, "onStopJob() called: Reschedule=" + reschedule);
        return reschedule;
    }

    public static void schedule(Context context, long minimumDelayMillis) {
        JobScheduler jobScheduler = (JobScheduler) context.getSystemService("jobscheduler");
        ComponentName idlerJobServiceName = new ComponentName(context, TimeZoneUpdateIdler.class);
        JobInfo.Builder jobInfoBuilder = new JobInfo.Builder(TIME_ZONE_UPDATE_IDLE_JOB_ID, idlerJobServiceName).setRequiresDeviceIdle(true).setRequiresCharging(true).setMinimumLatency(minimumDelayMillis);
        Slog.d(TAG, "schedule() called: minimumDelayMillis=" + minimumDelayMillis);
        jobScheduler.schedule(jobInfoBuilder.build());
    }

    public static void unschedule(Context context) {
        JobScheduler jobScheduler = (JobScheduler) context.getSystemService("jobscheduler");
        Slog.d(TAG, "unschedule() called");
        jobScheduler.cancel(TIME_ZONE_UPDATE_IDLE_JOB_ID);
    }
}
