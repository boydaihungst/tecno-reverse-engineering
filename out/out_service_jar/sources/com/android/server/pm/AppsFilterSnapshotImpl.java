package com.android.server.pm;

import com.android.server.utils.SnapshotCache;
import com.android.server.utils.WatchedSparseBooleanMatrix;
import java.util.Arrays;
/* loaded from: classes2.dex */
public final class AppsFilterSnapshotImpl extends AppsFilterBase {
    /* JADX INFO: Access modifiers changed from: package-private */
    public AppsFilterSnapshotImpl(AppsFilterImpl orig) {
        synchronized (orig.mImplicitlyQueryableLock) {
            this.mImplicitlyQueryable = orig.mImplicitQueryableSnapshot.snapshot();
            this.mRetainedImplicitlyQueryable = orig.mRetainedImplicitlyQueryableSnapshot.snapshot();
        }
        this.mImplicitQueryableSnapshot = new SnapshotCache.Sealed();
        this.mRetainedImplicitlyQueryableSnapshot = new SnapshotCache.Sealed();
        synchronized (orig.mQueriesViaPackageLock) {
            this.mQueriesViaPackage = orig.mQueriesViaPackageSnapshot.snapshot();
        }
        this.mQueriesViaPackageSnapshot = new SnapshotCache.Sealed();
        synchronized (orig.mQueriesViaComponentLock) {
            this.mQueriesViaComponent = orig.mQueriesViaComponentSnapshot.snapshot();
        }
        this.mQueriesViaComponentSnapshot = new SnapshotCache.Sealed();
        synchronized (orig.mQueryableViaUsesLibraryLock) {
            this.mQueryableViaUsesLibrary = orig.mQueryableViaUsesLibrarySnapshot.snapshot();
        }
        this.mQueryableViaUsesLibrarySnapshot = new SnapshotCache.Sealed();
        synchronized (orig.mForceQueryableLock) {
            this.mForceQueryable = orig.mForceQueryableSnapshot.snapshot();
        }
        this.mForceQueryableSnapshot = new SnapshotCache.Sealed();
        synchronized (orig.mProtectedBroadcastsLock) {
            this.mProtectedBroadcasts = orig.mProtectedBroadcastsSnapshot.snapshot();
        }
        this.mProtectedBroadcastsSnapshot = new SnapshotCache.Sealed();
        this.mQueriesViaComponentRequireRecompute = orig.mQueriesViaComponentRequireRecompute;
        this.mForceQueryableByDevicePackageNames = (String[]) Arrays.copyOf(orig.mForceQueryableByDevicePackageNames, orig.mForceQueryableByDevicePackageNames.length);
        this.mSystemAppsQueryable = orig.mSystemAppsQueryable;
        this.mFeatureConfig = orig.mFeatureConfig.snapshot();
        this.mOverlayReferenceMapper = orig.mOverlayReferenceMapper;
        this.mSystemSigningDetails = orig.mSystemSigningDetails;
        this.mCacheReady = orig.mCacheReady;
        if (this.mCacheReady) {
            synchronized (orig.mCacheLock) {
                this.mShouldFilterCache = orig.mShouldFilterCacheSnapshot.snapshot();
            }
        } else {
            this.mShouldFilterCache = new WatchedSparseBooleanMatrix();
        }
        this.mShouldFilterCacheSnapshot = new SnapshotCache.Sealed();
        this.mBackgroundHandler = null;
    }
}
