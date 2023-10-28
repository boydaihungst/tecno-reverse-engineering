package com.android.server.pm.parsing.pkg;

import com.android.server.pm.pkg.parsing.PkgWithoutStatePackageInfo;
/* loaded from: classes2.dex */
public interface PkgPackageInfo extends PkgWithoutStatePackageInfo {
    boolean isCoreApp();

    boolean isStub();
}
