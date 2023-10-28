package android.animation;
/* loaded from: classes.dex */
public class FloatEvaluator implements TypeEvaluator<Number> {
    /* JADX DEBUG: Method merged with bridge method */
    @Override // android.animation.TypeEvaluator
    public Float evaluate(float fraction, Number startValue, Number endValue) {
        float startFloat = startValue.floatValue();
        return Float.valueOf(((endValue.floatValue() - startFloat) * fraction) + startFloat);
    }
}
