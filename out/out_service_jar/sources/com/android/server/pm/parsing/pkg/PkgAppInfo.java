package com.android.server.pm.parsing.pkg;

import com.android.server.pm.pkg.parsing.PkgWithoutStateAppInfo;
/* loaded from: classes2.dex */
public interface PkgAppInfo extends PkgWithoutStateAppInfo {
    String getNativeLibraryDir();

    String getNativeLibraryRootDir();

    String getSecondaryNativeLibraryDir();

    @Deprecated
    int getUid();

    boolean isFactoryTest();

    boolean isNativeLibraryRootRequiresIsa();

    boolean isOdm();

    boolean isOem();

    boolean isPrivileged();

    boolean isProduct();

    boolean isSignedWithPlatformKey();

    boolean isSystem();

    boolean isSystemExt();

    boolean isVendor();
}
