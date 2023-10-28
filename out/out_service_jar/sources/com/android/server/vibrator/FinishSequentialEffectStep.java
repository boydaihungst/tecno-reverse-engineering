package com.android.server.vibrator;

import android.os.Trace;
import com.android.server.job.controllers.JobStatus;
import java.util.Arrays;
import java.util.List;
/* loaded from: classes2.dex */
final class FinishSequentialEffectStep extends Step {
    public final StartSequentialEffectStep startedStep;

    /* JADX INFO: Access modifiers changed from: package-private */
    public FinishSequentialEffectStep(StartSequentialEffectStep startedStep) {
        super(startedStep.conductor, JobStatus.NO_LATEST_RUNTIME);
        this.startedStep = startedStep;
    }

    @Override // com.android.server.vibrator.Step
    public boolean isCleanUp() {
        return true;
    }

    @Override // com.android.server.vibrator.Step
    public List<Step> play() {
        Trace.traceBegin(8388608L, "FinishSequentialEffectStep");
        try {
            this.conductor.vibratorManagerHooks.noteVibratorOff(this.conductor.getVibration().uid);
            Step nextStep = this.startedStep.nextStep();
            return nextStep == null ? VibrationStepConductor.EMPTY_STEP_LIST : Arrays.asList(nextStep);
        } finally {
            Trace.traceEnd(8388608L);
        }
    }

    @Override // com.android.server.vibrator.Step
    public List<Step> cancel() {
        cancelImmediately();
        return VibrationStepConductor.EMPTY_STEP_LIST;
    }

    @Override // com.android.server.vibrator.Step
    public void cancelImmediately() {
        this.conductor.vibratorManagerHooks.noteVibratorOff(this.conductor.getVibration().uid);
    }
}
