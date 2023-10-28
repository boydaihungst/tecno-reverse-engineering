package com.android.server.pm;

import android.content.pm.IPackageInstallObserver2;
import android.content.pm.PackageInfoLite;
import android.content.pm.PackageInstaller;
import android.content.pm.SigningDetails;
import android.content.pm.parsing.PackageLite;
import android.os.Message;
import android.os.Trace;
import android.os.UserHandle;
import android.util.ArrayMap;
import android.util.Pair;
import android.util.Slog;
import com.android.internal.content.F2fsUtils;
import com.android.internal.content.InstallLocationUtils;
import com.android.internal.util.Preconditions;
import com.android.server.pm.parsing.pkg.AndroidPackage;
import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
/* JADX INFO: Access modifiers changed from: package-private */
/* loaded from: classes2.dex */
public final class InstallParams extends HandlerParams {
    final List<String> mAllowlistedRestrictedPermissions;
    final int mAutoRevokePermissionsMode;
    final int mDataLoaderType;
    final boolean mForceQueryableOverride;
    final String[] mGrantedRuntimePermissions;
    int mInstallFlags;
    final int mInstallReason;
    final int mInstallScenario;
    final InstallSource mInstallSource;
    final MoveInfo mMoveInfo;
    final IPackageInstallObserver2 mObserver;
    final OriginInfo mOriginInfo;
    final String mPackageAbiOverride;
    final PackageLite mPackageLite;
    final int mPackageSource;
    MultiPackageInstallParams mParentInstallParams;
    final long mRequiredInstalledVersionCode;
    int mRet;
    final SigningDetails mSigningDetails;
    final String mVolumeUuid;

