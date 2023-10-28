package com.android.server.pm.parsing.pkg;

import android.content.pm.ApplicationInfo;
/* loaded from: classes2.dex */
interface AndroidPackageHidden {
    String getPrimaryCpuAbi();

    String getSeInfo();

    String getSecondaryCpuAbi();

    @Deprecated
    int getVersionCode();

    int getVersionCodeMajor();

    ApplicationInfo toAppInfoWithoutState();
}
