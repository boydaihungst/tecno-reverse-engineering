package com.android.server.pm;

import android.app.AppOpsManager;
import android.app.BroadcastOptions;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.IPackageInstallObserver2;
import android.content.pm.PackageInfoLite;
import android.content.pm.PackageInstaller;
import android.content.pm.PackageManagerInternal;
import android.content.pm.ParceledListSlice;
import android.content.pm.ResolveInfo;
import android.content.pm.SigningDetails;
import android.content.pm.VerifierInfo;
import android.content.pm.parsing.PackageLite;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.Process;
import android.os.RemoteException;
import android.os.SystemProperties;
import android.os.Trace;
import android.os.UserHandle;
import android.provider.DeviceConfig;
import android.provider.Settings;
import android.text.TextUtils;
import android.util.ArraySet;
import android.util.Pair;
import android.util.Slog;
import com.android.server.DeviceIdleInternal;
import com.android.server.pm.VerificationParams;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;
/* JADX INFO: Access modifiers changed from: package-private */
/* loaded from: classes2.dex */
public final class VerificationParams extends HandlerParams {
    private static final long DEFAULT_ENABLE_ROLLBACK_TIMEOUT_MILLIS = 10000;
    private static final long DEFAULT_INTEGRITY_VERIFICATION_TIMEOUT = 30000;
    private static final boolean DEFAULT_INTEGRITY_VERIFY_ENABLE = true;
    private static final boolean DEFAULT_VERIFY_ENABLE = true;
    private static final String PROPERTY_ENABLE_ROLLBACK_TIMEOUT_MILLIS = "enable_rollback_timeout";
    final int mDataLoaderType;
    private String mErrorMessage;
    final int mInstallFlags;
    final InstallSource mInstallSource;
    final IPackageInstallObserver2 mObserver;
    final OriginInfo mOriginInfo;
    final String mPackageAbiOverride;
    final PackageLite mPackageLite;
    MultiPackageVerificationParams mParentVerificationParams;
    final long mRequiredInstalledVersionCode;
    private int mRet;
    final int mSessionId;
    final SigningDetails mSigningDetails;
    final VerificationInfo mVerificationInfo;
    private boolean mWaitForEnableRollbackToComplete;
    private boolean mWaitForIntegrityVerificationToComplete;
    private boolean mWaitForVerificationToComplete;

    /* JADX INFO: Access modifiers changed from: package-private */
    public VerificationParams(UserHandle user, File stagedDir, IPackageInstallObserver2 observer, PackageInstaller.SessionParams sessionParams, InstallSource installSource, int installerUid, SigningDetails signingDetails, int sessionId, PackageLite lite, PackageManagerService pm) {
        super(user, pm);
        this.mRet = 1;
        this.mErrorMessage = null;
        this.mOriginInfo = OriginInfo.fromStagedFile(stagedDir);
        this.mObserver = observer;
        this.mInstallFlags = sessionParams.installFlags;
        this.mInstallSource = installSource;
        this.mPackageAbiOverride = sessionParams.abiOverride;
        this.mVerificationInfo = new VerificationInfo(sessionParams.originatingUri, sessionParams.referrerUri, sessionParams.originatingUid, installerUid);
        this.mSigningDetails = signingDetails;
        this.mRequiredInstalledVersionCode = sessionParams.requiredInstalledVersionCode;
        this.mDataLoaderType = sessionParams.dataLoaderParams != null ? sessionParams.dataLoaderParams.getType() : 0;
        this.mSessionId = sessionId;
        this.mPackageLite = lite;
    }

    public String toString() {
        return "InstallParams{" + Integer.toHexString(System.identityHashCode(this)) + " file=" + this.mOriginInfo.mFile + "}";
    }

    @Override // com.android.server.pm.HandlerParams
    public void handleStartCopy() {
        PackageInfoLite pkgLite = PackageManagerServiceUtils.getMinimalPackageInfo(this.mPm.mContext, this.mPackageLite, this.mOriginInfo.mResolvedPath, this.mInstallFlags, this.mPackageAbiOverride);
        Pair<Integer, String> ret = this.mInstallPackageHelper.verifyReplacingVersionCode(pkgLite, this.mRequiredInstalledVersionCode, this.mInstallFlags);
        setReturnCode(((Integer) ret.first).intValue(), (String) ret.second);
        if (this.mRet == 1 && !this.mOriginInfo.mExisting) {
            if ((this.mInstallFlags & 131072) == 0) {
                sendApkVerificationRequest(pkgLite);
            }
            if ((this.mInstallFlags & 262144) != 0) {
                sendEnableRollbackRequest();
            }
        }
    }

