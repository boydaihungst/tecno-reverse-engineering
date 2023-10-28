package com.android.server.job.restrictions;

import android.util.IndentingPrintWriter;
import android.util.proto.ProtoOutputStream;
import com.android.server.job.JobSchedulerService;
import com.android.server.job.controllers.JobStatus;
/* loaded from: classes.dex */
public abstract class JobRestriction {
    private final int mInternalReason;
    private final int mReason;
    final JobSchedulerService mService;

    public abstract void dumpConstants(IndentingPrintWriter indentingPrintWriter);

    public abstract boolean isJobRestricted(JobStatus jobStatus);

    /* JADX INFO: Access modifiers changed from: package-private */
    public JobRestriction(JobSchedulerService service, int reason, int internalReason) {
        this.mService = service;
        this.mReason = reason;
        this.mInternalReason = internalReason;
    }

    public void onSystemServicesReady() {
    }

    public void dumpConstants(ProtoOutputStream proto) {
    }

    public final int getReason() {
        return this.mReason;
    }

    public final int getInternalReason() {
        return this.mInternalReason;
    }
}
