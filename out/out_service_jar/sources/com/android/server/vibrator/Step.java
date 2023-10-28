package com.android.server.vibrator;

import android.os.SystemClock;
import com.android.server.job.controllers.JobStatus;
import java.util.List;
/* JADX INFO: Access modifiers changed from: package-private */
/* loaded from: classes2.dex */
public abstract class Step implements Comparable<Step> {
    public final VibrationStepConductor conductor;
    public final long startTime;

    public abstract List<Step> cancel();

    public abstract void cancelImmediately();

    public abstract List<Step> play();

    /* JADX INFO: Access modifiers changed from: package-private */
    public Step(VibrationStepConductor conductor, long startTime) {
        this.conductor = conductor;
        this.startTime = startTime;
    }

    /* JADX INFO: Access modifiers changed from: protected */
    public Vibration getVibration() {
        return this.conductor.getVibration();
    }

    public boolean isCleanUp() {
        return false;
    }

    public long getVibratorOnDuration() {
        return 0L;
    }

    public boolean acceptVibratorCompleteCallback(int vibratorId) {
        return false;
    }

    public long calculateWaitTime() {
        long j = this.startTime;
        if (j == JobStatus.NO_LATEST_RUNTIME) {
            return 0L;
        }
        return Math.max(0L, j - SystemClock.uptimeMillis());
    }

    /* JADX DEBUG: Method merged with bridge method */
    @Override // java.lang.Comparable
    public int compareTo(Step o) {
        return Long.compare(this.startTime, o.startTime);
    }
}
