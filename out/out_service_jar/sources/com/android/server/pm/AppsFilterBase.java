package com.android.server.pm;

import android.content.pm.SigningDetails;
import android.os.Binder;
import android.os.Handler;
import android.os.Process;
import android.os.Trace;
import android.os.UserHandle;
import android.text.TextUtils;
import android.util.ArrayMap;
import android.util.ArraySet;
import android.util.Slog;
import android.util.SparseArray;
import com.android.internal.util.function.QuadFunction;
import com.android.server.am.HostingRecord;
import com.android.server.om.OverlayReferenceMapper;
import com.android.server.pm.parsing.pkg.AndroidPackage;
import com.android.server.pm.pkg.PackageStateInternal;
import com.android.server.pm.snapshot.PackageDataSnapshot;
import com.android.server.utils.SnapshotCache;
import com.android.server.utils.Watched;
import com.android.server.utils.WatchedArrayList;
import com.android.server.utils.WatchedArrayMap;
import com.android.server.utils.WatchedArraySet;
import com.android.server.utils.WatchedSparseBooleanMatrix;
import com.android.server.utils.WatchedSparseSetArray;
import java.io.PrintWriter;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;
/* loaded from: classes2.dex */
public abstract class AppsFilterBase implements AppsFilterSnapshot {
    protected static final boolean CACHE_INVALID = false;
    protected static final int CACHE_REBUILD_DELAY_MAX_MS = 10000;
    protected static final int CACHE_REBUILD_DELAY_MIN_MS = 10000;
    protected static final boolean CACHE_VALID = true;
    protected static final boolean DEBUG_ALLOW_ALL = false;
    public static boolean DEBUG_LOGGING = false;
    public static boolean DEBUG_TRACING = false;
    protected static final String TAG = "AppsFilter";
    protected Handler mBackgroundHandler;
    protected FeatureConfig mFeatureConfig;
    @Watched
    protected WatchedArraySet<Integer> mForceQueryable;
    protected String[] mForceQueryableByDevicePackageNames;
    protected SnapshotCache<WatchedArraySet<Integer>> mForceQueryableSnapshot;
    protected SnapshotCache<WatchedSparseSetArray<Integer>> mImplicitQueryableSnapshot;
    @Watched
    protected WatchedSparseSetArray<Integer> mImplicitlyQueryable;
    protected OverlayReferenceMapper mOverlayReferenceMapper;
    @Watched
    protected WatchedArrayList<String> mProtectedBroadcasts;
    protected SnapshotCache<WatchedArrayList<String>> mProtectedBroadcastsSnapshot;
    @Watched
    protected WatchedSparseSetArray<Integer> mQueriesViaComponent;
    protected SnapshotCache<WatchedSparseSetArray<Integer>> mQueriesViaComponentSnapshot;
    @Watched
    protected WatchedSparseSetArray<Integer> mQueriesViaPackage;
    protected SnapshotCache<WatchedSparseSetArray<Integer>> mQueriesViaPackageSnapshot;
    @Watched
    protected WatchedSparseSetArray<Integer> mQueryableViaUsesLibrary;
    protected SnapshotCache<WatchedSparseSetArray<Integer>> mQueryableViaUsesLibrarySnapshot;
    @Watched
    protected WatchedSparseSetArray<Integer> mRetainedImplicitlyQueryable;
    protected SnapshotCache<WatchedSparseSetArray<Integer>> mRetainedImplicitlyQueryableSnapshot;
    @Watched
    protected WatchedSparseBooleanMatrix mShouldFilterCache;
    protected SnapshotCache<WatchedSparseBooleanMatrix> mShouldFilterCacheSnapshot;
    protected boolean mSystemAppsQueryable;
    protected SigningDetails mSystemSigningDetails;
    protected AtomicBoolean mQueriesViaComponentRequireRecompute = new AtomicBoolean(false);
    protected volatile boolean mCacheReady = false;
    protected AtomicBoolean mCacheValid = new AtomicBoolean(false);

    /* JADX INFO: Access modifiers changed from: protected */
    /* loaded from: classes2.dex */
    public interface ToString<T> {
        String toString(T t);
    }

    /* JADX INFO: Access modifiers changed from: protected */
    public boolean isForceQueryable(int callingAppId) {
        return this.mForceQueryable.contains(Integer.valueOf(callingAppId));
    }

    /* JADX INFO: Access modifiers changed from: protected */
    public boolean isQueryableViaPackage(int callingAppId, int targetAppId) {
        return this.mQueriesViaPackage.contains(callingAppId, Integer.valueOf(targetAppId));
    }

    /* JADX INFO: Access modifiers changed from: protected */
    public boolean isQueryableViaComponent(int callingAppId, int targetAppId) {
        return this.mQueriesViaComponent.contains(callingAppId, Integer.valueOf(targetAppId));
    }

    /* JADX INFO: Access modifiers changed from: protected */
    public boolean isImplicitlyQueryable(int callingUid, int targetUid) {
        return this.mImplicitlyQueryable.contains(callingUid, Integer.valueOf(targetUid));
    }

    /* JADX INFO: Access modifiers changed from: protected */
    public boolean isRetainedImplicitlyQueryable(int callingUid, int targetUid) {
        return this.mRetainedImplicitlyQueryable.contains(callingUid, Integer.valueOf(targetUid));
    }

    /* JADX INFO: Access modifiers changed from: protected */
    public boolean isQueryableViaUsesLibrary(int callingAppId, int targetAppId) {
        return this.mQueryableViaUsesLibrary.contains(callingAppId, Integer.valueOf(targetAppId));
    }

