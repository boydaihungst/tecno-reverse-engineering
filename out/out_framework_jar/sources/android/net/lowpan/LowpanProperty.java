package android.net.lowpan;

import java.util.Map;
/* loaded from: classes2.dex */
public abstract class LowpanProperty<T> {
    public abstract String getName();

    public abstract Class<T> getType();

    public void putInMap(Map map, T value) {
        map.put(getName(), value);
    }

    public T getFromMap(Map map) {
        return (T) map.get(getName());
    }
}
