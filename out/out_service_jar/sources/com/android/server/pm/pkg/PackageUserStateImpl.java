package com.android.server.pm.pkg;

import android.content.ComponentName;
import android.content.pm.PackageManager;
import android.content.pm.overlay.OverlayPaths;
import android.text.TextUtils;
import android.util.ArrayMap;
import android.util.ArraySet;
import android.util.Pair;
import com.android.internal.util.AnnotationValidations;
import com.android.internal.util.ArrayUtils;
import com.android.internal.util.CollectionUtils;
import com.android.server.utils.Snappable;
import com.android.server.utils.SnapshotCache;
import com.android.server.utils.Watchable;
import com.android.server.utils.WatchableImpl;
import com.android.server.utils.WatchedArrayMap;
import com.android.server.utils.WatchedArraySet;
import java.lang.annotation.Annotation;
import java.util.Collections;
import java.util.Map;
import java.util.Objects;
/* loaded from: classes2.dex */
public class PackageUserStateImpl extends WatchableImpl implements PackageUserStateInternal, Snappable {
    public int isNotifyScreenOn;
    private long mCeDataInode;
    private WatchedArrayMap<ComponentName, Pair<String, Integer>> mComponentLabelIconOverrideMap;
    protected WatchedArraySet<String> mDisabledComponentsWatched;
    private int mDistractionFlags;
    protected WatchedArraySet<String> mEnabledComponentsWatched;
    private int mEnabledState;
    private long mFirstInstallTime;
    private String mHarmfulAppWarning;
    private boolean mHidden;
    private int mInstallReason;
    private boolean mInstalled;
    private boolean mInstantApp;
    private String mLastDisableAppCaller;
    private boolean mNotLaunched;
    private OverlayPaths mOverlayPaths;
    protected WatchedArrayMap<String, OverlayPaths> mSharedLibraryOverlayPaths;
    final SnapshotCache<PackageUserStateImpl> mSnapshot;
    private String mSplashScreenTheme;
    private boolean mStopped;
    private WatchedArrayMap<String, SuspendParams> mSuspendParams;
    private int mUninstallReason;
    private boolean mVirtualPreload;
    private final Watchable mWatchable;

    private SnapshotCache<PackageUserStateImpl> makeCache() {
        return new SnapshotCache<PackageUserStateImpl>(this, this) { // from class: com.android.server.pm.pkg.PackageUserStateImpl.1
            /* JADX DEBUG: Method merged with bridge method */
            /* JADX WARN: Can't rename method to resolve collision */
            @Override // com.android.server.utils.SnapshotCache
            public PackageUserStateImpl createSnapshot() {
                return new PackageUserStateImpl(PackageUserStateImpl.this.mWatchable, (PackageUserStateImpl) this.mSource);
            }
        };
    }

    public PackageUserStateImpl() {
        this.mInstalled = true;
        this.mEnabledState = 0;
        this.mInstallReason = 0;
        this.mUninstallReason = 0;
        this.isNotifyScreenOn = 0;
        this.mWatchable = null;
        this.mSnapshot = makeCache();
    }

    public PackageUserStateImpl(Watchable watchable) {
        this.mInstalled = true;
        this.mEnabledState = 0;
        this.mInstallReason = 0;
        this.mUninstallReason = 0;
        this.isNotifyScreenOn = 0;
        this.mWatchable = watchable;
        this.mSnapshot = makeCache();
    }

