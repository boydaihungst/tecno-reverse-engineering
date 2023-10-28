package com.android.server.vibrator;

import android.os.SystemClock;
import android.os.Trace;
import java.util.Arrays;
import java.util.List;
/* JADX INFO: Access modifiers changed from: package-private */
/* loaded from: classes2.dex */
public final class CompleteEffectVibratorStep extends AbstractVibratorStep {
    private final boolean mCancelled;

    /* JADX INFO: Access modifiers changed from: package-private */
    public CompleteEffectVibratorStep(VibrationStepConductor conductor, long startTime, boolean cancelled, VibratorController controller, long previousStepVibratorOffTimeout) {
        super(conductor, startTime, controller, null, -1, previousStepVibratorOffTimeout);
        this.mCancelled = cancelled;
    }

    @Override // com.android.server.vibrator.Step
    public boolean isCleanUp() {
        return this.mCancelled;
    }

    @Override // com.android.server.vibrator.AbstractVibratorStep, com.android.server.vibrator.Step
    public List<Step> cancel() {
        if (this.mCancelled) {
            return Arrays.asList(new TurnOffVibratorStep(this.conductor, SystemClock.uptimeMillis(), this.controller));
        }
        return super.cancel();
    }

    /* JADX DEBUG: Don't trust debug lines info. Repeating lines: [111=5] */
    @Override // com.android.server.vibrator.Step
    public List<Step> play() {
        Trace.traceBegin(8388608L, "CompleteEffectVibratorStep");
        try {
            if (this.mVibratorCompleteCallbackReceived) {
                stopVibrating();
                List<Step> list = VibrationStepConductor.EMPTY_STEP_LIST;
                Trace.traceEnd(8388608L);
                return list;
            }
            float currentAmplitude = this.controller.getCurrentAmplitude();
            long remainingOnDuration = (this.previousStepVibratorOffTimeout - 1000) - SystemClock.uptimeMillis();
            long rampDownDuration = Math.min(remainingOnDuration, this.conductor.vibrationSettings.getRampDownDuration());
            long stepDownDuration = this.conductor.vibrationSettings.getRampStepDuration();
            if (currentAmplitude >= 0.001f && rampDownDuration > stepDownDuration) {
                float amplitudeDelta = currentAmplitude / ((float) (rampDownDuration / stepDownDuration));
                float amplitudeTarget = currentAmplitude - amplitudeDelta;
                long newVibratorOffTimeout = this.mCancelled ? rampDownDuration : this.previousStepVibratorOffTimeout;
                return Arrays.asList(new RampOffVibratorStep(this.conductor, this.startTime, amplitudeTarget, amplitudeDelta, this.controller, newVibratorOffTimeout));
            }
            if (this.mCancelled) {
                stopVibrating();
                return VibrationStepConductor.EMPTY_STEP_LIST;
            }
            return Arrays.asList(new TurnOffVibratorStep(this.conductor, this.previousStepVibratorOffTimeout, this.controller));
        } finally {
            Trace.traceEnd(8388608L);
        }
    }
}
