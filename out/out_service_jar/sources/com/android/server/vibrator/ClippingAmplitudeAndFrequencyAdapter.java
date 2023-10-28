package com.android.server.vibrator;

import android.os.VibratorInfo;
import android.os.vibrator.RampSegment;
import android.os.vibrator.StepSegment;
import android.os.vibrator.VibrationEffectSegment;
import android.util.MathUtils;
import android.util.Range;
import com.android.server.vibrator.VibrationEffectAdapters;
import java.util.List;
/* loaded from: classes2.dex */
final class ClippingAmplitudeAndFrequencyAdapter implements VibrationEffectAdapters.SegmentsAdapter<VibratorInfo> {
    /* JADX DEBUG: Method arguments types fixed to match base method, original types: [java.util.List, int, java.lang.Object] */
    @Override // com.android.server.vibrator.VibrationEffectAdapters.SegmentsAdapter
    public /* bridge */ /* synthetic */ int apply(List list, int i, VibratorInfo vibratorInfo) {
        return apply2((List<VibrationEffectSegment>) list, i, vibratorInfo);
    }

    /* renamed from: apply  reason: avoid collision after fix types in other method */
    public int apply2(List<VibrationEffectSegment> segments, int repeatIndex, VibratorInfo info) {
        int segmentCount = segments.size();
        for (int i = 0; i < segmentCount; i++) {
            VibrationEffectSegment segment = segments.get(i);
            if (segment instanceof StepSegment) {
                segments.set(i, apply((StepSegment) segment, info));
            } else if (segment instanceof RampSegment) {
                segments.set(i, apply((RampSegment) segment, info));
            }
        }
        return repeatIndex;
    }

    private StepSegment apply(StepSegment segment, VibratorInfo info) {
        float clampedFrequency = clampFrequency(info, segment.getFrequencyHz());
        return new StepSegment(clampAmplitude(info, clampedFrequency, segment.getAmplitude()), clampedFrequency, (int) segment.getDuration());
    }

    private RampSegment apply(RampSegment segment, VibratorInfo info) {
        float clampedStartFrequency = clampFrequency(info, segment.getStartFrequencyHz());
        float clampedEndFrequency = clampFrequency(info, segment.getEndFrequencyHz());
        return new RampSegment(clampAmplitude(info, clampedStartFrequency, segment.getStartAmplitude()), clampAmplitude(info, clampedEndFrequency, segment.getEndAmplitude()), clampedStartFrequency, clampedEndFrequency, (int) segment.getDuration());
    }

    private float clampFrequency(VibratorInfo info, float frequencyHz) {
        Range<Float> frequencyRangeHz = info.getFrequencyProfile().getFrequencyRangeHz();
        if (frequencyHz == 0.0f || frequencyRangeHz == null) {
            return info.getResonantFrequencyHz();
        }
        return frequencyRangeHz.clamp(Float.valueOf(frequencyHz)).floatValue();
    }

    private float clampAmplitude(VibratorInfo info, float frequencyHz, float amplitude) {
        VibratorInfo.FrequencyProfile mapping = info.getFrequencyProfile();
        if (mapping.isEmpty()) {
            return amplitude;
        }
        return MathUtils.min(amplitude, mapping.getMaxAmplitude(frequencyHz));
    }
}
