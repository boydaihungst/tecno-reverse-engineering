package android.animation;

import android.graphics.PointF;
/* loaded from: classes.dex */
public class PointFEvaluator implements TypeEvaluator<PointF> {
    private PointF mPoint;

    public PointFEvaluator() {
    }

    public PointFEvaluator(PointF reuse) {
        this.mPoint = reuse;
    }

    /* JADX DEBUG: Method merged with bridge method */
    @Override // android.animation.TypeEvaluator
    public PointF evaluate(float fraction, PointF startValue, PointF endValue) {
        float x = startValue.x + ((endValue.x - startValue.x) * fraction);
        float y = startValue.y + ((endValue.y - startValue.y) * fraction);
        PointF pointF = this.mPoint;
        if (pointF != null) {
            pointF.set(x, y);
            return this.mPoint;
        }
        return new PointF(x, y);
    }
}
