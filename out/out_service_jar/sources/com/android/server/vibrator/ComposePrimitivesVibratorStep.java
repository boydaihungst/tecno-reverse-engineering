package com.android.server.vibrator;

import android.os.Trace;
import android.os.VibrationEffect;
import android.os.vibrator.PrimitiveSegment;
import android.os.vibrator.VibrationEffectSegment;
import android.util.Slog;
import java.util.ArrayList;
import java.util.List;
/* JADX INFO: Access modifiers changed from: package-private */
/* loaded from: classes2.dex */
public final class ComposePrimitivesVibratorStep extends AbstractVibratorStep {
    private static final int DEFAULT_COMPOSITION_SIZE_LIMIT = 100;

    /* JADX INFO: Access modifiers changed from: package-private */
    public ComposePrimitivesVibratorStep(VibrationStepConductor conductor, long startTime, VibratorController controller, VibrationEffect.Composed effect, int index, long previousStepVibratorOffTimeout) {
        super(conductor, Math.max(startTime, previousStepVibratorOffTimeout), controller, effect, index, previousStepVibratorOffTimeout);
    }

    @Override // com.android.server.vibrator.Step
    public List<Step> play() {
        Trace.traceBegin(8388608L, "ComposePrimitivesStep");
        try {
            int limit = this.controller.getVibratorInfo().getCompositionSizeMax();
            List<PrimitiveSegment> primitives = unrollPrimitiveSegments(this.effect, this.segmentIndex, limit > 0 ? limit : 100);
            if (primitives.isEmpty()) {
                Slog.w("VibrationThread", "Ignoring wrong segment for a ComposePrimitivesStep: " + this.effect.getSegments().get(this.segmentIndex));
                return skipToNextSteps(1);
            }
            this.mVibratorOnResult = this.controller.on((PrimitiveSegment[]) primitives.toArray(new PrimitiveSegment[primitives.size()]), getVibration().id);
            return nextSteps(primitives.size());
        } finally {
            Trace.traceEnd(8388608L);
        }
    }

    private List<PrimitiveSegment> unrollPrimitiveSegments(VibrationEffect.Composed effect, int startIndex, int limit) {
        List<PrimitiveSegment> segments = new ArrayList<>(limit);
        int segmentCount = effect.getSegments().size();
        int repeatIndex = effect.getRepeatIndex();
        int i = startIndex;
        while (segments.size() < limit) {
            if (i == segmentCount) {
                if (repeatIndex < 0) {
                    break;
                }
                i = repeatIndex;
            }
            PrimitiveSegment primitiveSegment = (VibrationEffectSegment) effect.getSegments().get(i);
            if (!(primitiveSegment instanceof PrimitiveSegment)) {
                break;
            }
            segments.add(primitiveSegment);
            i++;
        }
        return segments;
    }
}
