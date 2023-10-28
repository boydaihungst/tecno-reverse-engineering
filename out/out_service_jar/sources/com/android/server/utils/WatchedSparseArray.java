package com.android.server.utils;

import android.util.SparseArray;
import java.util.ArrayList;
/* loaded from: classes2.dex */
public class WatchedSparseArray<E> extends WatchableImpl implements Snappable {
    private final Watcher mObserver;
    private final SparseArray<E> mStorage;
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

    public WatchedSparseArray() {
        this.mWatching = false;
        this.mObserver = new Watcher() { // from class: com.android.server.utils.WatchedSparseArray.1
            @Override // com.android.server.utils.Watcher
            public void onChange(Watchable o) {
                WatchedSparseArray.this.dispatchChange(o);
            }
        };
        this.mStorage = new SparseArray<>();
    }

    public WatchedSparseArray(int initialCapacity) {
        this.mWatching = false;
        this.mObserver = new Watcher() { // from class: com.android.server.utils.WatchedSparseArray.1
            @Override // com.android.server.utils.Watcher
            public void onChange(Watchable o) {
                WatchedSparseArray.this.dispatchChange(o);
            }
        };
        this.mStorage = new SparseArray<>(initialCapacity);
    }

    public WatchedSparseArray(SparseArray<E> c) {
        this.mWatching = false;
        this.mObserver = new Watcher() { // from class: com.android.server.utils.WatchedSparseArray.1
            @Override // com.android.server.utils.Watcher
            public void onChange(Watchable o) {
                WatchedSparseArray.this.dispatchChange(o);
            }
        };
        this.mStorage = c.clone();
    }

    public WatchedSparseArray(WatchedSparseArray<E> r) {
        this.mWatching = false;
        this.mObserver = new Watcher() { // from class: com.android.server.utils.WatchedSparseArray.1
            @Override // com.android.server.utils.Watcher
            public void onChange(Watchable o) {
                WatchedSparseArray.this.dispatchChange(o);
            }
        };
        this.mStorage = r.mStorage.clone();
    }

    public void copyFrom(SparseArray<E> src) {
        clear();
        int end = src.size();
        for (int i = 0; i < end; i++) {
            put(src.keyAt(i), src.valueAt(i));
        }
    }

    public void copyTo(SparseArray<E> dst) {
        dst.clear();
        int end = size();
        for (int i = 0; i < end; i++) {
            dst.put(keyAt(i), valueAt(i));
        }
    }

    public SparseArray<E> untrackedStorage() {
        return this.mStorage;
    }

    public boolean contains(int key) {
        return this.mStorage.contains(key);
    }

    public E get(int key) {
        return this.mStorage.get(key);
    }

    public E get(int key, E valueIfKeyNotFound) {
        return this.mStorage.get(key, valueIfKeyNotFound);
    }

    public void delete(int key) {
        E child = this.mStorage.get(key);
        this.mStorage.delete(key);
        unregisterChildIf(child);
        onChanged();
    }

    public E removeReturnOld(int key) {
        E result = (E) this.mStorage.removeReturnOld(key);
        unregisterChildIf(result);
        return result;
    }

    public void remove(int key) {
        delete(key);
    }

    public void removeAt(int index) {
        E child = this.mStorage.valueAt(index);
        this.mStorage.removeAt(index);
        unregisterChildIf(child);
        onChanged();
    }

    /* JADX DEBUG: Don't trust debug lines info. Repeating lines: [279=4] */
    public void removeAtRange(int index, int size) {
        ArrayList<E> children = new ArrayList<>();
        for (int i = 0; i < size; i++) {
            try {
                children.add(this.mStorage.valueAt(i + index));
            } catch (Exception e) {
            }
        }
        try {
            this.mStorage.removeAtRange(index, size);
            onChanged();
        } finally {
            for (int i2 = 0; i2 < size; i2++) {
                unregisterChildIf(children.get(i2));
            }
        }
    }

    public void put(int key, E value) {
        E old = this.mStorage.get(key);
        this.mStorage.put(key, value);
        unregisterChildIf(old);
        registerChild(value);
        onChanged();
    }

    public int size() {
        return this.mStorage.size();
    }

    public int keyAt(int index) {
        return this.mStorage.keyAt(index);
    }

    public E valueAt(int index) {
        return this.mStorage.valueAt(index);
    }

    public void setValueAt(int index, E value) {
        E old = this.mStorage.valueAt(index);
        this.mStorage.setValueAt(index, value);
        if (value != old) {
            unregisterChildIf(old);
            registerChild(value);
            onChanged();
        }
    }

    public int indexOfKey(int key) {
        return this.mStorage.indexOfKey(key);
    }

    public int indexOfValue(E value) {
        return this.mStorage.indexOfValue(value);
    }

    public int indexOfValueByValue(E value) {
        return this.mStorage.indexOfValueByValue(value);
    }

    public void clear() {
        if (this.mWatching) {
            int end = this.mStorage.size();
            for (int i = 0; i < end; i++) {
                unregisterChild(this.mStorage.valueAt(i));
            }
        }
        this.mStorage.clear();
        onChanged();
    }

    public void append(int key, E value) {
        this.mStorage.append(key, value);
        registerChild(value);
        onChanged();
    }

    public int hashCode() {
        return this.mStorage.hashCode();
    }

    public boolean equals(Object o) {
        if (o instanceof WatchedSparseArray) {
            WatchedSparseArray w = (WatchedSparseArray) o;
            return this.mStorage.equals(w.mStorage);
        }
        return false;
    }

    public String toString() {
        return this.mStorage.toString();
    }

    /* JADX DEBUG: Method merged with bridge method */
    @Override // com.android.server.utils.Snappable
    public WatchedSparseArray<E> snapshot() {
        WatchedSparseArray<E> l = new WatchedSparseArray<>(size());
        snapshot(l, this);
        return l;
    }

    public void snapshot(WatchedSparseArray<E> r) {
        snapshot(this, r);
    }

    /* JADX DEBUG: Multi-variable search result rejected for r4v0, resolved type: android.util.SparseArray<E> */
    /* JADX WARN: Multi-variable type inference failed */
    public static <E> void snapshot(WatchedSparseArray<E> dst, WatchedSparseArray<E> src) {
        if (dst.size() != 0) {
            throw new IllegalArgumentException("snapshot destination is not empty");
        }
        int end = src.size();
        for (int i = 0; i < end; i++) {
            Object maybeSnapshot = Snapshots.maybeSnapshot(src.valueAt(i));
            int key = src.keyAt(i);
            ((WatchedSparseArray) dst).mStorage.put(key, maybeSnapshot);
        }
        dst.seal();
    }
}