    /* JADX INFO: Access modifiers changed from: package-private */
    public InstallParams(OriginInfo originInfo, MoveInfo moveInfo, IPackageInstallObserver2 observer, int installFlags, InstallSource installSource, String volumeUuid, UserHandle user, String packageAbiOverride, int packageSource, PackageLite packageLite, PackageManagerService pm) {
        super(user, pm);
        this.mOriginInfo = originInfo;
        this.mMoveInfo = moveInfo;
        this.mObserver = observer;
        this.mInstallFlags = installFlags;
        this.mInstallSource = (InstallSource) Preconditions.checkNotNull(installSource);
        this.mVolumeUuid = volumeUuid;
        this.mPackageAbiOverride = packageAbiOverride;
        this.mGrantedRuntimePermissions = null;
        this.mAllowlistedRestrictedPermissions = null;
        this.mAutoRevokePermissionsMode = 3;
        this.mSigningDetails = SigningDetails.UNKNOWN;
        this.mInstallReason = 0;
        this.mInstallScenario = 0;
        this.mForceQueryableOverride = false;
        this.mDataLoaderType = 0;
        this.mRequiredInstalledVersionCode = -1L;
        this.mPackageSource = packageSource;
        this.mPackageLite = packageLite;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public InstallParams(File stagedDir, IPackageInstallObserver2 observer, PackageInstaller.SessionParams sessionParams, InstallSource installSource, UserHandle user, SigningDetails signingDetails, int installerUid, PackageLite packageLite, PackageManagerService pm) {
        super(user, pm);
        this.mOriginInfo = OriginInfo.fromStagedFile(stagedDir);
        this.mMoveInfo = null;
        this.mInstallReason = fixUpInstallReason(installSource.installerPackageName, installerUid, sessionParams.installReason);
        this.mInstallScenario = sessionParams.installScenario;
        this.mObserver = observer;
        this.mInstallFlags = sessionParams.installFlags;
        this.mInstallSource = installSource;
        this.mVolumeUuid = sessionParams.volumeUuid;
        this.mPackageAbiOverride = sessionParams.abiOverride;
        this.mGrantedRuntimePermissions = sessionParams.grantedRuntimePermissions;
        this.mAllowlistedRestrictedPermissions = sessionParams.whitelistedRestrictedPermissions;
        this.mAutoRevokePermissionsMode = sessionParams.autoRevokePermissionsMode;
        this.mSigningDetails = signingDetails;
        this.mForceQueryableOverride = sessionParams.forceQueryableOverride;
        this.mDataLoaderType = sessionParams.dataLoaderParams != null ? sessionParams.dataLoaderParams.getType() : 0;
        this.mRequiredInstalledVersionCode = sessionParams.requiredInstalledVersionCode;
        this.mPackageSource = sessionParams.packageSource;
        this.mPackageLite = packageLite;
    }

    public String toString() {
        return "InstallParams{" + Integer.toHexString(System.identityHashCode(this)) + " file=" + this.mOriginInfo.mFile + "}";
    }

    private int overrideInstallLocation(String packageName, int recommendedInstallLocation, int installLocation) {
        if (this.mOriginInfo.mStaged) {
            if (this.mOriginInfo.mFile != null) {
                this.mInstallFlags |= 16;
            } else {
                throw new IllegalStateException("Invalid stage location");
            }
        }
        if (recommendedInstallLocation < 0) {
            return InstallLocationUtils.getInstallationErrorCode(recommendedInstallLocation);
        }
        synchronized (this.mPm.mLock) {
            AndroidPackage installedPkg = this.mPm.mPackages.get(packageName);
            if (installedPkg != null) {
                recommendedInstallLocation = InstallLocationUtils.installLocationPolicy(installLocation, recommendedInstallLocation, this.mInstallFlags, installedPkg.isSystem(), installedPkg.isExternalStorage());
            }
        }
        int loc = recommendedInstallLocation;
        if (loc > 0) {
            PackageManagerService packageManagerService = this.mPm;
            this.mInstallFlags = PackageManagerService.sPmsExt.customizeInstallPkgFlags(this.mInstallFlags, packageName, this.mPm.mSettings.mPackages, getUser());
        }
        int i = this.mInstallFlags;
        boolean onInt = (i & 16) != 0;
        if (!onInt) {
            if (recommendedInstallLocation == 2) {
                this.mInstallFlags = i & (-17);
            } else {
                this.mInstallFlags = i | 16;
            }
        }
        return 1;
    }

    @Override // com.android.server.pm.HandlerParams
    public void handleStartCopy() {
        if ((this.mInstallFlags & 131072) != 0) {
            this.mRet = 1;
            return;
        }
        PackageInfoLite pkgLite = PackageManagerServiceUtils.getMinimalPackageInfo(this.mPm.mContext, this.mPackageLite, this.mOriginInfo.mResolvedPath, this.mInstallFlags, this.mPackageAbiOverride);
        boolean isStaged = (this.mInstallFlags & 2097152) != 0;
        if (isStaged) {
            Pair<Integer, String> ret = this.mInstallPackageHelper.verifyReplacingVersionCode(pkgLite, this.mRequiredInstalledVersionCode, this.mInstallFlags);
            int intValue = ((Integer) ret.first).intValue();
            this.mRet = intValue;
            if (intValue != 1) {
                return;
            }
        }
        boolean ephemeral = (this.mInstallFlags & 2048) != 0;
        if (PackageManagerService.DEBUG_INSTANT && ephemeral) {
            Slog.v("PackageManager", "pkgLite for install: " + pkgLite);
        }
        if (!this.mOriginInfo.mStaged && pkgLite.recommendedInstallLocation == -1) {
            pkgLite.recommendedInstallLocation = this.mPm.freeCacheForInstallation(pkgLite.recommendedInstallLocation, this.mPackageLite, this.mOriginInfo.mResolvedPath, this.mPackageAbiOverride, this.mInstallFlags);
        }
        this.mRet = overrideInstallLocation(pkgLite.packageName, pkgLite.recommendedInstallLocation, pkgLite.installLocation);
    }

    @Override // com.android.server.pm.HandlerParams
    void handleReturnCode() {
        processPendingInstall();
    }

    private void processPendingInstall() {
        InstallArgs args = createInstallArgs(this);
        if (this.mRet == 1) {
            this.mRet = args.copyApk();
        }
        if (this.mRet == 1) {
            F2fsUtils.releaseCompressedBlocks(this.mPm.mContext.getContentResolver(), new File(args.getCodePath()));
        }
        MultiPackageInstallParams multiPackageInstallParams = this.mParentInstallParams;
        if (multiPackageInstallParams != null) {
            multiPackageInstallParams.tryProcessInstallRequest(args, this.mRet);
            return;
        }
        PackageInstalledInfo res = new PackageInstalledInfo(this.mRet);
        processInstallRequestsAsync(res.mReturnCode == 1, Collections.singletonList(new InstallRequest(args, res)));
    }

    private InstallArgs createInstallArgs(InstallParams params) {
        if (params.mMoveInfo != null) {
            return new MoveInstallArgs(params);
        }
        return new FileInstallArgs(params);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void processInstallRequestsAsync(final boolean success, final List<InstallRequest> installRequests) {
        this.mPm.mHandler.post(new Runnable() { // from class: com.android.server.pm.InstallParams$$ExternalSyntheticLambda0
            @Override // java.lang.Runnable
            public final void run() {
                InstallParams.this.m5421xb6fb0c48(success, installRequests);
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$processInstallRequestsAsync$0$com-android-server-pm-InstallParams  reason: not valid java name */
    public /* synthetic */ void m5421xb6fb0c48(boolean success, List installRequests) {
        this.mInstallPackageHelper.processInstallRequests(success, installRequests);
    }

    private int fixUpInstallReason(String installerPackageName, int installerUid, int installReason) {
        if (this.mPm.snapshotComputer().checkUidPermission("android.permission.INSTALL_PACKAGES", installerUid) == 0) {
            return installReason;
        }
        String ownerPackage = this.mPm.mProtectedPackages.getDeviceOwnerOrProfileOwnerPackage(UserHandle.getUserId(installerUid));
        if (ownerPackage != null && ownerPackage.equals(installerPackageName)) {
            return 1;
        }
        if (installReason == 1) {
            return 0;
        }
        return installReason;
    }

    public void installStage() {
        Message msg = this.mPm.mHandler.obtainMessage(5);
        setTraceMethod("installStage").setTraceCookie(System.identityHashCode(this));
        msg.obj = this;
        Trace.asyncTraceBegin(262144L, "installStage", System.identityHashCode(msg.obj));
        Trace.asyncTraceBegin(262144L, "queueInstall", System.identityHashCode(msg.obj));
        this.mPm.mHandler.sendMessage(msg);
    }

    public void installStage(List<InstallParams> children) throws PackageManagerException {
        Message msg = this.mPm.mHandler.obtainMessage(5);
        MultiPackageInstallParams params = new MultiPackageInstallParams(this, children, this.mPm);
        params.setTraceMethod("installStageMultiPackage").setTraceCookie(System.identityHashCode(params));
        msg.obj = params;
        Trace.asyncTraceBegin(262144L, "installStageMultiPackage", System.identityHashCode(msg.obj));
        Trace.asyncTraceBegin(262144L, "queueInstall", System.identityHashCode(msg.obj));
        this.mPm.mHandler.sendMessage(msg);
    }

    public void movePackage() {
        Message msg = this.mPm.mHandler.obtainMessage(5);
        setTraceMethod("movePackage").setTraceCookie(System.identityHashCode(this));
        msg.obj = this;
        Trace.asyncTraceBegin(262144L, "movePackage", System.identityHashCode(msg.obj));
        Trace.asyncTraceBegin(262144L, "queueInstall", System.identityHashCode(msg.obj));
        this.mPm.mHandler.sendMessage(msg);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* loaded from: classes2.dex */
    public final class MultiPackageInstallParams extends HandlerParams {
        private final List<InstallParams> mChildParams;
        private final Map<InstallArgs, Integer> mCurrentState;

        MultiPackageInstallParams(InstallParams parent, List<InstallParams> childParams, PackageManagerService pm) throws PackageManagerException {
            super(parent.getUser(), pm);
            if (childParams.size() == 0) {
                throw new PackageManagerException("No child sessions found!");
            }
            this.mChildParams = childParams;
            for (int i = 0; i < childParams.size(); i++) {
                InstallParams childParam = childParams.get(i);
                childParam.mParentInstallParams = this;
            }
            this.mCurrentState = new ArrayMap(this.mChildParams.size());
        }

        @Override // com.android.server.pm.HandlerParams
        void handleStartCopy() {
            for (InstallParams params : this.mChildParams) {
                params.handleStartCopy();
            }
        }

        @Override // com.android.server.pm.HandlerParams
        void handleReturnCode() {
            for (InstallParams params : this.mChildParams) {
                params.handleReturnCode();
            }
        }

        void tryProcessInstallRequest(InstallArgs args, int currentStatus) {
            this.mCurrentState.put(args, Integer.valueOf(currentStatus));
            if (this.mCurrentState.size() != this.mChildParams.size()) {
                return;
            }
            int completeStatus = 1;
            Iterator<Integer> it = this.mCurrentState.values().iterator();
            while (true) {
                if (!it.hasNext()) {
                    break;
                }
                Integer status = it.next();
                if (status.intValue() == 0) {
                    return;
                }
                if (status.intValue() != 1) {
                    completeStatus = status.intValue();
                    break;
                }
            }
            List<InstallRequest> installRequests = new ArrayList<>(this.mCurrentState.size());
            for (Map.Entry<InstallArgs, Integer> entry : this.mCurrentState.entrySet()) {
                installRequests.add(new InstallRequest(entry.getKey(), new PackageInstalledInfo(completeStatus)));
            }
            InstallParams.this.processInstallRequestsAsync(completeStatus == 1, installRequests);
        }
    }
}
