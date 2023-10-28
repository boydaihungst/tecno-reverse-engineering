package com.android.server.pm;

import android.content.IntentFilter;
import com.android.server.utils.Snappable;
import com.android.server.utils.SnapshotCache;
import java.io.PrintWriter;
import java.util.ArrayList;
/* loaded from: classes2.dex */
public class PreferredIntentResolver extends WatchedIntentResolver<PreferredActivity, PreferredActivity> implements Snappable {
    final SnapshotCache<PreferredIntentResolver> mSnapshot;

    /* JADX DEBUG: Method merged with bridge method */
    /* JADX INFO: Access modifiers changed from: protected */
    @Override // com.android.server.IntentResolver
    public PreferredActivity[] newArray(int size) {
        return new PreferredActivity[size];
    }

    /* JADX DEBUG: Method merged with bridge method */
    /* JADX INFO: Access modifiers changed from: protected */
    @Override // com.android.server.IntentResolver
    public boolean isPackageForFilter(String packageName, PreferredActivity filter) {
        return packageName.equals(filter.mPref.mComponent.getPackageName());
    }

    /* JADX DEBUG: Method merged with bridge method */
    /* JADX INFO: Access modifiers changed from: protected */
    @Override // com.android.server.IntentResolver
    public void dumpFilter(PrintWriter out, String prefix, PreferredActivity filter) {
        filter.mPref.dump(out, prefix, filter);
    }

    /* JADX DEBUG: Method merged with bridge method */
    /* JADX INFO: Access modifiers changed from: protected */
    @Override // com.android.server.IntentResolver
    public IntentFilter getIntentFilter(PreferredActivity input) {
        return input.getIntentFilter();
    }

    public boolean shouldAddPreferredActivity(PreferredActivity pa) {
        ArrayList<PreferredActivity> pal = findFilters(pa);
        if (pal == null || pal.isEmpty()) {
            return true;
        }
        if (pa.mPref.mAlways) {
            int activityCount = pal.size();
            for (int i = 0; i < activityCount; i++) {
                PreferredActivity cur = pal.get(i);
                if (cur.mPref.mAlways && cur.mPref.mMatch == (pa.mPref.mMatch & 268369920) && cur.mPref.sameSet(pa.mPref)) {
                    return false;
                }
            }
            return true;
        }
        return false;
    }

    public PreferredIntentResolver() {
        this.mSnapshot = makeCache();
    }

    /* JADX DEBUG: Method merged with bridge method */
    /* JADX INFO: Access modifiers changed from: protected */
    @Override // com.android.server.IntentResolver
    public PreferredActivity snapshot(PreferredActivity f) {
        if (f == null) {
            return null;
        }
        return f.snapshot();
    }

    private PreferredIntentResolver(PreferredIntentResolver f) {
        copyFrom((WatchedIntentResolver) f);
        this.mSnapshot = new SnapshotCache.Sealed();
    }

    private SnapshotCache makeCache() {
        return new SnapshotCache<PreferredIntentResolver>(this, this) { // from class: com.android.server.pm.PreferredIntentResolver.1
            /* JADX DEBUG: Method merged with bridge method */
            /* JADX WARN: Can't rename method to resolve collision */
            @Override // com.android.server.utils.SnapshotCache
            public PreferredIntentResolver createSnapshot() {
                return new PreferredIntentResolver();
            }
        };
    }

    /* JADX DEBUG: Method merged with bridge method */
    @Override // com.android.server.utils.Snappable
    public PreferredIntentResolver snapshot() {
        return this.mSnapshot.snapshot();
    }
}
