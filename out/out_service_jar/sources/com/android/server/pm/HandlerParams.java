package com.android.server.pm;

import android.os.UserHandle;
import android.util.Slog;
/* JADX INFO: Access modifiers changed from: package-private */
/* loaded from: classes2.dex */
public abstract class HandlerParams {
    final InstallPackageHelper mInstallPackageHelper;
    final PackageManagerService mPm;
    int mTraceCookie;
    String mTraceMethod;
    private final UserHandle mUser;

    abstract void handleReturnCode();

    abstract void handleStartCopy();

    /* JADX INFO: Access modifiers changed from: package-private */
    public HandlerParams(UserHandle user, PackageManagerService pm) {
        this.mUser = user;
        this.mPm = pm;
        this.mInstallPackageHelper = new InstallPackageHelper(pm);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public UserHandle getUser() {
        return this.mUser;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public HandlerParams setTraceMethod(String traceMethod) {
        this.mTraceMethod = traceMethod;
        return this;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public HandlerParams setTraceCookie(int traceCookie) {
        this.mTraceCookie = traceCookie;
        return this;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public final void startCopy() {
        if (PackageManagerService.DEBUG_INSTALL) {
            Slog.i("PackageManager", "startCopy " + this.mUser + ": " + this);
        }
        handleStartCopy();
        handleReturnCode();
    }
}
