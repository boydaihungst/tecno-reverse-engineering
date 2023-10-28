package com.android.server.job.restrictions;

import android.os.PowerManager;
import android.util.IndentingPrintWriter;
import com.android.server.job.JobSchedulerService;
import com.android.server.job.controllers.JobStatus;
/* loaded from: classes.dex */
public class ThermalStatusRestriction extends JobRestriction {
    private static final int HIGHER_PRIORITY_THRESHOLD = 2;
    private static final int LOWER_THRESHOLD = 1;
    private static final int LOW_PRIORITY_THRESHOLD = 1;
    private static final String TAG = "ThermalStatusRestriction";
    private static final int UPPER_THRESHOLD = 3;
    private volatile int mThermalStatus;

    public ThermalStatusRestriction(JobSchedulerService service) {
        super(service, 4, 5);
        this.mThermalStatus = 0;
    }

    @Override // com.android.server.job.restrictions.JobRestriction
    public void onSystemServicesReady() {
        PowerManager powerManager = (PowerManager) this.mService.getTestableContext().getSystemService(PowerManager.class);
        powerManager.addThermalStatusListener(new PowerManager.OnThermalStatusChangedListener() { // from class: com.android.server.job.restrictions.ThermalStatusRestriction.1
            @Override // android.os.PowerManager.OnThermalStatusChangedListener
            public void onThermalStatusChanged(int status) {
                boolean z = true;
                if ((status < 1 || status > 3) && ((ThermalStatusRestriction.this.mThermalStatus < 1 || status >= 1) && (ThermalStatusRestriction.this.mThermalStatus >= 3 || status <= 3))) {
                    z = false;
                }
                boolean significantChange = z;
                ThermalStatusRestriction.this.mThermalStatus = status;
                if (significantChange) {
                    ThermalStatusRestriction.this.mService.onControllerStateChanged(null);
                }
            }
        });
    }

    @Override // com.android.server.job.restrictions.JobRestriction
    public boolean isJobRestricted(JobStatus job) {
        if (this.mThermalStatus >= 3) {
            return true;
        }
        int priority = job.getEffectivePriority();
        if (this.mThermalStatus >= 2) {
            return (job.shouldTreatAsExpeditedJob() || (priority == 400 && this.mService.isCurrentlyRunningLocked(job))) ? false : true;
        } else if (this.mThermalStatus >= 1) {
            return (priority == 200 && !this.mService.isCurrentlyRunningLocked(job)) || priority == 100;
        } else {
            return false;
        }
    }

    int getThermalStatus() {
        return this.mThermalStatus;
    }

    @Override // com.android.server.job.restrictions.JobRestriction
    public void dumpConstants(IndentingPrintWriter pw) {
        pw.print("Thermal status: ");
        pw.println(this.mThermalStatus);
    }
}
