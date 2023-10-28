package com.android.server.vibrator;

import android.os.VibratorInfo;
import android.os.vibrator.RampSegment;
import android.os.vibrator.StepSegment;
import android.os.vibrator.VibrationEffectSegment;
import com.android.server.vibrator.VibrationEffectAdapters;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
/* loaded from: classes2.dex */
final class RampDownAdapter implements VibrationEffectAdapters.SegmentsAdapter<VibratorInfo> {
    private final int mRampDownDuration;
    private final int mStepDuration;

    /* JADX DEBUG: Method arguments types fixed to match base method, original types: [java.util.List, int, java.lang.Object] */
    @Override // com.android.server.vibrator.VibrationEffectAdapters.SegmentsAdapter
    public /* bridge */ /* synthetic */ int apply(List list, int i, VibratorInfo vibratorInfo) {
        return apply2((List<VibrationEffectSegment>) list, i, vibratorInfo);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public RampDownAdapter(int rampDownDuration, int stepDuration) {
        this.mRampDownDuration = rampDownDuration;
        this.mStepDuration = stepDuration;
    }

    /* renamed from: apply  reason: avoid collision after fix types in other method */
    public int apply2(List<VibrationEffectSegment> segments, int repeatIndex, VibratorInfo info) {
        if (this.mRampDownDuration <= 0) {
            return repeatIndex;
        }
        return addRampDownToLoop(segments, addRampDownToZeroAmplitudeSegments(segments, repeatIndex));
    }

    private int addRampDownToZeroAmplitudeSegments(List<VibrationEffectSegment> segments, int repeatIndex) {
        int i;
        int segmentCount = segments.size();
        int i2 = 1;
        int segmentCount2 = segmentCount;
        int repeatIndex2 = repeatIndex;
        while (i2 < segmentCount2) {
            StepSegment stepSegment = (VibrationEffectSegment) segments.get(i2 - 1);
            if (isOffSegment(segments.get(i2)) && endsWithNonZeroAmplitude(stepSegment)) {
                List<VibrationEffectSegment> replacementSegments = null;
                long offDuration = segments.get(i2).getDuration();
                if (stepSegment instanceof StepSegment) {
                    replacementSegments = createStepsDown(stepSegment.getAmplitude(), stepSegment.getFrequencyHz(), offDuration);
                    i = 1;
                } else if (stepSegment instanceof RampSegment) {
                    float previousAmplitude = ((RampSegment) stepSegment).getEndAmplitude();
                    float previousFrequency = ((RampSegment) stepSegment).getEndFrequencyHz();
                    int i3 = this.mRampDownDuration;
                    if (offDuration <= i3) {
                        replacementSegments = Arrays.asList(createRampDown(previousAmplitude, previousFrequency, offDuration));
                        i = 1;
                    } else {
                        RampSegment createRampDown = createRampDown(0.0f, previousFrequency, offDuration - this.mRampDownDuration);
                        i = 1;
                        replacementSegments = Arrays.asList(createRampDown(previousAmplitude, previousFrequency, i3), createRampDown);
                    }
                } else {
                    i = 1;
                }
                if (replacementSegments != null) {
                    int segmentsAdded = replacementSegments.size() - i;
                    VibrationEffectSegment originalOffSegment = segments.remove(i2);
                    segments.addAll(i2, replacementSegments);
                    if (repeatIndex2 >= i2) {
                        if (repeatIndex2 == i2) {
                            segments.add(originalOffSegment);
                            repeatIndex2++;
                            segmentCount2++;
                        }
                        repeatIndex2 += segmentsAdded;
                    }
                    i2 += segmentsAdded;
                    segmentCount2 += segmentsAdded;
                }
            }
            i2++;
        }
        return repeatIndex2;
    }

    private int addRampDownToLoop(List<VibrationEffectSegment> segments, int repeatIndex) {
        if (repeatIndex < 0) {
            return repeatIndex;
        }
        int segmentCount = segments.size();
        if (!endsWithNonZeroAmplitude(segments.get(segmentCount - 1)) || !isOffSegment(segments.get(repeatIndex))) {
            return repeatIndex;
        }
        StepSegment stepSegment = (VibrationEffectSegment) segments.get(segmentCount - 1);
        VibrationEffectSegment offSegment = segments.get(repeatIndex);
        long offDuration = offSegment.getDuration();
        int i = this.mRampDownDuration;
        if (offDuration > i) {
            segments.set(repeatIndex, updateDuration(offSegment, offDuration - i));
            segments.add(repeatIndex, updateDuration(offSegment, this.mRampDownDuration));
        }
        int repeatIndex2 = repeatIndex + 1;
        if (stepSegment instanceof StepSegment) {
            float previousAmplitude = stepSegment.getAmplitude();
            float previousFrequency = stepSegment.getFrequencyHz();
            segments.addAll(createStepsDown(previousAmplitude, previousFrequency, Math.min(offDuration, this.mRampDownDuration)));
        } else if (stepSegment instanceof RampSegment) {
            float previousAmplitude2 = ((RampSegment) stepSegment).getEndAmplitude();
            float previousFrequency2 = ((RampSegment) stepSegment).getEndFrequencyHz();
            segments.add(createRampDown(previousAmplitude2, previousFrequency2, Math.min(offDuration, this.mRampDownDuration)));
        }
        return repeatIndex2;
    }

    private List<VibrationEffectSegment> createStepsDown(float amplitude, float frequency, long duration) {
        int stepCount = ((int) Math.min(duration, this.mRampDownDuration)) / this.mStepDuration;
        float amplitudeStep = amplitude / stepCount;
        List<VibrationEffectSegment> steps = new ArrayList<>();
        for (int i = 1; i < stepCount; i++) {
            steps.add(new StepSegment(amplitude - (i * amplitudeStep), frequency, this.mStepDuration));
        }
        int i2 = (int) duration;
        int remainingDuration = i2 - (this.mStepDuration * (stepCount - 1));
        steps.add(new StepSegment(0.0f, frequency, remainingDuration));
        return steps;
    }

    private static RampSegment createRampDown(float amplitude, float frequency, long duration) {
        return new RampSegment(amplitude, 0.0f, frequency, frequency, (int) duration);
    }

    private static VibrationEffectSegment updateDuration(VibrationEffectSegment segment, long newDuration) {
        if (segment instanceof RampSegment) {
            RampSegment ramp = (RampSegment) segment;
            return new RampSegment(ramp.getStartAmplitude(), ramp.getEndAmplitude(), ramp.getStartFrequencyHz(), ramp.getEndFrequencyHz(), (int) newDuration);
        } else if (segment instanceof StepSegment) {
            StepSegment step = (StepSegment) segment;
            return new StepSegment(step.getAmplitude(), step.getFrequencyHz(), (int) newDuration);
        } else {
            return segment;
        }
    }

    private static boolean isOffSegment(VibrationEffectSegment segment) {
        if (segment instanceof StepSegment) {
            return ((StepSegment) segment).getAmplitude() == 0.0f;
        } else if (segment instanceof RampSegment) {
            RampSegment ramp = (RampSegment) segment;
            return ramp.getStartAmplitude() == 0.0f && ramp.getEndAmplitude() == 0.0f;
        } else {
            return false;
        }
    }

    private static boolean endsWithNonZeroAmplitude(VibrationEffectSegment segment) {
        return segment instanceof StepSegment ? ((StepSegment) segment).getAmplitude() != 0.0f : (segment instanceof RampSegment) && ((RampSegment) segment).getEndAmplitude() != 0.0f;
    }
}