    private void sendApkVerificationRequest(PackageInfoLite pkgLite) {
        PackageManagerService packageManagerService = this.mPm;
        int verificationId = packageManagerService.mPendingVerificationToken;
        packageManagerService.mPendingVerificationToken = verificationId + 1;
        PackageVerificationState verificationState = new PackageVerificationState(this);
        this.mPm.mPendingVerification.append(verificationId, verificationState);
        sendIntegrityVerificationRequest(verificationId, pkgLite, verificationState);
        sendPackageVerificationRequest(verificationId, pkgLite, verificationState);
        if (verificationState.areAllVerificationsComplete()) {
            this.mPm.mPendingVerification.remove(verificationId);
        }
    }

    void sendEnableRollbackRequest() {
        PackageManagerService packageManagerService = this.mPm;
        int enableRollbackToken = packageManagerService.mPendingEnableRollbackToken;
        packageManagerService.mPendingEnableRollbackToken = enableRollbackToken + 1;
        Trace.asyncTraceBegin(262144L, "enable_rollback", enableRollbackToken);
        this.mPm.mPendingEnableRollback.append(enableRollbackToken, this);
        Intent enableRollbackIntent = new Intent("android.intent.action.PACKAGE_ENABLE_ROLLBACK");
        enableRollbackIntent.putExtra(PackageManagerInternal.EXTRA_ENABLE_ROLLBACK_TOKEN, enableRollbackToken);
        enableRollbackIntent.putExtra(PackageManagerInternal.EXTRA_ENABLE_ROLLBACK_SESSION_ID, this.mSessionId);
        enableRollbackIntent.setType("application/vnd.android.package-archive");
        enableRollbackIntent.addFlags(1);
        enableRollbackIntent.addFlags(67108864);
        this.mPm.mContext.sendBroadcastAsUser(enableRollbackIntent, UserHandle.SYSTEM, "android.permission.PACKAGE_ROLLBACK_AGENT");
        this.mWaitForEnableRollbackToComplete = true;
        long rollbackTimeout = DeviceConfig.getLong("rollback", PROPERTY_ENABLE_ROLLBACK_TIMEOUT_MILLIS, 10000L);
        if (rollbackTimeout < 0) {
            rollbackTimeout = 10000;
        }
        Message msg = this.mPm.mHandler.obtainMessage(22);
        msg.arg1 = enableRollbackToken;
        msg.arg2 = this.mSessionId;
        this.mPm.mHandler.sendMessageDelayed(msg, rollbackTimeout);
    }

