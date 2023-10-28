package com.android.server.pm.parsing.library;

import com.android.server.pm.parsing.pkg.ParsedPackage;
/* loaded from: classes2.dex */
public class AndroidHidlUpdater extends PackageSharedLibraryUpdater {
    @Override // com.android.server.pm.parsing.library.PackageSharedLibraryUpdater
    public void updatePackage(ParsedPackage parsedPackage, boolean isUpdatedSystemApp) {
        boolean isSystem = true;
        boolean isLegacy = parsedPackage.getTargetSdkVersion() <= 28;
        if (!parsedPackage.isSystem() && !isUpdatedSystemApp) {
            isSystem = false;
        }
        if (isLegacy && isSystem) {
            prefixRequiredLibrary(parsedPackage, "android.hidl.base-V1.0-java");
            prefixRequiredLibrary(parsedPackage, "android.hidl.manager-V1.0-java");
            return;
        }
        removeLibrary(parsedPackage, "android.hidl.base-V1.0-java");
        removeLibrary(parsedPackage, "android.hidl.manager-V1.0-java");
    }
}
