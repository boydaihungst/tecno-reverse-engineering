package com.android.server.pm;

import android.content.IntentFilter;
import com.android.server.utils.Snappable;
import com.android.server.utils.SnapshotCache;
/* loaded from: classes2.dex */
public class PersistentPreferredIntentResolver extends WatchedIntentResolver<PersistentPreferredActivity, PersistentPreferredActivity> implements Snappable {
    final SnapshotCache<PersistentPreferredIntentResolver> mSnapshot;

    /* JADX DEBUG: Method merged with bridge method */
    /* JADX INFO: Access modifiers changed from: protected */
    @Override // com.android.server.IntentResolver
    public PersistentPreferredActivity[] newArray(int size) {
        return new PersistentPreferredActivity[size];
    }

    /* JADX DEBUG: Method merged with bridge method */
    /* JADX INFO: Access modifiers changed from: protected */
    @Override // com.android.server.IntentResolver
    public IntentFilter getIntentFilter(PersistentPreferredActivity input) {
        return input.getIntentFilter();
    }

    /* JADX DEBUG: Method merged with bridge method */
    /* JADX INFO: Access modifiers changed from: protected */
    @Override // com.android.server.IntentResolver
    public boolean isPackageForFilter(String packageName, PersistentPreferredActivity filter) {
        return packageName.equals(filter.mComponent.getPackageName());
    }

    public PersistentPreferredIntentResolver() {
        this.mSnapshot = makeCache();
    }

    /* JADX DEBUG: Method merged with bridge method */
    /* JADX INFO: Access modifiers changed from: protected */
    @Override // com.android.server.IntentResolver
    public PersistentPreferredActivity snapshot(PersistentPreferredActivity f) {
        if (f == null) {
            return null;
        }
        return f.snapshot();
    }

    private PersistentPreferredIntentResolver(PersistentPreferredIntentResolver f) {
        copyFrom((WatchedIntentResolver) f);
        this.mSnapshot = new SnapshotCache.Sealed();
    }

    private SnapshotCache makeCache() {
        return new SnapshotCache<PersistentPreferredIntentResolver>(this, this) { // from class: com.android.server.pm.PersistentPreferredIntentResolver.1
            /* JADX DEBUG: Method merged with bridge method */
            /* JADX WARN: Can't rename method to resolve collision */
            @Override // com.android.server.utils.SnapshotCache
            public PersistentPreferredIntentResolver createSnapshot() {
                return new PersistentPreferredIntentResolver();
            }
        };
    }

    /* JADX DEBUG: Method merged with bridge method */
    @Override // com.android.server.utils.Snappable
    public PersistentPreferredIntentResolver snapshot() {
        return this.mSnapshot.snapshot();
    }
}