    protected boolean isQueryableViaComponentWhenRequireRecompute(ArrayMap<String, ? extends PackageStateInternal> existingSettings, PackageStateInternal callingPkgSetting, ArraySet<PackageStateInternal> callingSharedPkgSettings, AndroidPackage targetPkg, int callingAppId, int targetAppId) {
        if (callingPkgSetting == null) {
            for (int i = callingSharedPkgSettings.size() - 1; i >= 0; i--) {
                AndroidPackage pkg = callingSharedPkgSettings.valueAt(i).getPkg();
                if (pkg != null && AppsFilterUtils.canQueryViaComponents(pkg, targetPkg, this.mProtectedBroadcasts)) {
                    return true;
                }
            }
            return false;
        } else if (callingPkgSetting.getPkg() != null && AppsFilterUtils.canQueryViaComponents(callingPkgSetting.getPkg(), targetPkg, this.mProtectedBroadcasts)) {
            return true;
        } else {
            return false;
        }
    }

    @Override // com.android.server.pm.AppsFilterSnapshot
    public SparseArray<int[]> getVisibilityAllowList(PackageDataSnapshot snapshot, PackageStateInternal setting, int[] users, ArrayMap<String, ? extends PackageStateInternal> existingSettings) {
        int loc;
        int[] iArr = users;
        if (isForceQueryable(setting.getAppId())) {
            return null;
        }
        SparseArray<int[]> result = new SparseArray<>(iArr.length);
        int u = 0;
        while (u < iArr.length) {
            int userId = iArr[u];
            int[] appIds = new int[existingSettings.size()];
            int[] buffer = null;
            int allowListSize = 0;
            for (int i = existingSettings.size() - 1; i >= 0; i--) {
                PackageStateInternal existingSetting = existingSettings.valueAt(i);
                int existingAppId = existingSetting.getAppId();
                if (existingAppId >= 10000 && (loc = Arrays.binarySearch(appIds, 0, allowListSize, existingAppId)) < 0) {
                    int existingUid = UserHandle.getUid(userId, existingAppId);
                    if (!shouldFilterApplication(snapshot, existingUid, existingSetting, setting, userId)) {
                        if (buffer == null) {
                            buffer = new int[appIds.length];
                        }
                        int insert = ~loc;
                        System.arraycopy(appIds, insert, buffer, 0, allowListSize - insert);
                        appIds[insert] = existingAppId;
                        System.arraycopy(buffer, 0, appIds, insert + 1, allowListSize - insert);
                        allowListSize++;
                    }
                }
            }
            result.put(userId, Arrays.copyOf(appIds, allowListSize));
            u++;
            iArr = users;
        }
        return result;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public SparseArray<int[]> getVisibilityAllowList(PackageDataSnapshot snapshot, PackageStateInternal setting, int[] users, WatchedArrayMap<String, ? extends PackageStateInternal> existingSettings) {
        return getVisibilityAllowList(snapshot, setting, users, existingSettings.untrackedStorage());
    }

    /* JADX DEBUG: Another duplicated slice has different insns count: {[SGET]}, finally: {[SGET, INVOKE, IF] complete} */
    /* JADX DEBUG: Don't trust debug lines info. Repeating lines: [348=6, 349=6] */
    @Override // com.android.server.pm.AppsFilterSnapshot
    public boolean shouldFilterApplication(PackageDataSnapshot snapshot, int callingUid, Object callingSetting, PackageStateInternal targetPkgSetting, int userId) {
        if (DEBUG_TRACING) {
            Trace.traceBegin(262144L, "shouldFilterApplication");
        }
        try {
            int callingAppId = UserHandle.getAppId(callingUid);
            boolean z = false;
            if (callingAppId >= 10000 && targetPkgSetting.getAppId() >= 10000 && callingAppId != targetPkgSetting.getAppId()) {
                if (Process.isSdkSandboxUid(callingAppId)) {
                    int targetAppId = targetPkgSetting.getAppId();
                    int targetUid = UserHandle.getUid(userId, targetAppId);
                    if (!isForceQueryable(targetPkgSetting.getAppId()) && !isImplicitlyQueryable(callingUid, targetUid)) {
                        z = true;
                    }
                    return z;
                }
                if (this.mCacheReady) {
                    if (!shouldFilterApplicationUsingCache(callingUid, targetPkgSetting.getAppId(), userId)) {
                        if (DEBUG_TRACING) {
                            Trace.traceEnd(262144L);
                        }
                        return false;
                    }
                } else if (!shouldFilterApplicationInternal(snapshot, callingUid, callingSetting, targetPkgSetting, userId)) {
                    if (DEBUG_TRACING) {
                        Trace.traceEnd(262144L);
                    }
                    return false;
                }
                if (DEBUG_LOGGING || this.mFeatureConfig.isLoggingEnabled(callingAppId)) {
                    log(callingSetting, targetPkgSetting, "BLOCKED");
                }
                if (DEBUG_TRACING) {
                    Trace.traceEnd(262144L);
                }
                return true;
            }
            if (DEBUG_TRACING) {
                Trace.traceEnd(262144L);
            }
            return false;
        } finally {
            if (DEBUG_TRACING) {
                Trace.traceEnd(262144L);
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: protected */
    public boolean shouldFilterApplicationUsingCache(int callingUid, int appId, int userId) {
        int callingIndex = this.mShouldFilterCache.indexOfKey(callingUid);
        if (callingIndex < 0) {
            Slog.wtf(TAG, "Encountered calling uid with no cached rules: " + callingUid);
            return true;
        }
        int targetUid = UserHandle.getUid(userId, appId);
        int targetIndex = this.mShouldFilterCache.indexOfKey(targetUid);
        if (targetIndex < 0) {
            Slog.w(TAG, "Encountered calling -> target with no cached rules: " + callingUid + " -> " + targetUid);
            return true;
        }
        return this.mShouldFilterCache.valueAt(callingIndex, targetIndex);
    }

    /* JADX DEBUG: Another duplicated slice has different insns count: {[SGET]}, finally: {[SGET, CONST, INVOKE, IF] complete} */
    /* JADX DEBUG: Don't trust debug lines info. Repeating lines: [517=4, 543=5, 544=4, 560=4, 578=4, 612=7, 613=4, 628=4, 635=20, 636=20, 472=5, 473=4, 502=4] */
    /* JADX INFO: Access modifiers changed from: protected */
    /* JADX WARN: Removed duplicated region for block: B:342:0x0458 A[Catch: all -> 0x04e9, TryCatch #8 {all -> 0x04e9, blocks: (B:5:0x0017, B:8:0x0023, B:10:0x0027, B:17:0x0039, B:22:0x0058, B:24:0x005c, B:25:0x0061, B:27:0x006b, B:29:0x0074, B:33:0x0095, B:35:0x0099, B:38:0x00a0, B:40:0x00a6, B:42:0x00b2, B:44:0x00b6, B:63:0x00f1, B:65:0x00f5, B:67:0x00fc, B:69:0x010d, B:71:0x0116, B:73:0x011b, B:75:0x011f, B:90:0x0155, B:92:0x0159, B:112:0x0191, B:114:0x0195, B:115:0x0198, B:117:0x01a0, B:119:0x01a4, B:124:0x01b3, B:142:0x01e6, B:144:0x01ea, B:149:0x01f5, B:151:0x01f9, B:164:0x0221, B:166:0x0225, B:171:0x0230, B:173:0x0234, B:189:0x0264, B:191:0x0268, B:214:0x02bf, B:216:0x02c3, B:229:0x02f2, B:231:0x02f6, B:236:0x0307, B:238:0x030b, B:251:0x033b, B:253:0x033f, B:258:0x0350, B:260:0x0354, B:279:0x039f, B:281:0x03a3, B:349:0x0469, B:351:0x046d, B:353:0x0474, B:306:0x03f5, B:308:0x03f9, B:322:0x0424, B:324:0x0428, B:329:0x0439, B:331:0x043d, B:340:0x0454, B:342:0x0458, B:344:0x045f, B:299:0x03e0, B:301:0x03e4, B:356:0x047a, B:358:0x047e, B:360:0x0485, B:363:0x048b, B:365:0x048f, B:367:0x0496, B:205:0x02a3, B:207:0x02a7, B:372:0x04a4, B:374:0x04a8, B:376:0x04af, B:379:0x04b7, B:381:0x04bb, B:383:0x04c2, B:386:0x04ca, B:388:0x04ce, B:390:0x04d5, B:104:0x017f, B:106:0x0183, B:393:0x04dd, B:395:0x04e1, B:397:0x04e8, B:68:0x0102, B:49:0x00c2, B:51:0x00ca, B:53:0x00d6, B:55:0x00de, B:57:0x00e2, B:32:0x0089), top: B:419:0x0017 }] */
    /* JADX WARN: Removed duplicated region for block: B:351:0x046d A[Catch: all -> 0x04e9, TryCatch #8 {all -> 0x04e9, blocks: (B:5:0x0017, B:8:0x0023, B:10:0x0027, B:17:0x0039, B:22:0x0058, B:24:0x005c, B:25:0x0061, B:27:0x006b, B:29:0x0074, B:33:0x0095, B:35:0x0099, B:38:0x00a0, B:40:0x00a6, B:42:0x00b2, B:44:0x00b6, B:63:0x00f1, B:65:0x00f5, B:67:0x00fc, B:69:0x010d, B:71:0x0116, B:73:0x011b, B:75:0x011f, B:90:0x0155, B:92:0x0159, B:112:0x0191, B:114:0x0195, B:115:0x0198, B:117:0x01a0, B:119:0x01a4, B:124:0x01b3, B:142:0x01e6, B:144:0x01ea, B:149:0x01f5, B:151:0x01f9, B:164:0x0221, B:166:0x0225, B:171:0x0230, B:173:0x0234, B:189:0x0264, B:191:0x0268, B:214:0x02bf, B:216:0x02c3, B:229:0x02f2, B:231:0x02f6, B:236:0x0307, B:238:0x030b, B:251:0x033b, B:253:0x033f, B:258:0x0350, B:260:0x0354, B:279:0x039f, B:281:0x03a3, B:349:0x0469, B:351:0x046d, B:353:0x0474, B:306:0x03f5, B:308:0x03f9, B:322:0x0424, B:324:0x0428, B:329:0x0439, B:331:0x043d, B:340:0x0454, B:342:0x0458, B:344:0x045f, B:299:0x03e0, B:301:0x03e4, B:356:0x047a, B:358:0x047e, B:360:0x0485, B:363:0x048b, B:365:0x048f, B:367:0x0496, B:205:0x02a3, B:207:0x02a7, B:372:0x04a4, B:374:0x04a8, B:376:0x04af, B:379:0x04b7, B:381:0x04bb, B:383:0x04c2, B:386:0x04ca, B:388:0x04ce, B:390:0x04d5, B:104:0x017f, B:106:0x0183, B:393:0x04dd, B:395:0x04e1, B:397:0x04e8, B:68:0x0102, B:49:0x00c2, B:51:0x00ca, B:53:0x00d6, B:55:0x00de, B:57:0x00e2, B:32:0x0089), top: B:419:0x0017 }] */
    /* JADX WARN: Removed duplicated region for block: B:374:0x04a8 A[Catch: all -> 0x04e9, TryCatch #8 {all -> 0x04e9, blocks: (B:5:0x0017, B:8:0x0023, B:10:0x0027, B:17:0x0039, B:22:0x0058, B:24:0x005c, B:25:0x0061, B:27:0x006b, B:29:0x0074, B:33:0x0095, B:35:0x0099, B:38:0x00a0, B:40:0x00a6, B:42:0x00b2, B:44:0x00b6, B:63:0x00f1, B:65:0x00f5, B:67:0x00fc, B:69:0x010d, B:71:0x0116, B:73:0x011b, B:75:0x011f, B:90:0x0155, B:92:0x0159, B:112:0x0191, B:114:0x0195, B:115:0x0198, B:117:0x01a0, B:119:0x01a4, B:124:0x01b3, B:142:0x01e6, B:144:0x01ea, B:149:0x01f5, B:151:0x01f9, B:164:0x0221, B:166:0x0225, B:171:0x0230, B:173:0x0234, B:189:0x0264, B:191:0x0268, B:214:0x02bf, B:216:0x02c3, B:229:0x02f2, B:231:0x02f6, B:236:0x0307, B:238:0x030b, B:251:0x033b, B:253:0x033f, B:258:0x0350, B:260:0x0354, B:279:0x039f, B:281:0x03a3, B:349:0x0469, B:351:0x046d, B:353:0x0474, B:306:0x03f5, B:308:0x03f9, B:322:0x0424, B:324:0x0428, B:329:0x0439, B:331:0x043d, B:340:0x0454, B:342:0x0458, B:344:0x045f, B:299:0x03e0, B:301:0x03e4, B:356:0x047a, B:358:0x047e, B:360:0x0485, B:363:0x048b, B:365:0x048f, B:367:0x0496, B:205:0x02a3, B:207:0x02a7, B:372:0x04a4, B:374:0x04a8, B:376:0x04af, B:379:0x04b7, B:381:0x04bb, B:383:0x04c2, B:386:0x04ca, B:388:0x04ce, B:390:0x04d5, B:104:0x017f, B:106:0x0183, B:393:0x04dd, B:395:0x04e1, B:397:0x04e8, B:68:0x0102, B:49:0x00c2, B:51:0x00ca, B:53:0x00d6, B:55:0x00de, B:57:0x00e2, B:32:0x0089), top: B:419:0x0017 }] */
    /*
        Code decompiled incorrectly, please refer to instructions dump.
    */
    public boolean shouldFilterApplicationInternal(PackageDataSnapshot snapshot, int callingUid, Object callingSetting, PackageStateInternal targetPkgSetting, int targetUserId) {
        PackageStateInternal callingPkgSetting;
        int targetAppId;
        int callingAppId;
        PackageStateInternal callingPkgSetting2;
        ArraySet<PackageStateInternal> callingSharedPkgSettings;
        boolean z;
        PackageStateInternal callingPkgSetting3;
        if (DEBUG_TRACING) {
            Trace.traceBegin(262144L, "shouldFilterApplicationInternal");
        }
        try {
            boolean featureEnabled = this.mFeatureConfig.isGloballyEnabled();
            if (!featureEnabled) {
                if (DEBUG_LOGGING) {
                    Slog.d(TAG, "filtering disabled; skipped");
                }
                if (DEBUG_TRACING) {
                    Trace.traceEnd(262144L);
                }
                return false;
            } else if (callingSetting == null) {
                Slog.wtf(TAG, "No setting found for non system uid " + callingUid);
                if (DEBUG_TRACING) {
                    Trace.traceEnd(262144L);
                }
                return true;
            } else {
                if (DEBUG_TRACING) {
                    Trace.traceBegin(262144L, "callingSetting instanceof");
                }
                ArraySet<PackageStateInternal> callingSharedPkgSettings2 = new ArraySet<>();
                if (callingSetting instanceof PackageStateInternal) {
                    PackageStateInternal packageState = (PackageStateInternal) callingSetting;
                    if (packageState.hasSharedUser()) {
                        callingPkgSetting3 = null;
                        callingSharedPkgSettings2.addAll(getSharedUserPackages(packageState.getSharedUserAppId(), snapshot.getAllSharedUsers()));
                    } else {
                        callingPkgSetting3 = packageState;
                    }
                    callingPkgSetting = callingPkgSetting3;
                } else {
                    callingSharedPkgSettings2.addAll(((SharedUserSetting) callingSetting).getPackageStates());
                    callingPkgSetting = null;
                }
                if (DEBUG_TRACING) {
                    Trace.traceEnd(262144L);
                }
                if (callingPkgSetting == null) {
                    for (int i = callingSharedPkgSettings2.size() - 1; i >= 0; i--) {
                        AndroidPackage pkg = callingSharedPkgSettings2.valueAt(i).getPkg();
                        if (pkg != null && !this.mFeatureConfig.packageIsEnabled(pkg)) {
                            if (DEBUG_LOGGING) {
                                log(callingSetting, targetPkgSetting, "DISABLED");
                            }
                            if (DEBUG_TRACING) {
                                Trace.traceEnd(262144L);
                            }
                            return false;
                        }
                    }
                } else if (callingPkgSetting.getPkg() != null && !this.mFeatureConfig.packageIsEnabled(callingPkgSetting.getPkg())) {
                    if (DEBUG_LOGGING) {
                        log(callingSetting, targetPkgSetting, "DISABLED");
                    }
                    if (DEBUG_TRACING) {
                        Trace.traceEnd(262144L);
                    }
                    return false;
                }
                if (DEBUG_TRACING) {
                    Trace.traceBegin(262144L, "getAppId");
                }
                int callingAppId2 = callingPkgSetting != null ? callingPkgSetting.getAppId() : callingSharedPkgSettings2.valueAt(0).getAppId();
                int targetAppId2 = targetPkgSetting.getAppId();
                if (DEBUG_TRACING) {
                    Trace.traceEnd(262144L);
                }
                if (callingAppId2 == targetAppId2) {
                    if (DEBUG_LOGGING) {
                        log(callingSetting, targetPkgSetting, "same app id");
                    }
                    if (DEBUG_TRACING) {
                        Trace.traceEnd(262144L);
                    }
                    return false;
                }
                try {
                    if (DEBUG_TRACING) {
                        try {
                            Trace.traceBegin(262144L, "requestsQueryAllPackages");
                        } catch (Throwable th) {
                            th = th;
                            if (DEBUG_TRACING) {
                                Trace.traceEnd(262144L);
                            }
                            throw th;
                        }
                    }
                    if (callingPkgSetting == null) {
                        for (int i2 = callingSharedPkgSettings2.size() - 1; i2 >= 0; i2--) {
                            AndroidPackage pkg2 = callingSharedPkgSettings2.valueAt(i2).getPkg();
                            if (pkg2 != null && AppsFilterUtils.requestsQueryAllPackages(pkg2)) {
                                if (DEBUG_TRACING) {
                                    Trace.traceEnd(262144L);
                                }
                                if (DEBUG_TRACING) {
                                    Trace.traceEnd(262144L);
                                }
                                return false;
                            }
                        }
                    } else if (callingPkgSetting.getPkg() != null && AppsFilterUtils.requestsQueryAllPackages(callingPkgSetting.getPkg())) {
                        if (DEBUG_TRACING) {
                            Trace.traceEnd(262144L);
                        }
                        if (DEBUG_TRACING) {
                            Trace.traceEnd(262144L);
                        }
                        return false;
                    }
                    if (DEBUG_TRACING) {
                        Trace.traceEnd(262144L);
                    }
                    AndroidPackage targetPkg = targetPkgSetting.getPkg();
                    if (targetPkg == null) {
                        if (DEBUG_LOGGING) {
                            Slog.wtf(TAG, "shouldFilterApplication: targetPkg is null");
                        }
                        if (DEBUG_TRACING) {
                            Trace.traceEnd(262144L);
                        }
                        return true;
                    } else if (targetPkg.isStaticSharedLibrary()) {
                        if (DEBUG_TRACING) {
                            Trace.traceEnd(262144L);
                        }
                        return false;
                    } else {
                        try {
                            if (DEBUG_TRACING) {
                                try {
                                    Trace.traceBegin(262144L, "mForceQueryable");
                                } catch (Throwable th2) {
                                    th = th2;
                                    if (DEBUG_TRACING) {
                                        Trace.traceEnd(262144L);
                                    }
                                    throw th;
                                }
                            }
                            if (isForceQueryable(targetAppId2)) {
                                if (DEBUG_LOGGING) {
                                    log(callingSetting, targetPkgSetting, "force queryable");
                                }
                                if (DEBUG_TRACING) {
                                    Trace.traceEnd(262144L);
                                }
                                if (DEBUG_TRACING) {
                                    Trace.traceEnd(262144L);
                                }
                                return false;
                            }
                            if (DEBUG_TRACING) {
                                Trace.traceEnd(262144L);
                            }
                            try {
                                if (DEBUG_TRACING) {
                                    try {
                                        Trace.traceBegin(262144L, "mQueriesViaPackage");
                                    } catch (Throwable th3) {
                                        th = th3;
                                        if (DEBUG_TRACING) {
                                            Trace.traceEnd(262144L);
                                        }
                                        throw th;
                                    }
                                }
                                if (isQueryableViaPackage(callingAppId2, targetAppId2)) {
                                    if (DEBUG_LOGGING) {
                                        log(callingSetting, targetPkgSetting, "queries package");
                                    }
                                    if (DEBUG_TRACING) {
                                        Trace.traceEnd(262144L);
                                    }
                                    if (DEBUG_TRACING) {
                                        Trace.traceEnd(262144L);
                                    }
                                    return false;
                                }
                                if (DEBUG_TRACING) {
                                    Trace.traceEnd(262144L);
                                }
                                try {
                                    if (DEBUG_TRACING) {
                                        try {
                                            Trace.traceBegin(262144L, "mQueriesViaComponent");
                                        } catch (Throwable th4) {
                                            th = th4;
                                            if (DEBUG_TRACING) {
                                                Trace.traceEnd(262144L);
                                            }
                                            throw th;
                                        }
                                    }
                                    if (this.mQueriesViaComponentRequireRecompute.get()) {
                                        targetAppId = targetAppId2;
                                        callingAppId = callingAppId2;
                                        callingPkgSetting2 = callingPkgSetting;
                                        callingSharedPkgSettings = callingSharedPkgSettings2;
                                        z = false;
                                        try {
                                            if (isQueryableViaComponentWhenRequireRecompute(snapshot.getPackageStates(), callingPkgSetting, callingSharedPkgSettings2, targetPkg, callingAppId, targetAppId)) {
                                                try {
                                                    if (DEBUG_LOGGING) {
                                                        log(callingSetting, targetPkgSetting, "queries component");
                                                    }
                                                    if (DEBUG_TRACING) {
                                                        Trace.traceEnd(262144L);
                                                    }
                                                    return false;
                                                } catch (Throwable th5) {
                                                    th = th5;
                                                    if (DEBUG_TRACING) {
                                                    }
                                                    throw th;
                                                }
                                            }
                                        } catch (Throwable th6) {
                                            th = th6;
                                        }
                                    } else if (isQueryableViaComponent(callingAppId2, targetAppId2)) {
                                        if (DEBUG_LOGGING) {
                                            log(callingSetting, targetPkgSetting, "queries component");
                                        }
                                        if (DEBUG_TRACING) {
                                            Trace.traceEnd(262144L);
                                        }
                                        if (DEBUG_TRACING) {
                                            Trace.traceEnd(262144L);
                                        }
                                        return false;
                                    } else {
                                        targetAppId = targetAppId2;
                                        callingAppId = callingAppId2;
                                        callingSharedPkgSettings = callingSharedPkgSettings2;
                                        z = false;
                                        callingPkgSetting2 = callingPkgSetting;
                                    }
                                    if (DEBUG_TRACING) {
                                        Trace.traceEnd(262144L);
                                    }
                                    try {
                                        if (DEBUG_TRACING) {
                                            try {
                                                Trace.traceBegin(262144L, "mImplicitlyQueryable");
                                            } catch (Throwable th7) {
                                                th = th7;
                                                if (DEBUG_TRACING) {
                                                    Trace.traceEnd(262144L);
                                                }
                                                throw th;
                                            }
                                        }
                                        int targetUid = UserHandle.getUid(targetUserId, targetAppId);
                                        if (isImplicitlyQueryable(callingUid, targetUid)) {
                                            if (DEBUG_LOGGING) {
                                                log(callingSetting, targetPkgSetting, "implicitly queryable for user");
                                            }
                                            if (DEBUG_TRACING) {
                                                Trace.traceEnd(262144L);
                                            }
                                            if (DEBUG_TRACING) {
                                                Trace.traceEnd(262144L);
                                            }
                                            return z;
                                        }
                                        if (DEBUG_TRACING) {
                                            Trace.traceEnd(262144L);
                                        }
                                        try {
                                            if (DEBUG_TRACING) {
                                                try {
                                                    Trace.traceBegin(262144L, "mRetainedImplicitlyQueryable");
                                                } catch (Throwable th8) {
                                                    th = th8;
                                                    if (DEBUG_TRACING) {
                                                        Trace.traceEnd(262144L);
                                                    }
                                                    throw th;
                                                }
                                            }
                                            int targetUid2 = UserHandle.getUid(targetUserId, targetAppId);
                                            if (isRetainedImplicitlyQueryable(callingUid, targetUid2)) {
                                                if (DEBUG_LOGGING) {
                                                    log(callingSetting, targetPkgSetting, "retained implicitly queryable for user");
                                                }
                                                if (DEBUG_TRACING) {
                                                    Trace.traceEnd(262144L);
                                                }
                                                if (DEBUG_TRACING) {
                                                    Trace.traceEnd(262144L);
                                                }
                                                return z;
                                            }
                                            if (DEBUG_TRACING) {
                                                Trace.traceEnd(262144L);
                                            }
                                            try {
                                                if (DEBUG_TRACING) {
                                                    try {
                                                        Trace.traceBegin(262144L, "mOverlayReferenceMapper");
                                                    } catch (Throwable th9) {
                                                        th = th9;
                                                        if (DEBUG_TRACING) {
                                                        }
                                                        throw th;
                                                    }
                                                }
                                                String targetName = targetPkg.getPackageName();
                                                try {
                                                    if (callingSharedPkgSettings.isEmpty()) {
                                                        try {
                                                            if (this.mOverlayReferenceMapper.isValidActor(targetName, callingPkgSetting2.getPackageName())) {
                                                                if (DEBUG_LOGGING) {
                                                                    log(callingPkgSetting2, targetPkgSetting, "acts on target of overlay");
                                                                }
                                                                if (DEBUG_TRACING) {
                                                                    Trace.traceEnd(262144L);
                                                                }
                                                                if (DEBUG_TRACING) {
                                                                    Trace.traceEnd(262144L);
                                                                }
                                                                return z;
                                                            }
                                                        } catch (Throwable th10) {
                                                            th = th10;
                                                            if (DEBUG_TRACING) {
                                                                Trace.traceEnd(262144L);
                                                            }
                                                            throw th;
                                                        }
                                                    } else {
                                                        try {
                                                            int size = callingSharedPkgSettings.size();
                                                            int index = 0;
                                                            while (index < size) {
                                                                ArraySet<PackageStateInternal> callingSharedPkgSettings3 = callingSharedPkgSettings;
                                                                PackageStateInternal pkgSetting = callingSharedPkgSettings3.valueAt(index);
                                                                if (this.mOverlayReferenceMapper.isValidActor(targetName, pkgSetting.getPackageName())) {
                                                                    if (DEBUG_LOGGING) {
                                                                        log(callingPkgSetting2, targetPkgSetting, "matches shared user of package that acts on target of overlay");
                                                                    }
                                                                    if (DEBUG_TRACING) {
                                                                        Trace.traceEnd(262144L);
                                                                    }
                                                                    if (DEBUG_TRACING) {
                                                                        Trace.traceEnd(262144L);
                                                                    }
                                                                    return z;
                                                                }
                                                                index++;
                                                                callingSharedPkgSettings = callingSharedPkgSettings3;
                                                            }
                                                        } catch (Throwable th11) {
                                                            th = th11;
                                                            if (DEBUG_TRACING) {
                                                            }
                                                            throw th;
                                                        }
                                                    }
                                                    if (DEBUG_TRACING) {
                                                        Trace.traceEnd(262144L);
                                                    }
                                                    try {
                                                        if (DEBUG_TRACING) {
                                                            try {
                                                                Trace.traceBegin(262144L, "mQueryableViaUsesLibrary");
                                                            } catch (Throwable th12) {
                                                                th = th12;
                                                                if (DEBUG_TRACING) {
                                                                    Trace.traceEnd(262144L);
                                                                }
                                                                throw th;
                                                            }
                                                        }
                                                        try {
                                                            if (!isQueryableViaUsesLibrary(callingAppId, targetAppId)) {
                                                                if (DEBUG_TRACING) {
                                                                    Trace.traceEnd(262144L);
                                                                }
                                                                if (DEBUG_TRACING) {
                                                                    Trace.traceEnd(262144L);
                                                                }
                                                                return true;
                                                            }
                                                            if (DEBUG_LOGGING) {
                                                                log(callingSetting, targetPkgSetting, "queryable for library users");
                                                            }
                                                            if (DEBUG_TRACING) {
                                                                Trace.traceEnd(262144L);
                                                            }
                                                            if (DEBUG_TRACING) {
                                                                Trace.traceEnd(262144L);
                                                            }
                                                            return z;
                                                        } catch (Throwable th13) {
                                                            th = th13;
                                                            if (DEBUG_TRACING) {
                                                            }
                                                            throw th;
                                                        }
                                                    } catch (Throwable th14) {
                                                        th = th14;
                                                    }
                                                } catch (Throwable th15) {
                                                    th = th15;
                                                }
                                            } catch (Throwable th16) {
                                                th = th16;
                                            }
                                        } catch (Throwable th17) {
                                            th = th17;
                                        }
                                    } catch (Throwable th18) {
                                        th = th18;
                                    }
                                } catch (Throwable th19) {
                                    th = th19;
                                }
                            } catch (Throwable th20) {
                                th = th20;
                            }
                        } catch (Throwable th21) {
                            th = th21;
                        }
                    }
                } catch (Throwable th22) {
                    th = th22;
                }
            }
        } finally {
            if (DEBUG_TRACING) {
                Trace.traceEnd(262144L);
            }
        }
    }

    @Override // com.android.server.pm.AppsFilterSnapshot
    public boolean canQueryPackage(AndroidPackage querying, String potentialTarget) {
        int appId = UserHandle.getAppId(querying.getUid());
        if (appId >= 10000 && this.mFeatureConfig.packageIsEnabled(querying) && !AppsFilterUtils.requestsQueryAllPackages(querying)) {
            return !querying.getQueriesPackages().isEmpty() && querying.getQueriesPackages().contains(potentialTarget);
        }
        return true;
    }

    private static void log(Object callingSetting, PackageStateInternal targetPkgSetting, String description) {
        Slog.i(TAG, "interaction: " + (callingSetting == null ? HostingRecord.HOSTING_TYPE_SYSTEM : callingSetting) + " -> " + targetPkgSetting + " " + description);
    }

    /* JADX INFO: Access modifiers changed from: protected */
    public ArraySet<? extends PackageStateInternal> getSharedUserPackages(int sharedUserAppId, Collection<SharedUserSetting> sharedUserSettings) {
        for (SharedUserSetting setting : sharedUserSettings) {
            if (setting.mAppId == sharedUserAppId) {
                return setting.getPackageStates();
            }
        }
        return new ArraySet<>();
    }

    @Override // com.android.server.pm.AppsFilterSnapshot
    public void dumpQueries(PrintWriter pw, Integer filteringAppId, DumpState dumpState, final int[] users, final QuadFunction<Integer, Integer, Integer, Boolean, String[]> getPackagesForUid) {
        final SparseArray<String> cache = new SparseArray<>();
        ToString<Integer> expandPackages = new ToString() { // from class: com.android.server.pm.AppsFilterBase$$ExternalSyntheticLambda0
            @Override // com.android.server.pm.AppsFilterBase.ToString
            public final String toString(Object obj) {
                return AppsFilterBase.lambda$dumpQueries$0(cache, users, getPackagesForUid, (Integer) obj);
            }
        };
        pw.println();
        pw.println("Queries:");
        dumpState.onTitlePrinted();
        if (!this.mFeatureConfig.isGloballyEnabled()) {
            pw.println("  DISABLED");
            if (!DEBUG_LOGGING) {
                return;
            }
        }
        pw.println("  system apps queryable: " + this.mSystemAppsQueryable);
        dumpForceQueryable(pw, filteringAppId, expandPackages);
        dumpQueriesViaPackage(pw, filteringAppId, expandPackages);
        dumpQueriesViaComponent(pw, filteringAppId, expandPackages);
        dumpQueriesViaImplicitlyQueryable(pw, filteringAppId, users, expandPackages);
        dumpQueriesViaUsesLibrary(pw, filteringAppId, expandPackages);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static /* synthetic */ String lambda$dumpQueries$0(SparseArray cache, int[] users, QuadFunction getPackagesForUid, Integer input) {
        String cachedValue = (String) cache.get(input.intValue());
        if (cachedValue == null) {
            int callingUid = Binder.getCallingUid();
            int appId = UserHandle.getAppId(input.intValue());
            String[] packagesForUid = null;
            int size = users.length;
            for (int i = 0; packagesForUid == null && i < size; i++) {
                packagesForUid = (String[]) getPackagesForUid.apply(Integer.valueOf(callingUid), Integer.valueOf(users[i]), Integer.valueOf(appId), false);
            }
            if (packagesForUid == null) {
                cachedValue = "[app id " + input + " not installed]";
            } else {
                cachedValue = packagesForUid.length == 1 ? packagesForUid[0] : "[" + TextUtils.join(",", packagesForUid) + "]";
            }
            cache.put(input.intValue(), cachedValue);
        }
        return cachedValue;
    }

    /* JADX INFO: Access modifiers changed from: protected */
    public void dumpForceQueryable(PrintWriter pw, Integer filteringAppId, ToString<Integer> expandPackages) {
        pw.println("  queries via forceQueryable:");
        dumpPackageSet(pw, filteringAppId, this.mForceQueryable.untrackedStorage(), "forceQueryable", "  ", expandPackages);
    }

    /* JADX INFO: Access modifiers changed from: protected */
    public void dumpQueriesViaPackage(PrintWriter pw, Integer filteringAppId, ToString<Integer> expandPackages) {
        pw.println("  queries via package name:");
        dumpQueriesMap(pw, filteringAppId, this.mQueriesViaPackage, "    ", expandPackages);
    }

    /* JADX INFO: Access modifiers changed from: protected */
    public void dumpQueriesViaComponent(PrintWriter pw, Integer filteringAppId, ToString<Integer> expandPackages) {
        pw.println("  queries via component:");
        dumpQueriesMap(pw, filteringAppId, this.mQueriesViaComponent, "    ", expandPackages);
    }

    /* JADX INFO: Access modifiers changed from: protected */
    public void dumpQueriesViaImplicitlyQueryable(PrintWriter pw, Integer filteringAppId, int[] users, ToString<Integer> expandPackages) {
        pw.println("  queryable via interaction:");
        for (int user : users) {
            pw.append("    User ").append((CharSequence) Integer.toString(user)).println(":");
            Integer num = null;
            dumpQueriesMap(pw, filteringAppId == null ? null : Integer.valueOf(UserHandle.getUid(user, filteringAppId.intValue())), this.mImplicitlyQueryable, "      ", expandPackages);
            if (filteringAppId != null) {
                num = Integer.valueOf(UserHandle.getUid(user, filteringAppId.intValue()));
            }
            dumpQueriesMap(pw, num, this.mRetainedImplicitlyQueryable, "      ", expandPackages);
        }
    }

    /* JADX INFO: Access modifiers changed from: protected */
    public void dumpQueriesViaUsesLibrary(PrintWriter pw, Integer filteringAppId, ToString<Integer> expandPackages) {
        pw.println("  queryable via uses-library:");
        dumpQueriesMap(pw, filteringAppId, this.mQueryableViaUsesLibrary, "    ", expandPackages);
    }

    private static void dumpQueriesMap(PrintWriter pw, Integer filteringId, WatchedSparseSetArray<Integer> queriesMap, String spacing, ToString<Integer> toString) {
        String toString2;
        String toString3;
        for (int i = 0; i < queriesMap.size(); i++) {
            Integer callingId = Integer.valueOf(queriesMap.keyAt(i));
            if (Objects.equals(callingId, filteringId)) {
                ArraySet<Integer> arraySet = queriesMap.get(callingId.intValue());
                if (toString == null) {
                    toString3 = callingId.toString();
                } else {
                    toString3 = toString.toString(callingId);
                }
                dumpPackageSet(pw, null, arraySet, toString3, spacing, toString);
            } else {
                ArraySet<Integer> arraySet2 = queriesMap.get(callingId.intValue());
                if (toString == null) {
                    toString2 = callingId.toString();
                } else {
                    toString2 = toString.toString(callingId);
                }
                dumpPackageSet(pw, filteringId, arraySet2, toString2, spacing, toString);
            }
        }
    }

    private static <T> void dumpPackageSet(PrintWriter pw, T filteringId, ArraySet<T> targetPkgSet, String subTitle, String spacing, ToString<T> toString) {
        if (targetPkgSet != null && targetPkgSet.size() > 0) {
            if (filteringId == null || targetPkgSet.contains(filteringId)) {
                pw.append((CharSequence) spacing).append((CharSequence) subTitle).println(":");
                Iterator<T> it = targetPkgSet.iterator();
                while (it.hasNext()) {
                    T item = it.next();
                    if (filteringId == null || Objects.equals(filteringId, item)) {
                        pw.append((CharSequence) spacing).append("  ").println(toString == null ? item : toString.toString(item));
                    }
                }
            }
        }
    }
}
