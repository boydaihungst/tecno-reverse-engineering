package android.util;
/* loaded from: classes3.dex */
public abstract class FloatProperty<T> extends Property<T, Float> {
    public abstract void setValue(T t, float f);

    /* JADX DEBUG: Method arguments types fixed to match base method, original types: [java.lang.Object, java.lang.Object] */
    /* JADX DEBUG: Multi-variable search result rejected for r1v0, resolved type: java.lang.Object */
    /* JADX WARN: Multi-variable type inference failed */
    @Override // android.util.Property
    public /* bridge */ /* synthetic */ void set(Object obj, Float f) {
        set2((FloatProperty<T>) obj, f);
    }

    public FloatProperty(String name) {
        super(Float.class, name);
    }

    /* renamed from: set  reason: avoid collision after fix types in other method */
    public final void set2(T object, Float value) {
        setValue(object, value.floatValue());
    }
}
