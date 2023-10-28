package com.android.server.display.color;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.ColorSpace;
import android.hardware.display.ColorDisplayManager;
import android.opengl.Matrix;
import android.os.IBinder;
import android.util.Slog;
import android.view.SurfaceControl;
import java.io.PrintWriter;
/* JADX INFO: Access modifiers changed from: package-private */
/* loaded from: classes.dex */
public final class DisplayWhiteBalanceTintController extends TintController {
    private static final int COLORSPACE_MATRIX_LENGTH = 9;
    private static final int NUM_DISPLAY_PRIMARIES_VALS = 12;
    private static final int NUM_VALUES_PER_PRIMARY = 3;
    private float[] mChromaticAdaptationMatrix;
    int mCurrentColorTemperature;
    private float[] mCurrentColorTemperatureXYZ;
    ColorSpace.Rgb mDisplayColorSpaceRGB;
    private Boolean mIsAvailable;
    private int mTemperatureDefault;
    int mTemperatureMax;
    int mTemperatureMin;
    private final Object mLock = new Object();
    float[] mDisplayNominalWhiteXYZ = new float[3];
    boolean mSetUp = false;
    private float[] mMatrixDisplayWhiteBalance = new float[16];

    @Override // com.android.server.display.color.TintController
    public void setUp(Context context, boolean needsLinear) {
        this.mSetUp = false;
        Resources res = context.getResources();
        ColorSpace.Rgb displayColorSpaceRGB = getDisplayColorSpaceFromSurfaceControl();
        if (displayColorSpaceRGB == null) {
            Slog.w("ColorDisplayService", "Failed to get display color space from SurfaceControl, trying res");
            displayColorSpaceRGB = getDisplayColorSpaceFromResources(res);
            if (displayColorSpaceRGB == null) {
                Slog.e("ColorDisplayService", "Failed to get display color space from resources");
                return;
            }
        }
        if (!isColorMatrixValid(displayColorSpaceRGB.getTransform())) {
            Slog.e("ColorDisplayService", "Invalid display color space RGB-to-XYZ transform");
        } else if (!isColorMatrixValid(displayColorSpaceRGB.getInverseTransform())) {
            Slog.e("ColorDisplayService", "Invalid display color space XYZ-to-RGB transform");
        } else {
            String[] nominalWhiteValues = res.getStringArray(17236044);
            float[] displayNominalWhiteXYZ = new float[3];
            for (int i = 0; i < nominalWhiteValues.length; i++) {
                displayNominalWhiteXYZ[i] = Float.parseFloat(nominalWhiteValues[i]);
            }
            int colorTemperatureMin = res.getInteger(17694812);
            if (colorTemperatureMin <= 0) {
                Slog.e("ColorDisplayService", "Display white balance minimum temperature must be greater than 0");
                return;
            }
            int colorTemperatureMax = res.getInteger(17694811);
            if (colorTemperatureMax < colorTemperatureMin) {
                Slog.e("ColorDisplayService", "Display white balance max temp must be greater or equal to min");
                return;
            }
            int colorTemperature = res.getInteger(17694809);
            synchronized (this.mLock) {
                this.mDisplayColorSpaceRGB = displayColorSpaceRGB;
                this.mDisplayNominalWhiteXYZ = displayNominalWhiteXYZ;
                this.mTemperatureMin = colorTemperatureMin;
                this.mTemperatureMax = colorTemperatureMax;
                this.mTemperatureDefault = colorTemperature;
                this.mSetUp = true;
            }
            setMatrix(colorTemperature);
        }
    }

    @Override // com.android.server.display.color.TintController
    public float[] getMatrix() {
        return (this.mSetUp && isActivated()) ? this.mMatrixDisplayWhiteBalance : ColorDisplayService.MATRIX_IDENTITY;
    }

