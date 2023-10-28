package com.android.server.display;

import android.os.SystemProperties;
import android.util.MathUtils;
/* loaded from: classes.dex */
public class BrightnessUtils {
    private static final float A = 0.17883277f;
    private static final float B = 0.28466892f;
    private static final float C = 0.5599107f;
    private static final float GAMMA_TO_LINEAR_POWER = 1.8f;
    private static final float LINEAR_TO_GAMMA_POWER = 0.5556f;
    private static final float R = 0.5f;

    public static final float convertGammaToLinear(float val) {
        float ret;
        boolean tranConvert = SystemProperties.getBoolean("persist.sys.tran.brightness.gammalinear.convert", false);
        if (tranConvert) {
            return convertGammaToLinearFloat_transsion(val);
        }
        if (val <= 0.5f) {
            ret = MathUtils.sq(val / 0.5f);
        } else {
            ret = MathUtils.exp((val - C) / A) + B;
        }
        float normalizedRet = MathUtils.constrain(ret, 0.0f, 12.0f);
        return normalizedRet / 12.0f;
    }

    public static final float convertLinearToGamma(float val) {
        boolean tranConvert = SystemProperties.getBoolean("persist.sys.tran.brightness.gammalinear.convert", false);
        if (tranConvert) {
            return convertLinearToGammaFloat_transsion(val);
        }
        float normalizedVal = 12.0f * val;
        if (normalizedVal <= 1.0f) {
            float ret = MathUtils.sqrt(normalizedVal) * 0.5f;
            return ret;
        }
        float ret2 = C + (MathUtils.log(normalizedVal - B) * A);
        return ret2;
    }

    public static final float convertGammaToLinearFloat_transsion(float val) {
        return MathUtils.pow(val, (float) GAMMA_TO_LINEAR_POWER);
    }

    public static final float convertLinearToGammaFloat_transsion(float val) {
        return MathUtils.pow(val, (float) LINEAR_TO_GAMMA_POWER);
    }
}
