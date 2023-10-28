package android.util;

import android.graphics.Insets;
import android.graphics.Matrix;
import android.graphics.Point;
import android.graphics.Rect;
import android.view.SurfaceControl;
/* loaded from: classes3.dex */
public class RotationUtils {
    public static Insets rotateInsets(Insets insets, int rotation) {
        if (insets == null || insets == Insets.NONE) {
            return insets;
        }
        switch (rotation) {
            case 0:
                return insets;
            case 1:
                Insets rotated = Insets.of(insets.top, insets.right, insets.bottom, insets.left);
                return rotated;
            case 2:
                Insets rotated2 = Insets.of(insets.right, insets.bottom, insets.left, insets.top);
                return rotated2;
            case 3:
                Insets rotated3 = Insets.of(insets.bottom, insets.left, insets.top, insets.right);
                return rotated3;
            default:
                throw new IllegalArgumentException("unknown rotation: " + rotation);
        }
    }

    public static void rotateBounds(Rect inOutBounds, Rect parentBounds, int oldRotation, int newRotation) {
        rotateBounds(inOutBounds, parentBounds, deltaRotation(oldRotation, newRotation));
    }

    public static void rotateBounds(Rect inOutBounds, int parentWidth, int parentHeight, int rotation) {
        int origLeft = inOutBounds.left;
        int origTop = inOutBounds.top;
        switch (rotation) {
            case 0:
                return;
            case 1:
                inOutBounds.left = inOutBounds.top;
                inOutBounds.top = parentWidth - inOutBounds.right;
                inOutBounds.right = inOutBounds.bottom;
                inOutBounds.bottom = parentWidth - origLeft;
                return;
            case 2:
                inOutBounds.left = parentWidth - inOutBounds.right;
                inOutBounds.right = parentWidth - origLeft;
                inOutBounds.top = parentHeight - inOutBounds.bottom;
                inOutBounds.bottom = parentHeight - origTop;
                return;
            case 3:
                inOutBounds.left = parentHeight - inOutBounds.bottom;
                inOutBounds.bottom = inOutBounds.right;
                inOutBounds.right = parentHeight - inOutBounds.top;
                inOutBounds.top = origLeft;
                return;
            default:
                return;
        }
    }

    public static void rotateBounds(Rect inOutBounds, Rect parentBounds, int rotation) {
        rotateBounds(inOutBounds, parentBounds.right, parentBounds.bottom, rotation);
    }

    public static int deltaRotation(int oldRotation, int newRotation) {
        int delta = newRotation - oldRotation;
        return delta < 0 ? delta + 4 : delta;
    }

    public static void rotateSurface(SurfaceControl.Transaction t, SurfaceControl sc, int rotation) {
        switch (rotation) {
            case 0:
                t.setMatrix(sc, 1.0f, 0.0f, 0.0f, 1.0f);
                return;
            case 1:
                t.setMatrix(sc, 0.0f, -1.0f, 1.0f, 0.0f);
                return;
            case 2:
                t.setMatrix(sc, -1.0f, 0.0f, 0.0f, -1.0f);
                return;
            case 3:
                t.setMatrix(sc, 0.0f, 1.0f, -1.0f, 0.0f);
                return;
            default:
                return;
        }
    }

    public static void rotatePoint(Point inOutPoint, int rotation, int parentW, int parentH) {
        int origX = inOutPoint.x;
        switch (rotation) {
            case 0:
                return;
            case 1:
                inOutPoint.x = inOutPoint.y;
                inOutPoint.y = parentW - origX;
                return;
            case 2:
                inOutPoint.x = parentW - inOutPoint.x;
                inOutPoint.y = parentH - inOutPoint.y;
                return;
            case 3:
                inOutPoint.x = parentH - inOutPoint.y;
                inOutPoint.y = origX;
                return;
            default:
                return;
        }
    }

    public static void transformPhysicalToLogicalCoordinates(int rotation, int physicalWidth, int physicalHeight, Matrix out) {
        switch (rotation) {
            case 0:
                out.reset();
                return;
            case 1:
                out.setRotate(270.0f);
                out.postTranslate(0.0f, physicalWidth);
                return;
            case 2:
                out.setRotate(180.0f);
                out.postTranslate(physicalWidth, physicalHeight);
                return;
            case 3:
                out.setRotate(90.0f);
                out.postTranslate(physicalHeight, 0.0f);
                return;
            default:
                throw new IllegalArgumentException("Unknown rotation: " + rotation);
        }
    }
}
