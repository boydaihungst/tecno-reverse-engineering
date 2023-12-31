package com.android.server.vibrator;

import android.os.SystemClock;
import android.os.Trace;
import java.util.Arrays;
import java.util.List;
/* loaded from: classes2.dex */
final class TurnOffVibratorStep extends AbstractVibratorStep {
    /* JADX INFO: Access modifiers changed from: package-private */
    public TurnOffVibratorStep(VibrationStepConductor conductor, long startTime, VibratorController controller) {
        super(conductor, startTime, controller, null, -1, startTime);
    }

    @Override // com.android.server.vibrator.Step
    public boolean isCleanUp() {
        return true;
    }

    @Override // com.android.server.vibrator.AbstractVibratorStep, com.android.server.vibrator.Step
    public List<Step> cancel() {
        return Arrays.asList(new TurnOffVibratorStep(this.conductor, SystemClock.uptimeMillis(), this.controller));
    }

    @Override // com.android.server.vibrator.AbstractVibratorStep, com.android.server.vibrator.Step
    public void cancelImmediately() {
        stopVibrating();
    }

    @Override // com.android.server.vibrator.Step
    public List<Step> play() {
        Trace.traceBegin(8388608L, "TurnOffVibratorStep");
        try {
            stopVibrating();
            return VibrationStepConductor.EMPTY_STEP_LIST;
        } finally {
            Trace.traceEnd(8388608L);
        }
    }
}
