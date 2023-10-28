package com.android.server.pm;

import android.content.pm.PackageManagerInternal;
import android.content.pm.SigningDetails;
import android.content.pm.UserInfo;
import android.os.Handler;
import android.os.Trace;
import android.os.UserHandle;
import android.provider.DeviceConfig;
import android.util.ArrayMap;
import android.util.ArraySet;
import android.util.Slog;
import android.util.SparseBooleanArray;
import com.android.internal.util.ArrayUtils;
import com.android.server.FgThread;
import com.android.server.compat.CompatChange;
import com.android.server.job.controllers.JobStatus;
import com.android.server.om.OverlayReferenceMapper;
import com.android.server.pm.AppsFilterImpl;
import com.android.server.pm.parsing.pkg.AndroidPackage;
import com.android.server.pm.parsing.pkg.AndroidPackageUtils;
import com.android.server.pm.pkg.PackageStateInternal;
import com.android.server.pm.pkg.component.ParsedInstrumentation;
import com.android.server.pm.snapshot.PackageDataSnapshot;
import com.android.server.utils.Snappable;
import com.android.server.utils.SnapshotCache;
import com.android.server.utils.Watchable;
import com.android.server.utils.WatchableImpl;
import com.android.server.utils.WatchedArrayList;
import com.android.server.utils.WatchedArraySet;
import com.android.server.utils.WatchedSparseBooleanMatrix;
import com.android.server.utils.WatchedSparseSetArray;
import com.android.server.utils.Watcher;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
/* loaded from: classes2.dex */
public final class AppsFilterImpl extends AppsFilterLocked implements Watchable, Snappable {
    private final SnapshotCache<AppsFilterSnapshot> mSnapshot;
    private final WatchableImpl mWatchable = new WatchableImpl();

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

    /* JADX INFO: Access modifiers changed from: private */
    public void onChanged() {
        dispatchChange(this);
    }

    private void invalidateCache(String reason) {
        if (this.mCacheValid.compareAndSet(true, false)) {
            Slog.i("AppsFilter", "Invalidating cache: " + reason);
        }
    }

    AppsFilterImpl(FeatureConfig featureConfig, String[] forceQueryableList, boolean systemAppsQueryable, OverlayReferenceMapper.Provider overlayProvider, Handler backgroundHandler) {
        this.mFeatureConfig = featureConfig;
        this.mForceQueryableByDevicePackageNames = forceQueryableList;
        this.mSystemAppsQueryable = systemAppsQueryable;
        this.mOverlayReferenceMapper = new OverlayReferenceMapper(true, overlayProvider);
        this.mBackgroundHandler = backgroundHandler;
        this.mShouldFilterCache = new WatchedSparseBooleanMatrix();
        this.mShouldFilterCacheSnapshot = new SnapshotCache.Auto(this.mShouldFilterCache, this.mShouldFilterCache, "AppsFilter.mShouldFilterCache");
        this.mImplicitlyQueryable = new WatchedSparseSetArray<>();
        this.mImplicitQueryableSnapshot = new SnapshotCache.Auto(this.mImplicitlyQueryable, this.mImplicitlyQueryable, "AppsFilter.mImplicitlyQueryable");
        this.mRetainedImplicitlyQueryable = new WatchedSparseSetArray<>();
        this.mRetainedImplicitlyQueryableSnapshot = new SnapshotCache.Auto(this.mRetainedImplicitlyQueryable, this.mRetainedImplicitlyQueryable, "AppsFilter.mRetainedImplicitlyQueryable");
        this.mQueriesViaPackage = new WatchedSparseSetArray<>();
        this.mQueriesViaPackageSnapshot = new SnapshotCache.Auto(this.mQueriesViaPackage, this.mQueriesViaPackage, "AppsFilter.mQueriesViaPackage");
        this.mQueriesViaComponent = new WatchedSparseSetArray<>();
        this.mQueriesViaComponentSnapshot = new SnapshotCache.Auto(this.mQueriesViaComponent, this.mQueriesViaComponent, "AppsFilter.mQueriesViaComponent");
        this.mQueryableViaUsesLibrary = new WatchedSparseSetArray<>();
        this.mQueryableViaUsesLibrarySnapshot = new SnapshotCache.Auto(this.mQueryableViaUsesLibrary, this.mQueryableViaUsesLibrary, "AppsFilter.mQueryableViaUsesLibrary");
        this.mForceQueryable = new WatchedArraySet<>();
        this.mForceQueryableSnapshot = new SnapshotCache.Auto(this.mForceQueryable, this.mForceQueryable, "AppsFilter.mForceQueryable");
        this.mProtectedBroadcasts = new WatchedArrayList<>();
        this.mProtectedBroadcastsSnapshot = new SnapshotCache.Auto(this.mProtectedBroadcasts, this.mProtectedBroadcasts, "AppsFilter.mProtectedBroadcasts");
        this.mSnapshot = new SnapshotCache<AppsFilterSnapshot>(this, this) { // from class: com.android.server.pm.AppsFilterImpl.1
            /* JADX DEBUG: Method merged with bridge method */
            /* JADX WARN: Can't rename method to resolve collision */
            @Override // com.android.server.utils.SnapshotCache
            public AppsFilterSnapshot createSnapshot() {
                return new AppsFilterSnapshotImpl(AppsFilterImpl.this);
            }
        };
    }

