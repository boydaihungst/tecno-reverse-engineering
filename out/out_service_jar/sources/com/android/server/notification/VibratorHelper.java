package com.android.server.notification;

import android.content.Context;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.media.AudioAttributes;
import android.os.VibrationAttributes;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.util.Slog;
import com.android.server.pm.PackageManagerService;
import java.time.Duration;
import java.util.Arrays;
/* loaded from: classes2.dex */
public final class VibratorHelper {
    private static final long[] DEFAULT_VIBRATE_PATTERN = {0, 250, 250, 250};
    private static final String TAG = "NotificationVibratorHelper";
    private static final int VIBRATE_PATTERN_MAXLEN = 17;
    private final long[] mDefaultPattern;
    private final float[] mDefaultPwlePattern;
    private final long[] mFallbackPattern;
    private final float[] mFallbackPwlePattern;
    private final Vibrator mVibrator;

    public VibratorHelper(Context context) {
        this.mVibrator = (Vibrator) context.getSystemService(Vibrator.class);
        Resources resources = context.getResources();
        long[] jArr = DEFAULT_VIBRATE_PATTERN;
        this.mDefaultPattern = getLongArray(resources, 17236022, 17, jArr);
        this.mFallbackPattern = getLongArray(context.getResources(), 17236102, 17, jArr);
        this.mDefaultPwlePattern = getFloatArray(context.getResources(), 17236023);
        this.mFallbackPwlePattern = getFloatArray(context.getResources(), 17236103);
    }

    public static VibrationEffect createWaveformVibration(long[] pattern, boolean insistent) {
        if (pattern != null) {
            try {
                return VibrationEffect.createWaveform(pattern, insistent ? 0 : -1);
            } catch (IllegalArgumentException e) {
                Slog.e(TAG, "Error creating vibration waveform with pattern: " + Arrays.toString(pattern));
                return null;
            }
        }
        return null;
    }

    public static VibrationEffect createPwleWaveformVibration(float[] values, boolean insistent) {
        if (values == null) {
            return null;
        }
        try {
            int length = values.length;
            if (length != 0 && length % 3 == 0) {
                VibrationEffect.WaveformBuilder waveformBuilder = VibrationEffect.startWaveform();
                for (int i = 0; i < length; i += 3) {
                    waveformBuilder.addTransition(Duration.ofMillis((int) values[i + 2]), VibrationEffect.VibrationParameter.targetAmplitude(values[i]), VibrationEffect.VibrationParameter.targetFrequency(values[i + 1]));
                }
                VibrationEffect effect = waveformBuilder.build();
                if (insistent) {
                    return VibrationEffect.startComposition().repeatEffectIndefinitely(effect).compose();
                }
                return effect;
            }
            return null;
        } catch (IllegalArgumentException e) {
            Slog.e(TAG, "Error creating vibration PWLE waveform with pattern: " + Arrays.toString(values));
            return null;
        }
    }

    public void vibrate(VibrationEffect effect, AudioAttributes attrs, String reason) {
        this.mVibrator.vibrate(1000, PackageManagerService.PLATFORM_PACKAGE_NAME, effect, reason, new VibrationAttributes.Builder(attrs).build());
    }

    public void cancelVibration() {
        this.mVibrator.cancel(-15);
    }

    public VibrationEffect createFallbackVibration(boolean insistent) {
        VibrationEffect effect;
        if (this.mVibrator.hasFrequencyControl() && (effect = createPwleWaveformVibration(this.mFallbackPwlePattern, insistent)) != null) {
            return effect;
        }
        return createWaveformVibration(this.mFallbackPattern, insistent);
    }

    public VibrationEffect createDefaultVibration(boolean insistent) {
        VibrationEffect effect;
        if (this.mVibrator.hasFrequencyControl() && (effect = createPwleWaveformVibration(this.mDefaultPwlePattern, insistent)) != null) {
            return effect;
        }
        return createWaveformVibration(this.mDefaultPattern, insistent);
    }

    private static float[] getFloatArray(Resources resources, int resId) {
        TypedArray array = resources.obtainTypedArray(resId);
        try {
            float[] values = new float[array.length()];
            for (int i = 0; i < values.length; i++) {
                values[i] = array.getFloat(i, Float.NaN);
                if (Float.isNaN(values[i])) {
                    return null;
                }
            }
            return values;
        } finally {
            array.recycle();
        }
    }

    private static long[] getLongArray(Resources resources, int resId, int maxLength, long[] def) {
        int[] ar = resources.getIntArray(resId);
        if (ar == null) {
            return def;
        }
        int len = ar.length > maxLength ? maxLength : ar.length;
        long[] out = new long[len];
        for (int i = 0; i < len; i++) {
            out[i] = ar[i];
        }
        return out;
    }
}
