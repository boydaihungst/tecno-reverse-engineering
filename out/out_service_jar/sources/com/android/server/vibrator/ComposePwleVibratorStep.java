package com.android.server.vibrator;

import android.os.Trace;
import android.os.VibrationEffect;
import android.os.vibrator.RampSegment;
import android.os.vibrator.VibrationEffectSegment;
import android.util.Slog;
import java.util.ArrayList;
import java.util.List;
/* JADX INFO: Access modifiers changed from: package-private */
/* loaded from: classes2.dex */
public final class ComposePwleVibratorStep extends AbstractVibratorStep {
    private static final int DEFAULT_PWLE_SIZE_LIMIT = 100;

    /* JADX INFO: Access modifiers changed from: package-private */
    public ComposePwleVibratorStep(VibrationStepConductor conductor, long startTime, VibratorController controller, VibrationEffect.Composed effect, int index, long previousStepVibratorOffTimeout) {
        super(conductor, Math.max(startTime, previousStepVibratorOffTimeout), controller, effect, index, previousStepVibratorOffTimeout);
    }

    @Override // com.android.server.vibrator.Step
    public List<Step> play() {
        Trace.traceBegin(8388608L, "ComposePwleStep");
        try {
            int limit = this.controller.getVibratorInfo().getPwleSizeMax();
            List<RampSegment> pwles = unrollRampSegments(this.effect, this.segmentIndex, limit > 0 ? limit : 100);
            if (pwles.isEmpty()) {
                Slog.w("VibrationThread", "Ignoring wrong segment for a ComposePwleStep: " + this.effect.getSegments().get(this.segmentIndex));
                return skipToNextSteps(1);
            }
            this.mVibratorOnResult = this.controller.on((RampSegment[]) pwles.toArray(new RampSegment[pwles.size()]), getVibration().id);
            return nextSteps(pwles.size());
        } finally {
            Trace.traceEnd(8388608L);
        }
    }

    private List<RampSegment> unrollRampSegments(VibrationEffect.Composed effect, int startIndex, int limit) {
        List<RampSegment> segments = new ArrayList<>(limit);
        float bestBreakAmplitude = 1.0f;
        int bestBreakPosition = limit;
        int segmentCount = effect.getSegments().size();
        int repeatIndex = effect.getRepeatIndex();
        int i = startIndex;
        while (segments.size() <= limit) {
            if (i == segmentCount) {
                if (repeatIndex < 0) {
                    break;
                }
                i = repeatIndex;
            }
            VibrationEffectSegment segment = (VibrationEffectSegment) effect.getSegments().get(i);
            if (!(segment instanceof RampSegment)) {
                break;
            }
            RampSegment rampSegment = (RampSegment) segment;
            segments.add(rampSegment);
            if (isBetterBreakPosition(segments, bestBreakAmplitude, limit)) {
                bestBreakAmplitude = rampSegment.getEndAmplitude();
                bestBreakPosition = segments.size();
            }
            i++;
        }
        int i2 = segments.size();
        if (i2 > limit) {
            return segments.subList(0, bestBreakPosition);
        }
        return segments;
    }

    private boolean isBetterBreakPosition(List<RampSegment> segments, float currentBestBreakAmplitude, int limit) {
        RampSegment lastSegment = segments.get(segments.size() - 1);
        float breakAmplitudeCandidate = lastSegment.getEndAmplitude();
        int breakPositionCandidate = segments.size();
        if (breakPositionCandidate > limit) {
            return false;
        }
        if (breakAmplitudeCandidate == 0.0f) {
            return true;
        }
        return breakPositionCandidate >= limit / 2 && breakAmplitudeCandidate <= currentBestBreakAmplitude;
    }
}
