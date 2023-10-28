package com.android.server.pm;

import android.content.pm.IPackageInstallObserver2;
import android.content.pm.SigningDetails;
import android.os.UserHandle;
import com.android.internal.util.Preconditions;
import com.android.server.pm.parsing.pkg.ParsedPackage;
import java.util.List;
/* JADX INFO: Access modifiers changed from: package-private */
/* loaded from: classes2.dex */
public abstract class InstallArgs {
    final String mAbiOverride;
    final List<String> mAllowlistedRestrictedPermissions;
    final int mAutoRevokePermissionsMode;
    final int mDataLoaderType;
    final boolean mForceQueryableOverride;
    final int mInstallFlags;
    final String[] mInstallGrantPermissions;
    final int mInstallReason;
    final int mInstallScenario;
    final InstallSource mInstallSource;
    String[] mInstructionSets;
    final MoveInfo mMoveInfo;
    final IPackageInstallObserver2 mObserver;
    final OriginInfo mOriginInfo;
    final int mPackageSource;
    final PackageManagerService mPm;
    final RemovePackageHelper mRemovePackageHelper;
    final SigningDetails mSigningDetails;
    final int mTraceCookie;
    final String mTraceMethod;
    final UserHandle mUser;
    final String mVolumeUuid;

    /* JADX INFO: Access modifiers changed from: package-private */
    public abstract void cleanUpResourcesLI();

    /* JADX INFO: Access modifiers changed from: package-private */
    public abstract int copyApk();

    /* JADX INFO: Access modifiers changed from: package-private */
    public abstract boolean doPostDeleteLI(boolean z);

    /* JADX INFO: Access modifiers changed from: package-private */
    public abstract int doPostInstall(int i, int i2);

    /* JADX INFO: Access modifiers changed from: package-private */
    public abstract int doPreInstall(int i);

    /* JADX INFO: Access modifiers changed from: package-private */
    public abstract boolean doRename(int i, ParsedPackage parsedPackage);

    /* JADX INFO: Access modifiers changed from: package-private */
    public abstract String getCodePath();

    /* JADX INFO: Access modifiers changed from: package-private */
    public InstallArgs(OriginInfo originInfo, MoveInfo moveInfo, IPackageInstallObserver2 observer, int installFlags, InstallSource installSource, String volumeUuid, UserHandle user, String[] instructionSets, String abiOverride, String[] installGrantPermissions, List<String> allowlistedRestrictedPermissions, int autoRevokePermissionsMode, String traceMethod, int traceCookie, SigningDetails signingDetails, int installReason, int installScenario, boolean forceQueryableOverride, int dataLoaderType, int packageSource, PackageManagerService pm) {
        this.mOriginInfo = originInfo;
        this.mMoveInfo = moveInfo;
        this.mInstallFlags = installFlags;
        this.mObserver = observer;
        this.mInstallSource = (InstallSource) Preconditions.checkNotNull(installSource);
        this.mVolumeUuid = volumeUuid;
        this.mUser = user;
        this.mInstructionSets = instructionSets;
        this.mAbiOverride = abiOverride;
        this.mInstallGrantPermissions = installGrantPermissions;
        this.mAllowlistedRestrictedPermissions = allowlistedRestrictedPermissions;
        this.mAutoRevokePermissionsMode = autoRevokePermissionsMode;
        this.mTraceMethod = traceMethod;
        this.mTraceCookie = traceCookie;
        this.mSigningDetails = signingDetails;
        this.mInstallReason = installReason;
        this.mInstallScenario = installScenario;
        this.mForceQueryableOverride = forceQueryableOverride;
        this.mDataLoaderType = dataLoaderType;
        this.mPackageSource = packageSource;
        this.mPm = pm;
        this.mRemovePackageHelper = new RemovePackageHelper(pm);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public InstallArgs(InstallParams params) {
        this(params.mOriginInfo, params.mMoveInfo, params.mObserver, params.mInstallFlags, params.mInstallSource, params.mVolumeUuid, params.getUser(), null, params.mPackageAbiOverride, params.mGrantedRuntimePermissions, params.mAllowlistedRestrictedPermissions, params.mAutoRevokePermissionsMode, params.mTraceMethod, params.mTraceCookie, params.mSigningDetails, params.mInstallReason, params.mInstallScenario, params.mForceQueryableOverride, params.mDataLoaderType, params.mPackageSource, params.mPm);
    }

    int doPreCopy() {
        return 1;
    }

    int doPostCopy(int uid) {
        return 1;
    }

    protected boolean isEphemeral() {
        return (this.mInstallFlags & 2048) != 0;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public UserHandle getUser() {
        return this.mUser;
    }
}
