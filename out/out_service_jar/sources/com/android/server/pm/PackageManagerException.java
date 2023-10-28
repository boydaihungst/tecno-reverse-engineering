package com.android.server.pm;

import android.hardware.biometrics.fingerprint.V2_1.RequestStatus;
import com.android.server.pm.Installer;
/* loaded from: classes2.dex */
public class PackageManagerException extends Exception {
    public final int error;

    public PackageManagerException(String detailMessage) {
        super(detailMessage);
        this.error = RequestStatus.SYS_ETIMEDOUT;
    }

    public PackageManagerException(int error, String detailMessage) {
        super(detailMessage);
        this.error = error;
    }

    public PackageManagerException(int error, String detailMessage, Throwable throwable) {
        super(detailMessage, throwable);
        this.error = error;
    }

    public PackageManagerException(Throwable e) {
        super(e);
        this.error = RequestStatus.SYS_ETIMEDOUT;
    }

    public static PackageManagerException from(Installer.InstallerException e) throws PackageManagerException {
        throw new PackageManagerException(RequestStatus.SYS_ETIMEDOUT, e.getMessage(), e.getCause());
    }
}
