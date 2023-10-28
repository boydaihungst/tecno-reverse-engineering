package com.android.server.vibrator;

import android.os.VibrationEffect;
import android.os.VibratorInfo;
import com.android.server.vibrator.VibrationEffectAdapters;
import java.util.Arrays;
import java.util.List;
/* loaded from: classes2.dex */
final class DeviceVibrationEffectAdapter implements VibrationEffectAdapters.EffectAdapter<VibratorInfo> {
    private final List<VibrationEffectAdapters.SegmentsAdapter<VibratorInfo>> mSegmentAdapters;

    /* JADX INFO: Access modifiers changed from: package-private */
    public DeviceVibrationEffectAdapter(VibrationSettings settings) {
        this.mSegmentAdapters = Arrays.asList(new RampToStepAdapter(settings.getRampStepDuration()), new StepToRampAdapter(), new RampDownAdapter(settings.getRampDownDuration(), settings.getRampStepDuration()), new ClippingAmplitudeAndFrequencyAdapter());
    }

    /* JADX DEBUG: Method merged with bridge method */
    @Override // com.android.server.vibrator.VibrationEffectAdapters.EffectAdapter
    public VibrationEffect apply(VibrationEffect effect, VibratorInfo info) {
        return VibrationEffectAdapters.apply(effect, this.mSegmentAdapters, info);
    }
}
