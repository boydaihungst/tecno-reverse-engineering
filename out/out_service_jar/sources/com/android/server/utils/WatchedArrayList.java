package com.android.server.utils;

import java.util.ArrayList;
import java.util.Collection;
/* loaded from: classes2.dex */
public class WatchedArrayList<E> extends WatchableImpl implements Snappable {
    private final Watcher mObserver;
    private final ArrayList<E> mStorage;
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
                registerChild(this.mStorage.get(i));
            }
        }
    }

    @Override // com.android.server.utils.WatchableImpl, com.android.server.utils.Watchable
    public void unregisterObserver(Watcher observer) {
        super.unregisterObserver(observer);
        if (registeredObserverCount() == 0) {
            int end = this.mStorage.size();
            for (int i = 0; i < end; i++) {
                unregisterChild(this.mStorage.get(i));
            }
            this.mWatching = false;
        }
    }

    public WatchedArrayList() {
        this(0);
    }

    public WatchedArrayList(int capacity) {
        this.mWatching = false;
        this.mObserver = new Watcher() { // from class: com.android.server.utils.WatchedArrayList.1
            @Override // com.android.server.utils.Watcher
            public void onChange(Watchable what) {
                WatchedArrayList.this.dispatchChange(what);
            }
        };
        this.mStorage = new ArrayList<>(capacity);
    }

    public WatchedArrayList(Collection<? extends E> c) {
        this.mWatching = false;
        this.mObserver = new Watcher() { // from class: com.android.server.utils.WatchedArrayList.1
            @Override // com.android.server.utils.Watcher
            public void onChange(Watchable what) {
                WatchedArrayList.this.dispatchChange(what);
            }
        };
        ArrayList<E> arrayList = new ArrayList<>();
        this.mStorage = arrayList;
        if (c != null) {
            arrayList.addAll(c);
        }
    }

    public WatchedArrayList(ArrayList<E> c) {
        this.mWatching = false;
        this.mObserver = new Watcher() { // from class: com.android.server.utils.WatchedArrayList.1
            @Override // com.android.server.utils.Watcher
            public void onChange(Watchable what) {
                WatchedArrayList.this.dispatchChange(what);
            }
        };
        this.mStorage = new ArrayList<>(c);
    }

    public WatchedArrayList(WatchedArrayList<E> c) {
        this.mWatching = false;
        this.mObserver = new Watcher() { // from class: com.android.server.utils.WatchedArrayList.1
            @Override // com.android.server.utils.Watcher
            public void onChange(Watchable what) {
                WatchedArrayList.this.dispatchChange(what);
            }
        };
        this.mStorage = new ArrayList<>(c.mStorage);
    }

    public void copyFrom(ArrayList<E> src) {
        clear();
        int end = src.size();
        this.mStorage.ensureCapacity(end);
        for (int i = 0; i < end; i++) {
            add(src.get(i));
        }
    }

    public void copyTo(ArrayList<E> dst) {
        dst.clear();
        int end = size();
        dst.ensureCapacity(end);
        for (int i = 0; i < end; i++) {
            dst.add(get(i));
        }
    }

    public ArrayList<E> untrackedStorage() {
        return this.mStorage;
    }

    public boolean add(E value) {
        boolean result = this.mStorage.add(value);
        registerChild(value);
        onChanged();
        return result;
    }

    public void add(int index, E value) {
        this.mStorage.add(index, value);
        registerChild(value);
        onChanged();
    }

    public boolean addAll(Collection<? extends E> c) {
        if (c.size() > 0) {
            for (E e : c) {
                this.mStorage.add(e);
            }
            onChanged();
            return true;
        }
        return false;
    }

    public boolean addAll(int index, Collection<? extends E> c) {
        if (c.size() > 0) {
            for (E e : c) {
                this.mStorage.add(index, e);
                index++;
            }
            onChanged();
            return true;
        }
        return false;
    }

    public void clear() {
        if (this.mWatching) {
            int end = this.mStorage.size();
            for (int i = 0; i < end; i++) {
                unregisterChild(this.mStorage.get(i));
            }
        }
        this.mStorage.clear();
        onChanged();
    }

    public boolean contains(Object o) {
        return this.mStorage.contains(o);
    }

    public boolean containsAll(Collection<?> c) {
        return this.mStorage.containsAll(c);
    }

    public void ensureCapacity(int min) {
        this.mStorage.ensureCapacity(min);
    }

    public E get(int index) {
        return this.mStorage.get(index);
    }

    public int indexOf(Object o) {
        return this.mStorage.indexOf(o);
    }

    public boolean isEmpty() {
        return this.mStorage.isEmpty();
    }

    public int lastIndexOf(Object o) {
        return this.mStorage.lastIndexOf(o);
    }

    public E remove(int index) {
        E result = this.mStorage.remove(index);
        unregisterChildIf(result);
        onChanged();
        return result;
    }

    public boolean remove(Object o) {
        if (this.mStorage.remove(o)) {
            unregisterChildIf(o);
            onChanged();
            return true;
        }
        return false;
    }

    public E set(int index, E value) {
        E result = this.mStorage.set(index, value);
        if (value != result) {
            unregisterChildIf(result);
            registerChild(value);
            onChanged();
        }
        return result;
    }

    public int size() {
        return this.mStorage.size();
    }

    public boolean equals(Object o) {
        if (o instanceof WatchedArrayList) {
            WatchedArrayList w = (WatchedArrayList) o;
            return this.mStorage.equals(w.mStorage);
        }
        return false;
    }

    public int hashCode() {
        return this.mStorage.hashCode();
    }

    /* JADX DEBUG: Method merged with bridge method */
    @Override // com.android.server.utils.Snappable
    public WatchedArrayList<E> snapshot() {
        WatchedArrayList<E> l = new WatchedArrayList<>(size());
        snapshot(l, this);
        return l;
    }

    public void snapshot(WatchedArrayList<E> r) {
        snapshot(this, r);
    }

    /* JADX DEBUG: Multi-variable search result rejected for r3v0, resolved type: java.util.ArrayList<E> */
    /* JADX WARN: Multi-variable type inference failed */
    public static <E> void snapshot(WatchedArrayList<E> dst, WatchedArrayList<E> src) {
        if (dst.size() != 0) {
            throw new IllegalArgumentException("snapshot destination is not empty");
        }
        int end = src.size();
        ((WatchedArrayList) dst).mStorage.ensureCapacity(end);
        for (int i = 0; i < end; i++) {
            ((WatchedArrayList) dst).mStorage.add(Snapshots.maybeSnapshot(src.get(i)));
        }
        dst.seal();
    }
}