    public PackageUserStateImpl(Watchable watchable, PackageUserStateImpl other) {
        this.mInstalled = true;
        this.mEnabledState = 0;
        this.mInstallReason = 0;
        this.mUninstallReason = 0;
        this.isNotifyScreenOn = 0;
        this.mWatchable = watchable;
        WatchedArraySet<String> watchedArraySet = other.mDisabledComponentsWatched;
        this.mDisabledComponentsWatched = watchedArraySet == null ? null : watchedArraySet.snapshot();
        WatchedArraySet<String> watchedArraySet2 = other.mEnabledComponentsWatched;
        this.mEnabledComponentsWatched = watchedArraySet2 == null ? null : watchedArraySet2.snapshot();
        this.mOverlayPaths = other.mOverlayPaths;
        WatchedArrayMap<String, OverlayPaths> watchedArrayMap = other.mSharedLibraryOverlayPaths;
        this.mSharedLibraryOverlayPaths = watchedArrayMap == null ? null : watchedArrayMap.snapshot();
        this.mCeDataInode = other.mCeDataInode;
        this.mInstalled = other.mInstalled;
        this.mStopped = other.mStopped;
        this.mNotLaunched = other.mNotLaunched;
        this.mHidden = other.mHidden;
        this.mDistractionFlags = other.mDistractionFlags;
        this.mInstantApp = other.mInstantApp;
        this.mVirtualPreload = other.mVirtualPreload;
        this.mEnabledState = other.mEnabledState;
        this.mInstallReason = other.mInstallReason;
        this.mUninstallReason = other.mUninstallReason;
        this.mHarmfulAppWarning = other.mHarmfulAppWarning;
        this.mLastDisableAppCaller = other.mLastDisableAppCaller;
        this.mSplashScreenTheme = other.mSplashScreenTheme;
        WatchedArrayMap<String, SuspendParams> watchedArrayMap2 = other.mSuspendParams;
        this.mSuspendParams = watchedArrayMap2 == null ? null : watchedArrayMap2.snapshot();
        WatchedArrayMap<ComponentName, Pair<String, Integer>> watchedArrayMap3 = other.mComponentLabelIconOverrideMap;
        this.mComponentLabelIconOverrideMap = watchedArrayMap3 != null ? watchedArrayMap3.snapshot() : null;
        this.mFirstInstallTime = other.mFirstInstallTime;
        this.mSnapshot = new SnapshotCache.Sealed();
        this.isNotifyScreenOn = other.isNotifyScreenOn;
    }

    private void onChanged() {
        Watchable watchable = this.mWatchable;
        if (watchable != null) {
            watchable.dispatchChange(watchable);
        }
        dispatchChange(this);
    }

    /* JADX DEBUG: Method merged with bridge method */
    @Override // com.android.server.utils.Snappable
    public PackageUserStateImpl snapshot() {
        return this.mSnapshot.snapshot();
    }

    public boolean setOverlayPaths(OverlayPaths paths) {
        if (Objects.equals(paths, this.mOverlayPaths)) {
            return false;
        }
        if ((this.mOverlayPaths == null && paths.isEmpty()) || (paths == null && this.mOverlayPaths.isEmpty())) {
            return false;
        }
        this.mOverlayPaths = paths;
        onChanged();
        return true;
    }

    public boolean setSharedLibraryOverlayPaths(String library, OverlayPaths paths) {
        if (this.mSharedLibraryOverlayPaths == null) {
            WatchedArrayMap<String, OverlayPaths> watchedArrayMap = new WatchedArrayMap<>();
            this.mSharedLibraryOverlayPaths = watchedArrayMap;
            watchedArrayMap.registerObserver(this.mSnapshot);
        }
        OverlayPaths currentPaths = this.mSharedLibraryOverlayPaths.get(library);
        if (Objects.equals(paths, currentPaths)) {
            return false;
        }
        if (paths == null || paths.isEmpty()) {
            boolean returnValue = this.mSharedLibraryOverlayPaths.remove(library) != null;
            onChanged();
            return returnValue;
        }
        this.mSharedLibraryOverlayPaths.put(library, paths);
        onChanged();
        return true;
    }

    @Override // com.android.server.pm.pkg.PackageUserStateInternal
    public WatchedArraySet<String> getDisabledComponentsNoCopy() {
        return this.mDisabledComponentsWatched;
    }

    @Override // com.android.server.pm.pkg.PackageUserStateInternal
    public WatchedArraySet<String> getEnabledComponentsNoCopy() {
        return this.mEnabledComponentsWatched;
    }

    /* JADX DEBUG: Method merged with bridge method */
    @Override // com.android.server.pm.pkg.PackageUserState
    /* renamed from: getDisabledComponents */
    public ArraySet<String> m5857getDisabledComponents() {
        WatchedArraySet<String> watchedArraySet = this.mDisabledComponentsWatched;
        return watchedArraySet == null ? new ArraySet<>() : watchedArraySet.untrackedStorage();
    }

    /* JADX DEBUG: Method merged with bridge method */
    @Override // com.android.server.pm.pkg.PackageUserState
    /* renamed from: getEnabledComponents */
    public ArraySet<String> m5858getEnabledComponents() {
        WatchedArraySet<String> watchedArraySet = this.mEnabledComponentsWatched;
        return watchedArraySet == null ? new ArraySet<>() : watchedArraySet.untrackedStorage();
    }

