package com.android.server.utils;

import android.util.SparseIntArray;
/* loaded from: classes2.dex */
public class WatchedSparseIntArray extends WatchableImpl implements Snappable {
    private final SparseIntArray mStorage;

    private void onChanged() {
        dispatchChange(this);
    }

    public WatchedSparseIntArray() {
        this.mStorage = new SparseIntArray();
    }

    public WatchedSparseIntArray(int initialCapacity) {
        this.mStorage = new SparseIntArray(initialCapacity);
    }

    public WatchedSparseIntArray(SparseIntArray c) {
        this.mStorage = c.clone();
    }

    public WatchedSparseIntArray(WatchedSparseIntArray r) {
        this.mStorage = r.mStorage.clone();
    }

    public void copyFrom(SparseIntArray src) {
        clear();
        int end = src.size();
        for (int i = 0; i < end; i++) {
            put(src.keyAt(i), src.valueAt(i));
        }
    }

    public void copyTo(SparseIntArray dst) {
        dst.clear();
        int end = size();
        for (int i = 0; i < end; i++) {
            dst.put(keyAt(i), valueAt(i));
        }
    }

    public SparseIntArray untrackedStorage() {
        return this.mStorage;
    }

    public int get(int key) {
        return this.mStorage.get(key);
    }

    public int get(int key, int valueIfKeyNotFound) {
        return this.mStorage.get(key, valueIfKeyNotFound);
    }

    public void delete(int key) {
        int index = this.mStorage.indexOfKey(key);
        if (index >= 0) {
            this.mStorage.removeAt(index);
            onChanged();
        }
    }

    public void removeAt(int index) {
        this.mStorage.removeAt(index);
        onChanged();
    }

    public void put(int key, int value) {
        this.mStorage.put(key, value);
        onChanged();
    }

    public int size() {
        return this.mStorage.size();
    }

    public int keyAt(int index) {
        return this.mStorage.keyAt(index);
    }

    public int valueAt(int index) {
        return this.mStorage.valueAt(index);
    }

    public void setValueAt(int index, int value) {
        if (this.mStorage.valueAt(index) != value) {
            this.mStorage.setValueAt(index, value);
            onChanged();
        }
    }

    public int indexOfKey(int key) {
        return this.mStorage.indexOfKey(key);
    }

    public int indexOfValue(int value) {
        return this.mStorage.indexOfValue(value);
    }

    public void clear() {
        int count = size();
        this.mStorage.clear();
        if (count > 0) {
            onChanged();
        }
    }

    public void append(int key, int value) {
        this.mStorage.append(key, value);
        onChanged();
    }

    public int[] copyKeys() {
        return this.mStorage.copyKeys();
    }

    public int hashCode() {
        return this.mStorage.hashCode();
    }

    public boolean equals(Object o) {
        if (o instanceof WatchedSparseIntArray) {
            WatchedSparseIntArray w = (WatchedSparseIntArray) o;
            return this.mStorage.equals(w.mStorage);
        }
        return false;
    }

    public String toString() {
        return this.mStorage.toString();
    }

    /* JADX DEBUG: Method merged with bridge method */
    @Override // com.android.server.utils.Snappable
    public WatchedSparseIntArray snapshot() {
        WatchedSparseIntArray l = new WatchedSparseIntArray(this);
        l.seal();
        return l;
    }

    public void snapshot(WatchedSparseIntArray r) {
        snapshot(this, r);
    }

    public static void snapshot(WatchedSparseIntArray dst, WatchedSparseIntArray src) {
        if (dst.size() != 0) {
            throw new IllegalArgumentException("snapshot destination is not empty");
        }
        int end = src.size();
        for (int i = 0; i < end; i++) {
            dst.mStorage.put(src.keyAt(i), src.valueAt(i));
        }
        dst.seal();
    }
}
