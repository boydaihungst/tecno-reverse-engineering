package com.android.server.pm;
/* loaded from: classes2.dex */
public final class PostInstallData {
    public final InstallArgs args;
    public final Runnable mPostInstallRunnable;
    public final PackageInstalledInfo res;

    /* JADX INFO: Access modifiers changed from: package-private */
    public PostInstallData(InstallArgs args, PackageInstalledInfo res, Runnable postInstallRunnable) {
        this.args = args;
        this.res = res;
        this.mPostInstallRunnable = postInstallRunnable;
    }
}
