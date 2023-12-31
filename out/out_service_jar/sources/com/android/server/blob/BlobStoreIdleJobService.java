package com.android.server.blob;

import android.app.job.JobInfo;
import android.app.job.JobParameters;
import android.app.job.JobScheduler;
import android.app.job.JobService;
import android.content.ComponentName;
import android.content.Context;
import android.os.AsyncTask;
import android.util.Slog;
import com.android.server.LocalServices;
/* loaded from: classes.dex */
public class BlobStoreIdleJobService extends JobService {
    @Override // android.app.job.JobService
    public boolean onStartJob(final JobParameters params) {
        AsyncTask.execute(new Runnable() { // from class: com.android.server.blob.BlobStoreIdleJobService$$ExternalSyntheticLambda0
            @Override // java.lang.Runnable
            public final void run() {
                BlobStoreIdleJobService.this.m2552x61c5c0ff(params);
            }
        });
        return true;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$onStartJob$0$com-android-server-blob-BlobStoreIdleJobService  reason: not valid java name */
    public /* synthetic */ void m2552x61c5c0ff(JobParameters params) {
        BlobStoreManagerInternal blobStoreManagerInternal = (BlobStoreManagerInternal) LocalServices.getService(BlobStoreManagerInternal.class);
        blobStoreManagerInternal.onIdleMaintenance();
        jobFinished(params, false);
    }

    @Override // android.app.job.JobService
    public boolean onStopJob(JobParameters params) {
        Slog.d(BlobStoreConfig.TAG, "Idle maintenance job is stopped; id=" + params.getJobId() + ", reason=" + JobParameters.getInternalReasonCodeDescription(params.getInternalStopReasonCode()));
        return false;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static void schedule(Context context) {
        JobScheduler jobScheduler = (JobScheduler) context.getSystemService("jobscheduler");
        JobInfo job = new JobInfo.Builder(BlobStoreConfig.IDLE_JOB_ID, new ComponentName(context, BlobStoreIdleJobService.class)).setRequiresDeviceIdle(true).setRequiresCharging(true).setPeriodic(BlobStoreConfig.getIdleJobPeriodMs()).build();
        jobScheduler.schedule(job);
        if (BlobStoreConfig.LOGV) {
            Slog.v(BlobStoreConfig.TAG, "Scheduling the idle maintenance job");
        }
    }
}