    private static float[] mul3x3(float[] lhs, float[] rhs) {
        float[] r = {(lhs[0] * rhs[0]) + (lhs[3] * rhs[1]) + (lhs[6] * rhs[2]), (lhs[1] * rhs[0]) + (lhs[4] * rhs[1]) + (lhs[7] * rhs[2]), (lhs[2] * rhs[0]) + (lhs[5] * rhs[1]) + (lhs[8] * rhs[2]), (lhs[0] * rhs[3]) + (lhs[3] * rhs[4]) + (lhs[6] * rhs[5]), (lhs[1] * rhs[3]) + (lhs[4] * rhs[4]) + (lhs[7] * rhs[5]), (lhs[2] * rhs[3]) + (lhs[5] * rhs[4]) + (lhs[8] * rhs[5]), (lhs[0] * rhs[6]) + (lhs[3] * rhs[7]) + (lhs[6] * rhs[8]), (lhs[1] * rhs[6]) + (lhs[4] * rhs[7]) + (lhs[7] * rhs[8]), (lhs[2] * rhs[6]) + (lhs[5] * rhs[7]) + (lhs[8] * rhs[8])};
        return r;
    }

    @Override // com.android.server.display.color.TintController
    public void setMatrix(int cct) {
        if (!this.mSetUp) {
            Slog.w("ColorDisplayService", "Can't set display white balance temperature: uninitialized");
            return;
        }
        if (cct < this.mTemperatureMin) {
            Slog.w("ColorDisplayService", "Requested display color temperature is below allowed minimum");
            cct = this.mTemperatureMin;
        } else if (cct > this.mTemperatureMax) {
            Slog.w("ColorDisplayService", "Requested display color temperature is above allowed maximum");
            cct = this.mTemperatureMax;
        }
        synchronized (this.mLock) {
            this.mCurrentColorTemperature = cct;
            this.mCurrentColorTemperatureXYZ = ColorSpace.cctToXyz(cct);
            float[] chromaticAdaptation = ColorSpace.chromaticAdaptation(ColorSpace.Adaptation.BRADFORD, this.mDisplayNominalWhiteXYZ, this.mCurrentColorTemperatureXYZ);
            this.mChromaticAdaptationMatrix = chromaticAdaptation;
            float[] result = mul3x3(this.mDisplayColorSpaceRGB.getInverseTransform(), mul3x3(chromaticAdaptation, this.mDisplayColorSpaceRGB.getTransform()));
            float adaptedMaxR = result[0] + result[3] + result[6];
            float adaptedMaxG = result[1] + result[4] + result[7];
            float adaptedMaxB = result[2] + result[5] + result[8];
            float denum = Math.max(Math.max(adaptedMaxR, adaptedMaxG), adaptedMaxB);
            Matrix.setIdentityM(this.mMatrixDisplayWhiteBalance, 0);
            for (int i = 0; i < result.length; i++) {
                result[i] = result[i] / denum;
                if (!isColorMatrixCoeffValid(result[i])) {
                    Slog.e("ColorDisplayService", "Invalid DWB color matrix");
                    return;
                }
            }
            System.arraycopy(result, 0, this.mMatrixDisplayWhiteBalance, 0, 3);
            System.arraycopy(result, 3, this.mMatrixDisplayWhiteBalance, 4, 3);
            System.arraycopy(result, 6, this.mMatrixDisplayWhiteBalance, 8, 3);
            Slog.d("ColorDisplayService", "setDisplayWhiteBalanceTemperatureMatrix: cct = " + cct + " matrix = " + matrixToString(this.mMatrixDisplayWhiteBalance, 16));
        }
    }

    @Override // com.android.server.display.color.TintController
    public int getLevel() {
        return 125;
    }

    @Override // com.android.server.display.color.TintController
    public boolean isAvailable(Context context) {
        if (this.mIsAvailable == null) {
            this.mIsAvailable = Boolean.valueOf(ColorDisplayManager.isDisplayWhiteBalanceAvailable(context));
        }
        return this.mIsAvailable.booleanValue();
    }

