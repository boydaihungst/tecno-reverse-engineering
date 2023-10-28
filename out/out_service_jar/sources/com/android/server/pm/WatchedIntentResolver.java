package com.android.server.pm;

import com.android.server.IntentResolver;
import com.android.server.pm.WatchedIntentFilter;
import com.android.server.pm.snapshot.PackageDataSnapshot;
import com.android.server.utils.Snappable;
import com.android.server.utils.Watchable;
import com.android.server.utils.WatchableImpl;
import com.android.server.utils.Watcher;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
/* loaded from: classes2.dex */
public abstract class WatchedIntentResolver<F extends WatchedIntentFilter, R extends WatchedIntentFilter> extends IntentResolver<F, R> implements Watchable, Snappable {
    private static final Comparator<WatchedIntentFilter> sResolvePrioritySorter = new Comparator<WatchedIntentFilter>() { // from class: com.android.server.pm.WatchedIntentResolver.2
        /* JADX DEBUG: Method merged with bridge method */
        @Override // java.util.Comparator
        public int compare(WatchedIntentFilter o1, WatchedIntentFilter o2) {
            int q1 = o1.getPriority();
            int q2 = o2.getPriority();
            if (q1 > q2) {
                return -1;
            }
            return q1 < q2 ? 1 : 0;
        }
    };
    private final Watchable mWatchable = new WatchableImpl();
    private final Watcher mWatcher = new Watcher() { // from class: com.android.server.pm.WatchedIntentResolver.1
        @Override // com.android.server.utils.Watcher
        public void onChange(Watchable what) {
            WatchedIntentResolver.this.dispatchChange(what);
        }
    };

    /* JADX DEBUG: Multi-variable search result rejected for r0v0, resolved type: com.android.server.pm.WatchedIntentResolver<F extends com.android.server.pm.WatchedIntentFilter, R extends com.android.server.pm.WatchedIntentFilter> */
    /* JADX WARN: Multi-variable type inference failed */
    @Override // com.android.server.IntentResolver
    public /* bridge */ /* synthetic */ void addFilter(PackageDataSnapshot packageDataSnapshot, Object obj) {
        addFilter(packageDataSnapshot, (PackageDataSnapshot) ((WatchedIntentFilter) obj));
    }

    /* JADX DEBUG: Multi-variable search result rejected for r0v0, resolved type: com.android.server.pm.WatchedIntentResolver<F extends com.android.server.pm.WatchedIntentFilter, R extends com.android.server.pm.WatchedIntentFilter> */
    /* JADX WARN: Multi-variable type inference failed */
    @Override // com.android.server.IntentResolver
    public /* bridge */ /* synthetic */ void removeFilter(Object obj) {
        removeFilter((WatchedIntentResolver<F, R>) ((WatchedIntentFilter) obj));
    }

    /* JADX DEBUG: Multi-variable search result rejected for r0v0, resolved type: com.android.server.pm.WatchedIntentResolver<F extends com.android.server.pm.WatchedIntentFilter, R extends com.android.server.pm.WatchedIntentFilter> */
    /* JADX INFO: Access modifiers changed from: protected */
    /* JADX WARN: Multi-variable type inference failed */
    @Override // com.android.server.IntentResolver
    public /* bridge */ /* synthetic */ void removeFilterInternal(Object obj) {
        removeFilterInternal((WatchedIntentResolver<F, R>) ((WatchedIntentFilter) obj));
    }

    @Override // com.android.server.utils.Watchable
    public void registerObserver(Watcher observer) {
        this.mWatchable.registerObserver(observer);
    }

    @Override // com.android.server.utils.Watchable
    public void unregisterObserver(Watcher observer) {
        this.mWatchable.unregisterObserver(observer);
    }

    @Override // com.android.server.utils.Watchable
    public boolean isRegisteredObserver(Watcher observer) {
        return this.mWatchable.isRegisteredObserver(observer);
    }

    @Override // com.android.server.utils.Watchable
    public void dispatchChange(Watchable what) {
        this.mWatchable.dispatchChange(what);
    }

    protected void onChanged() {
        dispatchChange(this);
    }

    public void addFilter(PackageDataSnapshot snapshot, F f) {
        super.addFilter(snapshot, (PackageDataSnapshot) f);
        f.registerObserver(this.mWatcher);
        onChanged();
    }

    public void removeFilter(F f) {
        f.unregisterObserver(this.mWatcher);
        super.removeFilter((WatchedIntentResolver<F, R>) f);
        onChanged();
    }

    protected void removeFilterInternal(F f) {
        f.unregisterObserver(this.mWatcher);
        super.removeFilterInternal((WatchedIntentResolver<F, R>) f);
        onChanged();
    }

    @Override // com.android.server.IntentResolver
    protected void sortResults(List<R> results) {
        Collections.sort(results, sResolvePrioritySorter);
    }

    public ArrayList<F> findFilters(WatchedIntentFilter matching) {
        return super.findFilters(matching.getIntentFilter());
    }

    /* JADX INFO: Access modifiers changed from: protected */
    public void copyFrom(WatchedIntentResolver orig) {
        super.copyFrom((IntentResolver) orig);
    }
}
