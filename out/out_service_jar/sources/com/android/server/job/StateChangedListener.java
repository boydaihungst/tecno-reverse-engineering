package com.android.server.job;

import android.util.ArraySet;
import com.android.server.job.controllers.JobStatus;
import java.util.List;
/* loaded from: classes.dex */
public interface StateChangedListener {
    void onControllerStateChanged(ArraySet<JobStatus> arraySet);

    void onDeviceIdleStateChanged(boolean z);

    void onRestrictedBucketChanged(List<JobStatus> list);

    void onRunJobNow(JobStatus jobStatus);
}