    @Override // com.android.server.pm.pkg.PackageUserState
    public boolean isComponentEnabled(String componentName) {
        WatchedArraySet<String> watchedArraySet = this.mEnabledComponentsWatched;
        return watchedArraySet != null && watchedArraySet.contains(componentName);
    }

    @Override // com.android.server.pm.pkg.PackageUserState
    public boolean isComponentDisabled(String componentName) {
        WatchedArraySet<String> watchedArraySet = this.mDisabledComponentsWatched;
        return watchedArraySet != null && watchedArraySet.contains(componentName);
    }

    @Override // com.android.server.pm.pkg.PackageUserState
    public OverlayPaths getAllOverlayPaths() {
        if (this.mOverlayPaths == null && this.mSharedLibraryOverlayPaths == null) {
            return null;
        }
        OverlayPaths.Builder newPaths = new OverlayPaths.Builder();
        newPaths.addAll(this.mOverlayPaths);
        WatchedArrayMap<String, OverlayPaths> watchedArrayMap = this.mSharedLibraryOverlayPaths;
        if (watchedArrayMap != null) {
            for (OverlayPaths libOverlayPaths : watchedArrayMap.values()) {
                newPaths.addAll(libOverlayPaths);
            }
        }
        return newPaths.build();
    }

    public boolean overrideLabelAndIcon(ComponentName component, String nonLocalizedLabel, Integer icon) {
        Pair<String, Integer> pair;
        String existingLabel = null;
        Integer existingIcon = null;
        WatchedArrayMap<ComponentName, Pair<String, Integer>> watchedArrayMap = this.mComponentLabelIconOverrideMap;
        if (watchedArrayMap != null && (pair = watchedArrayMap.get(component)) != null) {
            existingLabel = (String) pair.first;
            existingIcon = (Integer) pair.second;
        }
        boolean changed = (TextUtils.equals(existingLabel, nonLocalizedLabel) && Objects.equals(existingIcon, icon)) ? false : true;
        if (changed) {
            if (nonLocalizedLabel == null && icon == null) {
                this.mComponentLabelIconOverrideMap.remove(component);
                if (this.mComponentLabelIconOverrideMap.isEmpty()) {
                    this.mComponentLabelIconOverrideMap = null;
                }
            } else {
                if (this.mComponentLabelIconOverrideMap == null) {
                    WatchedArrayMap<ComponentName, Pair<String, Integer>> watchedArrayMap2 = new WatchedArrayMap<>(1);
                    this.mComponentLabelIconOverrideMap = watchedArrayMap2;
                    watchedArrayMap2.registerObserver(this.mSnapshot);
                }
                this.mComponentLabelIconOverrideMap.put(component, Pair.create(nonLocalizedLabel, icon));
            }
            onChanged();
        }
        return changed;
    }

    public void resetOverrideComponentLabelIcon() {
        this.mComponentLabelIconOverrideMap = null;
    }

    @Override // com.android.server.pm.pkg.PackageUserStateInternal
    public Pair<String, Integer> getOverrideLabelIconForComponent(ComponentName componentName) {
        if (ArrayUtils.isEmpty(this.mComponentLabelIconOverrideMap)) {
            return null;
        }
        return this.mComponentLabelIconOverrideMap.get(componentName);
    }

    @Override // com.android.server.pm.pkg.PackageUserState
    public boolean isSuspended() {
        return !CollectionUtils.isEmpty(this.mSuspendParams);
    }

    public PackageUserStateImpl putSuspendParams(String suspendingPackage, SuspendParams suspendParams) {
        if (this.mSuspendParams == null) {
            WatchedArrayMap<String, SuspendParams> watchedArrayMap = new WatchedArrayMap<>();
            this.mSuspendParams = watchedArrayMap;
            watchedArrayMap.registerObserver(this.mSnapshot);
        }
        if (!this.mSuspendParams.containsKey(suspendingPackage) || !Objects.equals(this.mSuspendParams.get(suspendingPackage), suspendParams)) {
            this.mSuspendParams.put(suspendingPackage, suspendParams);
            onChanged();
        }
        return this;
    }

    public PackageUserStateImpl removeSuspension(String suspendingPackage) {
        WatchedArrayMap<String, SuspendParams> watchedArrayMap = this.mSuspendParams;
        if (watchedArrayMap != null) {
            watchedArrayMap.remove(suspendingPackage);
            onChanged();
        }
        return this;
    }

