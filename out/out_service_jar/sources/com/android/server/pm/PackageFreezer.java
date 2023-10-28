package com.android.server.pm;

import dalvik.system.CloseGuard;
import java.util.concurrent.atomic.AtomicBoolean;
/* JADX INFO: Access modifiers changed from: package-private */
/* loaded from: classes2.dex */
public final class PackageFreezer implements AutoCloseable {
    private final CloseGuard mCloseGuard;
    private final AtomicBoolean mClosed;
    private final String mPackageName;
    private final PackageManagerService mPm;

    /* JADX INFO: Access modifiers changed from: package-private */
    public PackageFreezer(PackageManagerService pm) {
        AtomicBoolean atomicBoolean = new AtomicBoolean();
        this.mClosed = atomicBoolean;
        CloseGuard closeGuard = CloseGuard.get();
        this.mCloseGuard = closeGuard;
        this.mPm = pm;
        this.mPackageName = null;
        atomicBoolean.set(true);
        closeGuard.open("close");
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public PackageFreezer(String packageName, int userId, String killReason, PackageManagerService pm) {
        PackageSetting ps;
        this.mClosed = new AtomicBoolean();
        CloseGuard closeGuard = CloseGuard.get();
        this.mCloseGuard = closeGuard;
        this.mPm = pm;
        this.mPackageName = packageName;
        synchronized (pm.mLock) {
            int refCounts = pm.mFrozenPackages.getOrDefault(packageName, 0).intValue() + 1;
            pm.mFrozenPackages.put(packageName, Integer.valueOf(refCounts));
            ps = pm.mSettings.getPackageLPr(packageName);
        }
        if (ps != null) {
            pm.killApplication(ps.getPackageName(), ps.getAppId(), userId, killReason);
        }
        closeGuard.open("close");
    }

    protected void finalize() throws Throwable {
        try {
            this.mCloseGuard.warnIfOpen();
            close();
        } finally {
            super.finalize();
        }
    }

    @Override // java.lang.AutoCloseable
    public void close() {
        this.mCloseGuard.close();
        if (this.mClosed.compareAndSet(false, true)) {
            synchronized (this.mPm.mLock) {
                int refCounts = this.mPm.mFrozenPackages.getOrDefault(this.mPackageName, 0).intValue() - 1;
                if (refCounts > 0) {
                    this.mPm.mFrozenPackages.put(this.mPackageName, Integer.valueOf(refCounts));
                } else {
                    this.mPm.mFrozenPackages.remove(this.mPackageName);
                }
            }
        }
    }
}
