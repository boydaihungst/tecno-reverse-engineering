package com.android.server.utils;

import android.util.LongSparseArray;
/* loaded from: classes2.dex */
public class WatchedLongSparseArray<E> extends WatchableImpl implements Snappable {
    private final Watcher mObserver;
    private final LongSparseArray<E> mStorage;
    private volatile boolean mWatching;

    private void onChanged() {
        dispatchChange(this);
    }

    private void registerChild(Object o) {
        if (this.mWatching && (o instanceof Watchable)) {
            ((Watchable) o).registerObserver(this.mObserver);
        }
    }

    private void unregisterChild(Object o) {
        if (this.mWatching && (o instanceof Watchable)) {
            ((Watchable) o).unregisterObserver(this.mObserver);
        }
    }

    private void unregisterChildIf(Object o) {
        if (this.mWatching && (o instanceof Watchable) && this.mStorage.indexOfValue(o) == -1) {
            ((Watchable) o).unregisterObserver(this.mObserver);
        }
    }

    @Override // com.android.server.utils.WatchableImpl, com.android.server.utils.Watchable
    public void registerObserver(Watcher observer) {
        super.registerObserver(observer);
        if (registeredObserverCount() == 1) {
            this.mWatching = true;
            int end = this.mStorage.size();
            for (int i = 0; i < end; i++) {
                registerChild(this.mStorage.valueAt(i));
            }
        }
    }

    @Override // com.android.server.utils.WatchableImpl, com.android.server.utils.Watchable
    public void unregisterObserver(Watcher observer) {
        super.unregisterObserver(observer);
        if (registeredObserverCount() == 0) {
            int end = this.mStorage.size();
            for (int i = 0; i < end; i++) {
                unregisterChild(this.mStorage.valueAt(i));
            }
            this.mWatching = false;
        }
    }

    public WatchedLongSparseArray() {
        this.mWatching = false;
        this.mObserver = new Watcher() { // from class: com.android.server.utils.WatchedLongSparseArray.1
            @Override // com.android.server.utils.Watcher
            public void onChange(Watchable o) {
                WatchedLongSparseArray.this.dispatchChange(o);
            }
        };
        this.mStorage = new LongSparseArray<>();
    }

    public WatchedLongSparseArray(int initialCapacity) {
        this.mWatching = false;
        this.mObserver = new Watcher() { // from class: com.android.server.utils.WatchedLongSparseArray.1
            @Override // com.android.server.utils.Watcher
            public void onChange(Watchable o) {
                WatchedLongSparseArray.this.dispatchChange(o);
            }
        };
        this.mStorage = new LongSparseArray<>(initialCapacity);
    }

    public WatchedLongSparseArray(LongSparseArray<E> c) {
        this.mWatching = false;
        this.mObserver = new Watcher() { // from class: com.android.server.utils.WatchedLongSparseArray.1
            @Override // com.android.server.utils.Watcher
            public void onChange(Watchable o) {
                WatchedLongSparseArray.this.dispatchChange(o);
            }
        };
        this.mStorage = c.clone();
    }

    public WatchedLongSparseArray(WatchedLongSparseArray<E> r) {
        this.mWatching = false;
        this.mObserver = new Watcher() { // from class: com.android.server.utils.WatchedLongSparseArray.1
            @Override // com.android.server.utils.Watcher
            public void onChange(Watchable o) {
                WatchedLongSparseArray.this.dispatchChange(o);
            }
        };
        this.mStorage = r.mStorage.clone();
    }

    public void copyFrom(LongSparseArray<E> src) {
        clear();
        int end = src.size();
        for (int i = 0; i < end; i++) {
            put(src.keyAt(i), src.valueAt(i));
        }
    }

    public void copyTo(LongSparseArray<E> dst) {
        dst.clear();
        int end = size();
        for (int i = 0; i < end; i++) {
            dst.put(keyAt(i), valueAt(i));
        }
    }

    public LongSparseArray<E> untrackedStorage() {
        return this.mStorage;
    }

    public E get(long key) {
        return this.mStorage.get(key);
    }

    public E get(long key, E valueIfKeyNotFound) {
        return this.mStorage.get(key, valueIfKeyNotFound);
    }

    public void delete(long key) {
        E old = this.mStorage.get(key, null);
        this.mStorage.delete(key);
        unregisterChildIf(old);
        onChanged();
    }

    public void remove(long key) {
        delete(key);
    }

    public void removeAt(int index) {
        E old = this.mStorage.valueAt(index);
        this.mStorage.removeAt(index);
        unregisterChildIf(old);
        onChanged();
    }

    public void put(long key, E value) {
        E old = this.mStorage.get(key);
        this.mStorage.put(key, value);
        unregisterChildIf(old);
        registerChild(value);
        onChanged();
    }

    public int size() {
        return this.mStorage.size();
    }

    public long keyAt(int index) {
        return this.mStorage.keyAt(index);
    }

    public E valueAt(int index) {
        return this.mStorage.valueAt(index);
    }

    public void setValueAt(int index, E value) {
        E old = this.mStorage.valueAt(index);
        this.mStorage.setValueAt(index, value);
        unregisterChildIf(old);
        registerChild(value);
        onChanged();
    }

    public int indexOfKey(long key) {
        return this.mStorage.indexOfKey(key);
    }

    public int indexOfValue(E value) {
        return this.mStorage.indexOfValue(value);
    }

    public int indexOfValueByValue(E value) {
        return this.mStorage.indexOfValueByValue(value);
    }

    public void clear() {
        int end = this.mStorage.size();
        for (int i = 0; i < end; i++) {
            unregisterChild(this.mStorage.valueAt(i));
        }
        this.mStorage.clear();
        onChanged();
    }

    public void append(long key, E value) {
        this.mStorage.append(key, value);
        registerChild(value);
        onChanged();
    }

    public String toString() {
        return this.mStorage.toString();
    }

    /* JADX DEBUG: Method merged with bridge method */
    @Override // com.android.server.utils.Snappable
    public WatchedLongSparseArray<E> snapshot() {
        WatchedLongSparseArray<E> l = new WatchedLongSparseArray<>(size());
        snapshot(l, this);
        return l;
    }

    public void snapshot(WatchedLongSparseArray<E> r) {
        snapshot(this, r);
    }

    /* JADX DEBUG: Multi-variable search result rejected for r5v0, resolved type: android.util.LongSparseArray<E> */
    /* JADX WARN: Multi-variable type inference failed */
    public static <E> void snapshot(WatchedLongSparseArray<E> dst, WatchedLongSparseArray<E> src) {
        if (dst.size() != 0) {
            throw new IllegalArgumentException("snapshot destination is not empty");
        }
        int end = src.size();
        for (int i = 0; i < end; i++) {
            Object maybeSnapshot = Snapshots.maybeSnapshot(src.valueAt(i));
            long key = src.keyAt(i);
            ((WatchedLongSparseArray) dst).mStorage.put(key, maybeSnapshot);
        }
        dst.seal();
    }
}
