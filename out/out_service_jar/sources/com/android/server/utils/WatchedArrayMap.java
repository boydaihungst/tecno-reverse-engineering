package com.android.server.utils;

import android.util.ArrayMap;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
/* loaded from: classes2.dex */
public class WatchedArrayMap<K, V> extends WatchableImpl implements Map<K, V>, Snappable {
    private final Watcher mObserver;
    private final ArrayMap<K, V> mStorage;
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
        if (this.mWatching && (o instanceof Watchable) && !this.mStorage.containsValue(o)) {
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

    public WatchedArrayMap() {
        this(0, false);
    }

    public WatchedArrayMap(int capacity) {
        this(capacity, false);
    }

    public WatchedArrayMap(int capacity, boolean identityHashCode) {
        this.mWatching = false;
        this.mObserver = new Watcher() { // from class: com.android.server.utils.WatchedArrayMap.1
            @Override // com.android.server.utils.Watcher
            public void onChange(Watchable what) {
                WatchedArrayMap.this.dispatchChange(what);
            }
        };
        this.mStorage = new ArrayMap<>(capacity, identityHashCode);
    }

    public WatchedArrayMap(Map<? extends K, ? extends V> map) {
        this.mWatching = false;
        this.mObserver = new Watcher() { // from class: com.android.server.utils.WatchedArrayMap.1
            @Override // com.android.server.utils.Watcher
            public void onChange(Watchable what) {
                WatchedArrayMap.this.dispatchChange(what);
            }
        };
        this.mStorage = new ArrayMap<>();
        if (map != null) {
            putAll(map);
        }
    }

    public WatchedArrayMap(ArrayMap<K, V> c) {
        this.mWatching = false;
        this.mObserver = new Watcher() { // from class: com.android.server.utils.WatchedArrayMap.1
            @Override // com.android.server.utils.Watcher
            public void onChange(Watchable what) {
                WatchedArrayMap.this.dispatchChange(what);
            }
        };
        this.mStorage = new ArrayMap<>(c);
    }

    public WatchedArrayMap(WatchedArrayMap<K, V> c) {
        this.mWatching = false;
        this.mObserver = new Watcher() { // from class: com.android.server.utils.WatchedArrayMap.1
            @Override // com.android.server.utils.Watcher
            public void onChange(Watchable what) {
                WatchedArrayMap.this.dispatchChange(what);
            }
        };
        this.mStorage = new ArrayMap<>(c.mStorage);
    }

    public void copyFrom(ArrayMap<K, V> src) {
        clear();
        int end = src.size();
        this.mStorage.ensureCapacity(end);
        for (int i = 0; i < end; i++) {
            put(src.keyAt(i), src.valueAt(i));
        }
    }

    public void copyTo(ArrayMap<K, V> dst) {
        dst.clear();
        int end = size();
        dst.ensureCapacity(end);
        for (int i = 0; i < end; i++) {
            dst.put(keyAt(i), valueAt(i));
        }
    }

    public ArrayMap<K, V> untrackedStorage() {
        return this.mStorage;
    }

    @Override // java.util.Map
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

    @Override // java.util.Map
    public boolean containsKey(Object key) {
        return this.mStorage.containsKey(key);
    }

    @Override // java.util.Map
    public boolean containsValue(Object value) {
        return this.mStorage.containsValue(value);
    }

    @Override // java.util.Map
    public Set<Map.Entry<K, V>> entrySet() {
        return Collections.unmodifiableSet(this.mStorage.entrySet());
    }

    @Override // java.util.Map
    public boolean equals(Object o) {
        if (o instanceof WatchedArrayMap) {
            WatchedArrayMap w = (WatchedArrayMap) o;
            return this.mStorage.equals(w.mStorage);
        }
        return false;
    }

    @Override // java.util.Map
    public V get(Object key) {
        return this.mStorage.get(key);
    }

    @Override // java.util.Map
    public int hashCode() {
        return this.mStorage.hashCode();
    }

    @Override // java.util.Map
    public boolean isEmpty() {
        return this.mStorage.isEmpty();
    }

    @Override // java.util.Map
    public Set<K> keySet() {
        return Collections.unmodifiableSet(this.mStorage.keySet());
    }

    @Override // java.util.Map
    public V put(K key, V value) {
        V result = this.mStorage.put(key, value);
        registerChild(value);
        onChanged();
        return result;
    }

    @Override // java.util.Map
    public void putAll(Map<? extends K, ? extends V> map) {
        for (Map.Entry<? extends K, ? extends V> element : map.entrySet()) {
            put(element.getKey(), element.getValue());
        }
    }

    @Override // java.util.Map
    public V remove(Object key) {
        V result = this.mStorage.remove(key);
        unregisterChildIf(result);
        onChanged();
        return result;
    }

    @Override // java.util.Map
    public int size() {
        return this.mStorage.size();
    }

    @Override // java.util.Map
    public Collection<V> values() {
        return Collections.unmodifiableCollection(this.mStorage.values());
    }

    public K keyAt(int index) {
        return this.mStorage.keyAt(index);
    }

    public V valueAt(int index) {
        return this.mStorage.valueAt(index);
    }

    public int indexOfKey(K key) {
        return this.mStorage.indexOfKey(key);
    }

    public int indexOfValue(V value) {
        return this.mStorage.indexOfValue(value);
    }

    public V setValueAt(int index, V value) {
        V result = this.mStorage.setValueAt(index, value);
        if (value != result) {
            unregisterChildIf(result);
            registerChild(value);
            onChanged();
        }
        return result;
    }

    public V removeAt(int index) {
        V result = this.mStorage.removeAt(index);
        unregisterChildIf(result);
        onChanged();
        return result;
    }

    /* JADX DEBUG: Method merged with bridge method */
    @Override // com.android.server.utils.Snappable
    public WatchedArrayMap<K, V> snapshot() {
        WatchedArrayMap<K, V> l = new WatchedArrayMap<>();
        snapshot(l, this);
        return l;
    }

    public void snapshot(WatchedArrayMap<K, V> r) {
        snapshot(this, r);
    }

    /* JADX DEBUG: Multi-variable search result rejected for r4v0, resolved type: android.util.ArrayMap<K, V> */
    /* JADX WARN: Multi-variable type inference failed */
    public static <K, V> void snapshot(WatchedArrayMap<K, V> dst, WatchedArrayMap<K, V> src) {
        if (dst.size() != 0) {
            throw new IllegalArgumentException("snapshot destination is not empty");
        }
        int end = src.size();
        ((WatchedArrayMap) dst).mStorage.ensureCapacity(end);
        for (int i = 0; i < end; i++) {
            Object maybeSnapshot = Snapshots.maybeSnapshot(src.valueAt(i));
            K key = src.keyAt(i);
            ((WatchedArrayMap) dst).mStorage.put(key, maybeSnapshot);
        }
        dst.seal();
    }
}
