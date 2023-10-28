package com.android.server.vibrator;

import android.content.Context;
import android.os.VibrationEffect;
import android.os.vibrator.PrebakedSegment;
import android.util.Slog;
import android.util.SparseArray;
/* JADX INFO: Access modifiers changed from: package-private */
/* loaded from: classes2.dex */
public final class VibrationScaler {
    private static final float SCALE_FACTOR_HIGH = 1.2f;
    private static final float SCALE_FACTOR_LOW = 0.8f;
    private static final float SCALE_FACTOR_NONE = 1.0f;
    private static final float SCALE_FACTOR_VERY_HIGH = 1.4f;
    private static final float SCALE_FACTOR_VERY_LOW = 0.6f;
    private static final int SCALE_HIGH = 1;
    private static final int SCALE_LOW = -1;
    private static final int SCALE_NONE = 0;
    private static final int SCALE_VERY_HIGH = 2;
    private static final int SCALE_VERY_LOW = -2;
    private static final String TAG = "VibrationScaler";
    private final int mDefaultVibrationAmplitude;
    private final SparseArray<ScaleLevel> mScaleLevels;
    private final VibrationSettings mSettingsController;

    /* JADX INFO: Access modifiers changed from: package-private */
    public VibrationScaler(Context context, VibrationSettings settingsController) {
        this.mSettingsController = settingsController;
        this.mDefaultVibrationAmplitude = context.getResources().getInteger(17694802);
        SparseArray<ScaleLevel> sparseArray = new SparseArray<>();
        this.mScaleLevels = sparseArray;
        sparseArray.put(-2, new ScaleLevel(SCALE_FACTOR_VERY_LOW));
        sparseArray.put(-1, new ScaleLevel(SCALE_FACTOR_LOW));
        sparseArray.put(0, new ScaleLevel(1.0f));
        sparseArray.put(1, new ScaleLevel(SCALE_FACTOR_HIGH));
        sparseArray.put(2, new ScaleLevel(SCALE_FACTOR_VERY_HIGH));
    }

    public int getExternalVibrationScale(int usageHint) {
        int defaultIntensity = this.mSettingsController.getDefaultIntensity(usageHint);
        int currentIntensity = this.mSettingsController.getCurrentIntensity(usageHint);
        if (currentIntensity == 0) {
            return 0;
        }
        int scaleLevel = currentIntensity - defaultIntensity;
        if (scaleLevel >= -2 && scaleLevel <= 2) {
            return scaleLevel;
        }
        Slog.w(TAG, "Error in scaling calculations, ended up with invalid scale level " + scaleLevel + " for vibration with usage " + usageHint);
        return 0;
    }

    public <T extends VibrationEffect> T scale(VibrationEffect effect, int usageHint) {
        int defaultIntensity = this.mSettingsController.getDefaultIntensity(usageHint);
        int currentIntensity = this.mSettingsController.getCurrentIntensity(usageHint);
        if (currentIntensity == 0) {
            currentIntensity = defaultIntensity;
        }
        int newEffectStrength = intensityToEffectStrength(currentIntensity);
        T t = (T) effect.applyEffectStrength(newEffectStrength).resolve(this.mDefaultVibrationAmplitude);
        ScaleLevel scale = this.mScaleLevels.get(currentIntensity - defaultIntensity);
        if (scale == null) {
            Slog.e(TAG, "No configured scaling level! (current=" + currentIntensity + ", default= " + defaultIntensity + ")");
            return t;
        }
        return (T) t.scale(scale.factor);
    }

    public PrebakedSegment scale(PrebakedSegment prebaked, int usageHint) {
        int currentIntensity = this.mSettingsController.getCurrentIntensity(usageHint);
        if (currentIntensity == 0) {
            currentIntensity = this.mSettingsController.getDefaultIntensity(usageHint);
        }
        int newEffectStrength = intensityToEffectStrength(currentIntensity);
        return prebaked.applyEffectStrength(newEffectStrength);
    }

    private static int intensityToEffectStrength(int intensity) {
        switch (intensity) {
            case 1:
                return 0;
            case 2:
                return 1;
            case 3:
                return 2;
            default:
                Slog.w(TAG, "Got unexpected vibration intensity: " + intensity);
                return 2;
        }
    }

    /* loaded from: classes2.dex */
    private static final class ScaleLevel {
        public final float factor;

        ScaleLevel(float factor) {
            this.factor = factor;
        }

        public String toString() {
            return "ScaleLevel{factor=" + this.factor + "}";
        }
    }
}
