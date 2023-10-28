package com.android.server.pm;
/* JADX INFO: Access modifiers changed from: package-private */
/* loaded from: classes2.dex */
public final class InstallRequest {
    public final InstallArgs mArgs;
    public final PackageInstalledInfo mInstallResult;

    /* JADX INFO: Access modifiers changed from: package-private */
    public InstallRequest(InstallArgs args, PackageInstalledInfo res) {
        this.mArgs = args;
        this.mInstallResult = res;
    }
}
