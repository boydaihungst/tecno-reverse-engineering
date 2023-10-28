package com.android.server.pm;

import android.content.pm.IPackageLoadingProgressCallback;
import com.android.server.pm.pkg.PackageStateInternal;
import com.android.server.pm.pkg.mutate.PackageStateWrite;
import java.util.function.Consumer;
/* JADX INFO: Access modifiers changed from: package-private */
/* loaded from: classes2.dex */
public final class IncrementalProgressListener extends IPackageLoadingProgressCallback.Stub {
    private final String mPackageName;
    private final PackageManagerService mPm;

    /* JADX INFO: Access modifiers changed from: package-private */
    public IncrementalProgressListener(String packageName, PackageManagerService pm) {
        this.mPackageName = packageName;
        this.mPm = pm;
    }

    public void onPackageLoadingProgressChanged(final float progress) {
        PackageStateInternal packageState = this.mPm.snapshotComputer().getPackageStateInternal(this.mPackageName);
        if (packageState == null) {
            return;
        }
        boolean wasLoading = packageState.isLoading();
        if (wasLoading) {
            this.mPm.commitPackageStateMutation(null, this.mPackageName, new Consumer() { // from class: com.android.server.pm.IncrementalProgressListener$$ExternalSyntheticLambda0
                @Override // java.util.function.Consumer
                public final void accept(Object obj) {
                    ((PackageStateWrite) obj).setLoadingProgress(progress);
                }
            });
            if (Math.abs(1.0f - progress) < 1.0E-8f) {
                this.mPm.mIncrementalManager.unregisterLoadingProgressCallbacks(packageState.getPathString());
                this.mPm.scheduleWriteSettings();
            }
        }
    }
}
