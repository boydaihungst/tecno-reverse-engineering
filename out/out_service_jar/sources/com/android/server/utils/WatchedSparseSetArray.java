package com.android.server.utils;

import android.util.ArraySet;
import android.util.SparseSetArray;
/* loaded from: classes2.dex */
public class WatchedSparseSetArray<T> extends WatchableImpl implements Snappable {
    private final SparseSetArray mStorage;

    private void onChanged() {
        dispatchChange(this);
    }

    public WatchedSparseSetArray() {
        this.mStorage = new SparseSetArray();
    }

    public WatchedSparseSetArray(WatchedSparseSetArray<T> watchedSparseSetArray) {
        this.mStorage = new SparseSetArray(watchedSparseSetArray.untrackedStorage());
    }

    public SparseSetArray<T> untrackedStorage() {
        return this.mStorage;
    }

    public boolean add(int n, T value) {
        boolean res = this.mStorage.add(n, value);
        onChanged();
        return res;
    }

    public void clear() {
        this.mStorage.clear();
        onChanged();
    }

    public boolean contains(int n, T value) {
        return this.mStorage.contains(n, value);
    }

    public ArraySet<T> get(int n) {
        return this.mStorage.get(n);
    }

    public boolean remove(int n, T value) {
        if (this.mStorage.remove(n, value)) {
            onChanged();
            return true;
        }
        return false;
    }

    public void remove(int n) {
        this.mStorage.remove(n);
        onChanged();
    }

    public int size() {
        return this.mStorage.size();
    }

    public int keyAt(int index) {
        return this.mStorage.keyAt(index);
    }

    public int sizeAt(int index) {
        return this.mStorage.sizeAt(index);
    }

    public T valueAt(int intIndex, int valueIndex) {
        return (T) this.mStorage.valueAt(intIndex, valueIndex);
    }

    @Override // com.android.server.utils.Snappable
    public Object snapshot() {
        WatchedSparseSetArray l = new WatchedSparseSetArray(this);
        l.seal();
        return l;
    }

    public void snapshot(WatchedSparseSetArray<T> r) {
        snapshot(this, r);
    }

    public static void snapshot(WatchedSparseSetArray dst, WatchedSparseSetArray src) {
        if (dst.size() != 0) {
            throw new IllegalArgumentException("snapshot destination is not empty");
        }
        int arraySize = src.size();
        for (int i = 0; i < arraySize; i++) {
            ArraySet set = src.get(i);
            int setSize = set.size();
            for (int j = 0; j < setSize; j++) {
                dst.mStorage.add(src.keyAt(i), set.valueAt(j));
            }
        }
        dst.seal();
    }
}