    public PackageUserStateImpl setDisabledComponents(ArraySet<String> value) {
        if (value == null) {
            return this;
        }
        if (this.mDisabledComponentsWatched == null) {
            WatchedArraySet<String> watchedArraySet = new WatchedArraySet<>();
            this.mDisabledComponentsWatched = watchedArraySet;
            watchedArraySet.registerObserver(this.mSnapshot);
        }
        this.mDisabledComponentsWatched.clear();
        this.mDisabledComponentsWatched.addAll(value);
        onChanged();
        return this;
    }

    public PackageUserStateImpl setEnabledComponents(ArraySet<String> value) {
        if (value == null) {
            return this;
        }
        if (this.mEnabledComponentsWatched == null) {
            WatchedArraySet<String> watchedArraySet = new WatchedArraySet<>();
            this.mEnabledComponentsWatched = watchedArraySet;
            watchedArraySet.registerObserver(this.mSnapshot);
        }
        this.mEnabledComponentsWatched.clear();
        this.mEnabledComponentsWatched.addAll(value);
        onChanged();
        return this;
    }

    public PackageUserStateImpl setEnabledComponents(WatchedArraySet<String> value) {
        this.mEnabledComponentsWatched = value;
        if (value != null) {
            value.registerObserver(this.mSnapshot);
        }
        onChanged();
        return this;
    }

    public PackageUserStateImpl setDisabledComponents(WatchedArraySet<String> value) {
        this.mDisabledComponentsWatched = value;
        if (value != null) {
            value.registerObserver(this.mSnapshot);
        }
        onChanged();
        return this;
    }

    public PackageUserStateImpl setCeDataInode(long value) {
        this.mCeDataInode = value;
        onChanged();
        return this;
    }

    public PackageUserStateImpl setInstalled(boolean value) {
        this.mInstalled = value;
        onChanged();
        return this;
    }

    public PackageUserStateImpl setStopped(boolean value) {
        this.mStopped = value;
        onChanged();
        return this;
    }

    public PackageUserStateImpl setNotLaunched(boolean value) {
        this.mNotLaunched = value;
        onChanged();
        return this;
    }

    public PackageUserStateImpl setHidden(boolean value) {
        this.mHidden = value;
        onChanged();
        return this;
    }

    public PackageUserStateImpl setDistractionFlags(int value) {
        this.mDistractionFlags = value;
        onChanged();
        return this;
    }

    public PackageUserStateImpl setInstantApp(boolean value) {
        this.mInstantApp = value;
        onChanged();
        return this;
    }

    public PackageUserStateImpl setVirtualPreload(boolean value) {
        this.mVirtualPreload = value;
        onChanged();
        return this;
    }

    public PackageUserStateImpl setEnabledState(int value) {
        this.mEnabledState = value;
        onChanged();
        return this;
    }

    public PackageUserStateImpl setInstallReason(int value) {
        this.mInstallReason = value;
        AnnotationValidations.validate(PackageManager.InstallReason.class, (Annotation) null, value);
        onChanged();
        return this;
    }

    public PackageUserStateImpl setUninstallReason(int value) {
        this.mUninstallReason = value;
        AnnotationValidations.validate(PackageManager.UninstallReason.class, (Annotation) null, value);
        onChanged();
        return this;
    }

    public PackageUserStateImpl setHarmfulAppWarning(String value) {
        this.mHarmfulAppWarning = value;
        onChanged();
        return this;
    }

    public PackageUserStateImpl setLastDisableAppCaller(String value) {
        this.mLastDisableAppCaller = value;
        onChanged();
        return this;
    }

    public PackageUserStateImpl setSharedLibraryOverlayPaths(ArrayMap<String, OverlayPaths> value) {
        if (value == null) {
            return this;
        }
        if (this.mSharedLibraryOverlayPaths == null) {
            this.mSharedLibraryOverlayPaths = new WatchedArrayMap<>();
            registerObserver(this.mSnapshot);
        }
        this.mSharedLibraryOverlayPaths.clear();
        this.mSharedLibraryOverlayPaths.putAll(value);
        onChanged();
        return this;
    }

    public PackageUserStateImpl setSplashScreenTheme(String value) {
        this.mSplashScreenTheme = value;
        onChanged();
        return this;
    }

