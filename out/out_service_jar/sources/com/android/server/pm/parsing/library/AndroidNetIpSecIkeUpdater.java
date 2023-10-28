package com.android.server.pm.parsing.library;

import com.android.server.pm.parsing.pkg.ParsedPackage;
/* loaded from: classes2.dex */
public class AndroidNetIpSecIkeUpdater extends PackageSharedLibraryUpdater {
    private static final String LIBRARY_NAME = "android.net.ipsec.ike";

    @Override // com.android.server.pm.parsing.library.PackageSharedLibraryUpdater
    public void updatePackage(ParsedPackage parsedPackage, boolean isUpdatedSystemApp) {
        removeLibrary(parsedPackage, LIBRARY_NAME);
    }
}
