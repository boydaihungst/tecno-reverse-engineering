package com.android.server.pm.dex;

import android.os.Binder;
import android.os.UserHandle;
import android.util.Log;
import com.android.server.pm.Installer;
import com.android.server.pm.parsing.pkg.AndroidPackage;
import com.android.server.pm.pkg.parsing.PackageInfoWithoutStateUtils;
import java.io.File;
/* loaded from: classes2.dex */
public class ViewCompiler {
    private final Object mInstallLock;
    private final Installer mInstaller;

    public ViewCompiler(Object installLock, Installer installer) {
        this.mInstallLock = installLock;
        this.mInstaller = installer;
    }

    public boolean compileLayouts(AndroidPackage pkg) {
        boolean compileLayouts;
        try {
            String packageName = pkg.getPackageName();
            String apkPath = pkg.getBaseApkPath();
            File dataDir = PackageInfoWithoutStateUtils.getDataDir(pkg, UserHandle.myUserId());
            String outDexFile = dataDir.getAbsolutePath() + "/code_cache/compiled_view.dex";
            Log.i("PackageManager", "Compiling layouts in " + packageName + " (" + apkPath + ") to " + outDexFile);
            long callingId = Binder.clearCallingIdentity();
            synchronized (this.mInstallLock) {
                compileLayouts = this.mInstaller.compileLayouts(apkPath, packageName, outDexFile, pkg.getUid());
            }
            Binder.restoreCallingIdentity(callingId);
            return compileLayouts;
        } catch (Throwable e) {
            Log.e("PackageManager", "Failed to compile layouts", e);
            return false;
        }
    }
}
