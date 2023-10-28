package com.android.server.vibrator;

import android.hardware.usb.gadget.V1_2.GadgetFunction;
import android.os.VibratorInfo;
import android.os.vibrator.RampSegment;
import android.os.vibrator.StepSegment;
import android.os.vibrator.VibrationEffectSegment;
import android.util.MathUtils;
import com.android.server.vibrator.VibrationEffectAdapters;
import java.util.ArrayList;
import java.util.List;
/* loaded from: classes2.dex */
final class StepToRampAdapter implements VibrationEffectAdapters.SegmentsAdapter<VibratorInfo> {
    /* JADX DEBUG: Method arguments types fixed to match base method, original types: [java.util.List, int, java.lang.Object] */
    @Override // com.android.server.vibrator.VibrationEffectAdapters.SegmentsAdapter
    public /* bridge */ /* synthetic */ int apply(List list, int i, VibratorInfo vibratorInfo) {
        return apply2((List<VibrationEffectSegment>) list, i, vibratorInfo);
    }

    /* renamed from: apply  reason: avoid collision after fix types in other method */
    public int apply2(List<VibrationEffectSegment> segments, int repeatIndex, VibratorInfo info) {
        if (!info.hasCapability((long) GadgetFunction.NCM)) {
            return repeatIndex;
        }
        convertStepsToRamps(info, segments);
        return splitLongRampSegments(info, segments, repeatIndex);
    }

    private void convertStepsToRamps(VibratorInfo info, List<VibrationEffectSegment> segments) {
        int segmentCount = segments.size();
        for (int i = 0; i < segmentCount; i++) {
            StepSegment stepSegment = (VibrationEffectSegment) segments.get(i);
            if (isStep(stepSegment) && stepSegment.getFrequencyHz() != 0.0f) {
                segments.set(i, convertStepToRamp(info, stepSegment));
            }
        }
        for (int i2 = 0; i2 < segmentCount; i2++) {
            if (segments.get(i2) instanceof RampSegment) {
                for (int j = i2 - 1; j >= 0 && isStep(segments.get(j)); j--) {
                    segments.set(j, convertStepToRamp(info, segments.get(j)));
                }
                for (int j2 = i2 + 1; j2 < segmentCount && isStep(segments.get(j2)); j2++) {
                    segments.set(j2, convertStepToRamp(info, segments.get(j2)));
                }
            }
        }
    }

    private int splitLongRampSegments(VibratorInfo info, List<VibrationEffectSegment> segments, int repeatIndex) {
        int maxDuration = info.getPwlePrimitiveDurationMax();
        if (maxDuration <= 0) {
            return repeatIndex;
        }
        int segmentCount = segments.size();
        int i = 0;
        while (i < segmentCount) {
            if (segments.get(i) instanceof RampSegment) {
                RampSegment ramp = segments.get(i);
                int splits = ((((int) ramp.getDuration()) + maxDuration) - 1) / maxDuration;
                if (splits > 1) {
                    segments.remove(i);
                    segments.addAll(i, splitRampSegment(info, ramp, splits));
                    int addedSegments = splits - 1;
                    if (repeatIndex > i) {
                        repeatIndex += addedSegments;
                    }
                    i += addedSegments;
                    segmentCount += addedSegments;
                }
            }
            i++;
        }
        return repeatIndex;
    }

    private static RampSegment convertStepToRamp(VibratorInfo info, StepSegment segment) {
        float frequencyHz = fillEmptyFrequency(info, segment.getFrequencyHz());
        return new RampSegment(segment.getAmplitude(), segment.getAmplitude(), frequencyHz, frequencyHz, (int) segment.getDuration());
    }

    private static List<RampSegment> splitRampSegment(VibratorInfo info, RampSegment ramp, int splits) {
        List<RampSegment> ramps = new ArrayList<>(splits);
        float startFrequencyHz = fillEmptyFrequency(info, ramp.getStartFrequencyHz());
        float endFrequencyHz = fillEmptyFrequency(info, ramp.getEndFrequencyHz());
        long splitDuration = ramp.getDuration() / splits;
        float previousAmplitude = ramp.getStartAmplitude();
        int i = 1;
        float previousAmplitude2 = previousAmplitude;
        float previousFrequency = startFrequencyHz;
        long accumulatedDuration = 0;
        while (i < splits) {
            long accumulatedDuration2 = accumulatedDuration + splitDuration;
            float durationRatio = ((float) accumulatedDuration2) / ((float) ramp.getDuration());
            float interpolatedFrequency = MathUtils.lerp(startFrequencyHz, endFrequencyHz, durationRatio);
            float interpolatedAmplitude = MathUtils.lerp(ramp.getStartAmplitude(), ramp.getEndAmplitude(), durationRatio);
            RampSegment rampSplit = new RampSegment(previousAmplitude2, interpolatedAmplitude, previousFrequency, interpolatedFrequency, (int) splitDuration);
            ramps.add(rampSplit);
            previousAmplitude2 = rampSplit.getEndAmplitude();
            previousFrequency = rampSplit.getEndFrequencyHz();
            i++;
            accumulatedDuration = accumulatedDuration2;
        }
        ramps.add(new RampSegment(previousAmplitude2, ramp.getEndAmplitude(), previousFrequency, endFrequencyHz, (int) (ramp.getDuration() - accumulatedDuration)));
        return ramps;
    }

    private static boolean isStep(VibrationEffectSegment segment) {
        return segment instanceof StepSegment;
    }

    private static float fillEmptyFrequency(VibratorInfo info, float frequencyHz) {
        return frequencyHz == 0.0f ? info.getResonantFrequencyHz() : frequencyHz;
    }
}
