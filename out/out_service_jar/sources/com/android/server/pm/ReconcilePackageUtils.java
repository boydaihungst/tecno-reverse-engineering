package com.android.server.pm;

import android.content.pm.SharedLibraryInfo;
import android.content.pm.SigningDetails;
import android.os.SystemProperties;
import android.util.ArrayMap;
import com.android.server.pm.Settings;
import com.android.server.pm.parsing.pkg.AndroidPackage;
import com.android.server.pm.parsing.pkg.ParsedPackage;
import com.android.server.utils.WatchedLongSparseArray;
import java.util.List;
import java.util.Map;
/* JADX INFO: Access modifiers changed from: package-private */
/* loaded from: classes2.dex */
public final class ReconcilePackageUtils {
    ReconcilePackageUtils() {
    }

    /* JADX DEBUG: Don't trust debug lines info. Repeating lines: [198=5, 250=5] */
    /* JADX WARN: Removed duplicated region for block: B:112:0x029e  */
    /* JADX WARN: Removed duplicated region for block: B:126:0x02c5  */
    /* JADX WARN: Removed duplicated region for block: B:184:0x03ad A[SYNTHETIC] */
    /* JADX WARN: Removed duplicated region for block: B:81:0x0214  */
    /* JADX WARN: Removed duplicated region for block: B:84:0x021d  */
    /*
        Code decompiled incorrectly, please refer to instructions dump.
    */
    public static Map<String, ReconciledPackage> reconcilePackages(ReconcileRequest request, SharedLibrariesImpl sharedLibraries, KeySetManagerService ksms, Settings settings) throws ReconcileFailure {
        PackageInstalledInfo res;
        DeletePackageAction deletePackageAction;
        PackageSetting signatureCheckPs;
        ArrayMap<String, AndroidPackage> combinedPackages;
        SigningDetails signingDetails;
        SigningDetails signingDetails2;
        boolean isRollback;
        SigningDetails sharedSigningDetails;
        boolean compatMatch;
        ReconcileRequest reconcileRequest = request;
        SharedLibrariesImpl sharedLibrariesImpl = sharedLibraries;
        KeySetManagerService keySetManagerService = ksms;
        Map<String, ScanResult> scannedPackages = reconcileRequest.mScannedPackages;
        Map<String, ReconciledPackage> result = new ArrayMap<>(scannedPackages.size());
        ArrayMap<String, AndroidPackage> combinedPackages2 = new ArrayMap<>(reconcileRequest.mAllPackages.size() + scannedPackages.size());
        combinedPackages2.putAll(reconcileRequest.mAllPackages);
        Map<String, WatchedLongSparseArray<SharedLibraryInfo>> incomingSharedLibraries = new ArrayMap<>();
        for (String installPackageName : scannedPackages.keySet()) {
            ScanResult scanResult = scannedPackages.get(installPackageName);
            combinedPackages2.put(scanResult.mPkgSetting.getPackageName(), scanResult.mRequest.mParsedPackage);
            List<SharedLibraryInfo> allowedSharedLibInfos = sharedLibrariesImpl.getAllowedSharedLibInfos(scanResult);
            if (allowedSharedLibInfos != null) {
                for (SharedLibraryInfo info : allowedSharedLibInfos) {
                    if (!SharedLibraryUtils.addSharedLibraryToPackageVersionMap(incomingSharedLibraries, info)) {
                        throw new ReconcileFailure("Shared Library " + info.getName() + " is being installed twice in this set!");
                    }
                }
            }
            InstallArgs installArgs = reconcileRequest.mInstallArgs.get(installPackageName);
            PackageInstalledInfo res2 = reconcileRequest.mInstallResults.get(installPackageName);
            PrepareResult prepareResult = reconcileRequest.mPreparedPackages.get(installPackageName);
            boolean isInstall = installArgs != null;
            if (isInstall && (res2 == null || prepareResult == null)) {
                throw new ReconcileFailure("Reconcile arguments are not balanced for " + installPackageName + "!");
            }
            if (isInstall && prepareResult.mReplace && !prepareResult.mSystem) {
                boolean killApp = (scanResult.mRequest.mScanFlags & 1024) == 0;
                int deleteFlags = (killApp ? 0 : 8) | 1;
                res = res2;
                DeletePackageAction deletePackageAction2 = DeletePackageHelper.mayDeletePackageLocked(res2.mRemovedInfo, prepareResult.mOriginalPs, prepareResult.mDisabledPs, deleteFlags, null);
                if (deletePackageAction2 == null) {
                    throw new ReconcileFailure(-10, "May not delete " + installPackageName + " to replace");
                }
                deletePackageAction = deletePackageAction2;
            } else {
                res = res2;
                deletePackageAction = null;
            }
            int scanFlags = scanResult.mRequest.mScanFlags;
            int parseFlags = scanResult.mRequest.mParseFlags;
            ParsedPackage parsedPackage = scanResult.mRequest.mParsedPackage;
            Map<String, ScanResult> scannedPackages2 = scannedPackages;
            PackageSetting disabledPkgSetting = scanResult.mRequest.mDisabledPkgSetting;
            PackageSetting lastStaticSharedLibSetting = scanResult.mStaticSharedLibraryInfo != null ? sharedLibrariesImpl.getStaticSharedLibLatestVersionSetting(scanResult) : null;
            PackageSetting signatureCheckPs2 = (prepareResult == null || lastStaticSharedLibSetting == null) ? scanResult.mPkgSetting : lastStaticSharedLibSetting;
            boolean sharedUserSignaturesChanged = false;
            Map<String, ReconciledPackage> result2 = result;
            SharedUserSetting sharedUserSetting = settings.getSharedUserSettingLPr(signatureCheckPs2);
            Map<String, WatchedLongSparseArray<SharedLibraryInfo>> incomingSharedLibraries2 = incomingSharedLibraries;
            if (keySetManagerService.shouldCheckUpgradeKeySetLocked(signatureCheckPs2, sharedUserSetting, scanFlags)) {
                if (!keySetManagerService.checkUpgradeKeySetLocked(signatureCheckPs2, parsedPackage)) {
                    if ((parseFlags & 16) == 0) {
                        throw new ReconcileFailure(-7, "Package " + parsedPackage.getPackageName() + " upgrade keys do not match the previously installed version");
                    }
                    String msg = "System package " + parsedPackage.getPackageName() + " signature changed; retaining data.";
                    PackageManagerService.reportSettingsProblem(5, msg);
                }
                signingDetails2 = parsedPackage.getSigningDetails();
                signatureCheckPs = signatureCheckPs2;
                combinedPackages = combinedPackages2;
            } else {
                try {
                    Settings.VersionInfo versionInfo = reconcileRequest.mVersionInfos.get(installPackageName);
                    boolean compareCompat = isCompatSignatureUpdateNeeded(versionInfo);
                    boolean compareRecover = isRecoverSignatureUpdateNeeded(versionInfo);
                    try {
                        if (installArgs != null) {
                            try {
                                combinedPackages = combinedPackages2;
                                if (installArgs.mInstallReason == 5) {
                                    isRollback = true;
                                    boolean compatMatch2 = PackageManagerServiceUtils.verifySignatures(signatureCheckPs2, sharedUserSetting, disabledPkgSetting, parsedPackage.getSigningDetails(), compareCompat, compareRecover, isRollback);
                                    removeAppKeySetData = compatMatch2;
                                    SigningDetails signingDetails3 = parsedPackage.getSigningDetails();
                                    if (sharedUserSetting == null) {
                                        try {
                                            SigningDetails sharedSigningDetails2 = sharedUserSetting.signatures.mSigningDetails;
                                            signatureCheckPs = signatureCheckPs2;
                                            SigningDetails mergedDetails = sharedSigningDetails2.mergeLineageWith(signingDetails3);
                                            if (mergedDetails != sharedSigningDetails2) {
                                                try {
                                                    for (AndroidPackage androidPackage : sharedUserSetting.getPackages()) {
                                                        if (androidPackage.getPackageName() != null) {
                                                            sharedSigningDetails = sharedSigningDetails2;
                                                            compatMatch = compatMatch2;
                                                            if (!androidPackage.getPackageName().equals(parsedPackage.getPackageName())) {
                                                                mergedDetails = mergedDetails.mergeLineageWith(androidPackage.getSigningDetails(), 2);
                                                            }
                                                        } else {
                                                            sharedSigningDetails = sharedSigningDetails2;
                                                            compatMatch = compatMatch2;
                                                        }
                                                        sharedSigningDetails2 = sharedSigningDetails;
                                                        compatMatch2 = compatMatch;
                                                    }
                                                    sharedUserSetting.signatures.mSigningDetails = mergedDetails;
                                                } catch (PackageManagerException e) {
                                                    e = e;
                                                    if ((parseFlags & 16) != 0) {
                                                        throw new ReconcileFailure(e);
                                                    }
                                                    SigningDetails signingDetails4 = parsedPackage.getSigningDetails();
                                                    if (sharedUserSetting == null) {
                                                        signingDetails = signingDetails4;
                                                    } else if (sharedUserSetting.signaturesChanged != null && !PackageManagerServiceUtils.canJoinSharedUserId(parsedPackage.getSigningDetails(), sharedUserSetting.getSigningDetails())) {
                                                        if (SystemProperties.getInt("ro.product.first_api_level", 0) <= 29) {
                                                            throw new ReconcileFailure(-104, "Signature mismatch for shared user: " + sharedUserSetting);
                                                        }
                                                        throw new IllegalStateException("Signature mismatch on system package " + parsedPackage.getPackageName() + " for shared user " + sharedUserSetting);
                                                    } else {
                                                        signingDetails = signingDetails4;
                                                        sharedUserSetting.signatures.mSigningDetails = parsedPackage.getSigningDetails();
                                                        sharedUserSetting.signaturesChanged = Boolean.TRUE;
                                                        sharedUserSignaturesChanged = true;
                                                    }
                                                    String msg2 = "System package " + parsedPackage.getPackageName() + " signature changed; retaining data.";
                                                    PackageManagerService.reportSettingsProblem(5, msg2);
                                                    signingDetails2 = signingDetails;
                                                    result2.put(installPackageName, new ReconciledPackage(request, installArgs, scanResult.mPkgSetting, res, reconcileRequest.mPreparedPackages.get(installPackageName), scanResult, deletePackageAction, allowedSharedLibInfos, signingDetails2, sharedUserSignaturesChanged, removeAppKeySetData));
                                                    reconcileRequest = request;
                                                    sharedLibrariesImpl = sharedLibraries;
                                                    result = result2;
                                                    scannedPackages = scannedPackages2;
                                                    incomingSharedLibraries = incomingSharedLibraries2;
                                                    combinedPackages2 = combinedPackages;
                                                    keySetManagerService = ksms;
                                                } catch (IllegalArgumentException e2) {
                                                    e = e2;
                                                    throw new RuntimeException("Signing certificates comparison made on incomparable signing details but somehow passed verifySignatures!", e);
                                                }
                                            }
                                            if (sharedUserSetting.signaturesChanged == null) {
                                                sharedUserSetting.signaturesChanged = Boolean.FALSE;
                                            }
                                        } catch (PackageManagerException e3) {
                                            e = e3;
                                            signatureCheckPs = signatureCheckPs2;
                                        } catch (IllegalArgumentException e4) {
                                            e = e4;
                                        }
                                    } else {
                                        signatureCheckPs = signatureCheckPs2;
                                    }
                                    signingDetails2 = signingDetails3;
                                }
                            } catch (PackageManagerException e5) {
                                e = e5;
                                combinedPackages = combinedPackages2;
                                signatureCheckPs = signatureCheckPs2;
                                if ((parseFlags & 16) != 0) {
                                }
                            } catch (IllegalArgumentException e6) {
                                e = e6;
                                throw new RuntimeException("Signing certificates comparison made on incomparable signing details but somehow passed verifySignatures!", e);
                            }
                        } else {
                            combinedPackages = combinedPackages2;
                        }
                        boolean compatMatch22 = PackageManagerServiceUtils.verifySignatures(signatureCheckPs2, sharedUserSetting, disabledPkgSetting, parsedPackage.getSigningDetails(), compareCompat, compareRecover, isRollback);
                        if (compatMatch22) {
                        }
                        SigningDetails signingDetails32 = parsedPackage.getSigningDetails();
                        if (sharedUserSetting == null) {
                        }
                        signingDetails2 = signingDetails32;
                    } catch (PackageManagerException e7) {
                        e = e7;
                        signatureCheckPs = signatureCheckPs2;
                    } catch (IllegalArgumentException e8) {
                        e = e8;
                    }
                    isRollback = false;
                } catch (PackageManagerException e9) {
                    e = e9;
                    signatureCheckPs = signatureCheckPs2;
                    combinedPackages = combinedPackages2;
                } catch (IllegalArgumentException e10) {
                    e = e10;
                }
            }
            result2.put(installPackageName, new ReconciledPackage(request, installArgs, scanResult.mPkgSetting, res, reconcileRequest.mPreparedPackages.get(installPackageName), scanResult, deletePackageAction, allowedSharedLibInfos, signingDetails2, sharedUserSignaturesChanged, removeAppKeySetData));
            reconcileRequest = request;
            sharedLibrariesImpl = sharedLibraries;
            result = result2;
            scannedPackages = scannedPackages2;
            incomingSharedLibraries = incomingSharedLibraries2;
            combinedPackages2 = combinedPackages;
            keySetManagerService = ksms;
        }
        Map<String, WatchedLongSparseArray<SharedLibraryInfo>> map = incomingSharedLibraries;
        ArrayMap<String, AndroidPackage> combinedPackages3 = combinedPackages2;
        Map<String, ReconciledPackage> result3 = result;
        Map<String, ScanResult> scannedPackages3 = scannedPackages;
        for (String installPackageName2 : scannedPackages3.keySet()) {
            Map<String, ScanResult> scannedPackages4 = scannedPackages3;
            ScanResult scanResult2 = scannedPackages4.get(installPackageName2);
            if ((scanResult2.mRequest.mScanFlags & 16) != 0) {
                scannedPackages3 = scannedPackages4;
            } else if ((scanResult2.mRequest.mParseFlags & 16) != 0) {
                scannedPackages3 = scannedPackages4;
            } else {
                try {
                    Map<String, WatchedLongSparseArray<SharedLibraryInfo>> map2 = map;
                    ArrayMap<String, AndroidPackage> combinedPackages4 = combinedPackages3;
                    try {
                        result3.get(installPackageName2).mCollectedSharedLibraryInfos = sharedLibraries.collectSharedLibraryInfos(scanResult2.mRequest.mParsedPackage, combinedPackages4, map2);
                        scannedPackages3 = scannedPackages4;
                        combinedPackages3 = combinedPackages4;
                        map = map2;
                    } catch (PackageManagerException e11) {
                        e = e11;
                        throw new ReconcileFailure(e.error, e.getMessage());
                    }
                } catch (PackageManagerException e12) {
                    e = e12;
                }
            }
        }
        return result3;
    }

    public static boolean isCompatSignatureUpdateNeeded(Settings.VersionInfo ver) {
        return ver.databaseVersion < 2;
    }

    public static boolean isRecoverSignatureUpdateNeeded(Settings.VersionInfo ver) {
        return ver.databaseVersion < 3;
    }
}
