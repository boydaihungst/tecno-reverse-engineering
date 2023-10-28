package com.android.server.pm.parsing.library;

import android.os.RemoteException;
import android.os.ServiceManager;
import android.util.Log;
import com.android.internal.compat.IPlatformCompat;
import com.android.server.pm.parsing.pkg.AndroidPackage;
import com.android.server.pm.parsing.pkg.AndroidPackageUtils;
import com.android.server.pm.parsing.pkg.ParsedPackage;
/* loaded from: classes2.dex */
public class AndroidTestBaseUpdater extends PackageSharedLibraryUpdater {
    private static final long REMOVE_ANDROID_TEST_BASE = 133396946;
    private static final String TAG = "AndroidTestBaseUpdater";

    private static boolean isChangeEnabled(AndroidPackage pkg) {
        if (!pkg.isSystem()) {
            IPlatformCompat platformCompat = IPlatformCompat.Stub.asInterface(ServiceManager.getService("platform_compat"));
            try {
                return platformCompat.isChangeEnabled((long) REMOVE_ANDROID_TEST_BASE, AndroidPackageUtils.generateAppInfoWithoutState(pkg));
            } catch (RemoteException | NullPointerException e) {
                Log.e(TAG, "Failed to get a response from PLATFORM_COMPAT_SERVICE", e);
            }
        }
        return pkg.getTargetSdkVersion() > 29;
    }

    @Override // com.android.server.pm.parsing.library.PackageSharedLibraryUpdater
    public void updatePackage(ParsedPackage pkg, boolean isUpdatedSystemApp) {
        if (!isChangeEnabled(pkg)) {
            prefixRequiredLibrary(pkg, "android.test.base");
        } else {
            prefixImplicitDependency(pkg, "android.test.runner", "android.test.base");
        }
    }
}
