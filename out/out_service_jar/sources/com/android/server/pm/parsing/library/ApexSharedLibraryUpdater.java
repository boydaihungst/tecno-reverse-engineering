package com.android.server.pm.parsing.library;

import android.util.ArrayMap;
import com.android.modules.utils.build.UnboundedSdkLevel;
import com.android.server.SystemConfig;
import com.android.server.pm.parsing.pkg.ParsedPackage;
/* loaded from: classes2.dex */
public class ApexSharedLibraryUpdater extends PackageSharedLibraryUpdater {
    private final ArrayMap<String, SystemConfig.SharedLibraryEntry> mSharedLibraries;

    public ApexSharedLibraryUpdater(ArrayMap<String, SystemConfig.SharedLibraryEntry> sharedLibraries) {
        this.mSharedLibraries = sharedLibraries;
    }

    @Override // com.android.server.pm.parsing.library.PackageSharedLibraryUpdater
    public void updatePackage(ParsedPackage parsedPackage, boolean isUpdatedSystemApp) {
        int builtInLibCount = this.mSharedLibraries.size();
        for (int i = 0; i < builtInLibCount; i++) {
            updateSharedLibraryForPackage(this.mSharedLibraries.valueAt(i), parsedPackage);
        }
    }

    private void updateSharedLibraryForPackage(SystemConfig.SharedLibraryEntry entry, ParsedPackage parsedPackage) {
        if (entry.onBootclasspathBefore != null && isTargetSdkAtMost(parsedPackage.getTargetSdkVersion(), entry.onBootclasspathBefore) && UnboundedSdkLevel.isAtLeast(entry.onBootclasspathBefore)) {
            prefixRequiredLibrary(parsedPackage, entry.name);
        }
        if (entry.canBeSafelyIgnored) {
            removeLibrary(parsedPackage, entry.name);
        }
    }

    private static boolean isTargetSdkAtMost(int targetSdk, String onBcpBefore) {
        return isCodename(onBcpBefore) ? targetSdk < 10000 : targetSdk < Integer.parseInt(onBcpBefore);
    }

    private static boolean isCodename(String version) {
        if (version.length() == 0) {
            throw new IllegalArgumentException();
        }
        return Character.isUpperCase(version.charAt(0));
    }
}
