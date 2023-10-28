package com.android.server.utils;

import java.util.WeakHashMap;
import java.util.concurrent.atomic.AtomicInteger;
/* loaded from: classes2.dex */
public abstract class SnapshotCache<T> extends Watcher {
    private static final boolean ENABLED = true;
    private static final WeakHashMap<SnapshotCache, Void> sCaches = new WeakHashMap<>();
    private volatile boolean mSealed;
    private volatile T mSnapshot;
    protected final T mSource;
    private final Statistics mStatistics;

    public abstract T createSnapshot();

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes2.dex */
    public static class Statistics {
        final String mName;
        private final AtomicInteger mReused = new AtomicInteger(0);
        private final AtomicInteger mRebuilt = new AtomicInteger(0);

        Statistics(String n) {
            this.mName = n;
        }
    }

    public SnapshotCache(T source, Watchable watchable, String name) {
        this.mSnapshot = null;
        this.mSealed = false;
        this.mSource = source;
        watchable.registerObserver(this);
        if (name != null) {
            this.mStatistics = new Statistics(name);
            sCaches.put(this, null);
            return;
        }
        this.mStatistics = null;
    }

    public SnapshotCache(T source, Watchable watchable) {
        this(source, watchable, null);
    }

    public SnapshotCache() {
        this.mSnapshot = null;
        this.mSealed = false;
        this.mSource = null;
        this.mSealed = true;
        this.mStatistics = null;
    }

    @Override // com.android.server.utils.Watcher
    public final void onChange(Watchable what) {
        if (this.mSealed) {
            throw new IllegalStateException("attempt to change a sealed object");
        }
        this.mSnapshot = null;
    }

    public final void seal() {
        this.mSealed = true;
    }

    public final T snapshot() {
        T s = this.mSnapshot;
        if (s == null) {
            s = createSnapshot();
            this.mSnapshot = s;
            Statistics statistics = this.mStatistics;
            if (statistics != null) {
                statistics.mRebuilt.incrementAndGet();
            }
        } else {
            Statistics statistics2 = this.mStatistics;
            if (statistics2 != null) {
                statistics2.mReused.incrementAndGet();
            }
        }
        return s;
    }

    /* loaded from: classes2.dex */
    public static class Sealed<T> extends SnapshotCache<T> {
        @Override // com.android.server.utils.SnapshotCache
        public T createSnapshot() {
            throw new UnsupportedOperationException("cannot snapshot a sealed snaphot");
        }
    }

    /* loaded from: classes2.dex */
    public static class Auto<T extends Snappable<T>> extends SnapshotCache<T> {
        public Auto(T source, Watchable watchable, String name) {
            super(source, watchable, name);
        }

        public Auto(T source, Watchable watchable) {
            this(source, watchable, null);
        }

        /* JADX DEBUG: Method merged with bridge method */
        @Override // com.android.server.utils.SnapshotCache
        public T createSnapshot() {
            return (T) ((Snappable) this.mSource).snapshot();
        }
    }
}
