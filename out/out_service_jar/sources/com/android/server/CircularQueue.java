package com.android.server;

import android.util.ArrayMap;
import java.util.Collection;
import java.util.LinkedList;
/* loaded from: classes.dex */
public class CircularQueue<K, V> extends LinkedList<K> {
    private final ArrayMap<K, V> mArrayMap = new ArrayMap<>();
    private final int mLimit;

    public CircularQueue(int limit) {
        this.mLimit = limit;
    }

    @Override // java.util.LinkedList, java.util.AbstractList, java.util.AbstractCollection, java.util.Collection, java.util.List, java.util.Deque, java.util.Queue
    public boolean add(K k) throws IllegalArgumentException {
        throw new IllegalArgumentException("Call of add(key) prohibited. Please call put(key, value) instead. ");
    }

    public V put(K key, V value) {
        super.add(key);
        this.mArrayMap.put(key, value);
        V removedValue = null;
        while (size() > this.mLimit) {
            removedValue = this.mArrayMap.remove(super.remove());
        }
        return removedValue;
    }

    public V removeElement(K key) {
        super.remove(key);
        return this.mArrayMap.remove(key);
    }

    public V getElement(K key) {
        return this.mArrayMap.get(key);
    }

    public boolean containsKey(K key) {
        return this.mArrayMap.containsKey(key);
    }

    public Collection<V> values() {
        return this.mArrayMap.values();
    }
}
