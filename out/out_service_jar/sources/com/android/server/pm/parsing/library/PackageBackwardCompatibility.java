package com.android.server.pm.parsing.library;

import android.util.Log;
import com.android.server.SystemConfig;
import com.android.server.pm.parsing.pkg.ParsedPackage;
import com.android.server.pm.pkg.parsing.ParsingPackage;
import java.util.ArrayList;
import java.util.List;
/* loaded from: classes2.dex */
public class PackageBackwardCompatibility extends PackageSharedLibraryUpdater {
    private static final PackageBackwardCompatibility INSTANCE;
    private static final String TAG = PackageBackwardCompatibility.class.getSimpleName();
    private final boolean mBootClassPathContainsATB;
    private final PackageSharedLibraryUpdater[] mPackageUpdaters;

    static {
        List<PackageSharedLibraryUpdater> packageUpdaters = new ArrayList<>();
        packageUpdaters.add(new AndroidNetIpSecIkeUpdater());
        packageUpdaters.add(new ComGoogleAndroidMapsUpdater());
        packageUpdaters.add(new OrgApacheHttpLegacyUpdater());
        packageUpdaters.add(new AndroidHidlUpdater());
        packageUpdaters.add(new AndroidTestRunnerSplitUpdater());
        boolean bootClassPathContainsATB = !addUpdaterForAndroidTestBase(packageUpdaters);
        packageUpdaters.add(new ApexSharedLibraryUpdater(SystemConfig.getInstance().getSharedLibraries()));
        PackageSharedLibraryUpdater[] updaterArray = (PackageSharedLibraryUpdater[]) packageUpdaters.toArray(new PackageSharedLibraryUpdater[0]);
        INSTANCE = new PackageBackwardCompatibility(bootClassPathContainsATB, updaterArray);
    }

    private static boolean addUpdaterForAndroidTestBase(List<PackageSharedLibraryUpdater> packageUpdaters) {
        boolean hasClass = false;
        try {
            Class clazz = ParsingPackage.class.getClassLoader().loadClass("android.content.pm.AndroidTestBaseUpdater");
            hasClass = clazz != null;
            Log.i(TAG, "Loaded android.content.pm.AndroidTestBaseUpdater");
        } catch (ClassNotFoundException e) {
            Log.i(TAG, "Could not find android.content.pm.AndroidTestBaseUpdater, ignoring");
        }
        if (hasClass) {
            packageUpdaters.add(new AndroidTestBaseUpdater());
        } else {
            packageUpdaters.add(new RemoveUnnecessaryAndroidTestBaseLibrary());
        }
        return hasClass;
    }

    public static PackageSharedLibraryUpdater getInstance() {
        return INSTANCE;
    }

    PackageSharedLibraryUpdater[] getPackageUpdaters() {
        return this.mPackageUpdaters;
    }

    private PackageBackwardCompatibility(boolean bootClassPathContainsATB, PackageSharedLibraryUpdater[] packageUpdaters) {
        this.mBootClassPathContainsATB = bootClassPathContainsATB;
        this.mPackageUpdaters = packageUpdaters;
    }

    public static void modifySharedLibraries(ParsedPackage parsedPackage, boolean isUpdatedSystemApp) {
        INSTANCE.updatePackage(parsedPackage, isUpdatedSystemApp);
    }

    @Override // com.android.server.pm.parsing.library.PackageSharedLibraryUpdater
    public void updatePackage(ParsedPackage parsedPackage, boolean isUpdatedSystemApp) {
        PackageSharedLibraryUpdater[] packageSharedLibraryUpdaterArr;
        for (PackageSharedLibraryUpdater packageUpdater : this.mPackageUpdaters) {
            packageUpdater.updatePackage(parsedPackage, isUpdatedSystemApp);
        }
    }

    public static boolean bootClassPathContainsATB() {
        return INSTANCE.mBootClassPathContainsATB;
    }

    /* loaded from: classes2.dex */
    public static class AndroidTestRunnerSplitUpdater extends PackageSharedLibraryUpdater {
        @Override // com.android.server.pm.parsing.library.PackageSharedLibraryUpdater
        public void updatePackage(ParsedPackage parsedPackage, boolean isUpdatedSystemApp) {
            prefixImplicitDependency(parsedPackage, "android.test.runner", "android.test.mock");
        }
    }

    /* loaded from: classes2.dex */
    public static class RemoveUnnecessaryOrgApacheHttpLegacyLibrary extends PackageSharedLibraryUpdater {
        @Override // com.android.server.pm.parsing.library.PackageSharedLibraryUpdater
        public void updatePackage(ParsedPackage parsedPackage, boolean isUpdatedSystemApp) {
            removeLibrary(parsedPackage, SharedLibraryNames.ORG_APACHE_HTTP_LEGACY);
        }
    }

    /* loaded from: classes2.dex */
    public static class RemoveUnnecessaryAndroidTestBaseLibrary extends PackageSharedLibraryUpdater {
        @Override // com.android.server.pm.parsing.library.PackageSharedLibraryUpdater
        public void updatePackage(ParsedPackage parsedPackage, boolean isUpdatedSystemApp) {
            removeLibrary(parsedPackage, "android.test.base");
        }
    }
}
