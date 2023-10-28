package com.android.server.utils;

import android.util.ArraySet;
import java.util.Collection;
/* loaded from: classes2.dex */
public class WatchedArraySet<E> extends WatchableImpl implements Snappable {
    private final Watcher mObserver;
    private final ArraySet<E> mStorage;
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
        if (this.mWatching && (o instanceof Watchable) && !this.mStorage.contains(o)) {
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

    public WatchedArraySet() {
        this(0, false);
    }

    public WatchedArraySet(int capacity) {
        this(capacity, false);
    }

    public WatchedArraySet(int capacity, boolean identityHashCode) {
        this.mWatching = false;
        this.mObserver = new Watcher() { // from class: com.android.server.utils.WatchedArraySet.1
            @Override // com.android.server.utils.Watcher
            public void onChange(Watchable what) {
                WatchedArraySet.this.dispatchChange(what);
            }
        };
        this.mStorage = new ArraySet<>(capacity, identityHashCode);
    }

    public WatchedArraySet(E[] array) {
        this.mWatching = false;
        this.mObserver = new Watcher() { // from class: com.android.server.utils.WatchedArraySet.1
            @Override // com.android.server.utils.Watcher
            public void onChange(Watchable what) {
                WatchedArraySet.this.dispatchChange(what);
            }
        };
        this.mStorage = new ArraySet<>(array);
    }

    public WatchedArraySet(ArraySet<E> c) {
        this.mWatching = false;
        this.mObserver = new Watcher() { // from class: com.android.server.utils.WatchedArraySet.1
            @Override // com.android.server.utils.Watcher
            public void onChange(Watchable what) {
                WatchedArraySet.this.dispatchChange(what);
            }
        };
        this.mStorage = new ArraySet<>((ArraySet) c);
    }

    public WatchedArraySet(WatchedArraySet<E> c) {
        this.mWatching = false;
        this.mObserver = new Watcher() { // from class: com.android.server.utils.WatchedArraySet.1
            @Override // com.android.server.utils.Watcher
            public void onChange(Watchable what) {
                WatchedArraySet.this.dispatchChange(what);
            }
        };
        this.mStorage = new ArraySet<>((ArraySet) c.mStorage);
    }

    public void copyFrom(ArraySet<E> src) {
        clear();
        int end = src.size();
        this.mStorage.ensureCapacity(end);
        for (int i = 0; i < end; i++) {
            add(src.valueAt(i));
        }
    }

    public void copyTo(ArraySet<E> dst) {
        dst.clear();
        int end = size();
        dst.ensureCapacity(end);
        for (int i = 0; i < end; i++) {
            dst.add(valueAt(i));
        }
    }

    public ArraySet<E> untrackedStorage() {
        return this.mStorage;
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

    public boolean contains(Object key) {
        return this.mStorage.contains(key);
    }

    public int indexOf(Object key) {
        return this.mStorage.indexOf(key);
    }

    public E valueAt(int index) {
        return this.mStorage.valueAt(index);
    }

    public boolean isEmpty() {
        return this.mStorage.isEmpty();
    }

    public boolean add(E value) {
        boolean result = this.mStorage.add(value);
        registerChild(value);
        onChanged();
        return result;
    }

    public void append(E value) {
        this.mStorage.append(value);
        registerChild(value);
        onChanged();
    }

    public void addAll(Collection<? extends E> collection) {
        this.mStorage.addAll(collection);
        onChanged();
    }

    public void addAll(WatchedArraySet<? extends E> array) {
        int end = array.size();
        for (int i = 0; i < end; i++) {
            add(array.valueAt(i));
        }
    }

    public boolean remove(Object o) {
        if (this.mStorage.remove(o)) {
            unregisterChildIf(o);
            onChanged();
            return true;
        }
        return false;
    }

    public E removeAt(int index) {
        E result = this.mStorage.removeAt(index);
        unregisterChildIf(result);
        onChanged();
        return result;
    }

    public boolean removeAll(ArraySet<? extends E> array) {
        int end = array.size();
        boolean any = false;
        for (int i = 0; i < end; i++) {
            any = remove(array.valueAt(i)) || any;
        }
        return any;
    }

    public int size() {
        return this.mStorage.size();
    }

    public boolean equals(Object object) {
        if (object instanceof WatchedArraySet) {
            return this.mStorage.equals(((WatchedArraySet) object).mStorage);
        }
        return this.mStorage.equals(object);
    }

    public int hashCode() {
        return this.mStorage.hashCode();
    }

    public String toString() {
        return this.mStorage.toString();
    }

    /* JADX DEBUG: Method merged with bridge method */
    @Override // com.android.server.utils.Snappable
    public WatchedArraySet<E> snapshot() {
        WatchedArraySet<E> l = new WatchedArraySet<>();
        snapshot(l, this);
        return l;
    }

    public void snapshot(WatchedArraySet<E> r) {
        snapshot(this, r);
    }

    public static <E> void snapshot(WatchedArraySet<E> dst, WatchedArraySet<E> src) {
        if (dst.size() != 0) {
            throw new IllegalArgumentException("snapshot destination is not empty");
        }
        int end = src.size();
        ((WatchedArraySet) dst).mStorage.ensureCapacity(end);
        for (int i = 0; i < end; i++) {
            ((WatchedArraySet) dst).mStorage.append(Snapshots.maybeSnapshot(src.valueAt(i)));
        }
        dst.seal();
    }
}
