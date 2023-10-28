package com.android.server.vibrator;

import android.os.VibrationEffect;
import android.os.vibrator.VibrationEffectSegment;
import java.util.ArrayList;
import java.util.List;
/* loaded from: classes2.dex */
public final class VibrationEffectAdapters {

    /* loaded from: classes2.dex */
    public interface EffectAdapter<T> {
        VibrationEffect apply(VibrationEffect vibrationEffect, T t);
    }

    /* loaded from: classes2.dex */
    public interface SegmentsAdapter<T> {
        int apply(List<VibrationEffectSegment> list, int i, T t);
    }

    public static <T> VibrationEffect apply(VibrationEffect effect, List<SegmentsAdapter<T>> adapters, T modifier) {
        if (!(effect instanceof VibrationEffect.Composed)) {
            return effect;
        }
        VibrationEffect.Composed composed = (VibrationEffect.Composed) effect;
        List<VibrationEffectSegment> newSegments = new ArrayList<>(composed.getSegments());
        int newRepeatIndex = composed.getRepeatIndex();
        int adapterCount = adapters.size();
        for (int i = 0; i < adapterCount; i++) {
            newRepeatIndex = adapters.get(i).apply(newSegments, newRepeatIndex, modifier);
        }
        return new VibrationEffect.Composed(newSegments, newRepeatIndex);
    }
}
