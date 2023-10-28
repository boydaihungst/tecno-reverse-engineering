package com.android.server.pm.parsing.pkg;

import com.android.internal.content.om.OverlayConfig;
import com.android.server.pm.pkg.AndroidPackageApi;
import com.android.server.pm.pkg.parsing.ParsingPackageRead;
/* loaded from: classes2.dex */
public interface AndroidPackage extends ParsingPackageRead, AndroidPackageApi, OverlayConfig.PackageProvider.Package {
    String getManifestPackageName();
}
