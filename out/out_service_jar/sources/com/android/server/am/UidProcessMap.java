package com.android.server.am;

import android.util.ArrayMap;
import android.util.SparseArray;
/* loaded from: classes.dex */
public class UidProcessMap<E> {
    final SparseArray<ArrayMap<String, E>> mMap = new SparseArray<>();

    public E get(int uid, String name) {
        ArrayMap<String, E> names = this.mMap.get(uid);
        if (names == null) {
            return null;
        }
        return names.get(name);
    }

    public E put(int uid, String name, E value) {
        ArrayMap<String, E> names = this.mMap.get(uid);
        if (names == null) {
            names = new ArrayMap<>(2);
            this.mMap.put(uid, names);
        }
        names.put(name, value);
        return value;
    }

    public E remove(int uid, String name) {
        ArrayMap<String, E> names;
        int index = this.mMap.indexOfKey(uid);
        if (index < 0 || (names = this.mMap.valueAt(index)) == null) {
            return null;
        }
        E old = names.remove(name);
        if (names.isEmpty()) {
            this.mMap.removeAt(index);
        }
        return old;
    }

    public SparseArray<ArrayMap<String, E>> getMap() {
        return this.mMap;
    }

    public int size() {
        return this.mMap.size();
    }

    public void clear() {
        this.mMap.clear();
    }

    public void putAll(UidProcessMap<E> other) {
        for (int i = other.mMap.size() - 1; i >= 0; i--) {
            int uid = other.mMap.keyAt(i);
            ArrayMap<String, E> names = this.mMap.get(uid);
            if (names != null) {
                names.putAll((ArrayMap<? extends String, ? extends E>) other.mMap.valueAt(i));
            } else {
                this.mMap.put(uid, new ArrayMap<>(other.mMap.valueAt(i)));
            }
        }
    }
}
