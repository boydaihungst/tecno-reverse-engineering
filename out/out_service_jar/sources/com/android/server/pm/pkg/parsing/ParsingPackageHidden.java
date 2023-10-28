package com.android.server.pm.pkg.parsing;

import android.content.pm.ApplicationInfo;
/* loaded from: classes2.dex */
interface ParsingPackageHidden {
    int getVersionCode();

    int getVersionCodeMajor();

    ApplicationInfo toAppInfoWithoutState();
}
