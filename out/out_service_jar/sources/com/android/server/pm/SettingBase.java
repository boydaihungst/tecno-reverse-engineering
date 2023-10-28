package com.android.server.pm;

import com.android.server.pm.permission.LegacyPermissionState;
import com.android.server.pm.pkg.mutate.PackageStateMutator;
import com.android.server.utils.Snappable;
import com.android.server.utils.Watchable;
import com.android.server.utils.WatchableImpl;
import com.android.server.utils.Watcher;
/* loaded from: classes2.dex */
public abstract class SettingBase implements Watchable, Snappable {
    private int mPkgFlags;
    private int mPkgPrivateFlags;
    private final Watchable mWatchable = new WatchableImpl();
    @Deprecated
    protected final LegacyPermissionState mLegacyPermissionsState = new LegacyPermissionState();

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

    public void onChanged() {
        PackageStateMutator.onPackageStateChanged();
        dispatchChange(this);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public SettingBase(int pkgFlags, int pkgPrivateFlags) {
        setFlags(pkgFlags);
        setPrivateFlags(pkgPrivateFlags);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public SettingBase(SettingBase orig) {
        if (orig != null) {
            copySettingBase(orig);
        }
    }

    public final void copySettingBase(SettingBase orig) {
        this.mPkgFlags = orig.mPkgFlags;
        this.mPkgPrivateFlags = orig.mPkgPrivateFlags;
        this.mLegacyPermissionsState.copyFrom(orig.mLegacyPermissionsState);
        onChanged();
    }

    @Deprecated
    public LegacyPermissionState getLegacyPermissionState() {
        return this.mLegacyPermissionsState;
    }

    public SettingBase setFlags(int pkgFlags) {
        this.mPkgFlags = 262401 & pkgFlags;
        onChanged();
        return this;
    }

    public SettingBase setPrivateFlags(int pkgPrivateFlags) {
        this.mPkgPrivateFlags = 1076757000 & pkgPrivateFlags;
        onChanged();
        return this;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public SettingBase setPkgFlags(int flags, int privateFlags) {
        this.mPkgFlags = flags;
        this.mPkgPrivateFlags = privateFlags;
        onChanged();
        return this;
    }

    public int getFlags() {
        return this.mPkgFlags;
    }

    public int getPrivateFlags() {
        return this.mPkgPrivateFlags;
    }
}
