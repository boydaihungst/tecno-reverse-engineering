package com.android.server.notification;

import android.app.job.JobInfo;
import android.app.job.JobParameters;
import android.app.job.JobScheduler;
import android.app.job.JobService;
import android.content.ComponentName;
import android.content.Context;
import com.android.server.LocalServices;
/* loaded from: classes2.dex */
public class ReviewNotificationPermissionsJobService extends JobService {
    protected static final int JOB_ID = 225373531;
    public static final String TAG = "ReviewNotificationPermissionsJobService";

    public static void scheduleJob(Context context, long rescheduleTimeMillis) {
        JobScheduler jobScheduler = (JobScheduler) context.getSystemService(JobScheduler.class);
        if (jobScheduler.getPendingJob(JOB_ID) != null) {
            jobScheduler.cancel(JOB_ID);
        }
        ComponentName component = new ComponentName(context, ReviewNotificationPermissionsJobService.class);
        JobInfo newJob = new JobInfo.Builder(JOB_ID, component).setPersisted(true).setMinimumLatency(rescheduleTimeMillis).build();
        jobScheduler.schedule(newJob);
    }

    @Override // android.app.job.JobService
    public boolean onStartJob(JobParameters params) {
        NotificationManagerInternal nmi = (NotificationManagerInternal) LocalServices.getService(NotificationManagerInternal.class);
        nmi.sendReviewPermissionsNotification();
        return false;
    }

    @Override // android.app.job.JobService
    public boolean onStopJob(JobParameters params) {
        return true;
    }
}
