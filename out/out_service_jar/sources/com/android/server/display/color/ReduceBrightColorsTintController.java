package com.android.server.display.color;

import android.content.Context;
import android.hardware.display.ColorDisplayManager;
import android.opengl.Matrix;
import android.util.Slog;
import com.android.server.display.color.ColorDisplayService;
import java.io.PrintWriter;
import java.util.Arrays;
/* loaded from: classes.dex */
public class ReduceBrightColorsTintController extends TintController {
    private int mStrength;
    private final float[] mMatrix = new float[16];
    private final float[] mCoefficients = new float[3];

    @Override // com.android.server.display.color.TintController
    public /* bridge */ /* synthetic */ void cancelAnimator() {
        super.cancelAnimator();
    }

    @Override // com.android.server.display.color.TintController
    public /* bridge */ /* synthetic */ void endAnimator() {
        super.endAnimator();
    }

    @Override // com.android.server.display.color.TintController
    public /* bridge */ /* synthetic */ ColorDisplayService.TintValueAnimator getAnimator() {
        return super.getAnimator();
    }

    @Override // com.android.server.display.color.TintController
    public /* bridge */ /* synthetic */ boolean isActivated() {
        return super.isActivated();
    }

    @Override // com.android.server.display.color.TintController
    public /* bridge */ /* synthetic */ boolean isActivatedStateNotSet() {
        return super.isActivatedStateNotSet();
    }

    @Override // com.android.server.display.color.TintController
    public /* bridge */ /* synthetic */ void setAnimator(ColorDisplayService.TintValueAnimator tintValueAnimator) {
        super.setAnimator(tintValueAnimator);
    }

    @Override // com.android.server.display.color.TintController
    public void setUp(Context context, boolean needsLinear) {
        String[] coefficients = context.getResources().getStringArray(needsLinear ? 17236111 : 17236112);
        for (int i = 0; i < 3 && i < coefficients.length; i++) {
            this.mCoefficients[i] = Float.parseFloat(coefficients[i]);
        }
    }

    @Override // com.android.server.display.color.TintController
    public float[] getMatrix() {
        if (isActivated()) {
            float[] fArr = this.mMatrix;
            return Arrays.copyOf(fArr, fArr.length);
        }
        return ColorDisplayService.MATRIX_IDENTITY;
    }

    @Override // com.android.server.display.color.TintController
    public void setMatrix(int strengthLevel) {
        if (strengthLevel < 0) {
            strengthLevel = 0;
        } else if (strengthLevel > 100) {
            strengthLevel = 100;
        }
        Slog.d("ColorDisplayService", "Setting bright color reduction level: " + strengthLevel);
        this.mStrength = strengthLevel;
        Matrix.setIdentityM(this.mMatrix, 0);
        float componentValue = computeComponentValue(strengthLevel);
        float[] fArr = this.mMatrix;
        fArr[0] = componentValue;
        fArr[5] = componentValue;
        fArr[10] = componentValue;
    }

    private float clamp(float value) {
        if (value > 1.0f) {
            return 1.0f;
        }
        if (value < 0.0f) {
            return 0.0f;
        }
        return value;
    }

    @Override // com.android.server.display.color.TintController
    public void dump(PrintWriter pw) {
        pw.println("    mStrength = " + this.mStrength);
    }

    @Override // com.android.server.display.color.TintController
    public int getLevel() {
        return 250;
    }

    @Override // com.android.server.display.color.TintController
    public boolean isAvailable(Context context) {
        return ColorDisplayManager.isColorTransformAccelerated(context);
    }

    @Override // com.android.server.display.color.TintController
    public void setActivated(Boolean isActivated) {
        super.setActivated(isActivated);
        Slog.i("ColorDisplayService", (isActivated == null || !isActivated.booleanValue()) ? "Turning off reduce bright colors" : "Turning on reduce bright colors");
    }

    public int getStrength() {
        return this.mStrength;
    }

    public float getOffsetFactor() {
        float[] fArr = this.mCoefficients;
        return fArr[0] + fArr[1] + fArr[2];
    }

    public float getAdjustedBrightness(float nits) {
        return computeComponentValue(this.mStrength) * nits;
    }

    private float computeComponentValue(int strengthLevel) {
        float percentageStrength = strengthLevel / 100.0f;
        float squaredPercentageStrength = percentageStrength * percentageStrength;
        float[] fArr = this.mCoefficients;
        return clamp((fArr[0] * squaredPercentageStrength) + (fArr[1] * percentageStrength) + fArr[2]);
    }
}