    @Override // com.android.server.display.color.TintController
    public void dump(PrintWriter pw) {
        synchronized (this.mLock) {
            pw.println("    mSetUp = " + this.mSetUp);
            if (this.mSetUp) {
                pw.println("    mTemperatureMin = " + this.mTemperatureMin);
                pw.println("    mTemperatureMax = " + this.mTemperatureMax);
                pw.println("    mTemperatureDefault = " + this.mTemperatureDefault);
                pw.println("    mCurrentColorTemperature = " + this.mCurrentColorTemperature);
                pw.println("    mCurrentColorTemperatureXYZ = " + matrixToString(this.mCurrentColorTemperatureXYZ, 3));
                pw.println("    mDisplayColorSpaceRGB RGB-to-XYZ = " + matrixToString(this.mDisplayColorSpaceRGB.getTransform(), 3));
                pw.println("    mChromaticAdaptationMatrix = " + matrixToString(this.mChromaticAdaptationMatrix, 3));
                pw.println("    mDisplayColorSpaceRGB XYZ-to-RGB = " + matrixToString(this.mDisplayColorSpaceRGB.getInverseTransform(), 3));
                pw.println("    mMatrixDisplayWhiteBalance = " + matrixToString(this.mMatrixDisplayWhiteBalance, 4));
            }
        }
    }

    public float getLuminance() {
        synchronized (this.mLock) {
            float[] fArr = this.mChromaticAdaptationMatrix;
            if (fArr != null && fArr.length == 9) {
                return 1.0f / ((fArr[1] + fArr[4]) + fArr[7]);
            }
            return -1.0f;
        }
    }

    private ColorSpace.Rgb makeRgbColorSpaceFromXYZ(float[] redGreenBlueXYZ, float[] whiteXYZ) {
        return new ColorSpace.Rgb("Display Color Space", redGreenBlueXYZ, whiteXYZ, 2.200000047683716d);
    }

    private ColorSpace.Rgb getDisplayColorSpaceFromSurfaceControl() {
        SurfaceControl.DisplayPrimaries primaries;
        IBinder displayToken = SurfaceControl.getInternalDisplayToken();
        if (displayToken == null || (primaries = SurfaceControl.getDisplayNativePrimaries(displayToken)) == null || primaries.red == null || primaries.green == null || primaries.blue == null || primaries.white == null) {
            return null;
        }
        return makeRgbColorSpaceFromXYZ(new float[]{primaries.red.X, primaries.red.Y, primaries.red.Z, primaries.green.X, primaries.green.Y, primaries.green.Z, primaries.blue.X, primaries.blue.Y, primaries.blue.Z}, new float[]{primaries.white.X, primaries.white.Y, primaries.white.Z});
    }

    private ColorSpace.Rgb getDisplayColorSpaceFromResources(Resources res) {
        String[] displayPrimariesValues = res.getStringArray(17236045);
        float[] displayRedGreenBlueXYZ = new float[9];
        float[] displayWhiteXYZ = new float[3];
        for (int i = 0; i < displayRedGreenBlueXYZ.length; i++) {
            displayRedGreenBlueXYZ[i] = Float.parseFloat(displayPrimariesValues[i]);
        }
        for (int i2 = 0; i2 < displayWhiteXYZ.length; i2++) {
            displayWhiteXYZ[i2] = Float.parseFloat(displayPrimariesValues[displayRedGreenBlueXYZ.length + i2]);
        }
        return makeRgbColorSpaceFromXYZ(displayRedGreenBlueXYZ, displayWhiteXYZ);
    }

    private boolean isColorMatrixCoeffValid(float coeff) {
        if (Float.isNaN(coeff) || Float.isInfinite(coeff)) {
            return false;
        }
        return true;
    }

    private boolean isColorMatrixValid(float[] matrix) {
        if (matrix == null || matrix.length != 9) {
            return false;
        }
        for (float f : matrix) {
            if (!isColorMatrixCoeffValid(f)) {
                return false;
            }
        }
        return true;
    }
}
