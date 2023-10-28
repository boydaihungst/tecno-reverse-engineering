package com.android.server.vibrator;

import android.os.SystemClock;
import android.os.Trace;
import java.util.Arrays;
import java.util.List;
/* loaded from: classes2.dex */
final class RampOffVibratorStep extends AbstractVibratorStep {
    private final float mAmplitudeDelta;
    private final float mAmplitudeTarget;

    /* JADX INFO: Access modifiers changed from: package-private */
    public RampOffVibratorStep(VibrationStepConductor conductor, long startTime, float amplitudeTarget, float amplitudeDelta, VibratorController controller, long previousStepVibratorOffTimeout) {
        super(conductor, startTime, controller, null, -1, previousStepVibratorOffTimeout);
        this.mAmplitudeTarget = amplitudeTarget;
        this.mAmplitudeDelta = amplitudeDelta;
    }

    @Override // com.android.server.vibrator.Step
    public boolean isCleanUp() {
        return true;
    }

    @Override // com.android.server.vibrator.AbstractVibratorStep, com.android.server.vibrator.Step
    public List<Step> cancel() {
        return Arrays.asList(new TurnOffVibratorStep(this.conductor, SystemClock.uptimeMillis(), this.controller));
    }

    /* JADX DEBUG: Don't trust debug lines info. Repeating lines: [81=4] */
    @Override // com.android.server.vibrator.Step
    public List<Step> play() {
        Trace.traceBegin(8388608L, "RampOffVibratorStep");
        try {
            if (this.mVibratorCompleteCallbackReceived) {
                stopVibrating();
                return VibrationStepConductor.EMPTY_STEP_LIST;
            }
            changeAmplitude(this.mAmplitudeTarget);
            float newAmplitudeTarget = this.mAmplitudeTarget - this.mAmplitudeDelta;
            return newAmplitudeTarget < 0.001f ? Arrays.asList(new TurnOffVibratorStep(this.conductor, this.previousStepVibratorOffTimeout, this.controller)) : Arrays.asList(new RampOffVibratorStep(this.conductor, this.startTime + this.conductor.vibrationSettings.getRampStepDuration(), newAmplitudeTarget, this.mAmplitudeDelta, this.controller, this.previousStepVibratorOffTimeout));
        } finally {
            Trace.traceEnd(8388608L);
        }
    }
}