    void sendIntegrityVerificationRequest(final int verificationId, PackageInfoLite pkgLite, PackageVerificationState verificationState) {
        if (!isIntegrityVerificationEnabled()) {
            verificationState.setIntegrityVerificationResult(1);
            return;
        }
        Intent integrityVerification = new Intent("android.intent.action.PACKAGE_NEEDS_INTEGRITY_VERIFICATION");
        integrityVerification.setDataAndType(Uri.fromFile(new File(this.mOriginInfo.mResolvedPath)), "application/vnd.android.package-archive");
        integrityVerification.addFlags(1342177281);
        integrityVerification.putExtra("android.content.pm.extra.VERIFICATION_ID", verificationId);
        integrityVerification.putExtra("android.intent.extra.PACKAGE_NAME", pkgLite.packageName);
        integrityVerification.putExtra("android.intent.extra.VERSION_CODE", pkgLite.versionCode);
        integrityVerification.putExtra("android.intent.extra.LONG_VERSION_CODE", pkgLite.getLongVersionCode());
        populateInstallerExtras(integrityVerification);
        integrityVerification.setPackage(PackageManagerService.PLATFORM_PACKAGE_NAME);
        BroadcastOptions options = BroadcastOptions.makeBasic();
        this.mPm.mContext.sendOrderedBroadcastAsUser(integrityVerification, UserHandle.SYSTEM, null, -1, options.toBundle(), new BroadcastReceiver() { // from class: com.android.server.pm.VerificationParams.1
            @Override // android.content.BroadcastReceiver
            public void onReceive(Context context, Intent intent) {
                Message msg = VerificationParams.this.mPm.mHandler.obtainMessage(26);
                msg.arg1 = verificationId;
                VerificationParams.this.mPm.mHandler.sendMessageDelayed(msg, VerificationParams.this.getIntegrityVerificationTimeout());
            }
        }, null, 0, null, null);
        Trace.asyncTraceBegin(262144L, "integrity_verification", verificationId);
        this.mWaitForIntegrityVerificationToComplete = true;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public long getIntegrityVerificationTimeout() {
        long timeout = Settings.Global.getLong(this.mPm.mContext.getContentResolver(), "app_integrity_verification_timeout", 30000L);
        return Math.max(timeout, 30000L);
    }

    private boolean isIntegrityVerificationEnabled() {
        return true;
    }

    /* JADX WARN: Removed duplicated region for block: B:20:0x0055  */
    /* JADX WARN: Removed duplicated region for block: B:21:0x0057  */
    /* JADX WARN: Removed duplicated region for block: B:24:0x006d  */
    /* JADX WARN: Removed duplicated region for block: B:72:0x02fb  */
    /*
        Code decompiled incorrectly, please refer to instructions dump.
    */
    private void sendPackageVerificationRequest(final int verificationId, PackageInfoLite pkgLite, PackageVerificationState verificationState) {
        UserHandle verifierUser;
        String requiredVerifierPackage;
        boolean requiredVerifierPackageOverridden;
        int i;
        int requiredUid;
        boolean streaming;
        String rootHashString;
        List<ComponentName> sufficientVerifiers;
        String str;
        int requiredUid2;
        boolean streaming2;
        Intent verification;
        int verificationCodeAtTimeout;
        UserHandle verifierUser2 = getUser();
        if (verifierUser2 != UserHandle.ALL) {
            verifierUser = verifierUser2;
        } else {
            verifierUser = UserHandle.SYSTEM;
        }
        int verifierUserId = verifierUser.getIdentifier();
        String requiredVerifierPackage2 = this.mPm.mRequiredVerifierPackage;
        int i2 = this.mInstallFlags;
        if ((i2 & 32) != 0 && (i2 & 524288) == 0) {
            String adbVerifierOverridePackage = SystemProperties.get("debug.pm.adb_verifier_override_package", "");
            if (!TextUtils.isEmpty(adbVerifierOverridePackage) && packageExists(adbVerifierOverridePackage) && !isAdbVerificationEnabled(pkgLite, verifierUserId, true)) {
                requiredVerifierPackage = adbVerifierOverridePackage;
                requiredVerifierPackageOverridden = true;
                Computer snapshot = this.mPm.snapshotComputer();
                int requiredUid3 = requiredVerifierPackage != null ? -1 : snapshot.getPackageUid(requiredVerifierPackage, 268435456L, verifierUserId);
                verificationState.setRequiredVerifierUid(requiredUid3);
                boolean isVerificationEnabled = isVerificationEnabled(pkgLite, verifierUserId);
                if (!this.mOriginInfo.mExisting) {
                    i = 1;
                    requiredUid = requiredUid3;
                } else if (isVerificationEnabled) {
                    Intent verification2 = new Intent("android.intent.action.PACKAGE_NEEDS_VERIFICATION");
                    verification2.addFlags(268435456);
                    verification2.setDataAndType(Uri.fromFile(new File(this.mOriginInfo.mResolvedPath)), "application/vnd.android.package-archive");
                    verification2.addFlags(1);
                    ParceledListSlice<ResolveInfo> receivers = this.mPm.queryIntentReceivers(snapshot, verification2, "application/vnd.android.package-archive", 0L, verifierUserId);
                    String str2 = "PackageManager";
                    if (PackageManagerService.DEBUG_VERIFY) {
                        Slog.d("PackageManager", "Found " + receivers.getList().size() + " verifiers for intent " + verification2.toString() + " with " + pkgLite.verifiers.length + " optional verifiers");
                    }
                    Intent verification3 = verification2;
                    verification3.putExtra("android.content.pm.extra.VERIFICATION_ID", verificationId);
                    verification3.putExtra("android.content.pm.extra.VERIFICATION_INSTALL_FLAGS", this.mInstallFlags);
                    verification3.putExtra("android.content.pm.extra.VERIFICATION_PACKAGE_NAME", pkgLite.packageName);
                    verification3.putExtra("android.content.pm.extra.VERIFICATION_VERSION_CODE", pkgLite.versionCode);
                    verification3.putExtra("android.content.pm.extra.VERIFICATION_LONG_VERSION_CODE", pkgLite.getLongVersionCode());
                    String baseCodePath = this.mPackageLite.getBaseApkPath();
                    String[] splitCodePaths = this.mPackageLite.getSplitApkPaths();
                    String rootHashString2 = PackageManagerServiceUtils.buildVerificationRootHashString(baseCodePath, splitCodePaths);
                    if (rootHashString2 != null) {
                        verification3.putExtra("android.content.pm.extra.VERIFICATION_ROOT_HASH", rootHashString2);
                    }
                    verification3.putExtra("android.content.pm.extra.DATA_LOADER_TYPE", this.mDataLoaderType);
                    verification3.putExtra("android.content.pm.extra.SESSION_ID", this.mSessionId);
                    populateInstallerExtras(verification3);
                    boolean streaming3 = this.mDataLoaderType == 2 && this.mSigningDetails.getSignatureSchemeVersion() == 4 && getDefaultVerificationResponse() == 1;
                    final long verificationTimeout = VerificationUtils.getVerificationTimeout(this.mPm.mContext, streaming3);
                    List<ComponentName> sufficientVerifiers2 = matchVerifiers(pkgLite, receivers.getList(), verificationState);
                    if (!pkgLite.isSdkLibrary) {
                        streaming = streaming3;
                        rootHashString = rootHashString2;
                        sufficientVerifiers = sufficientVerifiers2;
                    } else {
                        if (sufficientVerifiers2 == null) {
                            sufficientVerifiers2 = new ArrayList();
                        }
                        streaming = streaming3;
                        rootHashString = rootHashString2;
                        ComponentName sdkSandboxComponentName = new ComponentName(PackageManagerService.PLATFORM_PACKAGE_NAME, "com.android.server.sdksandbox.SdkSandboxVerifierReceiver");
                        sufficientVerifiers2.add(sdkSandboxComponentName);
                        verificationState.addSufficientVerifier(Process.myUid());
                        sufficientVerifiers = sufficientVerifiers2;
                    }
                    DeviceIdleInternal idleController = (DeviceIdleInternal) this.mPm.mInjector.getLocalService(DeviceIdleInternal.class);
                    BroadcastOptions options = BroadcastOptions.makeBasic();
                    options.setTemporaryAppAllowlist(verificationTimeout, 0, 305, "");
                    if (sufficientVerifiers != null) {
                        int n = sufficientVerifiers.size();
                        if (n == 0) {
                            Slog.i("PackageManager", "Additional verifiers required, but none installed.");
                            setReturnCode(-22, "Additional verifiers required, but none installed.");
                            str = "PackageManager";
                            requiredUid2 = requiredUid3;
                            streaming2 = streaming;
                            verification = verification3;
                        } else {
                            int i3 = 0;
                            while (i3 < n) {
                                ComponentName verifierComponent = sufficientVerifiers.get(i3);
                                Intent verification4 = verification3;
                                idleController.addPowerSaveTempWhitelistApp(Process.myUid(), verifierComponent.getPackageName(), verificationTimeout, verifierUserId, false, 305, "package verifier");
                                Intent sufficientIntent = new Intent(verification4);
                                sufficientIntent.setComponent(verifierComponent);
                                this.mPm.mContext.sendBroadcastAsUser(sufficientIntent, verifierUser, null, options.toBundle());
                                i3++;
                                verification3 = verification4;
                                str2 = str2;
                                n = n;
                                baseCodePath = baseCodePath;
                                splitCodePaths = splitCodePaths;
                                sufficientVerifiers = sufficientVerifiers;
                                snapshot = snapshot;
                            }
                            str = str2;
                            requiredUid2 = requiredUid3;
                            streaming2 = streaming;
                            verification = verification3;
                        }
                    } else {
                        str = "PackageManager";
                        requiredUid2 = requiredUid3;
                        streaming2 = streaming;
                        verification = verification3;
                    }
                    if (requiredVerifierPackage == null) {
                        Slog.e(str, "Required verifier is null");
                        return;
                    }
                    if (getDefaultVerificationResponse() == 1) {
                        verificationCodeAtTimeout = 2;
                    } else {
                        verificationCodeAtTimeout = -1;
                    }
                    final PackageVerificationResponse response = new PackageVerificationResponse(verificationCodeAtTimeout, requiredUid2);
                    if (!requiredVerifierPackageOverridden) {
                        ComponentName requiredVerifierComponent = matchComponentForVerifier(requiredVerifierPackage, receivers.getList());
                        verification.setComponent(requiredVerifierComponent);
                    } else {
                        verification.setPackage(requiredVerifierPackage);
                    }
                    idleController.addPowerSaveTempWhitelistApp(Process.myUid(), requiredVerifierPackage, verificationTimeout, verifierUserId, false, 305, "package verifier");
                    if (streaming2) {
                        startVerificationTimeoutCountdown(verificationId, streaming2, response, verificationTimeout);
                    }
                    final boolean z = streaming2;
                    this.mPm.mContext.sendOrderedBroadcastAsUser(verification, verifierUser, "android.permission.PACKAGE_VERIFICATION_AGENT", -1, options.toBundle(), new BroadcastReceiver() { // from class: com.android.server.pm.VerificationParams.2
                        @Override // android.content.BroadcastReceiver
                        public void onReceive(Context context, Intent intent) {
                            boolean z2 = z;
                            if (!z2) {
                                VerificationParams.this.startVerificationTimeoutCountdown(verificationId, z2, response, verificationTimeout);
                            }
                        }
                    }, null, 0, null, null);
                    Trace.asyncTraceBegin(262144L, "verification", verificationId);
                    this.mWaitForVerificationToComplete = true;
                    return;
                } else {
                    i = 1;
                    requiredUid = requiredUid3;
                }
                verificationState.setVerifierResponse(requiredUid, i);
            }
        }
        requiredVerifierPackage = requiredVerifierPackage2;
        requiredVerifierPackageOverridden = false;
        Computer snapshot2 = this.mPm.snapshotComputer();
        int requiredUid32 = requiredVerifierPackage != null ? -1 : snapshot2.getPackageUid(requiredVerifierPackage, 268435456L, verifierUserId);
        verificationState.setRequiredVerifierUid(requiredUid32);
        boolean isVerificationEnabled2 = isVerificationEnabled(pkgLite, verifierUserId);
        if (!this.mOriginInfo.mExisting) {
        }
        verificationState.setVerifierResponse(requiredUid, i);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void startVerificationTimeoutCountdown(int verificationId, boolean streaming, PackageVerificationResponse response, long verificationTimeout) {
        Message msg = this.mPm.mHandler.obtainMessage(16);
        msg.arg1 = verificationId;
        msg.arg2 = streaming ? 1 : 0;
        msg.obj = response;
        this.mPm.mHandler.sendMessageDelayed(msg, verificationTimeout);
    }

    int getDefaultVerificationResponse() {
        if (this.mPm.mUserManager.hasUserRestriction("ensure_verify_apps", getUser().getIdentifier())) {
            return -1;
        }
        return Settings.Global.getInt(this.mPm.mContext.getContentResolver(), "verifier_default_response", 1);
    }

    private boolean packageExists(String packageName) {
        boolean z;
        synchronized (this.mPm.mLock) {
            z = this.mPm.mSettings.getPackageLPr(packageName) != null;
        }
        return z;
    }

    private boolean isAdbVerificationEnabled(PackageInfoLite pkgInfoLite, int userId, boolean requestedDisableVerification) {
        if (this.mPm.isUserRestricted(userId, "ensure_verify_apps")) {
            return true;
        }
        if (!requestedDisableVerification) {
            return Settings.Global.getInt(this.mPm.mContext.getContentResolver(), "verifier_verify_adb_installs", 1) != 0;
        } else if (packageExists(pkgInfoLite.packageName)) {
            return !pkgInfoLite.debuggable;
        } else {
            return true;
        }
    }

    private boolean isVerificationEnabled(PackageInfoLite pkgInfoLite, int userId) {
        VerificationInfo verificationInfo = this.mVerificationInfo;
        int installerUid = verificationInfo == null ? -1 : verificationInfo.mInstallerUid;
        int installFlags = this.mInstallFlags;
        if ((installFlags & 32) != 0) {
            boolean requestedDisableVerification = (this.mInstallFlags & 524288) != 0;
            return isAdbVerificationEnabled(pkgInfoLite, userId, requestedDisableVerification);
        }
        if ((installFlags & 2048) != 0 && this.mPm.mInstantAppInstallerActivity != null && this.mPm.mInstantAppInstallerActivity.packageName.equals(this.mPm.mRequiredVerifierPackage)) {
            try {
                ((AppOpsManager) this.mPm.mInjector.getSystemService(AppOpsManager.class)).checkPackage(installerUid, this.mPm.mRequiredVerifierPackage);
                if (PackageManagerService.DEBUG_VERIFY) {
                    Slog.i("PackageManager", "disable verification for instant app");
                }
                return false;
            } catch (SecurityException e) {
            }
        }
        return true;
    }

    private List<ComponentName> matchVerifiers(PackageInfoLite pkgInfo, List<ResolveInfo> receivers, PackageVerificationState verificationState) {
        int verifierUid;
        if (pkgInfo == null || pkgInfo.verifiers == null) {
            Slog.i("PackageManager", "pkgInfo =" + (pkgInfo == null ? "null" : pkgInfo.toString()));
            return null;
        } else if (pkgInfo.verifiers.length == 0) {
            return null;
        } else {
            int n = pkgInfo.verifiers.length;
            List<ComponentName> sufficientVerifiers = new ArrayList<>(n + 1);
            for (int i = 0; i < n; i++) {
                VerifierInfo verifierInfo = pkgInfo.verifiers[i];
                ComponentName comp = matchComponentForVerifier(verifierInfo.packageName, receivers);
                if (comp != null && (verifierUid = this.mInstallPackageHelper.getUidForVerifier(verifierInfo)) != -1) {
                    if (PackageManagerService.DEBUG_VERIFY) {
                        Slog.d("PackageManager", "Added sufficient verifier " + verifierInfo.packageName + " with the correct signature");
                    }
                    sufficientVerifiers.add(comp);
                    verificationState.addSufficientVerifier(verifierUid);
                }
            }
            return sufficientVerifiers;
        }
    }

    private static ComponentName matchComponentForVerifier(String packageName, List<ResolveInfo> receivers) {
        ActivityInfo targetReceiver = null;
        int nr = receivers.size();
        int i = 0;
        while (true) {
            if (i >= nr) {
                break;
            }
            ResolveInfo info = receivers.get(i);
            if (info.activityInfo == null || !packageName.equals(info.activityInfo.packageName)) {
                i++;
            } else {
                targetReceiver = info.activityInfo;
                break;
            }
        }
        if (targetReceiver == null) {
            return null;
        }
        return new ComponentName(targetReceiver.packageName, targetReceiver.name);
    }

    void populateInstallerExtras(Intent intent) {
        intent.putExtra("android.content.pm.extra.VERIFICATION_INSTALLER_PACKAGE", this.mInstallSource.initiatingPackageName);
        VerificationInfo verificationInfo = this.mVerificationInfo;
        if (verificationInfo != null) {
            if (verificationInfo.mOriginatingUri != null) {
                intent.putExtra("android.intent.extra.ORIGINATING_URI", this.mVerificationInfo.mOriginatingUri);
            }
            if (this.mVerificationInfo.mReferrer != null) {
                intent.putExtra("android.intent.extra.REFERRER", this.mVerificationInfo.mReferrer);
            }
            if (this.mVerificationInfo.mOriginatingUid >= 0) {
                intent.putExtra("android.intent.extra.ORIGINATING_UID", this.mVerificationInfo.mOriginatingUid);
            }
            if (this.mVerificationInfo.mInstallerUid >= 0) {
                intent.putExtra("android.content.pm.extra.VERIFICATION_INSTALLER_UID", this.mVerificationInfo.mInstallerUid);
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void setReturnCode(int ret, String message) {
        if (this.mRet == 1) {
            this.mRet = ret;
            this.mErrorMessage = message;
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void handleVerificationFinished() {
        this.mWaitForVerificationToComplete = false;
        handleReturnCode();
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void handleIntegrityVerificationFinished() {
        this.mWaitForIntegrityVerificationToComplete = false;
        handleReturnCode();
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void handleRollbackEnabled() {
        this.mWaitForEnableRollbackToComplete = false;
        handleReturnCode();
    }

    @Override // com.android.server.pm.HandlerParams
    void handleReturnCode() {
        if (this.mWaitForVerificationToComplete || this.mWaitForIntegrityVerificationToComplete || this.mWaitForEnableRollbackToComplete) {
            return;
        }
        sendVerificationCompleteNotification();
    }

    private void sendVerificationCompleteNotification() {
        MultiPackageVerificationParams multiPackageVerificationParams = this.mParentVerificationParams;
        if (multiPackageVerificationParams != null) {
            multiPackageVerificationParams.trySendVerificationCompleteNotification(this);
            return;
        }
        try {
            this.mObserver.onPackageInstalled((String) null, this.mRet, this.mErrorMessage, new Bundle());
        } catch (RemoteException e) {
            Slog.i("PackageManager", "Observer no longer exists.");
        }
    }

    public void verifyStage() {
        this.mPm.mHandler.post(new Runnable() { // from class: com.android.server.pm.VerificationParams$$ExternalSyntheticLambda1
            @Override // java.lang.Runnable
            public final void run() {
                VerificationParams.this.startCopy();
            }
        });
    }

    public void verifyStage(List<VerificationParams> children) throws PackageManagerException {
        final MultiPackageVerificationParams params = new MultiPackageVerificationParams(this, children, this.mPm);
        Handler handler = this.mPm.mHandler;
        Objects.requireNonNull(params);
        handler.post(new Runnable() { // from class: com.android.server.pm.VerificationParams$$ExternalSyntheticLambda0
            @Override // java.lang.Runnable
            public final void run() {
                VerificationParams.MultiPackageVerificationParams.this.startCopy();
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* loaded from: classes2.dex */
    public static final class MultiPackageVerificationParams extends HandlerParams {
        private final List<VerificationParams> mChildParams;
        private final IPackageInstallObserver2 mObserver;
        private final Set<VerificationParams> mVerificationState;

        MultiPackageVerificationParams(VerificationParams parent, List<VerificationParams> children, PackageManagerService pm) throws PackageManagerException {
            super(parent.getUser(), pm);
            if (children.size() == 0) {
                throw new PackageManagerException("No child sessions found!");
            }
            this.mChildParams = children;
            for (int i = 0; i < children.size(); i++) {
                VerificationParams childParams = children.get(i);
                childParams.mParentVerificationParams = this;
            }
            this.mVerificationState = new ArraySet(this.mChildParams.size());
            this.mObserver = parent.mObserver;
        }

        @Override // com.android.server.pm.HandlerParams
        void handleStartCopy() {
            for (VerificationParams params : this.mChildParams) {
                params.handleStartCopy();
            }
        }

        @Override // com.android.server.pm.HandlerParams
        void handleReturnCode() {
            for (VerificationParams params : this.mChildParams) {
                params.handleReturnCode();
            }
        }

        void trySendVerificationCompleteNotification(VerificationParams child) {
            this.mVerificationState.add(child);
            if (this.mVerificationState.size() != this.mChildParams.size()) {
                return;
            }
            int completeStatus = 1;
            String errorMsg = null;
            for (VerificationParams params : this.mVerificationState) {
                int status = params.mRet;
                if (status != 1) {
                    completeStatus = status;
                    errorMsg = params.mErrorMessage;
                    break;
                }
            }
            try {
                this.mObserver.onPackageInstalled((String) null, completeStatus, errorMsg, new Bundle());
            } catch (RemoteException e) {
                Slog.i("PackageManager", "Observer no longer exists.");
            }
        }
    }
}
