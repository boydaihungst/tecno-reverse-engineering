package android.animation.dynamicanimation;

import android.util.FloatProperty;
/* loaded from: classes.dex */
public abstract class FloatPropertyCompat<T> {
    final String mPropertyName;

    public abstract float getValue(T t);

    public abstract void setValue(T t, float f);

    public FloatPropertyCompat(String name) {
        this.mPropertyName = name;
    }

    public static <T> FloatPropertyCompat<T> createFloatPropertyCompat(final FloatProperty<T> property) {
        return new FloatPropertyCompat<T>(property.getName()) { // from class: android.animation.dynamicanimation.FloatPropertyCompat.1
            @Override // android.animation.dynamicanimation.FloatPropertyCompat
            public float getValue(T object) {
                return property.get(object).floatValue();
            }

            @Override // android.animation.dynamicanimation.FloatPropertyCompat
            public void setValue(T object, float value) {
                property.setValue(object, value);
            }
        };
    }
}
