package com.android.server.display.color;

import android.content.Context;
import android.util.Slog;
import com.android.server.display.color.ColorDisplayService;
import java.io.PrintWriter;
/* JADX INFO: Access modifiers changed from: package-private */
/* loaded from: classes.dex */
public abstract class TintController {
    private ColorDisplayService.TintValueAnimator mAnimator;
    private Boolean mIsActivated;

    public abstract int getLevel();

    public abstract float[] getMatrix();

    public abstract boolean isAvailable(Context context);

    public abstract void setMatrix(int i);

    public abstract void setUp(Context context, boolean z);

    public ColorDisplayService.TintValueAnimator getAnimator() {
        return this.mAnimator;
    }

    public void setAnimator(ColorDisplayService.TintValueAnimator animator) {
        this.mAnimator = animator;
    }

    public void cancelAnimator() {
        ColorDisplayService.TintValueAnimator tintValueAnimator = this.mAnimator;
        if (tintValueAnimator != null) {
            tintValueAnimator.cancel();
        }
    }

    public void endAnimator() {
        ColorDisplayService.TintValueAnimator tintValueAnimator = this.mAnimator;
        if (tintValueAnimator != null) {
            tintValueAnimator.end();
            this.mAnimator = null;
        }
    }

    public void setActivated(Boolean isActivated) {
        this.mIsActivated = isActivated;
    }

    public boolean isActivated() {
        Boolean bool = this.mIsActivated;
        return bool != null && bool.booleanValue();
    }

    public boolean isActivatedStateNotSet() {
        return this.mIsActivated == null;
    }

    public void dump(PrintWriter pw) {
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static String matrixToString(float[] matrix, int columns) {
        if (matrix == null || columns <= 0) {
            Slog.e("ColorDisplayService", "Invalid arguments when formatting matrix to string, matrix is null: " + (matrix == null) + " columns: " + columns);
            return "";
        }
        StringBuilder sb = new StringBuilder("");
        for (int i = 0; i < matrix.length; i++) {
            if (i % columns == 0) {
                sb.append("\n      ");
            }
            sb.append(String.format("%9.6f", Float.valueOf(matrix[i])));
        }
        return sb.toString();
    }
}