    public PackageUserStateImpl setSuspendParams(ArrayMap<String, SuspendParams> value) {
        if (value == null) {
            return this;
        }
        if (this.mSuspendParams == null) {
            this.mSuspendParams = new WatchedArrayMap<>();
            registerObserver(this.mSnapshot);
        }
        this.mSuspendParams.clear();
        this.mSuspendParams.putAll(value);
        onChanged();
        return this;
    }

    public PackageUserStateImpl setComponentLabelIconOverrideMap(ArrayMap<ComponentName, Pair<String, Integer>> value) {
        if (value == null) {
            return this;
        }
        if (this.mComponentLabelIconOverrideMap == null) {
            this.mComponentLabelIconOverrideMap = new WatchedArrayMap<>();
            registerObserver(this.mSnapshot);
        }
        this.mComponentLabelIconOverrideMap.clear();
        this.mComponentLabelIconOverrideMap.putAll(value);
        onChanged();
        return this;
    }

    public PackageUserStateImpl setFirstInstallTime(long value) {
        this.mFirstInstallTime = value;
        onChanged();
        return this;
    }

    @Override // com.android.server.pm.pkg.PackageUserState
    public Map<String, OverlayPaths> getSharedLibraryOverlayPaths() {
        WatchedArrayMap<String, OverlayPaths> watchedArrayMap = this.mSharedLibraryOverlayPaths;
        if (watchedArrayMap != null) {
            return watchedArrayMap;
        }
        return Collections.emptyMap();
    }

    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        PackageUserStateImpl that = (PackageUserStateImpl) o;
        if (Objects.equals(this.mDisabledComponentsWatched, that.mDisabledComponentsWatched) && Objects.equals(this.mEnabledComponentsWatched, that.mEnabledComponentsWatched) && this.mCeDataInode == that.mCeDataInode && this.mInstalled == that.mInstalled && this.mStopped == that.mStopped && this.mNotLaunched == that.mNotLaunched && this.mHidden == that.mHidden && this.mDistractionFlags == that.mDistractionFlags && this.mInstantApp == that.mInstantApp && this.mVirtualPreload == that.mVirtualPreload && this.mEnabledState == that.mEnabledState && this.mInstallReason == that.mInstallReason && this.mUninstallReason == that.mUninstallReason && Objects.equals(this.mHarmfulAppWarning, that.mHarmfulAppWarning) && Objects.equals(this.mLastDisableAppCaller, that.mLastDisableAppCaller) && Objects.equals(this.mOverlayPaths, that.mOverlayPaths) && Objects.equals(this.mSharedLibraryOverlayPaths, that.mSharedLibraryOverlayPaths) && Objects.equals(this.mSplashScreenTheme, that.mSplashScreenTheme) && Objects.equals(this.mSuspendParams, that.mSuspendParams) && Objects.equals(this.mComponentLabelIconOverrideMap, that.mComponentLabelIconOverrideMap) && this.mFirstInstallTime == that.mFirstInstallTime && Objects.equals(this.mWatchable, that.mWatchable)) {
            return true;
        }
        return false;
    }

    public int hashCode() {
        int _hash = (1 * 31) + Objects.hashCode(this.mDisabledComponentsWatched);
        return (((((((((((((((((((((((((((((((((((((((((_hash * 31) + Objects.hashCode(this.mEnabledComponentsWatched)) * 31) + Long.hashCode(this.mCeDataInode)) * 31) + Boolean.hashCode(this.mInstalled)) * 31) + Boolean.hashCode(this.mStopped)) * 31) + Boolean.hashCode(this.mNotLaunched)) * 31) + Boolean.hashCode(this.mHidden)) * 31) + this.mDistractionFlags) * 31) + Boolean.hashCode(this.mInstantApp)) * 31) + Boolean.hashCode(this.mVirtualPreload)) * 31) + this.mEnabledState) * 31) + this.mInstallReason) * 31) + this.mUninstallReason) * 31) + Objects.hashCode(this.mHarmfulAppWarning)) * 31) + Objects.hashCode(this.mLastDisableAppCaller)) * 31) + Objects.hashCode(this.mOverlayPaths)) * 31) + Objects.hashCode(this.mSharedLibraryOverlayPaths)) * 31) + Objects.hashCode(this.mSplashScreenTheme)) * 31) + Objects.hashCode(this.mSuspendParams)) * 31) + Objects.hashCode(this.mComponentLabelIconOverrideMap)) * 31) + Long.hashCode(this.mFirstInstallTime)) * 31) + Objects.hashCode(this.mWatchable);
    }

    public WatchedArraySet<String> getDisabledComponentsWatched() {
        return this.mDisabledComponentsWatched;
    }

    public WatchedArraySet<String> getEnabledComponentsWatched() {
        return this.mEnabledComponentsWatched;
    }

    @Override // com.android.server.pm.pkg.PackageUserState
    public long getCeDataInode() {
        return this.mCeDataInode;
    }

    @Override // com.android.server.pm.pkg.PackageUserState
    public boolean isInstalled() {
        return this.mInstalled;
    }

    @Override // com.android.server.pm.pkg.PackageUserState
    public boolean isStopped() {
        return this.mStopped;
    }

    @Override // com.android.server.pm.pkg.PackageUserState
    public boolean isNotLaunched() {
        return this.mNotLaunched;
    }

    @Override // com.android.server.pm.pkg.PackageUserState
    public boolean isHidden() {
        return this.mHidden;
    }

    @Override // com.android.server.pm.pkg.PackageUserState
    public int getDistractionFlags() {
        return this.mDistractionFlags;
    }

    @Override // com.android.server.pm.pkg.PackageUserState
    public boolean isInstantApp() {
        return this.mInstantApp;
    }

    @Override // com.android.server.pm.pkg.PackageUserState
    public boolean isVirtualPreload() {
        return this.mVirtualPreload;
    }

    @Override // com.android.server.pm.pkg.PackageUserState
    public int getEnabledState() {
        return this.mEnabledState;
    }

    public int isNotifyScreenOn() {
        return this.isNotifyScreenOn;
    }

    @Override // com.android.server.pm.pkg.PackageUserState
    public int getInstallReason() {
        return this.mInstallReason;
    }

    @Override // com.android.server.pm.pkg.PackageUserState
    public int getUninstallReason() {
        return this.mUninstallReason;
    }

    @Override // com.android.server.pm.pkg.PackageUserState
    public String getHarmfulAppWarning() {
        return this.mHarmfulAppWarning;
    }

    @Override // com.android.server.pm.pkg.PackageUserState
    public String getLastDisableAppCaller() {
        return this.mLastDisableAppCaller;
    }

    @Override // com.android.server.pm.pkg.PackageUserState
    public OverlayPaths getOverlayPaths() {
        return this.mOverlayPaths;
    }

    @Override // com.android.server.pm.pkg.PackageUserState
    public String getSplashScreenTheme() {
        return this.mSplashScreenTheme;
    }

    @Override // com.android.server.pm.pkg.PackageUserStateInternal
    public WatchedArrayMap<String, SuspendParams> getSuspendParams() {
        return this.mSuspendParams;
    }

    public WatchedArrayMap<ComponentName, Pair<String, Integer>> getComponentLabelIconOverrideMap() {
        return this.mComponentLabelIconOverrideMap;
    }

    @Override // com.android.server.pm.pkg.PackageUserState
    public long getFirstInstallTime() {
        return this.mFirstInstallTime;
    }

    public Watchable getWatchable() {
        return this.mWatchable;
    }

    public SnapshotCache<PackageUserStateImpl> getSnapshot() {
        return this.mSnapshot;
    }

    public PackageUserStateImpl setDisabledComponentsWatched(WatchedArraySet<String> value) {
        this.mDisabledComponentsWatched = value;
        return this;
    }

    public PackageUserStateImpl setEnabledComponentsWatched(WatchedArraySet<String> value) {
        this.mEnabledComponentsWatched = value;
        return this;
    }

    public PackageUserStateImpl setSharedLibraryOverlayPaths(WatchedArrayMap<String, OverlayPaths> value) {
        this.mSharedLibraryOverlayPaths = value;
        return this;
    }

    public PackageUserStateImpl setSuspendParams(WatchedArrayMap<String, SuspendParams> value) {
        this.mSuspendParams = value;
        return this;
    }

    public PackageUserStateImpl setComponentLabelIconOverrideMap(WatchedArrayMap<ComponentName, Pair<String, Integer>> value) {
        this.mComponentLabelIconOverrideMap = value;
        return this;
    }

    @Deprecated
    private void __metadata() {
    }
}
