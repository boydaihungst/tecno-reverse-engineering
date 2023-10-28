package com.android.server.pm;

import android.app.job.JobParameters;
import android.app.job.JobService;
/* loaded from: classes2.dex */
public final class BackgroundDexOptJobService extends JobService {
    @Override // android.app.job.JobService
    public boolean onStartJob(JobParameters params) {
        return BackgroundDexOptService.getService().onStartJob(this, params);
    }

    @Override // android.app.job.JobService
    public boolean onStopJob(JobParameters params) {
        return BackgroundDexOptService.getService().onStopJob(this, params);
    }
}
