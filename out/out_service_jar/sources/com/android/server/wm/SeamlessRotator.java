package com.android.server.wm;

import android.graphics.Matrix;
import android.view.DisplayInfo;
import android.view.SurfaceControl;
import com.android.server.wm.utils.CoordinateTransforms;
import java.io.PrintWriter;
import java.io.StringWriter;
/* loaded from: classes2.dex */
public class SeamlessRotator {
    private final boolean mApplyFixedTransformHint;
    private final int mFixedTransformHint;
    private final float[] mFloat9;
    private final int mNewRotation;
    private final int mOldRotation;
    private final Matrix mTransform;

    public SeamlessRotator(int oldRotation, int newRotation, DisplayInfo info, boolean applyFixedTransformationHint) {
        Matrix matrix = new Matrix();
        this.mTransform = matrix;
        this.mFloat9 = new float[9];
        this.mOldRotation = oldRotation;
        this.mNewRotation = newRotation;
        this.mApplyFixedTransformHint = applyFixedTransformationHint;
        this.mFixedTransformHint = oldRotation;
        boolean z = true;
        if (info.rotation != 1 && info.rotation != 3) {
            z = false;
        }
        boolean flipped = z;
        int pH = flipped ? info.logicalWidth : info.logicalHeight;
        int pW = flipped ? info.logicalHeight : info.logicalWidth;
        Matrix tmp = new Matrix();
        CoordinateTransforms.transformLogicalToPhysicalCoordinates(oldRotation, pW, pH, matrix);
        CoordinateTransforms.transformPhysicalToLogicalCoordinates(newRotation, pW, pH, tmp);
        matrix.postConcat(tmp);
    }

    public void unrotate(SurfaceControl.Transaction transaction, WindowContainer win) {
        applyTransform(transaction, win.getSurfaceControl());
        float[] winSurfacePos = {win.mLastSurfacePosition.x, win.mLastSurfacePosition.y};
        this.mTransform.mapPoints(winSurfacePos);
        transaction.setPosition(win.getSurfaceControl(), winSurfacePos[0], winSurfacePos[1]);
        if (this.mApplyFixedTransformHint) {
            transaction.setFixedTransformHint(win.mSurfaceControl, this.mFixedTransformHint);
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void applyTransform(SurfaceControl.Transaction t, SurfaceControl sc) {
        t.setMatrix(sc, this.mTransform, this.mFloat9);
    }

    public int getOldRotation() {
        return this.mOldRotation;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void finish(SurfaceControl.Transaction t, WindowContainer win) {
        if (win.mSurfaceControl == null || !win.mSurfaceControl.isValid()) {
            return;
        }
        setIdentityMatrix(t, win.mSurfaceControl);
        t.setPosition(win.mSurfaceControl, win.mLastSurfacePosition.x, win.mLastSurfacePosition.y);
        if (this.mApplyFixedTransformHint) {
            t.unsetFixedTransformHint(win.mSurfaceControl);
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void setIdentityMatrix(SurfaceControl.Transaction t, SurfaceControl sc) {
        t.setMatrix(sc, Matrix.IDENTITY_MATRIX, this.mFloat9);
    }

    public void dump(PrintWriter pw) {
        pw.print("{old=");
        pw.print(this.mOldRotation);
        pw.print(", new=");
        pw.print(this.mNewRotation);
        pw.print("}");
    }

    public String toString() {
        StringWriter sw = new StringWriter();
        dump(new PrintWriter(sw));
        return "ForcedSeamlessRotator" + sw.toString();
    }
}
