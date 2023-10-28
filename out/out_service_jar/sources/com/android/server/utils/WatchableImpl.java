package com.android.server.utils;

import java.util.ArrayList;
import java.util.Objects;
/* loaded from: classes2.dex */
public class WatchableImpl implements Watchable {
    protected final ArrayList<Watcher> mObservers = new ArrayList<>();
    private boolean mSealed = false;

    @Override // com.android.server.utils.Watchable
    public void registerObserver(Watcher observer) {
        Objects.requireNonNull(observer, "observer may not be null");
        synchronized (this.mObservers) {
            if (!this.mObservers.contains(observer)) {
                this.mObservers.add(observer);
            }
        }
    }

    @Override // com.android.server.utils.Watchable
    public void unregisterObserver(Watcher observer) {
        Objects.requireNonNull(observer, "observer may not be null");
        synchronized (this.mObservers) {
            this.mObservers.remove(observer);
        }
    }

    @Override // com.android.server.utils.Watchable
    public boolean isRegisteredObserver(Watcher observer) {
        boolean contains;
        synchronized (this.mObservers) {
            contains = this.mObservers.contains(observer);
        }
        return contains;
    }

    public int registeredObserverCount() {
        return this.mObservers.size();
    }

    @Override // com.android.server.utils.Watchable
    public void dispatchChange(Watchable what) {
        synchronized (this.mObservers) {
            if (this.mSealed) {
                throw new IllegalStateException("attempt to change a sealed object");
            }
            int end = this.mObservers.size();
            for (int i = 0; i < end; i++) {
                this.mObservers.get(i).onChange(what);
            }
        }
    }

    public void seal() {
        synchronized (this.mObservers) {
            this.mSealed = true;
        }
    }

    public boolean isSealed() {
        boolean z;
        synchronized (this.mObservers) {
            z = this.mSealed;
        }
        return z;
    }
}
