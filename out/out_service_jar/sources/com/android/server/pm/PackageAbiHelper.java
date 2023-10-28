package com.android.server.pm;

import android.util.ArraySet;
import android.util.Pair;
import com.android.server.pm.parsing.pkg.AndroidPackage;
import com.android.server.pm.parsing.pkg.AndroidPackageUtils;
import com.android.server.pm.parsing.pkg.ParsedPackage;
import com.android.server.pm.pkg.PackageStateInternal;
import java.io.File;
/* loaded from: classes2.dex */
public interface PackageAbiHelper {
    NativeLibraryPaths deriveNativeLibraryPaths(AndroidPackage androidPackage, boolean z, File file);

    Pair<Abis, NativeLibraryPaths> derivePackageAbi(AndroidPackage androidPackage, boolean z, String str, File file) throws PackageManagerException;

    String getAdjustedAbiForSharedUser(ArraySet<? extends PackageStateInternal> arraySet, AndroidPackage androidPackage);

    Abis getBundledAppAbis(AndroidPackage androidPackage);

    /* loaded from: classes2.dex */
    public static final class NativeLibraryPaths {
        public final String nativeLibraryDir;
        public final String nativeLibraryRootDir;
        public final boolean nativeLibraryRootRequiresIsa;
        public final String secondaryNativeLibraryDir;

        /* JADX INFO: Access modifiers changed from: package-private */
        public NativeLibraryPaths(String nativeLibraryRootDir, boolean nativeLibraryRootRequiresIsa, String nativeLibraryDir, String secondaryNativeLibraryDir) {
            this.nativeLibraryRootDir = nativeLibraryRootDir;
            this.nativeLibraryRootRequiresIsa = nativeLibraryRootRequiresIsa;
            this.nativeLibraryDir = nativeLibraryDir;
            this.secondaryNativeLibraryDir = secondaryNativeLibraryDir;
        }

        public void applyTo(ParsedPackage pkg) {
            pkg.setNativeLibraryRootDir(this.nativeLibraryRootDir).setNativeLibraryRootRequiresIsa(this.nativeLibraryRootRequiresIsa).setNativeLibraryDir(this.nativeLibraryDir).setSecondaryNativeLibraryDir(this.secondaryNativeLibraryDir);
        }
    }

    /* loaded from: classes2.dex */
    public static final class Abis {
        public final String primary;
        public final String secondary;

        /* JADX INFO: Access modifiers changed from: package-private */
        public Abis(String primary, String secondary) {
            this.primary = primary;
            this.secondary = secondary;
        }

        Abis(AndroidPackage pkg, PackageSetting pkgSetting) {
            this(AndroidPackageUtils.getPrimaryCpuAbi(pkg, pkgSetting), AndroidPackageUtils.getSecondaryCpuAbi(pkg, pkgSetting));
        }

        public void applyTo(ParsedPackage pkg) {
            pkg.setPrimaryCpuAbi(this.primary).setSecondaryCpuAbi(this.secondary);
        }

        public void applyTo(PackageSetting pkgSetting) {
            if (pkgSetting != null) {
                pkgSetting.setPrimaryCpuAbi(this.primary);
                pkgSetting.setSecondaryCpuAbi(this.secondary);
            }
        }
    }
}
