package com.android.server.pm.dex;

import com.android.server.pm.InstructionSets;
import com.android.server.pm.PackageDexOptimizer;
import com.android.server.pm.parsing.pkg.AndroidPackage;
import com.android.server.pm.parsing.pkg.AndroidPackageUtils;
import com.android.server.pm.pkg.PackageStateInternal;
import java.io.File;
import java.util.Arrays;
/* loaded from: classes2.dex */
public final class ArtUtils {
    private ArtUtils() {
    }

    public static ArtPackageInfo createArtPackageInfo(AndroidPackage pkg, PackageStateInternal pkgSetting) {
        return new ArtPackageInfo(pkg.getPackageName(), Arrays.asList(InstructionSets.getAppDexInstructionSets(AndroidPackageUtils.getPrimaryCpuAbi(pkg, pkgSetting), AndroidPackageUtils.getSecondaryCpuAbi(pkg, pkgSetting))), AndroidPackageUtils.getAllCodePaths(pkg), getOatDir(pkg, pkgSetting));
    }

    private static String getOatDir(AndroidPackage pkg, PackageStateInternal pkgSetting) {
        if (AndroidPackageUtils.canHaveOatDir(pkg, pkgSetting.getTransientState().isUpdatedSystemApp())) {
            File codePath = new File(pkg.getPath());
            if (codePath.isDirectory()) {
                return PackageDexOptimizer.getOatDir(codePath).getAbsolutePath();
            }
            return null;
        }
        return null;
    }
}
