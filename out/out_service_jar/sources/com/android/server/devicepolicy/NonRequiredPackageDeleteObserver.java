package com.android.server.devicepolicy;

import android.content.pm.IPackageDeleteObserver;
import android.util.Log;
import android.util.Slog;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
/* loaded from: classes.dex */
final class NonRequiredPackageDeleteObserver extends IPackageDeleteObserver.Stub {
    private static final int PACKAGE_DELETE_TIMEOUT_SEC = 30;
    private final CountDownLatch mLatch;
    private final AtomicInteger mPackageCount;
    private boolean mSuccess;

    /* JADX INFO: Access modifiers changed from: package-private */
    public NonRequiredPackageDeleteObserver(int packageCount) {
        AtomicInteger atomicInteger = new AtomicInteger(0);
        this.mPackageCount = atomicInteger;
        this.mLatch = new CountDownLatch(packageCount);
        atomicInteger.set(packageCount);
    }

    public void packageDeleted(String packageName, int returnCode) {
        if (returnCode != 1) {
            Slog.e("DevicePolicyManager", "Failed to delete package: " + packageName);
            this.mLatch.notifyAll();
            return;
        }
        int currentPackageCount = this.mPackageCount.decrementAndGet();
        if (currentPackageCount == 0) {
            this.mSuccess = true;
            Slog.i("DevicePolicyManager", "All non-required system apps with launcher icon, and all disallowed apps have been uninstalled.");
        }
        this.mLatch.countDown();
    }

    public boolean awaitPackagesDeletion() {
        try {
            this.mLatch.await(30L, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Log.w("DevicePolicyManager", "Interrupted while waiting for package deletion", e);
            Thread.currentThread().interrupt();
        }
        return this.mSuccess;
    }
}
