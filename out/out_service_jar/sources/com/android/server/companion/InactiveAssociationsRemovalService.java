package com.android.server.companion;

import android.app.job.JobInfo;
import android.app.job.JobParameters;
import android.app.job.JobScheduler;
import android.app.job.JobService;
import android.content.ComponentName;
import android.content.Context;
import android.util.Slog;
import com.android.server.LocalServices;
import java.util.concurrent.TimeUnit;
/* loaded from: classes.dex */
public class InactiveAssociationsRemovalService extends JobService {
    private static final int JOB_ID = InactiveAssociationsRemovalService.class.hashCode();
    private static final long ONE_DAY_INTERVAL = TimeUnit.DAYS.toMillis(1);

    @Override // android.app.job.JobService
    public boolean onStartJob(JobParameters params) {
        Slog.i("CompanionDeviceManagerService", "Execute the Association Removal job");
        ((CompanionDeviceManagerServiceInternal) LocalServices.getService(CompanionDeviceManagerServiceInternal.class)).removeInactiveSelfManagedAssociations();
        jobFinished(params, false);
        return true;
    }

    @Override // android.app.job.JobService
    public boolean onStopJob(JobParameters params) {
        Slog.i("CompanionDeviceManagerService", "Association removal job stopped; id=" + params.getJobId() + ", reason=" + JobParameters.getInternalReasonCodeDescription(params.getInternalStopReasonCode()));
        return false;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static void schedule(Context context) {
        Slog.i("CompanionDeviceManagerService", "Scheduling the Association Removal job");
        JobScheduler jobScheduler = (JobScheduler) context.getSystemService(JobScheduler.class);
        JobInfo job = new JobInfo.Builder(JOB_ID, new ComponentName(context, InactiveAssociationsRemovalService.class)).setRequiresCharging(true).setRequiresDeviceIdle(true).setPeriodic(ONE_DAY_INTERVAL).build();
        jobScheduler.schedule(job);
    }
}
