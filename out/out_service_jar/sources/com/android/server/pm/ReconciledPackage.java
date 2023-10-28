package com.android.server.pm;

import android.content.pm.SharedLibraryInfo;
import android.content.pm.SigningDetails;
import android.util.ArrayMap;
import com.android.server.pm.parsing.pkg.AndroidPackage;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
/* loaded from: classes2.dex */
final class ReconciledPackage {
    public final List<SharedLibraryInfo> mAllowedSharedLibraryInfos;
    public ArrayList<SharedLibraryInfo> mCollectedSharedLibraryInfos;
    public final DeletePackageAction mDeletePackageAction;
    public final InstallArgs mInstallArgs;
    public final PackageInstalledInfo mInstallResult;
    public final PackageSetting mPkgSetting;
    public final PrepareResult mPrepareResult;
    public final boolean mRemoveAppKeySetData;
    public final ReconcileRequest mRequest;
    public final ScanResult mScanResult;
    public final boolean mSharedUserSignaturesChanged;
    public final SigningDetails mSigningDetails;

    /* JADX INFO: Access modifiers changed from: package-private */
    public ReconciledPackage(ReconcileRequest request, InstallArgs installArgs, PackageSetting pkgSetting, PackageInstalledInfo installResult, PrepareResult prepareResult, ScanResult scanResult, DeletePackageAction deletePackageAction, List<SharedLibraryInfo> allowedSharedLibraryInfos, SigningDetails signingDetails, boolean sharedUserSignaturesChanged, boolean removeAppKeySetData) {
        this.mRequest = request;
        this.mInstallArgs = installArgs;
        this.mPkgSetting = pkgSetting;
        this.mInstallResult = installResult;
        this.mPrepareResult = prepareResult;
        this.mScanResult = scanResult;
        this.mDeletePackageAction = deletePackageAction;
        this.mAllowedSharedLibraryInfos = allowedSharedLibraryInfos;
        this.mSigningDetails = signingDetails;
        this.mSharedUserSignaturesChanged = sharedUserSignaturesChanged;
        this.mRemoveAppKeySetData = removeAppKeySetData;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public Map<String, AndroidPackage> getCombinedAvailablePackages() {
        ArrayMap<String, AndroidPackage> combined = new ArrayMap<>(this.mRequest.mAllPackages.size() + this.mRequest.mScannedPackages.size());
        combined.putAll(this.mRequest.mAllPackages);
        for (ScanResult scanResult : this.mRequest.mScannedPackages.values()) {
            combined.put(scanResult.mPkgSetting.getPackageName(), scanResult.mRequest.mParsedPackage);
        }
        return combined;
    }
}