    /* JADX DEBUG: Method merged with bridge method */
    @Override // com.android.server.utils.Snappable
    public AppsFilterSnapshot snapshot() {
        return this.mSnapshot.snapshot();
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes2.dex */
    public static class FeatureConfigImpl implements FeatureConfig, CompatChange.ChangeListener {
        private static final String FILTERING_ENABLED_NAME = "package_query_filtering_enabled";
        private AppsFilterImpl mAppsFilter;
        private final ArraySet<String> mDisabledPackages;
        private volatile boolean mFeatureEnabled;
        private final PackageManagerServiceInjector mInjector;
        private SparseBooleanArray mLoggingEnabled;
        private final PackageManagerInternal mPmInternal;

        private FeatureConfigImpl(PackageManagerInternal pmInternal, PackageManagerServiceInjector injector) {
            this.mFeatureEnabled = true;
            this.mDisabledPackages = new ArraySet<>();
            this.mLoggingEnabled = null;
            this.mPmInternal = pmInternal;
            this.mInjector = injector;
        }

        FeatureConfigImpl(FeatureConfigImpl orig) {
            this.mFeatureEnabled = true;
            ArraySet<String> arraySet = new ArraySet<>();
            this.mDisabledPackages = arraySet;
            this.mLoggingEnabled = null;
            this.mInjector = null;
            this.mPmInternal = null;
            this.mFeatureEnabled = orig.mFeatureEnabled;
            arraySet.addAll((ArraySet<? extends String>) orig.mDisabledPackages);
            this.mLoggingEnabled = orig.mLoggingEnabled;
        }

        public void setAppsFilter(AppsFilterImpl filter) {
            this.mAppsFilter = filter;
        }

        @Override // com.android.server.pm.FeatureConfig
        public void onSystemReady() {
            this.mFeatureEnabled = DeviceConfig.getBoolean("package_manager_service", FILTERING_ENABLED_NAME, true);
            DeviceConfig.addOnPropertiesChangedListener("package_manager_service", FgThread.getExecutor(), new DeviceConfig.OnPropertiesChangedListener() { // from class: com.android.server.pm.AppsFilterImpl$FeatureConfigImpl$$ExternalSyntheticLambda0
                public final void onPropertiesChanged(DeviceConfig.Properties properties) {
                    AppsFilterImpl.FeatureConfigImpl.this.m5374xdc7e04ce(properties);
                }
            });
            this.mInjector.getCompatibility().registerListener(135549675L, this);
        }

        /* JADX INFO: Access modifiers changed from: package-private */
        /* renamed from: lambda$onSystemReady$0$com-android-server-pm-AppsFilterImpl$FeatureConfigImpl  reason: not valid java name */
        public /* synthetic */ void m5374xdc7e04ce(DeviceConfig.Properties properties) {
            if (properties.getKeyset().contains(FILTERING_ENABLED_NAME)) {
                synchronized (this) {
                    this.mFeatureEnabled = properties.getBoolean(FILTERING_ENABLED_NAME, true);
                }
            }
        }

        @Override // com.android.server.pm.FeatureConfig
        public boolean isGloballyEnabled() {
            if (AppsFilterBase.DEBUG_TRACING) {
                Trace.traceBegin(262144L, "isGloballyEnabled");
            }
            try {
                return this.mFeatureEnabled;
            } finally {
                if (AppsFilterBase.DEBUG_TRACING) {
                    Trace.traceEnd(262144L);
                }
            }
        }

        @Override // com.android.server.pm.FeatureConfig
        public boolean packageIsEnabled(AndroidPackage pkg) {
            if (AppsFilterBase.DEBUG_TRACING) {
                Trace.traceBegin(262144L, "packageIsEnabled");
            }
            try {
                return !this.mDisabledPackages.contains(pkg.getPackageName());
            } finally {
                if (AppsFilterBase.DEBUG_TRACING) {
                    Trace.traceEnd(262144L);
                }
            }
        }

        @Override // com.android.server.pm.FeatureConfig
        public boolean isLoggingEnabled(int uid) {
            SparseBooleanArray sparseBooleanArray = this.mLoggingEnabled;
            return sparseBooleanArray != null && sparseBooleanArray.indexOfKey(uid) >= 0;
        }

        @Override // com.android.server.pm.FeatureConfig
        public void enableLogging(int appId, boolean enable) {
            int index;
            if (enable) {
                if (this.mLoggingEnabled == null) {
                    this.mLoggingEnabled = new SparseBooleanArray();
                }
                this.mLoggingEnabled.put(appId, true);
                return;
            }
            SparseBooleanArray sparseBooleanArray = this.mLoggingEnabled;
            if (sparseBooleanArray != null && (index = sparseBooleanArray.indexOfKey(appId)) >= 0) {
                this.mLoggingEnabled.removeAt(index);
                if (this.mLoggingEnabled.size() == 0) {
                    this.mLoggingEnabled = null;
                }
            }
        }

        @Override // com.android.server.compat.CompatChange.ChangeListener
        public void onCompatChange(String packageName) {
            PackageDataSnapshot snapshot = this.mPmInternal.snapshot();
            AndroidPackage pkg = snapshot.getPackage(packageName);
            if (pkg == null) {
                return;
            }
            updateEnabledState(pkg);
            this.mAppsFilter.updateShouldFilterCacheForPackage(snapshot, packageName);
        }

        private void updateEnabledState(AndroidPackage pkg) {
            boolean enabled = this.mInjector.getCompatibility().isChangeEnabledInternalNoLogging(135549675L, AndroidPackageUtils.generateAppInfoWithoutState(pkg));
            if (enabled) {
                this.mDisabledPackages.remove(pkg.getPackageName());
            } else {
                this.mDisabledPackages.add(pkg.getPackageName());
            }
            AppsFilterImpl appsFilterImpl = this.mAppsFilter;
            if (appsFilterImpl != null) {
                appsFilterImpl.onChanged();
            }
        }

        @Override // com.android.server.pm.FeatureConfig
        public void updatePackageState(PackageStateInternal setting, boolean removed) {
            boolean enableLogging = (setting.getPkg() == null || removed || (!setting.getPkg().isTestOnly() && !setting.getPkg().isDebuggable())) ? false : true;
            enableLogging(setting.getAppId(), enableLogging);
            if (removed) {
                this.mDisabledPackages.remove(setting.getPackageName());
                AppsFilterImpl appsFilterImpl = this.mAppsFilter;
                if (appsFilterImpl != null) {
                    appsFilterImpl.onChanged();
                }
            } else if (setting.getPkg() != null) {
                updateEnabledState(setting.getPkg());
            }
        }

        @Override // com.android.server.pm.FeatureConfig
        public FeatureConfig snapshot() {
            return new FeatureConfigImpl(this);
        }
    }

    public static AppsFilterImpl create(PackageManagerServiceInjector injector, PackageManagerInternal pmInt) {
        String[] forcedQueryablePackageNames;
        boolean forceSystemAppsQueryable = injector.getContext().getResources().getBoolean(17891668);
        FeatureConfigImpl featureConfig = new FeatureConfigImpl(pmInt, injector);
        if (forceSystemAppsQueryable) {
            forcedQueryablePackageNames = new String[0];
        } else {
            String[] forcedQueryablePackageNames2 = injector.getContext().getResources().getStringArray(17236070);
            for (int i = 0; i < forcedQueryablePackageNames2.length; i++) {
                forcedQueryablePackageNames2[i] = forcedQueryablePackageNames2[i].intern();
            }
            forcedQueryablePackageNames = forcedQueryablePackageNames2;
        }
        AppsFilterImpl appsFilter = new AppsFilterImpl(featureConfig, forcedQueryablePackageNames, forceSystemAppsQueryable, null, injector.getBackgroundHandler());
        featureConfig.setAppsFilter(appsFilter);
        return appsFilter;
    }

    public FeatureConfig getFeatureConfig() {
        return this.mFeatureConfig;
    }

    public boolean grantImplicitAccess(int recipientUid, int visibleUid, boolean retainOnUpdate) {
        boolean changed;
        if (recipientUid == visibleUid) {
            return false;
        }
        synchronized (this.mImplicitlyQueryableLock) {
            if (retainOnUpdate) {
                changed = this.mRetainedImplicitlyQueryable.add(recipientUid, Integer.valueOf(visibleUid));
            } else {
                changed = this.mImplicitlyQueryable.add(recipientUid, Integer.valueOf(visibleUid));
            }
        }
        if (changed && DEBUG_LOGGING) {
            Slog.i("AppsFilter", (retainOnUpdate ? "retained " : "") + "implicit access granted: " + recipientUid + " -> " + visibleUid);
        }
        if (this.mCacheReady) {
            synchronized (this.mCacheLock) {
                this.mShouldFilterCache.put(recipientUid, visibleUid, false);
            }
        } else if (changed) {
            invalidateCache("grantImplicitAccess: " + recipientUid + " -> " + visibleUid);
        }
        if (changed) {
            onChanged();
        }
        return changed;
    }

    public void onSystemReady(PackageManagerInternal pmInternal) {
        this.mOverlayReferenceMapper.rebuildIfDeferred();
        this.mFeatureConfig.onSystemReady();
        updateEntireShouldFilterCacheAsync(pmInternal);
    }

    /* JADX WARN: Removed duplicated region for block: B:52:0x00d7  */
    /*
        Code decompiled incorrectly, please refer to instructions dump.
    */
    public void addPackage(PackageDataSnapshot snapshot, PackageStateInternal newPkgSetting, boolean isReplace) {
        long j;
        Object obj;
        ArraySet<String> additionalChangedPackages;
        ArrayMap<String, ? extends PackageStateInternal> settings;
        long j2;
        long j3 = 262144;
        if (DEBUG_TRACING) {
            Trace.traceBegin(262144L, "filter.addPackage");
        }
        if (isReplace) {
            try {
                removePackage(snapshot, newPkgSetting, true);
            } catch (Throwable th) {
                th = th;
                onChanged();
                if (DEBUG_TRACING) {
                }
                throw th;
            }
        }
        ArrayMap<String, ? extends PackageStateInternal> settings2 = snapshot.getPackageStates();
        UserInfo[] users = snapshot.getUserInfos();
        ArraySet<String> additionalChangedPackages2 = addPackageInternal(newPkgSetting, settings2);
        try {
            if (!this.mCacheReady) {
                j = 262144;
                invalidateCache("addPackage: " + newPkgSetting.getPackageName());
            } else {
                Object obj2 = this.mCacheLock;
                synchronized (obj2) {
                    try {
                        obj = obj2;
                        try {
                            updateShouldFilterCacheForPackage(snapshot, null, newPkgSetting, settings2, users, -1, settings2.size());
                            if (additionalChangedPackages2 != null) {
                                int index = 0;
                                while (index < additionalChangedPackages2.size()) {
                                    String changedPackage = additionalChangedPackages2.valueAt(index);
                                    PackageStateInternal changedPkgSetting = settings2.get(changedPackage);
                                    if (changedPkgSetting == null) {
                                        additionalChangedPackages = additionalChangedPackages2;
                                        settings = settings2;
                                        j2 = j3;
                                    } else {
                                        additionalChangedPackages = additionalChangedPackages2;
                                        settings = settings2;
                                        j2 = j3;
                                        try {
                                            updateShouldFilterCacheForPackage(snapshot, null, changedPkgSetting, settings, users, -1, settings2.size());
                                        } catch (Throwable th2) {
                                            th = th2;
                                            throw th;
                                        }
                                    }
                                    index++;
                                    settings2 = settings;
                                    additionalChangedPackages2 = additionalChangedPackages;
                                    j3 = j2;
                                }
                                j = j3;
                            } else {
                                j = 262144;
                            }
                        } catch (Throwable th3) {
                            th = th3;
                            throw th;
                        }
                    } catch (Throwable th4) {
                        th = th4;
                        obj = obj2;
                    }
                }
            }
            onChanged();
            if (DEBUG_TRACING) {
                Trace.traceEnd(j);
            }
        } catch (Throwable th5) {
            th = th5;
            onChanged();
            if (DEBUG_TRACING) {
                Trace.traceEnd(262144L);
            }
            throw th;
        }
    }

    private ArraySet<String> addPackageInternal(PackageStateInternal newPkgSetting, ArrayMap<String, ? extends PackageStateInternal> existingSettings) {
        boolean protectedBroadcastsChanged;
        boolean newIsForceQueryable;
        boolean existingIsForceQueryable;
        if (Objects.equals(PackageManagerService.PLATFORM_PACKAGE_NAME, newPkgSetting.getPackageName())) {
            this.mSystemSigningDetails = newPkgSetting.getSigningDetails();
            for (PackageStateInternal setting : existingSettings.values()) {
                if (isSystemSigned(this.mSystemSigningDetails, setting)) {
                    synchronized (this.mForceQueryableLock) {
                        this.mForceQueryable.add(Integer.valueOf(setting.getAppId()));
                    }
                }
            }
        }
        AndroidPackage newPkg = newPkgSetting.getPkg();
        if (newPkg == null) {
            return null;
        }
        synchronized (this.mProtectedBroadcastsLock) {
            protectedBroadcastsChanged = this.mProtectedBroadcasts.addAll(newPkg.getProtectedBroadcasts());
        }
        if (protectedBroadcastsChanged) {
            this.mQueriesViaComponentRequireRecompute.set(true);
        }
        synchronized (this.mForceQueryableLock) {
            if (!this.mForceQueryable.contains(Integer.valueOf(newPkgSetting.getAppId())) && !newPkgSetting.isForceQueryableOverride() && (!newPkgSetting.isSystem() || (!this.mSystemAppsQueryable && !newPkg.isForceQueryable() && !ArrayUtils.contains(this.mForceQueryableByDevicePackageNames, newPkg.getPackageName())))) {
                newIsForceQueryable = false;
                if (!newIsForceQueryable || (this.mSystemSigningDetails != null && isSystemSigned(this.mSystemSigningDetails, newPkgSetting))) {
                    this.mForceQueryable.add(Integer.valueOf(newPkgSetting.getAppId()));
                }
            }
            newIsForceQueryable = true;
            if (!newIsForceQueryable) {
            }
            this.mForceQueryable.add(Integer.valueOf(newPkgSetting.getAppId()));
        }
        for (int i = existingSettings.size() - 1; i >= 0; i--) {
            PackageStateInternal existingSetting = existingSettings.valueAt(i);
            if (existingSetting.getAppId() != newPkgSetting.getAppId() && existingSetting.getPkg() != null) {
                AndroidPackage existingPkg = existingSetting.getPkg();
                if (!newIsForceQueryable) {
                    if (!this.mQueriesViaComponentRequireRecompute.get() && AppsFilterUtils.canQueryViaComponents(existingPkg, newPkg, this.mProtectedBroadcasts)) {
                        synchronized (this.mQueriesViaComponentLock) {
                            this.mQueriesViaComponent.add(existingSetting.getAppId(), Integer.valueOf(newPkgSetting.getAppId()));
                        }
                    }
                    if (AppsFilterUtils.canQueryViaPackage(existingPkg, newPkg) || AppsFilterUtils.canQueryAsInstaller(existingSetting, newPkg)) {
                        synchronized (this.mQueriesViaPackageLock) {
                            this.mQueriesViaPackage.add(existingSetting.getAppId(), Integer.valueOf(newPkgSetting.getAppId()));
                        }
                    }
                    if (AppsFilterUtils.canQueryViaUsesLibrary(existingPkg, newPkg)) {
                        synchronized (this.mQueryableViaUsesLibraryLock) {
                            this.mQueryableViaUsesLibrary.add(existingSetting.getAppId(), Integer.valueOf(newPkgSetting.getAppId()));
                        }
                    }
                }
                synchronized (this.mForceQueryableLock) {
                    existingIsForceQueryable = this.mForceQueryable.contains(Integer.valueOf(existingSetting.getAppId()));
                }
                if (!existingIsForceQueryable) {
                    if (!this.mQueriesViaComponentRequireRecompute.get() && AppsFilterUtils.canQueryViaComponents(newPkg, existingPkg, this.mProtectedBroadcasts)) {
                        synchronized (this.mQueriesViaComponentLock) {
                            this.mQueriesViaComponent.add(newPkgSetting.getAppId(), Integer.valueOf(existingSetting.getAppId()));
                        }
                    }
                    if (AppsFilterUtils.canQueryViaPackage(newPkg, existingPkg) || AppsFilterUtils.canQueryAsInstaller(newPkgSetting, existingPkg)) {
                        synchronized (this.mQueriesViaPackageLock) {
                            this.mQueriesViaPackage.add(newPkgSetting.getAppId(), Integer.valueOf(existingSetting.getAppId()));
                        }
                    }
                    if (AppsFilterUtils.canQueryViaUsesLibrary(newPkg, existingPkg)) {
                        synchronized (this.mQueryableViaUsesLibraryLock) {
                            this.mQueryableViaUsesLibrary.add(newPkgSetting.getAppId(), Integer.valueOf(existingSetting.getAppId()));
                        }
                    }
                }
                if (newPkgSetting.getPkg() != null && existingSetting.getPkg() != null && (pkgInstruments(newPkgSetting.getPkg(), existingSetting.getPkg()) || pkgInstruments(existingSetting.getPkg(), newPkgSetting.getPkg()))) {
                    synchronized (this.mQueriesViaPackageLock) {
                        this.mQueriesViaPackage.add(newPkgSetting.getAppId(), Integer.valueOf(existingSetting.getAppId()));
                        this.mQueriesViaPackage.add(existingSetting.getAppId(), Integer.valueOf(newPkgSetting.getAppId()));
                    }
                }
            }
        }
        int existingSize = existingSettings.size();
        ArrayMap<String, AndroidPackage> existingPkgs = new ArrayMap<>(existingSize);
        for (int index = 0; index < existingSize; index++) {
            PackageStateInternal pkgSetting = existingSettings.valueAt(index);
            if (pkgSetting.getPkg() != null) {
                existingPkgs.put(pkgSetting.getPackageName(), pkgSetting.getPkg());
            }
        }
        ArraySet<String> changedPackages = this.mOverlayReferenceMapper.addPkg(newPkgSetting.getPkg(), existingPkgs);
        this.mFeatureConfig.updatePackageState(newPkgSetting, false);
        return changedPackages;
    }

    private void removeAppIdFromVisibilityCache(int appId) {
        synchronized (this.mCacheLock) {
            int i = 0;
            while (i < this.mShouldFilterCache.size()) {
                if (UserHandle.getAppId(this.mShouldFilterCache.keyAt(i)) == appId) {
                    this.mShouldFilterCache.removeAt(i);
                    i--;
                }
                i++;
            }
        }
    }

    private void updateEntireShouldFilterCache(PackageDataSnapshot snapshot, int subjectUserId) {
        ArrayMap<String, ? extends PackageStateInternal> settings = snapshot.getPackageStates();
        UserInfo[] users = snapshot.getUserInfos();
        int userId = -10000;
        int u = 0;
        while (true) {
            if (u >= users.length) {
                break;
            } else if (subjectUserId != users[u].id) {
                u++;
            } else {
                userId = subjectUserId;
                break;
            }
        }
        if (userId == -10000) {
            Slog.e("AppsFilter", "We encountered a new user that isn't a member of known users, updating the whole cache");
            userId = -1;
        }
        updateEntireShouldFilterCacheInner(snapshot, settings, users, userId);
        onChanged();
    }

    private void updateEntireShouldFilterCacheInner(PackageDataSnapshot snapshot, ArrayMap<String, ? extends PackageStateInternal> settings, UserInfo[] users, int subjectUserId) {
        synchronized (this.mCacheLock) {
            if (subjectUserId == -1) {
                this.mShouldFilterCache.clear();
            }
            this.mShouldFilterCache.setCapacity(users.length * settings.size());
            for (int i = settings.size() - 1; i >= 0; i--) {
                updateShouldFilterCacheForPackage(snapshot, null, settings.valueAt(i), settings, users, subjectUserId, i);
            }
        }
    }

    private void updateEntireShouldFilterCacheAsync(PackageManagerInternal pmInternal) {
        updateEntireShouldFilterCacheAsync(pmInternal, JobStatus.DEFAULT_TRIGGER_UPDATE_DELAY);
    }

    private void updateEntireShouldFilterCacheAsync(final PackageManagerInternal pmInternal, final long delayMs) {
        this.mBackgroundHandler.postDelayed(new Runnable() { // from class: com.android.server.pm.AppsFilterImpl$$ExternalSyntheticLambda0
            @Override // java.lang.Runnable
            public final void run() {
                AppsFilterImpl.this.m5373xc48ca7d6(pmInternal, delayMs);
            }
        }, delayMs);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$updateEntireShouldFilterCacheAsync$0$com-android-server-pm-AppsFilterImpl  reason: not valid java name */
    public /* synthetic */ void m5373xc48ca7d6(PackageManagerInternal pmInternal, long delayMs) {
        if (!this.mCacheValid.compareAndSet(false, true)) {
            return;
        }
        ArrayMap<String, AndroidPackage> packagesCache = new ArrayMap<>();
        PackageDataSnapshot snapshot = pmInternal.snapshot();
        ArrayMap<String, ? extends PackageStateInternal> settings = snapshot.getPackageStates();
        UserInfo[] users = snapshot.getUserInfos();
        packagesCache.ensureCapacity(settings.size());
        UserInfo[][] usersRef = {users};
        int max = settings.size();
        for (int i = 0; i < max; i++) {
            AndroidPackage pkg = settings.valueAt(i).getPkg();
            packagesCache.put(settings.keyAt(i), pkg);
        }
        updateEntireShouldFilterCacheInner(snapshot, settings, usersRef[0], -1);
        onChanged();
        if (!this.mCacheValid.compareAndSet(true, true)) {
            Slog.i("AppsFilter", "Cache invalidated while building, retrying.");
            updateEntireShouldFilterCacheAsync(pmInternal, Math.min(2 * delayMs, (long) JobStatus.DEFAULT_TRIGGER_UPDATE_DELAY));
            return;
        }
        this.mCacheReady = true;
    }

    public void onUserCreated(PackageDataSnapshot snapshot, int newUserId) {
        if (!this.mCacheReady) {
            return;
        }
        updateEntireShouldFilterCache(snapshot, newUserId);
    }

    public void onUserDeleted(int userId) {
        if (!this.mCacheReady) {
            return;
        }
        removeShouldFilterCacheForUser(userId);
        onChanged();
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void updateShouldFilterCacheForPackage(PackageDataSnapshot snapshot, String packageName) {
        if (!this.mCacheReady) {
            return;
        }
        ArrayMap<String, ? extends PackageStateInternal> settings = snapshot.getPackageStates();
        UserInfo[] users = snapshot.getUserInfos();
        synchronized (this.mCacheLock) {
            updateShouldFilterCacheForPackage(snapshot, null, settings.get(packageName), settings, users, -1, settings.size());
        }
        onChanged();
    }

    private void updateShouldFilterCacheForPackage(PackageDataSnapshot snapshot, String skipPackageName, PackageStateInternal subjectSetting, ArrayMap<String, ? extends PackageStateInternal> allSettings, UserInfo[] allUsers, int subjectUserId, int maxIndex) {
        for (int i = Math.min(maxIndex, allSettings.size() - 1); i >= 0; i--) {
            PackageStateInternal otherSetting = allSettings.valueAt(i);
            if (subjectSetting.getAppId() != otherSetting.getAppId() && subjectSetting.getPackageName() != skipPackageName && otherSetting.getPackageName() != skipPackageName) {
                if (subjectUserId == -1) {
                    for (UserInfo userInfo : allUsers) {
                        updateShouldFilterCacheForUser(snapshot, subjectSetting, allUsers, otherSetting, userInfo.id);
                    }
                } else {
                    updateShouldFilterCacheForUser(snapshot, subjectSetting, allUsers, otherSetting, subjectUserId);
                }
            }
        }
    }

    private void updateShouldFilterCacheForUser(PackageDataSnapshot snapshot, PackageStateInternal subjectSetting, UserInfo[] allUsers, PackageStateInternal otherSetting, int subjectUserId) {
        for (UserInfo userInfo : allUsers) {
            int otherUser = userInfo.id;
            int subjectUid = UserHandle.getUid(subjectUserId, subjectSetting.getAppId());
            int otherUid = UserHandle.getUid(otherUser, otherSetting.getAppId());
            boolean shouldFilterSubjectToOther = shouldFilterApplicationInternal(snapshot, subjectUid, subjectSetting, otherSetting, otherUser);
            boolean shouldFilterOtherToSubject = shouldFilterApplicationInternal(snapshot, otherUid, otherSetting, subjectSetting, subjectUserId);
            this.mShouldFilterCache.put(subjectUid, otherUid, shouldFilterSubjectToOther);
            this.mShouldFilterCache.put(otherUid, subjectUid, shouldFilterOtherToSubject);
        }
    }

    private void removeShouldFilterCacheForUser(int userId) {
        synchronized (this.mCacheLock) {
            int[] cacheUids = this.mShouldFilterCache.keys();
            int size = cacheUids.length;
            int pos = Arrays.binarySearch(cacheUids, UserHandle.getUid(userId, 0));
            int fromIndex = pos >= 0 ? pos : ~pos;
            if (fromIndex < size && UserHandle.getUserId(cacheUids[fromIndex]) == userId) {
                int pos2 = Arrays.binarySearch(cacheUids, UserHandle.getUid(userId + 1, 0) - 1);
                int toIndex = pos2 >= 0 ? pos2 + 1 : ~pos2;
                if (fromIndex < toIndex && UserHandle.getUserId(cacheUids[toIndex - 1]) == userId) {
                    this.mShouldFilterCache.removeRange(fromIndex, toIndex);
                    this.mShouldFilterCache.compact();
                    return;
                }
                Slog.w("AppsFilter", "Failed to remove should filter cache for user " + userId + ", fromIndex=" + fromIndex + ", toIndex=" + toIndex);
                return;
            }
            Slog.w("AppsFilter", "Failed to remove should filter cache for user " + userId + ", fromIndex=" + fromIndex);
        }
    }

    private static boolean isSystemSigned(SigningDetails sysSigningDetails, PackageStateInternal pkgSetting) {
        return pkgSetting.isSystem() && pkgSetting.getSigningDetails().signaturesMatchExactly(sysSigningDetails);
    }

    private void collectProtectedBroadcasts(ArrayMap<String, ? extends PackageStateInternal> existingSettings, String excludePackage) {
        synchronized (this.mProtectedBroadcastsLock) {
            this.mProtectedBroadcasts.clear();
            for (int i = existingSettings.size() - 1; i >= 0; i--) {
                PackageStateInternal setting = existingSettings.valueAt(i);
                if (setting.getPkg() != null && !setting.getPkg().getPackageName().equals(excludePackage)) {
                    List<String> protectedBroadcasts = setting.getPkg().getProtectedBroadcasts();
                    if (!protectedBroadcasts.isEmpty()) {
                        this.mProtectedBroadcasts.addAll(protectedBroadcasts);
                    }
                }
            }
        }
    }

    @Override // com.android.server.pm.AppsFilterBase
    protected boolean isQueryableViaComponentWhenRequireRecompute(ArrayMap<String, ? extends PackageStateInternal> existingSettings, PackageStateInternal callingPkgSetting, ArraySet<PackageStateInternal> callingSharedPkgSettings, AndroidPackage targetPkg, int callingAppId, int targetAppId) {
        recomputeComponentVisibility(existingSettings);
        return isQueryableViaComponent(callingAppId, targetAppId);
    }

    private void recomputeComponentVisibility(ArrayMap<String, ? extends PackageStateInternal> existingSettings) {
        boolean canQueryViaComponents;
        synchronized (this.mQueriesViaComponentLock) {
            this.mQueriesViaComponent.clear();
        }
        for (int i = existingSettings.size() - 1; i >= 0; i--) {
            PackageStateInternal setting = existingSettings.valueAt(i);
            if (setting.getPkg() != null && !AppsFilterUtils.requestsQueryAllPackages(setting.getPkg())) {
                for (int j = existingSettings.size() - 1; j >= 0; j--) {
                    if (i != j) {
                        PackageStateInternal otherSetting = existingSettings.valueAt(j);
                        if (otherSetting.getPkg() != null && !this.mForceQueryable.contains(Integer.valueOf(otherSetting.getAppId()))) {
                            synchronized (this.mProtectedBroadcastsLock) {
                                canQueryViaComponents = AppsFilterUtils.canQueryViaComponents(setting.getPkg(), otherSetting.getPkg(), this.mProtectedBroadcasts);
                            }
                            if (canQueryViaComponents) {
                                synchronized (this.mQueriesViaComponentLock) {
                                    this.mQueriesViaComponent.add(setting.getAppId(), Integer.valueOf(otherSetting.getAppId()));
                                }
                            } else {
                                continue;
                            }
                        }
                    }
                }
                continue;
            }
        }
        this.mQueriesViaComponentRequireRecompute.set(false);
        onChanged();
    }

    public void addPackage(PackageDataSnapshot snapshot, PackageStateInternal newPkgSetting) {
        addPackage(snapshot, newPkgSetting, false);
    }

    /* JADX WARN: Unsupported multi-entry loop pattern (BACK_EDGE: B:117:0x0269 -> B:118:0x026a). Please submit an issue!!! */
    /* JADX WARN: Unsupported multi-entry loop pattern (BACK_EDGE: B:133:? -> B:86:0x01f0). Please submit an issue!!! */
    /* JADX WARN: Unsupported multi-entry loop pattern (BACK_EDGE: B:145:? -> B:105:0x0238). Please submit an issue!!! */
    public void removePackage(PackageDataSnapshot snapshot, PackageStateInternal setting, boolean isReplace) {
        boolean protectedBroadcastsChanged;
        ArraySet<String> additionalChangedPackages;
        Object obj;
        int index;
        Object obj2;
        int i;
        ArraySet<? extends PackageStateInternal> sharedUserPackages;
        ArraySet<String> additionalChangedPackages2;
        PackageStateInternal packageStateInternal = setting;
        ArrayMap<String, ? extends PackageStateInternal> settings = snapshot.getPackageStates();
        UserInfo[] users = snapshot.getUserInfos();
        Collection<SharedUserSetting> sharedUserSettings = snapshot.getAllSharedUsers();
        synchronized (this.mImplicitlyQueryableLock) {
            for (UserInfo userInfo : users) {
                int userId = userInfo.id;
                int removingUid = UserHandle.getUid(userId, setting.getAppId());
                this.mImplicitlyQueryable.remove(removingUid);
                for (int i2 = this.mImplicitlyQueryable.size() - 1; i2 >= 0; i2--) {
                    this.mImplicitlyQueryable.remove(this.mImplicitlyQueryable.keyAt(i2), Integer.valueOf(removingUid));
                }
                if (!isReplace) {
                    this.mRetainedImplicitlyQueryable.remove(removingUid);
                    for (int i3 = this.mRetainedImplicitlyQueryable.size() - 1; i3 >= 0; i3--) {
                        this.mRetainedImplicitlyQueryable.remove(this.mRetainedImplicitlyQueryable.keyAt(i3), Integer.valueOf(removingUid));
                    }
                }
            }
        }
        if (!this.mQueriesViaComponentRequireRecompute.get()) {
            synchronized (this.mQueriesViaComponentLock) {
                this.mQueriesViaComponent.remove(setting.getAppId());
                for (int i4 = this.mQueriesViaComponent.size() - 1; i4 >= 0; i4--) {
                    this.mQueriesViaComponent.remove(this.mQueriesViaComponent.keyAt(i4), Integer.valueOf(setting.getAppId()));
                }
            }
        }
        synchronized (this.mQueriesViaPackageLock) {
            this.mQueriesViaPackage.remove(setting.getAppId());
            for (int i5 = this.mQueriesViaPackage.size() - 1; i5 >= 0; i5--) {
                this.mQueriesViaPackage.remove(this.mQueriesViaPackage.keyAt(i5), Integer.valueOf(setting.getAppId()));
            }
        }
        synchronized (this.mQueryableViaUsesLibraryLock) {
            this.mQueryableViaUsesLibrary.remove(setting.getAppId());
            for (int i6 = this.mQueryableViaUsesLibrary.size() - 1; i6 >= 0; i6--) {
                this.mQueryableViaUsesLibrary.remove(this.mQueryableViaUsesLibrary.keyAt(i6), Integer.valueOf(setting.getAppId()));
            }
        }
        synchronized (this.mForceQueryableLock) {
            this.mForceQueryable.remove(Integer.valueOf(setting.getAppId()));
        }
        synchronized (this.mProtectedBroadcastsLock) {
            try {
                if (setting.getPkg() != null && !setting.getPkg().getProtectedBroadcasts().isEmpty()) {
                    String removingPackageName = setting.getPkg().getPackageName();
                    ArrayList<String> protectedBroadcasts = new ArrayList<>(this.mProtectedBroadcasts.untrackedStorage());
                    collectProtectedBroadcasts(settings, removingPackageName);
                    boolean protectedBroadcastsChanged2 = !this.mProtectedBroadcasts.containsAll(protectedBroadcasts);
                    protectedBroadcastsChanged = protectedBroadcastsChanged2;
                } else {
                    protectedBroadcastsChanged = false;
                }
                try {
                    if (protectedBroadcastsChanged) {
                        this.mQueriesViaComponentRequireRecompute.set(true);
                    }
                    ArraySet<String> additionalChangedPackages3 = this.mOverlayReferenceMapper.removePkg(setting.getPackageName());
                    this.mFeatureConfig.updatePackageState(packageStateInternal, true);
                    if (setting.hasSharedUser()) {
                        ArraySet<? extends PackageStateInternal> sharedUserPackages2 = getSharedUserPackages(setting.getSharedUserAppId(), sharedUserSettings);
                        for (int i7 = sharedUserPackages2.size() - 1; i7 >= 0; i7--) {
                            if (sharedUserPackages2.valueAt(i7) != packageStateInternal) {
                                addPackageInternal(sharedUserPackages2.valueAt(i7), settings);
                            }
                        }
                    }
                    if (this.mCacheReady) {
                        removeAppIdFromVisibilityCache(setting.getAppId());
                        if (setting.hasSharedUser()) {
                            ArraySet<? extends PackageStateInternal> sharedUserPackages3 = getSharedUserPackages(setting.getSharedUserAppId(), sharedUserSettings);
                            int i8 = sharedUserPackages3.size() - 1;
                            while (i8 >= 0) {
                                PackageStateInternal siblingSetting = sharedUserPackages3.valueAt(i8);
                                if (siblingSetting == packageStateInternal) {
                                    i = i8;
                                    sharedUserPackages = sharedUserPackages3;
                                    additionalChangedPackages2 = additionalChangedPackages3;
                                } else {
                                    Object obj3 = this.mCacheLock;
                                    synchronized (obj3) {
                                        try {
                                            obj2 = obj3;
                                            i = i8;
                                            sharedUserPackages = sharedUserPackages3;
                                            additionalChangedPackages2 = additionalChangedPackages3;
                                            try {
                                                updateShouldFilterCacheForPackage(snapshot, setting.getPackageName(), siblingSetting, settings, users, -1, settings.size());
                                            } catch (Throwable th) {
                                                th = th;
                                                throw th;
                                            }
                                        } catch (Throwable th2) {
                                            th = th2;
                                            obj2 = obj3;
                                            throw th;
                                        }
                                    }
                                }
                                i8 = i - 1;
                                additionalChangedPackages3 = additionalChangedPackages2;
                                sharedUserPackages3 = sharedUserPackages;
                                packageStateInternal = setting;
                            }
                            additionalChangedPackages = additionalChangedPackages3;
                        } else {
                            additionalChangedPackages = additionalChangedPackages3;
                        }
                        if (additionalChangedPackages != null) {
                            int index2 = 0;
                            while (index2 < additionalChangedPackages.size()) {
                                String changedPackage = additionalChangedPackages.valueAt(index2);
                                PackageStateInternal changedPkgSetting = settings.get(changedPackage);
                                if (changedPkgSetting == null) {
                                    index = index2;
                                } else {
                                    Object obj4 = this.mCacheLock;
                                    synchronized (obj4) {
                                        try {
                                            obj = obj4;
                                            index = index2;
                                            try {
                                                updateShouldFilterCacheForPackage(snapshot, null, changedPkgSetting, settings, users, -1, settings.size());
                                            } catch (Throwable th3) {
                                                th = th3;
                                                throw th;
                                            }
                                        } catch (Throwable th4) {
                                            th = th4;
                                            obj = obj4;
                                            throw th;
                                        }
                                    }
                                }
                                index2 = index + 1;
                            }
                        }
                    } else {
                        invalidateCache("removePackage: " + setting.getPackageName());
                    }
                    onChanged();
                } catch (Throwable th5) {
                    th = th5;
                    throw th;
                }
            } catch (Throwable th6) {
                th = th6;
            }
        }
    }

    /* JADX DEBUG: Another duplicated slice has different insns count: {[SGET]}, finally: {[SGET, INVOKE, IF] complete} */
    private static boolean pkgInstruments(AndroidPackage source, AndroidPackage target) {
        try {
            if (DEBUG_TRACING) {
                Trace.traceBegin(262144L, "pkgInstruments");
            }
            String packageName = target.getPackageName();
            List<ParsedInstrumentation> inst = source.getInstrumentations();
            for (int i = ArrayUtils.size(inst) - 1; i >= 0; i--) {
                if (Objects.equals(inst.get(i).getTargetPackage(), packageName)) {
                    return true;
                }
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
}
