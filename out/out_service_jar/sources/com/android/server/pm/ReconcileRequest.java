package com.android.server.pm;

import com.android.server.pm.Settings;
import com.android.server.pm.parsing.pkg.AndroidPackage;
import java.util.Collections;
import java.util.Map;
/* loaded from: classes2.dex */
final class ReconcileRequest {
    public final Map<String, AndroidPackage> mAllPackages;
    public final Map<String, InstallArgs> mInstallArgs;
    public final Map<String, PackageInstalledInfo> mInstallResults;
    public final Map<String, PrepareResult> mPreparedPackages;
    public final Map<String, ScanResult> mScannedPackages;
    public final Map<String, Settings.VersionInfo> mVersionInfos;

    /* JADX INFO: Access modifiers changed from: package-private */
    public ReconcileRequest(Map<String, ScanResult> scannedPackages, Map<String, InstallArgs> installArgs, Map<String, PackageInstalledInfo> installResults, Map<String, PrepareResult> preparedPackages, Map<String, AndroidPackage> allPackages, Map<String, Settings.VersionInfo> versionInfos) {
        this.mScannedPackages = scannedPackages;
        this.mInstallArgs = installArgs;
        this.mInstallResults = installResults;
        this.mPreparedPackages = preparedPackages;
        this.mAllPackages = allPackages;
        this.mVersionInfos = versionInfos;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public ReconcileRequest(Map<String, ScanResult> scannedPackages, Map<String, AndroidPackage> allPackages, Map<String, Settings.VersionInfo> versionInfos) {
        this(scannedPackages, Collections.emptyMap(), Collections.emptyMap(), Collections.emptyMap(), allPackages, versionInfos);
    }
}
