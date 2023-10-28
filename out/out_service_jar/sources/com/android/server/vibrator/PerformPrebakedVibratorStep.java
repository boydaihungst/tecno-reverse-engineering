package com.android.server.vibrator;

import android.os.Trace;
import android.os.VibrationEffect;
import android.os.vibrator.PrebakedSegment;
import android.os.vibrator.VibrationEffectSegment;
import android.util.Slog;
import java.util.ArrayList;
import java.util.List;
/* JADX INFO: Access modifiers changed from: package-private */
/* loaded from: classes2.dex */
public final class PerformPrebakedVibratorStep extends AbstractVibratorStep {
    /* JADX INFO: Access modifiers changed from: package-private */
    public PerformPrebakedVibratorStep(VibrationStepConductor conductor, long startTime, VibratorController controller, VibrationEffect.Composed effect, int index, long previousStepVibratorOffTimeout) {
        super(conductor, Math.max(startTime, previousStepVibratorOffTimeout), controller, effect, index, previousStepVibratorOffTimeout);
    }

    /* JADX DEBUG: Don't trust debug lines info. Repeating lines: [84=4] */
    @Override // com.android.server.vibrator.Step
    public List<Step> play() {
        Trace.traceBegin(8388608L, "PerformPrebakedVibratorStep");
        try {
            PrebakedSegment prebakedSegment = (VibrationEffectSegment) this.effect.getSegments().get(this.segmentIndex);
            if (!(prebakedSegment instanceof PrebakedSegment)) {
                Slog.w("VibrationThread", "Ignoring wrong segment for a PerformPrebakedVibratorStep: " + prebakedSegment);
                return skipToNextSteps(1);
            }
            PrebakedSegment prebaked = prebakedSegment;
            VibrationEffect fallback = getVibration().getFallback(prebaked.getEffectId());
            this.mVibratorOnResult = this.controller.on(prebaked, getVibration().id);
            if (this.mVibratorOnResult == 0 && prebaked.shouldFallback() && (fallback instanceof VibrationEffect.Composed)) {
                AbstractVibratorStep fallbackStep = this.conductor.nextVibrateStep(this.startTime, this.controller, replaceCurrentSegment((VibrationEffect.Composed) fallback), this.segmentIndex, this.previousStepVibratorOffTimeout);
                List<Step> fallbackResult = fallbackStep.play();
                this.mVibratorOnResult = fallbackStep.getVibratorOnDuration();
                return fallbackResult;
            }
            return nextSteps(1);
        } finally {
            Trace.traceEnd(8388608L);
        }
    }

    private VibrationEffect.Composed replaceCurrentSegment(VibrationEffect.Composed fallback) {
        List<VibrationEffectSegment> newSegments = new ArrayList<>(this.effect.getSegments());
        int newRepeatIndex = this.effect.getRepeatIndex();
        newSegments.remove(this.segmentIndex);
        newSegments.addAll(this.segmentIndex, fallback.getSegments());
        if (this.segmentIndex < this.effect.getRepeatIndex()) {
            newRepeatIndex += fallback.getSegments().size() - 1;
        }
        return new VibrationEffect.Composed(newSegments, newRepeatIndex);
    }
}
