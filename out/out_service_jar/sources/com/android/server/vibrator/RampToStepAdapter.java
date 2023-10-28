package com.android.server.vibrator;

import android.hardware.usb.gadget.V1_2.GadgetFunction;
import android.os.VibratorInfo;
import android.os.vibrator.RampSegment;
import android.os.vibrator.StepSegment;
import android.os.vibrator.VibrationEffectSegment;
import android.util.MathUtils;
import com.android.server.vibrator.VibrationEffectAdapters;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
/* loaded from: classes2.dex */
final class RampToStepAdapter implements VibrationEffectAdapters.SegmentsAdapter<VibratorInfo> {
    private final int mStepDuration;

    /* JADX DEBUG: Method arguments types fixed to match base method, original types: [java.util.List, int, java.lang.Object] */
    @Override // com.android.server.vibrator.VibrationEffectAdapters.SegmentsAdapter
    public /* bridge */ /* synthetic */ int apply(List list, int i, VibratorInfo vibratorInfo) {
        return apply2((List<VibrationEffectSegment>) list, i, vibratorInfo);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public RampToStepAdapter(int stepDuration) {
        this.mStepDuration = stepDuration;
    }

    /* renamed from: apply  reason: avoid collision after fix types in other method */
    public int apply2(List<VibrationEffectSegment> segments, int repeatIndex, VibratorInfo info) {
        if (info.hasCapability((long) GadgetFunction.NCM)) {
            return repeatIndex;
        }
        int segmentCount = segments.size();
        int i = 0;
        while (i < segmentCount) {
            VibrationEffectSegment segment = segments.get(i);
            if (segment instanceof RampSegment) {
                List<StepSegment> steps = apply(info, (RampSegment) segment);
                segments.remove(i);
                segments.addAll(i, steps);
                int addedSegments = steps.size() - 1;
                if (repeatIndex > i) {
                    repeatIndex += addedSegments;
                }
                i += addedSegments;
                segmentCount += addedSegments;
            }
            i++;
        }
        return repeatIndex;
    }

    private List<StepSegment> apply(VibratorInfo info, RampSegment ramp) {
        if (Float.compare(ramp.getStartAmplitude(), ramp.getEndAmplitude()) == 0) {
            return Arrays.asList(new StepSegment(ramp.getStartAmplitude(), fillEmptyFrequency(info, ramp.getStartFrequencyHz()), (int) ramp.getDuration()));
        }
        List<StepSegment> steps = new ArrayList<>();
        long duration = ramp.getDuration();
        int i = this.mStepDuration;
        int stepCount = ((int) ((duration + i) - 1)) / i;
        for (int i2 = 0; i2 < stepCount - 1; i2++) {
            float pos = i2 / stepCount;
            float startFrequencyHz = fillEmptyFrequency(info, ramp.getStartFrequencyHz());
            float endFrequencyHz = fillEmptyFrequency(info, ramp.getEndFrequencyHz());
            steps.add(new StepSegment(MathUtils.lerp(ramp.getStartAmplitude(), ramp.getEndAmplitude(), pos), MathUtils.lerp(startFrequencyHz, endFrequencyHz, pos), this.mStepDuration));
        }
        int duration2 = ((int) ramp.getDuration()) - (this.mStepDuration * (stepCount - 1));
        float endFrequencyHz2 = fillEmptyFrequency(info, ramp.getEndFrequencyHz());
        steps.add(new StepSegment(ramp.getEndAmplitude(), endFrequencyHz2, duration2));
        return steps;
    }

    private static float fillEmptyFrequency(VibratorInfo info, float frequencyHz) {
        return frequencyHz == 0.0f ? info.getResonantFrequencyHz() : frequencyHz;
    }
}
