package com.android.server.pm;

import android.content.IntentFilter;
import com.android.server.utils.Snappable;
import com.android.server.utils.SnapshotCache;
import java.util.List;
/* JADX INFO: Access modifiers changed from: package-private */
/* loaded from: classes2.dex */
public class CrossProfileIntentResolver extends WatchedIntentResolver<CrossProfileIntentFilter, CrossProfileIntentFilter> implements Snappable {
    final SnapshotCache<CrossProfileIntentResolver> mSnapshot;

    /* JADX DEBUG: Method merged with bridge method */
    /* JADX INFO: Access modifiers changed from: protected */
    @Override // com.android.server.IntentResolver
    public CrossProfileIntentFilter[] newArray(int size) {
        return new CrossProfileIntentFilter[size];
    }

    /* JADX DEBUG: Method merged with bridge method */
    /* JADX INFO: Access modifiers changed from: protected */
    @Override // com.android.server.IntentResolver
    public boolean isPackageForFilter(String packageName, CrossProfileIntentFilter filter) {
        return false;
    }

    @Override // com.android.server.pm.WatchedIntentResolver, com.android.server.IntentResolver
    protected void sortResults(List<CrossProfileIntentFilter> results) {
    }

    /* JADX DEBUG: Method merged with bridge method */
    /* JADX INFO: Access modifiers changed from: protected */
    @Override // com.android.server.IntentResolver
    public IntentFilter getIntentFilter(CrossProfileIntentFilter input) {
        return input.getIntentFilter();
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public CrossProfileIntentResolver() {
        this.mSnapshot = makeCache();
    }

    /* JADX DEBUG: Method merged with bridge method */
    /* JADX INFO: Access modifiers changed from: protected */
    @Override // com.android.server.IntentResolver
    public CrossProfileIntentFilter snapshot(CrossProfileIntentFilter f) {
        if (f == null) {
            return null;
        }
        return f.snapshot();
    }

    private CrossProfileIntentResolver(CrossProfileIntentResolver f) {
        copyFrom((WatchedIntentResolver) f);
        this.mSnapshot = new SnapshotCache.Sealed();
    }

    private SnapshotCache makeCache() {
        return new SnapshotCache<CrossProfileIntentResolver>(this, this) { // from class: com.android.server.pm.CrossProfileIntentResolver.1
            /* JADX DEBUG: Method merged with bridge method */
            /* JADX WARN: Can't rename method to resolve collision */
            @Override // com.android.server.utils.SnapshotCache
            public CrossProfileIntentResolver createSnapshot() {
                return new CrossProfileIntentResolver();
            }
        };
    }

    /* JADX DEBUG: Method merged with bridge method */
    @Override // com.android.server.utils.Snappable
    public CrossProfileIntentResolver snapshot() {
        return this.mSnapshot.snapshot();
    }
}
