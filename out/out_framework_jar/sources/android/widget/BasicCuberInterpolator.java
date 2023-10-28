package android.widget;

import android.graphics.PointF;
import android.view.animation.Interpolator;
/* loaded from: classes4.dex */
public class BasicCuberInterpolator implements Interpolator {
    private static final int ACCURACY = 4096;
    private final PointF mControlPoint1;
    private final PointF mControlPoint2;
    private int mLastI = 0;

    /* JADX INFO: Access modifiers changed from: package-private */
    public BasicCuberInterpolator(float x1, float y1, float x2, float y2) {
        PointF pointF = new PointF();
        this.mControlPoint1 = pointF;
        PointF pointF2 = new PointF();
        this.mControlPoint2 = pointF2;
        pointF.x = x1;
        pointF.y = y1;
        pointF2.x = x2;
        pointF2.y = y2;
    }

    @Override // android.animation.TimeInterpolator
    public float getInterpolation(float input) {
        float t = input;
        int i = this.mLastI;
        while (true) {
            if (i >= 4096) {
                break;
            }
            t = (i * 1.0f) / 4096.0f;
            double x = cubicCurves(t, 0.0d, this.mControlPoint1.x, this.mControlPoint2.x, 1.0d);
            if (x < input) {
                i++;
            } else {
                this.mLastI = i;
                break;
            }
        }
        double value = cubicCurves(t, 0.0d, this.mControlPoint1.y, this.mControlPoint2.y, 1.0d);
        if (value > 0.999d) {
            value = 1.0d;
            this.mLastI = 0;
        }
        return (float) value;
    }

    public static double cubicCurves(double t, double value0, double value1, double value2, double value3) {
        double u = 1.0d - t;
        double tt = t * t;
        double uu = u * u;
        double uuu = uu * u;
        double ttt = tt * t;
        double value = uuu * value0;
        return value + (uu * 3.0d * t * value1) + (3.0d * u * tt * value2) + (ttt * value3);
    }
}
