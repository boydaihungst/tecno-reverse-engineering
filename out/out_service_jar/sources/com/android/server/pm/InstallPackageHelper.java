package com.android.server.pm;

import android.app.AppOpsManager;
import android.app.ApplicationPackageManager;
import android.app.BroadcastOptions;
import android.app.backup.IBackupManager;
import android.content.ContentResolver;
import android.content.Context;
import android.content.IIntentReceiver;
import android.content.Intent;
import android.content.IntentSender;
import android.content.pm.IPackageInstallObserver2;
import android.content.pm.PackageChangeEvent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageInfoLite;
import android.content.pm.PackageManager;
import android.content.pm.SharedLibraryInfo;
import android.content.pm.Signature;
import android.content.pm.SigningDetails;
import android.content.pm.VerifierInfo;
import android.content.pm.dex.DexMetadataHelper;
import android.content.pm.parsing.ApkLiteParseUtils;
import android.hardware.biometrics.fingerprint.V2_1.RequestStatus;
import android.os.Binder;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Message;
import android.os.RemoteException;
import android.os.SystemProperties;
import android.os.Trace;
import android.os.UserHandle;
import android.os.incremental.IncrementalManager;
import android.os.incremental.IncrementalStorage;
import android.os.storage.StorageManager;
import android.os.storage.VolumeInfo;
import android.provider.Settings;
import android.util.ArrayMap;
import android.util.ArraySet;
import android.util.EventLog;
import android.util.Log;
import android.util.Pair;
import android.util.Slog;
import android.util.SparseArray;
import com.android.internal.content.F2fsUtils;
import com.android.internal.security.VerityUtils;
import com.android.internal.util.ArrayUtils;
import com.android.internal.util.CollectionUtils;
import com.android.internal.util.FrameworkStatsLog;
import com.android.server.EventLogTags;
import com.android.server.UiModeManagerService;
import com.android.server.job.controllers.JobStatus;
import com.android.server.pm.Installer;
import com.android.server.pm.ParallelPackageParser;
import com.android.server.pm.dex.ArtManagerService;
import com.android.server.pm.dex.DexManager;
import com.android.server.pm.dex.DexoptOptions;
import com.android.server.pm.dex.ViewCompiler;
import com.android.server.pm.parsing.PackageCacher;
import com.android.server.pm.parsing.PackageParser2;
import com.android.server.pm.parsing.pkg.AndroidPackage;
import com.android.server.pm.parsing.pkg.AndroidPackageUtils;
import com.android.server.pm.parsing.pkg.ParsedPackage;
import com.android.server.pm.permission.PermissionManagerServiceInternal;
import com.android.server.pm.pkg.PackageStateInternal;
import com.android.server.pm.pkg.component.ComponentMutateUtils;
import com.android.server.pm.pkg.component.ParsedInstrumentation;
import com.android.server.rollback.RollbackManagerInternal;
import com.android.server.usb.descriptors.UsbTerminalTypes;
import com.android.server.utils.WatchedArrayMap;
import com.android.server.utils.WatchedLongSparseArray;
import com.mediatek.cta.CtaManagerFactory;
import com.mediatek.server.MtkSystemServiceFactory;
import com.mediatek.server.pm.hbtpackage.HBTPackage;
import com.mediatek.server.powerhal.PowerHalManager;
import com.transsion.hubcore.server.pm.ITranPackageManagerService;
import dalvik.system.VMRuntime;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.security.DigestException;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.security.cert.CertificateException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;
/* JADX INFO: Access modifiers changed from: package-private */
/* loaded from: classes2.dex */
public final class InstallPackageHelper {
    private final ApexManager mApexManager;
    private final AppDataHelper mAppDataHelper;
    private final ArtManagerService mArtManagerService;
    private final BroadcastHelper mBroadcastHelper;
    private final Context mContext;
    private final DexManager mDexManager;
    private final IncrementalManager mIncrementalManager;
    private final PackageManagerServiceInjector mInjector;
    private final PackageAbiHelper mPackageAbiHelper;
    private final PackageDexOptimizer mPackageDexOptimizer;
    private final PackageManagerService mPm;
    private final PowerHalManager mPowerHalManager;
    private final RemovePackageHelper mRemovePackageHelper;
    private final SharedLibrariesImpl mSharedLibraries;
    private final ViewCompiler mViewCompiler;

    /* JADX INFO: Access modifiers changed from: package-private */
    public InstallPackageHelper(PackageManagerService pm, AppDataHelper appDataHelper) {
        this.mPowerHalManager = MtkSystemServiceFactory.getInstance().makePowerHalManager();
        this.mPm = pm;
        this.mInjector = pm.mInjector;
        this.mAppDataHelper = appDataHelper;
        this.mBroadcastHelper = new BroadcastHelper(pm.mInjector);
        this.mRemovePackageHelper = new RemovePackageHelper(pm);
        this.mIncrementalManager = pm.mInjector.getIncrementalManager();
        this.mApexManager = pm.mInjector.getApexManager();
        this.mDexManager = pm.mInjector.getDexManager();
        this.mArtManagerService = pm.mInjector.getArtManagerService();
        this.mContext = pm.mInjector.getContext();
        this.mPackageDexOptimizer = pm.mInjector.getPackageDexOptimizer();
        this.mPackageAbiHelper = pm.mInjector.getAbiHelper();
        this.mViewCompiler = pm.mInjector.getViewCompiler();
        this.mSharedLibraries = pm.mInjector.getSharedLibrariesImpl();
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public InstallPackageHelper(PackageManagerService pm) {
        this(pm, new AppDataHelper(pm));
    }

    public AndroidPackage commitReconciledScanResultLocked(ReconciledPackage reconciledPkg, int[] allUsers) {
        PackageSetting pkgSetting;
        PackageSetting pkgSetting2;
        PackageSetting pkgSetting3;
        PackageSetting pkgSetting4;
        PackageSetting ips;
        ScanResult result = reconciledPkg.mScanResult;
        ScanRequest request = result.mRequest;
        ParsedPackage parsedPackage = request.mParsedPackage;
        if (PackageManagerService.PLATFORM_PACKAGE_NAME.equals(parsedPackage.getPackageName())) {
            parsedPackage.setVersionCode(this.mPm.getSdkVersion()).setVersionCodeMajor(0);
        }
        AndroidPackage oldPkg = request.mOldPkg;
        int parseFlags = request.mParseFlags;
        int scanFlags = request.mScanFlags;
        PackageSetting oldPkgSetting = request.mOldPkgSetting;
        PackageSetting originalPkgSetting = request.mOriginalPkgSetting;
        UserHandle user = request.mUser;
        String realPkgName = request.mRealPkgName;
        List<String> changedAbiCodePath = result.mChangedAbiCodePath;
        if (request.mPkgSetting != null) {
            SharedUserSetting requestSharedUserSetting = this.mPm.mSettings.getSharedUserSettingLPr(request.mPkgSetting);
            SharedUserSetting resultSharedUserSetting = this.mPm.mSettings.getSharedUserSettingLPr(result.mPkgSetting);
            if (requestSharedUserSetting != null && requestSharedUserSetting != resultSharedUserSetting) {
                requestSharedUserSetting.removePackage(request.mPkgSetting);
                if (this.mPm.mSettings.checkAndPruneSharedUserLPw(requestSharedUserSetting, false) && reconciledPkg.mInstallResult != null && reconciledPkg.mInstallResult.mRemovedInfo != null) {
                    reconciledPkg.mInstallResult.mRemovedInfo.mRemovedAppId = requestSharedUserSetting.mAppId;
                }
            }
        }
        if (result.mExistingSettingCopied) {
            PackageSetting pkgSetting5 = request.mPkgSetting;
            pkgSetting5.updateFrom(result.mPkgSetting);
            pkgSetting2 = pkgSetting5;
        } else {
            PackageSetting pkgSetting6 = result.mPkgSetting;
            if (originalPkgSetting != null) {
                pkgSetting = pkgSetting6;
                this.mPm.mSettings.addRenamedPackageLPw(AndroidPackageUtils.getRealPackageOrNull(parsedPackage), originalPkgSetting.getPackageName());
                this.mPm.mTransferredPackages.add(originalPkgSetting.getPackageName());
            } else {
                pkgSetting = pkgSetting6;
                this.mPm.mSettings.removeRenamedPackageLPw(parsedPackage.getPackageName());
            }
            pkgSetting2 = pkgSetting;
        }
        SharedUserSetting sharedUserSetting = this.mPm.mSettings.getSharedUserSettingLPr(pkgSetting2);
        if (sharedUserSetting != null) {
            sharedUserSetting.addPackage(pkgSetting2);
            if (parsedPackage.isLeavingSharedUid() && SharedUidMigration.applyStrategy(2) && sharedUserSetting.isSingleUser()) {
                this.mPm.mSettings.convertSharedUserSettingsLPw(sharedUserSetting);
            }
        }
        if (reconciledPkg.mInstallArgs != null && reconciledPkg.mInstallArgs.mForceQueryableOverride) {
            pkgSetting2.setForceQueryableOverride(true);
        }
        if (reconciledPkg.mInstallArgs != null) {
            InstallSource installSource = reconciledPkg.mInstallArgs.mInstallSource;
            if (installSource.initiatingPackageName != null && (ips = this.mPm.mSettings.getPackageLPr(installSource.initiatingPackageName)) != null) {
                installSource = installSource.setInitiatingPackageSignatures(ips.getSignatures());
            }
            pkgSetting2.setInstallSource(installSource);
            if (oldPkgSetting != null) {
                HBTPackage.HBTcheckUpdate(oldPkgSetting.getPackageName(), InstructionSets.getAppDexInstructionSets(pkgSetting2.getPrimaryCpuAbi(), pkgSetting2.getSecondaryCpuAbi()), InstructionSets.getAppDexInstructionSets(oldPkgSetting.getPrimaryCpuAbi(), oldPkgSetting.getSecondaryCpuAbi()));
            } else {
                HBTPackage.HBTcheckInstall(pkgSetting2.getPackageName(), InstructionSets.getAppDexInstructionSets(pkgSetting2.getPrimaryCpuAbi(), pkgSetting2.getSecondaryCpuAbi()));
            }
        }
        parsedPackage.setUid(pkgSetting2.getAppId());
        AndroidPackage pkg = parsedPackage.hideAsFinal();
        this.mPm.mSettings.writeUserRestrictionsLPw(pkgSetting2, oldPkgSetting);
        if (realPkgName != null) {
            this.mPm.mTransferredPackages.add(pkg.getPackageName());
        }
        if (reconciledPkg.mCollectedSharedLibraryInfos != null || (oldPkgSetting != null && oldPkgSetting.getUsesLibraryInfos() != null)) {
            this.mSharedLibraries.executeSharedLibrariesUpdateLPw(pkg, pkgSetting2, null, null, reconciledPkg.mCollectedSharedLibraryInfos, allUsers);
        }
        KeySetManagerService ksms = this.mPm.mSettings.getKeySetManagerService();
        if (reconciledPkg.mRemoveAppKeySetData) {
            ksms.removeAppKeySetDataLPw(pkg.getPackageName());
        }
        if (reconciledPkg.mSharedUserSignaturesChanged) {
            sharedUserSetting.signaturesChanged = Boolean.TRUE;
            sharedUserSetting.signatures.mSigningDetails = reconciledPkg.mSigningDetails;
        }
        pkgSetting2.setSigningDetails(reconciledPkg.mSigningDetails);
        if (changedAbiCodePath == null || changedAbiCodePath.size() <= 0) {
            pkgSetting3 = pkgSetting2;
        } else {
            int i = changedAbiCodePath.size() - 1;
            while (i >= 0) {
                SharedUserSetting sharedUserSetting2 = sharedUserSetting;
                String codePathString = changedAbiCodePath.get(i);
                try {
                    pkgSetting4 = pkgSetting2;
                    try {
                        this.mPm.mInstaller.rmdex(codePathString, InstructionSets.getDexCodeInstructionSet(InstructionSets.getPreferredInstructionSet()));
                    } catch (Installer.InstallerException e) {
                    }
                } catch (Installer.InstallerException e2) {
                    pkgSetting4 = pkgSetting2;
                }
                i--;
                sharedUserSetting = sharedUserSetting2;
                pkgSetting2 = pkgSetting4;
            }
            pkgSetting3 = pkgSetting2;
        }
        int userId = user == null ? 0 : user.getIdentifier();
        PackageSetting pkgSetting7 = pkgSetting3;
        commitPackageSettings(pkg, oldPkg, pkgSetting7, oldPkgSetting, scanFlags, (Integer.MIN_VALUE & parseFlags) != 0, reconciledPkg);
        if (pkgSetting7.getInstantApp(userId)) {
            this.mPm.mInstantAppRegistry.addInstantApp(userId, pkgSetting7.getAppId());
        }
        if (!IncrementalManager.isIncrementalPath(pkgSetting7.getPathString())) {
            pkgSetting7.setLoadingProgress(1.0f);
        }
        return pkg;
    }

    /* JADX DEBUG: Don't trust debug lines info. Repeating lines: [554=4] */
    /* JADX WARN: Removed duplicated region for block: B:103:0x01ff A[Catch: all -> 0x01ce, TryCatch #8 {all -> 0x01ce, blocks: (B:87:0x0190, B:90:0x01b2, B:92:0x01c0, B:91:0x01bb, B:93:0x01c7, B:98:0x01d8, B:100:0x01dc, B:101:0x01f4, B:103:0x01ff, B:104:0x0203, B:111:0x0210, B:105:0x0204, B:106:0x020b), top: B:149:0x0190 }] */
    /* JADX WARN: Removed duplicated region for block: B:114:0x0218  */
    /* JADX WARN: Removed duplicated region for block: B:115:0x021a  */
    /* JADX WARN: Removed duplicated region for block: B:149:0x0190 A[EXC_TOP_SPLITTER, SYNTHETIC] */
    /* JADX WARN: Removed duplicated region for block: B:151:0x010d A[EXC_TOP_SPLITTER, SYNTHETIC] */
    /* JADX WARN: Removed duplicated region for block: B:33:0x0092 A[Catch: all -> 0x0231, TryCatch #0 {all -> 0x0231, blocks: (B:24:0x007b, B:31:0x008e, B:33:0x0092, B:38:0x00a6, B:30:0x0086), top: B:135:0x004c }] */
    /* JADX WARN: Removed duplicated region for block: B:34:0x009c  */
    /* JADX WARN: Removed duplicated region for block: B:41:0x00ab  */
    /* JADX WARN: Removed duplicated region for block: B:66:0x00e6 A[LOOP:0: B:64:0x00e0->B:66:0x00e6, LOOP_END] */
    /*
        Code decompiled incorrectly, please refer to instructions dump.
    */
    private void commitPackageSettings(AndroidPackage pkg, AndroidPackage oldPkg, PackageSetting pkgSetting, PackageSetting oldPkgSetting, int scanFlags, boolean chatty, ReconciledPackage reconciledPkg) {
        PackageManagerTracedLock packageManagerTracedLock;
        Map<String, AndroidPackage> combinedSigningDetails;
        int collectionSize;
        int i;
        StringBuilder r;
        List<String> protectedBroadcasts;
        int i2;
        String pkgName = pkg.getPackageName();
        if (this.mPm.mCustomResolverComponentName != null && this.mPm.mCustomResolverComponentName.getPackageName().equals(pkg.getPackageName())) {
            this.mPm.setUpCustomResolverActivity(pkg, pkgSetting);
        }
        if (pkg.getPackageName().equals(PackageManagerService.PLATFORM_PACKAGE_NAME)) {
            this.mPm.setPlatformPackage(pkg, pkgSetting);
        }
        ArrayList<AndroidPackage> clientLibPkgs = null;
        PackageManagerTracedLock packageManagerTracedLock2 = this.mPm.mLock;
        synchronized (packageManagerTracedLock2) {
            try {
                try {
                    if (ArrayUtils.isEmpty(reconciledPkg.mAllowedSharedLibraryInfos)) {
                        packageManagerTracedLock = packageManagerTracedLock2;
                    } else {
                        try {
                            for (SharedLibraryInfo info : reconciledPkg.mAllowedSharedLibraryInfos) {
                                this.mSharedLibraries.commitSharedLibraryInfoLPw(info);
                            }
                            Map<String, AndroidPackage> combinedSigningDetails2 = reconciledPkg.getCombinedAvailablePackages();
                            try {
                                combinedSigningDetails = combinedSigningDetails2;
                                packageManagerTracedLock = packageManagerTracedLock2;
                                try {
                                    this.mSharedLibraries.updateSharedLibrariesLPw(pkg, pkgSetting, null, null, combinedSigningDetails);
                                } catch (PackageManagerException e) {
                                    e = e;
                                    Slog.e("PackageManager", "updateSharedLibrariesLPr failed: ", e);
                                    if ((scanFlags & 16) != 0) {
                                    }
                                    if (reconciledPkg.mInstallResult != null) {
                                    }
                                    if ((scanFlags & 16) == 0) {
                                        this.mPm.snapshotComputer().checkPackageFrozen(pkgName);
                                    }
                                    boolean isReplace = reconciledPkg.mPrepareResult == null && reconciledPkg.mPrepareResult.mReplace;
                                    if (clientLibPkgs != null) {
                                        while (i2 < clientLibPkgs.size()) {
                                        }
                                    }
                                    Trace.traceBegin(262144L, "updateSettings");
                                    synchronized (this.mPm.mLock) {
                                    }
                                }
                            } catch (PackageManagerException e2) {
                                e = e2;
                                combinedSigningDetails = combinedSigningDetails2;
                                packageManagerTracedLock = packageManagerTracedLock2;
                            }
                            if ((scanFlags & 16) != 0) {
                                clientLibPkgs = this.mSharedLibraries.updateAllSharedLibrariesLPw(pkg, pkgSetting, combinedSigningDetails);
                            }
                        } catch (Throwable th) {
                            th = th;
                            packageManagerTracedLock = packageManagerTracedLock2;
                            while (true) {
                                try {
                                    break;
                                } catch (Throwable th2) {
                                    th = th2;
                                }
                            }
                            throw th;
                        }
                    }
                    if (reconciledPkg.mInstallResult != null) {
                        reconciledPkg.mInstallResult.mLibraryConsumers = clientLibPkgs;
                    }
                    if ((scanFlags & 16) == 0 && (scanFlags & 1024) == 0 && (scanFlags & 2048) == 0) {
                        this.mPm.snapshotComputer().checkPackageFrozen(pkgName);
                    }
                    boolean isReplace2 = reconciledPkg.mPrepareResult == null && reconciledPkg.mPrepareResult.mReplace;
                    if (clientLibPkgs != null && (pkg.getStaticSharedLibName() == null || isReplace2)) {
                        for (i2 = 0; i2 < clientLibPkgs.size(); i2++) {
                            AndroidPackage clientPkg = clientLibPkgs.get(i2);
                            this.mPm.killApplication(clientPkg.getPackageName(), clientPkg.getUid(), "update lib");
                        }
                    }
                    Trace.traceBegin(262144L, "updateSettings");
                    synchronized (this.mPm.mLock) {
                        try {
                            try {
                                this.mPm.mSettings.insertPackageSettingLPw(pkgSetting, pkg);
                                ITranPackageManagerService.Instance().setThemeResourceIdForApp(pkg);
                                this.mPm.setGriffinPackages(pkg);
                                this.mPm.mPackages.put(pkg.getPackageName(), pkg);
                                if ((8388608 & scanFlags) != 0) {
                                    try {
                                        this.mApexManager.registerApkInApex(pkg);
                                    } catch (Throwable th3) {
                                        th = th3;
                                        throw th;
                                    }
                                }
                                KeySetManagerService ksms = this.mPm.mSettings.getKeySetManagerService();
                                ksms.addScannedPackageLPw(pkg);
                                Computer snapshot = this.mPm.snapshotComputer();
                                this.mPm.mComponentResolver.addAllComponents(pkg, chatty, this.mPm.mSetupWizardPackage, snapshot);
                                this.mPm.mAppsFilter.addPackage(snapshot, pkgSetting, isReplace2);
                                this.mPm.addAllPackageProperties(pkg);
                                if (oldPkgSetting != null && oldPkgSetting.getPkg() != null) {
                                    this.mPm.mDomainVerificationManager.migrateState(oldPkgSetting, pkgSetting);
                                    collectionSize = ArrayUtils.size(pkg.getInstrumentations());
                                    i = 0;
                                    r = null;
                                    while (i < collectionSize) {
                                        try {
                                            ParsedInstrumentation a = pkg.getInstrumentations().get(i);
                                            int collectionSize2 = collectionSize;
                                            ComponentMutateUtils.setPackageName(a, pkg.getPackageName());
                                            KeySetManagerService ksms2 = ksms;
                                            this.mPm.addInstrumentation(a.getComponentName(), a);
                                            if (chatty) {
                                                if (r == null) {
                                                    r = new StringBuilder(256);
                                                } else {
                                                    r.append(' ');
                                                }
                                                r.append(a.getName());
                                            }
                                            i++;
                                            collectionSize = collectionSize2;
                                            ksms = ksms2;
                                        } catch (Throwable th4) {
                                            th = th4;
                                            throw th;
                                        }
                                    }
                                    if (r != null && PackageManagerService.DEBUG_PACKAGE_SCANNING) {
                                        Log.d("PackageManager", "  Instrumentation: " + ((Object) r));
                                    }
                                    protectedBroadcasts = pkg.getProtectedBroadcasts();
                                    if (!protectedBroadcasts.isEmpty()) {
                                        synchronized (this.mPm.mProtectedBroadcasts) {
                                            this.mPm.mProtectedBroadcasts.addAll(protectedBroadcasts);
                                        }
                                    }
                                    this.mPm.mPermissionManager.onPackageAdded(pkg, (scanFlags & 8192) == 0, oldPkg);
                                    Trace.traceEnd(262144L);
                                }
                                this.mPm.mDomainVerificationManager.addPackage(pkgSetting);
                                collectionSize = ArrayUtils.size(pkg.getInstrumentations());
                                i = 0;
                                r = null;
                                while (i < collectionSize) {
                                }
                                if (r != null) {
                                    Log.d("PackageManager", "  Instrumentation: " + ((Object) r));
                                }
                                protectedBroadcasts = pkg.getProtectedBroadcasts();
                                if (!protectedBroadcasts.isEmpty()) {
                                }
                                this.mPm.mPermissionManager.onPackageAdded(pkg, (scanFlags & 8192) == 0, oldPkg);
                                Trace.traceEnd(262144L);
                            } catch (Throwable th5) {
                                th = th5;
                            }
                        } catch (Throwable th6) {
                            th = th6;
                        }
                    }
                } catch (Throwable th7) {
                    th = th7;
                }
            } catch (Throwable th8) {
                th = th8;
                packageManagerTracedLock = packageManagerTracedLock2;
            }
        }
    }

    /* JADX DEBUG: Don't trust debug lines info. Repeating lines: [682=4, 639=5] */
    public int installExistingPackageAsUser(final String packageName, final int userId, int installFlags, int installReason, List<String> allowlistedRestrictedPermissions, final IntentSender intentSender) {
        boolean installed;
        if (PackageManagerService.DEBUG_INSTALL) {
            Log.v("PackageManager", "installExistingPackageAsUser package=" + packageName + " userId=" + userId + " installFlags=" + installFlags + " installReason=" + installReason + " allowlistedRestrictedPermissions=" + allowlistedRestrictedPermissions);
        }
        int callingUid = Binder.getCallingUid();
        if (this.mContext.checkCallingOrSelfPermission("android.permission.INSTALL_PACKAGES") == 0 || this.mContext.checkCallingOrSelfPermission("com.android.permission.INSTALL_EXISTING_PACKAGES") == 0) {
            Computer preLockSnapshot = this.mPm.snapshotComputer();
            preLockSnapshot.enforceCrossUserPermission(callingUid, userId, true, true, "installExistingPackage for user " + userId);
            if (this.mPm.isUserRestricted(userId, "no_install_apps")) {
                boolean ignore = IPackageManagerServiceLice.Instance().ignoreInstallUserRestriction(packageName, callingUid, true);
                if (!ignore) {
                    return -111;
                }
            }
            long callingId = Binder.clearCallingIdentity();
            boolean instantApp = (installFlags & 2048) != 0;
            boolean fullApp = (installFlags & 16384) != 0;
            try {
                synchronized (this.mPm.mLock) {
                    try {
                        Computer snapshot = this.mPm.snapshotComputer();
                        PackageSetting pkgSetting = this.mPm.mSettings.getPackageLPr(packageName);
                        if (pkgSetting == null) {
                            try {
                                return -3;
                            } catch (Throwable th) {
                                th = th;
                            }
                        } else {
                            if (snapshot.canViewInstantApps(callingUid, UserHandle.getUserId(callingUid))) {
                                installed = false;
                            } else {
                                boolean installAllowed = false;
                                try {
                                    int[] userIds = this.mPm.mUserManager.getUserIds();
                                    int length = userIds.length;
                                    installed = false;
                                    int i = 0;
                                    while (i < length) {
                                        try {
                                            int checkUserId = userIds[i];
                                            int i2 = length;
                                            installAllowed = !pkgSetting.getInstantApp(checkUserId);
                                            if (installAllowed) {
                                                break;
                                            }
                                            i++;
                                            length = i2;
                                        } catch (Throwable th2) {
                                            th = th2;
                                        }
                                    }
                                    if (!installAllowed) {
                                        return -3;
                                    }
                                } catch (Throwable th3) {
                                    th = th3;
                                }
                            }
                            try {
                                if (!pkgSetting.getInstalled(userId)) {
                                    pkgSetting.setInstalled(true, userId);
                                    pkgSetting.setHidden(false, userId);
                                    pkgSetting.setInstallReason(installReason, userId);
                                    pkgSetting.setUninstallReason(0, userId);
                                    pkgSetting.setFirstInstallTime(System.currentTimeMillis(), userId);
                                    this.mPm.mSettings.writePackageRestrictionsLPr(userId);
                                    this.mPm.mSettings.writeKernelMappingLPr(pkgSetting);
                                    installed = true;
                                } else if (fullApp && pkgSetting.getInstantApp(userId)) {
                                    installed = true;
                                }
                                ScanPackageUtils.setInstantAppForUser(this.mPm.mInjector, pkgSetting, userId, instantApp, fullApp);
                                if (installed) {
                                    if (pkgSetting.getPkg() != null) {
                                        PermissionManagerServiceInternal.PackageInstalledParams.Builder permissionParamsBuilder = new PermissionManagerServiceInternal.PackageInstalledParams.Builder();
                                        if ((4194304 & installFlags) != 0) {
                                            permissionParamsBuilder.setAllowlistedRestrictedPermissions(pkgSetting.getPkg().getRequestedPermissions());
                                        }
                                        this.mPm.mPermissionManager.onPackageInstalled(pkgSetting.getPkg(), -1, permissionParamsBuilder.build(), userId);
                                        synchronized (this.mPm.mInstallLock) {
                                            this.mAppDataHelper.prepareAppDataAfterInstallLIF(pkgSetting.getPkg());
                                        }
                                    }
                                    PackageManagerService packageManagerService = this.mPm;
                                    packageManagerService.sendPackageAddedForUser(packageManagerService.snapshotComputer(), packageName, pkgSetting, userId, 0);
                                    synchronized (this.mPm.mLock) {
                                        this.mPm.updateSequenceNumberLP(pkgSetting, new int[]{userId});
                                    }
                                    final PackageInstalledInfo res = new PackageInstalledInfo(1);
                                    res.mPkg = pkgSetting.getPkg();
                                    res.mNewUsers = new int[]{userId};
                                    PostInstallData postInstallData = new PostInstallData(null, res, new Runnable() { // from class: com.android.server.pm.InstallPackageHelper$$ExternalSyntheticLambda1
                                        @Override // java.lang.Runnable
                                        public final void run() {
                                            InstallPackageHelper.this.m5418x66335d47(packageName, userId, intentSender, res);
                                        }
                                    });
                                    restoreAndPostInstall(userId, res, postInstallData);
                                }
                                Binder.restoreCallingIdentity(callingId);
                                return 1;
                            } catch (Throwable th4) {
                                th = th4;
                            }
                        }
                    } catch (Throwable th5) {
                        th = th5;
                    }
                    while (true) {
                        try {
                            break;
                        } catch (Throwable th6) {
                            th = th6;
                        }
                    }
                    throw th;
                }
            } finally {
                Binder.restoreCallingIdentity(callingId);
            }
        }
        throw new SecurityException("Neither user " + callingUid + " nor current process has android.permission.INSTALL_PACKAGES.");
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$installExistingPackageAsUser$0$com-android-server-pm-InstallPackageHelper  reason: not valid java name */
    public /* synthetic */ void m5418x66335d47(String packageName, int userId, IntentSender intentSender, PackageInstalledInfo res) {
        this.mPm.restorePermissionsAndUpdateRolesForNewUserInstall(packageName, userId);
        if (intentSender != null) {
            onRestoreComplete(res.mReturnCode, this.mContext, intentSender);
        }
    }

    private static void onRestoreComplete(int returnCode, Context context, IntentSender target) {
        Intent fillIn = new Intent();
        fillIn.putExtra("android.content.pm.extra.STATUS", PackageManager.installStatusToPublicStatus(returnCode));
        try {
            BroadcastOptions options = BroadcastOptions.makeBasic();
            options.setPendingIntentBackgroundActivityLaunchAllowed(false);
            target.sendIntent(context, 0, fillIn, null, null, null, options.toBundle());
        } catch (IntentSender.SendIntentException e) {
        }
    }

    public void restoreAndPostInstall(int userId, PackageInstalledInfo res, PostInstallData data) {
        if (PackageManagerService.DEBUG_INSTALL) {
            Log.v("PackageManager", "restoreAndPostInstall userId=" + userId + " package=" + res.mPkg);
        }
        boolean update = (res.mRemovedInfo == null || res.mRemovedInfo.mRemovedPackage == null) ? false : true;
        boolean doRestore = (update || res.mPkg == null) ? false : true;
        if (this.mPm.mNextInstallToken < 0) {
            this.mPm.mNextInstallToken = 1;
        }
        PackageManagerService packageManagerService = this.mPm;
        int token = packageManagerService.mNextInstallToken;
        packageManagerService.mNextInstallToken = token + 1;
        if (data != null) {
            this.mPm.mRunningInstalls.put(token, data);
        } else if (PackageManagerService.DEBUG_INSTALL) {
            Log.v("PackageManager", "No post-install required for " + token);
        }
        if (PackageManagerService.DEBUG_INSTALL) {
            Log.v("PackageManager", "+ starting restore round-trip " + token);
        }
        if (res.mReturnCode == 1 && doRestore) {
            if (res.mFreezer != null) {
                res.mFreezer.close();
            }
            doRestore = performBackupManagerRestore(userId, token, res);
        }
        if (res.mReturnCode == 1 && !doRestore && update) {
            doRestore = performRollbackManagerRestore(userId, token, res, data);
        }
        if (!doRestore) {
            if (PackageManagerService.DEBUG_INSTALL) {
                Log.v("PackageManager", "No restore - queue post-install for " + token);
            }
            Trace.asyncTraceBegin(262144L, "postInstall", token);
            Message msg = this.mPm.mHandler.obtainMessage(9, token, 0);
            this.mPm.mHandler.sendMessage(msg);
        }
    }

    private boolean performBackupManagerRestore(int userId, int token, PackageInstalledInfo res) {
        IBackupManager iBackupManager = this.mInjector.getIBackupManager();
        if (iBackupManager != null) {
            if (userId == -1) {
                userId = 0;
            }
            if (PackageManagerService.DEBUG_INSTALL) {
                Log.v("PackageManager", "token " + token + " to BM for possible restore for user " + userId);
            }
            Trace.asyncTraceBegin(262144L, "restore", token);
            try {
                if (iBackupManager.isUserReadyForBackup(userId)) {
                    iBackupManager.restoreAtInstallForUser(userId, res.mPkg.getPackageName(), token);
                    return true;
                }
                Slog.w("PackageManager", "User " + userId + " is not ready. Restore at install didn't take place.");
                return false;
            } catch (RemoteException e) {
                return true;
            } catch (Exception e2) {
                Slog.e("PackageManager", "Exception trying to enqueue restore", e2);
                return false;
            }
        }
        Slog.e("PackageManager", "Backup Manager not found!");
        return false;
    }

    private boolean performRollbackManagerRestore(int userId, int token, PackageInstalledInfo res, PostInstallData data) {
        PackageSetting ps;
        int appId;
        long ceDataInode;
        boolean z;
        String packageName = res.mPkg.getPackageName();
        int[] allUsers = this.mPm.mUserManager.getUserIds();
        synchronized (this.mPm.mLock) {
            try {
                try {
                    ps = this.mPm.mSettings.getPackageLPr(packageName);
                    if (ps == null) {
                        appId = -1;
                        ceDataInode = -1;
                    } else {
                        int appId2 = ps.getAppId();
                        long ceDataInode2 = ps.getCeDataInode(userId);
                        appId = appId2;
                        ceDataInode = ceDataInode2;
                    }
                } catch (Throwable th) {
                    th = th;
                }
            } catch (Throwable th2) {
                th = th2;
            }
            try {
                int[] installedUsers = ps.queryInstalledUsers(allUsers, true);
                if (data != null && data.args != null && ((data.args.mInstallFlags & 262144) != 0 || (data.args.mInstallFlags & 128) != 0)) {
                    z = true;
                } else {
                    z = false;
                }
                boolean doSnapshotOrRestore = z;
                if (ps == null || !doSnapshotOrRestore) {
                    return false;
                }
                String seInfo = AndroidPackageUtils.getSeInfo(res.mPkg, ps);
                RollbackManagerInternal rollbackManager = (RollbackManagerInternal) this.mInjector.getLocalService(RollbackManagerInternal.class);
                rollbackManager.snapshotAndRestoreUserData(packageName, UserHandle.toUserHandles(installedUsers), appId, ceDataInode, seInfo, token);
                return true;
            } catch (Throwable th3) {
                th = th3;
                throw th;
            }
        }
    }

    public void processInstallRequests(boolean success, List<InstallRequest> installRequests) {
        final List<InstallRequest> apexInstallRequests = new ArrayList<>();
        List<InstallRequest> apkInstallRequests = new ArrayList<>();
        for (InstallRequest request : installRequests) {
            if ((request.mArgs.mInstallFlags & 131072) != 0) {
                apexInstallRequests.add(request);
            } else {
                apkInstallRequests.add(request);
            }
        }
        if (!apexInstallRequests.isEmpty() && !apkInstallRequests.isEmpty()) {
            throw new IllegalStateException("Attempted to do a multi package install of both APEXes and APKs");
        }
        if (!apexInstallRequests.isEmpty()) {
            if (success) {
                Thread t = new Thread(new Runnable() { // from class: com.android.server.pm.InstallPackageHelper$$ExternalSyntheticLambda0
                    @Override // java.lang.Runnable
                    public final void run() {
                        InstallPackageHelper.this.m5419xc8263c13(apexInstallRequests);
                    }
                }, "installApexPackages");
                t.start();
                return;
            }
            InstallRequest request2 = apexInstallRequests.get(0);
            this.mPm.notifyInstallObserver(request2.mInstallResult, request2.mArgs.mObserver);
            return;
        }
        if (success) {
            for (InstallRequest request3 : apkInstallRequests) {
                request3.mArgs.doPreInstall(request3.mInstallResult.mReturnCode);
            }
            synchronized (this.mPm.mInstallLock) {
                installPackagesTracedLI(apkInstallRequests);
            }
            for (InstallRequest request4 : apkInstallRequests) {
                request4.mArgs.doPostInstall(request4.mInstallResult.mReturnCode, request4.mInstallResult.mUid);
            }
        }
        for (InstallRequest request5 : apkInstallRequests) {
            restoreAndPostInstall(request5.mArgs.mUser.getIdentifier(), request5.mInstallResult, new PostInstallData(request5.mArgs, request5.mInstallResult, null));
        }
    }

    /* JADX DEBUG: Method merged with bridge method */
    /* JADX INFO: Access modifiers changed from: private */
    /* renamed from: installApexPackagesTraced */
    public void m5419xc8263c13(List<InstallRequest> requests) {
        try {
            Trace.traceBegin(262144L, "installApexPackages");
            installApexPackages(requests);
        } finally {
            Trace.traceEnd(262144L);
        }
    }

    private void installApexPackages(List<InstallRequest> requests) {
        File dir;
        File[] apexes;
        if (requests.isEmpty()) {
            return;
        }
        if (requests.size() != 1) {
            throw new IllegalStateException("Only a non-staged install of a single APEX is supported");
        }
        InstallRequest request = requests.get(0);
        try {
            dir = request.mArgs.mOriginInfo.mResolvedFile;
            apexes = dir.listFiles();
        } catch (PackageManagerException e) {
            request.mInstallResult.setError("APEX installation failed", e);
        }
        if (apexes == null) {
            throw new PackageManagerException(RequestStatus.SYS_ETIMEDOUT, dir.getAbsolutePath() + " is not a directory");
        }
        if (apexes.length != 1) {
            throw new PackageManagerException(RequestStatus.SYS_ETIMEDOUT, "Expected exactly one .apex file under " + dir.getAbsolutePath() + " got: " + apexes.length);
        }
        PackageParser2 packageParser = this.mPm.mInjector.getScanningPackageParser();
        this.mApexManager.installPackage(apexes[0], packageParser);
        if (packageParser != null) {
            packageParser.close();
        }
        PackageManagerService.invalidatePackageInfoCache();
        this.mPm.notifyInstallObserver(request.mInstallResult, request.mArgs.mObserver);
    }

    private void installPackagesTracedLI(List<InstallRequest> requests) {
        try {
            Trace.traceBegin(262144L, "installPackages");
            installPackagesLI(requests);
        } finally {
            Trace.traceEnd(262144L);
        }
    }

    /*  JADX ERROR: JadxRuntimeException in pass: BlockProcessor
        jadx.core.utils.exceptions.JadxRuntimeException: Unreachable block: B:221:0x05ea
        	at jadx.core.dex.visitors.blocks.BlockProcessor.checkForUnreachableBlocks(BlockProcessor.java:81)
        	at jadx.core.dex.visitors.blocks.BlockProcessor.processBlocksTree(BlockProcessor.java:47)
        	at jadx.core.dex.visitors.blocks.BlockProcessor.visit(BlockProcessor.java:39)
        */
    /* JADX DEBUG: Don't trust debug lines info. Repeating lines: [1047=4, 1061=10, 1062=7, 1063=7, 1064=7, 1065=7, 1067=7, 1068=7, 1073=7, 1074=7, 1075=7, 1076=7, 1078=7, 1079=14, 1081=7, 1082=7, 1084=7, 1085=14, 1086=7, 1087=7, 1089=7, 1092=7, 1093=7, 1094=7, 1096=8, 1097=7, 1099=7, 1101=7, 988=4] */
    private void installPackagesLI(java.util.List<com.android.server.pm.InstallRequest> r35) {
        /*
            r34 = this;
            r9 = r34
            android.util.ArrayMap r0 = new android.util.ArrayMap
            int r1 = r35.size()
            r0.<init>(r1)
            r10 = r0
            android.util.ArrayMap r0 = new android.util.ArrayMap
            int r1 = r35.size()
            r0.<init>(r1)
            r11 = r0
            android.util.ArrayMap r0 = new android.util.ArrayMap
            int r1 = r35.size()
            r0.<init>(r1)
            r12 = r0
            android.util.ArrayMap r0 = new android.util.ArrayMap
            int r1 = r35.size()
            r0.<init>(r1)
            r13 = r0
            android.util.ArrayMap r0 = new android.util.ArrayMap
            int r1 = r35.size()
            r0.<init>(r1)
            r14 = r0
            android.util.ArrayMap r0 = new android.util.ArrayMap
            int r1 = r35.size()
            r0.<init>(r1)
            r15 = r0
            r16 = 0
            r6 = 1
            r3 = 262144(0x40000, double:1.295163E-318)
            java.lang.String r0 = "installPackagesLI"
            android.os.Trace.traceBegin(r3, r0)     // Catch: java.lang.Throwable -> L6db
            java.util.Iterator r0 = r35.iterator()     // Catch: java.lang.Throwable -> L6db
        L4d:
            boolean r1 = r0.hasNext()     // Catch: java.lang.Throwable -> L6db
            if (r1 == 0) goto L4b9
            java.lang.Object r1 = r0.next()     // Catch: java.lang.Throwable -> L4af
            com.android.server.pm.InstallRequest r1 = (com.android.server.pm.InstallRequest) r1     // Catch: java.lang.Throwable -> L4af
            r2 = r1
            java.lang.String r1 = "preparePackage"
            android.os.Trace.traceBegin(r3, r1)     // Catch: java.lang.Throwable -> L3b6 com.android.server.pm.PrepareFailure -> L3c4
            com.android.server.pm.InstallArgs r1 = r2.mArgs     // Catch: java.lang.Throwable -> L3b6 com.android.server.pm.PrepareFailure -> L3c4
            com.android.server.pm.PackageInstalledInfo r5 = r2.mInstallResult     // Catch: java.lang.Throwable -> L3b6 com.android.server.pm.PrepareFailure -> L3c4
            com.android.server.pm.PrepareResult r1 = r9.preparePackageLI(r1, r5)     // Catch: java.lang.Throwable -> L3b6 com.android.server.pm.PrepareFailure -> L3c4
            r5 = r1
            android.os.Trace.traceEnd(r3)     // Catch: java.lang.Throwable -> L6db
            com.android.server.pm.PackageInstalledInfo r1 = r2.mInstallResult     // Catch: java.lang.Throwable -> L6db
            r1.setReturnCode(r6)     // Catch: java.lang.Throwable -> L6db
            com.android.server.pm.PackageInstalledInfo r1 = r2.mInstallResult     // Catch: java.lang.Throwable -> L6db
            com.android.server.pm.InstallArgs r3 = r2.mArgs     // Catch: java.lang.Throwable -> L3a9
            com.android.server.pm.InstallSource r3 = r3.mInstallSource     // Catch: java.lang.Throwable -> L3a9
            java.lang.String r3 = r3.installerPackageName     // Catch: java.lang.Throwable -> L3a9
            r1.mInstallerPackageName = r3     // Catch: java.lang.Throwable -> L3a9
            com.android.server.pm.parsing.pkg.ParsedPackage r1 = r5.mPackageToScan     // Catch: java.lang.Throwable -> L3a9
            java.lang.String r1 = r1.getPackageName()     // Catch: java.lang.Throwable -> L3a9
            r4 = r1
            r13.put(r4, r5)     // Catch: java.lang.Throwable -> L3a9
            com.android.server.pm.PackageInstalledInfo r1 = r2.mInstallResult     // Catch: java.lang.Throwable -> L3a9
            r12.put(r4, r1)     // Catch: java.lang.Throwable -> L3a9
            com.android.server.pm.InstallArgs r1 = r2.mArgs     // Catch: java.lang.Throwable -> L3a9
            r11.put(r4, r1)     // Catch: java.lang.Throwable -> L3a9
            com.android.server.pm.parsing.pkg.ParsedPackage r3 = r5.mPackageToScan     // Catch: com.android.server.pm.PackageManagerException -> L2d4 java.lang.Throwable -> L3a9
            int r1 = r5.mParseFlags     // Catch: com.android.server.pm.PackageManagerException -> L2d4 java.lang.Throwable -> L3a9
            int r6 = r5.mScanFlags     // Catch: com.android.server.pm.PackageManagerException -> L2d4 java.lang.Throwable -> L3a9
            long r21 = java.lang.System.currentTimeMillis()     // Catch: com.android.server.pm.PackageManagerException -> L2d4 java.lang.Throwable -> L3a9
            com.android.server.pm.InstallArgs r7 = r2.mArgs     // Catch: com.android.server.pm.PackageManagerException -> L2d4 java.lang.Throwable -> L3a9
            android.os.UserHandle r7 = r7.mUser     // Catch: com.android.server.pm.PackageManagerException -> L2d4 java.lang.Throwable -> L3a9
            com.android.server.pm.InstallArgs r8 = r2.mArgs     // Catch: com.android.server.pm.PackageManagerException -> L2d4 java.lang.Throwable -> L3a9
            java.lang.String r8 = r8.mAbiOverride     // Catch: com.android.server.pm.PackageManagerException -> L2d4 java.lang.Throwable -> L3a9
            r23 = r1
            r1 = r34
            r24 = r13
            r13 = r2
            r2 = r3
            r25 = 262144(0x40000, double:1.295163E-318)
            r3 = r23
            r18 = r12
            r12 = r4
            r4 = r6
            r17 = r5
            r20 = r11
            r11 = 1
            r19 = 0
            r5 = r21
            r11 = 2
            com.android.server.pm.ScanResult r1 = r1.scanPackageTracedLI(r2, r3, r4, r5, r7, r8)     // Catch: java.lang.Throwable -> L2cb com.android.server.pm.PackageManagerException -> L2d0
            com.android.server.pm.PackageSetting r2 = r1.mPkgSetting     // Catch: java.lang.Throwable -> L2cb com.android.server.pm.PackageManagerException -> L2d0
            com.android.server.pm.parsing.pkg.AndroidPackage r2 = r2.getPkg()     // Catch: java.lang.Throwable -> L2cb com.android.server.pm.PackageManagerException -> L2d0
            java.lang.String r2 = r2.getPackageName()     // Catch: java.lang.Throwable -> L2cb com.android.server.pm.PackageManagerException -> L2d0
            java.lang.Object r2 = r10.put(r2, r1)     // Catch: java.lang.Throwable -> L2cb com.android.server.pm.PackageManagerException -> L2d0
            if (r2 == 0) goto L1c0
            com.android.server.pm.PackageInstalledInfo r0 = r13.mInstallResult     // Catch: java.lang.Throwable -> L2cb com.android.server.pm.PackageManagerException -> L2d0
            r2 = -5
            java.lang.StringBuilder r3 = new java.lang.StringBuilder     // Catch: java.lang.Throwable -> L2cb com.android.server.pm.PackageManagerException -> L2d0
            r3.<init>()     // Catch: java.lang.Throwable -> L2cb com.android.server.pm.PackageManagerException -> L2d0
            java.lang.String r4 = "Duplicate package "
            java.lang.StringBuilder r3 = r3.append(r4)     // Catch: java.lang.Throwable -> L2cb com.android.server.pm.PackageManagerException -> L2d0
            com.android.server.pm.PackageSetting r4 = r1.mPkgSetting     // Catch: java.lang.Throwable -> L2cb com.android.server.pm.PackageManagerException -> L2d0
            com.android.server.pm.parsing.pkg.AndroidPackage r4 = r4.getPkg()     // Catch: java.lang.Throwable -> L2cb com.android.server.pm.PackageManagerException -> L2d0
            java.lang.String r4 = r4.getPackageName()     // Catch: java.lang.Throwable -> L2cb com.android.server.pm.PackageManagerException -> L2d0
            java.lang.StringBuilder r3 = r3.append(r4)     // Catch: java.lang.Throwable -> L2cb com.android.server.pm.PackageManagerException -> L2d0
            java.lang.String r4 = " in multi-package install request."
            java.lang.StringBuilder r3 = r3.append(r4)     // Catch: java.lang.Throwable -> L2cb com.android.server.pm.PackageManagerException -> L2d0
            java.lang.String r3 = r3.toString()     // Catch: java.lang.Throwable -> L2cb com.android.server.pm.PackageManagerException -> L2d0
            r0.setError(r2, r3)     // Catch: java.lang.Throwable -> L2cb com.android.server.pm.PackageManagerException -> L2d0
            if (r16 == 0) goto L15c
            java.util.Iterator r0 = r35.iterator()
        L100:
            boolean r2 = r0.hasNext()
            if (r2 == 0) goto L15b
            java.lang.Object r2 = r0.next()
            com.android.server.pm.InstallRequest r2 = (com.android.server.pm.InstallRequest) r2
            com.android.server.pm.InstallArgs r3 = r2.mArgs
            int r4 = r3.mDataLoaderType
            if (r4 == r11) goto L113
            goto L100
        L113:
            android.content.pm.SigningDetails r4 = r3.mSigningDetails
            int r4 = r4.getSignatureSchemeVersion()
            r8 = 4
            if (r4 == r8) goto L11d
            goto L100
        L11d:
            com.android.server.pm.PackageInstalledInfo r4 = r2.mInstallResult
            com.android.server.pm.parsing.pkg.AndroidPackage r4 = r4.mPkg
            java.lang.String r4 = r4.getBaseApkPath()
            com.android.server.pm.PackageInstalledInfo r5 = r2.mInstallResult
            com.android.server.pm.parsing.pkg.AndroidPackage r5 = r5.mPkg
            java.lang.String[] r5 = r5.getSplitCodePaths()
            com.android.server.pm.OriginInfo r6 = r3.mOriginInfo
            java.io.File r6 = r6.mResolvedFile
            android.net.Uri r6 = android.net.Uri.fromFile(r6)
            com.android.server.pm.PackageManagerService r7 = r9.mPm
            int r8 = r7.mPendingVerificationToken
            int r11 = r8 + 1
            r7.mPendingVerificationToken = r11
            r27 = r8
            java.lang.String r7 = com.android.server.pm.PackageManagerServiceUtils.buildVerificationRootHashString(r4, r5)
            r29 = 1
            int r8 = r3.mDataLoaderType
            android.os.UserHandle r32 = r3.getUser()
            android.content.Context r11 = r9.mContext
            r28 = r6
            r30 = r7
            r31 = r8
            r33 = r11
            com.android.server.pm.VerificationUtils.broadcastPackageVerified(r27, r28, r29, r30, r31, r32, r33)
            r11 = 2
            goto L100
        L15b:
            goto L1bc
        L15c:
            java.util.Collection r0 = r10.values()
            java.util.Iterator r0 = r0.iterator()
        L164:
            boolean r2 = r0.hasNext()
            if (r2 == 0) goto L18c
            java.lang.Object r2 = r0.next()
            com.android.server.pm.ScanResult r2 = (com.android.server.pm.ScanResult) r2
            com.android.server.pm.ScanRequest r3 = r2.mRequest
            com.android.server.pm.parsing.pkg.ParsedPackage r3 = r3.mParsedPackage
            java.lang.String r3 = r3.getPackageName()
            java.lang.Boolean r4 = java.lang.Boolean.valueOf(r19)
            java.lang.Object r3 = r15.getOrDefault(r3, r4)
            java.lang.Boolean r3 = (java.lang.Boolean) r3
            boolean r3 = r3.booleanValue()
            if (r3 == 0) goto L18b
            r9.cleanUpAppIdCreation(r2)
        L18b:
            goto L164
        L18c:
            java.util.Iterator r0 = r35.iterator()
        L190:
            boolean r2 = r0.hasNext()
            if (r2 == 0) goto L1bc
            java.lang.Object r2 = r0.next()
            com.android.server.pm.InstallRequest r2 = (com.android.server.pm.InstallRequest) r2
            com.android.server.pm.PackageInstalledInfo r3 = r2.mInstallResult
            com.android.server.pm.PackageFreezer r3 = r3.mFreezer
            if (r3 == 0) goto L1a9
            com.android.server.pm.PackageInstalledInfo r3 = r2.mInstallResult
            com.android.server.pm.PackageFreezer r3 = r3.mFreezer
            r3.close()
        L1a9:
            com.android.server.pm.PackageInstalledInfo r3 = r2.mInstallResult
            int r3 = r3.mReturnCode
            r4 = 1
            if (r3 != r4) goto L1b7
            com.android.server.pm.PackageInstalledInfo r3 = r2.mInstallResult
            r11 = r19
            r3.mReturnCode = r11
            goto L1b9
        L1b7:
            r11 = r19
        L1b9:
            r19 = r11
            goto L190
        L1bc:
            android.os.Trace.traceEnd(r25)
            return
        L1c0:
            r11 = r19
            com.android.server.pm.ScanRequest r2 = r1.mRequest     // Catch: com.android.server.pm.PackageManagerException -> L2c9 java.lang.Throwable -> L6d9
            com.android.server.pm.parsing.pkg.AndroidPackage r2 = r2.mOldPkg     // Catch: com.android.server.pm.PackageManagerException -> L2c9 java.lang.Throwable -> L6d9
            com.android.server.pm.PackageSetting r3 = r1.mPkgSetting     // Catch: com.android.server.pm.PackageManagerException -> L2c9 java.lang.Throwable -> L6d9
            com.android.server.pm.parsing.pkg.AndroidPackage r3 = r3.getPkg()     // Catch: com.android.server.pm.PackageManagerException -> L2c9 java.lang.Throwable -> L6d9
            boolean r2 = r9.checkNoAppStorageIsConsistent(r2, r3)     // Catch: com.android.server.pm.PackageManagerException -> L2c9 java.lang.Throwable -> L6d9
            if (r2 != 0) goto L299
            com.android.server.pm.PackageInstalledInfo r0 = r13.mInstallResult     // Catch: com.android.server.pm.PackageManagerException -> L2c9 java.lang.Throwable -> L6d9
            r2 = -7
            java.lang.String r3 = "Update attempted to change value of android.internal.PROPERTY_NO_APP_DATA_STORAGE"
            r0.setError(r2, r3)     // Catch: com.android.server.pm.PackageManagerException -> L2c9 java.lang.Throwable -> L6d9
            if (r16 == 0) goto L23c
            java.util.Iterator r0 = r35.iterator()
        L1e0:
            boolean r2 = r0.hasNext()
            if (r2 == 0) goto L23b
            java.lang.Object r2 = r0.next()
            com.android.server.pm.InstallRequest r2 = (com.android.server.pm.InstallRequest) r2
            com.android.server.pm.InstallArgs r3 = r2.mArgs
            int r4 = r3.mDataLoaderType
            r5 = 2
            if (r4 == r5) goto L1f4
            goto L1e0
        L1f4:
            android.content.pm.SigningDetails r4 = r3.mSigningDetails
            int r4 = r4.getSignatureSchemeVersion()
            r5 = 4
            if (r4 == r5) goto L1fe
            goto L1e0
        L1fe:
            com.android.server.pm.PackageInstalledInfo r4 = r2.mInstallResult
            com.android.server.pm.parsing.pkg.AndroidPackage r4 = r4.mPkg
            java.lang.String r4 = r4.getBaseApkPath()
            com.android.server.pm.PackageInstalledInfo r5 = r2.mInstallResult
            com.android.server.pm.parsing.pkg.AndroidPackage r5 = r5.mPkg
            java.lang.String[] r5 = r5.getSplitCodePaths()
            com.android.server.pm.OriginInfo r6 = r3.mOriginInfo
            java.io.File r6 = r6.mResolvedFile
            android.net.Uri r6 = android.net.Uri.fromFile(r6)
            com.android.server.pm.PackageManagerService r7 = r9.mPm
            int r8 = r7.mPendingVerificationToken
            int r11 = r8 + 1
            r7.mPendingVerificationToken = r11
            r27 = r8
            java.lang.String r7 = com.android.server.pm.PackageManagerServiceUtils.buildVerificationRootHashString(r4, r5)
            r29 = 1
            int r8 = r3.mDataLoaderType
            android.os.UserHandle r32 = r3.getUser()
            android.content.Context r11 = r9.mContext
            r28 = r6
            r30 = r7
            r31 = r8
            r33 = r11
            com.android.server.pm.VerificationUtils.broadcastPackageVerified(r27, r28, r29, r30, r31, r32, r33)
            goto L1e0
        L23b:
            goto L295
        L23c:
            java.util.Collection r0 = r10.values()
            java.util.Iterator r0 = r0.iterator()
        L244:
            boolean r2 = r0.hasNext()
            if (r2 == 0) goto L26c
            java.lang.Object r2 = r0.next()
            com.android.server.pm.ScanResult r2 = (com.android.server.pm.ScanResult) r2
            com.android.server.pm.ScanRequest r3 = r2.mRequest
            com.android.server.pm.parsing.pkg.ParsedPackage r3 = r3.mParsedPackage
            java.lang.String r3 = r3.getPackageName()
            java.lang.Boolean r4 = java.lang.Boolean.valueOf(r11)
            java.lang.Object r3 = r15.getOrDefault(r3, r4)
            java.lang.Boolean r3 = (java.lang.Boolean) r3
            boolean r3 = r3.booleanValue()
            if (r3 == 0) goto L26b
            r9.cleanUpAppIdCreation(r2)
        L26b:
            goto L244
        L26c:
            java.util.Iterator r0 = r35.iterator()
        L270:
            boolean r2 = r0.hasNext()
            if (r2 == 0) goto L295
            java.lang.Object r2 = r0.next()
            com.android.server.pm.InstallRequest r2 = (com.android.server.pm.InstallRequest) r2
            com.android.server.pm.PackageInstalledInfo r3 = r2.mInstallResult
            com.android.server.pm.PackageFreezer r3 = r3.mFreezer
            if (r3 == 0) goto L289
            com.android.server.pm.PackageInstalledInfo r3 = r2.mInstallResult
            com.android.server.pm.PackageFreezer r3 = r3.mFreezer
            r3.close()
        L289:
            com.android.server.pm.PackageInstalledInfo r3 = r2.mInstallResult
            int r3 = r3.mReturnCode
            r4 = 1
            if (r3 != r4) goto L294
            com.android.server.pm.PackageInstalledInfo r3 = r2.mInstallResult
            r3.mReturnCode = r11
        L294:
            goto L270
        L295:
            android.os.Trace.traceEnd(r25)
            return
        L299:
            boolean r2 = r9.optimisticallyRegisterAppId(r1)     // Catch: com.android.server.pm.PackageManagerException -> L2c9 java.lang.Throwable -> L6d9
            java.lang.Boolean r2 = java.lang.Boolean.valueOf(r2)     // Catch: com.android.server.pm.PackageManagerException -> L2c9 java.lang.Throwable -> L6d9
            r15.put(r12, r2)     // Catch: com.android.server.pm.PackageManagerException -> L2c9 java.lang.Throwable -> L6d9
            com.android.server.pm.PackageSetting r2 = r1.mPkgSetting     // Catch: com.android.server.pm.PackageManagerException -> L2c9 java.lang.Throwable -> L6d9
            com.android.server.pm.parsing.pkg.AndroidPackage r2 = r2.getPkg()     // Catch: com.android.server.pm.PackageManagerException -> L2c9 java.lang.Throwable -> L6d9
            java.lang.String r2 = r2.getPackageName()     // Catch: com.android.server.pm.PackageManagerException -> L2c9 java.lang.Throwable -> L6d9
            com.android.server.pm.PackageManagerService r3 = r9.mPm     // Catch: com.android.server.pm.PackageManagerException -> L2c9 java.lang.Throwable -> L6d9
            com.android.server.pm.PackageSetting r4 = r1.mPkgSetting     // Catch: com.android.server.pm.PackageManagerException -> L2c9 java.lang.Throwable -> L6d9
            com.android.server.pm.parsing.pkg.AndroidPackage r4 = r4.getPkg()     // Catch: com.android.server.pm.PackageManagerException -> L2c9 java.lang.Throwable -> L6d9
            com.android.server.pm.Settings$VersionInfo r3 = r3.getSettingsVersionForPackage(r4)     // Catch: com.android.server.pm.PackageManagerException -> L2c9 java.lang.Throwable -> L6d9
            r14.put(r2, r3)     // Catch: com.android.server.pm.PackageManagerException -> L2c9 java.lang.Throwable -> L6d9
            r12 = r18
            r11 = r20
            r13 = r24
            r3 = r25
            r6 = 1
            goto L4d
        L2c9:
            r0 = move-exception
            goto L2e3
        L2cb:
            r0 = move-exception
            r11 = r19
            goto L6e3
        L2d0:
            r0 = move-exception
            r11 = r19
            goto L2e3
        L2d4:
            r0 = move-exception
            r17 = r5
            r20 = r11
            r18 = r12
            r24 = r13
            r11 = 0
            r25 = 262144(0x40000, double:1.295163E-318)
            r13 = r2
            r12 = r4
        L2e3:
            com.android.server.pm.PackageInstalledInfo r1 = r13.mInstallResult     // Catch: java.lang.Throwable -> L6d9
            java.lang.String r2 = "Scanning Failed."
            r1.setError(r2, r0)     // Catch: java.lang.Throwable -> L6d9
            if (r16 == 0) goto L34c
            java.util.Iterator r1 = r35.iterator()
        L2f0:
            boolean r2 = r1.hasNext()
            if (r2 == 0) goto L34b
            java.lang.Object r2 = r1.next()
            com.android.server.pm.InstallRequest r2 = (com.android.server.pm.InstallRequest) r2
            com.android.server.pm.InstallArgs r3 = r2.mArgs
            int r4 = r3.mDataLoaderType
            r5 = 2
            if (r4 == r5) goto L304
            goto L2f0
        L304:
            android.content.pm.SigningDetails r4 = r3.mSigningDetails
            int r4 = r4.getSignatureSchemeVersion()
            r5 = 4
            if (r4 == r5) goto L30e
            goto L2f0
        L30e:
            com.android.server.pm.PackageInstalledInfo r4 = r2.mInstallResult
            com.android.server.pm.parsing.pkg.AndroidPackage r4 = r4.mPkg
            java.lang.String r4 = r4.getBaseApkPath()
            com.android.server.pm.PackageInstalledInfo r5 = r2.mInstallResult
            com.android.server.pm.parsing.pkg.AndroidPackage r5 = r5.mPkg
            java.lang.String[] r5 = r5.getSplitCodePaths()
            com.android.server.pm.OriginInfo r6 = r3.mOriginInfo
            java.io.File r6 = r6.mResolvedFile
            android.net.Uri r6 = android.net.Uri.fromFile(r6)
            com.android.server.pm.PackageManagerService r7 = r9.mPm
            int r8 = r7.mPendingVerificationToken
            int r11 = r8 + 1
            r7.mPendingVerificationToken = r11
            r27 = r8
            java.lang.String r7 = com.android.server.pm.PackageManagerServiceUtils.buildVerificationRootHashString(r4, r5)
            r29 = 1
            int r8 = r3.mDataLoaderType
            android.os.UserHandle r32 = r3.getUser()
            android.content.Context r11 = r9.mContext
            r28 = r6
            r30 = r7
            r31 = r8
            r33 = r11
            com.android.server.pm.VerificationUtils.broadcastPackageVerified(r27, r28, r29, r30, r31, r32, r33)
            goto L2f0
        L34b:
            goto L3a5
        L34c:
            java.util.Collection r1 = r10.values()
            java.util.Iterator r1 = r1.iterator()
        L354:
            boolean r2 = r1.hasNext()
            if (r2 == 0) goto L37c
            java.lang.Object r2 = r1.next()
            com.android.server.pm.ScanResult r2 = (com.android.server.pm.ScanResult) r2
            com.android.server.pm.ScanRequest r3 = r2.mRequest
            com.android.server.pm.parsing.pkg.ParsedPackage r3 = r3.mParsedPackage
            java.lang.String r3 = r3.getPackageName()
            java.lang.Boolean r4 = java.lang.Boolean.valueOf(r11)
            java.lang.Object r3 = r15.getOrDefault(r3, r4)
            java.lang.Boolean r3 = (java.lang.Boolean) r3
            boolean r3 = r3.booleanValue()
            if (r3 == 0) goto L37b
            r9.cleanUpAppIdCreation(r2)
        L37b:
            goto L354
        L37c:
            java.util.Iterator r1 = r35.iterator()
        L380:
            boolean r2 = r1.hasNext()
            if (r2 == 0) goto L3a5
            java.lang.Object r2 = r1.next()
            com.android.server.pm.InstallRequest r2 = (com.android.server.pm.InstallRequest) r2
            com.android.server.pm.PackageInstalledInfo r3 = r2.mInstallResult
            com.android.server.pm.PackageFreezer r3 = r3.mFreezer
            if (r3 == 0) goto L399
            com.android.server.pm.PackageInstalledInfo r3 = r2.mInstallResult
            com.android.server.pm.PackageFreezer r3 = r3.mFreezer
            r3.close()
        L399:
            com.android.server.pm.PackageInstalledInfo r3 = r2.mInstallResult
            int r3 = r3.mReturnCode
            r4 = 1
            if (r3 != r4) goto L3a4
            com.android.server.pm.PackageInstalledInfo r3 = r2.mInstallResult
            r3.mReturnCode = r11
        L3a4:
            goto L380
        L3a5:
            android.os.Trace.traceEnd(r25)
            return
        L3a9:
            r0 = move-exception
            r20 = r11
            r18 = r12
            r24 = r13
            r11 = 0
            r25 = 262144(0x40000, double:1.295163E-318)
            goto L6e3
        L3b6:
            r0 = move-exception
            r25 = r3
            r20 = r11
            r18 = r12
            r24 = r13
            r11 = 0
            r13 = r2
            r8 = 4
            goto L4aa
        L3c4:
            r0 = move-exception
            r25 = r3
            r20 = r11
            r18 = r12
            r24 = r13
            r11 = 0
            r13 = r2
            com.android.server.pm.PackageInstalledInfo r1 = r13.mInstallResult     // Catch: java.lang.Throwable -> L4a8
            int r2 = r0.error     // Catch: java.lang.Throwable -> L4a8
            java.lang.String r3 = r0.getMessage()     // Catch: java.lang.Throwable -> L4a8
            r1.setError(r2, r3)     // Catch: java.lang.Throwable -> L4a8
            com.android.server.pm.PackageInstalledInfo r1 = r13.mInstallResult     // Catch: java.lang.Throwable -> L4a8
            java.lang.String r2 = r0.mConflictingPackage     // Catch: java.lang.Throwable -> L4a8
            r1.mOrigPackage = r2     // Catch: java.lang.Throwable -> L4a8
            com.android.server.pm.PackageInstalledInfo r1 = r13.mInstallResult     // Catch: java.lang.Throwable -> L4a8
            java.lang.String r2 = r0.mConflictingPermission     // Catch: java.lang.Throwable -> L4a8
            r1.mOrigPermission = r2     // Catch: java.lang.Throwable -> L4a8
            android.os.Trace.traceEnd(r25)     // Catch: java.lang.Throwable -> L6d9
            if (r16 == 0) goto L44b
            java.util.Iterator r1 = r35.iterator()
        L3ef:
            boolean r2 = r1.hasNext()
            if (r2 == 0) goto L44a
            java.lang.Object r2 = r1.next()
            com.android.server.pm.InstallRequest r2 = (com.android.server.pm.InstallRequest) r2
            com.android.server.pm.InstallArgs r3 = r2.mArgs
            int r4 = r3.mDataLoaderType
            r5 = 2
            if (r4 == r5) goto L403
            goto L3ef
        L403:
            android.content.pm.SigningDetails r4 = r3.mSigningDetails
            int r4 = r4.getSignatureSchemeVersion()
            r8 = 4
            if (r4 == r8) goto L40d
            goto L3ef
        L40d:
            com.android.server.pm.PackageInstalledInfo r4 = r2.mInstallResult
            com.android.server.pm.parsing.pkg.AndroidPackage r4 = r4.mPkg
            java.lang.String r4 = r4.getBaseApkPath()
            com.android.server.pm.PackageInstalledInfo r5 = r2.mInstallResult
            com.android.server.pm.parsing.pkg.AndroidPackage r5 = r5.mPkg
            java.lang.String[] r5 = r5.getSplitCodePaths()
            com.android.server.pm.OriginInfo r6 = r3.mOriginInfo
            java.io.File r6 = r6.mResolvedFile
            android.net.Uri r6 = android.net.Uri.fromFile(r6)
            com.android.server.pm.PackageManagerService r7 = r9.mPm
            int r11 = r7.mPendingVerificationToken
            int r12 = r11 + 1
            r7.mPendingVerificationToken = r12
            r27 = r11
            java.lang.String r7 = com.android.server.pm.PackageManagerServiceUtils.buildVerificationRootHashString(r4, r5)
            r29 = 1
            int r11 = r3.mDataLoaderType
            android.os.UserHandle r32 = r3.getUser()
            android.content.Context r12 = r9.mContext
            r28 = r6
            r30 = r7
            r31 = r11
            r33 = r12
            com.android.server.pm.VerificationUtils.broadcastPackageVerified(r27, r28, r29, r30, r31, r32, r33)
            goto L3ef
        L44a:
            goto L4a4
        L44b:
            java.util.Collection r1 = r10.values()
            java.util.Iterator r1 = r1.iterator()
        L453:
            boolean r2 = r1.hasNext()
            if (r2 == 0) goto L47b
            java.lang.Object r2 = r1.next()
            com.android.server.pm.ScanResult r2 = (com.android.server.pm.ScanResult) r2
            com.android.server.pm.ScanRequest r3 = r2.mRequest
            com.android.server.pm.parsing.pkg.ParsedPackage r3 = r3.mParsedPackage
            java.lang.String r3 = r3.getPackageName()
            java.lang.Boolean r4 = java.lang.Boolean.valueOf(r11)
            java.lang.Object r3 = r15.getOrDefault(r3, r4)
            java.lang.Boolean r3 = (java.lang.Boolean) r3
            boolean r3 = r3.booleanValue()
            if (r3 == 0) goto L47a
            r9.cleanUpAppIdCreation(r2)
        L47a:
            goto L453
        L47b:
            java.util.Iterator r1 = r35.iterator()
        L47f:
            boolean r2 = r1.hasNext()
            if (r2 == 0) goto L4a4
            java.lang.Object r2 = r1.next()
            com.android.server.pm.InstallRequest r2 = (com.android.server.pm.InstallRequest) r2
            com.android.server.pm.PackageInstalledInfo r3 = r2.mInstallResult
            com.android.server.pm.PackageFreezer r3 = r3.mFreezer
            if (r3 == 0) goto L498
            com.android.server.pm.PackageInstalledInfo r3 = r2.mInstallResult
            com.android.server.pm.PackageFreezer r3 = r3.mFreezer
            r3.close()
        L498:
            com.android.server.pm.PackageInstalledInfo r3 = r2.mInstallResult
            int r3 = r3.mReturnCode
            r4 = 1
            if (r3 != r4) goto L4a3
            com.android.server.pm.PackageInstalledInfo r3 = r2.mInstallResult
            r3.mReturnCode = r11
        L4a3:
            goto L47f
        L4a4:
            android.os.Trace.traceEnd(r25)
            return
        L4a8:
            r0 = move-exception
            r8 = 4
        L4aa:
            android.os.Trace.traceEnd(r25)     // Catch: java.lang.Throwable -> L6d9
            throw r0     // Catch: java.lang.Throwable -> L6d9
        L4af:
            r0 = move-exception
            r20 = r11
            r18 = r12
            r24 = r13
            r8 = 4
            goto L6e2
        L4b9:
            r25 = r3
            r20 = r11
            r18 = r12
            r24 = r13
            r8 = 4
            r11 = 0
            com.android.server.pm.ReconcileRequest r0 = new com.android.server.pm.ReconcileRequest     // Catch: java.lang.Throwable -> L6d9
            com.android.server.pm.PackageManagerService r1 = r9.mPm     // Catch: java.lang.Throwable -> L6d9
            com.android.server.utils.WatchedArrayMap<java.lang.String, com.android.server.pm.parsing.pkg.AndroidPackage> r1 = r1.mPackages     // Catch: java.lang.Throwable -> L6d9
            java.util.Map r7 = java.util.Collections.unmodifiableMap(r1)     // Catch: java.lang.Throwable -> L6d9
            r2 = r0
            r3 = r10
            r4 = r20
            r5 = r18
            r6 = r24
            r1 = r8
            r8 = r14
            r2.<init>(r3, r4, r5, r6, r7, r8)     // Catch: java.lang.Throwable -> L6d9
            r2 = r0
            r3 = 0
            com.android.server.pm.PackageManagerService r0 = r9.mPm     // Catch: java.lang.Throwable -> L6d9
            com.android.server.pm.PackageManagerTracedLock r4 = r0.mLock     // Catch: java.lang.Throwable -> L6d9
            monitor-enter(r4)     // Catch: java.lang.Throwable -> L6d9
            java.lang.String r0 = "reconcilePackages"
            r5 = r25
            android.os.Trace.traceBegin(r5, r0)     // Catch: com.android.server.pm.ReconcileFailure -> L5e5 java.lang.Throwable -> L5e7
            com.android.server.pm.SharedLibrariesImpl r0 = r9.mSharedLibraries     // Catch: com.android.server.pm.ReconcileFailure -> L5e5 java.lang.Throwable -> L5e7
            com.android.server.pm.PackageManagerService r7 = r9.mPm     // Catch: com.android.server.pm.ReconcileFailure -> L5e5 java.lang.Throwable -> L5e7
            com.android.server.pm.Settings r7 = r7.mSettings     // Catch: com.android.server.pm.ReconcileFailure -> L5e5 java.lang.Throwable -> L5e7
            com.android.server.pm.KeySetManagerService r7 = r7.getKeySetManagerService()     // Catch: com.android.server.pm.ReconcileFailure -> L5e5 java.lang.Throwable -> L5e7
            com.android.server.pm.PackageManagerService r8 = r9.mPm     // Catch: com.android.server.pm.ReconcileFailure -> L5e5 java.lang.Throwable -> L5e7
            com.android.server.pm.Settings r8 = r8.mSettings     // Catch: com.android.server.pm.ReconcileFailure -> L5e5 java.lang.Throwable -> L5e7
            java.util.Map r0 = com.android.server.pm.ReconcilePackageUtils.reconcilePackages(r2, r0, r7, r8)     // Catch: com.android.server.pm.ReconcileFailure -> L5e5 java.lang.Throwable -> L5e7
            r7 = r0
            android.os.Trace.traceEnd(r5)     // Catch: java.lang.Throwable -> L6d6
            java.lang.String r0 = "commitPackages"
            android.os.Trace.traceBegin(r5, r0)     // Catch: java.lang.Throwable -> L5df
            com.android.server.pm.CommitRequest r0 = new com.android.server.pm.CommitRequest     // Catch: java.lang.Throwable -> L5df
            com.android.server.pm.PackageManagerService r8 = r9.mPm     // Catch: java.lang.Throwable -> L5df
            com.android.server.pm.UserManagerService r8 = r8.mUserManager     // Catch: java.lang.Throwable -> L5df
            int[] r8 = r8.getUserIds()     // Catch: java.lang.Throwable -> L5df
            r0.<init>(r7, r8)     // Catch: java.lang.Throwable -> L5df
            r3 = r0
            r9.commitPackagesLocked(r3)     // Catch: java.lang.Throwable -> L5df
            r16 = 1
            android.os.Trace.traceEnd(r5)     // Catch: java.lang.Throwable -> L6d6
            monitor-exit(r4)     // Catch: java.lang.Throwable -> L6d6
            r9.executePostCommitSteps(r3)     // Catch: java.lang.Throwable -> L6d9
            if (r16 == 0) goto L581
            java.util.Iterator r0 = r35.iterator()
        L526:
            boolean r2 = r0.hasNext()
            if (r2 == 0) goto L580
            java.lang.Object r2 = r0.next()
            com.android.server.pm.InstallRequest r2 = (com.android.server.pm.InstallRequest) r2
            com.android.server.pm.InstallArgs r3 = r2.mArgs
            int r4 = r3.mDataLoaderType
            r7 = 2
            if (r4 == r7) goto L53a
            goto L526
        L53a:
            android.content.pm.SigningDetails r4 = r3.mSigningDetails
            int r4 = r4.getSignatureSchemeVersion()
            if (r4 == r1) goto L543
            goto L526
        L543:
            com.android.server.pm.PackageInstalledInfo r4 = r2.mInstallResult
            com.android.server.pm.parsing.pkg.AndroidPackage r4 = r4.mPkg
            java.lang.String r4 = r4.getBaseApkPath()
            com.android.server.pm.PackageInstalledInfo r7 = r2.mInstallResult
            com.android.server.pm.parsing.pkg.AndroidPackage r7 = r7.mPkg
            java.lang.String[] r7 = r7.getSplitCodePaths()
            com.android.server.pm.OriginInfo r8 = r3.mOriginInfo
            java.io.File r8 = r8.mResolvedFile
            android.net.Uri r8 = android.net.Uri.fromFile(r8)
            com.android.server.pm.PackageManagerService r11 = r9.mPm
            int r12 = r11.mPendingVerificationToken
            int r13 = r12 + 1
            r11.mPendingVerificationToken = r13
            r25 = r12
            java.lang.String r11 = com.android.server.pm.PackageManagerServiceUtils.buildVerificationRootHashString(r4, r7)
            r27 = 1
            int r12 = r3.mDataLoaderType
            android.os.UserHandle r30 = r3.getUser()
            android.content.Context r13 = r9.mContext
            r26 = r8
            r28 = r11
            r29 = r12
            r31 = r13
            com.android.server.pm.VerificationUtils.broadcastPackageVerified(r25, r26, r27, r28, r29, r30, r31)
            goto L526
        L580:
            goto L5da
        L581:
            java.util.Collection r0 = r10.values()
            java.util.Iterator r0 = r0.iterator()
        L589:
            boolean r1 = r0.hasNext()
            if (r1 == 0) goto L5b1
            java.lang.Object r1 = r0.next()
            com.android.server.pm.ScanResult r1 = (com.android.server.pm.ScanResult) r1
            com.android.server.pm.ScanRequest r2 = r1.mRequest
            com.android.server.pm.parsing.pkg.ParsedPackage r2 = r2.mParsedPackage
            java.lang.String r2 = r2.getPackageName()
            java.lang.Boolean r3 = java.lang.Boolean.valueOf(r11)
            java.lang.Object r2 = r15.getOrDefault(r2, r3)
            java.lang.Boolean r2 = (java.lang.Boolean) r2
            boolean r2 = r2.booleanValue()
            if (r2 == 0) goto L5b0
            r9.cleanUpAppIdCreation(r1)
        L5b0:
            goto L589
        L5b1:
            java.util.Iterator r0 = r35.iterator()
        L5b5:
            boolean r1 = r0.hasNext()
            if (r1 == 0) goto L5da
            java.lang.Object r1 = r0.next()
            com.android.server.pm.InstallRequest r1 = (com.android.server.pm.InstallRequest) r1
            com.android.server.pm.PackageInstalledInfo r2 = r1.mInstallResult
            com.android.server.pm.PackageFreezer r2 = r2.mFreezer
            if (r2 == 0) goto L5ce
            com.android.server.pm.PackageInstalledInfo r2 = r1.mInstallResult
            com.android.server.pm.PackageFreezer r2 = r2.mFreezer
            r2.close()
        L5ce:
            com.android.server.pm.PackageInstalledInfo r2 = r1.mInstallResult
            int r2 = r2.mReturnCode
            r3 = 1
            if (r2 != r3) goto L5d9
            com.android.server.pm.PackageInstalledInfo r2 = r1.mInstallResult
            r2.mReturnCode = r11
        L5d9:
            goto L5b5
        L5da:
            android.os.Trace.traceEnd(r5)
            return
        L5df:
            r0 = move-exception
            android.os.Trace.traceEnd(r5)     // Catch: java.lang.Throwable -> L6d6
            throw r0     // Catch: java.lang.Throwable -> L6d6
        L5e5:
            r0 = move-exception
            goto L5ed
        L5e7:
            r0 = move-exception
            goto L6ce
        L5ea:
            r0 = move-exception
            r5 = r25
        L5ed:
            java.util.Iterator r7 = r35.iterator()     // Catch: java.lang.Throwable -> L5e7
        L5f1:
            boolean r8 = r7.hasNext()     // Catch: java.lang.Throwable -> L5e7
            if (r8 == 0) goto L605
            java.lang.Object r8 = r7.next()     // Catch: java.lang.Throwable -> L5e7
            com.android.server.pm.InstallRequest r8 = (com.android.server.pm.InstallRequest) r8     // Catch: java.lang.Throwable -> L5e7
            com.android.server.pm.PackageInstalledInfo r12 = r8.mInstallResult     // Catch: java.lang.Throwable -> L5e7
            java.lang.String r13 = "Reconciliation failed..."
            r12.setError(r13, r0)     // Catch: java.lang.Throwable -> L5e7
            goto L5f1
        L605:
            android.os.Trace.traceEnd(r5)     // Catch: java.lang.Throwable -> L6d6
            monitor-exit(r4)     // Catch: java.lang.Throwable -> L6d6
            if (r16 == 0) goto L66e
            java.util.Iterator r4 = r35.iterator()
        L60f:
            boolean r7 = r4.hasNext()
            if (r7 == 0) goto L66d
            java.lang.Object r7 = r4.next()
            com.android.server.pm.InstallRequest r7 = (com.android.server.pm.InstallRequest) r7
            com.android.server.pm.InstallArgs r8 = r7.mArgs
            int r11 = r8.mDataLoaderType
            r12 = 2
            if (r11 == r12) goto L623
            goto L60f
        L623:
            android.content.pm.SigningDetails r11 = r8.mSigningDetails
            int r11 = r11.getSignatureSchemeVersion()
            if (r11 == r1) goto L62c
            goto L60f
        L62c:
            com.android.server.pm.PackageInstalledInfo r11 = r7.mInstallResult
            com.android.server.pm.parsing.pkg.AndroidPackage r11 = r11.mPkg
            java.lang.String r11 = r11.getBaseApkPath()
            com.android.server.pm.PackageInstalledInfo r12 = r7.mInstallResult
            com.android.server.pm.parsing.pkg.AndroidPackage r12 = r12.mPkg
            java.lang.String[] r12 = r12.getSplitCodePaths()
            com.android.server.pm.OriginInfo r13 = r8.mOriginInfo
            java.io.File r13 = r13.mResolvedFile
            android.net.Uri r13 = android.net.Uri.fromFile(r13)
            com.android.server.pm.PackageManagerService r1 = r9.mPm
            int r5 = r1.mPendingVerificationToken
            int r6 = r5 + 1
            r1.mPendingVerificationToken = r6
            r25 = r5
            java.lang.String r1 = com.android.server.pm.PackageManagerServiceUtils.buildVerificationRootHashString(r11, r12)
            r27 = 1
            int r5 = r8.mDataLoaderType
            android.os.UserHandle r30 = r8.getUser()
            android.content.Context r6 = r9.mContext
            r26 = r13
            r28 = r1
            r29 = r5
            r31 = r6
            com.android.server.pm.VerificationUtils.broadcastPackageVerified(r25, r26, r27, r28, r29, r30, r31)
            r1 = 4
            r5 = 262144(0x40000, double:1.295163E-318)
            goto L60f
        L66d:
            goto L6c7
        L66e:
            java.util.Collection r1 = r10.values()
            java.util.Iterator r1 = r1.iterator()
        L676:
            boolean r4 = r1.hasNext()
            if (r4 == 0) goto L69e
            java.lang.Object r4 = r1.next()
            com.android.server.pm.ScanResult r4 = (com.android.server.pm.ScanResult) r4
            com.android.server.pm.ScanRequest r5 = r4.mRequest
            com.android.server.pm.parsing.pkg.ParsedPackage r5 = r5.mParsedPackage
            java.lang.String r5 = r5.getPackageName()
            java.lang.Boolean r6 = java.lang.Boolean.valueOf(r11)
            java.lang.Object r5 = r15.getOrDefault(r5, r6)
            java.lang.Boolean r5 = (java.lang.Boolean) r5
            boolean r5 = r5.booleanValue()
            if (r5 == 0) goto L69d
            r9.cleanUpAppIdCreation(r4)
        L69d:
            goto L676
        L69e:
            java.util.Iterator r1 = r35.iterator()
        L6a2:
            boolean r4 = r1.hasNext()
            if (r4 == 0) goto L6c7
            java.lang.Object r4 = r1.next()
            com.android.server.pm.InstallRequest r4 = (com.android.server.pm.InstallRequest) r4
            com.android.server.pm.PackageInstalledInfo r5 = r4.mInstallResult
            com.android.server.pm.PackageFreezer r5 = r5.mFreezer
            if (r5 == 0) goto L6bb
            com.android.server.pm.PackageInstalledInfo r5 = r4.mInstallResult
            com.android.server.pm.PackageFreezer r5 = r5.mFreezer
            r5.close()
        L6bb:
            com.android.server.pm.PackageInstalledInfo r5 = r4.mInstallResult
            int r5 = r5.mReturnCode
            r6 = 1
            if (r5 != r6) goto L6c6
            com.android.server.pm.PackageInstalledInfo r5 = r4.mInstallResult
            r5.mReturnCode = r11
        L6c6:
            goto L6a2
        L6c7:
            r4 = 262144(0x40000, double:1.295163E-318)
            android.os.Trace.traceEnd(r4)
            return
        L6ce:
            r5 = 262144(0x40000, double:1.295163E-318)
            android.os.Trace.traceEnd(r5)     // Catch: java.lang.Throwable -> L6d6
            throw r0     // Catch: java.lang.Throwable -> L6d6
        L6d6:
            r0 = move-exception
            monitor-exit(r4)     // Catch: java.lang.Throwable -> L6d6
            throw r0     // Catch: java.lang.Throwable -> L6d9
        L6d9:
            r0 = move-exception
            goto L6e3
        L6db:
            r0 = move-exception
            r20 = r11
            r18 = r12
            r24 = r13
        L6e2:
            r11 = 0
        L6e3:
            if (r16 == 0) goto L745
            java.util.Iterator r1 = r35.iterator()
        L6e9:
            boolean r2 = r1.hasNext()
            if (r2 == 0) goto L744
            java.lang.Object r2 = r1.next()
            com.android.server.pm.InstallRequest r2 = (com.android.server.pm.InstallRequest) r2
            com.android.server.pm.InstallArgs r3 = r2.mArgs
            int r4 = r3.mDataLoaderType
            r5 = 2
            if (r4 == r5) goto L6fd
            goto L6e9
        L6fd:
            android.content.pm.SigningDetails r4 = r3.mSigningDetails
            int r4 = r4.getSignatureSchemeVersion()
            r6 = 4
            if (r4 == r6) goto L707
            goto L6e9
        L707:
            com.android.server.pm.PackageInstalledInfo r4 = r2.mInstallResult
            com.android.server.pm.parsing.pkg.AndroidPackage r4 = r4.mPkg
            java.lang.String r4 = r4.getBaseApkPath()
            com.android.server.pm.PackageInstalledInfo r7 = r2.mInstallResult
            com.android.server.pm.parsing.pkg.AndroidPackage r7 = r7.mPkg
            java.lang.String[] r7 = r7.getSplitCodePaths()
            com.android.server.pm.OriginInfo r8 = r3.mOriginInfo
            java.io.File r8 = r8.mResolvedFile
            android.net.Uri r8 = android.net.Uri.fromFile(r8)
            com.android.server.pm.PackageManagerService r11 = r9.mPm
            int r12 = r11.mPendingVerificationToken
            int r13 = r12 + 1
            r11.mPendingVerificationToken = r13
            r25 = r12
            java.lang.String r11 = com.android.server.pm.PackageManagerServiceUtils.buildVerificationRootHashString(r4, r7)
            r27 = 1
            int r12 = r3.mDataLoaderType
            android.os.UserHandle r30 = r3.getUser()
            android.content.Context r13 = r9.mContext
            r26 = r8
            r28 = r11
            r29 = r12
            r31 = r13
            com.android.server.pm.VerificationUtils.broadcastPackageVerified(r25, r26, r27, r28, r29, r30, r31)
            goto L6e9
        L744:
            goto L79e
        L745:
            java.util.Collection r1 = r10.values()
            java.util.Iterator r1 = r1.iterator()
        L74d:
            boolean r2 = r1.hasNext()
            if (r2 == 0) goto L775
            java.lang.Object r2 = r1.next()
            com.android.server.pm.ScanResult r2 = (com.android.server.pm.ScanResult) r2
            com.android.server.pm.ScanRequest r3 = r2.mRequest
            com.android.server.pm.parsing.pkg.ParsedPackage r3 = r3.mParsedPackage
            java.lang.String r3 = r3.getPackageName()
            java.lang.Boolean r4 = java.lang.Boolean.valueOf(r11)
            java.lang.Object r3 = r15.getOrDefault(r3, r4)
            java.lang.Boolean r3 = (java.lang.Boolean) r3
            boolean r3 = r3.booleanValue()
            if (r3 == 0) goto L774
            r9.cleanUpAppIdCreation(r2)
        L774:
            goto L74d
        L775:
            java.util.Iterator r1 = r35.iterator()
        L779:
            boolean r2 = r1.hasNext()
            if (r2 == 0) goto L79e
            java.lang.Object r2 = r1.next()
            com.android.server.pm.InstallRequest r2 = (com.android.server.pm.InstallRequest) r2
            com.android.server.pm.PackageInstalledInfo r3 = r2.mInstallResult
            com.android.server.pm.PackageFreezer r3 = r3.mFreezer
            if (r3 == 0) goto L792
            com.android.server.pm.PackageInstalledInfo r3 = r2.mInstallResult
            com.android.server.pm.PackageFreezer r3 = r3.mFreezer
            r3.close()
        L792:
            com.android.server.pm.PackageInstalledInfo r3 = r2.mInstallResult
            int r3 = r3.mReturnCode
            r4 = 1
            if (r3 != r4) goto L79d
            com.android.server.pm.PackageInstalledInfo r3 = r2.mInstallResult
            r3.mReturnCode = r11
        L79d:
            goto L779
        L79e:
            r1 = 262144(0x40000, double:1.295163E-318)
            android.os.Trace.traceEnd(r1)
            throw r0
        */
        throw new UnsupportedOperationException("Method not decompiled: com.android.server.pm.InstallPackageHelper.installPackagesLI(java.util.List):void");
    }

    private boolean checkNoAppStorageIsConsistent(AndroidPackage oldPkg, AndroidPackage newPkg) {
        if (oldPkg == null) {
            return true;
        }
        PackageManager.Property curProp = oldPkg.getProperties().get("android.internal.PROPERTY_NO_APP_DATA_STORAGE");
        PackageManager.Property newProp = newPkg.getProperties().get("android.internal.PROPERTY_NO_APP_DATA_STORAGE");
        if (curProp == null || !curProp.getBoolean()) {
            if (newProp == null || !newProp.getBoolean()) {
                return true;
            }
            return false;
        } else if (newProp != null && newProp.getBoolean()) {
            return true;
        } else {
            return false;
        }
    }

    /*  JADX ERROR: JadxRuntimeException in pass: BlockProcessor
        jadx.core.utils.exceptions.JadxRuntimeException: Unreachable block: B:454:0x0b04
        	at jadx.core.dex.visitors.blocks.BlockProcessor.checkForUnreachableBlocks(BlockProcessor.java:81)
        	at jadx.core.dex.visitors.blocks.BlockProcessor.processBlocksTree(BlockProcessor.java:47)
        	at jadx.core.dex.visitors.blocks.BlockProcessor.visit(BlockProcessor.java:39)
        */
    /* JADX DEBUG: Don't trust debug lines info. Repeating lines: [1169=4, 1172=4, 1724=10, 1819=11, 1503=18] */
    private com.android.server.pm.PrepareResult preparePackageLI(com.android.server.pm.InstallArgs r44, com.android.server.pm.PackageInstalledInfo r45) throws com.android.server.pm.PrepareFailure {
        /*
            r43 = this;
            r1 = r43
            r2 = r44
            r3 = r45
            int r4 = r2.mInstallFlags
            java.io.File r5 = new java.io.File
            java.lang.String r6 = r44.getCodePath()
            r5.<init>(r6)
            java.lang.String r6 = r2.mVolumeUuid
            if (r6 == 0) goto L17
            r6 = 1
            goto L18
        L17:
            r6 = 0
        L18:
            r9 = r4 & 2048(0x800, float:2.87E-42)
            if (r9 == 0) goto L1e
            r9 = 1
            goto L1f
        L1e:
            r9 = 0
        L1f:
            r10 = r4 & 16384(0x4000, float:2.2959E-41)
            if (r10 == 0) goto L25
            r10 = 1
            goto L26
        L25:
            r10 = 0
        L26:
            r11 = 65536(0x10000, float:9.18355E-41)
            r12 = r4 & r11
            if (r12 == 0) goto L2e
            r12 = 1
            goto L2f
        L2e:
            r12 = 0
        L2f:
            int r13 = r2.mInstallReason
            r14 = 5
            if (r13 != r14) goto L36
            r13 = 1
            goto L37
        L36:
            r13 = 0
        L37:
            r14 = 6
            com.android.server.pm.MoveInfo r15 = r2.mMoveInfo
            if (r15 == 0) goto L3e
            r14 = r14 | 512(0x200, float:7.175E-43)
        L3e:
            r15 = r4 & 4096(0x1000, float:5.74E-42)
            if (r15 == 0) goto L44
            r14 = r14 | 1024(0x400, float:1.435E-42)
        L44:
            if (r9 == 0) goto L48
            r14 = r14 | 8192(0x2000, float:1.14794E-41)
        L48:
            if (r10 == 0) goto L4c
            r14 = r14 | 16384(0x4000, float:2.2959E-41)
        L4c:
            if (r12 == 0) goto L54
            r15 = 32768(0x8000, float:4.5918E-41)
            r14 = r14 | r15
            r15 = r14
            goto L55
        L54:
            r15 = r14
        L55:
            boolean r14 = com.android.server.pm.PackageManagerService.DEBUG_INSTALL
            if (r14 == 0) goto L71
            java.lang.String r14 = "PackageManager"
            java.lang.StringBuilder r11 = new java.lang.StringBuilder
            r11.<init>()
            java.lang.String r7 = "installPackageLI: path="
            java.lang.StringBuilder r7 = r11.append(r7)
            java.lang.StringBuilder r7 = r7.append(r5)
            java.lang.String r7 = r7.toString()
            android.util.Slog.d(r14, r7)
        L71:
            r7 = -116(0xffffffffffffff8c, float:NaN)
            if (r9 == 0) goto L96
            if (r6 != 0) goto L78
            goto L96
        L78:
            java.lang.String r8 = "PackageManager"
            java.lang.StringBuilder r11 = new java.lang.StringBuilder
            r11.<init>()
            java.lang.String r14 = "Incompatible ephemeral install; external="
            java.lang.StringBuilder r11 = r11.append(r14)
            java.lang.StringBuilder r11 = r11.append(r6)
            java.lang.String r11 = r11.toString()
            android.util.Slog.i(r8, r11)
            com.android.server.pm.PrepareFailure r8 = new com.android.server.pm.PrepareFailure
            r8.<init>(r7)
            throw r8
        L96:
            com.android.server.pm.PackageManagerService r11 = r1.mPm
            int r11 = r11.getDefParseFlags()
            r14 = -2147483648(0xffffffff80000000, float:-0.0)
            r11 = r11 | r14
            r11 = r11 | 64
            if (r6 == 0) goto La6
            r16 = 8
            goto La8
        La6:
            r16 = 0
        La8:
            r11 = r11 | r16
            java.lang.String r14 = "parsePackage"
            r7 = 262144(0x40000, double:1.295163E-318)
            android.os.Trace.traceBegin(r7, r14)
            com.android.server.pm.PackageManagerService r14 = r1.mPm     // Catch: java.lang.Throwable -> L11f9 com.android.server.pm.PackageManagerException -> L120e
            com.android.server.pm.PackageManagerServiceInjector r14 = r14.mInjector     // Catch: java.lang.Throwable -> L11f9 com.android.server.pm.PackageManagerException -> L120e
            com.android.server.pm.parsing.PackageParser2 r14 = r14.getPreparingPackageParser()     // Catch: java.lang.Throwable -> L11f9 com.android.server.pm.PackageManagerException -> L120e
            r7 = 0
            com.android.server.pm.parsing.pkg.ParsedPackage r8 = r14.parsePackage(r5, r11, r7)     // Catch: java.lang.Throwable -> L11d6
            r7 = r8
            com.android.server.pm.parsing.pkg.AndroidPackageUtils.validatePackageDexMetadata(r7)     // Catch: java.lang.Throwable -> L11d6
            if (r14 == 0) goto Lf6
            r14.close()     // Catch: java.lang.Throwable -> Lca com.android.server.pm.PackageManagerException -> Le0
            goto Lf6
        Lca:
            r0 = move-exception
            r2 = r0
            r40 = r4
            r33 = r5
            r38 = r6
            r37 = r9
            r34 = r10
            r36 = r11
            r35 = r12
            r20 = r13
            r10 = r15
            r12 = r3
            goto L122c
        Le0:
            r0 = move-exception
            r2 = r0
            r40 = r4
            r33 = r5
            r38 = r6
            r37 = r9
            r34 = r10
            r36 = r11
            r35 = r12
            r20 = r13
            r10 = r15
            r12 = r3
            goto L1222
        Lf6:
            r17 = 262144(0x40000, double:1.295163E-318)
            android.os.Trace.traceEnd(r17)
            com.transsion.hubcore.server.pm.ITranPackageManagerService r8 = com.transsion.hubcore.server.pm.ITranPackageManagerService.Instance()     // Catch: com.android.server.pm.PackageManagerException -> L11b6
            r8.preparePackageLI(r7)     // Catch: com.android.server.pm.PackageManagerException -> L11b6
            if (r9 == 0) goto L174
            int r8 = r7.getTargetSdkVersion()
            r14 = 26
            if (r8 < r14) goto L146
            java.lang.String r8 = r7.getSharedUserId()
            if (r8 != 0) goto L118
            r33 = r5
            goto L176
        L118:
            java.lang.String r8 = "PackageManager"
            java.lang.StringBuilder r14 = new java.lang.StringBuilder
            r14.<init>()
            r33 = r5
            java.lang.String r5 = "Instant app package "
            java.lang.StringBuilder r5 = r14.append(r5)
            java.lang.String r14 = r7.getPackageName()
            java.lang.StringBuilder r5 = r5.append(r14)
            java.lang.String r14 = " may not declare sharedUserId."
            java.lang.StringBuilder r5 = r5.append(r14)
            java.lang.String r5 = r5.toString()
            android.util.Slog.w(r8, r5)
            com.android.server.pm.PrepareFailure r5 = new com.android.server.pm.PrepareFailure
            java.lang.String r8 = "Instant app package may not declare a sharedUserId"
            r14 = -116(0xffffffffffffff8c, float:NaN)
            r5.<init>(r14, r8)
            throw r5
        L146:
            r33 = r5
            java.lang.String r5 = "PackageManager"
            java.lang.StringBuilder r8 = new java.lang.StringBuilder
            r8.<init>()
            java.lang.String r14 = "Instant app package "
            java.lang.StringBuilder r8 = r8.append(r14)
            java.lang.String r14 = r7.getPackageName()
            java.lang.StringBuilder r8 = r8.append(r14)
            java.lang.String r14 = " does not target at least O"
            java.lang.StringBuilder r8 = r8.append(r14)
            java.lang.String r8 = r8.toString()
            android.util.Slog.w(r5, r8)
            com.android.server.pm.PrepareFailure r5 = new com.android.server.pm.PrepareFailure
            java.lang.String r8 = "Instant app package must target at least O"
            r14 = -116(0xffffffffffffff8c, float:NaN)
            r5.<init>(r14, r8)
            throw r5
        L174:
            r33 = r5
        L176:
            boolean r5 = r7.isStaticSharedLibrary()
            r8 = -19
            if (r5 == 0) goto L193
            com.android.server.pm.PackageManagerService.renameStaticSharedLibraryPackage(r7)
            if (r6 != 0) goto L184
            goto L193
        L184:
            java.lang.String r5 = "PackageManager"
            java.lang.String r14 = "Static shared libs can only be installed on internal storage."
            android.util.Slog.i(r5, r14)
            com.android.server.pm.PrepareFailure r5 = new com.android.server.pm.PrepareFailure
            java.lang.String r14 = "Packages declaring static-shared libs cannot be updated"
            r5.<init>(r8, r14)
            throw r5
        L193:
            java.lang.String r5 = r7.getPackageName()
            r3.mName = r5
            boolean r14 = r7.isTestOnly()
            if (r14 == 0) goto L1b2
            r14 = r4 & 4
            if (r14 == 0) goto L1a6
            r34 = r10
            goto L1b4
        L1a6:
            com.android.server.pm.PrepareFailure r8 = new com.android.server.pm.PrepareFailure
            r14 = -15
            r34 = r10
            java.lang.String r10 = "installPackageLI"
            r8.<init>(r14, r10)
            throw r8
        L1b2:
            r34 = r10
        L1b4:
            android.content.pm.SigningDetails r10 = r2.mSigningDetails
            android.content.pm.SigningDetails r14 = android.content.pm.SigningDetails.UNKNOWN
            if (r10 == r14) goto L1c0
            android.content.pm.SigningDetails r10 = r2.mSigningDetails
            r7.setSigningDetails(r10)
            goto L1d8
        L1c0:
            android.content.pm.parsing.result.ParseTypeImpl r10 = android.content.pm.parsing.result.ParseTypeImpl.forDefaultParsing()
            r14 = 0
            android.content.pm.parsing.result.ParseResult r17 = com.android.server.pm.pkg.parsing.ParsingPackageUtils.getSigningDetails(r10, r7, r14)
            boolean r14 = r17.isError()
            if (r14 != 0) goto L11a3
            java.lang.Object r14 = r17.getResult()
            android.content.pm.SigningDetails r14 = (android.content.pm.SigningDetails) r14
            r7.setSigningDetails(r14)
        L1d8:
            r10 = 2
            if (r9 == 0) goto L212
            android.content.pm.SigningDetails r14 = r7.getSigningDetails()
            int r14 = r14.getSignatureSchemeVersion()
            if (r14 < r10) goto L1e6
            goto L212
        L1e6:
            java.lang.String r8 = "PackageManager"
            java.lang.StringBuilder r10 = new java.lang.StringBuilder
            r10.<init>()
            java.lang.String r14 = "Instant app package "
            java.lang.StringBuilder r10 = r10.append(r14)
            java.lang.String r14 = r7.getPackageName()
            java.lang.StringBuilder r10 = r10.append(r14)
            java.lang.String r14 = " is not signed with at least APK Signature Scheme v2"
            java.lang.StringBuilder r10 = r10.append(r14)
            java.lang.String r10 = r10.toString()
            android.util.Slog.w(r8, r10)
            com.android.server.pm.PrepareFailure r8 = new com.android.server.pm.PrepareFailure
            java.lang.String r10 = "Instant app package must be signed with APK Signature Scheme v2 or greater"
            r14 = -116(0xffffffffffffff8c, float:NaN)
            r8.<init>(r14, r10)
            throw r8
        L212:
            r24 = 0
            r14 = 0
            com.android.server.pm.PackageManagerService r8 = r1.mPm
            com.android.server.pm.PackageManagerTracedLock r8 = r8.mLock
            monitor-enter(r8)
            r17 = r4 & 2
            r26 = 2097152(0x200000, float:2.938736E-39)
            if (r17 == 0) goto L3fc
            com.android.server.pm.PackageManagerService r10 = r1.mPm     // Catch: java.lang.Throwable -> L3ea
            com.android.server.pm.Settings r10 = r10.mSettings     // Catch: java.lang.Throwable -> L3ea
            java.lang.String r10 = r10.getRenamedPackageLPr(r5)     // Catch: java.lang.Throwable -> L3ea
            r35 = r12
            java.util.List r12 = r7.getOriginalPackages()     // Catch: java.lang.Throwable -> L3da
            boolean r12 = r12.contains(r10)     // Catch: java.lang.Throwable -> L3da
            if (r12 == 0) goto L2c3
            com.android.server.pm.PackageManagerService r12 = r1.mPm     // Catch: java.lang.Throwable -> L2b3
            com.android.server.utils.WatchedArrayMap<java.lang.String, com.android.server.pm.parsing.pkg.AndroidPackage> r12 = r12.mPackages     // Catch: java.lang.Throwable -> L2b3
            boolean r12 = r12.containsKey(r10)     // Catch: java.lang.Throwable -> L2b3
            if (r12 == 0) goto L2b0
            r7.setPackageName(r10)     // Catch: java.lang.Throwable -> L2b3
            java.lang.String r12 = r7.getPackageName()     // Catch: java.lang.Throwable -> L2b3
            r5 = r12
            r14 = 1
            boolean r12 = com.android.server.pm.PackageManagerService.DEBUG_INSTALL     // Catch: java.lang.Throwable -> L29e
            if (r12 == 0) goto L294
            java.lang.String r12 = "PackageManager"
            r17 = r14
            java.lang.StringBuilder r14 = new java.lang.StringBuilder     // Catch: java.lang.Throwable -> L282
            r14.<init>()     // Catch: java.lang.Throwable -> L282
            r36 = r11
            java.lang.String r11 = "Replacing existing renamed package: oldName="
            java.lang.StringBuilder r11 = r14.append(r11)     // Catch: java.lang.Throwable -> L272
            java.lang.StringBuilder r11 = r11.append(r10)     // Catch: java.lang.Throwable -> L272
            java.lang.String r14 = " pkgName="
            java.lang.StringBuilder r11 = r11.append(r14)     // Catch: java.lang.Throwable -> L272
            java.lang.StringBuilder r11 = r11.append(r5)     // Catch: java.lang.Throwable -> L272
            java.lang.String r11 = r11.toString()     // Catch: java.lang.Throwable -> L272
            android.util.Slog.d(r12, r11)     // Catch: java.lang.Throwable -> L272
            goto L298
        L272:
            r0 = move-exception
            r2 = r0
            r12 = r3
            r40 = r4
            r38 = r6
            r37 = r9
            r20 = r13
            r10 = r15
            r14 = r17
            goto L119e
        L282:
            r0 = move-exception
            r36 = r11
            r2 = r0
            r12 = r3
            r40 = r4
            r38 = r6
            r37 = r9
            r20 = r13
            r10 = r15
            r14 = r17
            goto L119e
        L294:
            r36 = r11
            r17 = r14
        L298:
            r14 = r17
            r17 = r10
            goto L302
        L29e:
            r0 = move-exception
            r36 = r11
            r17 = r14
            r2 = r0
            r12 = r3
            r40 = r4
            r38 = r6
            r37 = r9
            r20 = r13
            r10 = r15
            goto L119e
        L2b0:
            r36 = r11
            goto L2c5
        L2b3:
            r0 = move-exception
            r36 = r11
            r2 = r0
            r12 = r3
            r40 = r4
            r38 = r6
            r37 = r9
            r20 = r13
            r10 = r15
            goto L119e
        L2c3:
            r36 = r11
        L2c5:
            com.android.server.pm.PackageManagerService r11 = r1.mPm     // Catch: java.lang.Throwable -> L3cc
            com.android.server.utils.WatchedArrayMap<java.lang.String, com.android.server.pm.parsing.pkg.AndroidPackage> r11 = r11.mPackages     // Catch: java.lang.Throwable -> L3cc
            boolean r11 = r11.containsKey(r5)     // Catch: java.lang.Throwable -> L3cc
            if (r11 == 0) goto L300
            r14 = 1
            boolean r11 = com.android.server.pm.PackageManagerService.DEBUG_INSTALL     // Catch: java.lang.Throwable -> L2f2
            if (r11 == 0) goto L2ef
            java.lang.String r11 = "PackageManager"
            java.lang.StringBuilder r12 = new java.lang.StringBuilder     // Catch: java.lang.Throwable -> L2f2
            r12.<init>()     // Catch: java.lang.Throwable -> L2f2
            r17 = r10
            java.lang.String r10 = "Replace existing package: "
            java.lang.StringBuilder r10 = r12.append(r10)     // Catch: java.lang.Throwable -> L2f2
            java.lang.StringBuilder r10 = r10.append(r5)     // Catch: java.lang.Throwable -> L2f2
            java.lang.String r10 = r10.toString()     // Catch: java.lang.Throwable -> L2f2
            android.util.Slog.d(r11, r10)     // Catch: java.lang.Throwable -> L2f2
            goto L302
        L2ef:
            r17 = r10
            goto L302
        L2f2:
            r0 = move-exception
            r2 = r0
            r12 = r3
            r40 = r4
            r38 = r6
            r37 = r9
            r20 = r13
            r10 = r15
            goto L119e
        L300:
            r17 = r10
        L302:
            if (r14 == 0) goto L3c5
            com.android.server.pm.PackageManagerService r10 = r1.mPm     // Catch: java.lang.Throwable -> L3b5
            com.android.server.utils.WatchedArrayMap<java.lang.String, com.android.server.pm.parsing.pkg.AndroidPackage> r10 = r10.mPackages     // Catch: java.lang.Throwable -> L3b5
            java.lang.Object r10 = r10.get(r5)     // Catch: java.lang.Throwable -> L3b5
            com.android.server.pm.parsing.pkg.AndroidPackage r10 = (com.android.server.pm.parsing.pkg.AndroidPackage) r10     // Catch: java.lang.Throwable -> L3b5
            int r11 = r10.getTargetSdkVersion()     // Catch: java.lang.Throwable -> L3b5
            int r12 = r7.getTargetSdkVersion()     // Catch: java.lang.Throwable -> L3b5
            r18 = r14
            r14 = 22
            if (r11 <= r14) goto L36c
            if (r12 <= r14) goto L321
            r37 = r9
            goto L36e
        L321:
            com.android.server.pm.PrepareFailure r14 = new com.android.server.pm.PrepareFailure     // Catch: java.lang.Throwable -> L35c
            java.lang.StringBuilder r2 = new java.lang.StringBuilder     // Catch: java.lang.Throwable -> L35c
            r2.<init>()     // Catch: java.lang.Throwable -> L35c
            r37 = r9
            java.lang.String r9 = "Package "
            java.lang.StringBuilder r2 = r2.append(r9)     // Catch: java.lang.Throwable -> L3a7
            java.lang.String r9 = r7.getPackageName()     // Catch: java.lang.Throwable -> L3a7
            java.lang.StringBuilder r2 = r2.append(r9)     // Catch: java.lang.Throwable -> L3a7
            java.lang.String r9 = " new target SDK "
            java.lang.StringBuilder r2 = r2.append(r9)     // Catch: java.lang.Throwable -> L3a7
            java.lang.StringBuilder r2 = r2.append(r12)     // Catch: java.lang.Throwable -> L3a7
            java.lang.String r9 = " doesn't support runtime permissions but the old target SDK "
            java.lang.StringBuilder r2 = r2.append(r9)     // Catch: java.lang.Throwable -> L3a7
            java.lang.StringBuilder r2 = r2.append(r11)     // Catch: java.lang.Throwable -> L3a7
            java.lang.String r9 = " does."
            java.lang.StringBuilder r2 = r2.append(r9)     // Catch: java.lang.Throwable -> L3a7
            java.lang.String r2 = r2.toString()     // Catch: java.lang.Throwable -> L3a7
            r9 = -26
            r14.<init>(r9, r2)     // Catch: java.lang.Throwable -> L3a7
            throw r14     // Catch: java.lang.Throwable -> L3a7
        L35c:
            r0 = move-exception
            r37 = r9
            r2 = r0
            r12 = r3
            r40 = r4
            r38 = r6
            r20 = r13
            r10 = r15
            r14 = r18
            goto L119e
        L36c:
            r37 = r9
        L36e:
            com.transsion.hubcore.sru.ITranSruManager r2 = com.transsion.hubcore.sru.ITranSruManager.Instance()     // Catch: java.lang.Throwable -> L3a7
            boolean r2 = r2.isInWhiteList(r5)     // Catch: java.lang.Throwable -> L3a7
            if (r2 != 0) goto L3c9
            boolean r2 = r10.isPersistent()     // Catch: java.lang.Throwable -> L3a7
            if (r2 == 0) goto L3c9
            r2 = r4 & r26
            if (r2 == 0) goto L383
            goto L3c9
        L383:
            com.android.server.pm.PrepareFailure r2 = new com.android.server.pm.PrepareFailure     // Catch: java.lang.Throwable -> L3a7
            java.lang.StringBuilder r9 = new java.lang.StringBuilder     // Catch: java.lang.Throwable -> L3a7
            r9.<init>()     // Catch: java.lang.Throwable -> L3a7
            java.lang.String r14 = "Package "
            java.lang.StringBuilder r9 = r9.append(r14)     // Catch: java.lang.Throwable -> L3a7
            java.lang.String r14 = r10.getPackageName()     // Catch: java.lang.Throwable -> L3a7
            java.lang.StringBuilder r9 = r9.append(r14)     // Catch: java.lang.Throwable -> L3a7
            java.lang.String r14 = " is a persistent app. Persistent apps are not updateable."
            java.lang.StringBuilder r9 = r9.append(r14)     // Catch: java.lang.Throwable -> L3a7
            java.lang.String r9 = r9.toString()     // Catch: java.lang.Throwable -> L3a7
            r14 = -2
            r2.<init>(r14, r9)     // Catch: java.lang.Throwable -> L3a7
            throw r2     // Catch: java.lang.Throwable -> L3a7
        L3a7:
            r0 = move-exception
            r2 = r0
            r12 = r3
            r40 = r4
            r38 = r6
            r20 = r13
            r10 = r15
            r14 = r18
            goto L119e
        L3b5:
            r0 = move-exception
            r37 = r9
            r18 = r14
            r2 = r0
            r12 = r3
            r40 = r4
            r38 = r6
            r20 = r13
            r10 = r15
            goto L119e
        L3c5:
            r37 = r9
            r18 = r14
        L3c9:
            r2 = r18
            goto L403
        L3cc:
            r0 = move-exception
            r37 = r9
            r2 = r0
            r12 = r3
            r40 = r4
            r38 = r6
            r20 = r13
            r10 = r15
            goto L119e
        L3da:
            r0 = move-exception
            r37 = r9
            r36 = r11
            r2 = r0
            r12 = r3
            r40 = r4
            r38 = r6
            r20 = r13
            r10 = r15
            goto L119e
        L3ea:
            r0 = move-exception
            r37 = r9
            r36 = r11
            r35 = r12
            r2 = r0
            r12 = r3
            r40 = r4
            r38 = r6
            r20 = r13
            r10 = r15
            goto L119e
        L3fc:
            r37 = r9
            r36 = r11
            r35 = r12
            r2 = r14
        L403:
            com.android.server.pm.PackageManagerService r9 = r1.mPm     // Catch: java.lang.Throwable -> L118e
            com.android.server.pm.Settings r9 = r9.mSettings     // Catch: java.lang.Throwable -> L118e
            com.android.server.pm.PackageSetting r9 = r9.getPackageLPr(r5)     // Catch: java.lang.Throwable -> L118e
            r10 = r9
            if (r10 != 0) goto L44d
            boolean r11 = r7.isSdkLibrary()     // Catch: java.lang.Throwable -> L440
            if (r11 == 0) goto L44d
            com.android.server.pm.SharedLibrariesImpl r11 = r1.mSharedLibraries     // Catch: java.lang.Throwable -> L440
            java.lang.String r12 = r7.getSdkLibName()     // Catch: java.lang.Throwable -> L440
            com.android.server.utils.WatchedLongSparseArray r11 = r11.getSharedLibraryInfos(r12)     // Catch: java.lang.Throwable -> L440
            if (r11 == 0) goto L43d
            int r12 = r11.size()     // Catch: java.lang.Throwable -> L440
            if (r12 <= 0) goto L43d
            r12 = 0
            java.lang.Object r14 = r11.valueAt(r12)     // Catch: java.lang.Throwable -> L440
            android.content.pm.SharedLibraryInfo r14 = (android.content.pm.SharedLibraryInfo) r14     // Catch: java.lang.Throwable -> L440
            r12 = r14
            com.android.server.pm.PackageManagerService r14 = r1.mPm     // Catch: java.lang.Throwable -> L440
            com.android.server.pm.Settings r14 = r14.mSettings     // Catch: java.lang.Throwable -> L440
            r17 = r10
            java.lang.String r10 = r12.getPackageName()     // Catch: java.lang.Throwable -> L440
            com.android.server.pm.PackageSetting r10 = r14.getPackageLPr(r10)     // Catch: java.lang.Throwable -> L440
            goto L451
        L43d:
            r17 = r10
            goto L44f
        L440:
            r0 = move-exception
            r14 = r2
            r12 = r3
            r40 = r4
            r38 = r6
            r20 = r13
            r10 = r15
            r2 = r0
            goto L119e
        L44d:
            r17 = r10
        L44f:
            r10 = r17
        L451:
            boolean r11 = r7.isStaticSharedLibrary()     // Catch: java.lang.Throwable -> L118e
            if (r11 == 0) goto L46c
            com.android.server.pm.SharedLibrariesImpl r11 = r1.mSharedLibraries     // Catch: java.lang.Throwable -> L440
            android.content.pm.SharedLibraryInfo r11 = r11.getLatestStaticSharedLibraVersionLPr(r7)     // Catch: java.lang.Throwable -> L440
            if (r11 == 0) goto L46c
            com.android.server.pm.PackageManagerService r12 = r1.mPm     // Catch: java.lang.Throwable -> L440
            com.android.server.pm.Settings r12 = r12.mSettings     // Catch: java.lang.Throwable -> L440
            java.lang.String r14 = r11.getPackageName()     // Catch: java.lang.Throwable -> L440
            com.android.server.pm.PackageSetting r12 = r12.getPackageLPr(r14)     // Catch: java.lang.Throwable -> L440
            r10 = r12
        L46c:
            if (r10 == 0) goto L572
            boolean r12 = com.android.server.pm.PackageManagerService.DEBUG_INSTALL     // Catch: java.lang.Throwable -> L565
            if (r12 == 0) goto L48a
            java.lang.String r12 = "PackageManager"
            java.lang.StringBuilder r14 = new java.lang.StringBuilder     // Catch: java.lang.Throwable -> L440
            r14.<init>()     // Catch: java.lang.Throwable -> L440
            java.lang.String r11 = "Existing package for signature checking: "
            java.lang.StringBuilder r11 = r14.append(r11)     // Catch: java.lang.Throwable -> L440
            java.lang.StringBuilder r11 = r11.append(r10)     // Catch: java.lang.Throwable -> L440
            java.lang.String r11 = r11.toString()     // Catch: java.lang.Throwable -> L440
            android.util.Slog.d(r12, r11)     // Catch: java.lang.Throwable -> L440
        L48a:
            com.android.server.pm.PackageManagerService r11 = r1.mPm     // Catch: java.lang.Throwable -> L565
            com.android.server.pm.Settings r11 = r11.mSettings     // Catch: java.lang.Throwable -> L565
            com.android.server.pm.KeySetManagerService r11 = r11.getKeySetManagerService()     // Catch: java.lang.Throwable -> L565
            com.android.server.pm.PackageManagerService r12 = r1.mPm     // Catch: java.lang.Throwable -> L565
            com.android.server.pm.Settings r12 = r12.mSettings     // Catch: java.lang.Throwable -> L565
            com.android.server.pm.SharedUserSetting r12 = r12.getSharedUserSettingLPr(r10)     // Catch: java.lang.Throwable -> L565
            boolean r14 = r11.shouldCheckUpgradeKeySetLocked(r10, r12, r15)     // Catch: java.lang.Throwable -> L565
            if (r14 == 0) goto L4fe
            boolean r14 = r11.checkUpgradeKeySetLocked(r10, r7)     // Catch: java.lang.Throwable -> L4f1
            if (r14 == 0) goto L4ad
            r38 = r4
            r30 = r10
            r10 = r15
            goto L577
        L4ad:
            com.android.server.pm.PrepareFailure r14 = new com.android.server.pm.PrepareFailure     // Catch: java.lang.Throwable -> L4f1
            r20 = r15
            java.lang.StringBuilder r15 = new java.lang.StringBuilder     // Catch: java.lang.Throwable -> L4e3
            r15.<init>()     // Catch: java.lang.Throwable -> L4e3
            r38 = r4
            java.lang.String r4 = "Package "
            java.lang.StringBuilder r4 = r15.append(r4)     // Catch: java.lang.Throwable -> L4d5
            java.lang.String r15 = r7.getPackageName()     // Catch: java.lang.Throwable -> L4d5
            java.lang.StringBuilder r4 = r4.append(r15)     // Catch: java.lang.Throwable -> L4d5
            java.lang.String r15 = " upgrade keys do not match the previously installed version"
            java.lang.StringBuilder r4 = r4.append(r15)     // Catch: java.lang.Throwable -> L4d5
            java.lang.String r4 = r4.toString()     // Catch: java.lang.Throwable -> L4d5
            r15 = -7
            r14.<init>(r15, r4)     // Catch: java.lang.Throwable -> L4d5
            throw r14     // Catch: java.lang.Throwable -> L4d5
        L4d5:
            r0 = move-exception
            r14 = r2
            r12 = r3
            r10 = r20
            r40 = r38
            r2 = r0
            r38 = r6
            r20 = r13
            goto L119e
        L4e3:
            r0 = move-exception
            r14 = r2
            r12 = r3
            r40 = r4
            r38 = r6
            r10 = r20
            r2 = r0
            r20 = r13
            goto L119e
        L4f1:
            r0 = move-exception
            r14 = r2
            r12 = r3
            r40 = r4
            r38 = r6
            r20 = r13
            r10 = r15
            r2 = r0
            goto L119e
        L4fe:
            r38 = r4
            r20 = r15
            com.android.server.pm.PackageManagerService r4 = r1.mPm     // Catch: java.lang.Throwable -> L543 com.android.server.pm.PackageManagerException -> L551
            com.android.server.pm.Settings$VersionInfo r4 = r4.getSettingsVersionForPackage(r7)     // Catch: java.lang.Throwable -> L543 com.android.server.pm.PackageManagerException -> L551
            boolean r18 = com.android.server.pm.ReconcilePackageUtils.isCompatSignatureUpdateNeeded(r4)     // Catch: java.lang.Throwable -> L543 com.android.server.pm.PackageManagerException -> L551
            com.android.server.pm.PackageManagerService r4 = r1.mPm     // Catch: java.lang.Throwable -> L543 com.android.server.pm.PackageManagerException -> L551
            com.android.server.pm.Settings$VersionInfo r4 = r4.getSettingsVersionForPackage(r7)     // Catch: java.lang.Throwable -> L543 com.android.server.pm.PackageManagerException -> L551
            boolean r19 = com.android.server.pm.ReconcilePackageUtils.isRecoverSignatureUpdateNeeded(r4)     // Catch: java.lang.Throwable -> L543 com.android.server.pm.PackageManagerException -> L551
            r4 = 0
            android.content.pm.SigningDetails r17 = r7.getSigningDetails()     // Catch: java.lang.Throwable -> L543 com.android.server.pm.PackageManagerException -> L551
            r15 = 8
            r14 = r10
            r30 = r10
            r10 = r20
            r15 = r12
            r16 = r4
            r20 = r13
            boolean r4 = com.android.server.pm.PackageManagerServiceUtils.verifySignatures(r14, r15, r16, r17, r18, r19, r20)     // Catch: com.android.server.pm.PackageManagerException -> L540 java.lang.Throwable -> L5b7
            if (r4 == 0) goto L53f
            com.android.server.pm.PackageManagerService r14 = r1.mPm     // Catch: com.android.server.pm.PackageManagerException -> L540 java.lang.Throwable -> L5b7
            com.android.server.pm.PackageManagerTracedLock r14 = r14.mLock     // Catch: com.android.server.pm.PackageManagerException -> L540 java.lang.Throwable -> L5b7
            monitor-enter(r14)     // Catch: com.android.server.pm.PackageManagerException -> L540 java.lang.Throwable -> L5b7
            java.lang.String r15 = r7.getPackageName()     // Catch: java.lang.Throwable -> L53b
            r11.removeAppKeySetDataLPw(r15)     // Catch: java.lang.Throwable -> L53b
            monitor-exit(r14)     // Catch: java.lang.Throwable -> L53b
            goto L53f
        L53b:
            r0 = move-exception
            r15 = r0
            monitor-exit(r14)     // Catch: java.lang.Throwable -> L53b
            throw r15     // Catch: com.android.server.pm.PackageManagerException -> L540 java.lang.Throwable -> L5b7
        L53f:
            goto L577
        L540:
            r0 = move-exception
            r4 = r0
            goto L557
        L543:
            r0 = move-exception
            r10 = r20
            r14 = r2
            r12 = r3
            r20 = r13
            r40 = r38
            r2 = r0
            r38 = r6
            goto L119e
        L551:
            r0 = move-exception
            r30 = r10
            r10 = r20
            r4 = r0
        L557:
            com.android.server.pm.PrepareFailure r14 = new com.android.server.pm.PrepareFailure     // Catch: java.lang.Throwable -> L5b7
            int r15 = r4.error     // Catch: java.lang.Throwable -> L5b7
            r16 = r11
            java.lang.String r11 = r4.getMessage()     // Catch: java.lang.Throwable -> L5b7
            r14.<init>(r15, r11)     // Catch: java.lang.Throwable -> L5b7
            throw r14     // Catch: java.lang.Throwable -> L5b7
        L565:
            r0 = move-exception
            r10 = r15
            r14 = r2
            r12 = r3
            r40 = r4
            r38 = r6
            r20 = r13
            r2 = r0
            goto L119e
        L572:
            r38 = r4
            r30 = r10
            r10 = r15
        L577:
            if (r9 == 0) goto L5c3
            boolean r4 = com.android.server.pm.PackageManagerService.DEBUG_INSTALL     // Catch: java.lang.Throwable -> L5b7
            if (r4 == 0) goto L595
            java.lang.String r4 = "PackageManager"
            java.lang.StringBuilder r11 = new java.lang.StringBuilder     // Catch: java.lang.Throwable -> L5b7
            r11.<init>()     // Catch: java.lang.Throwable -> L5b7
            java.lang.String r12 = "Existing package: "
            java.lang.StringBuilder r11 = r11.append(r12)     // Catch: java.lang.Throwable -> L5b7
            java.lang.StringBuilder r11 = r11.append(r9)     // Catch: java.lang.Throwable -> L5b7
            java.lang.String r11 = r11.toString()     // Catch: java.lang.Throwable -> L5b7
            android.util.Slog.d(r4, r11)     // Catch: java.lang.Throwable -> L5b7
        L595:
            com.android.server.pm.parsing.pkg.AndroidPackage r4 = r9.getPkg()     // Catch: java.lang.Throwable -> L5b7
            if (r4 == 0) goto L5a5
            com.android.server.pm.parsing.pkg.AndroidPackage r4 = r9.getPkg()     // Catch: java.lang.Throwable -> L5b7
            boolean r4 = r4.isSystem()     // Catch: java.lang.Throwable -> L5b7
            r24 = r4
        L5a5:
            com.android.server.pm.PackageManagerService r4 = r1.mPm     // Catch: java.lang.Throwable -> L5b7
            com.android.server.pm.UserManagerService r4 = r4.mUserManager     // Catch: java.lang.Throwable -> L5b7
            int[] r4 = r4.getUserIds()     // Catch: java.lang.Throwable -> L5b7
            r11 = 1
            int[] r4 = r9.queryInstalledUsers(r4, r11)     // Catch: java.lang.Throwable -> L5b7
            r3.mOrigUsers = r4     // Catch: java.lang.Throwable -> L5b7
            r4 = r24
            goto L5c5
        L5b7:
            r0 = move-exception
            r14 = r2
            r12 = r3
            r20 = r13
            r40 = r38
            r2 = r0
            r38 = r6
            goto L119e
        L5c3:
            r4 = r24
        L5c5:
            java.util.List r11 = r7.getPermissionGroups()     // Catch: java.lang.Throwable -> L117a
            int r11 = com.android.internal.util.ArrayUtils.size(r11)     // Catch: java.lang.Throwable -> L117a
            r12 = 0
        L5ce:
            if (r12 >= r11) goto L6a4
        L5d1:
            java.util.List r15 = r7.getPermissionGroups()     // Catch: java.lang.Throwable -> L693
            java.lang.Object r15 = r15.get(r12)     // Catch: java.lang.Throwable -> L693
            com.android.server.pm.pkg.component.ParsedPermissionGroup r15 = (com.android.server.pm.pkg.component.ParsedPermissionGroup) r15     // Catch: java.lang.Throwable -> L693
            com.android.server.pm.PackageManagerService r14 = r1.mPm     // Catch: java.lang.Throwable -> L693
            r19 = r9
            java.lang.String r9 = r15.getName()     // Catch: java.lang.Throwable -> L693
            r20 = r13
            r13 = 0
            android.content.pm.PermissionGroupInfo r9 = r14.getPermissionGroupInfo(r9, r13)     // Catch: java.lang.Throwable -> L684
            if (r9 == 0) goto L676
            boolean r13 = cannotInstallWithBadPermissionGroups(r7)     // Catch: java.lang.Throwable -> L684
            if (r13 == 0) goto L676
            java.lang.String r13 = r9.packageName     // Catch: java.lang.Throwable -> L684
            if (r2 != 0) goto L611
            java.lang.String r14 = r7.getPackageName()     // Catch: java.lang.Throwable -> L605
            boolean r14 = r14.equals(r13)     // Catch: java.lang.Throwable -> L605
            if (r14 != 0) goto L601
            goto L611
        L601:
            r31 = r2
            goto L67a
        L605:
            r0 = move-exception
            r14 = r2
            r12 = r3
            r24 = r4
            r40 = r38
            r2 = r0
            r38 = r6
            goto L119e
        L611:
            boolean r14 = r1.doesSignatureMatchForPermissions(r13, r7, r10)     // Catch: java.lang.Throwable -> L684
            if (r14 == 0) goto L61a
            r31 = r2
            goto L67a
        L61a:
            r14 = 3
            java.lang.Object[] r14 = new java.lang.Object[r14]     // Catch: java.lang.Throwable -> L684
            java.lang.String r18 = "146211400"
            r21 = 0
            r14[r21] = r18     // Catch: java.lang.Throwable -> L684
            r17 = -1
            java.lang.Integer r17 = java.lang.Integer.valueOf(r17)     // Catch: java.lang.Throwable -> L684
            r18 = 1
            r14[r18] = r17     // Catch: java.lang.Throwable -> L684
            java.lang.String r17 = r7.getPackageName()     // Catch: java.lang.Throwable -> L684
            r18 = 2
            r14[r18] = r17     // Catch: java.lang.Throwable -> L684
            r17 = r9
            r9 = 1397638484(0x534e4554, float:8.859264E11)
            android.util.EventLog.writeEvent(r9, r14)     // Catch: java.lang.Throwable -> L684
            com.android.server.pm.PrepareFailure r9 = new com.android.server.pm.PrepareFailure     // Catch: java.lang.Throwable -> L684
            java.lang.StringBuilder r14 = new java.lang.StringBuilder     // Catch: java.lang.Throwable -> L684
            r14.<init>()     // Catch: java.lang.Throwable -> L684
            r31 = r2
            java.lang.String r2 = "Package "
            java.lang.StringBuilder r2 = r14.append(r2)     // Catch: java.lang.Throwable -> L713
            java.lang.String r14 = r7.getPackageName()     // Catch: java.lang.Throwable -> L713
            java.lang.StringBuilder r2 = r2.append(r14)     // Catch: java.lang.Throwable -> L713
            java.lang.String r14 = " attempting to redeclare permission group "
            java.lang.StringBuilder r2 = r2.append(r14)     // Catch: java.lang.Throwable -> L713
            java.lang.String r14 = r15.getName()     // Catch: java.lang.Throwable -> L713
            java.lang.StringBuilder r2 = r2.append(r14)     // Catch: java.lang.Throwable -> L713
            java.lang.String r14 = " already owned by "
            java.lang.StringBuilder r2 = r2.append(r14)     // Catch: java.lang.Throwable -> L713
            java.lang.StringBuilder r2 = r2.append(r13)     // Catch: java.lang.Throwable -> L713
            java.lang.String r2 = r2.toString()     // Catch: java.lang.Throwable -> L713
            r14 = -126(0xffffffffffffff82, float:NaN)
            r9.<init>(r14, r2)     // Catch: java.lang.Throwable -> L713
            throw r9     // Catch: java.lang.Throwable -> L713
        L676:
            r31 = r2
            r17 = r9
        L67a:
            int r12 = r12 + 1
            r9 = r19
            r13 = r20
            r2 = r31
            goto L5ce
        L684:
            r0 = move-exception
            r31 = r2
            r2 = r0
            r12 = r3
            r24 = r4
            r14 = r31
            r40 = r38
            r38 = r6
            goto L119e
        L693:
            r0 = move-exception
            r31 = r2
            r20 = r13
            r2 = r0
            r12 = r3
            r24 = r4
            r14 = r31
            r40 = r38
            r38 = r6
            goto L119e
        L6a4:
            r31 = r2
            r19 = r9
            r20 = r13
            java.util.List r2 = r7.getPermissions()     // Catch: java.lang.Throwable -> L116a
            int r2 = com.android.internal.util.ArrayUtils.size(r2)     // Catch: java.lang.Throwable -> L116a
            int r9 = r2 + (-1)
        L6b4:
            if (r9 < 0) goto L951
            java.util.List r12 = r7.getPermissions()     // Catch: java.lang.Throwable -> L943
            java.lang.Object r12 = r12.get(r9)     // Catch: java.lang.Throwable -> L943
            com.android.server.pm.pkg.component.ParsedPermission r12 = (com.android.server.pm.pkg.component.ParsedPermission) r12     // Catch: java.lang.Throwable -> L943
            com.android.server.pm.PackageManagerService r13 = r1.mPm     // Catch: java.lang.Throwable -> L943
            com.android.server.pm.permission.PermissionManagerServiceInternal r13 = r13.mPermissionManager     // Catch: java.lang.Throwable -> L943
            java.lang.String r14 = r12.getName()     // Catch: java.lang.Throwable -> L943
            com.android.server.pm.permission.Permission r13 = r13.getPermissionTEMP(r14)     // Catch: java.lang.Throwable -> L943
            int r14 = r12.getProtectionLevel()     // Catch: java.lang.Throwable -> L943
            r14 = r14 & 4096(0x1000, float:5.74E-42)
            if (r14 == 0) goto L720
            if (r4 != 0) goto L720
            java.lang.String r14 = "PackageManager"
            java.lang.StringBuilder r15 = new java.lang.StringBuilder     // Catch: java.lang.Throwable -> L713
            r15.<init>()     // Catch: java.lang.Throwable -> L713
            r24 = r2
            java.lang.String r2 = "Non-System package "
            java.lang.StringBuilder r2 = r15.append(r2)     // Catch: java.lang.Throwable -> L713
            java.lang.String r15 = r7.getPackageName()     // Catch: java.lang.Throwable -> L713
            java.lang.StringBuilder r2 = r2.append(r15)     // Catch: java.lang.Throwable -> L713
            java.lang.String r15 = " attempting to delcare ephemeral permission "
            java.lang.StringBuilder r2 = r2.append(r15)     // Catch: java.lang.Throwable -> L713
            java.lang.String r15 = r12.getName()     // Catch: java.lang.Throwable -> L713
            java.lang.StringBuilder r2 = r2.append(r15)     // Catch: java.lang.Throwable -> L713
            java.lang.String r15 = "; Removing ephemeral."
            java.lang.StringBuilder r2 = r2.append(r15)     // Catch: java.lang.Throwable -> L713
            java.lang.String r2 = r2.toString()     // Catch: java.lang.Throwable -> L713
            android.util.Slog.w(r14, r2)     // Catch: java.lang.Throwable -> L713
            int r2 = r12.getProtectionLevel()     // Catch: java.lang.Throwable -> L713
            r2 = r2 & (-4097(0xffffffffffffefff, float:NaN))
            com.android.server.pm.pkg.component.ComponentMutateUtils.setProtectionLevel(r12, r2)     // Catch: java.lang.Throwable -> L713
            goto L722
        L713:
            r0 = move-exception
            r2 = r0
            r12 = r3
            r24 = r4
            r14 = r31
            r40 = r38
            r38 = r6
            goto L119e
        L720:
            r24 = r2
        L722:
            if (r13 == 0) goto L7ff
            java.lang.String r2 = r13.getPackageName()     // Catch: java.lang.Throwable -> L943
            boolean r14 = r1.doesSignatureMatchForPermissions(r2, r7, r10)     // Catch: java.lang.Throwable -> L943
            if (r14 != 0) goto L7aa
            java.lang.String r14 = "android"
            boolean r14 = r2.equals(r14)     // Catch: java.lang.Throwable -> L943
            if (r14 == 0) goto L76b
            java.lang.String r14 = "PackageManager"
            java.lang.StringBuilder r15 = new java.lang.StringBuilder     // Catch: java.lang.Throwable -> L943
            r15.<init>()     // Catch: java.lang.Throwable -> L943
            java.lang.String r3 = "Package "
            java.lang.StringBuilder r3 = r15.append(r3)     // Catch: java.lang.Throwable -> L943
            java.lang.String r15 = r7.getPackageName()     // Catch: java.lang.Throwable -> L943
            java.lang.StringBuilder r3 = r3.append(r15)     // Catch: java.lang.Throwable -> L943
            java.lang.String r15 = " attempting to redeclare system permission "
            java.lang.StringBuilder r3 = r3.append(r15)     // Catch: java.lang.Throwable -> L943
            java.lang.String r15 = r12.getName()     // Catch: java.lang.Throwable -> L943
            java.lang.StringBuilder r3 = r3.append(r15)     // Catch: java.lang.Throwable -> L943
            java.lang.String r15 = "; ignoring new declaration"
            java.lang.StringBuilder r3 = r3.append(r15)     // Catch: java.lang.Throwable -> L943
            java.lang.String r3 = r3.toString()     // Catch: java.lang.Throwable -> L943
            android.util.Slog.w(r14, r3)     // Catch: java.lang.Throwable -> L943
            r7.removePermission(r9)     // Catch: java.lang.Throwable -> L943
            goto L7ff
        L76b:
            com.android.server.pm.PrepareFailure r3 = new com.android.server.pm.PrepareFailure     // Catch: java.lang.Throwable -> L943
            java.lang.StringBuilder r15 = new java.lang.StringBuilder     // Catch: java.lang.Throwable -> L943
            r15.<init>()     // Catch: java.lang.Throwable -> L943
            java.lang.String r14 = "Package "
            java.lang.StringBuilder r14 = r15.append(r14)     // Catch: java.lang.Throwable -> L943
            java.lang.String r15 = r7.getPackageName()     // Catch: java.lang.Throwable -> L943
            java.lang.StringBuilder r14 = r14.append(r15)     // Catch: java.lang.Throwable -> L943
            java.lang.String r15 = " attempting to redeclare permission "
            java.lang.StringBuilder r14 = r14.append(r15)     // Catch: java.lang.Throwable -> L943
            java.lang.String r15 = r12.getName()     // Catch: java.lang.Throwable -> L943
            java.lang.StringBuilder r14 = r14.append(r15)     // Catch: java.lang.Throwable -> L943
            java.lang.String r15 = " already owned by "
            java.lang.StringBuilder r14 = r14.append(r15)     // Catch: java.lang.Throwable -> L943
            java.lang.StringBuilder r14 = r14.append(r2)     // Catch: java.lang.Throwable -> L943
            java.lang.String r14 = r14.toString()     // Catch: java.lang.Throwable -> L943
            r15 = -112(0xffffffffffffff90, float:NaN)
            r3.<init>(r15, r14)     // Catch: java.lang.Throwable -> L943
            java.lang.String r14 = r12.getName()     // Catch: java.lang.Throwable -> L943
            com.android.server.pm.PrepareFailure r3 = r3.conflictsWithExistingPermission(r14, r2)     // Catch: java.lang.Throwable -> L943
            throw r3     // Catch: java.lang.Throwable -> L943
        L7aa:
            java.lang.String r3 = "android"
            java.lang.String r14 = r7.getPackageName()     // Catch: java.lang.Throwable -> L943
            boolean r3 = r3.equals(r14)     // Catch: java.lang.Throwable -> L943
            if (r3 != 0) goto L7ff
            int r3 = r12.getProtectionLevel()     // Catch: java.lang.Throwable -> L943
            r3 = r3 & 15
            r14 = 1
            if (r3 != r14) goto L7ff
            if (r13 == 0) goto L7ff
            boolean r3 = r13.isRuntime()     // Catch: java.lang.Throwable -> L943
            if (r3 != 0) goto L7ff
            java.lang.String r3 = "PackageManager"
            java.lang.StringBuilder r14 = new java.lang.StringBuilder     // Catch: java.lang.Throwable -> L943
            r14.<init>()     // Catch: java.lang.Throwable -> L943
            java.lang.String r15 = "Package "
            java.lang.StringBuilder r14 = r14.append(r15)     // Catch: java.lang.Throwable -> L943
            java.lang.String r15 = r7.getPackageName()     // Catch: java.lang.Throwable -> L943
            java.lang.StringBuilder r14 = r14.append(r15)     // Catch: java.lang.Throwable -> L943
            java.lang.String r15 = " trying to change a non-runtime permission "
            java.lang.StringBuilder r14 = r14.append(r15)     // Catch: java.lang.Throwable -> L943
            java.lang.String r15 = r12.getName()     // Catch: java.lang.Throwable -> L943
            java.lang.StringBuilder r14 = r14.append(r15)     // Catch: java.lang.Throwable -> L943
            java.lang.String r15 = " to runtime; keeping old protection level"
            java.lang.StringBuilder r14 = r14.append(r15)     // Catch: java.lang.Throwable -> L943
            java.lang.String r14 = r14.toString()     // Catch: java.lang.Throwable -> L943
            android.util.Slog.w(r3, r14)     // Catch: java.lang.Throwable -> L943
            int r3 = r13.getProtectionLevel()     // Catch: java.lang.Throwable -> L943
            com.android.server.pm.pkg.component.ComponentMutateUtils.setProtectionLevel(r12, r3)     // Catch: java.lang.Throwable -> L943
        L7ff:
            java.lang.String r2 = r12.getGroup()     // Catch: java.lang.Throwable -> L943
            if (r2 == 0) goto L936
            boolean r2 = cannotInstallWithBadPermissionGroups(r7)     // Catch: java.lang.Throwable -> L943
            if (r2 == 0) goto L930
            r2 = 0
            r3 = 0
        L80d:
            if (r3 >= r11) goto L82c
            java.util.List r14 = r7.getPermissionGroups()     // Catch: java.lang.Throwable -> L943
            java.lang.Object r14 = r14.get(r3)     // Catch: java.lang.Throwable -> L943
            com.android.server.pm.pkg.component.ParsedPermissionGroup r14 = (com.android.server.pm.pkg.component.ParsedPermissionGroup) r14     // Catch: java.lang.Throwable -> L943
            java.lang.String r14 = r14.getName()     // Catch: java.lang.Throwable -> L943
            java.lang.String r15 = r12.getGroup()     // Catch: java.lang.Throwable -> L943
            boolean r14 = r14.equals(r15)     // Catch: java.lang.Throwable -> L943
            if (r14 == 0) goto L829
            r2 = 1
            goto L82c
        L829:
            int r3 = r3 + 1
            goto L80d
        L82c:
            if (r2 != 0) goto L928
            com.android.server.pm.PackageManagerService r3 = r1.mPm     // Catch: java.lang.Throwable -> L943
            java.lang.String r14 = r12.getGroup()     // Catch: java.lang.Throwable -> L943
            r15 = 0
            android.content.pm.PermissionGroupInfo r3 = r3.getPermissionGroupInfo(r14, r15)     // Catch: java.lang.Throwable -> L943
            if (r3 == 0) goto L8cc
            java.lang.String r15 = r3.packageName     // Catch: java.lang.Throwable -> L943
            java.lang.String r14 = "android"
            boolean r14 = r14.equals(r15)     // Catch: java.lang.Throwable -> L943
            if (r14 != 0) goto L8c2
            boolean r14 = r1.doesSignatureMatchForPermissions(r15, r7, r10)     // Catch: java.lang.Throwable -> L943
            if (r14 == 0) goto L852
            r2 = 3
            r3 = 1397638484(0x534e4554, float:8.859264E11)
            r14 = 2
            goto L93b
        L852:
            r14 = 3
            java.lang.Object[] r14 = new java.lang.Object[r14]     // Catch: java.lang.Throwable -> L943
            java.lang.String r18 = "146211400"
            r21 = 0
            r14[r21] = r18     // Catch: java.lang.Throwable -> L943
            r17 = -1
            java.lang.Integer r17 = java.lang.Integer.valueOf(r17)     // Catch: java.lang.Throwable -> L943
            r18 = 1
            r14[r18] = r17     // Catch: java.lang.Throwable -> L943
            java.lang.String r17 = r7.getPackageName()     // Catch: java.lang.Throwable -> L943
            r18 = 2
            r14[r18] = r17     // Catch: java.lang.Throwable -> L943
            r40 = r2
            r2 = 1397638484(0x534e4554, float:8.859264E11)
            android.util.EventLog.writeEvent(r2, r14)     // Catch: java.lang.Throwable -> L943
            com.android.server.pm.PrepareFailure r2 = new com.android.server.pm.PrepareFailure     // Catch: java.lang.Throwable -> L943
            java.lang.StringBuilder r14 = new java.lang.StringBuilder     // Catch: java.lang.Throwable -> L943
            r14.<init>()     // Catch: java.lang.Throwable -> L943
            r41 = r3
            java.lang.String r3 = "Package "
            java.lang.StringBuilder r3 = r14.append(r3)     // Catch: java.lang.Throwable -> L943
            java.lang.String r14 = r7.getPackageName()     // Catch: java.lang.Throwable -> L943
            java.lang.StringBuilder r3 = r3.append(r14)     // Catch: java.lang.Throwable -> L943
            java.lang.String r14 = " attempting to declare permission "
            java.lang.StringBuilder r3 = r3.append(r14)     // Catch: java.lang.Throwable -> L943
            java.lang.String r14 = r12.getName()     // Catch: java.lang.Throwable -> L943
            java.lang.StringBuilder r3 = r3.append(r14)     // Catch: java.lang.Throwable -> L943
            java.lang.String r14 = " in group "
            java.lang.StringBuilder r3 = r3.append(r14)     // Catch: java.lang.Throwable -> L943
            java.lang.String r14 = r12.getGroup()     // Catch: java.lang.Throwable -> L943
            java.lang.StringBuilder r3 = r3.append(r14)     // Catch: java.lang.Throwable -> L943
            java.lang.String r14 = " owned by package "
            java.lang.StringBuilder r3 = r3.append(r14)     // Catch: java.lang.Throwable -> L943
            java.lang.StringBuilder r3 = r3.append(r15)     // Catch: java.lang.Throwable -> L943
            java.lang.String r14 = " with incompatible certificate"
            java.lang.StringBuilder r3 = r3.append(r14)     // Catch: java.lang.Throwable -> L943
            java.lang.String r3 = r3.toString()     // Catch: java.lang.Throwable -> L943
            r14 = -127(0xffffffffffffff81, float:NaN)
            r2.<init>(r14, r3)     // Catch: java.lang.Throwable -> L943
            throw r2     // Catch: java.lang.Throwable -> L943
        L8c2:
            r40 = r2
            r41 = r3
            r2 = 3
            r3 = 1397638484(0x534e4554, float:8.859264E11)
            r14 = 2
            goto L93b
        L8cc:
            r40 = r2
            r41 = r3
            r2 = 3
            java.lang.Object[] r2 = new java.lang.Object[r2]     // Catch: java.lang.Throwable -> L943
            java.lang.String r3 = "146211400"
            r14 = 0
            r2[r14] = r3     // Catch: java.lang.Throwable -> L943
            r3 = -1
            java.lang.Integer r3 = java.lang.Integer.valueOf(r3)     // Catch: java.lang.Throwable -> L943
            r14 = 1
            r2[r14] = r3     // Catch: java.lang.Throwable -> L943
            java.lang.String r3 = r7.getPackageName()     // Catch: java.lang.Throwable -> L943
            r14 = 2
            r2[r14] = r3     // Catch: java.lang.Throwable -> L943
            r3 = 1397638484(0x534e4554, float:8.859264E11)
            android.util.EventLog.writeEvent(r3, r2)     // Catch: java.lang.Throwable -> L943
            com.android.server.pm.PrepareFailure r2 = new com.android.server.pm.PrepareFailure     // Catch: java.lang.Throwable -> L943
            java.lang.StringBuilder r3 = new java.lang.StringBuilder     // Catch: java.lang.Throwable -> L943
            r3.<init>()     // Catch: java.lang.Throwable -> L943
            java.lang.String r14 = "Package "
            java.lang.StringBuilder r3 = r3.append(r14)     // Catch: java.lang.Throwable -> L943
            java.lang.String r14 = r7.getPackageName()     // Catch: java.lang.Throwable -> L943
            java.lang.StringBuilder r3 = r3.append(r14)     // Catch: java.lang.Throwable -> L943
            java.lang.String r14 = " attempting to declare permission "
            java.lang.StringBuilder r3 = r3.append(r14)     // Catch: java.lang.Throwable -> L943
            java.lang.String r14 = r12.getName()     // Catch: java.lang.Throwable -> L943
            java.lang.StringBuilder r3 = r3.append(r14)     // Catch: java.lang.Throwable -> L943
            java.lang.String r14 = " in non-existing group "
            java.lang.StringBuilder r3 = r3.append(r14)     // Catch: java.lang.Throwable -> L943
            java.lang.String r14 = r12.getGroup()     // Catch: java.lang.Throwable -> L943
            java.lang.StringBuilder r3 = r3.append(r14)     // Catch: java.lang.Throwable -> L943
            java.lang.String r3 = r3.toString()     // Catch: java.lang.Throwable -> L943
            r14 = -127(0xffffffffffffff81, float:NaN)
            r2.<init>(r14, r3)     // Catch: java.lang.Throwable -> L943
            throw r2     // Catch: java.lang.Throwable -> L943
        L928:
            r40 = r2
            r2 = 3
            r3 = 1397638484(0x534e4554, float:8.859264E11)
            r14 = 2
            goto L93b
        L930:
            r2 = 3
            r3 = 1397638484(0x534e4554, float:8.859264E11)
            r14 = 2
            goto L93b
        L936:
            r2 = 3
            r3 = 1397638484(0x534e4554, float:8.859264E11)
            r14 = 2
        L93b:
            int r9 = r9 + (-1)
            r3 = r45
            r2 = r24
            goto L6b4
        L943:
            r0 = move-exception
            r12 = r45
            r2 = r0
            r24 = r4
            r14 = r31
            r40 = r38
            r38 = r6
            goto L119e
        L951:
            r24 = r2
            monitor-exit(r8)     // Catch: java.lang.Throwable -> L1166
            if (r4 == 0) goto L96f
            if (r6 != 0) goto L965
            if (r37 != 0) goto L95b
            goto L96f
        L95b:
            com.android.server.pm.PrepareFailure r2 = new com.android.server.pm.PrepareFailure
            java.lang.String r3 = "Cannot update a system app with an instant app"
            r8 = -116(0xffffffffffffff8c, float:NaN)
            r2.<init>(r8, r3)
            throw r2
        L965:
            com.android.server.pm.PrepareFailure r2 = new com.android.server.pm.PrepareFailure
            java.lang.String r3 = "Cannot install updates to system apps on sdcard"
            r8 = -19
            r2.<init>(r8, r3)
            throw r2
        L96f:
            r2 = r44
            com.android.server.pm.MoveInfo r3 = r2.mMoveInfo
            r8 = -110(0xffffffffffffff92, float:NaN)
            if (r3 == 0) goto L9c2
            r3 = 1
            r9 = r10 | 1
            r3 = r9 | 256(0x100, float:3.59E-43)
            com.android.server.pm.PackageManagerService r9 = r1.mPm
            com.android.server.pm.PackageManagerTracedLock r9 = r9.mLock
            monitor-enter(r9)
            com.android.server.pm.PackageManagerService r10 = r1.mPm     // Catch: java.lang.Throwable -> L9ba
            com.android.server.pm.Settings r10 = r10.mSettings     // Catch: java.lang.Throwable -> L9ba
            com.android.server.pm.PackageSetting r10 = r10.getPackageLPr(r5)     // Catch: java.lang.Throwable -> L9ba
            if (r10 != 0) goto L9a4
            java.lang.StringBuilder r11 = new java.lang.StringBuilder     // Catch: java.lang.Throwable -> L9ba
            r11.<init>()     // Catch: java.lang.Throwable -> L9ba
            java.lang.String r12 = "Missing settings for moved package "
            java.lang.StringBuilder r11 = r11.append(r12)     // Catch: java.lang.Throwable -> L9ba
            java.lang.StringBuilder r11 = r11.append(r5)     // Catch: java.lang.Throwable -> L9ba
            java.lang.String r11 = r11.toString()     // Catch: java.lang.Throwable -> L9ba
            r12 = r45
            r12.setError(r8, r11)     // Catch: java.lang.Throwable -> L9c0
            goto L9a6
        L9a4:
            r12 = r45
        L9a6:
            if (r10 == 0) goto L9b7
            java.lang.String r11 = r10.getPrimaryCpuAbi()     // Catch: java.lang.Throwable -> L9c0
            com.android.server.pm.parsing.pkg.ParsedPackage r11 = r7.setPrimaryCpuAbi(r11)     // Catch: java.lang.Throwable -> L9c0
            java.lang.String r13 = r10.getSecondaryCpuAbi()     // Catch: java.lang.Throwable -> L9c0
            r11.setSecondaryCpuAbi(r13)     // Catch: java.lang.Throwable -> L9c0
        L9b7:
            monitor-exit(r9)     // Catch: java.lang.Throwable -> L9c0
            goto La2e
        L9ba:
            r0 = move-exception
            r12 = r45
        L9bd:
            r8 = r0
            monitor-exit(r9)     // Catch: java.lang.Throwable -> L9c0
            throw r8
        L9c0:
            r0 = move-exception
            goto L9bd
        L9c2:
            r12 = r45
            r3 = 1
            r9 = r10 | 1
            com.android.server.pm.PackageManagerService r3 = r1.mPm     // Catch: com.android.server.pm.PackageManagerException -> L1136
            com.android.server.pm.PackageManagerTracedLock r3 = r3.mLock     // Catch: com.android.server.pm.PackageManagerException -> L1136
            monitor-enter(r3)     // Catch: com.android.server.pm.PackageManagerException -> L1136
            com.android.server.pm.PackageManagerService r10 = r1.mPm     // Catch: java.lang.Throwable -> L1124
            com.android.server.pm.Settings r10 = r10.mSettings     // Catch: java.lang.Throwable -> L1124
            com.android.server.pm.PackageSetting r10 = r10.getPackageLPr(r5)     // Catch: java.lang.Throwable -> L1124
            com.android.server.pm.PackageManagerService r11 = r1.mPm     // Catch: java.lang.Throwable -> L1124
            com.android.server.utils.WatchedArrayMap<java.lang.String, com.android.server.pm.parsing.pkg.AndroidPackage> r11 = r11.mPackages     // Catch: java.lang.Throwable -> L1124
            java.lang.Object r11 = r11.get(r5)     // Catch: java.lang.Throwable -> L1124
            com.android.server.pm.parsing.pkg.AndroidPackage r11 = (com.android.server.pm.parsing.pkg.AndroidPackage) r11     // Catch: java.lang.Throwable -> L1124
            monitor-exit(r3)     // Catch: java.lang.Throwable -> L1124
            if (r10 == 0) goto L9f9
            com.android.server.pm.pkg.PackageStateUnserialized r3 = r10.getPkgState()     // Catch: com.android.server.pm.PackageManagerException -> L9ed
            boolean r3 = r3.isUpdatedSystemApp()     // Catch: com.android.server.pm.PackageManagerException -> L9ed
            if (r3 == 0) goto L9f9
            r3 = 1
            goto L9fa
        L9ed:
            r0 = move-exception
            r2 = r0
            r18 = r4
            r19 = r5
            r40 = r38
            r38 = r6
            goto L1140
        L9f9:
            r3 = 0
        L9fa:
            java.lang.String r13 = r2.mAbiOverride     // Catch: com.android.server.pm.PackageManagerException -> L1136
            java.lang.String r13 = com.android.server.pm.PackageManagerServiceUtils.deriveAbiOverride(r13)     // Catch: com.android.server.pm.PackageManagerException -> L1136
            if (r11 == 0) goto La0a
            boolean r14 = r11.isSystem()     // Catch: com.android.server.pm.PackageManagerException -> L9ed
            if (r14 == 0) goto La0a
            r14 = 1
            goto La0b
        La0a:
            r14 = 0
        La0b:
            com.android.server.pm.PackageAbiHelper r15 = r1.mPackageAbiHelper     // Catch: com.android.server.pm.PackageManagerException -> L1136
            if (r3 != 0) goto La14
            if (r14 == 0) goto La12
            goto La14
        La12:
            r8 = 0
            goto La15
        La14:
            r8 = 1
        La15:
            r18 = r3
            java.io.File r3 = com.android.server.pm.ScanPackageUtils.getAppLib32InstallDir()     // Catch: com.android.server.pm.PackageManagerException -> L1136
            android.util.Pair r3 = r15.derivePackageAbi(r7, r8, r13, r3)     // Catch: com.android.server.pm.PackageManagerException -> L1136
            java.lang.Object r8 = r3.first     // Catch: com.android.server.pm.PackageManagerException -> L1136
            com.android.server.pm.PackageAbiHelper$Abis r8 = (com.android.server.pm.PackageAbiHelper.Abis) r8     // Catch: com.android.server.pm.PackageManagerException -> L1136
            r8.applyTo(r7)     // Catch: com.android.server.pm.PackageManagerException -> L1136
            java.lang.Object r8 = r3.second     // Catch: com.android.server.pm.PackageManagerException -> L1136
            com.android.server.pm.PackageAbiHelper$NativeLibraryPaths r8 = (com.android.server.pm.PackageAbiHelper.NativeLibraryPaths) r8     // Catch: com.android.server.pm.PackageManagerException -> L1136
            r8.applyTo(r7)     // Catch: com.android.server.pm.PackageManagerException -> L1136
            r3 = r9
        La2e:
            int r8 = r12.mReturnCode
            boolean r8 = r2.doRename(r8, r7)
            if (r8 == 0) goto L1117
            r1.setUpFsVerityIfPossible(r7)     // Catch: java.lang.Throwable -> L10f0
            java.lang.String r8 = "installPackageLI"
            r11 = r38
            com.android.server.pm.PackageFreezer r8 = r1.freezePackageForInstall(r5, r11, r8)
            r9 = 1
            r10 = 0
            r13 = r3
            r14 = r36
            if (r31 == 0) goto L1000
            java.lang.String r15 = r7.getPackageName()     // Catch: java.lang.Throwable -> Lfee
            r18 = r4
            com.android.server.pm.PackageManagerService r4 = r1.mPm     // Catch: java.lang.Throwable -> Lfde
            com.android.server.pm.PackageManagerTracedLock r4 = r4.mLock     // Catch: java.lang.Throwable -> Lfde
            monitor-enter(r4)     // Catch: java.lang.Throwable -> Lfde
            r19 = r5
            com.android.server.pm.PackageManagerService r5 = r1.mPm     // Catch: java.lang.Throwable -> Lfbd
            com.android.server.utils.WatchedArrayMap<java.lang.String, com.android.server.pm.parsing.pkg.AndroidPackage> r5 = r5.mPackages     // Catch: java.lang.Throwable -> Lfbd
            java.lang.Object r5 = r5.get(r15)     // Catch: java.lang.Throwable -> Lfbd
            com.android.server.pm.parsing.pkg.AndroidPackage r5 = (com.android.server.pm.parsing.pkg.AndroidPackage) r5     // Catch: java.lang.Throwable -> Lfbd
            monitor-exit(r4)     // Catch: java.lang.Throwable -> Lfbd
            boolean r4 = r7.isStaticSharedLibrary()     // Catch: java.lang.Throwable -> Lfaf
            if (r4 == 0) goto Laaa
            if (r5 == 0) goto Laa5
            r4 = r11 & 32
            if (r4 == 0) goto La72
            r38 = r6
            r24 = r9
            goto Laae
        La72:
            com.android.server.pm.PrepareFailure r4 = new com.android.server.pm.PrepareFailure     // Catch: java.lang.Throwable -> La97
            r38 = r6
            java.lang.String r6 = "Packages declaring static-shared libs cannot be updated"
            r24 = r9
            r9 = -5
            r4.<init>(r9, r6)     // Catch: java.lang.Throwable -> La7f
            throw r4     // Catch: java.lang.Throwable -> La7f
        La7f:
            r0 = move-exception
            r42 = r3
            r40 = r11
            r9 = r24
            r2 = r31
            r3 = r0
            goto L10e8
        La8b:
            r0 = move-exception
            r24 = r9
            r42 = r3
            r40 = r11
            r2 = r31
            r3 = r0
            goto L10e8
        La97:
            r0 = move-exception
            r38 = r6
            r24 = r9
            r42 = r3
            r40 = r11
            r2 = r31
            r3 = r0
            goto L10e8
        Laa5:
            r38 = r6
            r24 = r9
            goto Laae
        Laaa:
            r38 = r6
            r24 = r9
        Laae:
            r4 = r3 & 8192(0x2000, float:1.14794E-41)
            if (r4 == 0) goto Lab4
            r4 = 1
            goto Lab5
        Lab4:
            r4 = 0
        Lab5:
            com.android.server.pm.PackageManagerService r6 = r1.mPm     // Catch: java.lang.Throwable -> Lfa3
            com.android.server.pm.PackageManagerTracedLock r6 = r6.mLock     // Catch: java.lang.Throwable -> Lfa3
            monitor-enter(r6)     // Catch: java.lang.Throwable -> Lfa3
            boolean r9 = com.android.server.pm.PackageManagerService.DEBUG_INSTALL     // Catch: java.lang.Throwable -> Lf8e
            if (r9 == 0) goto Lb16
            java.lang.String r9 = "PackageManager"
            r16 = r10
            java.lang.StringBuilder r10 = new java.lang.StringBuilder     // Catch: java.lang.Throwable -> Laf4
            r10.<init>()     // Catch: java.lang.Throwable -> Laf4
            r40 = r11
            java.lang.String r11 = "replacePackageLI: new="
            java.lang.StringBuilder r10 = r10.append(r11)     // Catch: java.lang.Throwable -> Lae6
            java.lang.StringBuilder r10 = r10.append(r7)     // Catch: java.lang.Throwable -> Lae6
            java.lang.String r11 = ", old="
            java.lang.StringBuilder r10 = r10.append(r11)     // Catch: java.lang.Throwable -> Lae6
            java.lang.StringBuilder r10 = r10.append(r5)     // Catch: java.lang.Throwable -> Lae6
            java.lang.String r10 = r10.toString()     // Catch: java.lang.Throwable -> Lae6
            android.util.Slog.d(r9, r10)     // Catch: java.lang.Throwable -> Lae6
            goto Lb1a
        Lae6:
            r0 = move-exception
            r2 = r0
            r42 = r3
            r41 = r4
            r39 = r8
            r32 = r13
            r29 = r14
            goto Lf9e
        Laf4:
            r0 = move-exception
            r40 = r11
            r2 = r0
            r42 = r3
            r41 = r4
            r39 = r8
            r32 = r13
            r29 = r14
            goto Lf9e
        Lb04:
            r0 = move-exception
            r16 = r10
            r40 = r11
            r2 = r0
            r42 = r3
            r41 = r4
            r39 = r8
            r32 = r13
            r29 = r14
            goto Lf9e
        Lb16:
            r16 = r10
            r40 = r11
        Lb1a:
            com.android.server.pm.PackageManagerService r9 = r1.mPm     // Catch: java.lang.Throwable -> Lf81
            com.android.server.pm.Settings r9 = r9.mSettings     // Catch: java.lang.Throwable -> Lf81
            com.android.server.pm.PackageSetting r9 = r9.getPackageLPr(r15)     // Catch: java.lang.Throwable -> Lf81
            com.android.server.pm.PackageManagerService r10 = r1.mPm     // Catch: java.lang.Throwable -> Lf81
            com.android.server.pm.Settings r10 = r10.mSettings     // Catch: java.lang.Throwable -> Lf81
            com.android.server.pm.PackageSetting r10 = r10.getDisabledSystemPkgLPr(r9)     // Catch: java.lang.Throwable -> Lf81
            com.android.server.pm.PackageManagerService r11 = r1.mPm     // Catch: java.lang.Throwable -> Lf81
            com.android.server.pm.Settings r11 = r11.mSettings     // Catch: java.lang.Throwable -> Lf81
            com.android.server.pm.SharedUserSetting r11 = r11.getSharedUserSettingLPr(r9)     // Catch: java.lang.Throwable -> Lf81
            r25 = r10
            com.android.server.pm.PackageManagerService r10 = r1.mPm     // Catch: java.lang.Throwable -> Lf81
            com.android.server.pm.Settings r10 = r10.mSettings     // Catch: java.lang.Throwable -> Lf81
            com.android.server.pm.KeySetManagerService r10 = r10.getKeySetManagerService()     // Catch: java.lang.Throwable -> Lf81
            boolean r27 = r10.shouldCheckUpgradeKeySetLocked(r9, r11, r3)     // Catch: java.lang.Throwable -> Lf81
            if (r27 == 0) goto Lb7e
            boolean r27 = r10.checkUpgradeKeySetLocked(r9, r7)     // Catch: java.lang.Throwable -> Lb70
            if (r27 == 0) goto Lb50
            r27 = r10
            r30 = r11
            r32 = r13
            goto Lbd7
        Lb50:
            r27 = r10
            com.android.server.pm.PrepareFailure r10 = new com.android.server.pm.PrepareFailure     // Catch: java.lang.Throwable -> Lb70
            r30 = r11
            java.lang.StringBuilder r11 = new java.lang.StringBuilder     // Catch: java.lang.Throwable -> Lb70
            r11.<init>()     // Catch: java.lang.Throwable -> Lb70
            r32 = r13
            java.lang.String r13 = "New package not signed by keys specified by upgrade-keysets: "
            java.lang.StringBuilder r11 = r11.append(r13)     // Catch: java.lang.Throwable -> Lbc7
            java.lang.StringBuilder r11 = r11.append(r15)     // Catch: java.lang.Throwable -> Lbc7
            java.lang.String r11 = r11.toString()     // Catch: java.lang.Throwable -> Lbc7
            r13 = -7
            r10.<init>(r13, r11)     // Catch: java.lang.Throwable -> Lbc7
            throw r10     // Catch: java.lang.Throwable -> Lbc7
        Lb70:
            r0 = move-exception
            r32 = r13
            r2 = r0
            r42 = r3
            r41 = r4
            r39 = r8
            r29 = r14
            goto Lf9e
        Lb7e:
            r27 = r10
            r30 = r11
            r32 = r13
            android.content.pm.SigningDetails r10 = r7.getSigningDetails()     // Catch: java.lang.Throwable -> Lf76
            android.content.pm.SigningDetails r11 = r5.getSigningDetails()     // Catch: java.lang.Throwable -> Lf76
            r13 = 1
            boolean r41 = r10.checkCapability(r11, r13)     // Catch: java.lang.Throwable -> Lf76
            if (r41 != 0) goto Lbd3
            r13 = 8
            boolean r13 = r11.checkCapability(r10, r13)     // Catch: java.lang.Throwable -> Lbc7
            if (r13 != 0) goto Lbc2
            if (r20 == 0) goto Lba4
            boolean r13 = r11.hasAncestorOrSelf(r10)     // Catch: java.lang.Throwable -> Lbc7
            if (r13 == 0) goto Lba4
            goto Lbd7
        Lba4:
            com.android.server.pm.PrepareFailure r13 = new com.android.server.pm.PrepareFailure     // Catch: java.lang.Throwable -> Lbc7
            r39 = r10
            java.lang.StringBuilder r10 = new java.lang.StringBuilder     // Catch: java.lang.Throwable -> Lbc7
            r10.<init>()     // Catch: java.lang.Throwable -> Lbc7
            r41 = r11
            java.lang.String r11 = "New package has a different signature: "
            java.lang.StringBuilder r10 = r10.append(r11)     // Catch: java.lang.Throwable -> Lbc7
            java.lang.StringBuilder r10 = r10.append(r15)     // Catch: java.lang.Throwable -> Lbc7
            java.lang.String r10 = r10.toString()     // Catch: java.lang.Throwable -> Lbc7
            r11 = -7
            r13.<init>(r11, r10)     // Catch: java.lang.Throwable -> Lbc7
            throw r13     // Catch: java.lang.Throwable -> Lbc7
        Lbc2:
            r39 = r10
            r41 = r11
            goto Lbd7
        Lbc7:
            r0 = move-exception
            r2 = r0
            r42 = r3
            r41 = r4
            r39 = r8
            r29 = r14
            goto Lf9e
        Lbd3:
            r39 = r10
            r41 = r11
        Lbd7:
            byte[] r10 = r5.getRestrictUpdateHash()     // Catch: java.lang.Throwable -> Lf76
            if (r10 == 0) goto Lc82
            boolean r10 = r5.isSystem()     // Catch: java.lang.Throwable -> Lc76
            if (r10 == 0) goto Lc82
            java.lang.String r10 = "SHA-512"
            java.security.MessageDigest r10 = java.security.MessageDigest.getInstance(r10)     // Catch: java.lang.Throwable -> Lc58 java.lang.Throwable -> Lc76
            java.io.File r11 = new java.io.File     // Catch: java.lang.Throwable -> Lc58 java.lang.Throwable -> Lc76
            java.lang.String r13 = r7.getBaseApkPath()     // Catch: java.lang.Throwable -> Lc58 java.lang.Throwable -> Lc76
            r11.<init>(r13)     // Catch: java.lang.Throwable -> Lc58 java.lang.Throwable -> Lc76
            updateDigest(r10, r11)     // Catch: java.lang.Throwable -> Lc58 java.lang.Throwable -> Lc76
            java.lang.String[] r11 = r7.getSplitCodePaths()     // Catch: java.lang.Throwable -> Lc58 java.lang.Throwable -> Lc76
            boolean r11 = com.android.internal.util.ArrayUtils.isEmpty(r11)     // Catch: java.lang.Throwable -> Lc58 java.lang.Throwable -> Lc76
            if (r11 != 0) goto Lc22
            java.lang.String[] r11 = r7.getSplitCodePaths()     // Catch: java.lang.Throwable -> Lc58 java.lang.Throwable -> Lc76
            int r13 = r11.length     // Catch: java.lang.Throwable -> Lc58 java.lang.Throwable -> Lc76
            r29 = r14
            r14 = 0
        Lc07:
            if (r14 >= r13) goto Lc24
            r39 = r11[r14]     // Catch: java.lang.Throwable -> Lc55 java.lang.Throwable -> Lc8f
            r41 = r39
            r39 = r11
            java.io.File r11 = new java.io.File     // Catch: java.lang.Throwable -> Lc55 java.lang.Throwable -> Lc8f
            r42 = r13
            r13 = r41
            r11.<init>(r13)     // Catch: java.lang.Throwable -> Lc55 java.lang.Throwable -> Lc8f
            updateDigest(r10, r11)     // Catch: java.lang.Throwable -> Lc55 java.lang.Throwable -> Lc8f
            int r14 = r14 + 1
            r11 = r39
            r13 = r42
            goto Lc07
        Lc22:
            r29 = r14
        Lc24:
            byte[] r11 = r10.digest()     // Catch: java.lang.Throwable -> Lc55 java.lang.Throwable -> Lc8f
            byte[] r10 = r5.getRestrictUpdateHash()     // Catch: java.lang.Throwable -> Lc8f
            boolean r10 = java.util.Arrays.equals(r10, r11)     // Catch: java.lang.Throwable -> Lc8f
            if (r10 == 0) goto Lc3b
            byte[] r10 = r5.getRestrictUpdateHash()     // Catch: java.lang.Throwable -> Lc8f
            r7.setRestrictUpdateHash(r10)     // Catch: java.lang.Throwable -> Lc8f
            goto Lc84
        Lc3b:
            com.android.server.pm.PrepareFailure r10 = new com.android.server.pm.PrepareFailure     // Catch: java.lang.Throwable -> Lc8f
            java.lang.StringBuilder r13 = new java.lang.StringBuilder     // Catch: java.lang.Throwable -> Lc8f
            r13.<init>()     // Catch: java.lang.Throwable -> Lc8f
            java.lang.String r14 = "New package fails restrict-update check: "
            java.lang.StringBuilder r13 = r13.append(r14)     // Catch: java.lang.Throwable -> Lc8f
            java.lang.StringBuilder r13 = r13.append(r15)     // Catch: java.lang.Throwable -> Lc8f
            java.lang.String r13 = r13.toString()     // Catch: java.lang.Throwable -> Lc8f
            r14 = -2
            r10.<init>(r14, r13)     // Catch: java.lang.Throwable -> Lc8f
            throw r10     // Catch: java.lang.Throwable -> Lc8f
        Lc55:
            r0 = move-exception
            r10 = r0
            goto Lc5c
        Lc58:
            r0 = move-exception
            r29 = r14
            r10 = r0
        Lc5c:
            com.android.server.pm.PrepareFailure r11 = new com.android.server.pm.PrepareFailure     // Catch: java.lang.Throwable -> Lc8f
            java.lang.StringBuilder r13 = new java.lang.StringBuilder     // Catch: java.lang.Throwable -> Lc8f
            r13.<init>()     // Catch: java.lang.Throwable -> Lc8f
            java.lang.String r14 = "Could not compute hash: "
            java.lang.StringBuilder r13 = r13.append(r14)     // Catch: java.lang.Throwable -> Lc8f
            java.lang.StringBuilder r13 = r13.append(r15)     // Catch: java.lang.Throwable -> Lc8f
            java.lang.String r13 = r13.toString()     // Catch: java.lang.Throwable -> Lc8f
            r14 = -2
            r11.<init>(r14, r13)     // Catch: java.lang.Throwable -> Lc8f
            throw r11     // Catch: java.lang.Throwable -> Lc8f
        Lc76:
            r0 = move-exception
            r29 = r14
            r2 = r0
            r42 = r3
            r41 = r4
            r39 = r8
            goto Lf9e
        Lc82:
            r29 = r14
        Lc84:
            java.lang.String r10 = r5.getSharedUserId()     // Catch: java.lang.Throwable -> Lf6d
            if (r10 == 0) goto Lc99
            java.lang.String r10 = r5.getSharedUserId()     // Catch: java.lang.Throwable -> Lc8f
            goto Lc9b
        Lc8f:
            r0 = move-exception
            r2 = r0
            r42 = r3
            r41 = r4
            r39 = r8
            goto Lf9e
        Lc99:
            java.lang.String r10 = "<nothing>"
        Lc9b:
            java.lang.String r11 = r7.getSharedUserId()     // Catch: java.lang.Throwable -> Lf6d
            if (r11 == 0) goto Lca6
            java.lang.String r11 = r7.getSharedUserId()     // Catch: java.lang.Throwable -> Lc8f
            goto Lca8
        Lca6:
            java.lang.String r11 = "<nothing>"
        Lca8:
            boolean r13 = r10.equals(r11)     // Catch: java.lang.Throwable -> Lf6d
            if (r13 == 0) goto Lf30
            boolean r13 = r5.isLeavingSharedUid()     // Catch: java.lang.Throwable -> Lf6d
            if (r13 == 0) goto Lcfa
            boolean r13 = r7.isLeavingSharedUid()     // Catch: java.lang.Throwable -> Lcf0
            if (r13 == 0) goto Lcbd
            r39 = r8
            goto Lcfc
        Lcbd:
            com.android.server.pm.PrepareFailure r13 = new com.android.server.pm.PrepareFailure     // Catch: java.lang.Throwable -> Lcf0
            java.lang.StringBuilder r14 = new java.lang.StringBuilder     // Catch: java.lang.Throwable -> Lcf0
            r14.<init>()     // Catch: java.lang.Throwable -> Lcf0
            r39 = r8
            java.lang.String r8 = "Package "
            java.lang.StringBuilder r8 = r14.append(r8)     // Catch: java.lang.Throwable -> Lce8
            java.lang.String r14 = r7.getPackageName()     // Catch: java.lang.Throwable -> Lce8
            java.lang.StringBuilder r8 = r8.append(r14)     // Catch: java.lang.Throwable -> Lce8
            java.lang.String r14 = " attempting to rejoin "
            java.lang.StringBuilder r8 = r8.append(r14)     // Catch: java.lang.Throwable -> Lce8
            java.lang.StringBuilder r8 = r8.append(r11)     // Catch: java.lang.Throwable -> Lce8
            java.lang.String r8 = r8.toString()     // Catch: java.lang.Throwable -> Lce8
            r14 = -24
            r13.<init>(r14, r8)     // Catch: java.lang.Throwable -> Lce8
            throw r13     // Catch: java.lang.Throwable -> Lce8
        Lce8:
            r0 = move-exception
            r2 = r0
            r42 = r3
            r41 = r4
            goto Lf9e
        Lcf0:
            r0 = move-exception
            r39 = r8
            r2 = r0
            r42 = r3
            r41 = r4
            goto Lf9e
        Lcfa:
            r39 = r8
        Lcfc:
            com.android.server.pm.PackageManagerService r8 = r1.mPm     // Catch: java.lang.Throwable -> Lf28
            com.android.server.pm.UserManagerService r8 = r8.mUserManager     // Catch: java.lang.Throwable -> Lf28
            int[] r8 = r8.getUserIds()     // Catch: java.lang.Throwable -> Lf28
            r13 = 1
            int[] r14 = r9.queryInstalledUsers(r8, r13)     // Catch: java.lang.Throwable -> Lf28
            r13 = r14
            r14 = 0
            int[] r23 = r9.queryInstalledUsers(r8, r14)     // Catch: java.lang.Throwable -> Lf28
            r28 = r23
            if (r4 == 0) goto Ldb0
            android.os.UserHandle r14 = r2.mUser     // Catch: java.lang.Throwable -> Lda8
            if (r14 == 0) goto Ld65
            android.os.UserHandle r14 = r2.mUser     // Catch: java.lang.Throwable -> Lda8
            int r14 = r14.getIdentifier()     // Catch: java.lang.Throwable -> Lda8
            r41 = r4
            r4 = -1
            if (r14 != r4) goto Ld25
            r17 = r11
            goto Ld69
        Ld25:
            android.os.UserHandle r4 = r2.mUser     // Catch: java.lang.Throwable -> Lda2
            int r4 = r4.getIdentifier()     // Catch: java.lang.Throwable -> Lda2
            boolean r4 = r9.getInstantApp(r4)     // Catch: java.lang.Throwable -> Lda2
            if (r4 == 0) goto Ld33
            goto Ldb4
        Ld33:
            java.lang.String r4 = "PackageManager"
            java.lang.StringBuilder r14 = new java.lang.StringBuilder     // Catch: java.lang.Throwable -> Lda2
            r14.<init>()     // Catch: java.lang.Throwable -> Lda2
            r17 = r11
            java.lang.String r11 = "Can't replace full app with instant app: "
            java.lang.StringBuilder r11 = r14.append(r11)     // Catch: java.lang.Throwable -> Lda2
            java.lang.StringBuilder r11 = r11.append(r15)     // Catch: java.lang.Throwable -> Lda2
            java.lang.String r14 = " for user: "
            java.lang.StringBuilder r11 = r11.append(r14)     // Catch: java.lang.Throwable -> Lda2
            android.os.UserHandle r14 = r2.mUser     // Catch: java.lang.Throwable -> Lda2
            int r14 = r14.getIdentifier()     // Catch: java.lang.Throwable -> Lda2
            java.lang.StringBuilder r11 = r11.append(r14)     // Catch: java.lang.Throwable -> Lda2
            java.lang.String r11 = r11.toString()     // Catch: java.lang.Throwable -> Lda2
            android.util.Slog.w(r4, r11)     // Catch: java.lang.Throwable -> Lda2
            com.android.server.pm.PrepareFailure r4 = new com.android.server.pm.PrepareFailure     // Catch: java.lang.Throwable -> Lda2
            r11 = -116(0xffffffffffffff8c, float:NaN)
            r4.<init>(r11)     // Catch: java.lang.Throwable -> Lda2
            throw r4     // Catch: java.lang.Throwable -> Lda2
        Ld65:
            r41 = r4
            r17 = r11
        Ld69:
            int r4 = r8.length     // Catch: java.lang.Throwable -> Lda2
            r11 = 0
        Ld6b:
            if (r11 >= r4) goto Ldb4
            r14 = r8[r11]     // Catch: java.lang.Throwable -> Lda2
            boolean r42 = r9.getInstantApp(r14)     // Catch: java.lang.Throwable -> Lda2
            if (r42 == 0) goto Ld78
            int r11 = r11 + 1
            goto Ld6b
        Ld78:
            java.lang.String r4 = "PackageManager"
            java.lang.StringBuilder r11 = new java.lang.StringBuilder     // Catch: java.lang.Throwable -> Lda2
            r11.<init>()     // Catch: java.lang.Throwable -> Lda2
            java.lang.String r2 = "Can't replace full app with instant app: "
            java.lang.StringBuilder r2 = r11.append(r2)     // Catch: java.lang.Throwable -> Lda2
            java.lang.StringBuilder r2 = r2.append(r15)     // Catch: java.lang.Throwable -> Lda2
            java.lang.String r11 = " for user: "
            java.lang.StringBuilder r2 = r2.append(r11)     // Catch: java.lang.Throwable -> Lda2
            java.lang.StringBuilder r2 = r2.append(r14)     // Catch: java.lang.Throwable -> Lda2
            java.lang.String r2 = r2.toString()     // Catch: java.lang.Throwable -> Lda2
            android.util.Slog.w(r4, r2)     // Catch: java.lang.Throwable -> Lda2
            com.android.server.pm.PrepareFailure r2 = new com.android.server.pm.PrepareFailure     // Catch: java.lang.Throwable -> Lda2
            r4 = -116(0xffffffffffffff8c, float:NaN)
            r2.<init>(r4)     // Catch: java.lang.Throwable -> Lda2
            throw r2     // Catch: java.lang.Throwable -> Lda2
        Lda2:
            r0 = move-exception
            r2 = r0
            r42 = r3
            goto Lf9e
        Lda8:
            r0 = move-exception
            r41 = r4
            r2 = r0
            r42 = r3
            goto Lf9e
        Ldb0:
            r41 = r4
            r17 = r11
        Ldb4:
            monitor-exit(r6)     // Catch: java.lang.Throwable -> Lf22
            com.android.server.pm.PackageRemovedInfo r2 = new com.android.server.pm.PackageRemovedInfo     // Catch: java.lang.Throwable -> Lf16
            com.android.server.pm.PackageManagerService r4 = r1.mPm     // Catch: java.lang.Throwable -> Lf16
            r2.<init>(r4)     // Catch: java.lang.Throwable -> Lf16
            r12.mRemovedInfo = r2     // Catch: java.lang.Throwable -> Lf16
            com.android.server.pm.PackageRemovedInfo r2 = r12.mRemovedInfo     // Catch: java.lang.Throwable -> Lf16
            int r4 = r5.getUid()     // Catch: java.lang.Throwable -> Lf16
            r2.mUid = r4     // Catch: java.lang.Throwable -> Lf16
            com.android.server.pm.PackageRemovedInfo r2 = r12.mRemovedInfo     // Catch: java.lang.Throwable -> Lf16
            java.lang.String r4 = r5.getPackageName()     // Catch: java.lang.Throwable -> Lf16
            r2.mRemovedPackage = r4     // Catch: java.lang.Throwable -> Lf16
            com.android.server.pm.PackageRemovedInfo r2 = r12.mRemovedInfo     // Catch: java.lang.Throwable -> Lf16
            com.android.server.pm.InstallSource r4 = r9.getInstallSource()     // Catch: java.lang.Throwable -> Lf16
            java.lang.String r4 = r4.installerPackageName     // Catch: java.lang.Throwable -> Lf16
            r2.mInstallerPackageName = r4     // Catch: java.lang.Throwable -> Lf16
            com.android.server.pm.PackageRemovedInfo r2 = r12.mRemovedInfo     // Catch: java.lang.Throwable -> Lf16
            java.lang.String r4 = r7.getStaticSharedLibName()     // Catch: java.lang.Throwable -> Lf16
            if (r4 == 0) goto Lde2
            r4 = 1
            goto Lde3
        Lde2:
            r4 = 0
        Lde3:
            r2.mIsStaticSharedLib = r4     // Catch: java.lang.Throwable -> Lf16
            com.android.server.pm.PackageRemovedInfo r2 = r12.mRemovedInfo     // Catch: java.lang.Throwable -> Lf16
            r4 = 1
            r2.mIsUpdate = r4     // Catch: java.lang.Throwable -> Lf16
            com.android.server.pm.PackageRemovedInfo r2 = r12.mRemovedInfo     // Catch: java.lang.Throwable -> Lf16
            r2.mOrigUsers = r13     // Catch: java.lang.Throwable -> Lf16
            com.android.server.pm.PackageRemovedInfo r2 = r12.mRemovedInfo     // Catch: java.lang.Throwable -> Lf16
            android.util.SparseArray r4 = new android.util.SparseArray     // Catch: java.lang.Throwable -> Lf16
            int r6 = r13.length     // Catch: java.lang.Throwable -> Lf16
            r4.<init>(r6)     // Catch: java.lang.Throwable -> Lf16
            r2.mInstallReasons = r4     // Catch: java.lang.Throwable -> Lf16
            r2 = 0
        Ldf9:
            int r4 = r13.length     // Catch: java.lang.Throwable -> Lf16
            if (r2 >= r4) goto Le1c
            r4 = r13[r2]     // Catch: java.lang.Throwable -> Le10
            com.android.server.pm.PackageRemovedInfo r6 = r12.mRemovedInfo     // Catch: java.lang.Throwable -> Le10
            android.util.SparseArray<java.lang.Integer> r6 = r6.mInstallReasons     // Catch: java.lang.Throwable -> Le10
            int r10 = r9.getInstallReason(r4)     // Catch: java.lang.Throwable -> Le10
            java.lang.Integer r10 = java.lang.Integer.valueOf(r10)     // Catch: java.lang.Throwable -> Le10
            r6.put(r4, r10)     // Catch: java.lang.Throwable -> Le10
            int r2 = r2 + 1
            goto Ldf9
        Le10:
            r0 = move-exception
            r42 = r3
            r9 = r24
            r2 = r31
            r8 = r39
            r3 = r0
            goto L10e8
        Le1c:
            com.android.server.pm.PackageRemovedInfo r2 = r12.mRemovedInfo     // Catch: java.lang.Throwable -> Lf16
            android.util.SparseArray r4 = new android.util.SparseArray     // Catch: java.lang.Throwable -> Lf16
            r6 = r28
            int r10 = r6.length     // Catch: java.lang.Throwable -> Lf16
            r4.<init>(r10)     // Catch: java.lang.Throwable -> Lf16
            r2.mUninstallReasons = r4     // Catch: java.lang.Throwable -> Lf16
            r2 = 0
        Le29:
            int r4 = r6.length     // Catch: java.lang.Throwable -> Lf16
            if (r2 >= r4) goto Le40
            r4 = r6[r2]     // Catch: java.lang.Throwable -> Le10
            com.android.server.pm.PackageRemovedInfo r10 = r12.mRemovedInfo     // Catch: java.lang.Throwable -> Le10
            android.util.SparseArray<java.lang.Integer> r10 = r10.mUninstallReasons     // Catch: java.lang.Throwable -> Le10
            int r11 = r9.getUninstallReason(r4)     // Catch: java.lang.Throwable -> Le10
            java.lang.Integer r11 = java.lang.Integer.valueOf(r11)     // Catch: java.lang.Throwable -> Le10
            r10.put(r4, r11)     // Catch: java.lang.Throwable -> Le10
            int r2 = r2 + 1
            goto Le29
        Le40:
            com.android.server.pm.PackageRemovedInfo r2 = r12.mRemovedInfo     // Catch: java.lang.Throwable -> Lf16
            boolean r4 = r5.isExternalStorage()     // Catch: java.lang.Throwable -> Lf16
            r2.mIsExternal = r4     // Catch: java.lang.Throwable -> Lf16
            boolean r2 = r5.isSystem()     // Catch: java.lang.Throwable -> Lf16
            r10 = r2
            if (r10 == 0) goto Lee4
            boolean r2 = r5.isPrivileged()     // Catch: java.lang.Throwable -> Lf16
            boolean r4 = r5.isOem()     // Catch: java.lang.Throwable -> Lf16
            boolean r11 = r5.isVendor()     // Catch: java.lang.Throwable -> Lf16
            boolean r14 = r5.isProduct()     // Catch: java.lang.Throwable -> Lf16
            boolean r16 = r5.isOdm()     // Catch: java.lang.Throwable -> Lf16
            boolean r17 = r5.isSystemExt()     // Catch: java.lang.Throwable -> Lf16
            r22 = r36
            r21 = 65536(0x10000, float:9.18355E-41)
            r21 = r3 | r21
            if (r2 == 0) goto Le72
            r27 = 131072(0x20000, float:1.83671E-40)
            goto Le74
        Le72:
            r27 = 0
        Le74:
            r21 = r21 | r27
            if (r4 == 0) goto Le7b
            r27 = 262144(0x40000, float:3.67342E-40)
            goto Le7d
        Le7b:
            r27 = 0
        Le7d:
            r21 = r21 | r27
            if (r11 == 0) goto Le84
            r27 = 524288(0x80000, float:7.34684E-40)
            goto Le86
        Le84:
            r27 = 0
        Le86:
            r21 = r21 | r27
            if (r14 == 0) goto Le8d
            r27 = 1048576(0x100000, float:1.469368E-39)
            goto Le8f
        Le8d:
            r27 = 0
        Le8f:
            r21 = r21 | r27
            if (r16 == 0) goto Le96
            r27 = 4194304(0x400000, float:5.877472E-39)
            goto Le98
        Le96:
            r27 = 0
        Le98:
            r21 = r21 | r27
            if (r17 == 0) goto Le9f
            r23 = r26
            goto Lea1
        Le9f:
            r23 = 0
        Lea1:
            r21 = r21 | r23
            boolean r23 = com.android.server.pm.PackageManagerService.DEBUG_INSTALL     // Catch: java.lang.Throwable -> Lf16
            if (r23 == 0) goto Led1
            r23 = r2
            java.lang.String r2 = "PackageManager"
            r42 = r3
            java.lang.StringBuilder r3 = new java.lang.StringBuilder     // Catch: java.lang.Throwable -> Lfd1
            r3.<init>()     // Catch: java.lang.Throwable -> Lfd1
            r26 = r4
            java.lang.String r4 = "replaceSystemPackageLI: new="
            java.lang.StringBuilder r3 = r3.append(r4)     // Catch: java.lang.Throwable -> Lfd1
            java.lang.StringBuilder r3 = r3.append(r7)     // Catch: java.lang.Throwable -> Lfd1
            java.lang.String r4 = ", old="
            java.lang.StringBuilder r3 = r3.append(r4)     // Catch: java.lang.Throwable -> Lfd1
            java.lang.StringBuilder r3 = r3.append(r5)     // Catch: java.lang.Throwable -> Lfd1
            java.lang.String r3 = r3.toString()     // Catch: java.lang.Throwable -> Lfd1
            android.util.Slog.d(r2, r3)     // Catch: java.lang.Throwable -> Lfd1
            goto Led7
        Led1:
            r23 = r2
            r42 = r3
            r26 = r4
        Led7:
            r2 = 1
            r12.setReturnCode(r2)     // Catch: java.lang.Throwable -> Lfd1
            r2 = r22
            r3 = r21
            r14 = r2
            r13 = r3
            r2 = r31
            goto Lf12
        Lee4:
            r42 = r3
            r2 = 1
            boolean r3 = com.android.server.pm.PackageManagerService.DEBUG_INSTALL     // Catch: java.lang.Throwable -> L1039
            if (r3 == 0) goto Lf0e
            java.lang.String r3 = "PackageManager"
            java.lang.StringBuilder r4 = new java.lang.StringBuilder     // Catch: java.lang.Throwable -> L1039
            r4.<init>()     // Catch: java.lang.Throwable -> L1039
            java.lang.String r11 = "replaceNonSystemPackageLI: new="
            java.lang.StringBuilder r4 = r4.append(r11)     // Catch: java.lang.Throwable -> L1039
            java.lang.StringBuilder r4 = r4.append(r7)     // Catch: java.lang.Throwable -> L1039
            java.lang.String r11 = ", old="
            java.lang.StringBuilder r4 = r4.append(r11)     // Catch: java.lang.Throwable -> L1039
            java.lang.StringBuilder r4 = r4.append(r5)     // Catch: java.lang.Throwable -> L1039
            java.lang.String r4 = r4.toString()     // Catch: java.lang.Throwable -> L1039
            android.util.Slog.d(r3, r4)     // Catch: java.lang.Throwable -> L1039
        Lf0e:
            r14 = r29
            r13 = r32
        Lf12:
            r3 = r25
            goto L1062
        Lf16:
            r0 = move-exception
            r42 = r3
            r3 = r0
            r9 = r24
            r2 = r31
            r8 = r39
            goto L10e8
        Lf22:
            r0 = move-exception
            r42 = r3
            r2 = r0
            goto Lf9e
        Lf28:
            r0 = move-exception
            r42 = r3
            r41 = r4
            r2 = r0
            goto Lf9e
        Lf30:
            r42 = r3
            r41 = r4
            r39 = r8
            r17 = r11
            com.android.server.pm.PrepareFailure r2 = new com.android.server.pm.PrepareFailure     // Catch: java.lang.Throwable -> Lfa0
            java.lang.StringBuilder r3 = new java.lang.StringBuilder     // Catch: java.lang.Throwable -> Lfa0
            r3.<init>()     // Catch: java.lang.Throwable -> Lfa0
            java.lang.String r4 = "Package "
            java.lang.StringBuilder r3 = r3.append(r4)     // Catch: java.lang.Throwable -> Lfa0
            java.lang.String r4 = r7.getPackageName()     // Catch: java.lang.Throwable -> Lfa0
            java.lang.StringBuilder r3 = r3.append(r4)     // Catch: java.lang.Throwable -> Lfa0
            java.lang.String r4 = " shared user changed from "
            java.lang.StringBuilder r3 = r3.append(r4)     // Catch: java.lang.Throwable -> Lfa0
            java.lang.StringBuilder r3 = r3.append(r10)     // Catch: java.lang.Throwable -> Lfa0
            java.lang.String r4 = " to "
            java.lang.StringBuilder r3 = r3.append(r4)     // Catch: java.lang.Throwable -> Lfa0
            r4 = r17
            java.lang.StringBuilder r3 = r3.append(r4)     // Catch: java.lang.Throwable -> Lfa0
            java.lang.String r3 = r3.toString()     // Catch: java.lang.Throwable -> Lfa0
            r8 = -24
            r2.<init>(r8, r3)     // Catch: java.lang.Throwable -> Lfa0
            throw r2     // Catch: java.lang.Throwable -> Lfa0
        Lf6d:
            r0 = move-exception
            r42 = r3
            r41 = r4
            r39 = r8
            r2 = r0
            goto Lf9e
        Lf76:
            r0 = move-exception
            r42 = r3
            r41 = r4
            r39 = r8
            r29 = r14
            r2 = r0
            goto Lf9e
        Lf81:
            r0 = move-exception
            r42 = r3
            r41 = r4
            r39 = r8
            r32 = r13
            r29 = r14
            r2 = r0
            goto Lf9e
        Lf8e:
            r0 = move-exception
            r42 = r3
            r41 = r4
            r39 = r8
            r16 = r10
            r40 = r11
            r32 = r13
            r29 = r14
            r2 = r0
        Lf9e:
            monitor-exit(r6)     // Catch: java.lang.Throwable -> Lfa0
            throw r2     // Catch: java.lang.Throwable -> Lfd1
        Lfa0:
            r0 = move-exception
            r2 = r0
            goto Lf9e
        Lfa3:
            r0 = move-exception
            r42 = r3
            r40 = r11
            r3 = r0
            r9 = r24
            r2 = r31
            goto L10e8
        Lfaf:
            r0 = move-exception
            r42 = r3
            r38 = r6
            r24 = r9
            r40 = r11
            r3 = r0
            r2 = r31
            goto L10e8
        Lfbd:
            r0 = move-exception
            r42 = r3
            r38 = r6
            r39 = r8
            r24 = r9
            r16 = r10
            r40 = r11
            r32 = r13
            r29 = r14
            r2 = r0
        Lfcf:
            monitor-exit(r4)     // Catch: java.lang.Throwable -> Lfdb
            throw r2     // Catch: java.lang.Throwable -> Lfd1
        Lfd1:
            r0 = move-exception
            r3 = r0
            r9 = r24
            r2 = r31
            r8 = r39
            goto L10e8
        Lfdb:
            r0 = move-exception
            r2 = r0
            goto Lfcf
        Lfde:
            r0 = move-exception
            r42 = r3
            r19 = r5
            r38 = r6
            r24 = r9
            r40 = r11
            r3 = r0
            r2 = r31
            goto L10e8
        Lfee:
            r0 = move-exception
            r42 = r3
            r18 = r4
            r19 = r5
            r38 = r6
            r24 = r9
            r40 = r11
            r3 = r0
            r2 = r31
            goto L10e8
        L1000:
            r42 = r3
            r18 = r4
            r19 = r5
            r38 = r6
            r39 = r8
            r24 = r9
            r16 = r10
            r40 = r11
            r32 = r13
            r29 = r14
            r9 = 0
            r10 = 0
            r2 = 0
            r5 = 0
            java.lang.String r3 = r7.getPackageName()     // Catch: java.lang.Throwable -> L10e2
            boolean r4 = com.android.server.pm.PackageManagerService.DEBUG_INSTALL     // Catch: java.lang.Throwable -> L10e2
            if (r4 == 0) goto L1041
            java.lang.String r4 = "PackageManager"
            java.lang.StringBuilder r6 = new java.lang.StringBuilder     // Catch: java.lang.Throwable -> L1039
            r6.<init>()     // Catch: java.lang.Throwable -> L1039
            java.lang.String r8 = "installNewPackageLI: "
            java.lang.StringBuilder r6 = r6.append(r8)     // Catch: java.lang.Throwable -> L1039
            java.lang.StringBuilder r6 = r6.append(r7)     // Catch: java.lang.Throwable -> L1039
            java.lang.String r6 = r6.toString()     // Catch: java.lang.Throwable -> L1039
            android.util.Slog.d(r4, r6)     // Catch: java.lang.Throwable -> L1039
            goto L1041
        L1039:
            r0 = move-exception
            r3 = r0
            r9 = r24
            r8 = r39
            goto L10e8
        L1041:
            com.android.server.pm.PackageManagerService r4 = r1.mPm     // Catch: java.lang.Throwable -> L10e2
            com.android.server.pm.PackageManagerTracedLock r4 = r4.mLock     // Catch: java.lang.Throwable -> L10e2
            monitor-enter(r4)     // Catch: java.lang.Throwable -> L10e2
            com.android.server.pm.PackageManagerService r6 = r1.mPm     // Catch: java.lang.Throwable -> L10d4
            com.android.server.pm.Settings r6 = r6.mSettings     // Catch: java.lang.Throwable -> L10d4
            java.lang.String r6 = r6.getRenamedPackageLPr(r3)     // Catch: java.lang.Throwable -> L10d4
            if (r6 != 0) goto L10ae
            com.android.server.pm.PackageManagerService r8 = r1.mPm     // Catch: java.lang.Throwable -> L10d4
            com.android.server.utils.WatchedArrayMap<java.lang.String, com.android.server.pm.parsing.pkg.AndroidPackage> r8 = r8.mPackages     // Catch: java.lang.Throwable -> L10d4
            boolean r8 = r8.containsKey(r3)     // Catch: java.lang.Throwable -> L10d4
            if (r8 != 0) goto L108c
            monitor-exit(r4)     // Catch: java.lang.Throwable -> L10d4
            r3 = r10
            r10 = r16
            r14 = r29
            r13 = r32
        L1062:
            r4 = 0
            com.android.server.pm.PrepareResult r6 = new com.android.server.pm.PrepareResult     // Catch: java.lang.Throwable -> L1086
            r23 = r6
            r24 = r2
            r25 = r13
            r26 = r14
            r27 = r5
            r28 = r7
            r29 = r2
            r30 = r10
            r31 = r9
            r32 = r3
            r23.<init>(r24, r25, r26, r27, r28, r29, r30, r31, r32)     // Catch: java.lang.Throwable -> L1086
            r8 = r39
            r12.mFreezer = r8
            if (r4 == 0) goto L1085
            r8.close()
        L1085:
            return r6
        L1086:
            r0 = move-exception
            r8 = r39
            r3 = r0
            r9 = r4
            goto L10e8
        L108c:
            r8 = r39
            com.android.server.pm.PrepareFailure r11 = new com.android.server.pm.PrepareFailure     // Catch: java.lang.Throwable -> L10df
            java.lang.StringBuilder r13 = new java.lang.StringBuilder     // Catch: java.lang.Throwable -> L10df
            r13.<init>()     // Catch: java.lang.Throwable -> L10df
            java.lang.String r14 = "Attempt to re-install "
            java.lang.StringBuilder r13 = r13.append(r14)     // Catch: java.lang.Throwable -> L10df
            java.lang.StringBuilder r13 = r13.append(r3)     // Catch: java.lang.Throwable -> L10df
            java.lang.String r14 = " without first uninstalling."
            java.lang.StringBuilder r13 = r13.append(r14)     // Catch: java.lang.Throwable -> L10df
            java.lang.String r13 = r13.toString()     // Catch: java.lang.Throwable -> L10df
            r14 = -1
            r11.<init>(r14, r13)     // Catch: java.lang.Throwable -> L10df
            throw r11     // Catch: java.lang.Throwable -> L10df
        L10ae:
            r8 = r39
            com.android.server.pm.PrepareFailure r11 = new com.android.server.pm.PrepareFailure     // Catch: java.lang.Throwable -> L10df
            java.lang.StringBuilder r13 = new java.lang.StringBuilder     // Catch: java.lang.Throwable -> L10df
            r13.<init>()     // Catch: java.lang.Throwable -> L10df
            java.lang.String r14 = "Attempt to re-install "
            java.lang.StringBuilder r13 = r13.append(r14)     // Catch: java.lang.Throwable -> L10df
            java.lang.StringBuilder r13 = r13.append(r3)     // Catch: java.lang.Throwable -> L10df
            java.lang.String r14 = " without first uninstalling package running as "
            java.lang.StringBuilder r13 = r13.append(r14)     // Catch: java.lang.Throwable -> L10df
            java.lang.StringBuilder r13 = r13.append(r6)     // Catch: java.lang.Throwable -> L10df
            java.lang.String r13 = r13.toString()     // Catch: java.lang.Throwable -> L10df
            r14 = -1
            r11.<init>(r14, r13)     // Catch: java.lang.Throwable -> L10df
            throw r11     // Catch: java.lang.Throwable -> L10df
        L10d4:
            r0 = move-exception
            r8 = r39
            r6 = r0
        L10d8:
            monitor-exit(r4)     // Catch: java.lang.Throwable -> L10df
            throw r6     // Catch: java.lang.Throwable -> L10da
        L10da:
            r0 = move-exception
            r3 = r0
            r9 = r24
            goto L10e8
        L10df:
            r0 = move-exception
            r6 = r0
            goto L10d8
        L10e2:
            r0 = move-exception
            r8 = r39
            r3 = r0
            r9 = r24
        L10e8:
            r12.mFreezer = r8
            if (r9 == 0) goto L10ef
            r8.close()
        L10ef:
            throw r3
        L10f0:
            r0 = move-exception
            r42 = r3
            r18 = r4
            r19 = r5
            r40 = r38
            r38 = r6
            r2 = r0
            com.android.server.pm.PrepareFailure r3 = new com.android.server.pm.PrepareFailure
            java.lang.StringBuilder r4 = new java.lang.StringBuilder
            r4.<init>()
            java.lang.String r5 = "Failed to set up verity: "
            java.lang.StringBuilder r4 = r4.append(r5)
            java.lang.StringBuilder r4 = r4.append(r2)
            java.lang.String r4 = r4.toString()
            r5 = -110(0xffffffffffffff92, float:NaN)
            r3.<init>(r5, r4)
            throw r3
        L1117:
            r42 = r3
            r18 = r4
            com.android.server.pm.PrepareFailure r2 = new com.android.server.pm.PrepareFailure
            r3 = -4
            java.lang.String r4 = "Failed rename"
            r2.<init>(r3, r4)
            throw r2
        L1124:
            r0 = move-exception
            r18 = r4
            r19 = r5
            r40 = r38
            r38 = r6
            r2 = r0
        L112e:
            monitor-exit(r3)     // Catch: java.lang.Throwable -> L1133
            throw r2     // Catch: com.android.server.pm.PackageManagerException -> L1130
        L1130:
            r0 = move-exception
            r2 = r0
            goto L1140
        L1133:
            r0 = move-exception
            r2 = r0
            goto L112e
        L1136:
            r0 = move-exception
            r18 = r4
            r19 = r5
            r40 = r38
            r38 = r6
            r2 = r0
        L1140:
            java.lang.String r3 = "PackageManager"
            java.lang.String r4 = "Error deriving application ABI"
            android.util.Slog.e(r3, r4, r2)
            com.android.server.pm.PrepareFailure r3 = new com.android.server.pm.PrepareFailure
            java.lang.StringBuilder r4 = new java.lang.StringBuilder
            r4.<init>()
            java.lang.String r5 = "Error deriving application ABI: "
            java.lang.StringBuilder r4 = r4.append(r5)
            java.lang.String r5 = r2.getMessage()
            java.lang.StringBuilder r4 = r4.append(r5)
            java.lang.String r4 = r4.toString()
            r5 = -110(0xffffffffffffff92, float:NaN)
            r3.<init>(r5, r4)
            throw r3
        L1166:
            r0 = move-exception
            r12 = r45
            goto L116c
        L116a:
            r0 = move-exception
            r12 = r3
        L116c:
            r18 = r4
            r19 = r5
            r40 = r38
            r38 = r6
            r2 = r0
            r24 = r18
            r14 = r31
            goto L119e
        L117a:
            r0 = move-exception
            r31 = r2
            r12 = r3
            r18 = r4
            r19 = r5
            r20 = r13
            r40 = r38
            r38 = r6
            r2 = r0
            r24 = r18
            r14 = r31
            goto L119e
        L118e:
            r0 = move-exception
            r31 = r2
            r12 = r3
            r40 = r4
            r19 = r5
            r38 = r6
            r20 = r13
            r10 = r15
            r2 = r0
            r14 = r31
        L119e:
            monitor-exit(r8)     // Catch: java.lang.Throwable -> L11a0
            throw r2
        L11a0:
            r0 = move-exception
            r2 = r0
            goto L119e
        L11a3:
            r40 = r4
            r38 = r6
            r35 = r12
            r12 = r3
            com.android.server.pm.PrepareFailure r3 = new com.android.server.pm.PrepareFailure
            java.lang.String r4 = "Failed collect during installPackageLI"
            java.lang.Exception r6 = r17.getException()
            r3.<init>(r4, r6)
            throw r3
        L11b6:
            r0 = move-exception
            r40 = r4
            r33 = r5
            r38 = r6
            r37 = r9
            r34 = r10
            r36 = r11
            r35 = r12
            r20 = r13
            r10 = r15
            r12 = r3
            r2 = r0
            com.android.server.pm.PrepareFailure r3 = new com.android.server.pm.PrepareFailure
            int r4 = r2.error
            java.lang.String r5 = r2.getMessage()
            r3.<init>(r4, r5)
            throw r3
        L11d6:
            r0 = move-exception
            r40 = r4
            r33 = r5
            r38 = r6
            r37 = r9
            r34 = r10
            r36 = r11
            r35 = r12
            r20 = r13
            r10 = r15
            r12 = r3
            r2 = r0
            if (r14 == 0) goto L11f5
            r14.close()     // Catch: java.lang.Throwable -> L11f0
            goto L11f5
        L11f0:
            r0 = move-exception
            r3 = r0
            r2.addSuppressed(r3)     // Catch: com.android.server.pm.PackageManagerException -> L11f6 java.lang.Throwable -> L122a
        L11f5:
            throw r2     // Catch: com.android.server.pm.PackageManagerException -> L11f6 java.lang.Throwable -> L122a
        L11f6:
            r0 = move-exception
            r2 = r0
            goto L1222
        L11f9:
            r0 = move-exception
            r40 = r4
            r33 = r5
            r38 = r6
            r37 = r9
            r34 = r10
            r36 = r11
            r35 = r12
            r20 = r13
            r10 = r15
            r12 = r3
            r2 = r0
            goto L122c
        L120e:
            r0 = move-exception
            r40 = r4
            r33 = r5
            r38 = r6
            r37 = r9
            r34 = r10
            r36 = r11
            r35 = r12
            r20 = r13
            r10 = r15
            r12 = r3
            r2 = r0
        L1222:
            com.android.server.pm.PrepareFailure r3 = new com.android.server.pm.PrepareFailure     // Catch: java.lang.Throwable -> L122a
            java.lang.String r4 = "Failed parse during installPackageLI"
            r3.<init>(r4, r2)     // Catch: java.lang.Throwable -> L122a
            throw r3     // Catch: java.lang.Throwable -> L122a
        L122a:
            r0 = move-exception
            r2 = r0
        L122c:
            r3 = 262144(0x40000, double:1.295163E-318)
            android.os.Trace.traceEnd(r3)
            throw r2
        */
        throw new UnsupportedOperationException("Method not decompiled: com.android.server.pm.InstallPackageHelper.preparePackageLI(com.android.server.pm.InstallArgs, com.android.server.pm.PackageInstalledInfo):com.android.server.pm.PrepareResult");
    }

    private static boolean cannotInstallWithBadPermissionGroups(ParsedPackage parsedPackage) {
        return parsedPackage.getTargetSdkVersion() >= 31;
    }

    private boolean doesSignatureMatchForPermissions(String sourcePackageName, ParsedPackage parsedPackage, int scanFlags) {
        PackageSetting sourcePackageSetting;
        KeySetManagerService ksms;
        SharedUserSetting sharedUserSetting;
        synchronized (this.mPm.mLock) {
            sourcePackageSetting = this.mPm.mSettings.getPackageLPr(sourcePackageName);
            ksms = this.mPm.mSettings.getKeySetManagerService();
            sharedUserSetting = this.mPm.mSettings.getSharedUserSettingLPr(sourcePackageSetting);
        }
        SigningDetails sourceSigningDetails = sourcePackageSetting == null ? SigningDetails.UNKNOWN : sourcePackageSetting.getSigningDetails();
        if (sourcePackageName.equals(parsedPackage.getPackageName()) && ksms.shouldCheckUpgradeKeySetLocked(sourcePackageSetting, sharedUserSetting, scanFlags)) {
            return ksms.checkUpgradeKeySetLocked(sourcePackageSetting, parsedPackage);
        }
        if (sourceSigningDetails.checkCapability(parsedPackage.getSigningDetails(), 4)) {
            return true;
        }
        if (parsedPackage.getSigningDetails().checkCapability(sourceSigningDetails, 4)) {
            synchronized (this.mPm.mLock) {
                sourcePackageSetting.setSigningDetails(parsedPackage.getSigningDetails());
            }
            return true;
        }
        return false;
    }

    private void setUpFsVerityIfPossible(AndroidPackage pkg) throws Installer.InstallerException, PrepareFailure, IOException, DigestException, NoSuchAlgorithmException {
        String[] splitCodePaths;
        if (!PackageManagerServiceUtils.isApkVerityEnabled()) {
            return;
        }
        if (IncrementalManager.isIncrementalPath(pkg.getPath()) && IncrementalManager.getVersion() < 2) {
            return;
        }
        ArrayMap<String, String> fsverityCandidates = new ArrayMap<>();
        fsverityCandidates.put(pkg.getBaseApkPath(), VerityUtils.getFsveritySignatureFilePath(pkg.getBaseApkPath()));
        String dmPath = DexMetadataHelper.buildDexMetadataPathForApk(pkg.getBaseApkPath());
        if (new File(dmPath).exists()) {
            fsverityCandidates.put(dmPath, VerityUtils.getFsveritySignatureFilePath(dmPath));
        }
        for (String path : pkg.getSplitCodePaths()) {
            fsverityCandidates.put(path, VerityUtils.getFsveritySignatureFilePath(path));
            String splitDmPath = DexMetadataHelper.buildDexMetadataPathForApk(path);
            if (new File(splitDmPath).exists()) {
                fsverityCandidates.put(splitDmPath, VerityUtils.getFsveritySignatureFilePath(splitDmPath));
            }
        }
        pkg.getPackageName();
        for (Map.Entry<String, String> entry : fsverityCandidates.entrySet()) {
            String filePath = entry.getKey();
            String signaturePath = entry.getValue();
            if (new File(signaturePath).exists() && !VerityUtils.hasFsverity(filePath)) {
                try {
                    VerityUtils.setUpFsverity(filePath, signaturePath);
                } catch (IOException e) {
                    throw new PrepareFailure(-118, "Failed to enable fs-verity: " + e);
                }
            }
        }
    }

    private PackageFreezer freezePackageForInstall(String packageName, int installFlags, String killReason) {
        return freezePackageForInstall(packageName, -1, installFlags, killReason);
    }

    private PackageFreezer freezePackageForInstall(String packageName, int userId, int installFlags, String killReason) {
        if ((installFlags & 4096) != 0) {
            return new PackageFreezer(this.mPm);
        }
        return this.mPm.freezePackage(packageName, userId, killReason);
    }

    private static void updateDigest(MessageDigest digest, File file) throws IOException {
        DigestInputStream digestStream = new DigestInputStream(new FileInputStream(file), digest);
        int total = 0;
        while (true) {
            try {
                int length = digestStream.read();
                if (length != -1) {
                    total += length;
                } else {
                    digestStream.close();
                    return;
                }
            } catch (Throwable th) {
                try {
                    digestStream.close();
                } catch (Throwable th2) {
                    th.addSuppressed(th2);
                }
                throw th;
            }
        }
    }

    /* JADX DEBUG: Multi-variable search result rejected for r11v8, resolved type: boolean */
    /* JADX DEBUG: Multi-variable search result rejected for r11v9, resolved type: boolean */
    /* JADX WARN: Multi-variable type inference failed */
    /* JADX WARN: Removed duplicated region for block: B:29:0x00ef  */
    /* JADX WARN: Removed duplicated region for block: B:33:0x0112  */
    /* JADX WARN: Removed duplicated region for block: B:40:0x013b  */
    /* JADX WARN: Removed duplicated region for block: B:41:0x013d  */
    /* JADX WARN: Removed duplicated region for block: B:55:0x01aa  */
    /* JADX WARN: Removed duplicated region for block: B:56:0x01be  */
    /* JADX WARN: Removed duplicated region for block: B:59:0x01c3  */
    /* JADX WARN: Removed duplicated region for block: B:70:0x01cf A[SYNTHETIC] */
    /*
        Code decompiled incorrectly, please refer to instructions dump.
    */
    private void commitPackagesLocked(CommitRequest request) {
        Iterator<ReconciledPackage> it;
        PackageSetting ps;
        int i;
        boolean isPreload;
        PackageStateInternal deletedPkgSetting;
        AndroidPackage oldPackage;
        RemovePackageHelper removePackageHelper;
        boolean z;
        AndroidPackage oldPackage2;
        RemovePackageHelper removePackageHelper2;
        int i2;
        boolean z2;
        PackageSetting ps2;
        Iterator<ReconciledPackage> it2 = request.mReconciledPackages.values().iterator();
        while (it2.hasNext()) {
            ReconciledPackage reconciledPkg = it2.next();
            ScanResult scanResult = reconciledPkg.mScanResult;
            ScanRequest scanRequest = scanResult.mRequest;
            ParsedPackage parsedPackage = scanRequest.mParsedPackage;
            String packageName = parsedPackage.getPackageName();
            PackageInstalledInfo res = reconciledPkg.mInstallResult;
            RemovePackageHelper removePackageHelper3 = new RemovePackageHelper(this.mPm);
            DeletePackageHelper deletePackageHelper = new DeletePackageHelper(this.mPm);
            if (!reconciledPkg.mPrepareResult.mReplace) {
                it = it2;
            } else {
                AndroidPackage oldPackage3 = this.mPm.mPackages.get(packageName);
                PackageStateInternal deletedPkgSetting2 = this.mPm.snapshotComputer().getPackageStateInternal(oldPackage3.getPackageName());
                reconciledPkg.mPkgSetting.setFirstInstallTimeFromReplaced(deletedPkgSetting2, request.mAllUsers).setLastUpdateTime(System.currentTimeMillis());
                it = it2;
                res.mRemovedInfo.mBroadcastAllowList = this.mPm.mAppsFilter.getVisibilityAllowList(this.mPm.snapshotComputer(), reconciledPkg.mPkgSetting, request.mAllUsers, this.mPm.mSettings.getPackagesLocked());
                if (PackageManagerService.sPmsExt.isRemovableSysApp(packageName) && ITranPackageManagerService.Instance().isPreloadApp(packageName, oldPackage3.getBaseApkPath())) {
                    isPreload = true;
                } else {
                    isPreload = false;
                }
                if (reconciledPkg.mPrepareResult.mSystem) {
                    deletedPkgSetting = deletedPkgSetting2;
                    oldPackage = oldPackage3;
                    removePackageHelper = removePackageHelper3;
                    z = true;
                } else if (isPreload) {
                    deletedPkgSetting = deletedPkgSetting2;
                    oldPackage = oldPackage3;
                    removePackageHelper = removePackageHelper3;
                    z = true;
                } else {
                    try {
                        oldPackage2 = oldPackage3;
                        i2 = 1;
                        removePackageHelper2 = removePackageHelper3;
                        try {
                            deletePackageHelper.executeDeletePackageLIF(reconciledPkg.mDeletePackageAction, packageName, true, request.mAllUsers, false);
                        } catch (SystemDeleteException e) {
                            e = e;
                            if (this.mPm.mIsEngBuild) {
                                throw new RuntimeException("Unexpected failure", e);
                            }
                            PackageSetting ps1 = this.mPm.mSettings.getPackageLPr(reconciledPkg.mPrepareResult.mExistingPackage.getPackageName());
                            if ((reconciledPkg.mInstallArgs.mInstallFlags & i2) != 0) {
                            }
                            if (reconciledPkg.mInstallResult.mReturnCode == i2) {
                                res.mRemovedInfo.mRemovedForAllUsers = this.mPm.mPackages.get(ps2.getPackageName()) != null ? i2 : z2;
                            }
                            AndroidPackage pkg = commitReconciledScanResultLocked(reconciledPkg, request.mAllUsers);
                            updateSettingsLI(pkg, reconciledPkg, request.mAllUsers, res);
                            ps = this.mPm.mSettings.getPackageLPr(packageName);
                            if (ps != null) {
                            }
                            if (res.mReturnCode != i) {
                            }
                            it2 = it;
                        }
                    } catch (SystemDeleteException e2) {
                        e = e2;
                        oldPackage2 = oldPackage3;
                        removePackageHelper2 = removePackageHelper3;
                        i2 = 1;
                    }
                    PackageSetting ps12 = this.mPm.mSettings.getPackageLPr(reconciledPkg.mPrepareResult.mExistingPackage.getPackageName());
                    if ((reconciledPkg.mInstallArgs.mInstallFlags & i2) != 0) {
                        Set<String> oldCodePaths = ps12.getOldCodePaths();
                        if (oldCodePaths == null) {
                            oldCodePaths = new ArraySet();
                        }
                        String[] strArr = new String[i2];
                        z2 = false;
                        strArr[0] = oldPackage2.getBaseApkPath();
                        Collections.addAll(oldCodePaths, strArr);
                        Collections.addAll(oldCodePaths, oldPackage2.getSplitCodePaths());
                        ps12.setOldCodePaths(oldCodePaths);
                    } else {
                        z2 = false;
                        ps12.setOldCodePaths(null);
                    }
                    if (reconciledPkg.mInstallResult.mReturnCode == i2 && (ps2 = this.mPm.mSettings.getPackageLPr(parsedPackage.getPackageName())) != null) {
                        res.mRemovedInfo.mRemovedForAllUsers = this.mPm.mPackages.get(ps2.getPackageName()) != null ? i2 : z2;
                    }
                }
                AndroidPackage oldPackage4 = oldPackage;
                removePackageHelper.removePackageLI(oldPackage4, z);
                if (disableSystemPackageLPw(oldPackage4)) {
                    res.mRemovedInfo.mArgs = null;
                } else {
                    PackageStateInternal deletedPkgSetting3 = deletedPkgSetting;
                    res.mRemovedInfo.mArgs = new FileInstallArgs(oldPackage4.getPath(), InstructionSets.getAppDexInstructionSets(AndroidPackageUtils.getPrimaryCpuAbi(oldPackage4, deletedPkgSetting3), AndroidPackageUtils.getSecondaryCpuAbi(oldPackage4, deletedPkgSetting3)), this.mPm);
                }
            }
            AndroidPackage pkg2 = commitReconciledScanResultLocked(reconciledPkg, request.mAllUsers);
            updateSettingsLI(pkg2, reconciledPkg, request.mAllUsers, res);
            ps = this.mPm.mSettings.getPackageLPr(packageName);
            if (ps != null) {
                i = 1;
            } else {
                i = 1;
                res.mNewUsers = ps.queryInstalledUsers(this.mPm.mUserManager.getUserIds(), true);
                ps.setUpdateAvailable(false);
            }
            if (res.mReturnCode != i) {
                this.mPm.updateSequenceNumberLP(ps, res.mNewUsers);
                this.mPm.updateInstantAppInstallerLocked(packageName);
            }
            it2 = it;
        }
        ApplicationPackageManager.invalidateGetPackagesForUidCache();
    }

    private boolean disableSystemPackageLPw(AndroidPackage oldPkg) {
        return this.mPm.mSettings.disableSystemPackageLPw(oldPkg.getPackageName(), true);
    }

    private void updateSettingsLI(AndroidPackage newPackage, ReconciledPackage reconciledPkg, int[] allUsers, PackageInstalledInfo res) {
        updateSettingsInternalLI(newPackage, reconciledPkg, allUsers, res);
    }

    /* JADX DEBUG: Don't trust debug lines info. Repeating lines: [2290=11] */
    /* JADX DEBUG: Failed to insert an additional move for type inference into block B:214:0x0403 */
    /* JADX WARN: Multi-variable type inference failed */
    /* JADX WARN: Type inference failed for: r13v0 */
    /* JADX WARN: Type inference failed for: r13v1 */
    /* JADX WARN: Type inference failed for: r13v10 */
    /* JADX WARN: Type inference failed for: r13v12 */
    /* JADX WARN: Type inference failed for: r13v13 */
    /* JADX WARN: Type inference failed for: r13v15 */
    /* JADX WARN: Type inference failed for: r13v16 */
    /* JADX WARN: Type inference failed for: r13v2 */
    /* JADX WARN: Type inference failed for: r13v24 */
    /* JADX WARN: Type inference failed for: r13v5 */
    /* JADX WARN: Type inference failed for: r13v6 */
    /* JADX WARN: Type inference failed for: r13v8 */
    /* JADX WARN: Type inference failed for: r13v9 */
    private void updateSettingsInternalLI(AndroidPackage pkg, ReconciledPackage reconciledPkg, int[] allUsers, PackageInstalledInfo res) {
        PackageManagerTracedLock packageManagerTracedLock;
        AndroidPackage androidPackage;
        String pkgName;
        int installReason;
        int origUserId;
        int installReason2;
        InstallArgs installArgs;
        int i;
        InstallArgs installArgs2;
        boolean restrictedByPolicy;
        IncrementalManager incrementalManager;
        Iterator<SharedLibraryInfo> it;
        int[] iArr;
        SharedLibraryInfo sharedLib;
        ?? r13 = 262144;
        Trace.traceBegin(262144L, "updateSettings");
        String pkgName2 = pkg.getPackageName();
        int[] installedForUsers = res.mOrigUsers;
        InstallArgs installArgs3 = reconciledPkg.mInstallArgs;
        int installReason3 = installArgs3.mInstallReason;
        InstallSource installSource = installArgs3.mInstallSource;
        String installerPackageName = installSource.installerPackageName;
        if (PackageManagerService.DEBUG_INSTALL) {
            Slog.d("PackageManager", "New package installed in " + pkg.getPath());
        }
        PackageManagerTracedLock packageManagerTracedLock2 = this.mPm.mLock;
        synchronized (packageManagerTracedLock2) {
            try {
                try {
                    PackageSetting ps = this.mPm.mSettings.getPackageLPr(pkgName2);
                    int userId = installArgs3.mUser.getIdentifier();
                    if (ps != null) {
                        if (pkg.isSystem()) {
                            try {
                                if (PackageManagerService.DEBUG_INSTALL) {
                                    try {
                                    } catch (Throwable th) {
                                        th = th;
                                        r13 = packageManagerTracedLock2;
                                    }
                                    try {
                                        Slog.d("PackageManager", "Implicitly enabling system package on upgrade: " + pkgName2);
                                    } catch (Throwable th2) {
                                        th = th2;
                                        r13 = packageManagerTracedLock2;
                                        throw th;
                                    }
                                }
                                try {
                                    if (res.mOrigUsers != null) {
                                        try {
                                            int[] iArr2 = res.mOrigUsers;
                                            int length = iArr2.length;
                                            int i2 = 0;
                                            while (i2 < length) {
                                                int origUserId2 = iArr2[i2];
                                                int[] iArr3 = iArr2;
                                                try {
                                                    if (userId != -1) {
                                                        origUserId = origUserId2;
                                                        if (userId != origUserId) {
                                                            installReason2 = installReason3;
                                                            i2++;
                                                            iArr2 = iArr3;
                                                            installReason3 = installReason2;
                                                        }
                                                    } else {
                                                        origUserId = origUserId2;
                                                    }
                                                    ps.setEnabled(0, origUserId, installerPackageName);
                                                    i2++;
                                                    iArr2 = iArr3;
                                                    installReason3 = installReason2;
                                                } catch (Throwable th3) {
                                                    th = th3;
                                                    r13 = packageManagerTracedLock2;
                                                    throw th;
                                                }
                                                installReason2 = installReason3;
                                            }
                                            installReason = installReason3;
                                        } catch (Throwable th4) {
                                            th = th4;
                                            r13 = packageManagerTracedLock2;
                                        }
                                    } else {
                                        installReason = installReason3;
                                    }
                                    if (allUsers == null || installedForUsers == null) {
                                        installArgs = installArgs3;
                                    } else {
                                        try {
                                            int length2 = allUsers.length;
                                            int i3 = 0;
                                            while (i3 < length2) {
                                                int currentUserId = allUsers[i3];
                                                boolean installed = ArrayUtils.contains(installedForUsers, currentUserId);
                                                if (PackageManagerService.DEBUG_INSTALL) {
                                                    i = length2;
                                                    installArgs2 = installArgs3;
                                                    try {
                                                        Slog.d("PackageManager", "    user " + currentUserId + " => " + installed);
                                                    } catch (Throwable th5) {
                                                        th = th5;
                                                        r13 = packageManagerTracedLock2;
                                                        throw th;
                                                    }
                                                } else {
                                                    i = length2;
                                                    installArgs2 = installArgs3;
                                                }
                                                ps.setInstalled(installed, currentUserId);
                                                i3++;
                                                length2 = i;
                                                installArgs3 = installArgs2;
                                            }
                                            installArgs = installArgs3;
                                        } catch (Throwable th6) {
                                            th = th6;
                                            r13 = packageManagerTracedLock2;
                                        }
                                    }
                                    if (allUsers != null) {
                                        for (int currentUserId2 : allUsers) {
                                            ps.resetOverrideComponentLabelIcon(currentUserId2);
                                        }
                                    }
                                } catch (Throwable th7) {
                                    th = th7;
                                    r13 = packageManagerTracedLock2;
                                }
                            } catch (Throwable th8) {
                                th = th8;
                                r13 = packageManagerTracedLock2;
                            }
                        } else {
                            installReason = installReason3;
                            installArgs = installArgs3;
                        }
                        try {
                            if (!ps.getPkgState().getUsesLibraryInfos().isEmpty()) {
                                Iterator<SharedLibraryInfo> it2 = ps.getPkgState().getUsesLibraryInfos().iterator();
                                while (it2.hasNext()) {
                                    SharedLibraryInfo sharedLib2 = it2.next();
                                    int[] userIds = UserManagerService.getInstance().getUserIds();
                                    int length3 = userIds.length;
                                    int i4 = 0;
                                    while (i4 < length3) {
                                        int currentUserId3 = userIds[i4];
                                        if (sharedLib2.isDynamic()) {
                                            it = it2;
                                            iArr = userIds;
                                            PackageSetting libPs = this.mPm.mSettings.getPackageLPr(sharedLib2.getPackageName());
                                            if (libPs == null) {
                                                sharedLib = sharedLib2;
                                            } else {
                                                sharedLib = sharedLib2;
                                                ps.setOverlayPathsForLibrary(sharedLib2.getName(), libPs.getOverlayPaths(currentUserId3), currentUserId3);
                                            }
                                        } else {
                                            it = it2;
                                            sharedLib = sharedLib2;
                                            iArr = userIds;
                                        }
                                        i4++;
                                        it2 = it;
                                        userIds = iArr;
                                        sharedLib2 = sharedLib;
                                    }
                                }
                            }
                            if (userId != -1) {
                                ps.setInstalled(true, userId);
                                ps.setEnabled(0, userId, installerPackageName);
                            } else if (allUsers != null) {
                                for (int currentUserId4 : allUsers) {
                                    if (currentUserId4 != 999) {
                                        boolean installedForCurrentUser = ArrayUtils.contains(installedForUsers, currentUserId4);
                                        if (!this.mPm.isUserRestricted(currentUserId4, "no_install_apps") && !this.mPm.isUserRestricted(currentUserId4, "no_debugging_features")) {
                                            restrictedByPolicy = false;
                                            if (!installedForCurrentUser && restrictedByPolicy) {
                                                ps.setInstalled(false, currentUserId4);
                                            }
                                            ps.setInstalled(true, currentUserId4);
                                            ps.setEnabled(0, currentUserId4, installerPackageName);
                                        }
                                        restrictedByPolicy = true;
                                        if (!installedForCurrentUser) {
                                            ps.setInstalled(false, currentUserId4);
                                        }
                                        ps.setInstalled(true, currentUserId4);
                                        ps.setEnabled(0, currentUserId4, installerPackageName);
                                    }
                                }
                            }
                            this.mPm.mSettings.addInstallerPackageNames(ps.getInstallSource());
                            r13 = packageManagerTracedLock2;
                            int installReason4 = installReason;
                            InstallArgs installArgs4 = installArgs;
                            try {
                                PackageManagerService.sPmsExt.updatePackageSettings(userId, pkgName2, pkg, ps, allUsers, installerPackageName);
                                Set<Integer> previousUserIds = new ArraySet<>();
                                if (res.mRemovedInfo != null) {
                                    try {
                                        if (res.mRemovedInfo.mInstallReasons != null) {
                                            int installReasonCount = res.mRemovedInfo.mInstallReasons.size();
                                            for (int i5 = 0; i5 < installReasonCount; i5++) {
                                                int previousUserId = res.mRemovedInfo.mInstallReasons.keyAt(i5);
                                                int previousInstallReason = res.mRemovedInfo.mInstallReasons.valueAt(i5).intValue();
                                                ps.setInstallReason(previousInstallReason, previousUserId);
                                                previousUserIds.add(Integer.valueOf(previousUserId));
                                            }
                                        }
                                    } catch (Throwable th9) {
                                        th = th9;
                                        throw th;
                                    }
                                }
                                if (res.mRemovedInfo != null && res.mRemovedInfo.mUninstallReasons != null) {
                                    for (int i6 = 0; i6 < res.mRemovedInfo.mUninstallReasons.size(); i6++) {
                                        int previousUserId2 = res.mRemovedInfo.mUninstallReasons.keyAt(i6);
                                        int previousReason = res.mRemovedInfo.mUninstallReasons.valueAt(i6).intValue();
                                        ps.setUninstallReason(previousReason, previousUserId2);
                                    }
                                }
                                int[] allUsersList = this.mPm.mUserManager.getUserIds();
                                if (userId == -1) {
                                    for (int currentUserId5 : allUsersList) {
                                        if (!previousUserIds.contains(Integer.valueOf(currentUserId5)) && ps.getInstalled(currentUserId5)) {
                                            ps.setInstallReason(installReason4, currentUserId5);
                                        }
                                    }
                                } else if (!previousUserIds.contains(Integer.valueOf(userId))) {
                                    ps.setInstallReason(installReason4, userId);
                                }
                                String codePath = ps.getPathString();
                                if (IncrementalManager.isIncrementalPath(codePath) && (incrementalManager = this.mIncrementalManager) != null) {
                                    incrementalManager.registerLoadingProgressCallback(codePath, new IncrementalProgressListener(ps.getPackageName(), this.mPm));
                                }
                                for (int currentUserId6 : allUsersList) {
                                    if (ps.getInstalled(currentUserId6)) {
                                        ps.setUninstallReason(0, currentUserId6);
                                    }
                                }
                                this.mPm.mSettings.writeKernelMappingLPr(ps);
                                PermissionManagerServiceInternal.PackageInstalledParams.Builder permissionParamsBuilder = new PermissionManagerServiceInternal.PackageInstalledParams.Builder();
                                boolean grantPermissions = (installArgs4.mInstallFlags & 256) != 0;
                                if (grantPermissions) {
                                    List<String> grantedPermissions = installArgs4.mInstallGrantPermissions != null ? Arrays.asList(installArgs4.mInstallGrantPermissions) : pkg.getRequestedPermissions();
                                    permissionParamsBuilder.setGrantedPermissions(grantedPermissions);
                                }
                                boolean allowlistAllRestrictedPermissions = (installArgs4.mInstallFlags & 4194304) != 0;
                                List<String> allowlistedRestrictedPermissions = allowlistAllRestrictedPermissions ? pkg.getRequestedPermissions() : installArgs4.mAllowlistedRestrictedPermissions;
                                if (allowlistedRestrictedPermissions != null) {
                                    permissionParamsBuilder.setAllowlistedRestrictedPermissions(allowlistedRestrictedPermissions);
                                }
                                int autoRevokePermissionsMode = installArgs4.mAutoRevokePermissionsMode;
                                permissionParamsBuilder.setAutoRevokePermissionsMode(autoRevokePermissionsMode);
                                ScanResult scanResult = reconciledPkg.mScanResult;
                                androidPackage = pkg;
                                try {
                                    this.mPm.mPermissionManager.onPackageInstalled(androidPackage, scanResult.mPreviousAppId, permissionParamsBuilder.build(), userId);
                                    if (installArgs4.mPackageSource != 3) {
                                        try {
                                            if (installArgs4.mPackageSource != 4) {
                                                pkgName = pkgName2;
                                                packageManagerTracedLock = r13;
                                            }
                                        } catch (Throwable th10) {
                                            th = th10;
                                            throw th;
                                        }
                                    }
                                    pkgName = pkgName2;
                                    enableRestrictedSettings(pkgName, pkg.getUid());
                                    packageManagerTracedLock = r13;
                                } catch (Throwable th11) {
                                    th = th11;
                                    throw th;
                                }
                            } catch (Throwable th12) {
                                th = th12;
                            }
                        } catch (Throwable th13) {
                            th = th13;
                            r13 = packageManagerTracedLock2;
                        }
                    } else {
                        packageManagerTracedLock = packageManagerTracedLock2;
                        androidPackage = pkg;
                        pkgName = pkgName2;
                    }
                    res.mName = pkgName;
                    res.mUid = pkg.getUid();
                    res.mPkg = androidPackage;
                    res.setReturnCode(1);
                    Trace.traceBegin(262144L, "writeSettings");
                    this.mPm.writeSettingsLPrTEMP();
                    Trace.traceEnd(262144L);
                    Trace.traceEnd(262144L);
                } catch (Throwable th14) {
                    th = th14;
                }
            } catch (Throwable th15) {
                th = th15;
                r13 = packageManagerTracedLock2;
            }
        }
    }

    private void enableRestrictedSettings(String pkgName, int appId) {
        AppOpsManager appOpsManager = (AppOpsManager) this.mPm.mContext.getSystemService(AppOpsManager.class);
        int[] allUsersList = this.mPm.mUserManager.getUserIds();
        for (int userId : allUsersList) {
            int uid = UserHandle.getUid(userId, appId);
            appOpsManager.setMode(119, uid, pkgName, 2);
        }
    }

    private void executePostCommitSteps(CommitRequest commitRequest) {
        Iterator<ReconciledPackage> it;
        String packageName;
        PackageSetting realPkgSetting;
        ArraySet<IncrementalStorage> incrementalStorages = new ArraySet<>();
        Iterator<ReconciledPackage> it2 = commitRequest.mReconciledPackages.values().iterator();
        while (it2.hasNext()) {
            ReconciledPackage reconciledPkg = it2.next();
            boolean instantApp = (reconciledPkg.mScanResult.mRequest.mScanFlags & 8192) != 0;
            AndroidPackage pkg = reconciledPkg.mPkgSetting.getPkg();
            String packageName2 = pkg.getPackageName();
            String codePath = pkg.getPath();
            boolean onIncremental = this.mIncrementalManager != null && IncrementalManager.isIncrementalPath(codePath);
            if (onIncremental) {
                IncrementalStorage storage = this.mIncrementalManager.openStorage(codePath);
                if (storage == null) {
                    throw new IllegalArgumentException("Install: null storage for incremental package " + packageName2);
                }
                incrementalStorages.add(storage);
            }
            this.mAppDataHelper.prepareAppDataPostCommitLIF(pkg, 0);
            if (reconciledPkg.mPrepareResult.mClearCodeCache) {
                this.mAppDataHelper.clearAppDataLIF(pkg, -1, 39);
            }
            if (reconciledPkg.mPrepareResult.mReplace) {
                this.mDexManager.notifyPackageUpdated(pkg.getPackageName(), pkg.getBaseApkPath(), pkg.getSplitCodePaths());
            }
            this.mArtManagerService.prepareAppProfiles(pkg, this.mPm.resolveUserIds(reconciledPkg.mInstallArgs.mUser.getIdentifier()), true);
            int compilationReason = this.mDexManager.getCompilationReasonForInstallScenario(reconciledPkg.mInstallArgs.mInstallScenario);
            boolean isBackupOrRestore = reconciledPkg.mInstallArgs.mInstallReason == 2 || reconciledPkg.mInstallArgs.mInstallReason == 3;
            int dexoptFlags = (isBackupOrRestore ? 2048 : 0) | UsbTerminalTypes.TERMINAL_BIDIR_SKRPHONE_SUPRESS;
            DexoptOptions dexoptOptions = new DexoptOptions(packageName2, compilationReason, dexoptFlags);
            boolean performDexopt = ((instantApp && Settings.Global.getInt(this.mContext.getContentResolver(), "instant_app_dexopt_enabled", 0) == 0) || pkg.isDebuggable() || onIncremental || !dexoptOptions.isCompilationEnabled()) ? false : true;
            if (!performDexopt) {
                it = it2;
                packageName = packageName2;
            } else {
                if (SystemProperties.getBoolean("pm.precompile_layouts", false)) {
                    Trace.traceBegin(262144L, "compileLayouts");
                    this.mViewCompiler.compileLayouts(pkg);
                    Trace.traceEnd(262144L);
                }
                Trace.traceBegin(262144L, "dexopt");
                ScanResult result = reconciledPkg.mScanResult;
                PackageSetting realPkgSetting2 = result.mExistingSettingCopied ? result.mRequest.mPkgSetting : result.mPkgSetting;
                if (realPkgSetting2 != null) {
                    realPkgSetting = realPkgSetting2;
                } else {
                    realPkgSetting = reconciledPkg.mPkgSetting;
                }
                boolean isUpdatedSystemApp = reconciledPkg.mPkgSetting.getPkgState().isUpdatedSystemApp();
                realPkgSetting.getPkgState().setUpdatedSystemApp(isUpdatedSystemApp);
                it = it2;
                packageName = packageName2;
                this.mPackageDexOptimizer.performDexOpt(pkg, realPkgSetting, null, this.mPm.getOrCreateCompilerPackageStats(pkg), this.mDexManager.getPackageUseInfoOrDefault(packageName2), dexoptOptions);
                Trace.traceEnd(262144L);
            }
            BackgroundDexOptService.getService().notifyPackageChanged(packageName);
            notifyPackageChangeObserversOnUpdate(reconciledPkg);
            it2 = it;
        }
        PackageManagerServiceUtils.waitForNativeBinariesExtractionForIncremental(incrementalStorages);
    }

    private void notifyPackageChangeObserversOnUpdate(ReconciledPackage reconciledPkg) {
        PackageSetting pkgSetting = reconciledPkg.mPkgSetting;
        PackageInstalledInfo pkgInstalledInfo = reconciledPkg.mInstallResult;
        PackageRemovedInfo pkgRemovedInfo = pkgInstalledInfo.mRemovedInfo;
        PackageChangeEvent pkgChangeEvent = new PackageChangeEvent();
        pkgChangeEvent.packageName = pkgSetting.getPkg().getPackageName();
        pkgChangeEvent.version = pkgSetting.getVersionCode();
        pkgChangeEvent.lastUpdateTimeMillis = pkgSetting.getLastUpdateTime();
        boolean z = true;
        pkgChangeEvent.newInstalled = pkgRemovedInfo == null || !pkgRemovedInfo.mIsUpdate;
        if (pkgRemovedInfo == null || !pkgRemovedInfo.mDataRemoved) {
            z = false;
        }
        pkgChangeEvent.dataRemoved = z;
        pkgChangeEvent.isDeleted = false;
        this.mPm.notifyPackageChangeObservers(pkgChangeEvent);
    }

    public int installLocationPolicy(PackageInfoLite pkgLite, int installFlags) {
        String packageName = pkgLite.packageName;
        int installLocation = pkgLite.installLocation;
        synchronized (this.mPm.mLock) {
            AndroidPackage installedPkg = this.mPm.mPackages.get(packageName);
            if (installedPkg != null) {
                if ((installFlags & 2) != 0) {
                    if (installedPkg.isSystem()) {
                        return 1;
                    }
                    if (installLocation == 1) {
                        return 1;
                    }
                    if (installLocation != 2) {
                        return installedPkg.isExternalStorage() ? 2 : 1;
                    }
                } else {
                    return -4;
                }
            }
            return pkgLite.recommendedInstallLocation;
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public Pair<Integer, String> verifyReplacingVersionCode(PackageInfoLite pkgLite, long requiredInstalledVersionCode, int installFlags) {
        if ((131072 & installFlags) != 0) {
            return verifyReplacingVersionCodeForApex(pkgLite, requiredInstalledVersionCode, installFlags);
        }
        String packageName = pkgLite.packageName;
        synchronized (this.mPm.mLock) {
            AndroidPackage dataOwnerPkg = this.mPm.mPackages.get(packageName);
            PackageSetting dataOwnerPs = this.mPm.mSettings.getPackageLPr(packageName);
            if (dataOwnerPkg == null && dataOwnerPs != null) {
                dataOwnerPkg = dataOwnerPs.getPkg();
            }
            if (requiredInstalledVersionCode != -1) {
                if (dataOwnerPkg == null) {
                    String errorMsg = "Required installed version code was " + requiredInstalledVersionCode + " but package is not installed";
                    Slog.w("PackageManager", errorMsg);
                    return Pair.create(-121, errorMsg);
                } else if (dataOwnerPkg.getLongVersionCode() != requiredInstalledVersionCode) {
                    String errorMsg2 = "Required installed version code was " + requiredInstalledVersionCode + " but actual installed version is " + dataOwnerPkg.getLongVersionCode();
                    Slog.w("PackageManager", errorMsg2);
                    return Pair.create(-121, errorMsg2);
                }
            }
            if (dataOwnerPkg != null && !dataOwnerPkg.isSdkLibrary()) {
                if (!PackageManagerServiceUtils.isDowngradePermitted(installFlags, dataOwnerPkg.isDebuggable())) {
                    try {
                        PackageManagerServiceUtils.checkDowngrade(dataOwnerPkg, pkgLite);
                    } catch (PackageManagerException e) {
                        String errorMsg3 = "Downgrade detected: " + e.getMessage();
                        Slog.w("PackageManager", errorMsg3);
                        return Pair.create(-25, errorMsg3);
                    }
                } else if (dataOwnerPs.isSystem()) {
                    PackageSetting disabledPs = this.mPm.mSettings.getDisabledSystemPkgLPr(dataOwnerPs);
                    if (disabledPs != null) {
                        dataOwnerPkg = disabledPs.getPkg();
                    }
                    if (!Build.IS_DEBUGGABLE && !dataOwnerPkg.isDebuggable()) {
                        try {
                            PackageManagerServiceUtils.checkDowngrade(dataOwnerPkg, pkgLite);
                        } catch (PackageManagerException e2) {
                            String errorMsg4 = "System app: " + packageName + " cannot be downgraded to older than its preloaded version on the system image. " + e2.getMessage();
                            Slog.w("PackageManager", errorMsg4);
                            return Pair.create(-25, errorMsg4);
                        }
                    }
                }
            }
            return Pair.create(1, null);
        }
    }

    private Pair<Integer, String> verifyReplacingVersionCodeForApex(PackageInfoLite pkgLite, long requiredInstalledVersionCode, int installFlags) {
        String packageName = pkgLite.packageName;
        PackageInfo activePackage = this.mApexManager.getPackageInfo(packageName, 1);
        if (activePackage == null) {
            String errorMsg = "Attempting to install new APEX package " + packageName;
            Slog.w("PackageManager", errorMsg);
            return Pair.create(-23, errorMsg);
        }
        long activeVersion = activePackage.getLongVersionCode();
        if (requiredInstalledVersionCode != -1 && activeVersion != requiredInstalledVersionCode) {
            String errorMsg2 = "Installed version of APEX package " + packageName + " does not match required. Active version: " + activeVersion + " required: " + requiredInstalledVersionCode;
            Slog.w("PackageManager", errorMsg2);
            return Pair.create(-121, errorMsg2);
        }
        boolean isAppDebuggable = (activePackage.applicationInfo.flags & 2) != 0;
        long newVersionCode = pkgLite.getLongVersionCode();
        if (!PackageManagerServiceUtils.isDowngradePermitted(installFlags, isAppDebuggable) && newVersionCode < activeVersion) {
            String errorMsg3 = "Downgrade of APEX package " + packageName + " is not allowed. Active version: " + activeVersion + " attempted: " + newVersionCode;
            Slog.w("PackageManager", errorMsg3);
            return Pair.create(-25, errorMsg3);
        }
        return Pair.create(1, null);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public int getUidForVerifier(VerifierInfo verifierInfo) {
        synchronized (this.mPm.mLock) {
            AndroidPackage pkg = this.mPm.mPackages.get(verifierInfo.packageName);
            if (pkg == null) {
                return -1;
            }
            if (pkg.getSigningDetails().getSignatures().length != 1) {
                Slog.i("PackageManager", "Verifier package " + verifierInfo.packageName + " has more than one signature; ignoring");
                return -1;
            }
            try {
                Signature verifierSig = pkg.getSigningDetails().getSignatures()[0];
                PublicKey publicKey = verifierSig.getPublicKey();
                byte[] expectedPublicKey = publicKey.getEncoded();
                byte[] actualPublicKey = verifierInfo.publicKey.getEncoded();
                if (!Arrays.equals(actualPublicKey, expectedPublicKey)) {
                    Slog.i("PackageManager", "Verifier package " + verifierInfo.packageName + " does not have the expected public key; ignoring");
                    return -1;
                }
                return pkg.getUid();
            } catch (CertificateException e) {
                return -1;
            }
        }
    }

    public void sendPendingBroadcasts() {
        int i;
        int numBroadcasts = 0;
        synchronized (this.mPm.mLock) {
            SparseArray<ArrayMap<String, ArrayList<String>>> userIdToPackagesToComponents = this.mPm.mPendingBroadcasts.copiedMap();
            int numUsers = userIdToPackagesToComponents.size();
            for (int n = 0; n < numUsers; n++) {
                numBroadcasts += userIdToPackagesToComponents.valueAt(n).size();
            }
            if (numBroadcasts == 0) {
                return;
            }
            String[] packages = new String[numBroadcasts];
            ArrayList<String>[] components = new ArrayList[numBroadcasts];
            int[] uids = new int[numBroadcasts];
            int i2 = 0;
            for (int n2 = 0; n2 < numUsers; n2++) {
                int packageUserId = userIdToPackagesToComponents.keyAt(n2);
                ArrayMap<String, ArrayList<String>> componentsToBroadcast = userIdToPackagesToComponents.valueAt(n2);
                int numComponents = CollectionUtils.size(componentsToBroadcast);
                for (int index = 0; index < numComponents; index++) {
                    packages[i2] = componentsToBroadcast.keyAt(index);
                    components[i2] = componentsToBroadcast.valueAt(index);
                    PackageSetting ps = this.mPm.mSettings.getPackageLPr(packages[i2]);
                    if (ps != null) {
                        i = UserHandle.getUid(packageUserId, ps.getAppId());
                    } else {
                        i = -1;
                    }
                    uids[i2] = i;
                    i2++;
                }
            }
            int numBroadcasts2 = i2;
            this.mPm.mPendingBroadcasts.clear();
            Computer snapshot = this.mPm.snapshotComputer();
            for (int i3 = 0; i3 < numBroadcasts2; i3++) {
                this.mPm.sendPackageChangedBroadcast(snapshot, packages[i3], true, components[i3], uids[i3], null);
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* JADX WARN: Removed duplicated region for block: B:124:0x02b4  */
    /* JADX WARN: Removed duplicated region for block: B:127:0x02d7  */
    /* JADX WARN: Removed duplicated region for block: B:129:0x02f6  */
    /* JADX WARN: Removed duplicated region for block: B:135:0x037b  */
    /* JADX WARN: Removed duplicated region for block: B:145:0x03c5  */
    /* JADX WARN: Removed duplicated region for block: B:153:0x0441  */
    /*
        Code decompiled incorrectly, please refer to instructions dump.
    */
    public void handlePackagePostInstall(PackageInstalledInfo res, InstallArgs installArgs, boolean launchedForRestore) {
        PackageStateInternal packageStateInternal;
        boolean killApp;
        boolean update;
        IPackageInstallObserver2 installObserver;
        String str;
        boolean allNewUsers;
        String installerPackageName;
        String installerPackage;
        int[] firstUserIds;
        Computer snapshot;
        SparseArray<int[]> newBroadcastAllowList;
        String installerPackageName2;
        boolean notifyVerifier;
        int[] iArr;
        PackageStateInternal pkgSetting;
        boolean update2;
        boolean killApp2 = (installArgs.mInstallFlags & 4096) == 0;
        boolean virtualPreload = (installArgs.mInstallFlags & 65536) != 0;
        String installerPackage2 = installArgs.mInstallSource.installerPackageName;
        IPackageInstallObserver2 installObserver2 = installArgs.mObserver;
        int dataLoaderType = installArgs.mDataLoaderType;
        boolean succeeded = res.mReturnCode == 1;
        boolean update3 = (res.mRemovedInfo == null || res.mRemovedInfo.mRemovedPackage == null) ? false : true;
        String packageName = res.mName;
        if (succeeded) {
            packageStateInternal = this.mPm.snapshotComputer().getPackageStateInternal(packageName);
        } else {
            packageStateInternal = null;
        }
        PackageStateInternal pkgSetting2 = packageStateInternal;
        boolean removedBeforeUpdate = pkgSetting2 == null || (pkgSetting2.isSystem() && !pkgSetting2.getPath().getPath().equals(res.mPkg.getPath()));
        if (succeeded && removedBeforeUpdate) {
            Slog.e("PackageManager", packageName + " was removed before handlePackagePostInstall could be executed");
            res.mReturnCode = -23;
            res.mReturnMsg = "Package was removed before install could complete.";
            InstallArgs args = res.mRemovedInfo != null ? res.mRemovedInfo.mArgs : null;
            if (args != null) {
                synchronized (this.mPm.mInstallLock) {
                    args.doPostDeleteLI(true);
                }
            }
            this.mPm.notifyInstallObserver(res, installObserver2);
            return;
        }
        if (succeeded) {
            this.mPm.mPerUidReadTimeoutsCache = null;
            if (res.mRemovedInfo != null) {
                if (res.mRemovedInfo.mIsExternal) {
                    if (PackageManagerService.DEBUG_INSTALL) {
                        Slog.i("PackageManager", "upgrading pkg " + res.mRemovedInfo.mRemovedPackage + " is ASEC-hosted -> UNAVAILABLE");
                    }
                    int[] uidArray = {res.mRemovedInfo.mUid};
                    ArrayList<String> pkgList = new ArrayList<>(1);
                    pkgList.add(res.mRemovedInfo.mRemovedPackage);
                    this.mBroadcastHelper.sendResourcesChangedBroadcast(false, true, pkgList, uidArray, (IIntentReceiver) null);
                }
                res.mRemovedInfo.sendPackageRemovedBroadcasts(killApp2, false);
            }
            if (res.mInstallerPackageName != null) {
                str = res.mInstallerPackageName;
            } else if (res.mRemovedInfo != null) {
                str = res.mRemovedInfo.mInstallerPackageName;
            } else {
                str = null;
            }
            String installerPackageName3 = str;
            grantCtaRuntimePerm(update3, res);
            this.mPm.notifyInstantAppPackageInstalled(res.mPkg.getPackageName(), res.mNewUsers);
            int[] firstUserIds2 = PackageManagerService.EMPTY_INT_ARRAY;
            int[] firstInstantUserIds = PackageManagerService.EMPTY_INT_ARRAY;
            int[] updateUserIds = PackageManagerService.EMPTY_INT_ARRAY;
            int[] instantUserIds = PackageManagerService.EMPTY_INT_ARRAY;
            boolean allNewUsers2 = res.mOrigUsers == null || res.mOrigUsers.length == 0;
            int[] iArr2 = res.mNewUsers;
            int length = iArr2.length;
            killApp = killApp2;
            int[] updateUserIds2 = updateUserIds;
            int[] instantUserIds2 = instantUserIds;
            int[] firstUserIds3 = firstUserIds2;
            int i = 0;
            while (i < length) {
                int i2 = length;
                int newUser = iArr2[i];
                boolean isInstantApp = pkgSetting2.getUserStateOrDefault(newUser).isInstantApp();
                if (allNewUsers2) {
                    if (isInstantApp) {
                        firstInstantUserIds = ArrayUtils.appendInt(firstInstantUserIds, newUser);
                        iArr = iArr2;
                        pkgSetting = pkgSetting2;
                        update2 = update3;
                    } else {
                        firstUserIds3 = ArrayUtils.appendInt(firstUserIds3, newUser);
                        iArr = iArr2;
                        pkgSetting = pkgSetting2;
                        update2 = update3;
                    }
                } else {
                    boolean isNew = true;
                    iArr = iArr2;
                    int[] iArr3 = res.mOrigUsers;
                    pkgSetting = pkgSetting2;
                    int length2 = iArr3.length;
                    update2 = update3;
                    int i3 = 0;
                    while (true) {
                        if (i3 >= length2) {
                            break;
                        }
                        int i4 = length2;
                        int origUser = iArr3[i3];
                        if (origUser != newUser) {
                            i3++;
                            length2 = i4;
                        } else {
                            isNew = false;
                            break;
                        }
                    }
                    if (isNew) {
                        if (isInstantApp) {
                            firstInstantUserIds = ArrayUtils.appendInt(firstInstantUserIds, newUser);
                        } else {
                            int[] firstInstantUserIds2 = ArrayUtils.appendInt(firstUserIds3, newUser);
                            firstUserIds3 = firstInstantUserIds2;
                        }
                    } else if (isInstantApp) {
                        instantUserIds2 = ArrayUtils.appendInt(instantUserIds2, newUser);
                    } else {
                        updateUserIds2 = ArrayUtils.appendInt(updateUserIds2, newUser);
                    }
                }
                i++;
                length = i2;
                iArr2 = iArr;
                pkgSetting2 = pkgSetting;
                update3 = update2;
            }
            boolean update4 = update3;
            if (res.mPkg.getStaticSharedLibName() == null) {
                this.mPm.mProcessLoggingHandler.invalidateBaseApkHash(res.mPkg.getBaseApkPath());
                int appId = UserHandle.getAppId(res.mUid);
                boolean isSystem = res.mPkg.isSystem();
                PackageManagerService packageManagerService = this.mPm;
                int[] firstInstantUserIds3 = firstInstantUserIds;
                int[] firstUserIds4 = firstUserIds3;
                allNewUsers = allNewUsers2;
                installerPackageName = packageName;
                update = update4;
                installObserver = installObserver2;
                installerPackage = installerPackage2;
                packageManagerService.sendPackageAddedForNewUsers(packageManagerService.snapshotComputer(), packageName, isSystem || virtualPreload, virtualPreload, appId, firstUserIds4, firstInstantUserIds3, dataLoaderType);
                Bundle extras = new Bundle();
                extras.putInt("android.intent.extra.UID", res.mUid);
                if (update) {
                    extras.putBoolean("android.intent.extra.REPLACING", true);
                }
                extras.putInt("android.content.pm.extra.DATA_LOADER_TYPE", dataLoaderType);
                synchronized (this.mPm.mLock) {
                    try {
                        Computer snapshot2 = this.mPm.snapshotComputer();
                        newBroadcastAllowList = this.mPm.mAppsFilter.getVisibilityAllowList(snapshot2, snapshot2.getPackageStateInternal(installerPackageName, 1000), updateUserIds2, this.mPm.mSettings.getPackagesLocked());
                    } catch (Throwable th) {
                        th = th;
                        while (true) {
                            try {
                                break;
                            } catch (Throwable th2) {
                                th = th2;
                            }
                        }
                        throw th;
                    }
                }
                this.mPm.sendPackageBroadcast("android.intent.action.PACKAGE_ADDED", installerPackageName, extras, 0, null, null, updateUserIds2, instantUserIds2, newBroadcastAllowList, null);
                PowerHalManager powerHalManager = this.mPowerHalManager;
                if (powerHalManager != null) {
                    powerHalManager.setInstallationBoost(false);
                }
                if (installerPackageName3 != null) {
                    this.mPm.sendPackageBroadcast("android.intent.action.PACKAGE_ADDED", installerPackageName, extras, 0, installerPackageName3, null, updateUserIds2, instantUserIds2, null, null);
                }
                if (this.mPm.mRequiredVerifierPackage == null) {
                    installerPackageName2 = installerPackageName3;
                } else {
                    installerPackageName2 = installerPackageName3;
                    if (!this.mPm.mRequiredVerifierPackage.equals(installerPackageName2)) {
                        notifyVerifier = true;
                        if (notifyVerifier) {
                            PackageManagerService packageManagerService2 = this.mPm;
                            packageManagerService2.sendPackageBroadcast("android.intent.action.PACKAGE_ADDED", installerPackageName, extras, 0, packageManagerService2.mRequiredVerifierPackage, null, updateUserIds2, instantUserIds2, null, null);
                        }
                        if (this.mPm.mRequiredInstallerPackage != null) {
                            PackageManagerService packageManagerService3 = this.mPm;
                            packageManagerService3.sendPackageBroadcast("android.intent.action.PACKAGE_ADDED", installerPackageName, extras, 16777216, packageManagerService3.mRequiredInstallerPackage, null, firstUserIds4, instantUserIds2, null, null);
                        }
                        if (!update) {
                            this.mPm.sendPackageBroadcast("android.intent.action.PACKAGE_REPLACED", installerPackageName, extras, 0, null, null, updateUserIds2, instantUserIds2, res.mRemovedInfo.mBroadcastAllowList, null);
                            if (installerPackageName2 != null) {
                                this.mPm.sendPackageBroadcast("android.intent.action.PACKAGE_REPLACED", installerPackageName, extras, 0, installerPackageName2, null, updateUserIds2, instantUserIds2, null, null);
                            }
                            if (notifyVerifier) {
                                PackageManagerService packageManagerService4 = this.mPm;
                                packageManagerService4.sendPackageBroadcast("android.intent.action.PACKAGE_REPLACED", installerPackageName, extras, 0, packageManagerService4.mRequiredVerifierPackage, null, updateUserIds2, instantUserIds2, null, null);
                            }
                            this.mPm.sendPackageBroadcast("android.intent.action.MY_PACKAGE_REPLACED", null, null, 0, installerPackageName, null, updateUserIds2, instantUserIds2, null, this.mBroadcastHelper.getTemporaryAppAllowlistBroadcastOptions(FrameworkStatsLog.APP_BACKGROUND_RESTRICTIONS_INFO__EXEMPTION_REASON__REASON_PACKAGE_REPLACED).toBundle());
                            firstUserIds = firstUserIds4;
                        } else if (!launchedForRestore || res.mPkg.isSystem()) {
                            firstUserIds = firstUserIds4;
                        } else {
                            if (PackageManagerService.DEBUG_BACKUP) {
                                Slog.i("PackageManager", "Post-restore of " + installerPackageName + " sending FIRST_LAUNCH in " + Arrays.toString(firstUserIds4));
                            }
                            firstUserIds = firstUserIds4;
                            this.mBroadcastHelper.sendFirstLaunchBroadcast(installerPackageName, installerPackage, firstUserIds, firstInstantUserIds3);
                        }
                        if (res.mPkg.isExternalStorage()) {
                            if (!update) {
                                StorageManager storageManager = (StorageManager) this.mInjector.getSystemService(StorageManager.class);
                                VolumeInfo volume = storageManager.findVolumeByUuid(StorageManager.convert(res.mPkg.getVolumeUuid()).toString());
                                int packageExternalStorageType = PackageManagerServiceUtils.getPackageExternalStorageType(volume, res.mPkg.isExternalStorage());
                                if (packageExternalStorageType != 0) {
                                    FrameworkStatsLog.write(181, packageExternalStorageType, installerPackageName);
                                }
                            }
                            if (PackageManagerService.DEBUG_INSTALL) {
                                Slog.i("PackageManager", "upgrading pkg " + res.mPkg + " is external");
                            }
                            int[] uidArray2 = {res.mPkg.getUid()};
                            ArrayList<String> pkgList2 = new ArrayList<>(1);
                            pkgList2.add(installerPackageName);
                            this.mBroadcastHelper.sendResourcesChangedBroadcast(true, true, pkgList2, uidArray2, (IIntentReceiver) null);
                        }
                    }
                }
                notifyVerifier = false;
                if (notifyVerifier) {
                }
                if (this.mPm.mRequiredInstallerPackage != null) {
                }
                if (!update) {
                }
                if (res.mPkg.isExternalStorage()) {
                }
            } else {
                allNewUsers = allNewUsers2;
                installerPackageName = packageName;
                installObserver = installObserver2;
                installerPackage = installerPackage2;
                update = update4;
                firstUserIds = firstUserIds3;
                if (!ArrayUtils.isEmpty(res.mLibraryConsumers)) {
                    Computer snapshot3 = this.mPm.snapshotComputer();
                    boolean dontKillApp = (update || res.mPkg.getStaticSharedLibName() == null) ? false : true;
                    for (int i5 = 0; i5 < res.mLibraryConsumers.size(); i5++) {
                        AndroidPackage pkg = res.mLibraryConsumers.get(i5);
                        this.mPm.sendPackageChangedBroadcast(snapshot3, pkg.getPackageName(), dontKillApp, new ArrayList<>(Collections.singletonList(pkg.getPackageName())), pkg.getUid(), null);
                    }
                }
            }
            if (firstUserIds.length > 0) {
                for (int userId : firstUserIds) {
                    this.mPm.restorePermissionsAndUpdateRolesForNewUserInstall(installerPackageName, userId);
                }
            }
            if (allNewUsers && !update) {
                this.mPm.notifyPackageAdded(installerPackageName, res.mUid);
            } else {
                this.mPm.notifyPackageChanged(installerPackageName, res.mUid);
            }
            EventLog.writeEvent((int) EventLogTags.UNKNOWN_SOURCES_ENABLED, getUnknownSourcesSettings());
            InstallArgs args2 = res.mRemovedInfo != null ? res.mRemovedInfo.mArgs : null;
            if (args2 != null) {
                if (!killApp) {
                    this.mPm.scheduleDeferredNoKillPostDelete(args2);
                } else {
                    synchronized (this.mPm.mInstallLock) {
                        args2.doPostDeleteLI(true);
                    }
                }
            } else {
                VMRuntime.getRuntime().requestConcurrentGC();
            }
            Computer snapshot4 = this.mPm.snapshotComputer();
            int length3 = firstUserIds.length;
            int i6 = 0;
            while (i6 < length3) {
                int userId2 = firstUserIds[i6];
                String installerPackage3 = installerPackage;
                int[] instantUserIds3 = instantUserIds2;
                PackageInfo info = snapshot4.getPackageInfo(installerPackageName, 0L, userId2);
                if (info == null) {
                    snapshot = snapshot4;
                } else {
                    this.mDexManager.notifyPackageInstalled(info, userId2);
                    snapshot = snapshot4;
                    PackageManagerService.sPmsExt.onPackageAdded(installerPackageName, null, userId2);
                }
                i6++;
                instantUserIds2 = instantUserIds3;
                installerPackage = installerPackage3;
                snapshot4 = snapshot;
            }
        } else {
            killApp = killApp2;
            update = update3;
            installObserver = installObserver2;
        }
        boolean deferInstallObserver = succeeded && update;
        if (deferInstallObserver) {
            if (killApp) {
                this.mPm.scheduleDeferredPendingKillInstallObserver(res, installObserver);
            } else {
                this.mPm.scheduleDeferredNoKillInstallObserver(res, installObserver);
            }
        } else {
            this.mPm.notifyInstallObserver(res, installObserver);
        }
        this.mPm.schedulePruneUnusedStaticSharedLibraries(true);
        if (installArgs.mTraceMethod != null) {
            Trace.asyncTraceEnd(262144L, installArgs.mTraceMethod, installArgs.mTraceCookie);
        }
    }

    private int getUnknownSourcesSettings() {
        return Settings.Secure.getIntForUser(this.mContext.getContentResolver(), "install_non_market_apps", -1, 0);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void installSystemStubPackages(List<String> systemStubPackageNames, int scanFlags) {
        int i = systemStubPackageNames.size();
        while (true) {
            i--;
            if (i < 0) {
                break;
            }
            String packageName = systemStubPackageNames.get(i);
            if (this.mPm.mSettings.isDisabledSystemPackageLPr(packageName)) {
                systemStubPackageNames.remove(i);
            } else {
                AndroidPackage pkg = this.mPm.mPackages.get(packageName);
                if (pkg == null) {
                    systemStubPackageNames.remove(i);
                } else {
                    PackageSetting ps = this.mPm.mSettings.getPackageLPr(packageName);
                    if (ps != null) {
                        int enabledState = ps.getEnabled(0);
                        if (enabledState == 3) {
                            systemStubPackageNames.remove(i);
                        }
                    }
                    try {
                        installStubPackageLI(pkg, 0, scanFlags);
                        ps.setEnabled(0, 0, PackageManagerService.PLATFORM_PACKAGE_NAME);
                        systemStubPackageNames.remove(i);
                    } catch (PackageManagerException e) {
                        Slog.e("PackageManager", "Failed to parse uncompressed system package: " + e.getMessage());
                    }
                }
            }
        }
        int i2 = systemStubPackageNames.size();
        for (int i3 = i2 - 1; i3 >= 0; i3--) {
            String pkgName = systemStubPackageNames.get(i3);
            this.mPm.mSettings.getPackageLPr(pkgName).setEnabled(2, 0, PackageManagerService.PLATFORM_PACKAGE_NAME);
            PackageManagerServiceUtils.logCriticalInfo(6, "Stub disabled; pkg: " + pkgName);
        }
    }

    /* JADX DEBUG: Another duplicated slice has different insns count: {[IF]}, finally: {[IF, INVOKE, MOVE_EXCEPTION, INVOKE, INVOKE, MOVE_EXCEPTION] complete} */
    /* JADX DEBUG: Don't trust debug lines info. Repeating lines: [3164=4, 3165=6, 3172=5] */
    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean enableCompressedPackage(AndroidPackage stubPkg, PackageSetting stubPkgSetting) {
        PackageFreezer freezer;
        int parseFlags = this.mPm.getDefParseFlags() | Integer.MIN_VALUE | 64;
        synchronized (this.mPm.mInstallLock) {
            try {
                freezer = this.mPm.freezePackage(stubPkg.getPackageName(), "setEnabledSetting");
                try {
                    AndroidPackage pkg = installStubPackageLI(stubPkg, parseFlags, 0);
                    this.mAppDataHelper.prepareAppDataAfterInstallLIF(pkg);
                    synchronized (this.mPm.mLock) {
                        try {
                            this.mSharedLibraries.updateSharedLibrariesLPw(pkg, stubPkgSetting, null, null, Collections.unmodifiableMap(this.mPm.mPackages));
                        } catch (PackageManagerException e) {
                            Slog.w("PackageManager", "updateAllSharedLibrariesLPw failed: ", e);
                        }
                        this.mPm.mPermissionManager.onPackageInstalled(pkg, -1, PermissionManagerServiceInternal.PackageInstalledParams.DEFAULT, -1);
                        this.mPm.writeSettingsLPrTEMP();
                    }
                    if (freezer != null) {
                        freezer.close();
                    }
                    this.mAppDataHelper.clearAppDataLIF(pkg, -1, 39);
                    this.mDexManager.notifyPackageUpdated(pkg.getPackageName(), pkg.getBaseApkPath(), pkg.getSplitCodePaths());
                } finally {
                }
            } catch (PackageManagerException e2) {
                try {
                    freezer = this.mPm.freezePackage(stubPkg.getPackageName(), "setEnabledSetting");
                } catch (PackageManagerException pme) {
                    Slog.wtf("PackageManager", "Failed to restore system package:" + stubPkg.getPackageName(), pme);
                    synchronized (this.mPm.mLock) {
                        try {
                            PackageSetting stubPs = this.mPm.mSettings.getPackageLPr(stubPkg.getPackageName());
                            if (stubPs != null) {
                                stubPs.setEnabled(2, 0, PackageManagerService.PLATFORM_PACKAGE_NAME);
                            }
                            this.mPm.writeSettingsLPrTEMP();
                            return false;
                        } catch (Throwable th) {
                            th = th;
                            throw th;
                        }
                    }
                }
                try {
                    synchronized (this.mPm.mLock) {
                        this.mPm.mSettings.enableSystemPackageLPw(stubPkg.getPackageName());
                        installPackageFromSystemLIF(stubPkg.getPath(), this.mPm.mUserManager.getUserIds(), null, true);
                        if (freezer != null) {
                            freezer.close();
                        }
                        synchronized (this.mPm.mLock) {
                            try {
                                PackageSetting stubPs2 = this.mPm.mSettings.getPackageLPr(stubPkg.getPackageName());
                                if (stubPs2 != null) {
                                    stubPs2.setEnabled(2, 0, PackageManagerService.PLATFORM_PACKAGE_NAME);
                                }
                                this.mPm.writeSettingsLPrTEMP();
                                return false;
                            } catch (Throwable th2) {
                                th = th2;
                                throw th;
                            }
                        }
                    }
                } finally {
                }
            }
        }
        return true;
    }

    private AndroidPackage installStubPackageLI(AndroidPackage stubPkg, int parseFlags, int scanFlags) throws PackageManagerException {
        if (PackageManagerService.DEBUG_COMPRESSION) {
            Slog.i("PackageManager", "Uncompressing system stub; pkg: " + stubPkg.getPackageName());
        }
        File scanFile = decompressPackage(stubPkg.getPackageName(), stubPkg.getPath());
        if (scanFile == null) {
            throw new PackageManagerException("Unable to decompress stub at " + stubPkg.getPath());
        }
        synchronized (this.mPm.mLock) {
            this.mPm.mSettings.disableSystemPackageLPw(stubPkg.getPackageName(), true);
        }
        RemovePackageHelper removePackageHelper = new RemovePackageHelper(this.mPm);
        removePackageHelper.removePackageLI(stubPkg, true);
        try {
            return scanSystemPackageTracedLI(scanFile, parseFlags, scanFlags, null);
        } catch (PackageManagerException e) {
            Slog.w("PackageManager", "Failed to install compressed system package:" + stubPkg.getPackageName(), e);
            removePackageHelper.removeCodePathLI(scanFile);
            throw e;
        }
    }

    private File decompressPackage(String packageName, String codePath) {
        if (!PackageManagerServiceUtils.compressedFileExists(codePath)) {
            if (PackageManagerService.DEBUG_COMPRESSION) {
                Slog.i("PackageManager", "No files to decompress at: " + codePath);
            }
            return null;
        }
        File dstCodePath = PackageManagerServiceUtils.getNextCodePath(Environment.getDataAppDirectory(null), packageName);
        int ret = PackageManagerServiceUtils.decompressFiles(codePath, dstCodePath, packageName);
        if (ret == 1) {
            ret = PackageManagerServiceUtils.extractNativeBinaries(dstCodePath, packageName);
        }
        if (ret == 1) {
            if (!this.mPm.isSystemReady()) {
                if (this.mPm.mReleaseOnSystemReady == null) {
                    this.mPm.mReleaseOnSystemReady = new ArrayList();
                }
                this.mPm.mReleaseOnSystemReady.add(dstCodePath);
            } else {
                ContentResolver resolver = this.mContext.getContentResolver();
                F2fsUtils.releaseCompressedBlocks(resolver, dstCodePath);
            }
            return dstCodePath;
        } else if (dstCodePath.exists()) {
            new RemovePackageHelper(this.mPm).removeCodePathLI(dstCodePath);
            return null;
        } else {
            return null;
        }
    }

    /* JADX DEBUG: Finally have unexpected throw blocks count: 2, expect 1 */
    public void restoreDisabledSystemPackageLIF(DeletePackageAction action, int[] allUserHandles, boolean writeSettings) throws SystemDeleteException {
        PackageSetting deletedPs = action.mDeletingPs;
        PackageRemovedInfo outInfo = action.mRemovedInfo;
        PackageSetting disabledPs = action.mDisabledPs;
        synchronized (this.mPm.mLock) {
            this.mPm.mSettings.enableSystemPackageLPw(disabledPs.getPkg().getPackageName());
            PackageManagerServiceUtils.removeNativeBinariesLI(deletedPs);
        }
        if (PackageManagerService.DEBUG_REMOVE) {
            Slog.d("PackageManager", "Re-installing system package: " + disabledPs);
        }
        try {
            try {
                synchronized (this.mPm.mInstallLock) {
                    int[] origUsers = outInfo == null ? null : outInfo.mOrigUsers;
                    installPackageFromSystemLIF(disabledPs.getPathString(), allUserHandles, origUsers, writeSettings);
                }
                if (disabledPs.getPkg().isStub()) {
                    synchronized (this.mPm.mLock) {
                        disableStubPackage(action, deletedPs, allUserHandles);
                    }
                }
            } catch (PackageManagerException e) {
                Slog.w("PackageManager", "Failed to restore system package:" + deletedPs.getPackageName() + ": " + e.getMessage());
                throw new SystemDeleteException(e);
            }
        } catch (Throwable th) {
            if (disabledPs.getPkg().isStub()) {
                synchronized (this.mPm.mLock) {
                    disableStubPackage(action, deletedPs, allUserHandles);
                }
            }
            throw th;
        }
    }

    private void disableStubPackage(DeletePackageAction action, PackageSetting deletedPs, int[] allUserHandles) {
        PackageSetting stubPs = this.mPm.mSettings.getPackageLPr(deletedPs.getPackageName());
        if (stubPs != null) {
            int userId = action.mUser == null ? -1 : action.mUser.getIdentifier();
            if (userId == -1) {
                for (int aUserId : allUserHandles) {
                    stubPs.setEnabled(2, aUserId, PackageManagerService.PLATFORM_PACKAGE_NAME);
                }
            } else if (userId >= 0) {
                stubPs.setEnabled(2, userId, PackageManagerService.PLATFORM_PACKAGE_NAME);
            }
        }
    }

    private void installPackageFromSystemLIF(String codePathString, int[] allUserHandles, int[] origUserHandles, boolean writeSettings) throws PackageManagerException {
        File codePath = new File(codePathString);
        int parseFlags = this.mPm.getDefParseFlags() | 1 | 16;
        int scanFlags = this.mPm.getSystemPackageScanFlags(codePath);
        AndroidPackage pkg = scanSystemPackageTracedLI(codePath, parseFlags, scanFlags, null);
        PackageSetting pkgSetting = this.mPm.mSettings.getPackageLPr(pkg.getPackageName());
        try {
            this.mSharedLibraries.updateSharedLibrariesLPw(pkg, pkgSetting, null, null, Collections.unmodifiableMap(this.mPm.mPackages));
        } catch (PackageManagerException e) {
            Slog.e("PackageManager", "updateAllSharedLibrariesLPw failed: " + e.getMessage());
        }
        this.mAppDataHelper.prepareAppDataAfterInstallLIF(pkg);
        setPackageInstalledForSystemPackage(pkg, allUserHandles, origUserHandles, writeSettings);
    }

    private void setPackageInstalledForSystemPackage(AndroidPackage pkg, int[] allUserHandles, int[] origUserHandles, boolean writeSettings) {
        synchronized (this.mPm.mLock) {
            try {
                try {
                    PackageSetting ps = this.mPm.mSettings.getPackageLPr(pkg.getPackageName());
                    boolean applyUserRestrictions = origUserHandles != null;
                    if (applyUserRestrictions) {
                        boolean installedStateChanged = false;
                        if (PackageManagerService.DEBUG_REMOVE) {
                            Slog.d("PackageManager", "Propagating install state across reinstall");
                        }
                        for (int userId : allUserHandles) {
                            boolean installed = ArrayUtils.contains(origUserHandles, userId);
                            if (PackageManagerService.DEBUG_REMOVE) {
                                Slog.d("PackageManager", "    user " + userId + " => " + installed);
                            }
                            if (installed != ps.getInstalled(userId)) {
                                installedStateChanged = true;
                            }
                            ps.setInstalled(installed, userId);
                            if (installed) {
                                ps.setUninstallReason(0, userId);
                            }
                        }
                        this.mPm.mSettings.writeAllUsersPackageRestrictionsLPr();
                        if (installedStateChanged) {
                            this.mPm.mSettings.writeKernelMappingLPr(ps);
                        }
                    }
                    this.mPm.mPermissionManager.onPackageInstalled(pkg, -1, PermissionManagerServiceInternal.PackageInstalledParams.DEFAULT, -1);
                    for (int userId2 : allUserHandles) {
                        if (applyUserRestrictions) {
                            this.mPm.mSettings.writePermissionStateForUserLPr(userId2, false);
                        }
                    }
                    if (writeSettings) {
                        this.mPm.writeSettingsLPrTEMP();
                    }
                } catch (Throwable th) {
                    th = th;
                    throw th;
                }
            } catch (Throwable th2) {
                th = th2;
            }
        }
    }

    public void prepareSystemPackageCleanUp(WatchedArrayMap<String, PackageSetting> packageSettings, List<String> possiblyDeletedUpdatedSystemApps, ArrayMap<String, File> expectingBetter, int[] userIds) {
        for (int index = packageSettings.size() - 1; index >= 0; index--) {
            PackageSetting ps = packageSettings.valueAt(index);
            String packageName = ps.getPackageName();
            if (ps.isSystem() || (PackageManagerService.sPmsExt.isRemovableSysApp(ps.getPackageName()) && ITranPackageManagerService.Instance().isPreloadPath(ps.getPathString()))) {
                AndroidPackage scannedPkg = this.mPm.mPackages.get(packageName);
                PackageSetting disabledPs = this.mPm.mSettings.getDisabledSystemPkgLPr(packageName);
                if (scannedPkg != null) {
                    if (disabledPs != null) {
                        PackageManagerServiceUtils.logCriticalInfo(5, "Expecting better updated system app for " + packageName + "; removing system app.  Last known codePath=" + ps.getPathString() + ", versionCode=" + ps.getVersionCode() + "; scanned versionCode=" + scannedPkg.getLongVersionCode());
                        this.mRemovePackageHelper.removePackageLI(scannedPkg, true);
                        expectingBetter.put(ps.getPackageName(), ps.getPath());
                    }
                } else if (disabledPs == null) {
                    PackageManagerServiceUtils.logCriticalInfo(5, "System package " + packageName + " no longer exists; its data will be wiped");
                    this.mRemovePackageHelper.removePackageDataLIF(ps, userIds, null, 0, false);
                } else if (disabledPs.getPath() == null || !disabledPs.getPath().exists() || disabledPs.getPkg() == null) {
                    possiblyDeletedUpdatedSystemApps.add(packageName);
                } else {
                    expectingBetter.put(disabledPs.getPackageName(), disabledPs.getPath());
                }
            }
        }
    }

    public void cleanupDisabledPackageSettings(List<String> possiblyDeletedUpdatedSystemApps, int[] userIds, int scanFlags) {
        String msg;
        for (int i = possiblyDeletedUpdatedSystemApps.size() - 1; i >= 0; i--) {
            String packageName = possiblyDeletedUpdatedSystemApps.get(i);
            AndroidPackage pkg = this.mPm.mPackages.get(packageName);
            this.mPm.mSettings.removeDisabledSystemPackageLPw(packageName);
            if (pkg == null) {
                msg = "Updated system package " + packageName + " no longer exists; removing its data";
            } else {
                msg = "Updated system package " + packageName + " no longer exists; rescanning package on data";
                this.mRemovePackageHelper.removePackageLI(pkg, true);
                try {
                    File codePath = new File(pkg.getPath());
                    scanSystemPackageTracedLI(codePath, 0, scanFlags, null);
                } catch (PackageManagerException e) {
                    Slog.e("PackageManager", "Failed to parse updated, ex-system package: " + e.getMessage());
                }
            }
            PackageSetting ps = this.mPm.mSettings.getPackageLPr(packageName);
            if (ps != null && this.mPm.mPackages.get(packageName) == null) {
                this.mRemovePackageHelper.removePackageDataLIF(ps, userIds, null, 0, false);
            }
            PackageManagerServiceUtils.logCriticalInfo(5, msg);
        }
    }

    public void installPackagesFromDir(File scanDir, List<File> frameworkSplits, int parseFlags, int scanFlags, PackageParser2 packageParser, ExecutorService executorService) {
        File[] files;
        int i = parseFlags;
        File[] files2 = scanDir.listFiles();
        if (ArrayUtils.isEmpty(files2)) {
            Log.d("PackageManager", "No files in app dir " + scanDir);
            return;
        }
        PackageManagerService.sMtkSystemServerIns.addBootEvent("Android:PMS_scan_data:" + scanDir.getPath().toString());
        if (PackageManagerService.DEBUG_PACKAGE_SCANNING) {
            Log.d("PackageManager", "Scanning app dir " + scanDir + " scanFlags=" + scanFlags + " flags=0x" + Integer.toHexString(parseFlags));
        }
        ParallelPackageParser parallelPackageParser = new ParallelPackageParser(packageParser, executorService, frameworkSplits);
        int fileCount = 0;
        int length = files2.length;
        int i2 = 0;
        while (i2 < length) {
            File file = files2[i2];
            boolean isPackage = (ApkLiteParseUtils.isApkFile(file) || file.isDirectory()) && !PackageInstallerService.isStageName(file.getName());
            if (!isPackage) {
                files = files2;
            } else {
                if ((scanFlags & 16777216) == 0) {
                    files = files2;
                } else {
                    PackageCacher cacher = new PackageCacher(this.mPm.getCacheDir());
                    files = files2;
                    Log.w("PackageManager", "Dropping cache of " + file.getAbsolutePath());
                    cacher.cleanCachedResult(file);
                }
                parallelPackageParser.submit(file, i);
                fileCount++;
            }
            i2++;
            files2 = files;
        }
        int fileCount2 = fileCount;
        while (fileCount2 > 0) {
            ParallelPackageParser.ParseResult parseResult = parallelPackageParser.take();
            Throwable throwable = parseResult.throwable;
            int errorCode = 1;
            String errorMsg = null;
            if (throwable == null) {
                if (parseResult.parsedPackage.isStaticSharedLibrary()) {
                    PackageManagerService.renameStaticSharedLibraryPackage(parseResult.parsedPackage);
                }
                try {
                    addForInitLI(parseResult.parsedPackage, i, scanFlags, null);
                } catch (PackageManagerException e) {
                    errorCode = e.error;
                    errorMsg = "Failed to scan " + parseResult.scanFile + ": " + e.getMessage();
                    Slog.w("PackageManager", errorMsg);
                }
            } else if (throwable instanceof PackageManagerException) {
                PackageManagerException e2 = (PackageManagerException) throwable;
                errorCode = e2.error;
                errorMsg = "Failed to parse " + parseResult.scanFile + ": " + e2.getMessage();
                Slog.w("PackageManager", errorMsg);
            } else {
                throw new IllegalStateException("Unexpected exception occurred while parsing " + parseResult.scanFile, throwable);
            }
            if ((8388608 & scanFlags) != 0 && errorCode != 1) {
                this.mApexManager.reportErrorWithApkInApex(scanDir.getAbsolutePath(), errorMsg);
            }
            if ((65536 & scanFlags) == 0 && errorCode != 1) {
                PackageManagerServiceUtils.logCriticalInfo(5, "Deleting invalid package at " + parseResult.scanFile);
                this.mRemovePackageHelper.removeCodePathLI(parseResult.scanFile);
            }
            fileCount2--;
            i = parseFlags;
        }
    }

    public void checkExistingBetterPackages(ArrayMap<String, File> expectingBetterPackages, List<String> stubSystemApps, int systemScanFlags, int systemParseFlags) {
        for (int i = 0; i < expectingBetterPackages.size(); i++) {
            String packageName = expectingBetterPackages.keyAt(i);
            if (!this.mPm.mPackages.containsKey(packageName)) {
                File scanFile = expectingBetterPackages.valueAt(i);
                PackageManagerServiceUtils.logCriticalInfo(5, "Expected better " + packageName + " but never showed up; reverting to system");
                Pair<Integer, Integer> rescanAndReparseFlags = this.mPm.getSystemPackageRescanFlagsAndReparseFlags(scanFile, systemScanFlags, systemParseFlags);
                int rescanFlags = ((Integer) rescanAndReparseFlags.first).intValue();
                int reparseFlags = ((Integer) rescanAndReparseFlags.second).intValue();
                if (rescanFlags == 0) {
                    Slog.e("PackageManager", "Ignoring unexpected fallback path " + scanFile);
                } else {
                    this.mPm.mSettings.enableSystemPackageLPw(packageName);
                    try {
                        AndroidPackage newPkg = scanSystemPackageTracedLI(scanFile, reparseFlags, rescanFlags, null);
                        if (newPkg.isStub()) {
                            stubSystemApps.add(packageName);
                        }
                    } catch (PackageManagerException e) {
                        Slog.e("PackageManager", "Failed to parse original system package: " + e.getMessage());
                    }
                }
            }
        }
    }

    public AndroidPackage scanSystemPackageTracedLI(File scanFile, int parseFlags, int scanFlags, UserHandle user) throws PackageManagerException {
        Trace.traceBegin(262144L, "scanPackage [" + scanFile.toString() + "]");
        try {
            return scanSystemPackageLI(scanFile, parseFlags, scanFlags, user);
        } finally {
            Trace.traceEnd(262144L);
        }
    }

    private AndroidPackage scanSystemPackageLI(File scanFile, int parseFlags, int scanFlags, UserHandle user) throws PackageManagerException {
        if (PackageManagerService.DEBUG_INSTALL) {
            Slog.d("PackageManager", "Parsing: " + scanFile);
        }
        Trace.traceBegin(262144L, "parsePackage");
        try {
            PackageParser2 pp = this.mPm.mInjector.getScanningPackageParser();
            ParsedPackage parsedPackage = pp.parsePackage(scanFile, parseFlags, false);
            if (pp != null) {
                pp.close();
            }
            Trace.traceEnd(262144L);
            if (parsedPackage.isStaticSharedLibrary()) {
                PackageManagerService.renameStaticSharedLibraryPackage(parsedPackage);
            }
            return addForInitLI(parsedPackage, parseFlags, scanFlags, user);
        } catch (Throwable th) {
            Trace.traceEnd(262144L);
            throw th;
        }
    }

    private AndroidPackage addForInitLI(ParsedPackage parsedPackage, int parseFlags, int scanFlags, UserHandle user) throws PackageManagerException {
        Pair<ScanResult, Boolean> scanResultPair = scanSystemPackageLI(parsedPackage, parseFlags, scanFlags, user);
        if (scanResultPair == null) {
            return null;
        }
        ScanResult scanResult = (ScanResult) scanResultPair.first;
        boolean shouldHideSystemApp = ((Boolean) scanResultPair.second).booleanValue();
        if (scanResult.mSuccess) {
            synchronized (this.mPm.mLock) {
                boolean appIdCreated = false;
                try {
                    try {
                        String pkgName = scanResult.mPkgSetting.getPackageName();
                        ReconcileRequest reconcileRequest = new ReconcileRequest(Collections.singletonMap(pkgName, scanResult), this.mPm.mPackages, Collections.singletonMap(pkgName, this.mPm.getSettingsVersionForPackage(parsedPackage)));
                        Map<String, ReconciledPackage> reconcileResult = ReconcilePackageUtils.reconcilePackages(reconcileRequest, this.mSharedLibraries, this.mPm.mSettings.getKeySetManagerService(), this.mPm.mSettings);
                        appIdCreated = optimisticallyRegisterAppId(scanResult);
                        commitReconciledScanResultLocked(reconcileResult.get(pkgName), this.mPm.mUserManager.getUserIds());
                    } catch (PackageManagerException e) {
                        if (appIdCreated) {
                            cleanUpAppIdCreation(scanResult);
                        }
                        throw e;
                    }
                } finally {
                }
            }
        }
        if (shouldHideSystemApp) {
            synchronized (this.mPm.mLock) {
                this.mPm.mSettings.disableSystemPackageLPw(parsedPackage.getPackageName(), true);
            }
        }
        if (this.mIncrementalManager != null && IncrementalManager.isIncrementalPath(parsedPackage.getPath()) && scanResult.mPkgSetting != null && scanResult.mPkgSetting.isLoading()) {
            this.mIncrementalManager.registerLoadingProgressCallback(parsedPackage.getPath(), new IncrementalProgressListener(parsedPackage.getPackageName(), this.mPm));
        }
        return scanResult.mPkgSetting.getPkg();
    }

    private boolean optimisticallyRegisterAppId(ScanResult result) throws PackageManagerException {
        boolean registerAppIdLPw;
        if (!result.mExistingSettingCopied || result.needsNewAppId()) {
            synchronized (this.mPm.mLock) {
                registerAppIdLPw = this.mPm.mSettings.registerAppIdLPw(result.mPkgSetting, result.needsNewAppId());
            }
            return registerAppIdLPw;
        }
        return false;
    }

    private void cleanUpAppIdCreation(ScanResult result) {
        if (result.mPkgSetting.getAppId() > 0) {
            this.mPm.mSettings.removeAppIdLPw(result.mPkgSetting.getAppId());
        }
    }

    private ScanResult scanPackageTracedLI(ParsedPackage parsedPackage, int parseFlags, int scanFlags, long currentTime, UserHandle user, String cpuAbiOverride) throws PackageManagerException {
        Trace.traceBegin(262144L, "scanPackage");
        try {
            return scanPackageNewLI(parsedPackage, parseFlags, scanFlags, currentTime, user, cpuAbiOverride);
        } finally {
            Trace.traceEnd(262144L);
        }
    }

    /* JADX WARN: Unsupported multi-entry loop pattern (BACK_EDGE: B:45:0x012b -> B:46:0x012c). Please submit an issue!!! */
    private ScanRequest prepareInitialScanRequest(ParsedPackage parsedPackage, int parseFlags, int scanFlags, UserHandle user, String cpuAbiOverride) throws PackageManagerException {
        SharedUserSetting sharedUserSetting;
        SharedUserSetting oldSharedUserSetting;
        synchronized (this.mPm.mLock) {
            try {
                AndroidPackage platformPackage = this.mPm.getPlatformPackage();
                String renamedPkgName = this.mPm.mSettings.getRenamedPackageLPr(AndroidPackageUtils.getRealPackageOrNull(parsedPackage));
                String realPkgName = ScanPackageUtils.getRealPackageName(parsedPackage, renamedPkgName);
                if (realPkgName != null) {
                    ScanPackageUtils.ensurePackageRenamed(parsedPackage, renamedPkgName);
                }
                PackageSetting originalPkgSetting = getOriginalPackageLocked(parsedPackage, renamedPkgName);
                PackageSetting installedPkgSetting = this.mPm.mSettings.getPackageLPr(parsedPackage.getPackageName());
                if (this.mPm.mTransferredPackages.contains(parsedPackage.getPackageName())) {
                    Slog.w("PackageManager", "Package " + parsedPackage.getPackageName() + " was transferred to another, but its .apk remains");
                }
                PackageSetting disabledPkgSetting = this.mPm.mSettings.getDisabledSystemPkgLPr(parsedPackage.getPackageName());
                boolean ignoreSharedUserId = false;
                if (installedPkgSetting == null || !installedPkgSetting.hasSharedUser()) {
                    ignoreSharedUserId = parsedPackage.isLeavingSharedUid();
                }
                if (!ignoreSharedUserId && parsedPackage.getSharedUserId() != null) {
                    sharedUserSetting = this.mPm.mSettings.getSharedUserLPw(parsedPackage.getSharedUserId(), 0, 0, true);
                } else {
                    sharedUserSetting = null;
                }
                if (PackageManagerService.DEBUG_PACKAGE_SCANNING && (parseFlags & Integer.MIN_VALUE) != 0 && sharedUserSetting != null) {
                    Log.d("PackageManager", "Shared UserID " + parsedPackage.getSharedUserId() + " (uid=" + sharedUserSetting.mAppId + "): packages=" + sharedUserSetting.getPackageStates());
                }
                if (installedPkgSetting == null) {
                    oldSharedUserSetting = null;
                } else {
                    SharedUserSetting oldSharedUserSetting2 = this.mPm.mSettings.getSharedUserSettingLPr(installedPkgSetting);
                    oldSharedUserSetting = oldSharedUserSetting2;
                }
                try {
                    boolean isPlatformPackage = platformPackage != null && platformPackage.getPackageName().equals(parsedPackage.getPackageName());
                    return new ScanRequest(parsedPackage, oldSharedUserSetting, installedPkgSetting == null ? null : installedPkgSetting.getPkg(), installedPkgSetting, sharedUserSetting, disabledPkgSetting, originalPkgSetting, realPkgName, parseFlags, scanFlags, isPlatformPackage, user, cpuAbiOverride);
                } catch (Throwable th) {
                    th = th;
                    throw th;
                }
            } catch (Throwable th2) {
                th = th2;
            }
        }
    }

    private ScanResult scanPackageNewLI(ParsedPackage parsedPackage, int parseFlags, int scanFlags, long currentTime, UserHandle user, String cpuAbiOverride) throws PackageManagerException {
        boolean isUpdatedSystemApp;
        ScanRequest initialScanRequest = prepareInitialScanRequest(parsedPackage, parseFlags, scanFlags, user, cpuAbiOverride);
        PackageSetting installedPkgSetting = initialScanRequest.mPkgSetting;
        PackageSetting disabledPkgSetting = initialScanRequest.mDisabledPkgSetting;
        if (installedPkgSetting != null) {
            isUpdatedSystemApp = installedPkgSetting.getPkgState().isUpdatedSystemApp();
        } else {
            isUpdatedSystemApp = disabledPkgSetting != null;
        }
        int newScanFlags = adjustScanFlags(scanFlags, installedPkgSetting, disabledPkgSetting, user, parsedPackage);
        ScanPackageUtils.applyPolicy(parsedPackage, newScanFlags, this.mPm.getPlatformPackage(), isUpdatedSystemApp);
        synchronized (this.mPm.mLock) {
            try {
                try {
                    assertPackageIsValid(parsedPackage, parseFlags, newScanFlags);
                } catch (Throwable th) {
                    th = th;
                }
                try {
                    ScanRequest request = new ScanRequest(parsedPackage, initialScanRequest.mOldSharedUserSetting, initialScanRequest.mOldPkg, installedPkgSetting, initialScanRequest.mSharedUserSetting, disabledPkgSetting, initialScanRequest.mOriginalPkgSetting, initialScanRequest.mRealPkgName, parseFlags, scanFlags, initialScanRequest.mIsPlatformPackage, user, cpuAbiOverride);
                    return ScanPackageUtils.scanPackageOnlyLI(request, this.mPm.mInjector, this.mPm.mFactoryTest, currentTime);
                } catch (Throwable th2) {
                    th = th2;
                    throw th;
                }
            } catch (Throwable th3) {
                th = th3;
            }
        }
    }

    private Pair<ScanResult, Boolean> scanSystemPackageLI(ParsedPackage parsedPackage, int parseFlags, int scanFlags, UserHandle user) throws PackageManagerException {
        boolean scanedIsExisted;
        boolean isRemovableApp = PackageManagerService.sPmsExt.isRemovableSysApp(parsedPackage.getPackageName());
        boolean z = true;
        boolean scanSystemPartition = (parseFlags & 16) != 0 || (isRemovableApp && !parsedPackage.getPath().startsWith("/data/") && ITranPackageManagerService.Instance().isPreloadPath(parsedPackage.getPath()));
        ScanRequest initialScanRequest = prepareInitialScanRequest(parsedPackage, parseFlags, scanFlags, user, null);
        PackageSetting installedPkgSetting = initialScanRequest.mPkgSetting;
        PackageSetting originalPkgSetting = initialScanRequest.mOriginalPkgSetting;
        PackageSetting pkgSetting = originalPkgSetting == null ? installedPkgSetting : originalPkgSetting;
        boolean pkgAlreadyExists = pkgSetting != null;
        String disabledPkgName = pkgAlreadyExists ? pkgSetting.getPackageName() : parsedPackage.getPackageName();
        synchronized (this.mPm.mLock) {
            try {
                boolean isUpgrade = this.mPm.isDeviceUpgrading();
                if (scanSystemPartition && !pkgAlreadyExists) {
                    try {
                        if (this.mPm.mSettings.getDisabledSystemPkgLPr(disabledPkgName) != null) {
                            Slog.w("PackageManager", "Inconsistent package setting of updated system app for " + disabledPkgName + ". To recover it, enable the system app and install it as non-updated system app.");
                            this.mPm.mSettings.removeDisabledSystemPackageLPw(disabledPkgName);
                        }
                    } catch (Throwable th) {
                        th = th;
                        while (true) {
                            try {
                                break;
                            } catch (Throwable th2) {
                                th = th2;
                            }
                        }
                        throw th;
                    }
                }
                PackageSetting disabledPkgSetting = this.mPm.mSettings.getDisabledSystemPkgLPr(disabledPkgName);
                boolean isSystemPkgUpdated = disabledPkgSetting != null;
                if (PackageManagerService.DEBUG_INSTALL && isSystemPkgUpdated) {
                    Slog.d("PackageManager", "updatedPkg = " + disabledPkgSetting);
                }
                if (parsedPackage != null && ITranPackageManagerService.Instance().needSkipRemovedPackage(parsedPackage.getPackageName(), parsedPackage.getPath())) {
                    return null;
                }
                if (PackageManagerService.sPmsExt.needSkipScanning(parsedPackage, disabledPkgSetting, pkgSetting)) {
                    return null;
                }
                if (scanSystemPartition && isSystemPkgUpdated) {
                    ScanRequest request = new ScanRequest(parsedPackage, this.mPm.mSettings.getSharedUserSettingLPr(disabledPkgSetting), null, disabledPkgSetting, initialScanRequest.mSharedUserSetting, null, null, null, parseFlags, scanFlags, initialScanRequest.mIsPlatformPackage, user, null);
                    z = true;
                    ScanPackageUtils.applyPolicy(parsedPackage, scanFlags, this.mPm.getPlatformPackage(), true);
                    ScanResult scanResult = ScanPackageUtils.scanPackageOnlyLI(request, this.mPm.mInjector, this.mPm.mFactoryTest, -1L);
                    if (scanResult.mExistingSettingCopied && scanResult.mRequest.mPkgSetting != null) {
                        scanResult.mRequest.mPkgSetting.updateFrom(scanResult.mPkgSetting);
                    }
                }
                boolean newPkgChangedPaths = (!pkgAlreadyExists || pkgSetting.getPathString().equals(parsedPackage.getPath())) ? false : z;
                boolean newPkgVersionGreater = (!pkgAlreadyExists || parsedPackage.getLongVersionCode() <= pkgSetting.getVersionCode()) ? false : z;
                boolean isSystemPkgBetter = (scanSystemPartition && isSystemPkgUpdated && newPkgChangedPaths && newPkgVersionGreater) ? z : false;
                if (isSystemPkgBetter) {
                    synchronized (this.mPm.mLock) {
                        this.mPm.mPackages.remove(pkgSetting.getPackageName());
                    }
                    PackageManagerServiceUtils.logCriticalInfo(5, "System package updated; name: " + pkgSetting.getPackageName() + "; " + pkgSetting.getVersionCode() + " --> " + parsedPackage.getLongVersionCode() + "; " + pkgSetting.getPathString() + " --> " + parsedPackage.getPath());
                    InstallArgs args = new FileInstallArgs(pkgSetting.getPathString(), InstructionSets.getAppDexInstructionSets(pkgSetting.getPrimaryCpuAbi(), pkgSetting.getSecondaryCpuAbi()), this.mPm);
                    args.cleanUpResourcesLI();
                    synchronized (this.mPm.mLock) {
                        this.mPm.mSettings.enableSystemPackageLPw(pkgSetting.getPackageName());
                    }
                }
                if (scanSystemPartition && isSystemPkgUpdated && !isSystemPkgBetter) {
                    parsedPackage.hideAsFinal();
                    throw new PackageManagerException(5, "Package " + parsedPackage.getPackageName() + " at " + parsedPackage.getPath() + " ignored: updated version " + (pkgAlreadyExists ? String.valueOf(pkgSetting.getVersionCode()) : UiModeManagerService.Shell.NIGHT_MODE_STR_UNKNOWN) + " better than this " + parsedPackage.getLongVersionCode());
                }
                boolean forceCollect = scanSystemPartition ? isUpgrade : PackageManagerServiceUtils.isApkVerificationForced(pkgSetting);
                if (PackageManagerService.DEBUG_VERIFY && forceCollect) {
                    Slog.d("PackageManager", "Force collect certificate of " + parsedPackage.getPackageName());
                }
                boolean skipVerify = (scanSystemPartition || (forceCollect && canSkipForcedPackageVerification(parsedPackage))) ? z : false;
                boolean z2 = z;
                ScanPackageUtils.collectCertificatesLI(pkgSetting, parsedPackage, this.mPm.getSettingsVersionForPackage(parsedPackage), forceCollect, skipVerify, this.mPm.isPreNMR1Upgrade());
                maybeClearProfilesForUpgradesLI(pkgSetting, parsedPackage);
                if (scanSystemPartition && !isSystemPkgUpdated && pkgAlreadyExists && !pkgSetting.isSystem()) {
                    if (!parsedPackage.getSigningDetails().checkCapability(pkgSetting.getSigningDetails(), z2 ? 1 : 0) && !pkgSetting.getSigningDetails().checkCapability(parsedPackage.getSigningDetails(), 8)) {
                        PackageManagerServiceUtils.logCriticalInfo(5, "System package signature mismatch; name: " + pkgSetting.getPackageName());
                        PackageFreezer freezer = this.mPm.freezePackage(parsedPackage.getPackageName(), "scanPackageInternalLI");
                        try {
                            DeletePackageHelper deletePackageHelper = new DeletePackageHelper(this.mPm);
                            deletePackageHelper.deletePackageLIF(parsedPackage.getPackageName(), null, true, this.mPm.mUserManager.getUserIds(), 0, null, false);
                            if (freezer != null) {
                                freezer.close();
                            }
                        } catch (Throwable th3) {
                            if (freezer != null) {
                                try {
                                    freezer.close();
                                } catch (Throwable th4) {
                                    th3.addSuppressed(th4);
                                }
                            }
                            throw th3;
                        }
                    } else if (newPkgVersionGreater) {
                        PackageManagerServiceUtils.logCriticalInfo(5, "System package enabled; name: " + pkgSetting.getPackageName() + "; " + pkgSetting.getVersionCode() + " --> " + parsedPackage.getLongVersionCode() + "; " + pkgSetting.getPathString() + " --> " + parsedPackage.getPath());
                        InstallArgs args2 = new FileInstallArgs(pkgSetting.getPathString(), InstructionSets.getAppDexInstructionSets(pkgSetting.getPrimaryCpuAbi(), pkgSetting.getSecondaryCpuAbi()), this.mPm);
                        synchronized (this.mPm.mInstallLock) {
                            args2.cleanUpResourcesLI();
                        }
                    } else {
                        boolean scanedIsExisted2 = false;
                        if (isRemovableApp && ITranPackageManagerService.Instance().isPreloadPath(parsedPackage.getPath()) && parsedPackage.getPath().equals(pkgSetting.getPathString()) && parsedPackage.getLongVersionCode() == pkgSetting.getVersionCode()) {
                            scanedIsExisted2 = true;
                            Slog.d("PackageManager", "scaned is existed, should not be hidden, pkg:" + parsedPackage.getPackageName() + ", codePath:" + parsedPackage.getPath());
                        }
                        if (!scanedIsExisted2) {
                            PackageManagerServiceUtils.logCriticalInfo(4, "System package disabled; name: " + pkgSetting.getPackageName() + "; old: " + pkgSetting.getPathString() + " @ " + pkgSetting.getVersionCode() + "; new: " + parsedPackage.getPath() + " @ " + parsedPackage.getPath());
                            scanedIsExisted = true;
                            return new Pair<>(scanPackageNewLI(parsedPackage, parseFlags, scanFlags | 2, 0L, user, null), Boolean.valueOf(scanedIsExisted));
                        }
                    }
                }
                scanedIsExisted = false;
                return new Pair<>(scanPackageNewLI(parsedPackage, parseFlags, scanFlags | 2, 0L, user, null), Boolean.valueOf(scanedIsExisted));
            } catch (Throwable th5) {
                th = th5;
            }
        }
    }

    private boolean canSkipForcedPackageVerification(AndroidPackage pkg) {
        pkg.getPackageName();
        if (VerityUtils.hasFsverity(pkg.getBaseApkPath())) {
            String[] splitCodePaths = pkg.getSplitCodePaths();
            if (!ArrayUtils.isEmpty(splitCodePaths)) {
                for (String str : splitCodePaths) {
                    if (!VerityUtils.hasFsverity(str)) {
                        return false;
                    }
                }
                return true;
            }
            return true;
        }
        return false;
    }

    private void maybeClearProfilesForUpgradesLI(PackageSetting originalPkgSetting, AndroidPackage pkg) {
        if (originalPkgSetting == null || !this.mPm.isDeviceUpgrading() || originalPkgSetting.getVersionCode() == pkg.getLongVersionCode()) {
            return;
        }
        this.mAppDataHelper.clearAppProfilesLIF(pkg);
        if (PackageManagerService.DEBUG_INSTALL) {
            Slog.d("PackageManager", originalPkgSetting.getPackageName() + " clear profile due to version change " + originalPkgSetting.getVersionCode() + " != " + pkg.getLongVersionCode());
        }
    }

    private PackageSetting getOriginalPackageLocked(AndroidPackage pkg, String renamedPkgName) {
        if (ScanPackageUtils.isPackageRenamed(pkg, renamedPkgName)) {
            return null;
        }
        for (int i = ArrayUtils.size(pkg.getOriginalPackages()) - 1; i >= 0; i--) {
            PackageSetting originalPs = this.mPm.mSettings.getPackageLPr(pkg.getOriginalPackages().get(i));
            if (originalPs != null && verifyPackageUpdateLPr(originalPs, pkg)) {
                if (this.mPm.mSettings.getSharedUserSettingLPr(originalPs) != null) {
                    String sharedUserSettingsName = this.mPm.mSettings.getSharedUserSettingLPr(originalPs).name;
                    if (!sharedUserSettingsName.equals(pkg.getSharedUserId())) {
                        Slog.w("PackageManager", "Unable to migrate data from " + originalPs.getPackageName() + " to " + pkg.getPackageName() + ": old shared user settings name " + sharedUserSettingsName + " differs from " + pkg.getSharedUserId());
                    }
                } else if (PackageManagerService.DEBUG_UPGRADE) {
                    Log.v("PackageManager", "Renaming new package " + pkg.getPackageName() + " to old name " + originalPs.getPackageName());
                }
                return originalPs;
            }
        }
        return null;
    }

    private boolean verifyPackageUpdateLPr(PackageSetting oldPkg, AndroidPackage newPkg) {
        if ((oldPkg.getFlags() & 1) == 0) {
            Slog.w("PackageManager", "Unable to update from " + oldPkg.getPackageName() + " to " + newPkg.getPackageName() + ": old package not in system partition");
            return false;
        } else if (this.mPm.mPackages.get(oldPkg.getPackageName()) != null) {
            Slog.w("PackageManager", "Unable to update from " + oldPkg.getPackageName() + " to " + newPkg.getPackageName() + ": old package still exists");
            return false;
        } else {
            return true;
        }
    }

    private void assertPackageIsValid(AndroidPackage pkg, int parseFlags, int scanFlags) throws PackageManagerException {
        if ((parseFlags & 64) != 0) {
            ScanPackageUtils.assertCodePolicy(pkg);
        }
        if (pkg.getPath() == null) {
            throw new PackageManagerException(-2, "Code and resource paths haven't been set correctly");
        }
        boolean isUserInstall = (scanFlags & 16) == 0;
        boolean isFirstBootOrUpgrade = (scanFlags & 4096) != 0;
        if ((isUserInstall || isFirstBootOrUpgrade) && this.mApexManager.isApexPackage(pkg.getPackageName())) {
            throw new PackageManagerException(-5, pkg.getPackageName() + " is an APEX package and can't be installed as an APK.");
        }
        KeySetManagerService ksms = this.mPm.mSettings.getKeySetManagerService();
        ksms.assertScannedPackageValid(pkg);
        synchronized (this.mPm.mLock) {
            if (pkg.getPackageName().equals(PackageManagerService.PLATFORM_PACKAGE_NAME) && this.mPm.getCoreAndroidApplication() != null) {
                Slog.w("PackageManager", "*************************************************");
                Slog.w("PackageManager", "Core android package being redefined.  Skipping.");
                Slog.w("PackageManager", " codePath=" + pkg.getPath());
                Slog.w("PackageManager", "*************************************************");
                throw new PackageManagerException(-5, "Core android package being redefined.  Skipping.");
            }
            PackageManagerService.sPmsExt.checkMtkResPkg(pkg);
            if ((scanFlags & 4) == 0 && this.mPm.mPackages.containsKey(pkg.getPackageName())) {
                throw new PackageManagerException(-5, "Application package " + pkg.getPackageName() + " already installed.  Skipping duplicate.");
            }
            if (pkg.isStaticSharedLibrary()) {
                if ((scanFlags & 4) == 0 && this.mPm.mPackages.containsKey(pkg.getManifestPackageName())) {
                    throw new PackageManagerException("Duplicate static shared lib provider package");
                }
                ScanPackageUtils.assertStaticSharedLibraryIsValid(pkg, scanFlags);
                assertStaticSharedLibraryVersionCodeIsValid(pkg);
            }
            if ((scanFlags & 128) != 0) {
                if (this.mPm.isExpectingBetter(pkg.getPackageName())) {
                    Slog.w("PackageManager", "Relax SCAN_REQUIRE_KNOWN requirement for package " + pkg.getPackageName());
                } else {
                    PackageSetting known = this.mPm.mSettings.getPackageLPr(pkg.getPackageName());
                    if (known != null) {
                        if (PackageManagerService.DEBUG_PACKAGE_SCANNING) {
                            Log.d("PackageManager", "Examining " + pkg.getPath() + " and requiring known path " + known.getPathString());
                        }
                        if (!pkg.getPath().equals(known.getPathString())) {
                            throw new PackageManagerException(-23, "Application package " + pkg.getPackageName() + " found at " + pkg.getPath() + " but expected at " + known.getPathString() + "; ignoring.");
                        }
                    } else {
                        throw new PackageManagerException(-19, "Application package " + pkg.getPackageName() + " not found; ignoring.");
                    }
                }
            }
            if ((scanFlags & 4) != 0) {
                this.mPm.mComponentResolver.assertProvidersNotDefined(pkg);
            }
            ScanPackageUtils.assertProcessesAreValid(pkg);
            assertPackageWithSharedUserIdIsPrivileged(pkg);
            if (pkg.getOverlayTarget() != null) {
                assertOverlayIsValid(pkg, parseFlags, scanFlags);
            }
            ScanPackageUtils.assertMinSignatureSchemeIsValid(pkg, parseFlags);
        }
    }

    private void assertStaticSharedLibraryVersionCodeIsValid(AndroidPackage pkg) throws PackageManagerException {
        long minVersionCode = Long.MIN_VALUE;
        long maxVersionCode = JobStatus.NO_LATEST_RUNTIME;
        WatchedLongSparseArray<SharedLibraryInfo> versionedLib = this.mSharedLibraries.getSharedLibraryInfos(pkg.getStaticSharedLibName());
        if (versionedLib != null) {
            int versionCount = versionedLib.size();
            int i = 0;
            while (true) {
                if (i >= versionCount) {
                    break;
                }
                SharedLibraryInfo libInfo = versionedLib.valueAt(i);
                long libVersionCode = libInfo.getDeclaringPackage().getLongVersionCode();
                if (libInfo.getLongVersion() < pkg.getStaticSharedLibVersion()) {
                    minVersionCode = Math.max(minVersionCode, 1 + libVersionCode);
                } else if (libInfo.getLongVersion() > pkg.getStaticSharedLibVersion()) {
                    maxVersionCode = Math.min(maxVersionCode, libVersionCode - 1);
                } else {
                    maxVersionCode = libVersionCode;
                    minVersionCode = libVersionCode;
                    break;
                }
                i++;
            }
        }
        if (pkg.getLongVersionCode() < minVersionCode || pkg.getLongVersionCode() > maxVersionCode) {
            throw new PackageManagerException("Static shared lib version codes must be ordered as lib versions");
        }
    }

    private void assertOverlayIsValid(AndroidPackage pkg, int parseFlags, int scanFlags) throws PackageManagerException {
        PackageSetting targetPkgSetting;
        if ((65536 & scanFlags) != 0) {
            if ((parseFlags & 16) == 0) {
                boolean isOSOverlay = ITranPackageManagerService.Instance().isOsOverlay(pkg);
                Log.i("binghua", "assertPackagelsValid pkg = " + pkg.getPackageName() + ", isOSOverlay = " + isOSOverlay);
                if (!this.mPm.isOverlayMutable(pkg.getPackageName()) && !isOSOverlay) {
                    throw new PackageManagerException("Overlay " + pkg.getPackageName() + " is static and cannot be upgraded.");
                }
                return;
            } else if ((524288 & scanFlags) != 0) {
                if (pkg.getTargetSdkVersion() < ScanPackageUtils.getVendorPartitionVersion()) {
                    Slog.w("PackageManager", "System overlay " + pkg.getPackageName() + " targets an SDK below the required SDK level of vendor overlays (" + ScanPackageUtils.getVendorPartitionVersion() + "). This will become an install error in a future release");
                    return;
                }
                return;
            } else if (pkg.getTargetSdkVersion() < Build.VERSION.SDK_INT) {
                Slog.w("PackageManager", "System overlay " + pkg.getPackageName() + " targets an SDK below the required SDK level of system overlays (" + Build.VERSION.SDK_INT + "). This will become an install error in a future release");
                return;
            } else {
                return;
            }
        }
        if (pkg.getTargetSdkVersion() < 29) {
            PackageSetting platformPkgSetting = this.mPm.mSettings.getPackageLPr(PackageManagerService.PLATFORM_PACKAGE_NAME);
            if (!PackageManagerServiceUtils.comparePackageSignatures(platformPkgSetting, pkg.getSigningDetails().getSignatures())) {
                throw new PackageManagerException("Overlay " + pkg.getPackageName() + " must target Q or later, or be signed with the platform certificate");
            }
        }
        if (pkg.getOverlayTargetOverlayableName() == null && (targetPkgSetting = this.mPm.mSettings.getPackageLPr(pkg.getOverlayTarget())) != null && !PackageManagerServiceUtils.comparePackageSignatures(targetPkgSetting, pkg.getSigningDetails().getSignatures())) {
            if (this.mPm.mOverlayConfigSignaturePackage == null) {
                throw new PackageManagerException("Overlay " + pkg.getPackageName() + " and target " + pkg.getOverlayTarget() + " signed with different certificates, and the overlay lacks <overlay android:targetName>");
            }
            PackageSetting refPkgSetting = this.mPm.mSettings.getPackageLPr(this.mPm.mOverlayConfigSignaturePackage);
            if (!PackageManagerServiceUtils.comparePackageSignatures(refPkgSetting, pkg.getSigningDetails().getSignatures())) {
                throw new PackageManagerException("Overlay " + pkg.getPackageName() + " signed with a different certificate than both the reference package and target " + pkg.getOverlayTarget() + ", and the overlay lacks <overlay android:targetName>");
            }
        }
    }

    private void assertPackageWithSharedUserIdIsPrivileged(AndroidPackage pkg) throws PackageManagerException {
        if (!pkg.isPrivileged() && pkg.getSharedUserId() != null) {
            SharedUserSetting sharedUserSetting = null;
            try {
                sharedUserSetting = this.mPm.mSettings.getSharedUserLPw(pkg.getSharedUserId(), 0, 0, false);
            } catch (PackageManagerException e) {
            }
            if (sharedUserSetting != null && sharedUserSetting.isPrivileged()) {
                PackageSetting platformPkgSetting = this.mPm.mSettings.getPackageLPr(PackageManagerService.PLATFORM_PACKAGE_NAME);
                if (!PackageManagerServiceUtils.comparePackageSignatures(platformPkgSetting, pkg.getSigningDetails().getSignatures())) {
                    throw new PackageManagerException("Apps that share a user with a privileged app must themselves be marked as privileged. " + pkg.getPackageName() + " shares privileged user " + pkg.getSharedUserId() + ".");
                }
            }
        }
    }

    private int adjustScanFlags(int scanFlags, PackageSetting pkgSetting, PackageSetting disabledPkgSetting, UserHandle user, AndroidPackage pkg) {
        int scanFlags2 = ScanPackageUtils.adjustScanFlagsWithPackageSetting(scanFlags, pkgSetting, disabledPkgSetting, user);
        boolean skipVendorPrivilegeScan = (524288 & scanFlags2) != 0 && ScanPackageUtils.getVendorPartitionVersion() < 28;
        if ((scanFlags2 & 131072) == 0 && !pkg.isPrivileged() && pkg.getSharedUserId() != null && !skipVendorPrivilegeScan) {
            SharedUserSetting sharedUserSetting = null;
            synchronized (this.mPm.mLock) {
                try {
                    sharedUserSetting = this.mPm.mSettings.getSharedUserLPw(pkg.getSharedUserId(), 0, 0, false);
                } catch (PackageManagerException e) {
                }
                if (sharedUserSetting != null && sharedUserSetting.isPrivileged()) {
                    PackageSetting platformPkgSetting = this.mPm.mSettings.getPackageLPr(PackageManagerService.PLATFORM_PACKAGE_NAME);
                    if (PackageManagerServiceUtils.compareSignatures(platformPkgSetting.getSigningDetails().getSignatures(), pkg.getSigningDetails().getSignatures()) != 0) {
                        scanFlags2 |= 131072;
                    }
                }
            }
        }
        return scanFlags2;
    }

    private void grantCtaRuntimePerm(boolean updated, PackageInstalledInfo res) {
        int[] iArr;
        boolean needGrantCtaRuntimePerm = CtaManagerFactory.getInstance().makeCtaManager().needGrantCtaRuntimePerm(updated, res.mPkg.getTargetSdkVersion());
        if (needGrantCtaRuntimePerm) {
            String[] ctaOnlyPermissions = CtaManagerFactory.getInstance().makeCtaManager().getCtaOnlyPermissions();
            Binder.getCallingUid();
            for (int userId : res.mNewUsers) {
                this.mPm.mPermissionManager.grantRequestedRuntimePermissionsInternal(res.mPkg, Arrays.asList(ctaOnlyPermissions), userId);
            }
        }
    }
}
