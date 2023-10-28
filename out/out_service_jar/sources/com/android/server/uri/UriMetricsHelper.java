package com.android.server.uri;

import android.app.StatsManager;
import android.content.Context;
import android.util.SparseArray;
import android.util.StatsEvent;
import com.android.internal.util.ConcurrentUtils;
import com.android.internal.util.FrameworkStatsLog;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
/* JADX INFO: Access modifiers changed from: package-private */
/* loaded from: classes2.dex */
public final class UriMetricsHelper {
    private static final StatsManager.PullAtomMetadata DAILY_PULL_METADATA = new StatsManager.PullAtomMetadata.Builder().setCoolDownMillis(TimeUnit.DAYS.toMillis(1)).build();
    private final Context mContext;
    private final PersistentUriGrantsProvider mPersistentUriGrantsProvider;

    /* JADX INFO: Access modifiers changed from: package-private */
    /* loaded from: classes2.dex */
    public interface PersistentUriGrantsProvider {
        ArrayList<UriPermission> providePersistentUriGrants();
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public UriMetricsHelper(Context context, PersistentUriGrantsProvider provider) {
        this.mContext = context;
        this.mPersistentUriGrantsProvider = provider;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void registerPuller() {
        StatsManager statsManager = (StatsManager) this.mContext.getSystemService(StatsManager.class);
        statsManager.setPullAtomCallback((int) FrameworkStatsLog.PERSISTENT_URI_PERMISSIONS_AMOUNT_PER_PACKAGE, DAILY_PULL_METADATA, ConcurrentUtils.DIRECT_EXECUTOR, new StatsManager.StatsPullAtomCallback() { // from class: com.android.server.uri.UriMetricsHelper$$ExternalSyntheticLambda0
            public final int onPullAtom(int i, List list) {
                return UriMetricsHelper.this.m7275lambda$registerPuller$0$comandroidserveruriUriMetricsHelper(i, list);
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$registerPuller$0$com-android-server-uri-UriMetricsHelper  reason: not valid java name */
    public /* synthetic */ int m7275lambda$registerPuller$0$comandroidserveruriUriMetricsHelper(int atomTag, List data) {
        reportPersistentUriPermissionsPerPackage(data);
        return 0;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void reportPersistentUriFlushed(int amount) {
        FrameworkStatsLog.write((int) FrameworkStatsLog.PERSISTENT_URI_PERMISSIONS_FLUSHED, amount);
    }

    private void reportPersistentUriPermissionsPerPackage(List<StatsEvent> data) {
        ArrayList<UriPermission> persistentUriGrants = this.mPersistentUriGrantsProvider.providePersistentUriGrants();
        SparseArray<Integer> perUidCount = new SparseArray<>();
        int persistentUriGrantsSize = persistentUriGrants.size();
        for (int i = 0; i < persistentUriGrantsSize; i++) {
            UriPermission uriPermission = persistentUriGrants.get(i);
            perUidCount.put(uriPermission.targetUid, Integer.valueOf(perUidCount.get(uriPermission.targetUid, 0).intValue() + 1));
        }
        int perUidCountSize = perUidCount.size();
        for (int i2 = 0; i2 < perUidCountSize; i2++) {
            int uid = perUidCount.keyAt(i2);
            int amount = perUidCount.valueAt(i2).intValue();
            data.add(FrameworkStatsLog.buildStatsEvent((int) FrameworkStatsLog.PERSISTENT_URI_PERMISSIONS_AMOUNT_PER_PACKAGE, uid, amount));
        }
    }
}
