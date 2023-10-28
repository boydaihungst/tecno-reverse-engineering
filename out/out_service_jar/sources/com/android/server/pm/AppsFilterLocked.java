package com.android.server.pm;

import com.android.server.pm.AppsFilterBase;
import java.io.PrintWriter;
/* JADX INFO: Access modifiers changed from: package-private */
/* loaded from: classes2.dex */
public abstract class AppsFilterLocked extends AppsFilterBase {
    protected final Object mForceQueryableLock = new Object();
    protected final Object mQueriesViaPackageLock = new Object();
    protected final Object mQueriesViaComponentLock = new Object();
    protected final Object mImplicitlyQueryableLock = new Object();
    protected final Object mQueryableViaUsesLibraryLock = new Object();
    protected final Object mProtectedBroadcastsLock = new Object();
    protected final Object mCacheLock = new Object();

    /* JADX INFO: Access modifiers changed from: protected */
    @Override // com.android.server.pm.AppsFilterBase
    public boolean isForceQueryable(int appId) {
        boolean isForceQueryable;
        synchronized (this.mForceQueryableLock) {
            isForceQueryable = super.isForceQueryable(appId);
        }
        return isForceQueryable;
    }

    /* JADX INFO: Access modifiers changed from: protected */
    @Override // com.android.server.pm.AppsFilterBase
    public boolean isQueryableViaPackage(int callingAppId, int targetAppId) {
        boolean isQueryableViaPackage;
        synchronized (this.mQueriesViaPackageLock) {
            isQueryableViaPackage = super.isQueryableViaPackage(callingAppId, targetAppId);
        }
        return isQueryableViaPackage;
    }

    /* JADX INFO: Access modifiers changed from: protected */
    @Override // com.android.server.pm.AppsFilterBase
    public boolean isQueryableViaComponent(int callingAppId, int targetAppId) {
        boolean isQueryableViaComponent;
        synchronized (this.mQueriesViaComponentLock) {
            isQueryableViaComponent = super.isQueryableViaComponent(callingAppId, targetAppId);
        }
        return isQueryableViaComponent;
    }

    /* JADX INFO: Access modifiers changed from: protected */
    @Override // com.android.server.pm.AppsFilterBase
    public boolean isImplicitlyQueryable(int callingUid, int targetUid) {
        boolean isImplicitlyQueryable;
        synchronized (this.mImplicitlyQueryableLock) {
            isImplicitlyQueryable = super.isImplicitlyQueryable(callingUid, targetUid);
        }
        return isImplicitlyQueryable;
    }

    /* JADX INFO: Access modifiers changed from: protected */
    @Override // com.android.server.pm.AppsFilterBase
    public boolean isRetainedImplicitlyQueryable(int callingUid, int targetUid) {
        boolean isRetainedImplicitlyQueryable;
        synchronized (this.mImplicitlyQueryableLock) {
            isRetainedImplicitlyQueryable = super.isRetainedImplicitlyQueryable(callingUid, targetUid);
        }
        return isRetainedImplicitlyQueryable;
    }

    /* JADX INFO: Access modifiers changed from: protected */
    @Override // com.android.server.pm.AppsFilterBase
    public boolean isQueryableViaUsesLibrary(int callingAppId, int targetAppId) {
        boolean isQueryableViaUsesLibrary;
        synchronized (this.mQueryableViaUsesLibraryLock) {
            isQueryableViaUsesLibrary = super.isQueryableViaUsesLibrary(callingAppId, targetAppId);
        }
        return isQueryableViaUsesLibrary;
    }

    /* JADX INFO: Access modifiers changed from: protected */
    @Override // com.android.server.pm.AppsFilterBase
    public boolean shouldFilterApplicationUsingCache(int callingUid, int appId, int userId) {
        boolean shouldFilterApplicationUsingCache;
        synchronized (this.mCacheLock) {
            shouldFilterApplicationUsingCache = super.shouldFilterApplicationUsingCache(callingUid, appId, userId);
        }
        return shouldFilterApplicationUsingCache;
    }

    /* JADX INFO: Access modifiers changed from: protected */
    @Override // com.android.server.pm.AppsFilterBase
    public void dumpForceQueryable(PrintWriter pw, Integer filteringAppId, AppsFilterBase.ToString<Integer> expandPackages) {
        synchronized (this.mForceQueryableLock) {
            super.dumpForceQueryable(pw, filteringAppId, expandPackages);
        }
    }

    /* JADX INFO: Access modifiers changed from: protected */
    @Override // com.android.server.pm.AppsFilterBase
    public void dumpQueriesViaPackage(PrintWriter pw, Integer filteringAppId, AppsFilterBase.ToString<Integer> expandPackages) {
        synchronized (this.mQueriesViaPackageLock) {
            super.dumpQueriesViaPackage(pw, filteringAppId, expandPackages);
        }
    }

    /* JADX INFO: Access modifiers changed from: protected */
    @Override // com.android.server.pm.AppsFilterBase
    public void dumpQueriesViaComponent(PrintWriter pw, Integer filteringAppId, AppsFilterBase.ToString<Integer> expandPackages) {
        synchronized (this.mQueriesViaComponentLock) {
            super.dumpQueriesViaComponent(pw, filteringAppId, expandPackages);
        }
    }

    /* JADX INFO: Access modifiers changed from: protected */
    @Override // com.android.server.pm.AppsFilterBase
    public void dumpQueriesViaImplicitlyQueryable(PrintWriter pw, Integer filteringAppId, int[] users, AppsFilterBase.ToString<Integer> expandPackages) {
        synchronized (this.mImplicitlyQueryableLock) {
            super.dumpQueriesViaImplicitlyQueryable(pw, filteringAppId, users, expandPackages);
        }
    }

    /* JADX INFO: Access modifiers changed from: protected */
    @Override // com.android.server.pm.AppsFilterBase
    public void dumpQueriesViaUsesLibrary(PrintWriter pw, Integer filteringAppId, AppsFilterBase.ToString<Integer> expandPackages) {
        synchronized (this.mQueryableViaUsesLibraryLock) {
            super.dumpQueriesViaUsesLibrary(pw, filteringAppId, expandPackages);
        }
    }
}
